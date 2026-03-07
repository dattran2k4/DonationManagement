package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EActivityStatus;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.dto.request.ActivityRequest;
import com.chiaseyeuthuong.dto.response.ActivityResponse;
import com.chiaseyeuthuong.dto.response.EventResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.Activity;
import com.chiaseyeuthuong.model.Event;
import com.chiaseyeuthuong.repository.ActivityRepository;
import com.chiaseyeuthuong.repository.EventRepository;
import com.chiaseyeuthuong.service.ActivityService;
import com.chiaseyeuthuong.service.DonorService;
import com.github.slugify.Slugify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.chiaseyeuthuong.service.impl.EventServiceImpl.UPLOAD_DIR;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ACTIVITY-SERVICE")
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final EventRepository eventRepository;
    private final DonorService donorService;

    @Override
    public PageResponse<ActivityResponse> getAllActivities(int page, int size, String search, EActivityStatus status) {

        int pageNumber = (page > 0) ? page - 1 : 0;

        PageRequest pageRequest = PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Activity> pageActivities = activityRepository.findAll(pageRequest);

        List<ActivityResponse> response = pageActivities.stream().map(this::toResponse).toList();

        return PageResponse.<ActivityResponse>builder()
                .page(page)
                .pageSize(size)
                .totalItems(pageActivities.getTotalElements())
                .totalPages(pageActivities.getTotalPages())
                .data(response)
                .build();
    }

    @Override
    public List<ActivityResponse> getAllActivitiesByEventId(Long eventId) {
        return activityRepository.findAllByEventId(eventId).stream().map(this::toResponse).toList();
    }

    @Override
    public void saveActivity(ActivityRequest request) {
        log.info("Processing saving activity from eventId {} ", request.getEventId());

        Event event = eventRepository.findById(request.getEventId()).orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        Activity activity = (request.getId() != null) ? getActivity(request.getId()) : new Activity();

        activity.setEvent(event);
        activity.setName(request.getName());
        activity.setContent(request.getContent());
        activity.setShortDescription(request.getShortDescription());
        activity.setStartDate(request.getStartDate());
        activity.setEndDate(request.getEndDate());
        activity.setCurrentAmount(request.getCurrentAmount());
        activity.setTargetAmount(request.getTargetAmount());
        activity.setThumbnailUrl(request.getThumbnailUrl());
        activity.setStatus(request.getStatus());

        Slugify slugify = Slugify.builder().build();
        String slug = slugify.slugify(request.getName());
        activity.setSlug(slug);

        Activity result = activityRepository.save(activity);

        log.info("Saved activity {} ", result.getId());
    }

    @Override
    public Activity getActivity(Long id) {
        return activityRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
    }

    @Override
    public ActivityResponse getActivityById(Long id) {
        return toResponse(getActivity(id));
    }

    @Override
    public ActivityResponse getActivityBySlug(String slug) {
        Activity activity = activityRepository.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
        return toResponse(activity);
    }

    @Override
    public void updateCurrentAmount(Activity activity, BigDecimal amount) {
        BigDecimal newCurrentAmount = activity.getCurrentAmount().add(amount);
        activity.setCurrentAmount(newCurrentAmount);
        activityRepository.save(activity);

        log.info("Updated current amount={} for activityId={} ", newCurrentAmount, activity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveThumbnailUrl(Long id, MultipartFile file) {
        try {
            Activity activity = null;
            if (id != null) {
                activity = getActivity(id);
            }
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) directory.mkdirs();

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(UPLOAD_DIR + fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/thumbnails/" + fileName;

            if (activity != null) {
                activity.setThumbnailUrl(fileUrl);
                activityRepository.save(activity);
                log.info("Saved thumbnail url activity {} ", activity.getId());
                log.info("Saved thumbnail url {} ", activity.getThumbnailUrl());
            } else {
                log.info("Saved thumbnail url without activity reference");
            }

            return fileUrl;
        } catch (IOException e) {
            log.error("Cannot save thumbnail caused: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot save thumbnail url caused ", e);
        }
    }

    private ActivityResponse toResponse(Activity activity) {
        ActivityResponse activityResponse = new ActivityResponse();
        BeanUtils.copyProperties(activity, activityResponse);
        activityResponse.setNumberOfDonors(donorService.getDorCountByObjectId(activity.getId(), EEntityType.ACTIVITY));
        EventResponse eventResponse = new EventResponse();
        BeanUtils.copyProperties(activity.getEvent(), eventResponse);
        activityResponse.setEvent(eventResponse);
        activityResponse.setEventId(activity.getEvent().getId());
        return activityResponse;
    }
}
