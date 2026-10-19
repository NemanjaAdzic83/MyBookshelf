package com.personallibrary.models;

public class BookInfo {
    private String isbn, title, authorFirstName, authorLastName;
    private String coverImageUrl, publisher, publishedDate, description;

    public BookInfo() {}

    public String getIsbn()            { return isbn; }           public void setIsbn(String s)           { isbn = s; }
    public String getTitle()           { return title; }          public void setTitle(String s)          { title = s; }
    public String getAuthorFirstName() { return authorFirstName; } public void setAuthorFirstName(String s) { authorFirstName = s; }
    public String getAuthorLastName()  { return authorLastName; }  public void setAuthorLastName(String s)  { authorLastName = s; }
    public String getCoverImageUrl()   { return coverImageUrl; }   public void setCoverImageUrl(String s)   { coverImageUrl = s; }
    public String getPublisher()       { return publisher; }       public void setPublisher(String s)       { publisher = s; }
    public String getPublishedDate()   { return publishedDate; }   public void setPublishedDate(String s)   { publishedDate = s; }
    public String getDescription()     { return description; }     public void setDescription(String s)     { description = s; }

    public String getFullAuthorName() {
        if (authorFirstName == null || authorFirstName.isEmpty()) return authorLastName != null ? authorLastName : "";
        if (authorLastName  == null || authorLastName.isEmpty())  return authorFirstName;
        return authorFirstName + " " + authorLastName;
    }

    public Book toBook(boolean inWishlist) {
        Book b = new Book();
        b.setTitle(title != null ? title : "Unknown title");
        b.setAuthorFirstName(authorFirstName != null ? authorFirstName : "");
        b.setAuthorLastName(authorLastName != null ? authorLastName : "Unknown author");
        b.setCoverImagePath(coverImageUrl);
        b.setInWishlist(inWishlist);
        return b;
    }
}
