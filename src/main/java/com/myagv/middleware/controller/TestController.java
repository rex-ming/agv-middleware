package com.myagv.middleware.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/simple-ping")
    public ResponseEntity<Map<String, String>> simplePing() {
        return ResponseEntity.ok(Map.of("message", "pong", "success", "true"));
    }
}