-- Megafarma · V5: datos del trabajador y cambio de contrasena obligatorio.
-- Nombres y apellidos por separado, DNI, telefono y correo de cada cuenta.
-- debe_cambiar_password = TRUE cuando el admin crea la cuenta o restablece la
-- contrasena: el trabajador entra con una clave temporal y debe cambiarla.

ALTER TABLE usuarios ADD COLUMN nombres VARCHAR(60);
ALTER TABLE usuarios ADD COLUMN apellidos VARCHAR(60);
ALTER TABLE usuarios ADD COLUMN dni VARCHAR(8);
ALTER TABLE usuarios ADD COLUMN telefono VARCHAR(9);
ALTER TABLE usuarios ADD COLUMN correo VARCHAR(120);
ALTER TABLE usuarios ADD COLUMN debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX uk_usuarios_dni ON usuarios (dni);
