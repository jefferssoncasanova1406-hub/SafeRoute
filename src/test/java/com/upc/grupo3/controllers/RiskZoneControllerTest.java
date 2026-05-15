package com.upc.grupo3.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.upc.grupo3.dtos.riskzone.RiskZoneDetailDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneOperationResponseDTO;
import com.upc.grupo3.services.RiskZoneService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class RiskZoneControllerTest {

    @Mock
    private RiskZoneService riskZoneService;

    @Mock
    private Authentication authentication;

    @Test
    void deleteRiskZoneShouldDelegateToSoftDeleteService() {
        RiskZoneController controller = new RiskZoneController(riskZoneService);
        RiskZoneOperationResponseDTO serviceResponse = RiskZoneOperationResponseDTO.builder()
                .message("Zona de riesgo desactivada correctamente")
                .zona(RiskZoneDetailDTO.builder()
                        .idZona(5)
                        .estado("INACTIVA")
                        .build())
                .build();

        when(authentication.getName()).thenReturn("ana.torres@demo.com");
        when(riskZoneService.deactivateRiskZone("ana.torres@demo.com", 5)).thenReturn(serviceResponse);

        ResponseEntity<RiskZoneOperationResponseDTO> response = controller.deleteRiskZone(5, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(riskZoneService).deactivateRiskZone("ana.torres@demo.com", 5);
    }
}
