package com.personallibrary.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.personallibrary.models.Book;
import com.personallibrary.repository.BookRepository;

import java.util.List;

public class LibraryViewModel extends AndroidViewModel {

    private final BookRepository repo;
    private final MutableLiveData<List<Book>> books = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isEmpty = new MutableLiveData<>(false);
    private final MutableLiveData<List<String>> genres = new MutableLiveData<>();
    private final MutableLiveData<String> activeGenre = new MutableLiveData<>("");

    public LibraryViewModel(@NonNull Application app) {
        super(app);
        repo = new BookRepository(app);
    }

    public LiveData<List<Book>> getBooks()      { return books; }
    public LiveData<Boolean> getIsEmpty()       { return isEmpty; }
    public LiveData<List<String>> getGenres()   { return genres; }
    public LiveData<String> getActiveGenre()    { return activeGenre; }

    public void loadBooks() {
        String genre = activeGenre.getValue();
        if (genre != null && !genre.isEmpty()) repo.getLibraryBooksByGenre(genre, this::publishBooks);
        else repo.getAllLibraryBooks(this::publishBooks);

        repo.getAllGenres(list -> {
            genres.setValue(list);
            // [ISPRAVKA L2] Ako je aktivni žanr nestao (obrisana posljednja knjiga tog žanra),
            // vrati filter na "Sve" umjesto da korisnik gleda praznu listu.
            String active = activeGenre.getValue();
            if (active != null && !active.isEmpty() && !list.contains(active)) {
                activeGenre.setValue("");
                loadBooks();
            }
        });
    }

    private void publishBooks(List<Book> list) {
        books.setValue(list);
        isEmpty.setValue(list.isEmpty());
    }

    public void filterByGenre(String genre) { activeGenre.setValue(genre); loadBooks(); }
    public void clearFilter()               { activeGenre.setValue(""); loadBooks(); }
}
