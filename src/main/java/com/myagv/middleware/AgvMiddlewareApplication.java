package com.myagv.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgvMiddlewareApplication {

    private static final Logger logger = LoggerFactory.getLogger(AgvMiddlewareApplication.class);

    public static void main(String[] args) {
        logger.info("Starting AGV Middleware Application...");
        try {
            SpringApplication.run(AgvMiddlewareApplication.class, args);
            logger.info("AGV Middleware Application started successfully");
        } catch (Exception e) {
            logger.error("Error starting AGV Middleware Application: {}", e.getMessage(), e);
            throw e;
        }
    }
}