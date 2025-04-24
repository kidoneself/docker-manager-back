package com.dsm.loader;

import com.alibaba.fastjson.JSONObject;
import com.dsm.config.AppConfig;
import com.dsm.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.URI;

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
            System.out.println("未设置HTTP代理，跳过代理设置");
            return;
        }
        System.out.println(proxyUrl);
        JSONObject jsonObject = JSONObject.parseObject(proxyUrl);
        String url = jsonObject.getString("url");
        appConfig.setProxyUrl(url); // ✅ 设置到全局字段

        try {
            URI uri = new URI(url);
            System.out.println("已设置系统 HTTP 代理: " + proxyUrl);
        } catch (Exception e) {
            System.err.println("代理地址格式不正确：" + proxyUrl);
        }
    }
}