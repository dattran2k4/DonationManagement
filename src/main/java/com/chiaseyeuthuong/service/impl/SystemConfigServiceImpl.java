package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.dto.request.SystemConfigItemRequest;
import com.chiaseyeuthuong.dto.request.SystemConfigUpsertRequest;
import com.chiaseyeuthuong.dto.response.SystemConfigResponse;
import com.chiaseyeuthuong.model.SystemConfig;
import com.chiaseyeuthuong.repository.SystemConfigRepository;
import com.chiaseyeuthuong.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "SYSTEM-CONFIG-SERVICE")
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private static final String SYSTEM_IMAGE_UPLOAD_DIR = "uploads/images/";

    @Override
    public List<SystemConfig> getAllSystemConfig() {
        return systemConfigRepository.findAll();
    }

    @Override
    public Map<String, String> getAllSystemConfigMap() {
        return systemConfigRepository.findAll().stream()
                .collect(Collectors.toMap(SystemConfig::getKey, config -> config.getValue() == null ? "" : config.getValue(), (oldVal, newVal) -> newVal));
    }

    @Override
    @Transactional
    public List<SystemConfigResponse> upsertSystemConfig(SystemConfigUpsertRequest request) {
        Set<String> keys = request.getConfigs().stream()
                .map(SystemConfigItemRequest::getKey)
                .map(String::trim)
                .collect(Collectors.toSet());

        Map<String, SystemConfig> existingByKey = new HashMap<>();
        systemConfigRepository.findByKeyIn(keys).forEach(config -> existingByKey.put(config.getKey(), config));

        List<SystemConfig> entities = request.getConfigs().stream()
                .map(item -> {
                    String key = item.getKey().trim();
                    SystemConfig entity = existingByKey.getOrDefault(key, new SystemConfig());
                    entity.setKey(key);
                    entity.setValue(item.getValue());
                    entity.setDescription(item.getDescription());
                    return entity;
                })
                .toList();

        return systemConfigRepository.saveAll(entities).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp tải lên không hợp lệ");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận tệp hình ảnh");
        }

        String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String safeName = originalName.replaceAll("\\s+", "_");
        String fileName = UUID.randomUUID() + "_" + safeName;

        try {
            File directory = new File(SYSTEM_IMAGE_UPLOAD_DIR);
            if (!directory.exists()) directory.mkdirs();

            Path filePath = Paths.get(SYSTEM_IMAGE_UPLOAD_DIR + fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/images/" + fileName;
        } catch (IOException ex) {
            log.error("Không thể lưu ảnh cấu hình: {}", ex.getMessage(), ex);
            throw new RuntimeException("Không thể lưu ảnh cấu hình", ex);
        }
    }

    private SystemConfigResponse toResponse(SystemConfig config) {
        SystemConfigResponse response = new SystemConfigResponse();
        BeanUtils.copyProperties(config, response);
        return response;
    }
}
