package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                .message("OK")
                .data(systemConfigService.getAllSystemConfig())
                .build();
    }
}
