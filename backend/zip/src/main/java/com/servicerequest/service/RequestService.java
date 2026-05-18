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

    public ServiceRequest updateSatus(String requestId, Status newStatus, String changedBy){
        ServiceRequest request = getRequestById(requestId);

        String oldStatus = request.getStatus().name();
        request.setStatus(newStatus);
        request.setUpdatedAt(LocalDateTime.now());

        request.getHistory().add(new HistoryEntry(
                requestId, changedBy, "Status", oldStatus, newStatus.name()
        ));
        return repository.save(request);
    }

    public ServiceRequest updatePriority(String requestId, Priority newPriority, String changedBy){
        ServiceRequest request = getRequestById(requestId);

        String oldPriority = request.getPriority() != null ? request.getPriority().name() : "NONE";
        request.setPriority(newPriority);
        request.setUpdatedAt(LocalDateTime.now());

        request.getHistory().add(new HistoryEntry(
                requestId, changedBy, "priority", oldPriority, newPriority.name()
        ));

        return repository.save(request);
    }

    private void ValidateCreateDTO(CreateRequestDTO dto){
        if (dto == null){
            throw new ValidationException("Request body cannot be null");
        }
        if(isBlank(dto.getTitle())){
            throw new ValidationException("Request title cannot be blank");
        }
        if(isBlank(dto.getDescription())){
            throw new ValidationException("Request description cannot be blank");
        }
        if(isBlank(dto.getCreatedBy())){
            throw new ValidationException("Request created by cannot be blank");
        }
    }

    private boolean isBlank(String value){
        return value == null || value.trim().isEmpty();
    }



}
