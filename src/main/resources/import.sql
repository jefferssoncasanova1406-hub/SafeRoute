TRUNCATE TABLE perfil, incidente, ruta, usuario, rol RESTART IDENTITY CASCADE;

INSERT INTO rol (nombre, descripcion) VALUES
('admin', 'Administrador del sistema'),
('usuario', 'Usuario registrado');

INSERT INTO usuario (nombre, email, contrasena, fecha_registro, estado, rol_id_rol) VALUES
('Ana Torres', 'ana.torres@demo.com', '$2a$10$2wOE8T6EyxJ0E5htSGcVPeY5vJGfveHFkNjEcNWef39/C4R2tQeM6', '2026-01-10', true, 1),
('Luis Rojas', 'luis.rojas@demo.com', '$2a$10$9x4uQBOjJkw3Xrq5JclZgOn7rH0xR271GGBqBPdZiZsaAJ2bI7IuW', '2026-01-15', true, 2),
('Carla Vega', 'carla.vega@demo.com', '$2a$10$kFz2QxYDs2fzGEylh4G6U.xiRaY1oCbcnZ6FQcmGeXgnz9KqSXBRe', '2026-02-01', false, 2);

INSERT INTO perfil (id_usuario, preferencias_riesg, radio_alerta, notificaciones_acti) VALUES
(1, 'alto', 1.5000000, true),
(2, 'medio', 1.0000000, true),
(3, 'bajo', 0.7500000, false);

INSERT INTO ruta (nivel_seguridad, distancia, tiempo_estimado, usuario_id_usuari) VALUES
(5, 12, 25, 1),
(4, 8, 18, 1),
(3, 15, 35, 2),
(2, 20, 45, 3);

INSERT INTO incidente (tipo_incidente, descripcion, fecha_incidente, fuente, ubicacion_id_ubicacio) VALUES
('Robo', 'Reporte de robo menor en la zona comercial', '2026-03-05', 'Serenazgo', 101),
('Accidente', 'Choque entre dos vehiculos sin heridos graves', '2026-03-18', 'Policia', 102),
('Congestion', 'Trafico intenso por cierre temporal de via', '2026-04-02', 'Waze', 103),
('Obra', 'Trabajos de mantenimiento con desvio peatonal', '2026-04-12', 'Municipalidad', 104);
