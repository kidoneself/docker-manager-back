package com.dsm.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.dsm.websocket.callback.PullImageCallback;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.api.DockerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Docker WebSocket处理器
 * 
 * @author dsm
 * @version 1.0
 * @since 2024-03-21
 */
@Slf4j
@Component
@Tag(name = "Docker WebSocket处理器", description = "处理Docker相关的WebSocket消息")
public class DockerWebSocketHandler extends TextWebSocketHandler {
    
    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();
    
    @Autowired
    private DockerService dockerService;
    
    /**
     * 连接建立时的处理
     * 
     * @param session WebSocket会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        SESSIONS.put(sessionId, session);
        log.info("WebSocket连接已建立: {}", sessionId);
    }
    
    /**
     * 处理接收到的消息
     * 
     * @param session WebSocket会话
     * @param message 接收到的消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            DockerWebSocketMessage wsMessage = JSON.parseObject(message.getPayload(), DockerWebSocketMessage.class);
            
            switch (wsMessage.getType()) {
                case "PULL_IMAGE":
                    handlePullImage(session, wsMessage);
                    break;
                default:
                    log.warn("未知的消息类型: {}", wsMessage.getType());
            }
        } catch (Exception e) {
            log.error("处理消息时发生错误", e);
            sendErrorMessage(session, "处理消息时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 连接关闭时的处理
     * 
     * @param session WebSocket会话
     * @param status 关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        SESSIONS.remove(sessionId);
        log.info("WebSocket连接已关闭: {}", sessionId);
    }
    
    /**
     * 处理拉取镜像的请求
     * 
     * @param session WebSocket会话
     * @param message WebSocket消息
     */
    private void handlePullImage(WebSocketSession session, DockerWebSocketMessage message) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String imageName = (String) data.get("imageName");
        String taskId = UUID.randomUUID().toString();
        
        // 发送开始消息
        sendMessage(session, new DockerWebSocketMessage(
            "PULL_START",
            taskId,
            Map.of("imageName", imageName)
        ));
        
        // 异步执行拉取操作
        CompletableFuture.runAsync(() -> {
            try {
                // 解析镜像名称和标签
                String[] parts = imageName.split(":");
                String image = parts[0];
                String tag = parts.length > 1 ? parts[1] : "latest";
                
                // 使用DockerService的pullImage方法
                dockerService.pullImage(image, tag, new PullImageCallback() {
                    @Override
                    public void onProgress(int progress, String status) {
                        // 发送进度消息
                        sendMessage(session, new DockerWebSocketMessage(
                            "PULL_PROGRESS",
                            taskId,
                            Map.of(
                                "progress", progress,
                                "status", status
                            )
                        ));
                    }
                    
                    @Override
                    public void onComplete() {
                        // 发送完成消息
                        sendMessage(session, new DockerWebSocketMessage(
                            "PULL_COMPLETE",
                            taskId,
                            Map.of("status", "success")
                        ));
                    }
                    
                    @Override
                    public void onError(String error) {
                        // 发送错误消息
                        sendErrorMessage(session, error);
                    }
                });
            } catch (Exception e) {
                sendErrorMessage(session, e.getMessage());
            }
        });
    }
    
    /**
     * 发送消息
     * 
     * @param session WebSocket会话
     * @param message WebSocket消息
     */
    private void sendMessage(WebSocketSession session, DockerWebSocketMessage message) {
        try {
            session.sendMessage(new TextMessage(JSON.toJSONString(message)));
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }
    
    /**
     * 发送错误消息
     * 
     * @param session WebSocket会话
     * @param error 错误信息
     */
    private void sendErrorMessage(WebSocketSession session, String error) {
        sendMessage(session, new DockerWebSocketMessage(
            "ERROR",
            UUID.randomUUID().toString(),
            Map.of("error", error)
        ));
    }
} 