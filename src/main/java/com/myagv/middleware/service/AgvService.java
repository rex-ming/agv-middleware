package com.myagv.middleware.service;

import com.myagv.middleware.dto.HealthDTO;
import com.myagv.middleware.dto.LocationDTO;
import com.myagv.middleware.dto.OrderDTO;
import com.myagv.middleware.dto.VehicleDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgvService {

    private static final Logger logger = LoggerFactory.getLogger(AgvService.class);

    private final OpenTCSKernelClient kernelClient;
    private final MockDataService mockDataService;
    private boolean useMockMode = true;

    private final Map<String, Integer> vehicleIdCache = new ConcurrentHashMap<>();
    private int nextVehicleId = 1;

    private final Map<String, Integer> orderIdCache = new ConcurrentHashMap<>();
    private int nextOrderId = 1;

    @Autowired
    public AgvService(OpenTCSKernelClient kernelClient, MockDataService mockDataService) {
        this.kernelClient = kernelClient;
        this.mockDataService = mockDataService;
        logger.info("AgvService initialized in mock mode");
    }

    public HealthDTO checkHealth() {
        logger.info("checkHealth called");
        return HealthDTO.builder()
                .status("UP")
                .service("AGV-Middleware")
                .opentcsConnected(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public List<VehicleDTO> getVehicles() {
        if (kernelClient.isConnected()) {
            List<Object> rawVehicles = kernelClient.getVehicles();
            if (!rawVehicles.isEmpty()) {
                logger.info("Converting {} real vehicles from OpenTCS", rawVehicles.size());
                return rawVehicles.stream()
                        .map(this::convertToVehicleDTO)
                        .toList();
            }
        }
        logger.debug("Using mock vehicles");
        return mockDataService.getVehicles();
    }

    public List<OrderDTO> getOrders() {
        if (kernelClient.isConnected()) {
            List<Object> rawOrders = kernelClient.getOrders();
            if (!rawOrders.isEmpty()) {
                logger.info("Converting {} real orders from OpenTCS", rawOrders.size());
                return rawOrders.stream()
                        .map(this::convertToOrderDTO)
                        .toList();
            }
        }
        logger.debug("Using mock orders");
        return mockDataService.getOrders();
    }

    public List<LocationDTO> getLocations() {
        return mockDataService.getLocations();
    }

    public OrderDTO createOrder(String destination, String priority) {
        System.out.println("========================================");
        System.out.println("AgvService.createOrder called!");
        System.out.println("Thread: " + Thread.currentThread().getName());
        System.out.println("destination: " + destination);
        System.out.println("priority: " + priority);
        System.out.println("kernelClient: " + kernelClient);
        System.out.println("kernelClient.isConnected(): " + kernelClient.isConnected());
        System.out.println("========================================");
        
        logger.info("=== AgvService.createOrder START ===");
        logger.info("Thread: {}", Thread.currentThread().getName());
        logger.info("Destination: {}, Priority: {}", destination, priority);
        logger.info("kernelClient: {}", kernelClient);
        logger.info("kernelClient.isConnected(): {}", kernelClient.isConnected());
        
        if (kernelClient.isConnectedWithRetry()) {
            System.out.println(">>> Attempting to create order in OpenTCS kernel...");
            Object rawOrder = kernelClient.createTransportOrder(destination, "Move");
            System.out.println(">>> createTransportOrder returned: " + rawOrder);
            if (rawOrder != null) {
                System.out.println(">>> Created order in OpenTCS kernel");
                
                try {
                    java.lang.reflect.Method getNameMethod = rawOrder.getClass().getMethod("getName");
                    String orderName = (String) getNameMethod.invoke(rawOrder);
                    System.out.println(">>> Order name: " + orderName);
                    
                    List<VehicleDTO> vehicles = getVehicles();
                    String idleVehicle = null;
                    for (VehicleDTO vehicle : vehicles) {
                        if ("idle".equalsIgnoreCase(vehicle.getStatus())) {
                            idleVehicle = vehicle.getName();
                            break;
                        }
                    }
                    
                    if (idleVehicle != null) {
                        System.out.println(">>> Assigning order " + orderName + " to vehicle " + idleVehicle);
                        boolean assigned = kernelClient.assignTransportOrder(orderName, idleVehicle);
                        System.out.println(">>> Assignment result: " + assigned);
                    } else {
                        System.out.println(">>> No idle vehicles available");
                    }
                } catch (Exception e) {
                    System.out.println(">>> Error assigning order: " + e.getMessage());
                    e.printStackTrace();
                }
                
                return convertToOrderDTO(rawOrder);
            }
        }
        System.out.println(">>> Using mock createOrder");
        return mockDataService.createOrder(destination, priority);
    }

    public boolean executeOrder(String orderName) {
        if (kernelClient.isConnected()) {
            logger.info("Order execution in OpenTCS is automatic for transport orders");
            return true;
        }
        logger.debug("Using mock executeOrder");
        return mockDataService.executeOrder(orderName);
    }

    public boolean assignOrder(String orderName, String vehicleName) {
        if (kernelClient.isConnected()) {
            logger.info("Assigning order {} to vehicle {}", orderName, vehicleName);
            return kernelClient.assignTransportOrder(orderName, vehicleName);
        }
        logger.debug("Using mock assignOrder");
        return mockDataService.assignOrder(orderName, vehicleName);
    }

    public boolean cancelOrder(String orderName) {
        if (kernelClient.isConnected()) {
            return kernelClient.cancelTransportOrder(orderName);
        }
        logger.debug("Using mock cancelOrder");
        return mockDataService.cancelOrder(orderName);
    }

    public boolean connectToKernel() {
        return kernelClient.connect();
    }

    public boolean isKernelConnected() {
        return kernelClient.isConnected();
    }

    private VehicleDTO convertToVehicleDTO(Object rawVehicle) {
        try {
            java.lang.reflect.Method getNameMethod = rawVehicle.getClass().getMethod("getName");
            String name = (String) getNameMethod.invoke(rawVehicle);
            logger.debug("Vehicle name from OpenTCS: {}", name);

            java.lang.reflect.Method getStateMethod = rawVehicle.getClass().getMethod("getState");
            Object stateObj = getStateMethod.invoke(rawVehicle);
            String status = stateObj != null ? stateObj.toString().toLowerCase() : "unknown";
            logger.debug("Vehicle state: {}", status);

            java.lang.reflect.Method getEnergyLevelMethod = rawVehicle.getClass().getMethod("getEnergyLevel");
            Object energyLevelObj = getEnergyLevelMethod.invoke(rawVehicle);
            int energyLevel = 0;
            if (energyLevelObj instanceof Long) {
                energyLevel = ((Long) energyLevelObj).intValue();
            } else if (energyLevelObj instanceof Integer) {
                energyLevel = (Integer) energyLevelObj;
            } else if (energyLevelObj instanceof Double) {
                energyLevel = ((Double) energyLevelObj).intValue();
            }
            logger.debug("Vehicle energy level: {}", energyLevel);

            double speed = 0.0;
            try {
                java.lang.reflect.Method getVelocityMethod = rawVehicle.getClass().getMethod("getVelocity");
                Object velocityObj = getVelocityMethod.invoke(rawVehicle);
                if (velocityObj instanceof Double) {
                    speed = (Double) velocityObj;
                } else if (velocityObj instanceof Float) {
                    speed = ((Float) velocityObj).doubleValue();
                }
                logger.debug("Vehicle velocity: {}", speed);
            } catch (Exception e) {
                logger.debug("Velocity not available: {}", e.getMessage());
            }

            String position = "UNKNOWN";
            try {
                java.lang.reflect.Method getCurrentPositionMethod = rawVehicle.getClass().getMethod("getCurrentPosition");
                Object positionObj = getCurrentPositionMethod.invoke(rawVehicle);
                if (positionObj != null) {
                    java.lang.reflect.Method posGetNameMethod = positionObj.getClass().getMethod("getName");
                    position = (String) posGetNameMethod.invoke(positionObj);
                }
            } catch (Exception e) {
                logger.debug("Position not available: {}", e.getMessage());
            }

            String currentOrder = null;
            try {
                java.lang.reflect.Method getTransportOrderMethod = rawVehicle.getClass().getMethod("getTransportOrder");
                Object orderObj = getTransportOrderMethod.invoke(rawVehicle);
                if (orderObj != null) {
                    java.lang.reflect.Method orderGetNameMethod = orderObj.getClass().getMethod("getName");
                    currentOrder = (String) orderGetNameMethod.invoke(orderObj);
                }
            } catch (Exception e) {
                logger.debug("Transport order not available: {}", e.getMessage());
            }

            String currentPosition = "UNKNOWN";
            try {
                java.lang.reflect.Method getCurrentPositionMethod = rawVehicle.getClass().getMethod("getCurrentPosition");
                Object posObj = getCurrentPositionMethod.invoke(rawVehicle);
                if (posObj != null) {
                    java.lang.reflect.Method posNameMethod = posObj.getClass().getMethod("getName");
                    currentPosition = (String) posNameMethod.invoke(posObj);
                }
            } catch (Exception e) {
                logger.debug("Current position not available: {}", e.getMessage());
            }

            int vehicleId = vehicleIdCache.computeIfAbsent(name, k -> nextVehicleId++);

            return VehicleDTO.builder()
                    .id(String.valueOf(vehicleId))
                    .name(name)
                    .status(status)
                    .position(currentPosition)
                    .battery(energyLevel)
                    .speed(speed)
                    .currentOrder(currentOrder)
                    .pathIndex(0)
                    .build();

        } catch (Exception e) {
            logger.error("Error converting vehicle: {}", e.getMessage());
            e.printStackTrace();
            return VehicleDTO.builder()
                    .id("unknown")
                    .name("Unknown")
                    .status("unknown")
                    .position("UNKNOWN")
                    .battery(0)
                    .speed(0)
                    .build();
        }
    }

    private OrderDTO convertToOrderDTO(Object rawOrder) {
        try {
            java.lang.reflect.Method getNameMethod = rawOrder.getClass().getMethod("getName");
            String name = (String) getNameMethod.invoke(rawOrder);

            java.lang.reflect.Method getStateMethod = rawOrder.getClass().getMethod("getState");
            Object stateObj = getStateMethod.invoke(rawOrder);
            String status = stateObj != null ? stateObj.toString().toLowerCase() : "unknown";

            String priority = "normal";
            try {
                java.lang.reflect.Method getPriorityMethod = rawOrder.getClass().getMethod("getPriority");
                Object priorityObj = getPriorityMethod.invoke(rawOrder);
                priority = priorityObj != null ? priorityObj.toString().toLowerCase() : "normal";
            } catch (NoSuchMethodException e) {
                logger.debug("Priority not available: {}", e.getMessage());
            }

            String destination = "UNKNOWN";
            String destPosition = "UNKNOWN";
            try {
                java.lang.reflect.Method getDestinationsMethod = rawOrder.getClass().getMethod("getDestinations");
                Object destinations = getDestinationsMethod.invoke(rawOrder);
                if (destinations instanceof List<?> destList && !destList.isEmpty()) {
                    Object firstDest = destList.get(0);
                    java.lang.reflect.Method getDestinationMethod = firstDest.getClass().getMethod("getDestination");
                    Object locationObj = getDestinationMethod.invoke(firstDest);
                    if (locationObj != null) {
                        java.lang.reflect.Method locGetNameMethod = locationObj.getClass().getMethod("getName");
                        destination = (String) locGetNameMethod.invoke(locationObj);
                        destPosition = destination;
                    }
                }
            } catch (Exception e) {
                logger.debug("Destination not available: {}", e.getMessage());
            }

            java.time.LocalDateTime createdAt = null;
            try {
                java.lang.reflect.Method getCreationTimeMethod = rawOrder.getClass().getMethod("getCreationTime");
                Object timeObj = getCreationTimeMethod.invoke(rawOrder);
                if (timeObj instanceof java.time.ZonedDateTime zdt) {
                    createdAt = zdt.toLocalDateTime();
                } else if (timeObj instanceof java.util.Date date) {
                    createdAt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            } catch (Exception e) {
                logger.debug("Creation time not available: {}", e.getMessage());
            }

            String assignedVehicle = null;
            try {
                java.lang.reflect.Method getProcessingVehicleMethod = rawOrder.getClass().getMethod("getProcessingVehicle");
                Object vehicleObj = getProcessingVehicleMethod.invoke(rawOrder);
                if (vehicleObj != null) {
                    java.lang.reflect.Method vehicleGetNameMethod = vehicleObj.getClass().getMethod("getName");
                    assignedVehicle = (String) vehicleGetNameMethod.invoke(vehicleObj);
                }
            } catch (Exception e) {
                logger.debug("Processing vehicle not available: {}", e.getMessage());
            }

            int progress = "completed".equalsIgnoreCase(status) ? 100 :
                          "pending".equalsIgnoreCase(status) ? 0 : 50;

            int orderId = orderIdCache.computeIfAbsent(name, k -> nextOrderId++);

            return OrderDTO.builder()
                    .id(String.valueOf(orderId))
                    .name(name)
                    .destination(destination)
                    .destPosition(destPosition)
                    .priority(priority)
                    .status(status)
                    .createdAt(createdAt)
                    .assignedVehicle(assignedVehicle)
                    .progress(progress)
                    .build();

        } catch (Exception e) {
            logger.error("Error converting order: {}", e.getMessage());
            e.printStackTrace();
            return OrderDTO.builder()
                    .id("unknown")
                    .name("Unknown")
                    .destination("UNKNOWN")
                    .priority("normal")
                    .status("unknown")
                    .progress(0)
                    .build();
        }
    }
}