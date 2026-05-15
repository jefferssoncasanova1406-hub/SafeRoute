package com.upc.grupo3.dtos.riskzone;

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
public class RiskZoneListResponseDTO {

    private String message;
    private List<RiskZoneDetailDTO> zonas;
}
