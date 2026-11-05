package com.personallibrary.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.personallibrary.models.Book;
import com.personallibrary.repository.BookRepository;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LibraryViewModel extends AndroidViewModel {

    // second filter , combined with the genre filter
    public enum ReadFilter{
        ALL,
        READ,
        UNREAD
    }

    public enum SortOrder{
        AUTHOR,
        TITLE,
        NEWEST,
        OLDEST
    }

    private static final Collator COLLATOR = Collator.getInstance(new Locale("sr"));

    private final BookRepository repo;
    private final MutableLiveData<List<Book>> books = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isEmpty = new MutableLiveData<>(false);
    private final MutableLiveData<List<String>> genres = new MutableLiveData<>();
    private final MutableLiveData<String> activeGenre = new MutableLiveData<>("");
    private final MutableLiveData<ReadFilter> readFilter = new MutableLiveData<>(ReadFilter.ALL);
    private final MutableLiveData<SortOrder> sortOrder = new MutableLiveData<>(SortOrder.AUTHOR);

    public LibraryViewModel(@NonNull Application app) {
        super(app);
        repo = new BookRepository(app);
    }

    public LiveData<List<Book>> getBooks()      { return books; }
    public LiveData<Boolean> getIsEmpty()       { return isEmpty; }
    public LiveData<List<String>> getGenres()   { return genres; }
    public LiveData<String> getActiveGenre()    { return activeGenre; }
    public LiveData<ReadFilter> getReadFilter(){
        return readFilter;
    }
    public LiveData<SortOrder> getSortOrder(){
        return sortOrder;
    }

    public void loadBooks() {
        String genre = activeGenre.getValue();
        if (genre != null && !genre.isEmpty()) repo.getLibraryBooksByGenre(genre, this::publishBooks);
        else repo.getAllLibraryBooks(this::publishBooks);

        repo.getAllGenres(list -> {
            genres.setValue(list);
            // Ako je aktivni žanr nestao (obrisana posljednja knjiga tog žanra),
            // vrati filter na "Sve" umjesto da korisnik gleda praznu listu.
            String active = activeGenre.getValue();
            if (active != null && !active.isEmpty() && !list.contains(active)) {
                activeGenre.setValue("");
                loadBooks();
            }
        });
    }

    private void publishBooks(List<Book> list) {
        ReadFilter f = readFilter.getValue();
        List<Book> out = new ArrayList<>();
        for (Book b : list){
            if (f == null || f == ReadFilter.ALL || (f == ReadFilter.READ) == b.isRead()) out.add(b);
        }

        // The list arrives sorted by author; other orders are applied here
        SortOrder s = sortOrder.getValue();
        if (s == SortOrder.TITLE) {
            Collections.sort(out, (a, b) -> COLLATOR.compare(nz(a.getTitle()), nz(b.getTitle())));
        } else if (s == SortOrder.NEWEST) {
            Collections.sort(out, (a, b) -> Long.compare(b.getDateAdded(), a.getDateAdded()));
        } else if (s == SortOrder.OLDEST) {
            Collections.sort(out, (a, b) -> Long.compare(a.getDateAdded(), b.getDateAdded()));
        }

        books.setValue(out);
        isEmpty.setValue(out.isEmpty());
    }

    private static String nz(String s) { return s == null ? "" : s; }

    public void filterByGenre(String genre) { activeGenre.setValue(genre); loadBooks(); }
    public void clearFilter()               { activeGenre.setValue(""); loadBooks(); }
    public void setReadFilter(ReadFilter f){
        readFilter.setValue(f);
        loadBooks();
    }

    public void setSortOrder(SortOrder s){
        sortOrder.setValue(s);
        loadBooks();
    }
}
