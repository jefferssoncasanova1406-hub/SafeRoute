package com.upc.av_2.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @NotBlank(message = "La propiedad app.jwt.secret es obligatoria")
    private String secret;

    @Positive(message = "La propiedad app.jwt.expiration-ms debe ser mayor que cero")
    private long expirationMs;
}
