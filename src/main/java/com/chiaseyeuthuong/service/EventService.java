package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.request.EventRequest;
import com.chiaseyeuthuong.dto.response.ActivityResponse;
import com.chiaseyeuthuong.dto.response.DonorResponse;
import com.chiaseyeuthuong.dto.response.EventResponse;
import com.chiaseyeuthuong.dto.response.EventDetailTabsSummaryResponse;
import com.chiaseyeuthuong.dto.response.DonationResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.model.Event;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface EventService {

    PageResponse<EventResponse> getAllEvents(int page, int size, String sortBy, String sortDir, String search, EEventStatus status, boolean excludeDraft, String... categoryIds);

    long createEvent(EventRequest request);

    long updateEvent(Long id, EventRequest request);

    EventResponse getEventById(Long id);

    Event findEventById(Long id);

    EventResponse getEventBySlug(String slug);

    EventResponse getPublicEventBySlug(String slug);

    long getEventCount(EEventStatus status);

    int syncStatusesBySchedule();

    void updateEventCurrentAmount(Event event, BigDecimal amount);

    String saveThumbnailUrl(Long id, MultipartFile file);

    EventDetailTabsSummaryResponse getEventDetailTabsSummary(Long eventId);

    PageResponse<ActivityResponse> getEventDetailActivities(Long eventId, int page, int size);

    PageResponse<DonorResponse> getEventDetailDonors(Long eventId, int page, int size);

    PageResponse<DonationResponse> getEventDetailDonations(Long eventId, int page, int size);
}
