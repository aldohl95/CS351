package com.servicerequest.service;

import com.servicerequest.exception.RequestNotFoundException;
import com.servicerequest.exception.ValidationException;
import com.servicerequest.model.HistoryEntry;
import com.servicerequest.model.Priority;
import com.servicerequest.model.ServiceRequest;
import com.servicerequest.model.Status;
import com.servicerequest.repository.RequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;



@Service
public class RequestService {

    private final RequestRepository repository;

    public RequestService(RequestRepository repository) {
        this.repository = repository;
    }


    public ServiceRequest createRequest(CreateRequestDTO dto){
        ValidateCreateDTO(dto);

        ServiceRequest request = new ServiceRequest(
                dto.getTitle().trim(),
                dto.getDescription().trim(),
                dto.getCreatedBy().trim(),
                dto.getPriority(),
                dto.getCategory()
        );

        request.getHistory().add(new HistoryEntry(
                request.getRequestId(),
                request.getCreatedBy(),
                "status",
                null,
                Status.OPEN.name()
        ));

        return repository.save(request);

    }

    public List<ServiceRequest> getAllRequests(){
        return repository.findAll();
    }

    public ServiceRequest getRequestById(String requestId){
        return repository.findById(requestId).orElseThrow(()-> new RequestNotFoundException(requestId));
    }

    public ServiceRequest updateStatus(String requestId, String rawStatus, String changedBy){
        Status newStatus = parseStatus(rawStatus);
        ServiceRequest request = getRequestById(requestId);

        String oldStatus = request.getStatus().name();
        request.setStatus(newStatus);
        request.setUpdatedAt(LocalDateTime.now());

        request.getHistory().add(new HistoryEntry(
                requestId, changedBy, "status", oldStatus, newStatus.name()
        ));
        return repository.save(request);
    }

    public ServiceRequest updatePriority(String requestId, String rawPriority, String changedBy){
        Priority newPriority = parsePriority(rawPriority);
        ServiceRequest request = getRequestById(requestId);

        String oldPriority = request.getPriority() != null ? request.getPriority().name() : "NONE";
        request.setPriority(newPriority);
        request.setUpdatedAt(LocalDateTime.now());

        request.getHistory().add(new HistoryEntry(
                requestId, changedBy, "priority", oldPriority, newPriority.name()
        ));

        return repository.save(request);
    }

    public SearchResult searchResults(SearchCriteria criteria){
        List<ServiceRequest> results = repository.search(criteria);
        return new SearchResult(results, criteria);
    }

    private Status parseStatus(String raw){
        if (raw == null || raw.trim().isEmpty()){
            throw new ValidationException("Status must not be blank");
        }
        try{
            return Status.valueOf(raw.trim().toUpperCase());
        }catch (IllegalArgumentException e){
            throw new ValidationException("Invalid status ' " + raw.trim() + "'.Accepted values: OPEN, IN_PROGRESS, RESOLVED, "
                    + "CLOSED.");
        }
    }

    public Priority parsePriority(String raw){
        if (raw == null || raw.trim().isEmpty()){
            throw new ValidationException("Priority must not be blank");
        }
        try{
            return Priority.valueOf(raw.trim().toUpperCase());
        }catch (IllegalArgumentException e){
            throw new ValidationException("Invalid priority '" + raw.trim() + "'. Accepted values: LOW, MEDIUM, HIGH.");
        }
    }

    private void ValidateCreateDTO(CreateRequestDTO dto){
        if (dto == null){
            throw new ValidationException("Request body cannot be null");
        }
        if(isBlank(dto.getTitle())){
            throw new ValidationException("Title is required");
        }
        if(isBlank(dto.getDescription())){
            throw new ValidationException("Description is required");
        }
        if(isBlank(dto.getCreatedBy())){
            throw new ValidationException("Requester identifier is required");
        }
    }

    private boolean isBlank(String value){
        return value == null || value.trim().isEmpty();
    }



}
