package com.servicerequest.controller;

import com.servicerequest.exception.RequestNotFoundException;
import com.servicerequest.exception.ValidationException;
import com.servicerequest.model.ServiceRequest;
import com.servicerequest.service.CreateRequestDTO;
import com.servicerequest.service.RequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static tools.jackson.databind.type.LogicalType.Map;

@RestController
@RequestMapping
@CrossOrigin (origins = "*")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService){
        this.requestService = requestservice;
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
    public ResponseEntity<List<ServiceRequest>> getAllRequests(){
        return ResponseEntity.ok(requestService.getallRequests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(@PathVariable("id") String requestId){
        try {
            ServiceRequest request = requestService.getRequestById(requestId);
            return ResponseEntity.ok(request);
        } catch (RequestNotFoundException ex) {
            return ResponseEntity.status(HttpSatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

}
