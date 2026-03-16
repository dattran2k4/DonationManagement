package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.dto.request.SystemConfigUpsertRequest;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import jakarta.validation.Valid;
import com.chiaseyeuthuong.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-SYSTEM-CONFIG-CONTROLLER")
@RequestMapping("/api/configs")
public class ApiSystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public ApiResponse getAllSystemConfig() {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách cấu hình hệ thống thành công")
                .data(systemConfigService.getAllSystemConfig())
                .build();
    }

    @GetMapping("/map")
    public ApiResponse getAllSystemConfigMap() {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy cấu hình key-value thành công")
                .data(systemConfigService.getAllSystemConfigMap())
                .build();
    }

    @PutMapping
    public ApiResponse upsertSystemConfig(@Valid @RequestBody SystemConfigUpsertRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Lưu cấu hình hệ thống thành công")
                .data(systemConfigService.upsertSystemConfig(request))
                .build();
    }

    @PostMapping("/upload-image")
    public ApiResponse uploadSystemConfigImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.builder()
                .status(201)
                .message("Tải ảnh cấu hình thành công")
                .data(systemConfigService.uploadImage(file))
                .build();
    }
}
