package com.upc.grupo3.dtos.routeevaluation;

import java.time.LocalDateTime;
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
public class RouteEvaluateRequestDTO {

    private String origin;
    private String destination;
    private String transportMode;
    private LocalDateTime departureTime;
}
