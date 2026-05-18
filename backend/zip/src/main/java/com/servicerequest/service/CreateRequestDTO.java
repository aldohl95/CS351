package com.servicerequest.service;

import com.servicerequest.model.Priority;

public class CreateRequestDTO {
    private String title;
    private String description;
    private String createdBy;
    private Priority priority;
    private String category;

    public CreateRequestDTO(String title, String description, String createdBy, Priority priority, String category) {
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.priority = priority;
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
}
