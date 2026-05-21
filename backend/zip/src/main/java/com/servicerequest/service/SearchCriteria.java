package com.servicerequest.service;

public class SearchCriteria {

    private String status;
    private String priority;
    private String requester;
    private String keyword;

    public SearchCriteria(){}

    public SearchCriteria(String status, String priority, String requester, String keyword) {
        this.status = status;
        this.priority = priority;
        this.requester = requester;
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public boolean isEmpty(){
        return isBlank(status) && isBlank(priority) && isBlank(requester) && isBlank(keyword);
    }

    private boolean isBlank(String value){
        return value == null || value.trim().isEmpty();
    }
}
