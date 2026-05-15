package com.upc.grupo3.dtos.riskzone;

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
public class RiskZoneOperationResponseDTO {

    private String message;
    private RiskZoneDetailDTO zona;
}
