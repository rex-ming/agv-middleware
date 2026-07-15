package com.myagv.middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDTO {

    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

    @JsonProperty("position")
    private String position;

    @JsonProperty("battery")
    private int battery;

    @JsonProperty("speed")
    private double speed;

    @JsonProperty("currentOrder")
    private String currentOrder;

    @JsonProperty("pathIndex")
    private int pathIndex;
}