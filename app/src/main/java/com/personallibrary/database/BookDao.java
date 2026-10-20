package com.personallibrary.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.personallibrary.models.Book;

import java.util.List;

@Dao
public interface BookDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertBook(Book book);

    @Update
    void updateBook(Book book);

    @Delete
    void deleteBook(Book book);

    @Query("SELECT * FROM books WHERE in_wishlist = 0 ORDER BY date_added DESC")
    List<Book> getRecentLibraryBooks();

    @Query("SELECT * FROM books WHERE in_wishlist = 0 ORDER BY author_last_name ASC, author_first_name ASC")
    List<Book> getAllLibraryBooksSortedByAuthor();

    @Query("SELECT * FROM books WHERE in_wishlist = 0 AND genre = :genre ORDER BY author_last_name ASC")
    List<Book> getLibraryBooksByGenre(String genre);

    // [ISPRAVKA L1] dodano "in_wishlist = 0": žanr koji postoji samo kod knjiga sa liste želja
    // više ne pravi chip koji pri kliku prikazuje praznu biblioteku.
    @Query("SELECT DISTINCT genre FROM books WHERE in_wishlist = 0 AND genre IS NOT NULL AND genre != '' ORDER BY genre ASC")
    List<String> getAllGenres();

    @Query("SELECT * FROM books WHERE in_wishlist = 1 ORDER BY date_added DESC")
    List<Book> getWishlistBooks();

    // [ISPRAVKA L3] Zamjena za searchBooks / findByTitleAndAuthor / countInLibrary / countAnywhere.
    // SQLite LOWER() i LIKE pouzdano rade samo za ASCII slova, pa Č, Ć, Š, Đ, Ž nisu poređena
    // ispravno. Filtriranje i poređenje se sada radi u Javi (String.equalsIgnoreCase / Normalizer),
    // a upit samo vraća sve knjige.
    @Query("SELECT * FROM books ORDER BY author_last_name ASC, title ASC")
    List<Book> getAllBooks();

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    Book getBookById(int id);

    @Query("UPDATE books SET in_wishlist = 0, date_added = :timestamp WHERE id = :id")
    void moveToLibrary(int id, long timestamp);

    // Reverse of moveToLibrary: a book added to the library by mistake goes back to the wishlist
    // date_added is refreshed so it appears at the top of  the wishlist
    @Query("UPDATE books SET in_wishlist=1, date_added= :timestamp WHERE id = :id")
    void moveToWishlist(int id, long timestamp);

    @Query("SELECT COUNT(*) FROM books WHERE in_wishlist = 0")
    int getLibraryCount();

    @Query("SELECT COUNT(*) FROM books WHERE in_wishlist = 1")
    int getWishlistCount();
}
