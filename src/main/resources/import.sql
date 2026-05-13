TRUNCATE TABLE ruta_segura_zona, ruta_segura, zona_riesgo, ubicacion, configuracion_privacidad, perfil, usuario, rol RESTART IDENTITY CASCADE;

INSERT INTO rol (nombre, descripcion, auditoria_fecha_creacion, auditoria_fecha_modificacion) VALUES
('admin', 'Administrador del sistema', '2026-05-12 08:00:00', '2026-05-12 08:00:00'),
('usuario', 'Usuario registrado', '2026-05-12 08:00:00', '2026-05-12 08:00:00');

INSERT INTO usuario (
    nombre,
    email,
    contrasena,
    fecha_registro,
    estado,
    rol_id_rol,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
) VALUES
('Ana Torres', 'ana.torres@demo.com', '$2a$12$qQoMn8NycNiny5CuWlCSPe/EEgCj5EmR2DUdBmqO4C5bG096iCNW2', '2026-01-10', true, 1, '2026-01-10 08:00:00', '2026-01-10 08:00:00'),
('Luis Rojas', 'luis.rojas@demo.com', '$2a$10$9x4uQBOjJkw3Xrq5JclZgOn7rH0xR271GGBqBPdZiZsaAJ2bI7IuW', '2026-01-15', true, 2, '2026-01-15 08:00:00', '2026-01-15 08:00:00'),
('Carla Vega', 'carla.vega@demo.com', '$2a$10$kFz2QxYDs2fzGEylh4G6U.xiRaY1oCbcnZ6FQcmGeXgnz9KqSXBRe', '2026-02-01', false, 2, '2026-02-01 08:00:00', '2026-02-01 08:00:00');

INSERT INTO perfil (
    id_usuario,
    preferencias_riesg,
    radio_alerta,
    notificaciones_acti,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
) VALUES
(1, 'alto', 1.5000000, true, '2026-05-12 08:00:00', '2026-05-12 08:00:00'),
(2, 'medio', 1.0000000, true, '2026-05-12 08:00:00', '2026-05-12 08:00:00'),
(3, 'bajo', 0.7500000, false, '2026-05-12 08:00:00', '2026-05-12 08:00:00');

INSERT INTO configuracion_privacidad (
    id_usuario,
    ubicacion_tiempo_real,
    compartir_datos_personales,
    fecha_actualizacion,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
) VALUES
(1, true, false, '2026-05-12 08:00:00', '2026-05-12 08:00:00', '2026-05-12 08:00:00'),
(2, true, false, '2026-05-12 08:00:00', '2026-05-12 08:00:00', '2026-05-12 08:00:00'),
(3, false, false, '2026-05-12 08:00:00', '2026-05-12 08:00:00', '2026-05-12 08:00:00');

INSERT INTO ubicacion (
    latitud,
    longitud,
    distrito,
    ciudad,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
) VALUES
(-12.0464000, -77.0428000, 'Cercado de Lima', 'Lima', '2026-05-12 08:00:00', '2026-05-12 08:00:00'),
(-12.1211000, -77.0305000, 'Miraflores', 'Lima', '2026-05-12 08:00:00', '2026-05-12 08:00:00');

INSERT INTO zona_riesgo (
    tipo,
    nivel_riesgo,
    descripcion,
    estado,
    coordenadas_geojson,
    fecha_actualizacion,
    ubicacion_id_ubicacion,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
) VALUES
(
    'ROBO',
    3,
    'Zona con alta incidencia reportada durante horario nocturno',
    true,
    '{"type":"Polygon","coordinates":[[[-77.0432000,-12.0469000],[-77.0423000,-12.0469000],[-77.0423000,-12.0460000],[-77.0432000,-12.0460000],[-77.0432000,-12.0469000]]]}',
    '2026-05-12 09:00:00',
    1,
    '2026-05-12 09:00:00',
    '2026-05-12 09:00:00'
),
(
    'ACOSO',
    2,
    'Zona en observacion por reportes recurrentes de acoso callejero',
    true,
    '{"type":"Polygon","coordinates":[[[-77.0310000,-12.1215000],[-77.0300000,-12.1215000],[-77.0300000,-12.1207000],[-77.0310000,-12.1207000],[-77.0310000,-12.1215000]]]}',
    '2026-05-12 09:15:00',
    2,
    '2026-05-12 09:15:00',
    '2026-05-12 09:15:00'
);
