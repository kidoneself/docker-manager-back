package com.dsm.controller;

import com.dsm.pojo.entity.LogEntry;
import com.dsm.service.LogService;
import com.dsm.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "日志管理", description = "日志相关的操作接口")
@RestController
@RequestMapping("/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @Operation(summary = "获取日志", description = "根据日志级别获取日志列表")
    @GetMapping
    public ApiResponse<List<LogEntry>> getLogs(@RequestParam(required = false) String level) {
        return ApiResponse.success(logService.getLogs(level));
    }

    @Operation(summary = "清除日志", description = "清除所有日志")
    @DeleteMapping
    public ApiResponse<Void> clearLogs() {
        logService.clearLogs();
        return ApiResponse.success(null);
    }
}