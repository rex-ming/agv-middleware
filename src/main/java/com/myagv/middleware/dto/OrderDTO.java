package com.myagv.middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("destPosition")
    private String destPosition;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("status")
    private String status;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("assignedVehicle")
    private String assignedVehicle;

    @JsonProperty("progress")
    private int progress;
}