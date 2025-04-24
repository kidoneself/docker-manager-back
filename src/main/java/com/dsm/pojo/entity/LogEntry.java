package com.dsm.pojo.entity;

import lombok.Data;

/**
 * 日志条目实体类
 */
@Data
public class LogEntry {
    /**
     * 日志级别（info、warning、error等）
     */
    private String level;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 日志记录时间戳
     */
    private long timestamp;

    public LogEntry(String level, String content, long timestamp) {
        this.level = level;
        this.content = content;
        this.timestamp = timestamp;
    }

    /**
     * 将操作日志转换为日志条目
     *
     * @param logs 操作日志对象
     * @return 转换后的日志条目
     */
    public static LogEntry fromOperationLog(OperationLog logs) {
        String level = "info";
        if (!logs.getStatus().equals("成功")) {
            level = "error";
        }

        String content = logs.getOperationType() + " - " + logs.getDetails() + ", 状态: " + logs.getStatus();

        return new LogEntry(level, content, logs.getOperationTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
    }
}