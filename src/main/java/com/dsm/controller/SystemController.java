package com.dsm.controller;

import com.dsm.api.DockerService;
import com.dsm.pojo.entity.OperationLog;
import com.dsm.pojo.entity.Route;
import com.dsm.pojo.entity.SystemStats;
import com.dsm.pojo.dto.SystemSettingDTO;
import com.dsm.service.LogService;
import com.dsm.service.SystemSettingService;
import com.dsm.utils.ApiResponse;
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

    @Operation(summary = "获取系统日志", description = "获取系统的操作日志")
    @GetMapping("/logs")
    public ApiResponse<List<OperationLog>> getLogs() {
        List<OperationLog> logs = new ArrayList<>();
        logs.add(new OperationLog("创建容器", "admin", "成功"));
        logs.add(new OperationLog("启动容器", "admin", "成功"));
        logs.add(new OperationLog("停止容器", "admin", "成功"));
        logService.addLog("info", "通过旧接口查询系统日志");

        return ApiResponse.success(logs);
    }

    @Operation(summary = "获取Docker状态", description = "检查Docker服务的运行状态")
    @GetMapping("/docker/status")
    public ApiResponse<Map<String, Object>> getDockerStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean isAvailable = dockerService.isDockerAvailable();

            response.put("available", isAvailable);
            if (isAvailable) {
                response.put("message", "Docker服务正常运行");
            } else {
                response.put("message", "Docker服务不可用");
            }

            logService.addLog("info", "检查Docker服务状态");

            return ApiResponse.success(response);
        } catch (Exception e) {
            response.put("available", false);
            response.put("message", "检查Docker状态时发生错误: " + e.getMessage());

            logService.addLog("error", "检查Docker状态时发生错误: " + e.getMessage());

            return ApiResponse.error(500, "检查Docker状态时发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "设置系统信息", description = "设置系统的配置信息")
    @PostMapping("/settings")
    public ApiResponse<String> setSystemSetting(@RequestBody SystemSettingDTO setting) {
        try {
            String oldValue = systemSettingService.get(setting.getKey());
            if (oldValue != null && !oldValue.isEmpty()) {
                systemSettingService.set(setting.getKey(), setting.getValue());
                logService.addLog("info", "更新系统配置: " + setting.getKey() + " 从 " + oldValue + " 更新为 " + setting.getValue());
                return ApiResponse.success("系统配置更新成功");
            } else {
                systemSettingService.set(setting.getKey(), setting.getValue());
                logService.addLog("info", "设置系统配置: " + setting.getKey() + " = " + setting.getValue());
                return ApiResponse.success("系统配置设置成功");
            }
        } catch (Exception e) {
            logService.addLog("error", "设置系统配置失败: " + e.getMessage());
            return ApiResponse.error(500, "设置系统配置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新系统信息", description = "更新系统的配置信息")
    @PutMapping("/settings")
    public ApiResponse<String> updateSystemSetting(@RequestBody SystemSettingDTO setting) {
        try {
            String oldValue = systemSettingService.get(setting.getKey());
            systemSettingService.set(setting.getKey(), setting.getValue());
            logService.addLog("info", "更新系统配置: " + setting.getKey() + " 从 " + oldValue + " 更新为 " + setting.getValue());
            return ApiResponse.success("系统配置更新成功");
        } catch (Exception e) {
            logService.addLog("error", "更新系统配置失败: " + e.getMessage());
            return ApiResponse.error(500, "更新系统配置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取系统信息", description = "获取系统的配置信息")
    @GetMapping("/settings")
    public ApiResponse<String> getSystemSetting(@RequestParam String key) {
        try {
            String value = systemSettingService.get(key);
            if (value == null || value.isEmpty()) {
                return ApiResponse.success("");
            }
            return ApiResponse.success(value);
        } catch (Exception e) {
            logService.addLog("error", "获取系统配置失败: " + e.getMessage());
            return ApiResponse.error(500, "获取系统配置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除系统信息", description = "删除系统的配置信息")
    @DeleteMapping("/settings")
    public ApiResponse<String> deleteSystemSetting(@RequestParam String key) {
        try {
            String oldValue = systemSettingService.get(key);
            if (oldValue != null && !oldValue.isEmpty()) {
                systemSettingService.set(key, "");
                logService.addLog("info", "删除系统配置: " + key + " = " + oldValue);
                return ApiResponse.success("系统配置删除成功");
            } else {
                return ApiResponse.success("系统配置不存在");
            }
        } catch (Exception e) {
            logService.addLog("error", "删除系统配置失败: " + e.getMessage());
            return ApiResponse.error(500, "删除系统配置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取授权菜单", description = "获取授权菜单")
    @GetMapping("/getMenu")
    public ApiResponse<List<Route>> getMenu() {
        List<Route> objects = new ArrayList<>();
        objects.add(new Route());
//        HashMap<String, Object> map = new HashMap<>();
//        map.put("list", objects);
        return ApiResponse.success(objects);
    }

}