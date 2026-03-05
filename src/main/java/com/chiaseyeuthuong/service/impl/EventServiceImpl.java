package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.request.EventRequest;
import com.chiaseyeuthuong.dto.response.ActivityResponse;
import com.chiaseyeuthuong.dto.response.CategoryResponse;
import com.chiaseyeuthuong.dto.response.EventResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.Category;
import com.chiaseyeuthuong.model.Event;
import com.chiaseyeuthuong.repository.CategoryRepository;
import com.chiaseyeuthuong.repository.EventRepository;
import com.chiaseyeuthuong.service.DonorService;
import com.chiaseyeuthuong.service.EventService;
import com.chiaseyeuthuong.service.EventSpecification;
import com.github.slugify.Slugify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EVENT-SERVICE")
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;

    private final DonorService donorService;

    @Override
    public PageResponse<EventResponse> getAllEvents(int page, int size, String sortBy, String sortDir, String search, EEventStatus status, String... categoryIds) {
        int pageNumber = (page > 0) ? page - 1 : 0;

        Sort.Direction sortDirection = ("asc".equals(sortDir)) ? Sort.Direction.ASC : Sort.Direction.DESC;

        String sortField = StringUtils.hasLength(sortBy) ? sortBy : "id";

        PageRequest pageRequest = PageRequest.of(pageNumber, size, Sort.by(sortDirection, sortField));

        Specification<Event> specification = EventSpecification.filterEvent(search, status, categoryIds);

        Page<Event> eventPage = eventRepository.findAll(specification, pageRequest);

        List<EventResponse> eventResponses = eventPage.getContent().stream().map(this::toResponse).toList();

        return PageResponse.<EventResponse>builder()
                .page(page)
                .pageSize(size)
                .totalItems(eventPage.getTotalElements())
                .totalPages(eventPage.getTotalPages())
                .data(eventResponses)
                .build();
    }


    @Override
    public long saveEvent(EventRequest request) {
        log.info("Processing saving event: ");
        Event event = (request.getId() != null) ? findEventById(request.getId()) : new Event();

        Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        event.setCategory(category);

        BeanUtils.copyProperties(request, event);
        Slugify slugify = Slugify.builder().build();
        String slug = slugify.slugify(request.getName());
        event.setSlug(slug);

        Event result = eventRepository.save(event);
        log.info("Saved event: {} ", result.getId());

        return result.getId();
    }

    @Override
    public EventResponse getEventById(Long id) {
        return toResponse(findEventById(id));
    }

    @Override
    public Event findEventById(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    @Override
    public EventResponse getEventBySlug(String slug) {
        Event event = eventRepository.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return toResponse(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(EEventStatus status, Long id) {
        Event event = findEventById(id);
        event.setStatus(status);

        if (status.equals(EEventStatus.COMPLETED)) {
            event.setCompletedAt(LocalDateTime.now());
        }

        eventRepository.save(event);
        log.info("Updated status event {} to: {} ", event.getId(), status);
    }

    @Override
    public long getEventCount(EEventStatus status) {
        if (status == null) {
            return eventRepository.count();
        }
        return eventRepository.countByStatus(status);
    }

    @Override
    public void updateEventCurrentAmount(Event event, BigDecimal amount) {
        BigDecimal newCurrentAmount = event.getCurrentAmount().add(amount);
        event.setCurrentAmount(newCurrentAmount);
        eventRepository.save(event);

        log.info("Updated current amount event {} to: {} ", event.getId(), newCurrentAmount);
    }

    private EventResponse toResponse(Event event) {
        EventResponse eventResponse = new EventResponse();
        BeanUtils.copyProperties(event, eventResponse);
        eventResponse.setNumberOfDonors(donorService.getDorCountByObjectId(event.getId(), EEntityType.EVENT));
        eventResponse.setCategoryId(event.getCategory().getId());

        CategoryResponse categoryResponse = new CategoryResponse();
        BeanUtils.copyProperties(event.getCategory(), categoryResponse);
        eventResponse.setCategory(categoryResponse);

        List<ActivityResponse> activities = event.getActivities()
                .stream()
                .map(activity -> {
                    ActivityResponse ar = new ActivityResponse();
                    BeanUtils.copyProperties(activity, ar);
                    return ar;
                })
                .toList();

        eventResponse.setActivities(activities);

        return eventResponse;
    }
}
