package com.upc.av_2.dtos;

import java.time.LocalDateTime;
import java.util.Map;
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
public class ApiErrorDTO {

    private LocalDateTime timestamp;
    private Integer status;
    private String error;
    private String message;
    private Map<String, String> details;
    private String path;
}
