package com.dsm.service.impl;

import com.dsm.pojo.entity.LogEntry;
import com.dsm.pojo.entity.OperationLog;
import com.dsm.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志服务实现类
 * 负责系统日志的记录和查询
 */
@Service
public class LogServiceImpl implements LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogServiceImpl.class);

    // 模拟日志存储，实际项目中应该使用持久化存储
    private final List<LogEntry> logs = new ArrayList<>();

    public LogServiceImpl() {
        // 初始化一些示例日志数据
        logs.add(new LogEntry("info", "系统启动完成", System.currentTimeMillis() - 3600000));
        logs.add(new LogEntry("info", "用户admin登录系统", System.currentTimeMillis() - 2700000));
        logs.add(new LogEntry("info", "创建容器nginx-web", System.currentTimeMillis() - 1800000));
        logs.add(new LogEntry("warning", "容器mysql-db内存使用率超过80%", System.currentTimeMillis() - 900000));
        logs.add(new LogEntry("error", "容器redis-cache启动失败", System.currentTimeMillis() - 600000));
        logs.add(new LogEntry("info", "更新容器mongo-db配置", System.currentTimeMillis() - 300000));
        logs.add(new LogEntry("info", "备份数据库完成", System.currentTimeMillis()));
    }

    @Override
    public List<LogEntry> getLogs(String level) {
        if (level == null || level.isEmpty()) {
            return logs;
        }

        return logs.stream()
                .filter(log -> log.getLevel().equals(level))
                .collect(Collectors.toList());
    }

    @Override
    public void clearLogs() {
        logs.clear();
    }

    // 添加操作日志的方法，方便其他服务记录日志
    @Override
    public void addOperationLog(OperationLog operationLog) {
        LogEntry logEntry = LogEntry.fromOperationLog(operationLog);
        logs.add(logEntry);
        logger.info("操作日志: {}", logEntry);
    }

    // 添加一般日志的方法
    @Override
    public void addLog(String level, String content) {
        LogEntry logEntry = new LogEntry(level, content, System.currentTimeMillis());
        logs.add(logEntry);
        switch (level.toLowerCase()) {
            case "error":
                logger.error("系统日志: {}", logEntry);
                break;
            case "warning":
                logger.warn("系统日志: {}", logEntry);
                break;
            default:
                logger.info("系统日志: {}", logEntry);
        }
    }
} 