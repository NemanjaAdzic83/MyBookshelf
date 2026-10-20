package com.personallibrary.repository;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.personallibrary.database.AppDatabase;
import com.personallibrary.database.BookDao;
import com.personallibrary.models.Book;
import com.personallibrary.utils.CoverStorage;

import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class BookRepository {

    public interface Callback<T> { void onResult(T result); }

    // [ISPRAVKA L10] Jedan zajednički executor i handler za SVE instance.
    // Ranije je svaki ViewModel pravio svoj Repository, a svaki Repository svoju nit
    // (newSingleThreadExecutor) koja se nikada nije gasila - curenje niti.
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler UI = new Handler(Looper.getMainLooper());

    private final Application app;
    private final BookDao dao;

    public BookRepository(Application app) {
        this.app = app;
        this.dao = AppDatabase.getInstance(app).bookDao();
    }

    /** Izvršava posao u pozadini, a rezultat vraća na glavnu nit. Zamjenjuje 15 ponovljenih lambdi. */
    private <T> void async(Supplier<T> work, Callback<T> cb) {
        EXEC.execute(() -> {
            T result = work.get();
            if (cb != null) UI.post(() -> cb.onResult(result));
        });
    }

    // ---------- čitanje ----------
    public void getRecentLibraryBooks(Callback<List<Book>> cb) { async(dao::getRecentLibraryBooks, cb); }
    public void getAllLibraryBooks(Callback<List<Book>> cb) {
        async(() -> sortByAuthor(dao.getAllLibraryBooksSortedByAuthor()), cb);
    }
    public void getLibraryBooksByGenre(String genre, Callback<List<Book>> cb) {
        async(() -> sortByAuthor(dao.getLibraryBooksByGenre(genre)), cb);
    }
    public void getWishlistBooks(Callback<List<Book>> cb)      { async(dao::getWishlistBooks, cb); }
    public void getAllGenres(Callback<List<String>> cb)        { async(dao::getAllGenres, cb); }
    public void getBookById(int id, Callback<Book> cb)         { async(() -> dao.getBookById(id), cb); }
    public void getLibraryCount(Callback<Integer> cb)          { async(dao::getLibraryCount, cb); }
    public void getWishlistCount(Callback<Integer> cb)         { async(dao::getWishlistCount, cb); }

    // ---------- sorting ----------
    // SQL ORDER BY compares characters by code point, which misorders letters such as Č, Ć, Š, Ž, Đ
    // (and Cyrillic Ј, Љ, Њ, Ђ, Ћ, Џ). A Collator with the "sr" locale sorts both Serbian scripts correctly
    // and works fine for plain English names too.
    private static final Collator COLLATOR = Collator.getInstance(new Locale("sr"));

    private static String nz(String s) { return s == null ? "" : s; }

    private static List<Book> sortByAuthor(List<Book> books) {
        List<Book> sorted = new ArrayList<>(books);
        Collections.sort(sorted, (a, b) -> {
            int c = COLLATOR.compare(nz(a.getAuthorLastName()), nz(b.getAuthorLastName()));
            if (c != 0) return c;
            c = COLLATOR.compare(nz(a.getAuthorFirstName()), nz(b.getAuthorFirstName()));
            if (c != 0) return c;
            return COLLATOR.compare(nz(a.getTitle()), nz(b.getTitle()));
        });
        return sorted;
    }

    // ---------- pretraga (ISPRAVKA L3) ----------
    public void searchBooks(String query, Callback<List<Book>> cb) {
        async(() -> {
            String needle = normalizeForSearch(query);
            List<Book> out = new ArrayList<>();
            for (Book b : dao.getAllBooks()) {
                if (normalizeForSearch(b.getTitle()).contains(needle)
                        || normalizeForSearch(b.getFullAuthorName()).contains(needle)) {
                    out.add(b);
                }
            }
            return out;
        }, cb);
    }

    /** "Čarobnjak" -> "carobnjak", "Andrić" -> "andric", "Đorđe" -> "dorde" (pretraga bez dijakritika). */
    static String normalizeForSearch(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        t = t.replaceAll("\\p{M}+", "");
        return t.replace('đ', 'd');
    }

    // ---------- provjera duplikata (ISPRAVKA L3) ----------
    private static boolean sameText(String a, String b) {
        return (a == null ? "" : a.trim()).equalsIgnoreCase(b == null ? "" : b.trim());
    }

    private static boolean sameBook(Book b, String title, String lastName) {
        return sameText(b.getTitle(), title) && sameText(b.getAuthorLastName(), lastName);
    }

    public void bookExistsInLibrary(String title, String lastName, Callback<Boolean> cb) {
        async(() -> {
            for (Book b : dao.getAllBooks()) {
                if (!b.isInWishlist() && sameBook(b, title, lastName)) return true;
            }
            return false;
        }, cb);
    }

    public void bookExistsAnywhere(String title, String lastName, Callback<Boolean> cb) {
        async(() -> {
            for (Book b : dao.getAllBooks()) {
                if (sameBook(b, title, lastName)) return true;
            }
            return false;
        }, cb);
    }

    public void findByTitleAndAuthor(String title, String lastName, Callback<Book> cb) {
        async(() -> {
            for (Book b : dao.getAllBooks()) {
                if (sameBook(b, title, lastName)) return b;
            }
            return null;
        }, cb);
    }

    /** Isto kao findByTitleAndAuthor, ali preskače knjigu sa datim id-jem (potrebno pri izmjeni knjige). */
    public void findOtherByTitleAndAuthor(String title, String lastName, int excludeId, Callback<Book> cb) {
        async(() -> {
            for (Book b : dao.getAllBooks()) {
                if (b.getId() != excludeId && sameBook(b, title, lastName)) return b;
            }
            return null;
        }, cb);
    }

    // ---------- pisanje ----------
    public void addBook(Book book, Callback<Long> cb) {
        async(() -> {
            book.setDateAdded(System.currentTimeMillis());
            return dao.insertBook(book);
        }, cb);
    }

    public void updateBook(Book book, Callback<Void> cb) {
        async(() -> { dao.updateBook(book); return null; }, cb);
    }

    /** Snima izmijenjenu knjigu; ako je naslovnica zamijenjena ili uklonjena, briše staru lokalnu sliku. */
    public void updateEditedBook(Book book, String previousCover, Callback<Boolean> cb) {
        async(() -> {
            dao.updateBook(book);
            String now = book.getCoverImagePath();
            if (previousCover != null && !previousCover.equals(now)) {
                CoverStorage.deleteIfLocal(app, previousCover);
            }
            return true;
        }, cb);
    }

    public void deleteBook(int id, Callback<Boolean> cb) {
        async(() -> {
            Book b = dao.getBookById(id);
            if (b == null) return false;
            dao.deleteBook(b);
            CoverStorage.deleteIfLocal(app, b.getCoverImagePath());   // ne ostavljamo "siročad" fajlove
            return true;
        }, cb);
    }

    public void moveToLibrary(int id, Callback<Boolean> cb) {
        async(() -> { dao.moveToLibrary(id, System.currentTimeMillis());
            return true; }, cb);
    }

    public void moveToWishlist(int id, Callback<Boolean> cb){
        async(() -> { dao.moveToWishlist(id, System.currentTimeMillis());
            return true; }, cb);
    }

    /** Kopira odabranu sliku u privatni direktorij (ISPRAVKA K4). Rezultat: putanja ili null. */
    public void saveCover(Uri source, Callback<String> cb) {
        async(() -> CoverStorage.copyToInternal(app, source), cb);
    }
}
