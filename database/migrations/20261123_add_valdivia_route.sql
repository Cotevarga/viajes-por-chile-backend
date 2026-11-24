INSERT INTO products (id, slug, title, destination, price, type, status, image, description, gallery)
SELECT CONCAT('PROD-', LPAD(COALESCE(MAX(CAST(SUBSTRING(id, 6) AS UNSIGNED)), 0) + 1, 2, '0')),
       'valdivia', 'Santiago - Valdivia', 'Valdivia', 22000, 'Salón Cama', 'Activo',
       'Valdivia.jpeg', 'Ríos navegables, bosques y arquitectura patrimonial en el corazón de la Región de Los Ríos.',
       '["Valdivia.jpeg","Valdi.JPG"]'
FROM products
HAVING NOT EXISTS (SELECT 1 FROM products WHERE slug = 'valdivia');
