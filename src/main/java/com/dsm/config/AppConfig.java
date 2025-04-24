package com.dsm.config;

import org.springframework.stereotype.Component;

@Component
public class AppConfig {
    private String proxyUrl;

    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }
}