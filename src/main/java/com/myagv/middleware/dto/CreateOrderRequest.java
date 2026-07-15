package com.myagv.middleware.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private String name;

    @NotBlank(message = "Destination is required")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.]+$", 
             message = "Invalid destination. Only letters, numbers, spaces, hyphens, underscores and periods are allowed.")
    private String destination;

    @Pattern(regexp = "^(high|normal|low)$", message = "Invalid priority. Must be high, normal, or low")
    @Builder.Default
    private String priority = "normal";
}