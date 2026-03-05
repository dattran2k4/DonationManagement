package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.model.SystemConfig;
import com.chiaseyeuthuong.repository.SystemConfigRepository;
import com.chiaseyeuthuong.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "SYSTEM-CONFIG-SERVICE")
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public List<SystemConfig> getAllSystemConfig() {
        return systemConfigRepository.findAll();
    }
}
