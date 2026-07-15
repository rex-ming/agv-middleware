package com.myagv.middleware.service;

import jakarta.annotation.PreDestroy;
import org.opentcs.access.rmi.ClientID;
import org.opentcs.access.rmi.services.RemoteKernelServicePortal;
import org.opentcs.access.rmi.services.RemoteTransportOrderService;
import org.opentcs.access.rmi.services.RemoteVehicleService;
import org.opentcs.access.to.order.DestinationCreationTO;
import org.opentcs.access.to.order.TransportOrderCreationTO;

import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.util.ClassMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@Component
public class OpenTCSKernelClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenTCSKernelClient.class);

    @Value("${opentcs.kernel.rmi.host:localhost}")
    private String rmiHost;

    @Value("${opentcs.kernel.rmi.port:1099}")
    private int rmiPort;

    @Value("${opentcs.kernel.connection-retry-delay:5000}")
    private long retryDelay;

    @Value("${opentcs.kernel.connection-max-retries:3}")
    private int maxRetries;

    @Value("${opentcs.kernel.username:Alice}")
    private String username;

    @Value("${opentcs.kernel.password:xyz}")
    private String password;

    private Registry registry;
    private RemoteKernelServicePortal kernelPortal;
    private RemoteVehicleService vehicleService;
    private RemoteTransportOrderService orderService;
    private ClientID clientId;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    public boolean connect() {
        if (connected.get()) {
            logger.info("Already connected to OpenTCS kernel");
            return true;
        }

        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                logger.info("Attempting to connect to OpenTCS kernel (attempt {}/{})", attempts + 1, maxRetries);
                
                registry = LocateRegistry.getRegistry(rmiHost, rmiPort);
                logger.info("✓ Connected to RMI registry at {}:{}", rmiHost, rmiPort);
                
                kernelPortal = (RemoteKernelServicePortal) registry.lookup(
                    "org.opentcs.access.rmi.services.RemoteKernelServicePortal"
                );
                logger.info("✓ Found RemoteKernelServicePortal");
                
                logger.info("[Step 2] Logging in...");
                
                java.lang.reflect.Method loginMethod = kernelPortal.getClass().getMethod(
                    "login", String.class, String.class, Predicate.class);
                
                Predicate<Object> eventFilter = new ClassMatcher(Object.class);
                Object result = loginMethod.invoke(kernelPortal, username, password, eventFilter);
                
                if (result instanceof ClientID) {
                    clientId = (ClientID) result;
                    logger.info("✓ Logged in successfully, ClientID: {}", clientId);
                }
                
                vehicleService = (RemoteVehicleService) registry.lookup(
                    "org.opentcs.access.rmi.services.RemoteVehicleService"
                );
                logger.info("✓ Found RemoteVehicleService");
                
                orderService = (RemoteTransportOrderService) registry.lookup(
                    "org.opentcs.access.rmi.services.RemoteTransportOrderService"
                );
                logger.info("✓ Found RemoteTransportOrderService");
                
                connected.set(true);
                logger.info("✅ Successfully connected to OpenTCS kernel");
                return true;
                
            } catch (RemoteException e) {
                attempts++;
                logger.warn("Failed to connect (RemoteException): {}", e.getMessage());
                
                if (attempts < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (NotBoundException e) {
                attempts++;
                logger.warn("Failed to find RMI service: {}", e.getMessage());
                
                if (attempts < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                attempts++;
                logger.error("Failed to connect (Exception): {}", e.getMessage());
                logger.error("Exception type: {}", e.getClass().getName());
                
                if (attempts < maxRetries) {
                    try {
                        logger.info("Retrying in {} ms...", retryDelay);
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.warn("Connection retry interrupted");
                        break;
                    }
                }
            }
        }
        
        logger.warn("Failed to connect to OpenTCS kernel after {} attempts, running in mock mode", maxRetries);
        return false;
    }

    public void disconnect() {
        if (kernelPortal != null && clientId != null) {
            try {
                kernelPortal.logout(clientId);
                logger.info("✓ Logged out from OpenTCS kernel");
            } catch (Exception e) {
                logger.warn("Error during logout: {}", e.getMessage());
            }
        }
        
        connected.set(false);
        registry = null;
        kernelPortal = null;
        vehicleService = null;
        orderService = null;
        clientId = null;
        logger.info("Disconnected from OpenTCS kernel");
    }

    public boolean isConnected() {
        boolean result = connected.get() && clientId != null && kernelPortal != null 
                        && vehicleService != null && orderService != null;
        logger.debug("isConnected() = {}, connected={}, clientId={}, kernelPortal={}, vehicleService={}, orderService={}", 
                    result, connected.get(), clientId != null, kernelPortal != null, 
                    vehicleService != null, orderService != null);
        return result;
    }

    public boolean isConnectedWithRetry() {
        if (connected.get() && clientId != null && kernelPortal != null 
            && vehicleService != null && orderService != null) {
            logger.debug("Connection check passed: connected={}, clientId={}, kernelPortal={}, vehicleService={}, orderService={}", 
                        connected.get(), clientId != null, kernelPortal != null, 
                        vehicleService != null, orderService != null);
            return true;
        }
        logger.warn("Connection check failed - attempting to reconnect...");
        return connect();
    }

    public List<Object> getVehicles() {
        if (!isConnected() || vehicleService == null) {
            return List.of();
        }
        try {
            logger.debug("Fetching vehicles from OpenTCS kernel...");
            
            Set<Vehicle> vehicles = vehicleService.fetch(clientId, Vehicle.class);
            List<Object> result = new ArrayList<>(vehicles);
            logger.info("✓ Fetched {} vehicles from OpenTCS kernel", result.size());
            return result;
            
        } catch (Exception e) {
            logger.error("Error fetching vehicles: {}", e.getMessage());
        }
        return List.of();
    }

    public List<Object> getOrders() {
        if (!isConnected() || orderService == null) {
            return List.of();
        }
        try {
            logger.debug("Fetching orders from OpenTCS kernel...");
            
            Set<TransportOrder> orders = orderService.fetch(clientId, TransportOrder.class);
            List<Object> result = new ArrayList<>(orders);
            logger.info("✓ Fetched {} orders from OpenTCS kernel", result.size());
            return result;
            
        } catch (Exception e) {
            logger.error("Error fetching orders: {}", e.getMessage());
        }
        return List.of();
    }

    public Object createTransportOrder(String destination, String operation) {
        logger.info("=== createTransportOrder START ===");
        logger.info("isConnected()={}, orderService={}", isConnected(), orderService);
        logger.info("clientId={}", clientId);
        
        if (!isConnected() || orderService == null) {
            logger.warn("Cannot create order: not connected or orderService is null");
            logger.info("=== createTransportOrder END (FAILED) ===");
            return null;
        }
        try {
            logger.info("Creating transport order to {} with operation {}", destination, operation);
            
            DestinationCreationTO destTO = null;
            if (destination.startsWith("Point-")) {
                try {
                    java.lang.reflect.Method method = DestinationCreationTO.class.getMethod("onlyDestination", String.class);
                    destTO = (DestinationCreationTO) method.invoke(null, destination);
                    logger.info("Created DestinationCreationTO using onlyDestination factory method: {}", destination);
                } catch (NoSuchMethodException e) {
                    logger.warn("onlyDestination factory method not found, trying constructor approach");
                    try {
                        java.lang.reflect.Constructor<?> constructor = DestinationCreationTO.class.getConstructor(String.class);
                        destTO = (DestinationCreationTO) constructor.newInstance(destination);
                        logger.info("Created DestinationCreationTO with only destination: {}", destination);
                    } catch (NoSuchMethodException ex) {
                        logger.warn("Constructor with only destination not found, trying with null operation");
                        destTO = new DestinationCreationTO(destination, null);
                    } catch (Exception ex) {
                        logger.error("Error creating DestinationCreationTO: {}", ex.getMessage());
                        return null;
                    }
                } catch (Exception e) {
                    logger.error("Error creating DestinationCreationTO: {}", e.getMessage());
                    return null;
                }
            } else {
                destTO = new DestinationCreationTO(destination, "");
            }
            List<DestinationCreationTO> destinations = List.of(destTO);
            
            String orderName = "Order-" + System.currentTimeMillis();
            TransportOrderCreationTO orderTO = null;
            
            List<Object> vehicles = getVehicles();
            String idleVehicle = null;
            for (Object vehicleObj : vehicles) {
                try {
                    java.lang.reflect.Method getStateMethod = vehicleObj.getClass().getMethod("getState");
                    Object stateObj = getStateMethod.invoke(vehicleObj);
                    String state = stateObj != null ? stateObj.toString().toLowerCase() : "unknown";
                    
                    if ("idle".equalsIgnoreCase(state)) {
                        java.lang.reflect.Method getNameMethod = vehicleObj.getClass().getMethod("getName");
                        idleVehicle = (String) getNameMethod.invoke(vehicleObj);
                        break;
                    }
                } catch (Exception e) {
                    logger.debug("Error checking vehicle state: {}", e.getMessage());
                }
            }
            
            orderTO = new TransportOrderCreationTO(orderName, destinations);
            
            if (idleVehicle != null) {
                logger.info("Setting intended vehicle: {}", idleVehicle);
                try {
                    java.lang.reflect.Method setIntendedVehicleMethod = orderTO.getClass().getMethod("setIntendedVehicleName", String.class);
                    setIntendedVehicleMethod.invoke(orderTO, idleVehicle);
                    logger.info("✓ Set intended vehicle via setter method");
                } catch (NoSuchMethodException e) {
                    logger.warn("setIntendedVehicleName method not available, trying builder pattern");
                    try {
                        java.lang.reflect.Method builderMethod = orderTO.getClass().getMethod("toBuilder");
                        Object builder = builderMethod.invoke(orderTO);
                        java.lang.reflect.Method setVehicleMethod = builder.getClass().getMethod("intendedVehicleName", String.class);
                        builder = setVehicleMethod.invoke(builder, idleVehicle);
                        java.lang.reflect.Method buildMethod = builder.getClass().getMethod("build");
                        orderTO = (TransportOrderCreationTO) buildMethod.invoke(builder);
                        logger.info("✓ Set intended vehicle via builder pattern");
                    } catch (Exception ex) {
                        logger.warn("Builder pattern also failed: {}", ex.getMessage());
                    }
                }
            } else {
                logger.info("No idle vehicle found, creating order without specified vehicle");
            }
            
            logger.debug("TransportOrderCreationTO created: {}", orderName);
            
            TransportOrder newOrder = orderService.createTransportOrder(clientId, orderTO);
            
            if (newOrder != null) {
                logger.info("✓ Created transport order: {}", newOrder.getName());
                return newOrder;
            } else {
                logger.warn("createTransportOrder returned null");
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error creating transport order: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean assignTransportOrder(String orderName, String vehicleName) {
        if (!isConnected() || orderService == null) {
            return false;
        }
        try {
            logger.info("Assigning transport order {} to vehicle {}", orderName, vehicleName);
            
            java.lang.reflect.Method assignMethod = orderService.getClass().getMethod(
                "assignTransportOrder", ClientID.class, String.class, String.class);
            assignMethod.invoke(orderService, clientId, orderName, vehicleName);
            
            logger.info("✓ Assigned transport order {} to vehicle {}", orderName, vehicleName);
            return true;
            
        } catch (NoSuchMethodException e) {
            logger.warn("assignTransportOrder method not available: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error assigning transport order: {}", e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean cancelTransportOrder(String orderName) {
        if (!isConnected() || orderService == null) {
            return false;
        }
        try {
            logger.info("Cancelling transport order: {}", orderName);
            
            java.lang.reflect.Method cancelMethod = orderService.getClass().getMethod(
                "cancelTransportOrder", ClientID.class, String.class);
            cancelMethod.invoke(orderService, clientId, orderName);
            
            logger.info("✓ Cancelled transport order: {}", orderName);
            return true;
            
        } catch (Exception e) {
            logger.error("Error cancelling transport order: {}", e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @PreDestroy
    public void cleanup() {
        disconnect();
    }
}