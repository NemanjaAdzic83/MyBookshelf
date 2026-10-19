package com.personallibrary.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.personallibrary.BuildConfig;
import com.personallibrary.models.BookInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Traži knjigu po ISBN-u.
 *
 * [ISPRAVKA N1] 1) Google Books je primarni izvor. Bez API ključa često vraća HTTP 429
 *                   ("previše zahtjeva"), pa je dodana opcija API_KEY (besplatan ključ).
 *                2) Ako Google ne uspije ili ne nađe knjigu, automatski se pokušava Open Library
 *                   (bez ključa, bez ograničenja za ovakvu upotrebu).
 */
public class GoogleBooksApiClient {
    private static final String TAG = "BooksAPI";

    /**
     * Optional Google Books API key, read from local.properties (GOOGLE_BOOKS_API_KEY=...).
     * Without it the app still works: Open Library is used as a fallback.
     */
    private static final String API_KEY = BuildConfig.GOOGLE_BOOKS_API_KEY;

    private static final String GOOGLE_URL = "https://www.googleapis.com/books/v1/volumes?q=isbn:";
    private static final String OPEN_LIBRARY_URL = "https://openlibrary.org/api/books?format=json&jscmd=data&bibkeys=ISBN:";

    public interface BookInfoCallback {
        void onSuccess(BookInfo info);
        void onError(String msg);
        void onNotFound();
    }

    private static class ApiException extends Exception {
        ApiException(String message) { super(message); }
    }

    private final OkHttpClient http = new OkHttpClient();
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    public void fetchBookByIsbn(String isbn, BookInfoCallback cb) {
        // [ISPRAVKA L18] dozvoljeno i malo 'x'; prazan kod (npr. QR bez cifara) ne šalje besmislen upit
        final String clean = isbn == null ? ""
                : isbn.replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
        if (clean.isEmpty()) { ui.post(cb::onNotFound); return; }

        exec.execute(() -> {
            BookInfo info = null;
            String error = null;

            // 1) Google Books
            try {
                info = queryGoogle(clean);
            } catch (IOException e) {
                error = "Check your internet connection.\n(" + e.getMessage() + ")";
            } catch (ApiException e) {
                error = e.getMessage();
            } catch (Exception e) {
                Log.e(TAG, "Google greška", e);
                error = "Unexpected error while searching.";
            }

            // 2) Rezervni izvor: Open Library (ako Google nije uspio ili nije našao knjigu)
            if (info == null) {
                try {
                    info = queryOpenLibrary(clean);
                } catch (Exception e) {
                    Log.w(TAG, "Open Library: " + e.getMessage());
                }
            }

            final BookInfo found = info;
            final String err = error;
            if (found != null) ui.post(() -> cb.onSuccess(found));
            else if (err != null) ui.post(() -> cb.onError(err));
            else ui.post(cb::onNotFound);
        });
    }

    // ---------------- Google Books ----------------
    private BookInfo queryGoogle(String isbn) throws IOException, ApiException {
        String url = GOOGLE_URL + isbn + (API_KEY.isEmpty() ? "" : "&key=" + API_KEY);
        Request req = new Request.Builder().url(url)
                .addHeader("User-Agent", "MyBookshelf/1.0").build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) {
                throw new ApiException(res.code() == 429
                        ? "Google Books has temporarily limited requests (HTTP 429).\nTry again in a few minutes or enter the book manually."
                        : "HTTP error: " + res.code());
            }
            String body = res.body() != null ? res.body().string() : "";
            return parseGoogle(body, isbn);
        }
    }

    private BookInfo parseGoogle(String json, String isbn) {
        try {
            JSONObject root = new JSONObject(json);
            if (root.optInt("totalItems", 0) == 0) return null;
            JSONObject vi = root.getJSONArray("items").getJSONObject(0).getJSONObject("volumeInfo");
            BookInfo info = new BookInfo();
            info.setIsbn(isbn);
            info.setTitle(vi.optString("title", "Unknown title"));
            if (vi.has("authors")) {
                JSONArray a = vi.getJSONArray("authors");
                if (a.length() > 0) { String[] p = splitName(a.getString(0)); info.setAuthorFirstName(p[0]); info.setAuthorLastName(p[1]); }
            } else { info.setAuthorFirstName(""); info.setAuthorLastName("Unknown author"); }
            info.setPublisher(vi.optString("publisher", ""));
            info.setPublishedDate(vi.optString("publishedDate", ""));
            info.setDescription(vi.optString("description", ""));
            if (vi.has("imageLinks")) {
                JSONObject imgs = vi.getJSONObject("imageLinks");
                String u = imgs.optString("extraLarge", imgs.optString("large", imgs.optString("medium", imgs.optString("thumbnail", ""))));
                u = u.replace("http://", "https://");
                info.setCoverImageUrl(u.isEmpty() ? null : u);
            }
            return info;
        } catch (Exception e) { Log.e(TAG, "JSON greška (Google): " + e.getMessage()); return null; }
    }

    // ---------------- Open Library ----------------
    private BookInfo queryOpenLibrary(String isbn) throws IOException, ApiException {
        Request req = new Request.Builder().url(OPEN_LIBRARY_URL + isbn)
                .addHeader("User-Agent", "MyBookshelf/1.0").build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new ApiException("Open Library HTTP error: " + res.code());
            String body = res.body() != null ? res.body().string() : "";
            return parseOpenLibrary(body, isbn);
        }
    }

    private BookInfo parseOpenLibrary(String json, String isbn) {
        try {
            // Odgovor je objekat sa ključem "ISBN:<broj>"; ako knjige nema, vraća se prazan objekat {}
            JSONObject book = new JSONObject(json).optJSONObject("ISBN:" + isbn);
            if (book == null) return null;

            BookInfo info = new BookInfo();
            info.setIsbn(isbn);
            info.setTitle(book.optString("title", "Unknown title"));

            JSONArray authors = book.optJSONArray("authors");
            if (authors != null && authors.length() > 0) {
                String[] p = splitName(authors.getJSONObject(0).optString("name", ""));
                info.setAuthorFirstName(p[0]);
                info.setAuthorLastName(p[1]);
            } else {
                info.setAuthorFirstName("");
                info.setAuthorLastName("Unknown author");
            }

            JSONArray publishers = book.optJSONArray("publishers");
            info.setPublisher(publishers != null && publishers.length() > 0
                    ? publishers.getJSONObject(0).optString("name", "") : "");
            info.setPublishedDate(book.optString("publish_date", ""));
            info.setDescription("");

            JSONObject cover = book.optJSONObject("cover");
            if (cover != null) {
                String u = cover.optString("large", cover.optString("medium", cover.optString("small", "")));
                info.setCoverImageUrl(u.isEmpty() ? null : u.replace("http://", "https://"));
            }
            return info;
        } catch (Exception e) { Log.e(TAG, "JSON greška (Open Library): " + e.getMessage()); return null; }
    }

    // ---------------- zajedničko ----------------
    private String[] splitName(String full) {
        if (full == null || full.trim().isEmpty()) return new String[]{"", "Unknown"};
        full = full.trim();
        if (full.contains(",")) { String[] p = full.split(",", 2); return new String[]{p[1].trim(), p[0].trim()}; }
        int sp = full.lastIndexOf(' ');
        return sp > 0 ? new String[]{full.substring(0, sp).trim(), full.substring(sp + 1).trim()} : new String[]{"", full};
    }

    public void shutdown() { exec.shutdown(); }
}
