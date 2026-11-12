-- Pólizas (los id se generan automáticamente: 1, 2, 3, 4)
INSERT INTO poliza (numero, tipo, estado, tomador, fecha_inicio, fecha_fin, meses_vigencia, canon_mensual, prima)
VALUES ('POL-IND-001', 'INDIVIDUAL', 'ACTIVA', 'Carlos Pérez', '2026-01-01', '2026-12-31', 12, 1500000.00, 18000000.00);

INSERT INTO poliza (numero, tipo, estado, tomador, fecha_inicio, fecha_fin, meses_vigencia, canon_mensual, prima)
VALUES ('POL-COL-001', 'COLECTIVA', 'ACTIVA', 'Inmobiliaria Los Andes S.A.S.', '2026-03-01', '2027-02-28', 12, 2000000.00, 24000000.00);

INSERT INTO poliza (numero, tipo, estado, tomador, fecha_inicio, fecha_fin, meses_vigencia, canon_mensual, prima)
VALUES ('POL-COL-002', 'COLECTIVA', 'ACTIVA', 'Administración Conjunto El Prado', '2026-06-01', '2026-11-30', 6, 1800000.00, 10800000.00);

INSERT INTO poliza (numero, tipo, estado, tomador, fecha_inicio, fecha_fin, meses_vigencia, canon_mensual, prima)
VALUES ('POL-IND-002', 'INDIVIDUAL', 'CANCELADA', 'Laura Gómez', '2025-07-01', '2026-06-30', 12, 1200000.00, 14400000.00);

-- Riesgos
INSERT INTO riesgo (poliza_id, direccion_inmueble, arrendatario, estado, fecha_creacion)
VALUES (1, 'Calle 100 # 15-20, Apto 301, Bogotá', 'Carlos Pérez', 'ACTIVO', '2026-01-01');

INSERT INTO riesgo (poliza_id, direccion_inmueble, arrendatario, estado, fecha_creacion)
VALUES (2, 'Carrera 7 # 72-10, Apto 502, Bogotá', 'Andrés Rojas', 'ACTIVO', '2026-03-01');

INSERT INTO riesgo (poliza_id, direccion_inmueble, arrendatario, estado, fecha_creacion)
VALUES (2, 'Calle 85 # 11-40, Apto 1203, Bogotá', 'María Torres', 'ACTIVO', '2026-03-15');

INSERT INTO riesgo (poliza_id, direccion_inmueble, arrendatario, estado, fecha_creacion)
VALUES (3, 'Avenida 19 # 120-30, Casa 8, Bogotá', 'Julián Castro', 'ACTIVO', '2026-06-01');

INSERT INTO riesgo (poliza_id, direccion_inmueble, arrendatario, estado, fecha_creacion)
VALUES (4, 'Calle 53 # 24-15, Apto 204, Bogotá', 'Laura Gómez', 'CANCELADO', '2025-07-01');