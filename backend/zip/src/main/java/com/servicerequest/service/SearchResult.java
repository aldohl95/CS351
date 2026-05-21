package com.servicerequest.service;

import com.servicerequest.model.ServiceRequest;

import java.util.List;

public class SearchResult {

    private final List<ServiceRequest> requests;
    private final int totalCount;
    private final String message;
    private final SearchCriteria appliedCriteria;

    public SearchResult(List<ServiceRequest> requests, SearchCriteria appliedCriteria){
        this.requests = requests;
        this.totalCount = requests.size();
        this.appliedCriteria = appliedCriteria;
        this.message = requests.isEmpty() ? "no requests matched" : totalCount + " Requests found";
    }

    public List<ServiceRequest> getRequests() {
        return requests;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public String getMessage() {
        return message;
    }

    public SearchCriteria getAppliedCriteria() {
        return appliedCriteria;
    }

    public boolean isEmpty() {
        return requests.isEmpty();
    }



}
