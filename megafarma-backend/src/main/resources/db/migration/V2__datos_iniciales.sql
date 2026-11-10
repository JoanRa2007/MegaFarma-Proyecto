-- Megafarma · Datos iniciales: roles, categorias y catalogo base.
-- Los usuarios y los lotes de demostracion los crea DatosIniciales
-- (necesita BCrypt y fechas relativas al dia de hoy).

INSERT INTO roles (nombre, descripcion) VALUES
    ('ADMIN', 'Administrador: acceso total'),
    ('VENDEDOR', 'Vendedor: ventas, clientes y consulta de catalogo y stock'),
    ('ALMACENERO', 'Almacenero: catalogo, entradas por lote, ajustes y bajas');

INSERT INTO categorias (nombre, descripcion) VALUES
    ('Analgésicos', 'Alivio del dolor y la fiebre'),
    ('Antibióticos', 'Tratamiento de infecciones bacterianas'),
    ('Antiinflamatorios', 'Reducción de la inflamación'),
    ('Antialérgicos', 'Antihistamínicos'),
    ('Gastrointestinal', 'Antiácidos y protectores gástricos'),
    ('Vitaminas', 'Suplementos y vitaminas'),
    ('Cuidado personal', 'Higiene y cuidado');

INSERT INTO productos (codigo, nombre, principio_activo, presentacion, laboratorio, precio_venta, stock_minimo, requiere_receta, categoria_id, creado_en, actualizado_en) VALUES
    ('MED-0001', 'Paracetamol 500 mg', 'Paracetamol', 'Caja x 100 tabletas', 'Genfar', 0.20, 100, FALSE, (SELECT id FROM categorias WHERE nombre = 'Analgésicos'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0002', 'Ibuprofeno 400 mg', 'Ibuprofeno', 'Caja x 100 tabletas', 'Farmindustria', 0.40, 80, FALSE, (SELECT id FROM categorias WHERE nombre = 'Antiinflamatorios'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0003', 'Amoxicilina 500 mg', 'Amoxicilina', 'Caja x 100 cápsulas', 'Portugal', 0.60, 60, TRUE, (SELECT id FROM categorias WHERE nombre = 'Antibióticos'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0004', 'Azitromicina 500 mg', 'Azitromicina', 'Caja x 3 tabletas', 'Medifarma', 4.50, 20, TRUE, (SELECT id FROM categorias WHERE nombre = 'Antibióticos'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0005', 'Loratadina 10 mg', 'Loratadina', 'Caja x 100 tabletas', 'Genfar', 0.30, 50, FALSE, (SELECT id FROM categorias WHERE nombre = 'Antialérgicos'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0006', 'Cetirizina 10 mg', 'Cetirizina', 'Caja x 100 tabletas', 'Farmindustria', 0.35, 50, FALSE, (SELECT id FROM categorias WHERE nombre = 'Antialérgicos'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0007', 'Omeprazol 20 mg', 'Omeprazol', 'Caja x 100 cápsulas', 'Portugal', 0.50, 60, FALSE, (SELECT id FROM categorias WHERE nombre = 'Gastrointestinal'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0008', 'Sal de Andrews', 'Bicarbonato de sodio', 'Caja x 50 sobres', 'GSK', 1.00, 30, FALSE, (SELECT id FROM categorias WHERE nombre = 'Gastrointestinal'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0009', 'Naproxeno 550 mg', 'Naproxeno sódico', 'Caja x 100 tabletas', 'Medifarma', 0.80, 40, FALSE, (SELECT id FROM categorias WHERE nombre = 'Antiinflamatorios'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0010', 'Diclofenaco 50 mg', 'Diclofenaco', 'Caja x 100 tabletas', 'Genfar', 0.25, 60, FALSE, (SELECT id FROM categorias WHERE nombre = 'Antiinflamatorios'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0011', 'Vitamina C 500 mg', 'Ácido ascórbico', 'Frasco x 100 tabletas', 'Hersil', 18.00, 10, FALSE, (SELECT id FROM categorias WHERE nombre = 'Vitaminas'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0012', 'Complejo B', 'Vitaminas B1, B6, B12', 'Caja x 100 tabletas', 'Hersil', 0.45, 40, FALSE, (SELECT id FROM categorias WHERE nombre = 'Vitaminas'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0013', 'Ciprofloxacino 500 mg', 'Ciprofloxacino', 'Caja x 100 tabletas', 'Portugal', 0.90, 40, TRUE, (SELECT id FROM categorias WHERE nombre = 'Antibióticos'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0014', 'Clorfenamina 4 mg', 'Clorfenamina maleato', 'Caja x 100 tabletas', 'Genfar', 0.15, 60, FALSE, (SELECT id FROM categorias WHERE nombre = 'Antialérgicos'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0015', 'Metamizol 500 mg', 'Metamizol sódico', 'Caja x 100 tabletas', 'Farmindustria', 0.30, 60, FALSE, (SELECT id FROM categorias WHERE nombre = 'Analgésicos'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MED-0016', 'Ranitidina 300 mg', 'Ranitidina', 'Caja x 100 tabletas', 'Medifarma', 0.40, 30, FALSE, (SELECT id FROM categorias WHERE nombre = 'Gastrointestinal'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CUI-0001', 'Alcohol medicinal 70 grados 250 ml', NULL, 'Frasco 250 ml', 'Alkofarma', 4.00, 20, FALSE, (SELECT id FROM categorias WHERE nombre = 'Cuidado personal'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CUI-0002', 'Algodón hidrófilo 100 g', NULL, 'Bolsa 100 g', 'CKF', 5.50, 15, FALSE, (SELECT id FROM categorias WHERE nombre = 'Cuidado personal'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CUI-0003', 'Mascarilla quirúrgica', NULL, 'Caja x 50 unidades', 'Medical Plus', 0.50, 100, FALSE, (SELECT id FROM categorias WHERE nombre = 'Cuidado personal'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CUI-0004', 'Suero rehidratante 1 L', 'Electrolitos', 'Botella 1 L', 'Electrolight', 6.50, 24, FALSE, (SELECT id FROM categorias WHERE nombre = 'Gastrointestinal'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
