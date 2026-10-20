package com.personallibrary.viewmodel;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.personallibrary.models.Book;
import com.personallibrary.repository.BookRepository;
public class BookDetailViewModel extends AndroidViewModel {
    private final BookRepository repo;
    private final MutableLiveData<Book>    book      = new MutableLiveData<>();
    private final MutableLiveData<Boolean> opSuccess = new MutableLiveData<>();
    public BookDetailViewModel(@NonNull Application app) { super(app); repo = new BookRepository(app); }
    public LiveData<Book>    getBook()      { return book; }
    public LiveData<Boolean> getOpSuccess() { return opSuccess; }
    public void loadBook(int id) { repo.getBookById(id, b -> book.setValue(b)); }
    public void moveToLibrary()  {
        Book b = book.getValue();
        if (b==null) return;
        repo.moveToLibrary(b.getId(), ok -> opSuccess.setValue(ok));
    }

    public void moveToWishlist(){
        Book b = book.getValue();
        if (b == null) return;
        repo.moveToWishlist(b.getId(), ok -> opSuccess.setValue(ok));
    }
    public void deleteBook()     { Book b = book.getValue(); if (b==null) return; repo.deleteBook(b.getId(),
            ok -> opSuccess.setValue(ok));
    }

    public void toggleRead(){
        Book b = book.getValue();
        if (b == null) return;
        repo.setRead(b.getId(), !b.isRead(), ok -> loadBook(b.getId()));
    }
}
