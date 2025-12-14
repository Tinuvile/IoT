package com.tinuvile.service;

import com.tinuvile.model.PublishStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * WebSocket消息推送服务
 * 定期推送发布状态更新到前端
 * 
 * @author tinuvile
 */
@Service
public class WebSocketService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private PublisherService publisherService;
    
    /**
     * 定期推送发布状态（每2秒一次）
     */
    @Scheduled(fixedRate = 2000)
    public void sendPublishStatusUpdate() {
        try {
            PublishStatus status = publisherService.getPublishStatus();
            messagingTemplate.convertAndSend("/topic/publish-status", status);
        } catch (Exception e) {
            logger.debug("推送发布状态失败: {}", e.getMessage());
        }
    }
    
    /**
     * 推送系统信息更新（每10秒一次）
     */
    @Scheduled(fixedRate = 10000)
    public void sendSystemInfoUpdate() {
        try {
            PublisherService.SystemInfo systemInfo = publisherService.getSystemInfo();
            messagingTemplate.convertAndSend("/topic/system-info", systemInfo);
        } catch (Exception e) {
            logger.debug("推送系统信息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 手动推送消息
     */
    public void sendMessage(String destination, Object message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            logger.warn("推送消息失败: {}", e.getMessage());
        }
    }
}