package com.hogwai.rowtransformers.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Book {

    @Id
    @GeneratedValue
    private Long id;

    private String title;
    private int pages;

    @ManyToOne
    private Author author;

    public Book() {}

    public Book(String title, int pages, Author author) {
        this.title = title;
        this.pages = pages;
        this.author = author;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = pages; }

    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
}
