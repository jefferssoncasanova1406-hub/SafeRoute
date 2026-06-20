package com.upc.grupo3.dtos.routeevaluation;

import java.time.LocalDateTime;
import java.util.List;
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
public class RouteEvaluateResponseDTO {

    private ResolvedPlaceDTO originResolved;
    private ResolvedPlaceDTO destinationResolved;
    private String transportMode;
    private LocalDateTime departureTime;
    private List<RouteOptionDTO> routes;
}
