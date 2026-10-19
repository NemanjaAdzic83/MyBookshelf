package com.personallibrary.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.personallibrary.models.Book;
import com.personallibrary.models.BookInfo;
import com.personallibrary.repository.BookRepository;
import com.personallibrary.utils.GoogleBooksApiClient;

import java.util.Locale;

/**
 * [ISPRAVKA K3] NOVA KLASA.
 *
 * BarcodeScannerActivity je ranije sama pozivala DAO, držala vlastiti ExecutorService i u
 * callback-ima API-ja direktno koristila "binding". Ako bi korisnik napustio ekran dok je
 * zahtjev u toku, binding je već bio null (onDestroy) i aplikacija bi pala sa NullPointerException.
 * Sada se sva logika nalazi ovdje, a Activity samo posmatra LiveData, koji je vezan za životni
 * ciklus i nikad ne isporučuje podatke uništenom ekranu.
 */
public class ScannerViewModel extends AndroidViewModel {

    public enum Type { LOADING, FOUND, NOT_FOUND, ERROR }

    public static final class ScanState {
        public final Type type;
        public final BookInfo info;     // FOUND
        public final Book existing;     // FOUND (null ako knjiga još nije u kolekciji)
        public final String isbn;       // NOT_FOUND
        public final String message;    // ERROR

        ScanState(Type type, BookInfo info, Book existing, String isbn, String message) {
            this.type = type; this.info = info; this.existing = existing;
            this.isbn = isbn; this.message = message;
        }
    }

    private final BookRepository repo;
    private final GoogleBooksApiClient api = new GoogleBooksApiClient();
    private final MutableLiveData<ScanState> state = new MutableLiveData<>();
    private boolean busy = false;

    public ScannerViewModel(@NonNull Application app) {
        super(app);
        repo = new BookRepository(app);
    }

    public LiveData<ScanState> getState() { return state; }
    public boolean isBusy()               { return busy; }

    /** Poziva se nakon što je dijalog zatvoren - skener smije ponovo čitati kodove. */
    public void scanFinished()  { busy = false; }

    /** Sprječava ponovni prikaz istog dijaloga ako se Activity ponovo kreira. */
    public void consumeState()  { state.setValue(null); }

    public void onCodeScanned(String rawCode) {
        if (busy || rawCode == null) return;
        busy = true;

        final String isbn = extractIsbn(rawCode.trim());
        if (isbn.isEmpty()) {
            state.setValue(new ScanState(Type.NOT_FOUND, null, null, rawCode, null));
            return;
        }

        state.setValue(new ScanState(Type.LOADING, null, null, null, null));
        api.fetchBookByIsbn(isbn, new GoogleBooksApiClient.BookInfoCallback() {
            @Override public void onSuccess(BookInfo info) { lookupExisting(info); }
            @Override public void onError(String msg) {
                state.setValue(new ScanState(Type.ERROR, null, null, null, msg));
            }
            @Override public void onNotFound() {
                state.setValue(new ScanState(Type.NOT_FOUND, null, null, isbn, null));
            }
        });
    }

    private void lookupExisting(BookInfo info) {
        String title = info.getTitle() != null ? info.getTitle() : "";
        String last = info.getAuthorLastName() != null ? info.getAuthorLastName() : "";
        repo.findByTitleAndAuthor(title, last, existing ->
                state.setValue(new ScanState(Type.FOUND, info, existing, null, null)));
    }

    public void saveBook(BookInfo info, boolean wishlist, BookRepository.Callback<Boolean> cb) {
        repo.addBook(info.toBook(wishlist), id -> cb.onResult(id != null && id > 0));
    }

    /** Iz QR/URL/"isbn:..." sadržaja izvlači ISBN. [ISPRAVKA L18] prepoznaje i malo 'x'. */
    static String extractIsbn(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("isbn:");
        if (idx >= 0) return clean(raw.substring(idx + 5));

        if (lower.startsWith("http")) {
            String digits = raw.replaceAll("[^0-9]", "");
            if (digits.length() >= 13) return digits.substring(digits.length() - 13);
            if (digits.length() >= 10) return digits;
        }
        return clean(raw);
    }

    private static String clean(String s) {
        return s.replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
    }

    @Override
    protected void onCleared() { api.shutdown(); }
}
