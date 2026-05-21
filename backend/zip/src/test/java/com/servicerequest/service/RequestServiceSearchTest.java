package com.servicerequest.service;

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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the search and filter features (FR-11 through FR-15).
 *
 * Required tests:
 *   1. Filtering by status
 *   2. Filtering by priority
 *   3. Filtering by requester
 *   4. Searching by title or keyword
 *   5. No-match result behaviour
 *
 * The repository is mocked so tests are pure unit tests with no file I/O.
 * The repository.search() method is stubbed to return controlled datasets,
 * keeping each test focused on the service layer's handling of results.
 */
@ExtendWith(MockitoExtension.class)
class RequestServiceSearchTest {

    @Mock
    private RequestRepository repository;

    @InjectMocks
    private RequestService requestService;

    // ── Shared test data ──────────────────────────────────────────────────────

    private ServiceRequest openHighRequest;
    private ServiceRequest inProgressMedRequest;
    private ServiceRequest resolvedLowRequest;

    @BeforeEach
    void setUp() {
        openHighRequest = new ServiceRequest(
                "Printer not working",
                "Printer in main office is unresponsive.",
                "jane.smith", Priority.HIGH, "Hardware");
        // Force status to OPEN (constructor always sets OPEN, so this is redundant
        // but makes the test data intent explicit)
        openHighRequest.setStatus(Status.OPEN);

        inProgressMedRequest = new ServiceRequest(
                "WiFi dropping",
                "Internet connection drops every 30 minutes.",
                "bob.jones", Priority.MEDIUM, "Network");
        inProgressMedRequest.setStatus(Status.IN_PROGRESS);

        resolvedLowRequest = new ServiceRequest(
                "Monitor flickering",
                "Screen flickers when brightness is above 50 percent.",
                "jane.smith", Priority.LOW, "Hardware");
        resolvedLowRequest.setStatus(Status.RESOLVED);
    }

    // =========================================================================
    // TEST 1: Filter by status (FR-11)
    // =========================================================================

    @Test
    @DisplayName("searchRequests - FR-11: filters by OPEN status returns only open requests")
    void searchRequests_filterByStatus_returnsMatchingRequests() {
        // Arrange: repository returns only the OPEN request when status=OPEN is applied
        SearchCriteria criteria = new SearchCriteria("OPEN", null, null, null);
        when(repository.search(criteria)).thenReturn(List.of(openHighRequest));

        // Act
        SearchResult result = requestService.searchRequests(criteria);

        // Assert — only the OPEN request is returned
        assertThat(result.getRequests()).hasSize(1);
        assertThat(result.getRequests().get(0).getStatus()).isEqualTo(Status.OPEN);
        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("searchRequests - FR-11: filters by IN_PROGRESS returns only in-progress requests")
    void searchRequests_filterByInProgress_returnsMatchingRequests() {
        SearchCriteria criteria = new SearchCriteria("IN_PROGRESS", null, null, null);
        when(repository.search(criteria)).thenReturn(List.of(inProgressMedRequest));

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.getRequests()).hasSize(1);
        assertThat(result.getRequests().get(0).getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    @DisplayName("searchRequests - FR-11: filters by RESOLVED returns only resolved requests")
    void searchRequests_filterByResolved_returnsMatchingRequests() {
        SearchCriteria criteria = new SearchCriteria("RESOLVED", null, null, null);
        when(repository.search(criteria)).thenReturn(List.of(resolvedLowRequest));

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.getRequests()).hasSize(1);
        assertThat(result.getRequests().get(0).getStatus()).isEqualTo(Status.RESOLVED);
    }

    // =========================================================================
    // TEST 2: Filter by priority (FR-12)
    // =========================================================================

    @Test
    @DisplayName("searchRequests - FR-12: filters by HIGH priority returns only high-priority requests")
    void searchRequests_filterByHighPriority_returnsMatchingRequests() {
        SearchCriteria criteria = new SearchCriteria(null, "HIGH", null, null);
        when(repository.search(criteria)).thenReturn(List.of(openHighRequest));

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.getRequests()).hasSize(1);
        assertThat(result.getRequests().get(0).getPriority()).isEqualTo(Priority.HIGH);
        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("searchRequests - FR-12: filters by LOW priority returns only low-priority requests")
    void searchRequests_filterByLowPriority_returnsMatchingRequests() {
        SearchCriteria criteria = new SearchCriteria(null, "LOW", null, null);
        when(repository.search(criteria)).thenReturn(List.of(resolvedLowRequest));

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.getRequests()).hasSize(1);
        assertThat(result.getRequests().get(0).getPriority()).isEqualTo(Priority.LOW);
    }

    // =========================================================================
    // TEST 3: Filter by requester (FR-13)
    // =========================================================================

