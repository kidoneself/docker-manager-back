package com.dsm.websocket.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

/**
 * WebSocket消息类
 * 
 * @author dsm
 * @version 1.0
 * @since 2024-03-21
 */
@Data
@Schema(description = "WebSocket消息")
public class DockerWebSocketMessage {
    
    @Schema(description = "消息类型", example = "PULL_IMAGE")
    private String type;
    
    @Schema(description = "任务ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private String taskId;
    
    @Schema(description = "消息数据")
    private Object data;
    
    @Schema(description = "时间戳", example = "1647123456789")
    private long timestamp;
    
    /**
     * 构造函数
     * 
     * @param type 消息类型
     * @param taskId 任务ID
     * @param data 消息数据
     */
    public DockerWebSocketMessage(String type, String taskId, Object data) {
        this.type = type;
        this.taskId = taskId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
} 