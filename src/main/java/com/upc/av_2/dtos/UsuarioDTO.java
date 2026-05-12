package com.upc.av_2.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
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
public class UsuarioDTO {

    private Integer idUsuario;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 255, message = "La contrasena debe tener entre 8 y 255 caracteres")
    private String contrasena;

    @NotNull(message = "La fecha de registro es obligatoria")
    @PastOrPresent(message = "La fecha de registro no puede ser futura")
    private LocalDate fechaRegistro;

    @NotNull(message = "El estado es obligatorio")
    private Boolean estado;

    @NotNull(message = "El rol es obligatorio")
    private Integer rolIdRol;
}
