-- ==============================================================================
-- Migração V9: CPF do familiar passa a ser opcional
-- O familiar agora nasce do evento user.created (cadastro no identity-service /
-- Keycloak), que não traz CPF. O CPF é completado depois, no perfil. A restrição
-- UNIQUE continua valendo para os CPFs informados (o PostgreSQL aceita vários NULL).
-- ==============================================================================

ALTER TABLE familiares ALTER COLUMN cpf DROP NOT NULL;
