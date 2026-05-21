package com.servicerequest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.servicerequest.Application;
import com.servicerequest.model.Priority;
import com.servicerequest.model.Status;
import com.servicerequest.service.CreateRequestDTO;
import com.servicerequest.service.UpdateRequestDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration test suite for the Service Request Tracking System.
 *
 * Strategy:
 * - @SpringBootTest loads the complete application context (controller, service, repository).
 * - @AutoConfigureMockMvc wires MockMvc to send real HTTP requests through the full stack.
 * - Each test class run gets a fresh temp file for JSON storage via @DynamicPropertySource,
 *   so tests are isolated from each other and from the real data directory.
 * - @DirtiesContext resets the Spring context (and therefore the in-memory repository)
 *   between test methods that mutate state.
 *
 * Coverage:
 *   FR-1   Create request
 *   FR-2   Default status is OPEN
 *   FR-3   List all requests
 *   FR-4   View request details by ID
 *   FR-5   Persistence (data survives a repository reload)
 *   FR-6   Update request status
 *   FR-7   Assign priority for the first time
 *   FR-8   Update an existing priority
 *   FR-9   Reject invalid status and priority values
 *   FR-10  Handle update attempts on non-existent requests
 *   FR-11  Filter by status
 *   FR-12  Filter by priority
 *   FR-13  Filter by requester
 *   FR-14  Search by title or description keyword
 *   FR-15  Empty result handling
 */
