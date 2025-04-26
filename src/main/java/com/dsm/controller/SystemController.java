package com.dsm.controller;

import com.dsm.api.DockerService;
import com.dsm.pojo.entity.Log;
import com.dsm.pojo.entity.Route;
import com.dsm.pojo.entity.SystemStats;
import com.dsm.pojo.entity.SystemSetting;
import com.dsm.service.LogService;
import com.dsm.service.SystemSettingService;
import com.dsm.utils.ApiResponse;
import com.dsm.utils.LogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
@Tag(name = "系统管理", description = "系统状态和监控接口")
public class SystemController {

    private final Random random = new Random();
    private final LogService logService;
    private final DockerService dockerService;
    private final SystemSettingService systemSettingService;

    @Operation(summary = "获取系统统计", description = "获取系统的CPU、内存和磁盘使用率")
    @GetMapping("/stats")
    public ApiResponse<SystemStats> getSystemStats() {
        return ApiResponse.success(new SystemStats(random.nextInt(100),  // CPU使用率
                random.nextInt(100),  // 内存使用率
                random.nextInt(100),  // 磁盘使用率
                System.currentTimeMillis()));
    }

    @Operation(summary = "获取系统日志", description = "获取系统日志列表")
    @GetMapping("/logs")
    public ApiResponse<List<Log>> getLogs() {
        return ApiResponse.success(logService.getLogs("SYSTEM", null));
    }

    @Operation(summary = "检查Docker服务状态", description = "检查Docker服务是否可用")
    @GetMapping("/docker/status")
    public ApiResponse<Boolean> checkDockerStatus() {
        try {
            boolean isAvailable = dockerService.isDockerAvailable();
            LogUtil.logSysInfo("检查Docker服务状态: " + (isAvailable ? "可用" : "不可用"));
            return ApiResponse.success(isAvailable);
        } catch (Exception e) {
            LogUtil.logSysError("检查Docker状态时发生错误: " + e.getMessage());
            return ApiResponse.error("检查Docker状态失败: " + e.getMessage());
        }
    }

    @Operation(summary = "设置系统配置", description = "设置系统配置项")
    @PostMapping("/settings")
    public ApiResponse<Void> setSetting(@RequestBody SystemSetting setting) {
        try {
            String oldValue = systemSettingService.get(setting.getKey());
            if (oldValue != null) {
                systemSettingService.set(setting.getKey(), setting.getValue());
                LogUtil.logSysInfo("更新系统配置: " + setting.getKey() + " 从 " + oldValue + " 更新为 " + setting.getValue());
            } else {
                systemSettingService.set(setting.getKey(), setting.getValue());
                LogUtil.logSysInfo("设置系统配置: " + setting.getKey() + " = " + setting.getValue());
            }
            return ApiResponse.success(null);
        } catch (Exception e) {
            LogUtil.logSysError("设置系统配置失败: " + e.getMessage());
            return ApiResponse.error("设置系统配置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新系统配置", description = "更新系统配置项")
    @PutMapping("/settings")
    public ApiResponse<Void> updateSetting(@RequestBody SystemSetting setting) {
        try {
            String oldValue = systemSettingService.get(setting.getKey());
            systemSettingService.set(setting.getKey(), setting.getValue());
            LogUtil.logSysInfo("更新系统配置: " + setting.getKey() + " 从 " + oldValue + " 更新为 " + setting.getValue());
            return ApiResponse.success(null);
        } catch (Exception e) {
            LogUtil.logSysError("更新系统配置失败: " + e.getMessage());
            return ApiResponse.error("更新系统配置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取系统配置", description = "获取系统配置项")
    @GetMapping("/settings")
    public ApiResponse<List<SystemSetting>> getSettings() {
        try {
            return ApiResponse.success(systemSettingService.getAll());
        } catch (Exception e) {
            LogUtil.logSysError("获取系统配置失败: " + e.getMessage());
            return ApiResponse.error("获取系统配置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除系统配置", description = "删除系统配置项")
    @DeleteMapping("/settings/{key}")
    public ApiResponse<Void> deleteSetting(@PathVariable String key) {
        try {
            String oldValue = systemSettingService.get(key);
            systemSettingService.set(key, "");
            LogUtil.logSysInfo("删除系统配置: " + key + " = " + oldValue);
            return ApiResponse.success(null);
        } catch (Exception e) {
            LogUtil.logSysError("删除系统配置失败: " + e.getMessage());
            return ApiResponse.error("删除系统配置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取授权菜单", description = "获取授权菜单")
    @GetMapping("/getMenu")
    public ApiResponse<List<Route>> getMenu() {
        List<Route> objects = new ArrayList<>();
        objects.add(new Route());
        return ApiResponse.success(objects);
    }
}