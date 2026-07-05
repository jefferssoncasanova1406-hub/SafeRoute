INSERT INTO rol (nombre, descripcion, auditoria_fecha_creacion, auditoria_fecha_modificacion) VALUES
('admin', 'Administrador del sistema', '2026-05-12 08:00:00', '2026-05-12 08:00:00'),
('usuario', 'Usuario registrado', '2026-05-12 08:00:00', '2026-05-12 08:00:00')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO usuario (
    nombre,
    email,
    contrasena,
    fecha_registro,
    estado,
    rol_id_rol,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT
    'Ana Torres',
    'ana.torres@demo.com',
    '$2a$12$jCqb1oyU6cxRXsD/JYbR0u7nVO9ki.Hvtd37TcVPW7YLB8CTLO0pS',
    '2026-01-10',
    true,
    (SELECT id_rol FROM rol WHERE lower(nombre) = 'admin'),
    '2026-01-10 08:00:00',
    '2026-01-10 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE lower(email) = 'ana.torres@demo.com');

INSERT INTO usuario (
    nombre,
    email,
    contrasena,
    fecha_registro,
    estado,
    rol_id_rol,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT
    'Luis Rojas',
    'luis.rojas@demo.com',
    '$2a$12$jCqb1oyU6cxRXsD/JYbR0u7nVO9ki.Hvtd37TcVPW7YLB8CTLO0pS',
    '2026-01-15',
    true,
    (SELECT id_rol FROM rol WHERE lower(nombre) = 'usuario'),
    '2026-01-15 08:00:00',
    '2026-01-15 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE lower(email) = 'luis.rojas@demo.com');

INSERT INTO usuario (
    nombre,
    email,
    contrasena,
    fecha_registro,
    estado,
    rol_id_rol,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT
    'Carla Vega',
    'carla.vega@demo.com',
    '$2a$12$jCqb1oyU6cxRXsD/JYbR0u7nVO9ki.Hvtd37TcVPW7YLB8CTLO0pS',
    '2026-02-01',
    false,
    (SELECT id_rol FROM rol WHERE lower(nombre) = 'usuario'),
    '2026-02-01 08:00:00',
    '2026-02-01 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE lower(email) = 'carla.vega@demo.com');

INSERT INTO perfil (
    id_usuario,
    preferencias_riesg,
    radio_alerta,
    notificaciones_acti,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT u.id_usuario, 'alto', 1.5000000, true, '2026-05-12 08:00:00', '2026-05-12 08:00:00'
FROM usuario u
WHERE lower(u.email) = 'ana.torres@demo.com'
  AND NOT EXISTS (SELECT 1 FROM perfil p WHERE p.id_usuario = u.id_usuario);

INSERT INTO perfil (
    id_usuario,
    preferencias_riesg,
    radio_alerta,
    notificaciones_acti,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT u.id_usuario, 'medio', 1.0000000, true, '2026-05-12 08:00:00', '2026-05-12 08:00:00'
FROM usuario u
WHERE lower(u.email) = 'luis.rojas@demo.com'
  AND NOT EXISTS (SELECT 1 FROM perfil p WHERE p.id_usuario = u.id_usuario);

INSERT INTO perfil (
    id_usuario,
    preferencias_riesg,
    radio_alerta,
    notificaciones_acti,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT u.id_usuario, 'bajo', 0.7500000, false, '2026-05-12 08:00:00', '2026-05-12 08:00:00'
FROM usuario u
WHERE lower(u.email) = 'carla.vega@demo.com'
  AND NOT EXISTS (SELECT 1 FROM perfil p WHERE p.id_usuario = u.id_usuario);

INSERT INTO configuracion_privacidad (
    id_usuario,
    ubicacion_tiempo_real,
    compartir_datos_personales,
    fecha_actualizacion,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT u.id_usuario, true, false, '2026-05-12 08:00:00', '2026-05-12 08:00:00', '2026-05-12 08:00:00'
FROM usuario u
WHERE lower(u.email) = 'ana.torres@demo.com'
  AND NOT EXISTS (SELECT 1 FROM configuracion_privacidad c WHERE c.id_usuario = u.id_usuario);

INSERT INTO configuracion_privacidad (
    id_usuario,
    ubicacion_tiempo_real,
    compartir_datos_personales,
    fecha_actualizacion,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT u.id_usuario, true, false, '2026-05-12 08:00:00', '2026-05-12 08:00:00', '2026-05-12 08:00:00'
FROM usuario u
WHERE lower(u.email) = 'luis.rojas@demo.com'
  AND NOT EXISTS (SELECT 1 FROM configuracion_privacidad c WHERE c.id_usuario = u.id_usuario);

INSERT INTO configuracion_privacidad (
    id_usuario,
    ubicacion_tiempo_real,
    compartir_datos_personales,
    fecha_actualizacion,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT u.id_usuario, false, false, '2026-05-12 08:00:00', '2026-05-12 08:00:00', '2026-05-12 08:00:00'
FROM usuario u
WHERE lower(u.email) = 'carla.vega@demo.com'
  AND NOT EXISTS (SELECT 1 FROM configuracion_privacidad c WHERE c.id_usuario = u.id_usuario);

INSERT INTO ubicacion (
    latitud,
    longitud,
    distrito,
    ciudad,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT -12.0464000, -77.0428000, 'Cercado de Lima', 'Lima', '2026-05-12 08:00:00', '2026-05-12 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM ubicacion
    WHERE latitud = -12.0464000 AND longitud = -77.0428000 AND distrito = 'Cercado de Lima'
);

INSERT INTO ubicacion (
    latitud,
    longitud,
    distrito,
    ciudad,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT -12.1211000, -77.0305000, 'Miraflores', 'Lima', '2026-05-12 08:00:00', '2026-05-12 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM ubicacion
    WHERE latitud = -12.1211000 AND longitud = -77.0305000 AND distrito = 'Miraflores'
);

INSERT INTO ubicacion (
    latitud,
    longitud,
    distrito,
    ciudad,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT -12.0465000, -77.0440000, 'Cercado de Lima', 'Lima', '2026-05-12 08:00:00', '2026-05-12 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM ubicacion
    WHERE latitud = -12.0465000 AND longitud = -77.0440000 AND distrito = 'Cercado de Lima'
);

INSERT INTO ubicacion (
    latitud,
    longitud,
    distrito,
    ciudad,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
)
SELECT -12.0465000, -77.0415000, 'Cercado de Lima', 'Lima', '2026-05-12 08:00:00', '2026-05-12 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM ubicacion
    WHERE latitud = -12.0465000 AND longitud = -77.0415000 AND distrito = 'Cercado de Lima'
);

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
)
SELECT
    'ROBO',
    3,
    'Zona con alta incidencia reportada durante horario nocturno',
    true,
    '{"type":"Polygon","coordinates":[[[-77.0432000,-12.0469000],[-77.0423000,-12.0469000],[-77.0423000,-12.0460000],[-77.0432000,-12.0460000],[-77.0432000,-12.0469000]]]}',
    '2026-05-12 09:00:00',
    (SELECT id_ubicacion FROM ubicacion WHERE latitud = -12.0464000 AND longitud = -77.0428000 LIMIT 1),
    '2026-05-12 09:00:00',
    '2026-05-12 09:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM zona_riesgo
    WHERE tipo = 'ROBO' AND descripcion = 'Zona con alta incidencia reportada durante horario nocturno'
);

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
)
SELECT
    'ACOSO',
    2,
    'Zona en observacion por reportes recurrentes de acoso callejero',
    true,
    '{"type":"Polygon","coordinates":[[[-77.0310000,-12.1215000],[-77.0300000,-12.1215000],[-77.0300000,-12.1207000],[-77.0310000,-12.1207000],[-77.0310000,-12.1215000]]]}',
    '2026-05-12 09:15:00',
    (SELECT id_ubicacion FROM ubicacion WHERE latitud = -12.1211000 AND longitud = -77.0305000 LIMIT 1),
    '2026-05-12 09:15:00',
    '2026-05-12 09:15:00'
WHERE NOT EXISTS (
    SELECT 1 FROM zona_riesgo
    WHERE tipo = 'ACOSO' AND descripcion = 'Zona en observacion por reportes recurrentes de acoso callejero'
);

INSERT INTO incidente_ciudadano (
    id_alerta,
    tipo_incidente,
    descripcion,
    nivel_riesgo,
    fecha_emision,
    estado_lectura,
    estado_moderacion,
    origen,
    zona_afectada,
    reportante_id_usuario,
    auditoria_fecha_creacion,
    auditoria_fecha_modificacion
) VALUES
(
    101,
    'Robo',
    'Reporte de robo a mano armada cerca de tu ruta frecuente.',
    'ALTO',
    '2026-05-12 09:30:00',
    'NO_LEIDA',
    'APROBADO',
    'SISTEMA',
    'Santiago de Surco',
    null,
    '2026-05-12 09:30:00',
    '2026-05-12 09:30:00'
),
(
    102,
    'Accidente',
    'Colision vehicular bloquea interseccion segura.',
    'MEDIO',
    '2026-05-12 08:15:00',
    'LEIDA',
    'APROBADO',
    'SISTEMA',
    'San Miguel',
    null,
    '2026-05-12 08:15:00',
    '2026-05-12 08:15:00'
),
(
    103,
    'Asalto',
    'Actividad sospechosa reportada por la comunidad.',
    'ALTO',
    '2026-05-10 20:00:00',
    'LEIDA',
    'APROBADO',
    'SISTEMA',
    'Cercado de Lima',
    null,
    '2026-05-10 20:00:00',
    '2026-05-10 20:00:00'
)
ON CONFLICT (id_alerta) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('incidente_ciudadano', 'id_alerta'),
    GREATEST((SELECT COALESCE(MAX(id_alerta), 1) FROM incidente_ciudadano), 1)
);

-- Puntos sugeridos para probar la ruta segura con desvio:
-- origen  = { "latitud": -12.0465000, "longitud": -77.0440000 }
-- destino = { "latitud": -12.0465000, "longitud": -77.0415000 }
