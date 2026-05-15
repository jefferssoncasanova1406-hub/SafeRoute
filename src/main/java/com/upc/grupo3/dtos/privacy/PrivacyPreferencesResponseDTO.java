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
    private String message;
}
