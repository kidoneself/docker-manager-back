package com.dsm.loader;

import com.alibaba.fastjson.JSONObject;
import com.dsm.config.AppConfig;
import com.dsm.service.SystemSettingService;
import com.dsm.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
public class ProxyConfigLoader {

    @Resource
    private SystemSettingService systemSettingService;

    @Autowired
    private AppConfig appConfig;

    @PostConstruct
    public void initProxy() {
        String proxyUrl = systemSettingService.get("proxy");
        if (proxyUrl == null || proxyUrl.isBlank()) {
            LogUtil.logSysInfo("未设置HTTP代理，跳过代理设置");
            return;
        }
        JSONObject jsonObject = JSONObject.parseObject(proxyUrl);
        String url = jsonObject.getString("url");
        appConfig.setProxyUrl(url); // ✅ 设置到全局字段
        try {
            // URI uri = new URI(url);
            LogUtil.logSysInfo("已设置系统 HTTP 代理: " + proxyUrl);
        } catch (Exception e) {
            LogUtil.logSysError("代理地址格式不正确：" + proxyUrl);
        }
    }
}