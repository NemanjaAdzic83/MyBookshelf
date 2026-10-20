package com.personallibrary.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * Room Entity — tabela "books".
 * v4.1: uklonjeno polje rating (RatingBar uklonjen iz aplikacije).
 * Ostala polja: id, title, author, cover, genre, inWishlist, dateAdded.
 */
@Entity(tableName = "books")
public class Book {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "author_first_name")
    private String authorFirstName;

    @ColumnInfo(name = "author_last_name", index = true)
    private String authorLastName;

    @ColumnInfo(name = "cover_image_path")
    private String coverImagePath;

    @ColumnInfo(name = "in_wishlist", index = true)
    private boolean inWishlist;

    @ColumnInfo(name = "date_added")
    private long dateAdded;

    @ColumnInfo(name = "genre")
    private String genre;

    // da li je knjiga procitana ili ne
    @ColumnInfo(name = "is_read", defaultValue = "0")
    private boolean read;

    public Book() {}

    public int getId()                         { return id; }
    public void setId(int id)                  { this.id = id; }
    public String getTitle()                   { return title; }
    public void setTitle(String t)             { title = t; }
    public String getAuthorFirstName()         { return authorFirstName; }
    public void setAuthorFirstName(String n)   { authorFirstName = n; }
    public String getAuthorLastName()          { return authorLastName; }
    public void setAuthorLastName(String n)    { authorLastName = n; }
    public String getCoverImagePath()          { return coverImagePath; }
    public void setCoverImagePath(String p)    { coverImagePath = p; }
    public boolean isInWishlist()              { return inWishlist; }
    public void setInWishlist(boolean w)       { inWishlist = w; }
    public long getDateAdded()                 { return dateAdded; }
    public void setDateAdded(long d)           { dateAdded = d; }
    public String getGenre()                   { return genre; }
    public void setGenre(String g)             { genre = g; }
    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getFullAuthorName() {
        if (authorFirstName == null || authorFirstName.isEmpty())
            return authorLastName != null ? authorLastName : "";
        return authorFirstName + " " + authorLastName;
    }

    public String getAuthorLastFirst() {
        if (authorFirstName == null || authorFirstName.isEmpty())
            return authorLastName != null ? authorLastName : "";
        return authorLastName + ", " + authorFirstName;
    }

    public boolean hasGenre() { return genre != null && !genre.isEmpty(); }
}
