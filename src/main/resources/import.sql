TRUNCATE TABLE perfil, usuario, rol RESTART IDENTITY CASCADE;

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
