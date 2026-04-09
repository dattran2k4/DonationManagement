package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.request.EventRequest;
import com.chiaseyeuthuong.dto.response.ActivityResponse;
import com.chiaseyeuthuong.dto.response.CategoryResponse;
import com.chiaseyeuthuong.dto.response.DonorResponse;
import com.chiaseyeuthuong.dto.response.DonationResponse;
import com.chiaseyeuthuong.dto.response.EventDetailTabsSummaryResponse;
import com.chiaseyeuthuong.dto.response.EventResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.exception.BusinessException;
import com.chiaseyeuthuong.model.Category;
import com.chiaseyeuthuong.model.Event;
import com.chiaseyeuthuong.repository.ActivityRepository;
import com.chiaseyeuthuong.repository.AuditLogRepository;
import com.chiaseyeuthuong.repository.CategoryRepository;
import com.chiaseyeuthuong.repository.EventRepository;
import com.chiaseyeuthuong.repository.DonationRepository;
import com.chiaseyeuthuong.service.AuditLogService;
import com.chiaseyeuthuong.service.ActivityService;
import com.chiaseyeuthuong.service.DonationService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EVENT-SERVICE")
public class EventServiceImpl implements EventService {
    private static final String EVENT_NOT_FOUND_MESSAGE = "Không tìm thấy sự kiện";
    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Không tìm thấy danh mục";
    private static final String END_DATE_BEFORE_START_DATE_MESSAGE = "Thời gian kết thúc sự kiện không được trước thời gian bắt đầu";
    private static final String UPCOMING_START_DATE_IN_PAST_MESSAGE = "Sự kiện sắp diễn ra không được có thời gian bắt đầu trong quá khứ";
    private static final String ONGOING_START_DATE_INVALID_MESSAGE = "Sự kiện đang diễn ra phải có thời gian bắt đầu trong hiện tại hoặc quá khứ";
    private static final String ONGOING_END_DATE_INVALID_MESSAGE = "Sự kiện đang diễn ra phải có thời gian kết thúc trong hiện tại hoặc tương lai";
    private static final String COMPLETED_START_DATE_INVALID_MESSAGE = "Sự kiện đã hoàn thành không được có thời gian bắt đầu trong tương lai";
    private static final String COMPLETED_END_DATE_INVALID_MESSAGE = "Sự kiện đã hoàn thành không được có thời gian kết thúc trong tương lai";
    private static final String EVENT_UPDATE_WINDOW_EXPIRED_MESSAGE = "Sự kiện chỉ được cập nhật trong vòng 3 ngày sau khi kết thúc";
    private static final String INVALID_EVENT_STATUS_MESSAGE = "Trạng thái sự kiện không hợp lệ";

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final ActivityRepository activityRepository;
    private final DonationRepository donationRepository;
    private final AuditLogRepository auditLogRepository;

    private final DonorService donorService;
    private final ActivityService activityService;
    private final DonationService donationService;
    private final AuditLogService auditLogService;

    public static final String UPLOAD_DIR = "uploads/thumbnails/";

    @Override
    public PageResponse<EventResponse> getAllEvents(int page, int size, String sortBy, String sortDir, String search, EEventStatus status, boolean excludeDraft, String... categoryIds) {
        int pageNumber = (page > 0) ? page - 1 : 0;

        Sort.Direction sortDirection = ("asc".equals(sortDir)) ? Sort.Direction.ASC : Sort.Direction.DESC;

        String sortField = StringUtils.hasLength(sortBy) ? sortBy : "id";

        PageRequest pageRequest = PageRequest.of(pageNumber, size, Sort.by(sortDirection, sortField));

        Specification<Event> specification = EventSpecification.filterEvent(search, status, excludeDraft, categoryIds);

        Page<Event> eventPage = eventRepository.findAll(specification, pageRequest);

        List<EventResponse> eventResponses = eventPage.getContent().stream()
                .map(event -> toResponse(event, false))
                .toList();

        return PageResponse.<EventResponse>builder()
                .page(page)
                .pageSize(size)
                .totalItems(eventPage.getTotalElements())
                .totalPages(eventPage.getTotalPages())
                .data(eventResponses)
                .build();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createEvent(EventRequest request) {
        log.info("Processing creating event");

        if (request.getStatus() == null) {
            request.setStatus(EEventStatus.DRAFT);
        }
        validateEventSchedule(request);

        Event event = new Event();
        toEntity(event, request);

        Event result = eventRepository.save(event);
        log.info("Created event: {} ", result.getId());

        Map<String, Object> afterValues = buildEventAuditMap(result);
        auditLogService.logCreate(EEntityType.EVENT, result.getId(), "Tạo mới sự kiện", afterValues);
        return result.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long updateEvent(Long id, EventRequest request) {
        log.info("Processing updating event: {}", id);

        Event event = findEventById(id);
        validateEventUpdateWindow(event);

        if (request.getStatus() == null) {
            request.setStatus(event.getStatus());
        }
        validateEventSchedule(request);

        Map<String, Object> beforeValues = buildEventAuditMap(event);

        toEntity(event, request);

        Event result = eventRepository.save(event);
        log.info("Updated event: {} ", result.getId());

        auditLogService.logUpdate(EEntityType.EVENT, result.getId(),
                "Cập nhật sự kiện", beforeValues, buildEventAuditMap(result));
        return result.getId();
    }

    private void toEntity(Event event, EventRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND_MESSAGE));
        event.setCategory(category);

        BeanUtils.copyProperties(request, event);
        event.setCurrentAmount(defaultAmount(request.getCurrentAmount()));
        event.setTargetAmount(defaultAmount(request.getTargetAmount()));
        Slugify slugify = Slugify.builder().build();
        event.setSlug(slugify.slugify(request.getName()));
    }

