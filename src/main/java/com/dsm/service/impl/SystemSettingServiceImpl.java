package com.dsm.service.impl;

import com.dsm.event.SystemSettingChangedEvent;
import com.dsm.mapper.SystemSettingMapper;
import com.dsm.pojo.entity.SystemSetting;
import com.dsm.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    @Autowired
    private SystemSettingMapper systemSettingMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public String get(String key) {
        return systemSettingMapper.getSettingValue(key);
    }

    @Override
    public void set(String key, String value) {
        String oldValue = get(key);
        systemSettingMapper.setSettingValue(key, value);
        // 发布配置变更事件
        eventPublisher.publishEvent(new SystemSettingChangedEvent(key, oldValue, value));
    }


}