package com.upc.av_2.config;

import com.upc.av_2.dtos.ApiErrorDTO;
import com.upc.av_2.exceptions.ApplicationConfigurationException;
import com.upc.av_2.exceptions.EmailAlreadyRegisteredException;
import com.upc.av_2.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorDTO> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        log.warn("ResponseStatusException status={} path={} message={}",
                status.value(), request.getRequestURI(), exception.getReason());
        ApiErrorDTO error = buildError(
                status,
                exception.getReason() != null ? exception.getReason() : "Error en la solicitud",
                request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(LinkedHashMap::new,
                        (map, error) -> map.put(error.getField(), error.getDefaultMessage()),
                        Map::putAll);
        log.warn("Validacion fallida path={} details={}", request.getRequestURI(), details);

        ApiErrorDTO error = buildError(
                HttpStatus.BAD_REQUEST,
                "Datos invalidos",
                details,
                request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorDTO> handleEmailAlreadyRegisteredException(
            EmailAlreadyRegisteredException exception,
            HttpServletRequest request) {
        log.warn("Registro rechazado por correo duplicado path={} message={}",
                request.getRequestURI(), exception.getMessage());
        ApiErrorDTO error = buildError(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        log.warn("Recurso no encontrado path={} message={}", request.getRequestURI(), exception.getMessage());
        ApiErrorDTO error = buildError(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ApplicationConfigurationException.class)
    public ResponseEntity<ApiErrorDTO> handleApplicationConfigurationException(
            ApplicationConfigurationException exception,
            HttpServletRequest request) {
        log.error("Error de configuracion path={} message={}", request.getRequestURI(), exception.getMessage());
        ApiErrorDTO error = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        String message = "Conflicto de integridad de datos";
        String causeMessage = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage()
                : exception.getMessage();

        if (causeMessage != null && causeMessage.toLowerCase().contains("email")) {
            message = "El correo ya esta registrado";
        }
        log.warn("Conflicto de integridad path={} message={} cause={}",
                request.getRequestURI(), message, causeMessage);

        ApiErrorDTO error = buildError(
                HttpStatus.CONFLICT,
                message,
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        log.warn("JSON invalido path={} message={}", request.getRequestURI(), exception.getMessage());
        ApiErrorDTO error = buildError(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no tiene un formato JSON valido",
                request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleGenericException(
            Exception exception,
            HttpServletRequest request) {
        log.error("Error interno no controlado path={}", request.getRequestURI(), exception);
        ApiErrorDTO error = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error interno en el servidor",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ApiErrorDTO buildError(HttpStatus status, String message, String path) {
        return buildError(status, message, null, path);
    }

    private ApiErrorDTO buildError(
            HttpStatus status,
            String message,
            Map<String, String> details,
            String path) {
        return ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .details(details)
                .path(path)
                .build();
    }
}
