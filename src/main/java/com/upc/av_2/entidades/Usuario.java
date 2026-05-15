package com.upc.av_2.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Usuario")
    private Integer idUsuario;

    @Column(name = "Nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "Email", nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "Contrasena", nullable = false, length = 255)
    private String contrasena;

    @Column(name = "Fecha_Registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(name = "Estado", nullable = false)
    private Boolean estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Rol_id_rol", nullable = false)
    private Rol rol;

    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY)
    private Perfil perfil;

    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY)
    private ConfiguracionPrivacidad configuracionPrivacidad;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Ruta> rutas = new ArrayList<>();
}
