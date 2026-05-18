package com.servicerequest.service;

import com.servicerequest.exception.RequestNotFoundException;
import com.servicerequest.exception.ValidationException;
import com.servicerequest.model.Priority;
import com.servicerequest.model.ServiceRequest;
import com.servicerequest.model.Status;
import com.servicerequest.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository repository;

    @InjectMocks
    private RequestService requestService;

    // ── Shared test data ──────────────────────────────────────────────────────

    private CreateRequestDTO validDTO;

    @BeforeEach
    void setUp() {
        validDTO = new CreateRequestDTO(
                "Printer not working",
                "The printer in the main office is unresponsive.",
                "jane.smith",
                Priority.HIGH,
                "Hardware"
        );
    }

    // =========================================================================
    // TEST 1: Successful request creation (FR-1)
    // =========================================================================

    @Test
    @DisplayName("createRequest - success: saved request contains all provided fields")
    void createRequest_success_savedRequestContainsAllFields() {
        // Arrange: repository returns whatever is passed to save()
        when(repository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ServiceRequest result = requestService.createRequest(validDTO);

        // Assert — all FR-1 required fields are present
        assertThat(result.getRequestId()).isNotNull().isNotBlank();
        assertThat(result.getTitle()).isEqualTo("Printer not working");
        assertThat(result.getDescription()).isEqualTo("The printer in the main office is unresponsive.");
        assertThat(result.getCreatedBy()).isEqualTo("jane.smith");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getPriority()).isEqualTo(Priority.HIGH);

        // Verify repository was called once
        verify(repository, times(1)).save(any(ServiceRequest.class));
    }

    // =========================================================================
    // TEST 2a: Missing title throws ValidationException (FR-1 validation)
    // =========================================================================

    @Test
    @DisplayName("createRequest - failure: null title throws ValidationException")
    void createRequest_nullTitle_throwsValidationException() {
        // Arrange
        CreateRequestDTO dto = new CreateRequestDTO(
                null,                          // missing title
                "Some description",
                "jane.smith",
                Priority.MEDIUM,
                null
        );

        // Act & Assert
        assertThatThrownBy(() -> requestService.createRequest(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Title is required");

        // Repository must never be reached when validation fails
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("createRequest - failure: blank title throws ValidationException")
    void createRequest_blankTitle_throwsValidationException() {
        CreateRequestDTO dto = new CreateRequestDTO(
                "   ",                         // blank title
                "Some description",
                "jane.smith",
                Priority.MEDIUM,
                null
        );

        assertThatThrownBy(() -> requestService.createRequest(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Title is required");

        verify(repository, never()).save(any());
    }

    // =========================================================================
    // TEST 2b: Missing description throws ValidationException
    // =========================================================================

    @Test
    @DisplayName("createRequest - failure: null description throws ValidationException")
    void createRequest_nullDescription_throwsValidationException() {
        CreateRequestDTO dto = new CreateRequestDTO(
                "Printer not working",
                null,                          // missing description
                "jane.smith",
                Priority.MEDIUM,
                null
        );

        assertThatThrownBy(() -> requestService.createRequest(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Description is required");

        verify(repository, never()).save(any());
    }

    // =========================================================================
    // TEST 2c: Missing createdBy (requester) throws ValidationException
    // =========================================================================

    @Test
    @DisplayName("createRequest - failure: null createdBy throws ValidationException")
    void createRequest_nullCreatedBy_throwsValidationException() {
        CreateRequestDTO dto = new CreateRequestDTO(
                "Printer not working",
                "The printer is broken.",
                null,                          // missing requester
                Priority.MEDIUM,
                null
        );

        assertThatThrownBy(() -> requestService.createRequest(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Requester identifier");

        verify(repository, never()).save(any());
    }

    // =========================================================================
    // TEST 3: Default status is OPEN (FR-2)
    // =========================================================================

    @Test
    @DisplayName("createRequest - default status is OPEN (FR-2)")
    void createRequest_defaultStatus_isOpen() {
        // Arrange
        when(repository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ServiceRequest result = requestService.createRequest(validDTO);

        // Assert — status must be OPEN regardless of what the caller provides
        assertThat(result.getStatus()).isEqualTo(Status.OPEN);
    }

    @Test
    @DisplayName("createRequest - initial history entry records OPEN status")
    void createRequest_historyEntry_recordsOpenStatus() {
        when(repository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequest result = requestService.createRequest(validDTO);

        // There should be exactly one history entry logging the OPEN status
        assertThat(result.getHistory()).hasSize(1);
        assertThat(result.getHistory().get(0).getField()).isEqualTo("status");
        assertThat(result.getHistory().get(0).getNewValue()).isEqualTo("OPEN");
        assertThat(result.getHistory().get(0).getOldValue()).isNull();
    }

    // =========================================================================
    // TEST 4: Retrieve all requests (FR-3)
    // =========================================================================

    @Test
    @DisplayName("getAllRequests - returns all requests from repository")
    void getAllRequests_returnsAllRequestsFromRepository() {
        // Arrange: stub with two sample requests
        ServiceRequest r1 = new ServiceRequest("Broken keyboard", "Keys stuck", "alice", null, null);
        ServiceRequest r2 = new ServiceRequest("No internet",    "WiFi down",  "bob",   null, null);
        when(repository.findAll()).thenReturn(List.of(r1, r2));

        // Act
        List<ServiceRequest> result = requestService.getAllRequests();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(r1, r2);
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllRequests - returns empty list when no requests exist")
    void getAllRequests_emptyRepository_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<ServiceRequest> result = requestService.getAllRequests();

        assertThat(result).isEmpty();
        verify(repository, times(1)).findAll();
    }

    // =========================================================================
    // TEST 5: Retrieve one request by ID (FR-4)
    // =========================================================================

    @Test
    @DisplayName("getRequestById - returns correct request when ID exists")
    void getRequestById_existingId_returnsRequest() {
        // Arrange
        ServiceRequest request = new ServiceRequest(
                "Monitor flickering", "Screen flickers intermittently.", "carol", Priority.MEDIUM, "Hardware");
        String id = request.getRequestId();
        when(repository.findById(id)).thenReturn(Optional.of(request));

        // Act
        ServiceRequest result = requestService.getRequestById(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo(id);
        assertThat(result.getTitle()).isEqualTo("Monitor flickering");
        verify(repository, times(1)).findById(id);
    }

    @Test
    @DisplayName("getRequestById - throws RequestNotFoundException for unknown ID")
    void getRequestById_unknownId_throwsRequestNotFoundException() {
        // Arrange
        String unknownId = "non-existent-uuid";
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> requestService.getRequestById(unknownId))
                .isInstanceOf(RequestNotFoundException.class)
                .hasMessageContaining(unknownId);

        verify(repository, times(1)).findById(unknownId);
    }
}