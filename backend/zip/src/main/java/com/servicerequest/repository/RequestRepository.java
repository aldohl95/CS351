package com.servicerequest.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.servicerequest.model.Priority;
import com.servicerequest.model.ServiceRequest;
import com.servicerequest.model.Status;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RequestRepository {
    @Value("${storage.requests-file:data/requests.json}")
    private String filePath;

    private final ObjectMapper objectMapper;

    private List<ServiceRequest> requests = new ArrayList<>();

    public RequestRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.findAndRegisterModules();
    }

    @PostConstruct
    public void load(){
        File file = new File(filePath);
        if(!file.exists()){
            requests = new ArrayList<>();
            return;
        }
        try{
            requests = objectMapper.readValue(file, new TypeReference<List<ServiceRequest>>() {});
        } catch(IOException e){
            throw new RuntimeException("Fialed to load requests from file" + filePath, e);
        }
    }

    public ServiceRequest save(ServiceRequest request){
        requests.removeIf(r-> r.getRequestId().equals(request.getRequestId()));
        requests.add(request);
        flush();
        return request;
    }

    public void deleteById(String requestId){
        requests.removeIf(r -> r.getRequestId().equals(requestId));
        flush();
    }

    public List<ServiceRequest> findAll(){
        return Collections.unmodifiableList(requests);
    }

    public Optional<ServiceRequest> findById(String requestId){
        return requests.stream().filter(r -> r.getRequestId().equals(requestId)).findFirst();
    }

    public List<ServiceRequest> findByStatus(Status status){
        return requests.stream().filter(r -> r.getStatus() == status).collect(Collectors.toList());
    }

    public List<ServiceRequest> findByPriority(Priority priority){
        return requests.stream().filter(r -> r.getPriority() == priority).collect(Collectors.toList());
    }

    public List<ServiceRequest> findByRequester(String requester){
        if(requester == null || requester.trim().isEmpty()){
            return List.of();
        }
        String term = requester.trim().toLowerCase();
        return requests.stream().filter(r -> r.getCreatedBy() != null
                && r.getCreatedBy().toLowerCase().contains(term)).collect(Collectors.toList());
    }

    public List<ServiceRequest> findByKeyword(String keyword){
        if(keyword == null || keyword.trim().isEmpty()){
            return List.of();
        }
        String term = keyword.trim().toLowerCase();
        return requests.stream().filter(r -> containsIgnoreCase(r.getTitle(), term) ||
                containsIgnoreCase(r.getDescription(),term)
                ).collect(Collectors.toList());
    }


    private boolean isBlank(String value){
        return value == null || value.trim().isEmpty();
    }

    private boolean containsIgnoreCase(String field, String term){
        return field != null && field.toLowerCase().contains(term);
    }

    private void flush(){
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, requests);
        } catch (IOException e){
            throw new RuntimeException("failed to persist requests to " + filePath, e);
        }
    }

}
