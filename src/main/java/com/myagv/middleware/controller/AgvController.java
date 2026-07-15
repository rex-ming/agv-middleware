package com.myagv.middleware.controller;

import com.myagv.middleware.dto.ApiResponse;
import com.myagv.middleware.dto.CreateOrderRequest;
import com.myagv.middleware.dto.HealthDTO;
import com.myagv.middleware.dto.LocationDTO;
import com.myagv.middleware.dto.OrderDTO;
import com.myagv.middleware.dto.VehicleDTO;
import com.myagv.middleware.service.AgvService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AgvController {

    private static final Logger logger = LoggerFactory.getLogger(AgvController.class);

    private final AgvService agvService;

    @Autowired
    public AgvController(AgvService agvService) {
        this.agvService = agvService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthDTO>> getHealth() {
        System.out.println("========================================");
        System.out.println("AgvController.getHealth called!");
        System.out.println("========================================");
        logger.debug("Health check requested");
        HealthDTO health = agvService.checkHealth();
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        logger.info("AgvController.test called!");
        return ResponseEntity.ok(ApiResponse.success("Test successful"));
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.success("pong"));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<ApiResponse<List<VehicleDTO>>> getVehicles() {
        logger.debug("Fetching vehicles");
        List<VehicleDTO> vehicles = agvService.getVehicles();
        return ResponseEntity.ok(ApiResponse.success(vehicles));
    }

    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<LocationDTO>>> getLocations() {
        logger.debug("Fetching locations");
        List<LocationDTO> locations = agvService.getLocations();
        return ResponseEntity.ok(ApiResponse.success(locations));
    }

    @GetMapping("/points")
    public ResponseEntity<ApiResponse<List<LocationDTO>>> getPoints() {
        logger.debug("Fetching points");
        List<LocationDTO> locations = agvService.getLocations();
        return ResponseEntity.ok(ApiResponse.success(locations));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrders() {
        logger.debug("Fetching orders");
        List<OrderDTO> orders = agvService.getOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        logger.info("========================================");
        logger.info("AgvController.createOrder called!");
        logger.info("destination: {}", request.getDestination());
        logger.info("priority: {}", request.getPriority());
        logger.info("agvService: {}", agvService);
        logger.info("========================================");
        
        try {
            OrderDTO order = agvService.createOrder(request.getDestination(), request.getPriority());
            if (order != null) {
                return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Failed to create order"));
            }
        } catch (Exception e) {
            logger.error("Error creating order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating order", e.getMessage()));
        }
    }

    @PostMapping("/execute/{orderName}")
    public ResponseEntity<ApiResponse<String>> executeOrder(@PathVariable String orderName) {
        logger.info("Executing order: {}", orderName);
        
        try {
            boolean success = agvService.executeOrder(orderName);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Order execution requested", orderName));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Failed to execute order"));
            }
        } catch (Exception e) {
            logger.error("Error executing order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error executing order", e.getMessage()));
        }
    }

    @PostMapping("/assign/{orderName}/{vehicleName}")
    public ResponseEntity<ApiResponse<String>> assignOrder(@PathVariable String orderName, @PathVariable String vehicleName) {
        logger.info("Assigning order {} to vehicle {}", orderName, vehicleName);
        
        try {
            boolean success = agvService.assignOrder(orderName, vehicleName);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Order assigned successfully", orderName));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Failed to assign order"));
            }
        } catch (Exception e) {
            logger.error("Error assigning order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error assigning order", e.getMessage()));
        }
    }

    @PostMapping("/cancel/{orderName}")
    public ResponseEntity<ApiResponse<String>> cancelOrder(@PathVariable String orderName) {
        logger.info("Canceling order: {}", orderName);
        
        try {
            boolean success = agvService.cancelOrder(orderName);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Order canceled", orderName));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Failed to cancel order"));
            }
        } catch (Exception e) {
            logger.error("Error canceling order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error canceling order", e.getMessage()));
        }
    }

    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<Boolean>> connectToKernel() {
        logger.info("Attempting to connect to OpenTCS kernel");
        
        try {
            boolean success = agvService.connectToKernel();
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Connected to OpenTCS kernel", true));
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.error("Failed to connect to OpenTCS kernel"));
            }
        } catch (Exception e) {
            logger.error("Error connecting to kernel: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error connecting to kernel", e.getMessage()));
        }
    }

    @PostMapping("/test-order")
    public ResponseEntity<ApiResponse<String>> testOrder(@RequestBody CreateOrderRequest request) {
        logger.info("========================================");
        logger.info("AgvController.testOrder called!");
        logger.info("destination: {}", request.getDestination());
        logger.info("priority: {}", request.getPriority());
        logger.info("agvService: {}", agvService);
        logger.info("========================================");
        
        return ResponseEntity.ok(ApiResponse.success("Test order endpoint called", "SUCCESS"));
    }

    @PostMapping("/create-order-now")
    public ResponseEntity<ApiResponse<OrderDTO>> createOrderNow(@Valid @RequestBody CreateOrderRequest request) {
        logger.info("========================================");
        logger.info("AgvController.createOrderNow called!");
        logger.info("destination: {}", request.getDestination());
        logger.info("priority: {}", request.getPriority());
        logger.info("agvService: {}", agvService);
        logger.info("========================================");
        
        try {
            OrderDTO order = agvService.createOrder(request.getDestination(), request.getPriority());
            if (order != null) {
                return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Failed to create order"));
            }
        } catch (Exception e) {
            logger.error("Error creating order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating order", e.getMessage()));
        }
    }
}