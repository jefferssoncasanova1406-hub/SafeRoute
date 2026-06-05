package com.upc.grupo3.dtos.privacy;

import jakarta.validation.constraints.NotNull;
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
public class PrivacyPreferencesRequestDTO {

    // HU17: Requerimientos específicos de alertas y notificaciones
    @NotNull(message = "La preferencia de notificaciones en la app es obligatoria")
    private Boolean appNotificationsEnabled;

    @NotNull(message = "La preferencia de notificaciones por correo es obligatoria")
    private Boolean emailNotificationsEnabled;

    @NotNull(message = "El nivel mínimo de riesgo a alertar es obligatorio")
    private Integer minRiskLevel; // 1: Bajo, 2: Medio, 3: Alto

    private String incidentTypesFiltered; // Ej: "robo,asalto,accidente"

    @NotNull(message = "La preferencia de ubicacion en tiempo real es obligatoria")
    private Boolean realTimeLocationEnabled;

    @NotNull(message = "La preferencia de comparticion de datos personales es obligatoria")
    private Boolean personalDataSharingEnabled;
}