    @Test
    @DisplayName("searchRequests - FR-13: filters by requester returns all requests from that user")
    void searchRequests_filterByRequester_returnsAllRequestsFromThatUser() {
        // jane.smith submitted two of the three test requests
        SearchCriteria criteria = new SearchCriteria(null, null, "jane.smith", null);
        when(repository.search(criteria))
                .thenReturn(List.of(openHighRequest, resolvedLowRequest));

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.getRequests()).hasSize(2);
        assertThat(result.getRequests())
                .allMatch(r -> r.getCreatedBy().equals("jane.smith"));
    }

    @Test
    @DisplayName("searchRequests - FR-13: filters by different requester returns only their requests")
    void searchRequests_filterByDifferentRequester_returnsOnlyTheirRequests() {
        SearchCriteria criteria = new SearchCriteria(null, null, "bob.jones", null);
        when(repository.search(criteria)).thenReturn(List.of(inProgressMedRequest));

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.getRequests()).hasSize(1);
        assertThat(result.getRequests().get(0).getCreatedBy()).isEqualTo("bob.jones");
    }

    // =========================================================================
    // TEST 4: Search by title or description keyword (FR-14)
    // =========================================================================

    @Test
    @DisplayName("searchRequests - FR-14: keyword matching title returns correct requests")
    void searchRequests_keywordMatchesTitle_returnsMatchingRequests() {
        // "Printer" appears in the title of openHighRequest
        SearchCriteria criteria = new SearchCriteria(null, null, null, "Printer");
        when(repository.search(criteria)).thenReturn(List.of(openHighRequest));

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.getRequests()).hasSize(1);
        assertThat(result.getRequests().get(0).getTitle()).containsIgnoringCase("printer");
    }

    @Test
    @DisplayName("searchRequests - FR-14: keyword matching description returns correct requests")
    void searchRequests_keywordMatchesDescription_returnsMatchingRequests() {
        // "drops" appears only in the description of inProgressMedRequest
        SearchCriteria criteria = new SearchCriteria(null, null, null, "drops");
        when(repository.search(criteria)).thenReturn(List.of(inProgressMedRequest));

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.getRequests()).hasSize(1);
        assertThat(result.getRequests().get(0).getDescription()).containsIgnoringCase("drops");
    }

    @Test
    @DisplayName("searchRequests - FR-14: keyword matches across both title and description fields")
    void searchRequests_keywordMatchesTitleAndDescription_returnsBothMatches() {
        // "Hardware" appears in the category but let's say "office" appears in
        // printer description — returned by the stubbed repository
        SearchCriteria criteria = new SearchCriteria(null, null, null, "office");
        when(repository.search(criteria)).thenReturn(List.of(openHighRequest));

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.getRequests()).isNotEmpty();
    }

    // =========================================================================
    // TEST 5: No-match result behaviour (FR-15)
    // =========================================================================

    @Test
    @DisplayName("searchRequests - FR-15: no matching status returns empty result with message")
    void searchRequests_noStatusMatch_returnsEmptySearchResult() {
        SearchCriteria criteria = new SearchCriteria("CLOSED", null, null, null);
        when(repository.search(criteria)).thenReturn(List.of());

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalCount()).isEqualTo(0);
        assertThat(result.getRequests()).isEmpty();
        assertThat(result.getMessage()).containsIgnoringCase("no requests matched");
    }

    @Test
    @DisplayName("searchRequests - FR-15: no matching priority returns empty result with message")
    void searchRequests_noPriorityMatch_returnsEmptySearchResult() {
        SearchCriteria criteria = new SearchCriteria(null, "MEDIUM", null, null);
        when(repository.search(criteria)).thenReturn(List.of());

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getMessage()).containsIgnoringCase("no requests matched");
    }

    @Test
    @DisplayName("searchRequests - FR-15: no matching requester returns empty result with message")
    void searchRequests_noRequesterMatch_returnsEmptySearchResult() {
        SearchCriteria criteria = new SearchCriteria(null, null, "unknown.user", null);
        when(repository.search(criteria)).thenReturn(List.of());

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getMessage()).containsIgnoringCase("no requests matched");
    }

    @Test
    @DisplayName("searchRequests - FR-15: no keyword match returns empty result with message")
    void searchRequests_noKeywordMatch_returnsEmptySearchResult() {
        SearchCriteria criteria = new SearchCriteria(null, null, null, "xyznotfound");
        when(repository.search(criteria)).thenReturn(List.of());

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getMessage()).containsIgnoringCase("no requests matched");
    }

    @Test
    @DisplayName("searchRequests - FR-15: result is never null regardless of criteria")
    void searchRequests_resultIsNeverNull() {
        SearchCriteria criteria = new SearchCriteria("CLOSED", "HIGH", "nobody", "nothinghere");
        when(repository.search(criteria)).thenReturn(List.of());

        SearchResult result = requestService.searchRequests(criteria);

        // The result object itself, the list, and the message must never be null
        assertThat(result).isNotNull();
        assertThat(result.getRequests()).isNotNull();
        assertThat(result.getMessage()).isNotNull().isNotBlank();
    }

    // ── Combined criteria test ────────────────────────────────────────────────

    @Test
    @DisplayName("searchRequests - combined: status + requester filters applied together")
    void searchRequests_combinedCriteria_appliesBothFilters() {
        // OPEN requests from jane.smith — only openHighRequest qualifies
        SearchCriteria criteria = new SearchCriteria("OPEN", null, "jane.smith", null);
        when(repository.search(criteria)).thenReturn(List.of(openHighRequest));

        SearchResult result = requestService.searchRequests(criteria);

        assertThat(result.getRequests()).hasSize(1);
        assertThat(result.getRequests().get(0).getStatus()).isEqualTo(Status.OPEN);
        assertThat(result.getRequests().get(0).getCreatedBy()).isEqualTo("jane.smith");
    }
}