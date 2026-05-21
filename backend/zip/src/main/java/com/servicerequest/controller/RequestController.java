package com.servicerequest.controller;

import com.servicerequest.exception.RequestNotFoundException;
import com.servicerequest.exception.ValidationException;
import com.servicerequest.model.ServiceRequest;
import com.servicerequest.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;



@RestController
@RequestMapping("/api/requests")
@CrossOrigin (origins = "*")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService){
        this.requestService = requestService;
    }

    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody CreateRequestDTO dto){
        try {
            ServiceRequest created = requestService.createRequest(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch(ValidationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error",ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getRequests(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String priority,
                                         @RequestParam(required = false)String requester,
                                         @RequestParam(required = false)String keyword){
        boolean hasFilters = status != null || priority != null || requester != null || keyword != null;

        if(!hasFilters){
            return ResponseEntity.ok(requestService.getAllRequests());
        }

        SearchCriteria criteria = new SearchCriteria(status, priority, requester, keyword);
        SearchResult result = requestService.searchRequests(criteria);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(@PathVariable("id") String requestId){
        try {
            ServiceRequest request = requestService.getRequestById(requestId);
            return ResponseEntity.ok(request);
        } catch (RequestNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateRequest(@PathVariable("id") String requestId, @RequestBody UpdateRequestDTO dto){
        try{
            ServiceRequest result = null;
            String changedBy = dto.getChangedBy() != null ? dto.getChangedBy() : "TBD";

            if (dto.getStatus() != null) {
                result = requestService.updateStatus(requestId, dto.getStatus(), changedBy);
            }

            if(dto.getPriority() != null) {
                result = requestService.updatePriority(requestId, dto.getPriority(), changedBy);
            }

            if(result == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No Status or Priority provided"));
            }

            return ResponseEntity.ok(result);
        }catch(ValidationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }catch(RequestNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

}
