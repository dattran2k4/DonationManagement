package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EActivityStatus;
import com.chiaseyeuthuong.dto.request.ActivityRequest;
import com.chiaseyeuthuong.dto.response.ActivityResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.model.Activity;
import com.chiaseyeuthuong.model.Donation;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ActivityService {
    PageResponse<ActivityResponse> getAllActivities(int page, int size, String search, EActivityStatus status);

    List<ActivityResponse> getAllActivitiesByEventId(Long eventId);

    void saveActivity(ActivityRequest request);

    Activity getActivity(Long id);

    ActivityResponse getActivityById(Long id);

    ActivityResponse getActivityBySlug(String slug);

    void updateCurrentAmount(Activity activity, BigDecimal amount);

    String saveThumbnailUrl(Long id, MultipartFile file);
}
