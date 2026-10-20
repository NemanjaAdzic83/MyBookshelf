package com.personallibrary.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.personallibrary.database.AppDatabase;
import com.personallibrary.database.BookDao;
import com.personallibrary.models.Book;
import com.personallibrary.repository.BookRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Exports the whole collection to a single .zip file and imports it back.
 *
 * ZIP layout:
 *   books.json          metadata of every book (library AND wishlist)
 *   covers/0.jpg ...    the user's own cover photos (copies from filesDir/covers)
 *
 * Why a ZIP and not just JSON: cover photos live in the app's private folder and would be
 * lost together with the app on uninstall, so a backup without them would be incomplete.
 *
 * Why no extra library: org.json and java.util.zip are part of Android.
 *
 * Import only ADDS books. Existing data is never deleted or overwritten.
 */
public final class BackupManager {

    private static final String FORMAT = "MyBookshelfBackup";
    private static final int VERSION = 1;
    private static final String JSON_ENTRY = "books.json";
    private static final String COVER_PREFIX = "covers/";
    private static final String COVER_DIR = "covers";   // same folder as CoverStorage

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler UI = new Handler(Looper.getMainLooper());

    private BackupManager() { }

    /** Outcome of an export or import, delivered on the main thread. */
    public static final class Result {
        public final boolean ok;
        public final int count;      // exported books, or imported books
        public final int skipped;    // import only: books already in the app
        public final String error;

        private Result(boolean ok, int count, int skipped, String error) {
            this.ok = ok; this.count = count; this.skipped = skipped; this.error = error;
        }
        static Result success(int count, int skipped) { return new Result(true, count, skipped, null); }
        static Result failure(String error)           { return new Result(false, 0, 0, error); }
    }

    // ------------------------------------------------------------------ public API

    public static void exportTo(Context ctx, Uri dest, BookRepository.Callback<Result> cb) {
        Context app = ctx.getApplicationContext();
        EXEC.execute(() -> {
            Result r;
            try {
                r = Result.success(doExport(app, dest), 0);
            } catch (Exception e) {
                r = Result.failure(describe(e));
            }
            final Result out = r;
            UI.post(() -> cb.onResult(out));
        });
    }

    public static void importFrom(Context ctx, Uri source, BookRepository.Callback<Result> cb) {
        Context app = ctx.getApplicationContext();
        EXEC.execute(() -> {
            Result r;
            try {
                r = doImport(app, source);
            } catch (Exception e) {
                r = Result.failure(describe(e));
            }
            final Result out = r;
            UI.post(() -> cb.onResult(out));
        });
    }

    // ------------------------------------------------------------------ export

