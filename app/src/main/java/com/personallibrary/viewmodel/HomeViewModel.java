package com.personallibrary.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.personallibrary.models.Book;
import com.personallibrary.repository.BookRepository;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final BookRepository repo;
    private final MutableLiveData<List<Book>> recentBooks = new MutableLiveData<>();
    private final MutableLiveData<Integer> libraryCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> wishlistCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public HomeViewModel(@NonNull Application app) {
        super(app);
        repo = new BookRepository(app);
    }

    public LiveData<List<Book>> getRecentBooks()  { return recentBooks; }
    public LiveData<Integer> getLibraryCount()    { return libraryCount; }
    public LiveData<Integer> getWishlistCount()   { return wishlistCount; }
    public LiveData<Boolean> getIsLoading()       { return isLoading; }

    public void loadData() {
        // [ISPRAVKA L9] Shimmer se prikazuje samo pri PRVOM učitavanju. Ranije je pri svakom
        // povratku na ekran sakrivao listu i pravio treperenje.
        if (recentBooks.getValue() == null) isLoading.setValue(true);

        repo.getRecentLibraryBooks(books -> {
            recentBooks.setValue(books);
            isLoading.setValue(false);
        });
        repo.getLibraryCount(c -> libraryCount.setValue(c));
        repo.getWishlistCount(c -> wishlistCount.setValue(c));
    }
}
