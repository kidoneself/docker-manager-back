package com.dsm.listener;

import com.dsm.config.AppConfig;
import com.dsm.event.SystemSettingChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SystemSettingChangedListener {

    @Autowired
    private AppConfig appConfig;

    @EventListener
    public void handleSystemSettingChanged(SystemSettingChangedEvent event) {
        String key = event.getKey();
        String newValue = event.getNewValue();

        // 如果是代理设置变更，更新AppConfig
        if ("proxy_url".equals(key)) {
            appConfig.setProxyUrl(newValue);
        }
    }
} 