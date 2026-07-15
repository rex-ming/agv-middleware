package com.myagv.middleware.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Enumeration;

// @Component
// @Order(1)
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("========================================");
        System.out.println("RequestLoggingFilter initialized!");
        System.out.println("========================================");
        logger.info("RequestLoggingFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String method = httpRequest.getMethod();
        String requestURI = httpRequest.getRequestURI();
        String queryString = httpRequest.getQueryString();
        String fullUrl = queryString != null ? requestURI + "?" + queryString : requestURI;
        
        System.out.println("========================================");
        System.out.println("[REQUEST] Method: " + method);
        System.out.println("[REQUEST] URL: " + fullUrl);
        System.out.println("[REQUEST] Remote Address: " + httpRequest.getRemoteAddr());
        System.out.println("[REQUEST] Content-Type: " + httpRequest.getContentType());
        System.out.println("[REQUEST] Content-Length: " + httpRequest.getContentLength());
        
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        System.out.println("[REQUEST] Headers:");
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println("  " + headerName + ": " + httpRequest.getHeader(headerName));
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[RESPONSE] Status: " + httpResponse.getStatus());
            System.out.println("[RESPONSE] Duration: " + duration + "ms");
            System.out.println("========================================");
            logger.info("Request completed: {} {} - Status: {} - Duration: {}ms", 
                       method, requestURI, httpResponse.getStatus(), duration);
        }
    }

    @Override
    public void destroy() {
        logger.info("RequestLoggingFilter destroyed");
    }
}