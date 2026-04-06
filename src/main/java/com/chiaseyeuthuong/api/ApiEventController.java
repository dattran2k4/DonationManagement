package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.request.EventRequest;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.ActivityService;
import com.chiaseyeuthuong.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-EVENT-CONTROLLER")
@RequestMapping("/api/events")
public class ApiEventController {

    private final EventService eventService;
    private final ActivityService activityService;

    @GetMapping
    public ApiResponse getAllEvents(@RequestParam(required = false, defaultValue = "0") int page,
                                    @RequestParam(required = false, defaultValue = "10") int size,
                                    @RequestParam(required = false) String sortBy,
                                    @RequestParam(required = false) String sortDir,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) EEventStatus status,
                                    @RequestParam(required = false, defaultValue = "false") boolean excludeDraft,
                                    @RequestParam(required = false) String... categoryIds) {
        return ApiResponse.builder()
                .status(200)
                .message("List events")
                .data(eventService.getAllEvents(page, size, sortBy, sortDir, search, status, excludeDraft, categoryIds))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse getEventById(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Get event by id")
                .data(eventService.getEventById(id))
                .build();
    }

    @GetMapping("/{id}/activities")
    public ApiResponse getAllActivitiesByEventId(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Get activities by event id")
                .data(activityService.getAllActivitiesByEventId(id))
                .build();
    }

    @GetMapping("/{id}/detail-tabs/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getEventDetailTabsSummary(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy tổng quan tab chi tiết sự kiện thành công")
                .data(eventService.getEventDetailTabsSummary(id))
                .build();
    }

    @GetMapping("/{id}/detail-tabs/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getEventDetailActivities(@PathVariable Long id,
                                                @RequestParam(required = false, defaultValue = "1") int page,
                                                @RequestParam(required = false, defaultValue = "10") int size) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách hoạt động theo sự kiện thành công")
                .data(eventService.getEventDetailActivities(id, page, size))
                .build();
    }

    @GetMapping("/{id}/detail-tabs/donors")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getEventDetailDonors(@PathVariable Long id,
                                            @RequestParam(required = false, defaultValue = "1") int page,
                                            @RequestParam(required = false, defaultValue = "10") int size) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách nhà hảo tâm theo sự kiện thành công")
                .data(eventService.getEventDetailDonors(id, page, size))
                .build();
    }

    @GetMapping("/{id}/detail-tabs/donations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getEventDetailDonations(@PathVariable Long id,
                                               @RequestParam(required = false, defaultValue = "1") int page,
                                               @RequestParam(required = false, defaultValue = "10") int size) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách quyên góp theo sự kiện thành công")
                .data(eventService.getEventDetailDonations(id, page, size))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse createEvent(@RequestBody @Valid EventRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Tạo mới sự kiện thành công")
                .data(eventService.createEvent(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse updateEvent(@Min(1) @PathVariable Long id, @RequestBody @Valid EventRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Cập nhật sự kiện thành công")
                .data(eventService.updateEvent(id, request))
                .build();
    }

    @PostMapping({"/upload", "/{id}/upload"})
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public ApiResponse uploadThumbnail(@PathVariable(required = false) Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.builder()
                .status(201)
                .message("Successfully updated thumbnail event")
                .data(eventService.saveThumbnailUrl(id, file))
                .build();
    }
}