    private void validateEventSchedule(EventRequest request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        EEventStatus status = request.getStatus();
        LocalDate today = LocalDate.now();

        if (startDate == null || endDate == null || status == null) {
            return;
        }

        if (endDate.isBefore(startDate)) {
            throw new BusinessException(END_DATE_BEFORE_START_DATE_MESSAGE);
        }

        switch (status) {
            case DRAFT -> log.info("Skip schedule validation for draft event");
            case UPCOMING -> validateUpcomingSchedule(startDate, today);
            case ONGOING -> validateOngoingSchedule(startDate, endDate, today);
            case COMPLETED -> validateCompletedSchedule(startDate, endDate, today);
            default -> throw new BusinessException(INVALID_EVENT_STATUS_MESSAGE);
        }
    }

    private void validateUpcomingSchedule(LocalDate startDate, LocalDate today) {
        if (startDate.isBefore(today)) {
            throw new BusinessException(UPCOMING_START_DATE_IN_PAST_MESSAGE);
        }
    }

    private void validateOngoingSchedule(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate.isAfter(today)) {
            throw new BusinessException(ONGOING_START_DATE_INVALID_MESSAGE);
        }
        if (endDate.isBefore(today)) {
            throw new BusinessException(ONGOING_END_DATE_INVALID_MESSAGE);
        }
    }

    private void validateCompletedSchedule(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate.isAfter(today)) {
            throw new BusinessException(COMPLETED_START_DATE_INVALID_MESSAGE);
        }
        if (endDate.isAfter(today)) {
            throw new BusinessException(COMPLETED_END_DATE_INVALID_MESSAGE);
        }
    }

    @Override
    public EventResponse getEventById(Long id) {
        return toResponse(findEventById(id));
    }

    @Override
    public Event findEventById(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND_MESSAGE));
    }

    @Override
    public EventResponse getEventBySlug(String slug) {
        Event event = eventRepository.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND_MESSAGE));
        return toResponse(event, false);
    }

    @Override
    public EventResponse getPublicEventBySlug(String slug) {
        Event event = eventRepository.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND_MESSAGE));
        if (EEventStatus.DRAFT.equals(event.getStatus())) {
            throw new ResourceNotFoundException(EVENT_NOT_FOUND_MESSAGE);
        }
        return toResponse(event, true);
    }

    @Override
    public long getEventCount(EEventStatus status) {
        if (status == null) {
            return eventRepository.count();
        }
        return eventRepository.countByStatus(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncStatusesBySchedule() {
        LocalDate today = LocalDate.now();
        List<Event> events = eventRepository.findAll();
        List<Event> changedEvents = new ArrayList<>();

        for (Event event : events) {
            EEventStatus currentStatus = event.getStatus();
            EEventStatus nextStatus = calculateStatusByDate(event, today);
            if (!Objects.equals(currentStatus, nextStatus)) {
                event.setStatus(nextStatus);
                if (EEventStatus.COMPLETED.equals(nextStatus) && event.getCompletedAt() == null) {
                    event.setCompletedAt(LocalDateTime.now());
                }
                changedEvents.add(event);
            }
        }

        if (!changedEvents.isEmpty()) {
            eventRepository.saveAll(changedEvents);
        }

        return changedEvents.size();
    }

    @Override
    public void updateEventCurrentAmount(Event event, BigDecimal amount) {
        BigDecimal newCurrentAmount = defaultAmount(event.getCurrentAmount()).add(amount);
        event.setCurrentAmount(newCurrentAmount);
        eventRepository.save(event);

        log.info("Updated current amount event {} to: {} ", event.getId(), newCurrentAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveThumbnailUrl(Long id, MultipartFile file) {
        try {
            Event event = null;
            if (id != null) {
                event = findEventById(id);
                validateEventUpdateWindow(event);
            }
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) directory.mkdirs();

            String safeName = Objects.requireNonNull(file.getOriginalFilename()).replace("\\s+", "_");

            String fileName = "%s_%s".formatted(UUID.randomUUID(), safeName);
            Path filePath = Paths.get(UPLOAD_DIR + fileName);

            //Lưu file vật lý
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/thumbnails/%s".formatted(fileName);

            if (event != null) {
                event.setThumbnailUrl(fileUrl);
                eventRepository.save(event);
                log.info("Saved thumbnail url event {} ", event.getId());
            } else {
                log.info("Saved thumbnail url without event reference");
            }

            return fileUrl;
        } catch (IOException e) {
            log.error("Cannot save thumbnail caused: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot save thumbnail url caused ", e);
        }
    }

    @Override
    public EventDetailTabsSummaryResponse getEventDetailTabsSummary(Long eventId) {
        findEventById(eventId);

        return EventDetailTabsSummaryResponse.builder()
                .activityCount(activityRepository.countByEventId(eventId))
                .donorCount(donorService.getDorCountByObjectId(eventId, EEntityType.EVENT))
                .donationCount(donationRepository.countByEventScopeId(eventId))
                .auditLogCount(auditLogRepository.countByEntityTypeAndEntityId(EEntityType.EVENT, eventId))
                .build();
    }

    @Override
    public PageResponse<ActivityResponse> getEventDetailActivities(Long eventId, int page, int size) {
        findEventById(eventId);
        return activityService.getActivitiesByEventId(eventId, page, size);
    }

    @Override
    public PageResponse<DonorResponse> getEventDetailDonors(Long eventId, int page, int size) {
        findEventById(eventId);
        return donorService.getDonorsByEventId(eventId, page, size);
    }

    @Override
    public PageResponse<DonationResponse> getEventDetailDonations(Long eventId, int page, int size) {
        findEventById(eventId);
        return donationService.getDonationsByEventId(eventId, page, size);
    }

    private EventResponse toResponse(Event event) {
        return toResponse(event, false);
    }

    private EventResponse toResponse(Event event, boolean excludeDraftActivities) {
        EventResponse eventResponse = new EventResponse();
        BeanUtils.copyProperties(event, eventResponse);
        eventResponse.setNumberOfDonors(donorService.getDorCountByObjectId(event.getId(), EEntityType.EVENT));
        eventResponse.setCategoryId(event.getCategory().getId());

        CategoryResponse categoryResponse = new CategoryResponse();
        BeanUtils.copyProperties(event.getCategory(), categoryResponse);
        eventResponse.setCategory(categoryResponse);

        List<ActivityResponse> activities = event.getActivities()
                .stream()
                .filter(activity -> !excludeDraftActivities || !com.chiaseyeuthuong.common.EActivityStatus.DRAFT.equals(activity.getStatus()))
                .map(activity -> {
                    ActivityResponse ar = new ActivityResponse();
                    BeanUtils.copyProperties(activity, ar);
                    return ar;
                })
                .toList();

        eventResponse.setActivities(activities);

        return eventResponse;
    }

    private Map<String, Object> buildEventAuditMap(Event event) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", event.getName());
        values.put("slug", event.getSlug());
        values.put("categoryId", event.getCategory() != null ? event.getCategory().getId() : null);
        values.put("status", event.getStatus() != null ? event.getStatus().name() : null);
        values.put("shortDescription", event.getShortDescription());
        values.put("location", event.getLocation());
        values.put("startDate", event.getStartDate());
        values.put("endDate", event.getEndDate());
        values.put("targetAmount", event.getTargetAmount());
        values.put("currentAmount", event.getCurrentAmount());
        values.put("thumbnailUrl", event.getThumbnailUrl());
        return values;
    }

    private void validateEventUpdateWindow(Event event) {
        LocalDate endDate = event.getEndDate();
        if (endDate == null) {
            return;
        }

        LocalDate lastEditableDate = endDate.plusDays(3);
        if (LocalDate.now().isAfter(lastEditableDate)) {
            throw new BusinessException(EVENT_UPDATE_WINDOW_EXPIRED_MESSAGE);
        }
    }

    private EEventStatus calculateStatusByDate(Event event, LocalDate today) {
        if (event.getStartDate() == null || event.getEndDate() == null) {
            return event.getStatus();
        }
        if (EEventStatus.DRAFT.equals(event.getStatus())) {
            return EEventStatus.DRAFT;
        }
        if (today.isBefore(event.getStartDate())) {
            return EEventStatus.UPCOMING;
        }
        if (today.isAfter(event.getEndDate())) {
            return EEventStatus.COMPLETED;
        }
        return EEventStatus.ONGOING;
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
