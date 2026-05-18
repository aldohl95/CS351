package com.servicerequest.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public class Comment {

    private String commentId;
    private String requestId;
    private String author;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd 'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public Comment(){}

    public Comment(String requestId, String author, String content){
        this.commentId = UUID.randomUUID().toString();
        this.requestId = requestId;
        this.author = author;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
