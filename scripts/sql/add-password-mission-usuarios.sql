-- Migración: añadir columna password_mission a usuarios (v0.13.0)
-- Uso: psql -U postgres -d qnt_spring -f scripts/sql/add-password-mission-usuarios.sql

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'usuarios' AND column_name = 'password_mission'
  ) THEN
    ALTER TABLE usuarios ADD COLUMN password_mission VARCHAR(30) NULL;
    COMMENT ON COLUMN usuarios.password_mission IS 'Clave/contraseña para misiones; relevante para pilotos. Máx. 30 caracteres.';
  END IF;
END $$;
