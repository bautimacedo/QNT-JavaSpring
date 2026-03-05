-- Inserta el Dock EDO-DOCK1 (Cañadón Amarillo) con coordenadas para el mapa.
-- Si no existe ningún site, se crea uno 'Cañadón Amarillo'.
-- Uso: psql -U qntgestion -d postgres_qnt -f scripts/sql/insert-dock-edo-dock1.sql

-- Asegurar que exista al menos un site para el dock
INSERT INTO sites (nombre)
SELECT 'Cañadón Amarillo'
WHERE NOT EXISTS (SELECT 1 FROM sites LIMIT 1);

-- Insertar el dock
INSERT INTO docks (
    nombre,
    modelo,
    numero_serie,
    estado,
    site_id,
    latitud,
    longitud
) VALUES (
    'EDO-DOCK1',
    'DJI Matrice4TD',
    'LV-R2135',
    'STOCK_ACTIVO',
    (SELECT id FROM sites ORDER BY id LIMIT 1),
    -39.006066994173,
    -67.881342056706
);
