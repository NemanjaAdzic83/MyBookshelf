package com.personallibrary.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.personallibrary.models.Book;
import com.personallibrary.repository.BookRepository;
import com.personallibrary.utils.CoverStorage;

/** Služi za dodavanje NOVE knjige i za IZMJENU postojeće (kada se pozove startEdit). */
public class AddBookViewModel extends AndroidViewModel {

    public enum SaveResult {
        SUCCESS_LIBRARY, SUCCESS_WISHLIST, SUCCESS_UPDATED,
        ERROR_EXISTS_LIBRARY, ERROR_EXISTS_WISHLIST,
        ERROR_SAVE, ERROR_COVER, ERROR_NOT_FOUND
    }

    private final BookRepository repo;
    private final MutableLiveData<SaveResult> saveResult = new MutableLiveData<>();
    private final MutableLiveData<String> coverPath = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Book> editBook = new MutableLiveData<>();

    // Stanje forme živi u ViewModelu, pa preživljava rotaciju ekrana.
    private String genre = "";
    private boolean saved = false;

    // --- samo za izmjenu ---
    private Book editingBook = null;      // knjiga koja se mijenja
    private String originalCover = null;  // naslovnica prije izmjene (ne smije se obrisati dok izmjena nije snimljena)
    private boolean editLoading = false;

    public AddBookViewModel(@NonNull Application app) {
        super(app);
        repo = new BookRepository(app);
    }

    public LiveData<SaveResult> getSaveResult() { return saveResult; }
    public LiveData<String> getCoverPath()       { return coverPath; }
    public LiveData<Boolean> getBusy()           { return busy; }
    public LiveData<Book> getEditBook()          { return editBook; }
    public String getGenre()                     { return genre; }
    public void setGenre(String g)               { genre = g == null ? "" : g; }

    public void consumeSaveResult() { saveResult.setValue(null); }
    /** Poziva se kad Activity prepiše podatke knjige u polja, da se to ne ponovi nakon rotacije. */
    public void consumeEditBook()   { editBook.setValue(null); }

    private boolean isBusy() { return Boolean.TRUE.equals(busy.getValue()); }

    private void publish(SaveResult r) {
        busy.setValue(false);
        saveResult.setValue(r);
    }

    // ---------- izmjena: učitavanje ----------
    public void startEdit(int id) {
        if (editingBook != null || editLoading) return;   // već učitano (npr. nakon rotacije)
        editLoading = true;
        busy.setValue(true);                               // dugmad su onemogućena dok se knjiga učitava
        repo.getBookById(id, b -> {
            editLoading = false;
            busy.setValue(false);
            if (b == null) { saveResult.setValue(SaveResult.ERROR_NOT_FOUND); return; }
            editingBook = b;
            originalCover = b.getCoverImagePath();
            genre = b.getGenre() == null ? "" : b.getGenre();
            coverPath.setValue(originalCover);
            editBook.setValue(b);
        });
    }

    // ---------- naslovnica ----------
    public void importCover(Uri uri) {
        if (isBusy()) return;
        busy.setValue(true);
        repo.saveCover(uri, path -> {
            busy.setValue(false);
            if (path == null) { saveResult.setValue(SaveResult.ERROR_COVER); return; }
            deleteTempCover();                 // zamjena: obriši prethodnu privremenu kopiju
            coverPath.setValue(path);
        });
    }

    public void removeCover() {
        deleteTempCover();
        coverPath.setValue(null);
    }

    /** Briše samo NOVO odabranu sliku koja još nije snimljena; originalna naslovnica knjige se ne dira. */
    private void deleteTempCover() {
        String current = coverPath.getValue();
        if (current != null && !current.equals(originalCover)) {
            CoverStorage.deleteIfLocal(getApplication(), current);
        }
    }

    @Override
    protected void onCleared() {
        if (!saved) deleteTempCover();         // korisnik je odustao - ne ostavljaj sliku na disku
    }

    // ---------- snimanje nove knjige ----------
    public void saveBook(String title, String firstName, String lastName, boolean toWishlist) {
        if (isBusy()) return;
        busy.setValue(true);

        repo.bookExistsInLibrary(title, lastName, inLib -> {
            if (inLib) { publish(SaveResult.ERROR_EXISTS_LIBRARY); return; }

            repo.bookExistsAnywhere(title, lastName, anywhere -> {
                if (anywhere) { publish(SaveResult.ERROR_EXISTS_WISHLIST); return; }

                Book book = new Book();
                book.setTitle(title);
                book.setAuthorFirstName(firstName);
                book.setAuthorLastName(lastName);
                book.setCoverImagePath(coverPath.getValue());
                book.setGenre(genre);
                book.setInWishlist(toWishlist);

                repo.addBook(book, id -> {
                    if (id != null && id > 0) {
                        saved = true;
                        publish(toWishlist ? SaveResult.SUCCESS_WISHLIST : SaveResult.SUCCESS_LIBRARY);
                    } else {
                        publish(SaveResult.ERROR_SAVE);
                    }
                });
            });
        });
    }

    // ---------- snimanje izmjene ----------
    public void saveEdit(String title, String firstName, String lastName) {
        if (isBusy() || editingBook == null) return;
        busy.setValue(true);

        // Provjera duplikata IZUZIMA samu knjigu, inače bi izmjena samo žanra javila "već postoji"
        repo.findOtherByTitleAndAuthor(title, lastName, editingBook.getId(), other -> {
            if (other != null) {
                publish(other.isInWishlist() ? SaveResult.ERROR_EXISTS_WISHLIST : SaveResult.ERROR_EXISTS_LIBRARY);
                return;
            }
            // id, datum dodavanja i mjesto (biblioteka / lista želja) ostaju nepromijenjeni
            editingBook.setTitle(title);
            editingBook.setAuthorFirstName(firstName);
            editingBook.setAuthorLastName(lastName);
            editingBook.setGenre(genre);
            editingBook.setCoverImagePath(coverPath.getValue());

            repo.updateEditedBook(editingBook, originalCover, ok -> {
                if (ok != null && ok) { saved = true; publish(SaveResult.SUCCESS_UPDATED); }
                else publish(SaveResult.ERROR_SAVE);
            });
        });
    }
}
