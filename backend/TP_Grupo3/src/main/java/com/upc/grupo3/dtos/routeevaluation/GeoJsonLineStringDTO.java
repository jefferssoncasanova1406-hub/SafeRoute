package com.upc.grupo3.dtos.routeevaluation;

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
public class GeoJsonLineStringDTO {

    private String type;
    private List<List<Double>> coordinates;
}
