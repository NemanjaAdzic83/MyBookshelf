package com.personallibrary.viewmodel;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.personallibrary.models.Book;
import com.personallibrary.repository.BookRepository;
import java.util.List;
public class WishlistViewModel extends AndroidViewModel {
    private final BookRepository repo;
    private final MutableLiveData<List<Book>> books   = new MutableLiveData<>();
    private final MutableLiveData<Boolean>    isEmpty = new MutableLiveData<>(false);
    public WishlistViewModel(@NonNull Application app) { super(app); repo = new BookRepository(app); }
    public LiveData<List<Book>> getBooks()   { return books; }
    public LiveData<Boolean>    getIsEmpty() { return isEmpty; }
    public void loadBooks() { repo.getWishlistBooks(list -> { books.setValue(list); isEmpty.setValue(list.isEmpty()); }); }

    // "Bought": wishlist -> library, then reload so the row disappears
    public void moveToLibrary(Book book) {
        repo.moveToLibrary(book.getId(), ok -> loadBooks());
    }

    // Undo of the above
    public void moveBackToWishlist(Book book) {
        repo.moveToWishlist(book.getId(), ok -> loadBooks());
    }
}
