package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.request.EventRequest;
import com.chiaseyeuthuong.dto.response.EventResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.model.Event;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface EventService {

    PageResponse<EventResponse> getAllEvents(int page, int size, String sortBy, String sortDir, String search, EEventStatus status, String... categoryIds);

    long saveEvent(EventRequest request);

    EventResponse getEventById(Long id);

    Event findEventById(Long id);

    EventResponse getEventBySlug(String slug);

    void updateStatus(EEventStatus status, Long id);

    long getEventCount(EEventStatus status);

    void updateEventCurrentAmount(Event event, BigDecimal amount);

    String saveThumbnailUrl(Long id, MultipartFile file);
}
