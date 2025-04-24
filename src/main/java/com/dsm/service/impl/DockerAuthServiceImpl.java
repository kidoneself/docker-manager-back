package com.dsm.service.impl;

import com.dsm.service.DockerAuthService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Docker认证服务实现类
 * 实现Docker登录、登出以及认证信息管理
 */
@Service
@RequiredArgsConstructor
public class DockerAuthServiceImpl implements DockerAuthService {

    @Resource
    private final DockerClient dockerClient;

    @Override
    public boolean login(String registry, String username, String password) {
        try {
            AuthConfig authConfig = new AuthConfig().withRegistryAddress(registry).withUsername(username).withPassword(password);

            AuthResponse response = dockerClient.authCmd().withAuthConfig(authConfig).exec();

            return response.getStatus().equals("Login Succeeded");
        } catch (Exception e) {
            throw new RuntimeException("Docker登录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean logout(String registry) {
        try {
            // Docker Java API 没有直接的 logout 方法
            // 我们可以通过清除认证信息来实现登出
            AuthConfig authConfig = new AuthConfig().withRegistryAddress(registry).withUsername("").withPassword("");

            dockerClient.authCmd().withAuthConfig(authConfig).exec();

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Docker登出失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isLoggedIn(String registry) {
        try {
            // 获取认证配置
            AuthConfig authConfig = dockerClient.authConfig();
            // 检查是否有认证信息
            if (authConfig == null || authConfig.getUsername() == null || authConfig.getUsername().isEmpty()) {
                return false;
            }

            // 处理 Docker Hub 的特殊情况
            if (registry.equals("https://index.docker.io/v1/")) {
                return authConfig.getRegistryAddress() == null || authConfig.getRegistryAddress().isEmpty() || authConfig.getRegistryAddress().equals("https://index.docker.io/v1/");
            }

            // 检查其他仓库
            return authConfig.getRegistryAddress() != null && authConfig.getRegistryAddress().equals(registry);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<Map<String, String>> getRegistries() {
        try {
            List<Map<String, String>> registries = new ArrayList<>();

            // 获取认证配置
            AuthConfig authConfig = dockerClient.authConfig();
            if (authConfig != null && authConfig.getRegistryAddress() != null) {
                Map<String, String> registry = new HashMap<>();
                registry.put("registry", authConfig.getRegistryAddress());
                registry.put("username", authConfig.getUsername());
                registries.add(registry);
            }

            return registries;
        } catch (Exception e) {
            throw new RuntimeException("获取仓库列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void addRegistry(String registry, String username, String password) {
        try {
            // 先尝试登录
            boolean success = login(registry, username, password);
            if (!success) {
                throw new RuntimeException("登录仓库失败");
            }
        } catch (Exception e) {
            throw new RuntimeException("添加仓库失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteRegistry(String registry) {
        try {
            // 先登出
            boolean success = logout(registry);
            if (!success) {
                throw new RuntimeException("登出仓库失败");
            }
        } catch (Exception e) {
            throw new RuntimeException("删除仓库失败: " + e.getMessage(), e);
        }
    }
} 