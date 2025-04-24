package com.dsm.controller;

import com.dsm.pojo.entity.AppStoreApp;
import com.dsm.service.AppStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app-store")
@Tag(name = "应用商店", description = "应用商店相关接口")
public class AppStoreController {

    @Autowired
    private AppStoreService appStoreService;

    @Operation(summary = "获取应用列表", description = "获取应用商店中的应用列表")
    @GetMapping("/apps")
    public Map<String, Object> getAppList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        return appStoreService.getAppList(page, pageSize, search, category);
    }

    @Operation(summary = "获取应用详情", description = "根据ID获取应用详情")
    @GetMapping("/apps/{id}")
    public AppStoreApp getAppDetail(@PathVariable String id) {
        return appStoreService.getAppDetail(id);
    }

    @Operation(summary = "安装应用", description = "安装指定的应用")
    @PostMapping("/apps/{id}/install")
    public Map<String, String> installApp(
            @PathVariable String id,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> selectedServices = (List<String>) request.get("selectedServices");
        @SuppressWarnings("unchecked")
        Map<String, String> envValues = (Map<String, String>) request.get("envValues");
        
        String taskId = appStoreService.installApp(id, selectedServices, envValues);
        return Map.of("taskId", taskId);
    }

    @Operation(summary = "获取安装状态", description = "获取应用安装的状态")
    @GetMapping("/install/{taskId}/status")
    public Map<String, Object> getInstallStatus(@PathVariable String taskId) {
        return appStoreService.getInstallStatus(taskId);
    }

    @Operation(summary = "取消安装", description = "取消正在进行的应用安装")
    @PostMapping("/install/{taskId}/cancel")
    public Map<String, Boolean> cancelInstall(@PathVariable String taskId) {
        boolean success = appStoreService.cancelInstall(taskId);
        return Map.of("success", success);
    }

    @Operation(summary = "下载文件", description = "下载文件到指定目录")
    @PostMapping("/download")
    public Map<String, Object> downloadFile(@RequestBody Map<String, String> request) {
        String fileUrl = request.get("fileUrl");
        return appStoreService.downloadFile(fileUrl);
    }
} 