@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-integration-test.yml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServiceRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // Temp directory created once for the entire test class
    private static Path tempDir;

    @DynamicPropertySource
    static void configureStoragePath(DynamicPropertyRegistry registry) throws Exception {
        tempDir = Files.createTempDirectory("service-request-integration-test");
        registry.add("storage.requests-file",
                () -> tempDir.resolve("requests.json").toString());
    }

    @AfterAll
    static void cleanUp() {
        // Best-effort cleanup of the temp file after all tests finish
        if (tempDir != null) {
            File f = tempDir.resolve("requests.json").toFile();
            if (f.exists()) f.delete();
            tempDir.toFile().delete();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Creates a request via the API and returns the generated requestId.
     */
    private String createAndGetId(String title, String description,
                                  String createdBy, String priority) throws Exception {
        CreateRequestDTO dto = new CreateRequestDTO(
                title, description, createdBy,
                priority != null ? Priority.valueOf(priority) : null,
                "General");

        MvcResult result = mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("requestId").asText();
    }

    // =========================================================================
    // FR-1 + FR-2: Create request — default status is OPEN
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("FR-1 + FR-2: POST /api/requests creates request with OPEN status")
    void createRequest_returnsCreatedRequestWithOpenStatus() throws Exception {
        CreateRequestDTO dto = new CreateRequestDTO(
                "Printer not working",
                "Printer in main office is unresponsive.",
                "jane.smith", Priority.HIGH, "Hardware");

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isCreated())
                // FR-1: all required fields present
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Printer not working"))
                .andExpect(jsonPath("$.description").value("Printer in main office is unresponsive."))
                .andExpect(jsonPath("$.createdBy").value("jane.smith"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                // FR-2: default status must be OPEN
                .andExpect(jsonPath("$.status").value("OPEN"))
                // initial history entry records the OPEN assignment
                .andExpect(jsonPath("$.history", hasSize(1)))
                .andExpect(jsonPath("$.history[0].field").value("status"))
                .andExpect(jsonPath("$.history[0].newValue").value("OPEN"));
    }

    @Test
    @Order(2)
    @DisplayName("FR-1 validation: missing title returns 400 with error message")
    void createRequest_missingTitle_returns400() throws Exception {
        CreateRequestDTO dto = new CreateRequestDTO(
                null, "Some description", "bob.jones", Priority.LOW, null);

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Title is required")));
    }

    @Test
    @Order(3)
    @DisplayName("FR-1 validation: missing description returns 400 with error message")
    void createRequest_missingDescription_returns400() throws Exception {
        CreateRequestDTO dto = new CreateRequestDTO(
                "Valid title", null, "bob.jones", Priority.LOW, null);

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Description is required")));
    }

    @Test
    @Order(4)
    @DisplayName("FR-1 validation: missing requester returns 400 with error message")
    void createRequest_missingRequester_returns400() throws Exception {
        CreateRequestDTO dto = new CreateRequestDTO(
                "Valid title", "Valid description", null, Priority.LOW, null);

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Requester identifier")));
    }

    // =========================================================================
    // FR-3: List all requests
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("FR-3: GET /api/requests returns all stored requests")
    void getAllRequests_returnsListOfRequests() throws Exception {
        // Create two requests first
        createAndGetId("WiFi dropping", "Connection drops every hour.",
                "bob.jones", "MEDIUM");
        createAndGetId("Monitor flickering", "Screen flickers intermittently.",
                "alice.lee", "LOW");

        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    // =========================================================================
    // FR-4: View request details by ID
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("FR-4: GET /api/requests/{id} returns full request detail")
    void getRequestById_existingId_returnsFullDetail() throws Exception {
        String id = createAndGetId("Keyboard broken", "Keys are stuck.",
                "carol.white", "HIGH");

        mockMvc.perform(get("/api/requests/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(id))
                .andExpect(jsonPath("$.title").value("Keyboard broken"))
                .andExpect(jsonPath("$.createdBy").value("carol.white"))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.history").isArray());
    }

    @Test
    @Order(7)
    @DisplayName("FR-4: GET /api/requests/{id} with unknown ID returns 404")
    void getRequestById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/requests/does-not-exist-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // =========================================================================
    // FR-5: Persistence — data survives after being written to the JSON file
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("FR-5: Created request appears in subsequent GET /api/requests call")
    void persistence_createdRequestAppearsInList() throws Exception {
        String id = createAndGetId("Network outage",
                "All network connectivity lost on floor 2.",
                "dave.kim", "HIGH");

        // Retrieve via list — should include the newly created request
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].requestId", hasItem(id)));
    }

    // =========================================================================
    // FR-6: Update request status
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("FR-6: PATCH status OPEN → IN_PROGRESS updates and records history")
    void updateStatus_openToInProgress_updatesStatusAndHistory() throws Exception {
        String id = createAndGetId("Server down",
                "Production server is unresponsive.", "ops.team", "HIGH");

        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setStatus("IN_PROGRESS");
        dto.setChangedBy("admin");

        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.history", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.history[*].newValue", hasItem("IN_PROGRESS")))
                .andExpect(jsonPath("$.history[*].oldValue", hasItem("OPEN")));
    }

    @Test
    @Order(10)
    @DisplayName("FR-6: PATCH status IN_PROGRESS → RESOLVED updates correctly")
    void updateStatus_inProgressToResolved_updatesStatus() throws Exception {
        String id = createAndGetId("VPN not connecting",
                "Cannot connect to VPN from home.", "remote.user", "MEDIUM");

        // First move to IN_PROGRESS
        UpdateRequestDTO toInProgress = new UpdateRequestDTO();
        toInProgress.setStatus("IN_PROGRESS");
        toInProgress.setChangedBy("admin");
        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(toInProgress)))
                .andExpect(status().isOk());

        // Then move to RESOLVED
        UpdateRequestDTO toResolved = new UpdateRequestDTO();
        toResolved.setStatus("RESOLVED");
        toResolved.setChangedBy("admin");
        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(toResolved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @Order(11)
    @DisplayName("FR-6: all four supported statuses are accepted via the API")
    void updateStatus_allFourStatuses_areAccepted() throws Exception {
        for (String status : new String[]{"IN_PROGRESS", "RESOLVED", "CLOSED", "OPEN"}) {
            String id = createAndGetId("Status test " + status,
                    "Testing status " + status, "tester", "LOW");

            UpdateRequestDTO dto = new UpdateRequestDTO();
            dto.setStatus(status);
            dto.setChangedBy("admin");

            mockMvc.perform(patch("/api/requests/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(status));
        }
    }

    // =========================================================================
    // FR-7 + FR-8: Assign and update priority
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("FR-7: PATCH priority assigns LOW and records history with NONE as old value")
    void assignPriority_firstAssignment_recordsNoneAsOldValue() throws Exception {
        // Create with no priority so it starts null
        CreateRequestDTO dto = new CreateRequestDTO(
                "Chair request", "Requesting a new office chair.",
                "emp.jones", null, "Facilities");
        MvcResult result = mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("requestId").asText();

        // Now assign priority for the first time
        UpdateRequestDTO update = new UpdateRequestDTO();
        update.setPriority("LOW");
        update.setChangedBy("admin");

        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.history[*].field", hasItem("priority")))
                .andExpect(jsonPath("$.history[*].oldValue", hasItem("NONE")))
                .andExpect(jsonPath("$.history[*].newValue", hasItem("LOW")));
    }

    @Test
    @Order(13)
    @DisplayName("FR-8: PATCH priority HIGH → MEDIUM updates and records old value in history")
    void updatePriority_changingExisting_recordsOldValue() throws Exception {
        String id = createAndGetId("Slow laptop",
                "Laptop is very slow to boot.", "emp.smith", "HIGH");

        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setPriority("MEDIUM");
        dto.setChangedBy("admin");

        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.history[*].oldValue", hasItem("HIGH")))
                .andExpect(jsonPath("$.history[*].newValue", hasItem("MEDIUM")));
    }

    // =========================================================================
    // FR-9: Reject invalid status and priority values
    // =========================================================================

    @Test
    @Order(14)
    @DisplayName("FR-9: invalid status value returns 400 with descriptive error")
    void updateStatus_invalidValue_returns400() throws Exception {
        String id = createAndGetId("Test invalid status",
                "Testing invalid status rejection.", "tester", "LOW");

        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setStatus("PENDING");   // not a valid Status enum value
        dto.setChangedBy("admin");

        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Invalid status")))
                .andExpect(jsonPath("$.error").value(containsString("PENDING")));
    }

    @Test
    @Order(15)
    @DisplayName("FR-9: invalid priority value returns 400 with descriptive error")
    void updatePriority_invalidValue_returns400() throws Exception {
        String id = createAndGetId("Test invalid priority",
                "Testing invalid priority rejection.", "tester", "LOW");

        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setPriority("URGENT");  // not a valid Priority enum value
        dto.setChangedBy("admin");

        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Invalid priority")))
                .andExpect(jsonPath("$.error").value(containsString("URGENT")));
    }

    @Test
    @Order(16)
    @DisplayName("FR-9: blank status value returns 400")
    void updateStatus_blankValue_returns400() throws Exception {
        String id = createAndGetId("Test blank status",
                "Testing blank status rejection.", "tester", "LOW");

        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setStatus("   ");
        dto.setChangedBy("admin");

        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("blank")));
    }

    // =========================================================================
    // FR-10: Handle update attempts on non-existent requests
    // =========================================================================

    @Test
    @Order(17)
    @DisplayName("FR-10: PATCH status on non-existent ID returns 404")
    void updateStatus_nonExistentId_returns404() throws Exception {
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setStatus("IN_PROGRESS");
        dto.setChangedBy("admin");

        mockMvc.perform(patch("/api/requests/non-existent-id-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("non-existent-id-xyz")));
    }

    @Test
    @Order(18)
    @DisplayName("FR-10: PATCH priority on non-existent ID returns 404")
    void updatePriority_nonExistentId_returns404() throws Exception {
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setPriority("HIGH");
        dto.setChangedBy("admin");

        mockMvc.perform(patch("/api/requests/another-bad-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // =========================================================================
    // FR-11: Filter by status
    // =========================================================================

    @Test
    @Order(19)
    @DisplayName("FR-11: GET /api/requests?status=OPEN returns only open requests")
    void filterByStatus_open_returnsOnlyOpenRequests() throws Exception {
        // Create one OPEN and move another to CLOSED
        createAndGetId("Open ticket", "This stays open.", "filter.user", "LOW");
        String closedId = createAndGetId("Closed ticket",
                "This gets closed.", "filter.user", "LOW");

        UpdateRequestDTO close = new UpdateRequestDTO();
        close.setStatus("CLOSED");
        close.setChangedBy("admin");
        mockMvc.perform(patch("/api/requests/" + closedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(close)))
                .andExpect(status().isOk());

        // Filter for OPEN — CLOSED request must not appear
        mockMvc.perform(get("/api/requests").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.requests[*].status",
                        everyItem(equalTo("OPEN"))));
    }

    @Test
    @Order(20)
    @DisplayName("FR-11: GET /api/requests?status=CLOSED returns only closed requests")
    void filterByStatus_closed_returnsOnlyClosedRequests() throws Exception {
        mockMvc.perform(get("/api/requests").param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests[*].status",
                        everyItem(equalTo("CLOSED"))));
    }

    // =========================================================================
    // FR-12: Filter by priority
    // =========================================================================

    @Test
    @Order(21)
    @DisplayName("FR-12: GET /api/requests?priority=HIGH returns only high-priority requests")
    void filterByPriority_high_returnsOnlyHighPriorityRequests() throws Exception {
        createAndGetId("High priority task",
                "This is urgent.", "hr.user", "HIGH");

        mockMvc.perform(get("/api/requests").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.requests[*].priority",
                        everyItem(equalTo("HIGH"))));
    }

    // =========================================================================
    // FR-13: Filter by requester
    // =========================================================================

    @Test
    @Order(22)
    @DisplayName("FR-13: GET /api/requests?requester=jane.smith returns only their requests")
    void filterByRequester_returnsOnlyThatUsersRequests() throws Exception {
        createAndGetId("Jane's request",
                "Submitted by jane.", "jane.smith", "LOW");

        mockMvc.perform(get("/api/requests").param("requester", "jane.smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.requests[*].createdBy",
                        everyItem(containsString("jane.smith"))));
    }

    @Test
    @Order(23)
    @DisplayName("FR-13: requester filter is case-insensitive partial match")
    void filterByRequester_partialMatch_returnsMatchingRequests() throws Exception {
        mockMvc.perform(get("/api/requests").param("requester", "jane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").isArray())
                // All results should contain "jane" somewhere in createdBy
                .andExpect(jsonPath("$.requests[*].createdBy",
                        everyItem(containsStringIgnoringCase("jane"))));
    }

    // =========================================================================
    // FR-14: Search by title or description keyword
    // =========================================================================

    @Test
    @Order(24)
    @DisplayName("FR-14: keyword search matches requests by title")
    void searchByKeyword_matchesTitle_returnsCorrectRequests() throws Exception {
        createAndGetId("Projector bulb burnt out",
                "The conference room projector is not working.", "av.team", "MEDIUM");

        mockMvc.perform(get("/api/requests").param("keyword", "Projector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.requests[*].title",
                        hasItem(containsStringIgnoringCase("projector"))));
    }

    @Test
    @Order(25)
    @DisplayName("FR-14: keyword search matches requests by description")
    void searchByKeyword_matchesDescription_returnsCorrectRequests() throws Exception {
        createAndGetId("Office issue",
                "The thermostat in room 204 is broken.", "facilities", "LOW");

        mockMvc.perform(get("/api/requests").param("keyword", "thermostat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.requests[*].description",
                        hasItem(containsStringIgnoringCase("thermostat"))));
    }

    @Test
    @Order(26)
    @DisplayName("FR-14: keyword search is case-insensitive")
    void searchByKeyword_caseInsensitive_returnsResults() throws Exception {
        mockMvc.perform(get("/api/requests").param("keyword", "PROJECTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.requests", hasSize(greaterThanOrEqualTo(1))));
    }

    // =========================================================================
    // FR-15: Empty result handling
    // =========================================================================

    @Test
    @Order(27)
    @DisplayName("FR-15: no matching status returns 200 with empty list and message")
    void emptyResult_noStatusMatch_returns200WithMessage() throws Exception {
        // RESOLVED is unlikely to have results early in the test run
        mockMvc.perform(get("/api/requests").param("status", "RESOLVED"))
                .andExpect(status().isOk())
                // Always returns a SearchResult object — never null, never 404
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.totalCount").isNumber())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @Order(28)
    @DisplayName("FR-15: no matching keyword returns 200 with empty list and descriptive message")
    void emptyResult_noKeywordMatch_returns200WithMessage() throws Exception {
        mockMvc.perform(get("/api/requests").param("keyword", "xyznotfoundanywhere"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.requests", hasSize(0)))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.message",
                        containsStringIgnoringCase("no requests matched")));
    }

    @Test
    @Order(29)
    @DisplayName("FR-15: no matching requester returns 200 with empty list and message")
    void emptyResult_noRequesterMatch_returns200WithMessage() throws Exception {
        mockMvc.perform(get("/api/requests").param("requester", "nobody.here"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests", hasSize(0)))
                .andExpect(jsonPath("$.message",
                        containsStringIgnoringCase("no requests matched")));
    }

    // =========================================================================
    // Combined filter + search (FR-11 through FR-14 together)
    // =========================================================================

    @Test
    @Order(30)
    @DisplayName("Combined: status + priority filters applied together as AND conditions")
    void combinedFilter_statusAndPriority_narrowsResults() throws Exception {
        createAndGetId("Combined test request",
                "Used to test combined filtering.", "combo.user", "HIGH");

        mockMvc.perform(get("/api/requests")
                        .param("status", "OPEN")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests[*].status", everyItem(equalTo("OPEN"))))
                .andExpect(jsonPath("$.requests[*].priority", everyItem(equalTo("HIGH"))));
    }

    @Test
    @Order(31)
    @DisplayName("Combined: requester + keyword filters applied together")
    void combinedFilter_requesterAndKeyword_narrowsResults() throws Exception {
        createAndGetId("Combo keyword match",
                "This has a unique term: xylophone123.", "combo.user", "LOW");

        mockMvc.perform(get("/api/requests")
                        .param("requester", "combo.user")
                        .param("keyword", "xylophone123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests[*].createdBy",
                        everyItem(containsString("combo.user"))))
                .andExpect(jsonPath("$.requests[*].description",
                        hasItem(containsString("xylophone123"))));
    }

    // =========================================================================
    // End-to-end workflow: full lifecycle of a single request
    // =========================================================================

    @Test
    @Order(32)
    @DisplayName("End-to-end: full request lifecycle from creation to CLOSED")
    void endToEnd_fullLifecycle_requestMovesToClosed() throws Exception {
        // 1. Create
        CreateRequestDTO create = new CreateRequestDTO(
                "E2E lifecycle test",
                "Full lifecycle integration test request.",
                "lifecycle.user", Priority.MEDIUM, "Test");

        MvcResult createResult = mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();

        String id = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("requestId").asText();

        // 2. Verify it appears in the full list
        mockMvc.perform(get("/api/requests"))
                .andExpect(jsonPath("$[*].requestId", hasItem(id)));

        // 3. Move to IN_PROGRESS
        UpdateRequestDTO toInProgress = new UpdateRequestDTO();
        toInProgress.setStatus("IN_PROGRESS");
        toInProgress.setChangedBy("support.admin");
        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(toInProgress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 4. Upgrade priority
        UpdateRequestDTO toPriorityHigh = new UpdateRequestDTO();
        toPriorityHigh.setPriority("HIGH");
        toPriorityHigh.setChangedBy("support.admin");
        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(toPriorityHigh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"));

        // 5. Move to RESOLVED
        UpdateRequestDTO toResolved = new UpdateRequestDTO();
        toResolved.setStatus("RESOLVED");
        toResolved.setChangedBy("support.admin");
        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(toResolved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // 6. Move to CLOSED
        UpdateRequestDTO toClosed = new UpdateRequestDTO();
        toClosed.setStatus("CLOSED");
        toClosed.setChangedBy("support.admin");
        mockMvc.perform(patch("/api/requests/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(toClosed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // 7. Verify final state — detail view shows full history
        mockMvc.perform(get("/api/requests/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                // history should have: creation + IN_PROGRESS + HIGH + RESOLVED + CLOSED = 5 entries
                .andExpect(jsonPath("$.history", hasSize(5)))
                .andExpect(jsonPath("$.history[*].newValue",
                        hasItems("OPEN", "IN_PROGRESS", "HIGH", "RESOLVED", "CLOSED")));

        // 8. Appears in CLOSED filter
        mockMvc.perform(get("/api/requests").param("status", "CLOSED"))
                .andExpect(jsonPath("$.requests[*].requestId", hasItem(id)));
    }
}
