package com.dsm.controller;

import com.dsm.pojo.request.DockerLoginRequest;
import com.dsm.pojo.request.DockerRegistryRequest;
import com.dsm.service.DockerAuthService;
import com.dsm.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/docker")
@Tag(name = "Docker认证管理", description = "Docker仓库认证")
public class DockerRegistryController {

    @Autowired
    private DockerAuthService dockerAuthService;

    @Operation(summary = "登录仓库", description = "登录Docker仓库")
    @PostMapping("/auth/login")
    public ApiResponse<Void> login(@RequestBody DockerLoginRequest request) {
        try {
            boolean success = dockerAuthService.login(request.getRegistry(), request.getUsername(), request.getPassword());
            if (success) {
                return ApiResponse.success(null);
            } else {
                return ApiResponse.error(500, "登录失败");
            }
        } catch (Exception e) {
            return ApiResponse.error(500, "登录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "登出仓库", description = "登出Docker仓库")
    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(@RequestParam String registry) {
        try {
            boolean success = dockerAuthService.logout(registry);
            if (success) {
                return ApiResponse.success(null);
            } else {
                return ApiResponse.error(500, "登出失败");
            }
        } catch (Exception e) {
            return ApiResponse.error(500, "登出失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取登录状态", description = "检查仓库登录状态")
    @GetMapping("/login/status")
    public ApiResponse<Boolean> getLoginStatus(@RequestParam String registry) {
        try {
            boolean isLoggedIn = dockerAuthService.isLoggedIn(registry);
            return ApiResponse.success(isLoggedIn);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取登录状态失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取仓库列表", description = "获取所有已配置的仓库列表")
    @GetMapping("/registries")
    public ApiResponse<List<Map<String, String>>> getRegistries() {
        try {
            List<Map<String, String>> registries = dockerAuthService.getRegistries();
            return ApiResponse.success(registries);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取仓库列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "添加仓库", description = "添加新的Docker仓库配置")
    @PostMapping("/registries")
    public ApiResponse<Void> addRegistry(@RequestBody DockerRegistryRequest request) {
        try {
            dockerAuthService.addRegistry(request.getRegistry(), request.getUsername(), request.getPassword());
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(500, "添加仓库失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除仓库", description = "删除Docker仓库配置")
    @DeleteMapping("/registries/{registry}")
    public ApiResponse<Void> deleteRegistry(@PathVariable String registry) {
        try {
            dockerAuthService.deleteRegistry(registry);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(500, "删除仓库失败: " + e.getMessage());
        }
    }
} 