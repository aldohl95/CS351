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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FR-6 through FR-10.
 *
 * Required tests:
 *   1. Valid status update
 *   2. Valid priority assignment
 *   3. Valid priority update
 *   4. Invalid status handling
 *   5. Invalid priority handling
 *   6. Update attempt on non-existent request
 */
@ExtendWith(MockitoExtension.class)
class RequestServiceUpdateTest {

    @Mock
    private RequestRepository repository;

    @InjectMocks
    private RequestService requestService;

    private ServiceRequest existingRequest;

    @BeforeEach
    void setUp() {
        // A pre-existing request in OPEN state with HIGH priority
        existingRequest = new ServiceRequest(
                "Printer not working",
                "Printer in main office is unresponsive.",
                "jane.smith",
                Priority.HIGH,
                "Hardware"
        );
    }

    // =========================================================================
    // TEST 1: Valid status update (FR-6)
    // =========================================================================

    @Test
    @DisplayName("updateStatus - valid: OPEN → IN_PROGRESS updates status and records history")
    void updateStatus_validValue_updatesStatusAndRecordsHistory() {
        // Arrange
        when(repository.findById(existingRequest.getRequestId()))
                .thenReturn(Optional.of(existingRequest));
        when(repository.save(any(ServiceRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        ServiceRequest result = requestService.updateStatus(
                existingRequest.getRequestId(), "IN_PROGRESS", "admin");

        // Assert — status changed
        assertThat(result.getStatus()).isEqualTo(Status.IN_PROGRESS);

        // Assert — history entry records old and new values
        boolean historyRecorded = result.getHistory().stream()
                .anyMatch(h -> h.getField().equals("status")
                        && h.getOldValue().equals("OPEN")
                        && h.getNewValue().equals("IN_PROGRESS")
                        && h.getChangedBy().equals("admin"));
        assertThat(historyRecorded).isTrue();

        verify(repository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    @DisplayName("updateStatus - valid: case-insensitive input is accepted")
    void updateStatus_lowercaseInput_isAccepted() {
        when(repository.findById(existingRequest.getRequestId()))
                .thenReturn(Optional.of(existingRequest));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // "resolved" in lowercase should still work
        ServiceRequest result = requestService.updateStatus(
                existingRequest.getRequestId(), "resolved", "admin");

        assertThat(result.getStatus()).isEqualTo(Status.RESOLVED);
    }

    @Test
    @DisplayName("updateStatus - valid: all four supported statuses are accepted (FR-6)")
    void updateStatus_allSupportedStatuses_areAccepted() {
        for (String status : new String[]{"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"}) {
            // Re-stub for each iteration
            ServiceRequest fresh = new ServiceRequest(
                    "Test", "Desc", "user", Priority.LOW, null);
            when(repository.findById(fresh.getRequestId()))
                    .thenReturn(Optional.of(fresh));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceRequest result = requestService.updateStatus(
                    fresh.getRequestId(), status, "admin");

            assertThat(result.getStatus()).isEqualTo(Status.valueOf(status));
        }
    }

    // =========================================================================
    // TEST 2: Valid priority assignment — first time (FR-7)
    // =========================================================================

    @Test
    @DisplayName("updatePriority - valid assignment: sets priority and records history (FR-7)")
    void updatePriority_firstAssignment_setsPriorityAndRecordsHistory() {
        // Arrange — request with no priority set
        ServiceRequest noPriorityRequest = new ServiceRequest(
                "WiFi down", "No internet access.", "bob.jones", null, null);
        when(repository.findById(noPriorityRequest.getRequestId()))
                .thenReturn(Optional.of(noPriorityRequest));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ServiceRequest result = requestService.updatePriority(
                noPriorityRequest.getRequestId(), "HIGH", "admin");

        // Assert — priority assigned
        assertThat(result.getPriority()).isEqualTo(Priority.HIGH);

        // Assert — history shows NONE → HIGH
        boolean historyRecorded = result.getHistory().stream()
                .anyMatch(h -> h.getField().equals("priority")
                        && h.getOldValue().equals("NONE")
                        && h.getNewValue().equals("HIGH"));
        assertThat(historyRecorded).isTrue();
    }

    // =========================================================================
    // TEST 3: Valid priority update — changing existing priority (FR-8)
    // =========================================================================

    @Test
    @DisplayName("updatePriority - valid update: HIGH → LOW changes priority and records history (FR-8)")
    void updatePriority_changingExistingPriority_updatesAndRecordsHistory() {
        // existingRequest already has HIGH priority from setUp()
        when(repository.findById(existingRequest.getRequestId()))
                .thenReturn(Optional.of(existingRequest));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ServiceRequest result = requestService.updatePriority(
                existingRequest.getRequestId(), "LOW", "admin");

        // Assert — priority changed
        assertThat(result.getPriority()).isEqualTo(Priority.LOW);

        // Assert — history records the change from HIGH to LOW
        boolean historyRecorded = result.getHistory().stream()
                .anyMatch(h -> h.getField().equals("priority")
                        && h.getOldValue().equals("HIGH")
                        && h.getNewValue().equals("LOW"));
        assertThat(historyRecorded).isTrue();
    }

    @Test
    @DisplayName("updatePriority - valid: all three supported priorities are accepted (FR-7)")
    void updatePriority_allSupportedPriorities_areAccepted() {
        for (String priority : new String[]{"LOW", "MEDIUM", "HIGH"}) {
            ServiceRequest fresh = new ServiceRequest(
                    "Test", "Desc", "user", null, null);
            when(repository.findById(fresh.getRequestId()))
                    .thenReturn(Optional.of(fresh));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceRequest result = requestService.updatePriority(
                    fresh.getRequestId(), priority, "admin");

            assertThat(result.getPriority()).isEqualTo(Priority.valueOf(priority));
        }
    }

    // =========================================================================
    // TEST 4: Invalid status handling (FR-9)
    // =========================================================================

    @Test
    @DisplayName("updateStatus - invalid: unrecognised value throws ValidationException (FR-9)")
    void updateStatus_unrecognisedValue_throwsValidationException() {
        assertThatThrownBy(() -> requestService.updateStatus(
                existingRequest.getRequestId(), "PENDING", "admin"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid status")
                .hasMessageContaining("PENDING");

        // Repository must never be reached when validation fails
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus - invalid: null status throws ValidationException (FR-9)")
    void updateStatus_nullValue_throwsValidationException() {
        assertThatThrownBy(() -> requestService.updateStatus(
                existingRequest.getRequestId(), null, "admin"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Status must not be blank");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus - invalid: blank status throws ValidationException (FR-9)")
    void updateStatus_blankValue_throwsValidationException() {
        assertThatThrownBy(() -> requestService.updateStatus(
                existingRequest.getRequestId(), "   ", "admin"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Status must not be blank");

        verify(repository, never()).save(any());
    }

    // =========================================================================
    // TEST 5: Invalid priority handling (FR-9)
    // =========================================================================

    @Test
    @DisplayName("updatePriority - invalid: unrecognised value throws ValidationException (FR-9)")
    void updatePriority_unrecognisedValue_throwsValidationException() {
        assertThatThrownBy(() -> requestService.updatePriority(
                existingRequest.getRequestId(), "URGENT", "admin"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid priority")
                .hasMessageContaining("URGENT");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("updatePriority - invalid: null priority throws ValidationException (FR-9)")
    void updatePriority_nullValue_throwsValidationException() {
        assertThatThrownBy(() -> requestService.updatePriority(
                existingRequest.getRequestId(), null, "admin"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Priority must not be blank");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("updatePriority - invalid: blank priority throws ValidationException (FR-9)")
    void updatePriority_blankValue_throwsValidationException() {
        assertThatThrownBy(() -> requestService.updatePriority(
                existingRequest.getRequestId(), "   ", "admin"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Priority must not be blank");

        verify(repository, never()).save(any());
    }

    // =========================================================================
    // TEST 6: Update attempt on non-existent request (FR-10)
    // =========================================================================

    @Test
    @DisplayName("updateStatus - non-existent ID throws RequestNotFoundException (FR-10)")
    void updateStatus_nonExistentId_throwsRequestNotFoundException() {
        String badId = "does-not-exist";
        when(repository.findById(badId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.updateStatus(badId, "IN_PROGRESS", "admin"))
                .isInstanceOf(RequestNotFoundException.class)
                .hasMessageContaining(badId);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("updatePriority - non-existent ID throws RequestNotFoundException (FR-10)")
    void updatePriority_nonExistentId_throwsRequestNotFoundException() {
        String badId = "does-not-exist";
        when(repository.findById(badId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.updatePriority(badId, "HIGH", "admin"))
                .isInstanceOf(RequestNotFoundException.class)
                .hasMessageContaining(badId);

        verify(repository, never()).save(any());
    }
}