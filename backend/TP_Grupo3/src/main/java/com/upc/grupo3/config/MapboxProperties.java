package com.upc.grupo3.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mapbox")
@Getter
@Setter
public class MapboxProperties {

    private String accessToken;
    private String geocodingUrl;
    private String directionsUrl;
}
