package com.dsm.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
public class OperationLog {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 操作类型（如：创建容器、删除容器等）
     */
    private String operationType;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 操作详情
     */
    private String details;

    /**
     * 操作状态（成功/失败）
     */
    private String status;

    public OperationLog() {
        this.operationTime = LocalDateTime.now();
    }

    public OperationLog(String operationType, String details, String status) {
        this.operationType = operationType;
        this.details = details;
        this.status = status;
        this.operationTime = LocalDateTime.now();
    }
}