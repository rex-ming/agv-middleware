package com.myagv.middleware.controller;

import com.myagv.middleware.dto.VehicleDTO;
import com.myagv.middleware.service.AgvService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.List;

// @Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final AgvService agvService;

    @Autowired
    public WebSocketController(SimpMessagingTemplate messagingTemplate, AgvService agvService) {
        this.messagingTemplate = messagingTemplate;
        this.agvService = agvService;
    }

    // @Scheduled(fixedRate = 2000)
    public void sendVehicleUpdates() {
        try {
            List<VehicleDTO> vehicles = agvService.getVehicles();
            messagingTemplate.convertAndSend("/topic/vehicles", vehicles);
        } catch (Exception e) {
            logger.error("Error sending vehicle updates: {}", e.getMessage());
        }
    }

    // @Scheduled(fixedRate = 3000)
    public void sendOrderUpdates() {
        try {
            var orders = agvService.getOrders();
            messagingTemplate.convertAndSend("/topic/orders", orders);
        } catch (Exception e) {
            logger.error("Error sending order updates: {}", e.getMessage());
        }
    }
}