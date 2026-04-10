package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EActivityStatus;
import com.chiaseyeuthuong.dto.request.ActivityRequest;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-ACTIVITY-CONTROLLER")
@RequestMapping("/api/activities")
public class ApiActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ApiResponse getAllActivities(@RequestParam(required = false, defaultValue = "0") int page,
                                        @RequestParam(required = false, defaultValue = "20") int size,
                                        @RequestParam(required = false) String search,
                                        @RequestParam(required = false) EActivityStatus status,
                                        @RequestParam(required = false, defaultValue = "false") boolean excludeDraft) {
        return ApiResponse.builder()
                .status(200)
                .message("OK")
                .data(activityService.getAllActivities(page, size, search, status, excludeDraft))
                .build();
    }

    @GetMapping("/view-activities")
    public ApiResponse getAllActivitiesByEventId(@RequestParam Long eventId) {
        return ApiResponse.builder()
                .status(200)
                .message("OK")
                .data(activityService.getAllActivitiesByEventId(eventId))
                .build();
    }

    @GetMapping("/{id}/detail")
    public ApiResponse getActivityById(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("OK")
                .data(activityService.getActivityById(id))
                .build();
    }

    @GetMapping("/{id}/detail-tabs/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getActivityDetailTabsSummary(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy tổng quan tab chi tiết hoạt động thành công")
                .data(activityService.getActivityDetailTabsSummary(id))
                .build();
    }

    @GetMapping("/{id}/detail-tabs/donors")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getActivityDetailDonors(@PathVariable Long id,
                                               @RequestParam(required = false, defaultValue = "1") int page,
                                               @RequestParam(required = false, defaultValue = "10") int size) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách nhà hảo tâm theo hoạt động thành công")
                .data(activityService.getActivityDetailDonors(id, page, size))
                .build();
    }

    @GetMapping("/{id}/detail-tabs/donations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getActivityDetailDonations(@PathVariable Long id,
                                                  @RequestParam(required = false, defaultValue = "1") int page,
                                                  @RequestParam(required = false, defaultValue = "10") int size) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách quyên góp theo hoạt động thành công")
                .data(activityService.getActivityDetailDonations(id, page, size))
                .build();
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public ApiResponse saveActivity(@RequestBody @Valid ActivityRequest request) {
        Long activityId = activityService.saveActivity(request);
        return ApiResponse.builder()
                .status(200)
                .message("Successfully saved activity")
                .data(activityId)
                .build();
    }

    @PostMapping({"/upload", "/{id}/upload"})
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public ApiResponse uploadThumbnail(@PathVariable(required = false) Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.builder()
                .status(201)
                .message("Successfully updated thumbnail event")
                .data(activityService.saveThumbnailUrl(id, file))
                .build();
    }
}
