package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.dto.request.SystemConfigUpsertRequest;
import com.chiaseyeuthuong.dto.response.SystemConfigResponse;
import com.chiaseyeuthuong.model.SystemConfig;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface SystemConfigService {

    List<SystemConfig> getAllSystemConfig();

    Map<String, String> getAllSystemConfigMap();

    List<SystemConfigResponse> upsertSystemConfig(SystemConfigUpsertRequest request);

    String uploadImage(MultipartFile file);
}
