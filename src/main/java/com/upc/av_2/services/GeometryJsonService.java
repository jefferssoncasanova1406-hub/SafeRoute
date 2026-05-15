package com.upc.av_2.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.av_2.exceptions.ApplicationConfigurationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeometryJsonService {

    private final ObjectMapper objectMapper;

    public <T> T read(String json, Class<T> type, String logMessage, String exceptionMessage) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            log.error(logMessage, exception);
            throw new ApplicationConfigurationException(exceptionMessage);
        }
    }

    public String write(Object value, String logMessage, String exceptionMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            log.error(logMessage, exception);
            throw new ApplicationConfigurationException(exceptionMessage);
        }
    }
}
