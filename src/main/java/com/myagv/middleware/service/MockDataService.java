package com.myagv.middleware.service;

import com.myagv.middleware.dto.LocationDTO;
import com.myagv.middleware.dto.OrderDTO;
import com.myagv.middleware.dto.VehicleDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MockDataService {

    private final List<VehicleDTO> mockVehicles = new ArrayList<>();
    private final List<OrderDTO> mockOrders = new ArrayList<>();
    private final List<LocationDTO> mockLocations = new ArrayList<>();
    private final AtomicInteger orderCounter = new AtomicInteger(1);

    private static final Map<String, String> DEST_POSITIONS = Map.of(
        "WORKSTATION-A", "B3",
        "WORKSTATION-B", "D4",
        "WORKSTATION-C", "F2",
        "STORAGE", "A1",
        "LOADING", "G3",
        "CHARGING", "E1"
    );

    public MockDataService() {
        initializeMockData();
    }

    private void initializeMockData() {
        mockLocations.addAll(List.of(
            LocationDTO.builder().name("A1").type("location").label("仓库").x(0).y(0).build(),
            LocationDTO.builder().name("B3").type("location").label("工位A").x(1).y(2).build(),
            LocationDTO.builder().name("D4").type("location").label("工位B").x(3).y(3).build(),
            LocationDTO.builder().name("F2").type("location").label("工位C").x(5).y(1).build(),
            LocationDTO.builder().name("E1").type("charging").label("充电").x(4).y(0).build(),
            LocationDTO.builder().name("G3").type("location").label("装卸").x(6).y(2).build(),
            LocationDTO.builder().name("C2").type("location").label("待命").x(2).y(1).build(),
            LocationDTO.builder().name("F1").type("location").label("阻塞").x(5).y(0).build()
        ));

        mockVehicles.addAll(List.of(
            VehicleDTO.builder().id("1").name("AGV-001").status("idle").position("A1").battery(85).speed(0).build(),
            VehicleDTO.builder().id("2").name("AGV-002").status("executing").position("B1").battery(62).speed(1.2).currentOrder("ORD-001").build(),
            VehicleDTO.builder().id("3").name("AGV-003").status("charging").position("E1").battery(25).speed(0).build(),
            VehicleDTO.builder().id("4").name("AGV-004").status("idle").position("C2").battery(93).speed(0).build(),
            VehicleDTO.builder().id("5").name("AGV-005").status("executing").position("C1").battery(47).speed(1.5).currentOrder("ORD-005").build(),
            VehicleDTO.builder().id("6").name("AGV-006").status("blocked").position("F1").battery(71).speed(0).build()
        ));

        mockOrders.addAll(List.of(
            OrderDTO.builder().id("1").name("ORD-001").destination("WORKSTATION-A").destPosition("B3").priority("high").status("active").createdAt(LocalDateTime.now().minusMinutes(5)).assignedVehicle("AGV-002").progress(40).build(),
            OrderDTO.builder().id("2").name("ORD-002").destination("STORAGE").destPosition("A1").priority("normal").status("pending").createdAt(LocalDateTime.now().minusMinutes(1)).assignedVehicle(null).progress(0).build(),
            OrderDTO.builder().id("3").name("ORD-003").destination("WORKSTATION-B").destPosition("D4").priority("low").status("pending").createdAt(LocalDateTime.now().minusMinutes(2)).assignedVehicle(null).progress(0).build(),
            OrderDTO.builder().id("4").name("ORD-004").destination("LOADING").destPosition("G3").priority("normal").status("completed").createdAt(LocalDateTime.now().minusMinutes(30)).assignedVehicle("AGV-001").progress(100).build(),
            OrderDTO.builder().id("5").name("ORD-005").destination("WORKSTATION-C").destPosition("F2").priority("high").status("active").createdAt(LocalDateTime.now().minusMinutes(2)).assignedVehicle("AGV-005").progress(20).build()
        ));
    }

    public List<VehicleDTO> getVehicles() {
        return new ArrayList<>(mockVehicles);
    }

    public List<OrderDTO> getOrders() {
        return new ArrayList<>(mockOrders);
    }

    public List<LocationDTO> getLocations() {
        return new ArrayList<>(mockLocations);
    }

    public OrderDTO createOrder(String destination, String priority) {
        String orderName = "WEB-ORDER-" + System.currentTimeMillis();
        OrderDTO newOrder = OrderDTO.builder()
            .id(String.valueOf(orderCounter.getAndIncrement()))
            .name(orderName)
            .destination(destination)
            .destPosition(DEST_POSITIONS.getOrDefault(destination, "A1"))
            .priority(priority != null ? priority : "normal")
            .status("pending")
            .createdAt(LocalDateTime.now())
            .assignedVehicle(null)
            .progress(0)
            .build();
        
        mockOrders.add(0, newOrder);
        return newOrder;
    }

    public boolean executeOrder(String orderName) {
        OrderDTO order = mockOrders.stream()
            .filter(o -> o.getName().equals(orderName))
            .findFirst()
            .orElse(null);
        
        if (order != null && "pending".equals(order.getStatus())) {
            order.setStatus("active");
            
            VehicleDTO idleVehicle = mockVehicles.stream()
                .filter(v -> "idle".equals(v.getStatus()) && "A1".equals(v.getPosition()))
                .findFirst()
                .orElse(null);
            
            if (idleVehicle != null) {
                idleVehicle.setStatus("executing");
                idleVehicle.setCurrentOrder(orderName);
                order.setAssignedVehicle(idleVehicle.getName());
            }
            return true;
        }
        return false;
    }

    public boolean assignOrder(String orderName, String vehicleName) {
        OrderDTO order = mockOrders.stream()
            .filter(o -> o.getName().equals(orderName))
            .findFirst()
            .orElse(null);
        
        VehicleDTO vehicle = mockVehicles.stream()
            .filter(v -> v.getName().equals(vehicleName))
            .findFirst()
            .orElse(null);
        
        if (order != null && vehicle != null) {
            order.setAssignedVehicle(vehicleName);
            vehicle.setCurrentOrder(orderName);
            vehicle.setStatus("executing");
            order.setStatus("active");
            return true;
        }
        return false;
    }

    public boolean cancelOrder(String orderName) {
        OrderDTO order = mockOrders.stream()
            .filter(o -> o.getName().equals(orderName))
            .findFirst()
            .orElse(null);
        
        if (order != null) {
            order.setStatus("completed");
            order.setProgress(100);
            
            VehicleDTO vehicle = mockVehicles.stream()
                .filter(v -> orderName.equals(v.getCurrentOrder()))
                .findFirst()
                .orElse(null);
            
            if (vehicle != null) {
                vehicle.setStatus("idle");
                vehicle.setCurrentOrder(null);
            }
            return true;
        }
        return false;
    }

    public void updateVehicleStatus(String vehicleName, String status) {
        mockVehicles.stream()
            .filter(v -> v.getName().equals(vehicleName))
            .findFirst()
            .ifPresent(v -> v.setStatus(status));
    }
}