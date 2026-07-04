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
public class ResolvedPlaceDTO {

    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
}
