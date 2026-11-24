ALTER TABLE products
  ADD COLUMN IF NOT EXISTS distance_km INT UNSIGNED NOT NULL DEFAULT 0;

UPDATE products
SET distance_km = CASE slug
  WHEN 'puertomontt' THEN 1030
  WHEN 'valparaiso' THEN 120
  WHEN 'laserena' THEN 475
  WHEN 'concepcion' THEN 500
  WHEN 'pucon' THEN 785
  WHEN 'chiloe' THEN 1150
  WHEN 'talca' THEN 255
  WHEN 'pichilemu' THEN 210
  WHEN 'quintero' THEN 160
  WHEN 'vina' THEN 130
  WHEN 'antofagasta' THEN 1365
  WHEN 'arica' THEN 2050
  WHEN 'valdivia' THEN 848
  ELSE distance_km
END;

ALTER TABLE tickets
  ADD COLUMN IF NOT EXISTS distance_km INT UNSIGNED NOT NULL DEFAULT 0;

UPDATE tickets t
JOIN products p ON p.id = t.product_id
SET t.distance_km = p.distance_km
WHERE t.distance_km = 0;
