package com.servicerequest.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class HistoryEntry {

    private String entryId;
    private String requestId;
    private String changedBy;
    private String field;
    private String oldValue;
    private String newValue;

    @JsonFormat(pattern = "yyyy-MM-DD'T'HH:mm:ss")
    private LocalDateTime timesStamp;

    public HistoryEntry(String requestId, String entryId, String changedBy, String field, String oldValue, String newValue, LocalDateTime timesStamp) {
        this.requestId = requestId;
        this.entryId = entryId;
        this.changedBy = changedBy;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timesStamp = timesStamp;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public LocalDateTime getTimesStamp() {
        return timesStamp;
    }

    public void setTimesStamp(LocalDateTime timesStamp) {
        this.timesStamp = timesStamp;
    }
}
