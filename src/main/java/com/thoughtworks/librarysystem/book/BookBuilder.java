package com.thoughtworks.librarysystem.book;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Wither;

@AllArgsConstructor
@RequiredArgsConstructor
@Wither
public class BookBuilder {

    private String author;
    private String title;

    public Book build(){
        Book book = new Book();
        book.setAuthor(author);
        book.setTitle(title);

        return book;
    }
}