    private static int doExport(Context ctx, Uri dest) throws Exception {
        List<Book> books = AppDatabase.getInstance(ctx).bookDao().getAllBooks();

        JSONArray array = new JSONArray();
        List<File> coverFiles = new ArrayList<>();     // index i  <->  entry "covers/i.jpg"

        for (Book b : books) {
            JSONObject o = new JSONObject();
            // Database ids are NOT exported: the importing phone assigns its own.
            o.put("title", b.getTitle());
            o.put("authorFirstName", b.getAuthorFirstName() == null ? "" : b.getAuthorFirstName());
            o.put("authorLastName", b.getAuthorLastName() == null ? "" : b.getAuthorLastName());
            if (b.hasGenre()) o.put("genre", b.getGenre());
            o.put("inWishlist", b.isInWishlist());
            o.put("read", b.isRead());
            o.put("dateAdded", b.getDateAdded());

            String cover = b.getCoverImagePath();
            File local = localCover(ctx, cover);
            if (local != null) {
                o.put("coverFile", COVER_PREFIX + coverFiles.size() + ".jpg");
                coverFiles.add(local);
            } else if (cover != null && !cover.isEmpty()) {
                o.put("coverUrl", cover);               // web cover: keep the link
            }
            array.put(o);
        }

        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("version", VERSION);
        root.put("exportedAt", System.currentTimeMillis());
        root.put("books", array);

        // "wt" = write and truncate, so an older longer file is not left behind.
        try (OutputStream os = ctx.getContentResolver().openOutputStream(dest, "wt")) {
            if (os == null) throw new IOException("Cannot open the destination file");
            try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(os))) {
                // JSON goes first so the importer can read the file in one forward pass.
                zip.putNextEntry(new ZipEntry(JSON_ENTRY));
                zip.write(root.toString().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();

                byte[] buf = new byte[8192];
                for (int i = 0; i < coverFiles.size(); i++) {
                    zip.putNextEntry(new ZipEntry(COVER_PREFIX + i + ".jpg"));
                    try (InputStream in = new FileInputStream(coverFiles.get(i))) {
                        int n;
                        while ((n = in.read(buf)) != -1) zip.write(buf, 0, n);
                    }
                    zip.closeEntry();
                }
            }
        }
        return books.size();
    }

    /** Returns the file only if it is one of OUR local cover copies and still exists. */
    private static File localCover(Context ctx, String path) {
        if (path == null || path.isEmpty()) return null;
        File f = new File(path);
        File dir = new File(ctx.getFilesDir(), COVER_DIR);
        return (f.isFile() && dir.equals(f.getParentFile())) ? f : null;
    }

    // ------------------------------------------------------------------ import

    private static Result doImport(Context ctx, Uri source) throws Exception {
        File coverDir = new File(ctx.getFilesDir(), COVER_DIR);
        if (!coverDir.exists() && !coverDir.mkdirs()) throw new IOException("Cannot create the covers folder");

        String json = null;
        // zip entry name -> path of the cover copy we created on this phone
        Map<String, String> restored = new HashMap<>();

        try {
            try (InputStream raw = ctx.getContentResolver().openInputStream(source)) {
                if (raw == null) throw new IOException("Cannot open the selected file");
                try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw))) {
                    ZipEntry e;
                    while ((e = zip.getNextEntry()) != null) {
                        String name = e.getName();
                        if (e.isDirectory()) continue;
                        if (JSON_ENTRY.equals(name)) {
                            json = new String(readAll(zip), StandardCharsets.UTF_8);
                        } else if (name.startsWith(COVER_PREFIX)) {
                            // SECURITY: the entry name is never used as a path (that would allow
                            // "zip slip" with names like ../../x). We always make our own file name.
                            File out = new File(coverDir, UUID.randomUUID() + ".jpg");
                            try (OutputStream os = new FileOutputStream(out)) {
                                byte[] buf = new byte[8192];
                                int n;
                                while ((n = zip.read(buf)) != -1) os.write(buf, 0, n);
                            }
                            restored.put(name, out.getAbsolutePath());
                        }
                    }
                }
            }

            if (json == null) throw new IOException("This file is not a MyBookshelf backup");

            JSONArray array;
            try {
                JSONObject root = new JSONObject(json);
                if (!FORMAT.equals(root.optString("format")))
                    throw new IOException("This file is not a MyBookshelf backup");
                if (root.optInt("version", 0) > VERSION)
                    throw new IOException("This backup was made by a newer version of the app");
                array = root.getJSONArray("books");
            } catch (JSONException ex) {
                throw new IOException("This file is not a MyBookshelf backup");
            }

            final AppDatabase db = AppDatabase.getInstance(ctx);
            final BookDao dao = db.bookDao();
            final int[] counts = new int[2];                 // [0] imported, [1] skipped
            final Set<String> usedCovers = new HashSet<>();

            // One transaction: either every new book is saved or none of them.
            db.runInTransaction(() -> {
                List<Book> known = dao.getAllBooks();        // grows as we insert, so duplicates
                // inside the file are skipped too
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.optJSONObject(i);
                    if (o == null) continue;

                    String title = o.optString("title", "").trim();
                    String last  = o.optString("authorLastName", "").trim();
                    if (title.isEmpty() || last.isEmpty() || contains(known, title, last)) {
                        counts[1]++;
                        continue;
                    }

                    Book b = new Book();
                    b.setTitle(title);
                    b.setAuthorFirstName(o.optString("authorFirstName", "").trim());
                    b.setAuthorLastName(last);
                    b.setGenre(o.has("genre") ? o.optString("genre") : null);
                    boolean wish = o.optBoolean("inWishlist", false);
                    b.setInWishlist(wish);
                    b.setRead(!wish && o.optBoolean("read", false));
                    b.setDateAdded(o.optLong("dateAdded", System.currentTimeMillis()));

                    String coverFile = o.optString("coverFile", "");
                    String local = restored.get(coverFile);
                    if (local != null) {
                        b.setCoverImagePath(local);
                        usedCovers.add(local);
                    } else if (o.has("coverUrl")) {
                        b.setCoverImagePath(o.optString("coverUrl"));
                    }

                    dao.insertBook(b);                       // id = 0, Room generates a new one
                    known.add(b);
                    counts[0]++;
                }
            });

            // Covers that belong to skipped books are not needed: remove them.
            for (String path : restored.values()) {
                if (!usedCovers.contains(path)) //noinspection ResultOfMethodCallIgnored
                    new File(path).delete();
            }
            return Result.success(counts[0], counts[1]);

        } catch (Exception e) {
            // Roll back the file side too: nothing may be left behind after a failed import.
            for (String path : restored.values()) //noinspection ResultOfMethodCallIgnored
                new File(path).delete();
            throw e;
        }
    }

    /** Same rule as the duplicate check when adding by hand: title + author last name, ignoring case. */
    private static boolean contains(List<Book> list, String title, String last) {
        for (Book b : list) {
            if (same(b.getTitle(), title) && same(b.getAuthorLastName(), last)) return true;
        }
        return false;
    }

    private static boolean same(String a, String b) {
        return (a == null ? "" : a.trim()).equalsIgnoreCase(b == null ? "" : b.trim());
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}