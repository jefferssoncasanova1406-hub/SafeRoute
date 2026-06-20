package com.upc.grupo3.dtos.routeevaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStepDTO {

    private Integer order;
    private String instruction;
    private String streetName;
    private Double distanceMeters;
    private Double durationSeconds;
    private String maneuverType;
    private String modifier;
}
