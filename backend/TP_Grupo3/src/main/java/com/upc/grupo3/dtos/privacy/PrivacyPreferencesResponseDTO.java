package com.upc.grupo3.dtos.privacy;

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
public class PrivacyPreferencesResponseDTO {

    private Integer userId;
    private Boolean realTimeLocationEnabled;
    private Boolean personalDataSharingEnabled;

    // HU17: Retorno de preferencias detalladas
    private Boolean appNotificationsEnabled;
    private Boolean emailNotificationsEnabled;
    private Integer minRiskLevel;
    private String incidentTypesFiltered;

    private String message; // Para cumplir el Escenario 3 (Confirmación exitosa)
}
