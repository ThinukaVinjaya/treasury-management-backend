package com.batch.treasury_management.controller;

import com.batch.treasury_management.dto.EventRequest;
import com.batch.treasury_management.dto.EventResponse;
import com.batch.treasury_management.dto.EventSummaryDTO;
import com.batch.treasury_management.repository.EventRepository;
import com.batch.treasury_management.repository.UserRepository;
import com.batch.treasury_management.response.ApiResponse;
import com.batch.treasury_management.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/treasurer/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@RequestBody EventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.ok(ApiResponse.success("Event created successfully", response));
    }

    @PutMapping("/{eventId}/temporary-treasurer")
    public ResponseEntity<ApiResponse<EventResponse>> assignTemporaryTreasurer(
            @PathVariable String eventId, @RequestParam String username) {

        EventResponse response = eventService.assignTemporaryTreasurer(eventId, username);
        return ResponseEntity.ok(ApiResponse.success("Temporary treasurer assigned successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<EventResponse> events = eventService.getAllEventsPaginated(pageable);
        return ResponseEntity.ok(ApiResponse.success("Events fetched successfully", events));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponse<String>> softDeleteEvent(
            @PathVariable String eventId, Authentication authentication) {

        String deletedBy = authentication.getName();
        eventService.softDeleteEvent(eventId, deletedBy);
        return ResponseEntity.ok(ApiResponse.success("Event soft deleted successfully"));
    }

    /** Full Event Summary */
    @GetMapping("/{eventId}/summary")
    public ResponseEntity<ApiResponse<EventSummaryDTO>> getEventSummary(@PathVariable String eventId) {
        return ResponseEntity.ok(ApiResponse.success("Event summary fetched",
                eventService.getEventSummary(eventId)));
    }

    /** Full Financial Report */
    @GetMapping("/{eventId}/report")
    public ResponseEntity<byte[]> generateEventReport(
            @PathVariable String eventId, Authentication authentication) {

        if (!isAuthorizedForEvent(eventId, authentication.getName())) {
            return ResponseEntity.status(403).build();
        }

        byte[] pdf = eventService.generateEventReport(eventId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "event-report-" + eventId + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    /** ✅ Unpaid / Contribution Status Report - All Members */
    @GetMapping("/{eventId}/contribution-report")
    public ResponseEntity<byte[]> generateContributionReport(
            @PathVariable String eventId, Authentication authentication) {

        if (!isAuthorizedForEvent(eventId, authentication.getName())) {
            return ResponseEntity.status(403).build();
        }

        byte[] pdf = eventService.generateEventContributionReport(eventId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "contribution-status-" + eventId + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // Permission Check
    private boolean isAuthorizedForEvent(String eventId, String username) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return false;

        String role = userOpt.get().getRole();
        if ("SUPER_ADMIN".equals(role) || "TREASURER".equals(role)) {
            return true;
        }

        // Check if Temporary Treasurer for this event
        return eventRepository.existsByIdAndTemporaryTreasurerIdAndIsDeletedFalse(eventId, userOpt.get().getId());
    }
}