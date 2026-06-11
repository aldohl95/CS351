package com.servicerequest.service;

public class AddCommentDTO {

    private String content;
    private String author;

    public AddCommentDTO() {}

    public String getContent() {
        return content;
    }

    public void setContent(String conetent) {
        this.content = conetent;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
