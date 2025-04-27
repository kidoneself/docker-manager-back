package com.dsm.service;

import com.dsm.pojo.entity.SystemSetting;

import java.util.List;

public interface SystemSettingService {
    String get(String key);

    void set(String key, String value);

}