package com.upc.av_2.dtos;

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

    @NotNull(message = "La preferencia de ubicacion en tiempo real es obligatoria")
    private Boolean realTimeLocationEnabled;

    @NotNull(message = "La preferencia de comparticion de datos personales es obligatoria")
    private Boolean personalDataSharingEnabled;
}
