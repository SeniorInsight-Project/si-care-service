-- ==============================================================================
-- Migração V7: identidade passa a ser do Keycloak (realm seniorinsight)
-- O care-service deixa de guardar contas e senhas: familiares e médicos passam a
-- ser ligados ao usuário do Keycloak pela claim sub (keycloak_sub), e as tabelas
-- usuarios e tokens_recuperacao deixam de existir.
-- Os valores antigos de usuario_id viram texto (usuario_id::text): só há dados de
-- desenvolvimento, que deixam de casar com um sub real do Keycloak.
-- ==============================================================================

-- 1. Familiares: guarda o e-mail (antes vinha de usuarios) e troca usuario_id por keycloak_sub
ALTER TABLE familiares ADD COLUMN email VARCHAR(320);
UPDATE familiares f SET email = u.email FROM usuarios u WHERE u.id = f.usuario_id;

ALTER TABLE familiares DROP CONSTRAINT IF EXISTS familiares_usuario_id_fkey;
ALTER TABLE familiares DROP CONSTRAINT IF EXISTS familiares_usuario_id_key;
DROP INDEX IF EXISTS idx_familiares_usuario_id;
ALTER TABLE familiares ALTER COLUMN usuario_id TYPE VARCHAR(64) USING usuario_id::text;
ALTER TABLE familiares RENAME COLUMN usuario_id TO keycloak_sub;
ALTER TABLE familiares ALTER COLUMN keycloak_sub SET NOT NULL;
ALTER TABLE familiares ADD CONSTRAINT uq_familiares_keycloak_sub UNIQUE (keycloak_sub);
CREATE INDEX idx_familiares_keycloak_sub ON familiares(keycloak_sub);
CREATE INDEX idx_familiares_email ON familiares(email);

-- 2. Médicos: troca usuario_id por keycloak_sub
ALTER TABLE medicos DROP CONSTRAINT IF EXISTS medicos_usuario_id_fkey;
ALTER TABLE medicos DROP CONSTRAINT IF EXISTS medicos_usuario_id_key;
DROP INDEX IF EXISTS idx_medicos_usuario_id;
ALTER TABLE medicos ALTER COLUMN usuario_id TYPE VARCHAR(64) USING usuario_id::text;
ALTER TABLE medicos RENAME COLUMN usuario_id TO keycloak_sub;
ALTER TABLE medicos ALTER COLUMN keycloak_sub SET NOT NULL;
ALTER TABLE medicos ADD CONSTRAINT uq_medicos_keycloak_sub UNIQUE (keycloak_sub);
CREATE INDEX idx_medicos_keycloak_sub ON medicos(keycloak_sub);

-- 3. Convites: quem emitiu passa a ser o sub do Keycloak
ALTER TABLE convites_rede DROP CONSTRAINT IF EXISTS convites_rede_criado_por_id_fkey;
ALTER TABLE convites_rede ALTER COLUMN criado_por_id TYPE VARCHAR(64) USING criado_por_id::text;
ALTER TABLE convites_rede RENAME COLUMN criado_por_id TO criado_por_sub;

-- 4. Histórico de medicação: quem registrou passa a ser o sub do Keycloak, com o nome para exibição
ALTER TABLE historicos_medicacao ADD COLUMN registrado_por_nome VARCHAR(255);
UPDATE historicos_medicacao h SET registrado_por_nome = u.email FROM usuarios u WHERE u.id = h.registrado_por_id;
ALTER TABLE historicos_medicacao DROP CONSTRAINT IF EXISTS historicos_medicacao_registrado_por_id_fkey;
ALTER TABLE historicos_medicacao ALTER COLUMN registrado_por_id TYPE VARCHAR(64) USING registrado_por_id::text;
ALTER TABLE historicos_medicacao RENAME COLUMN registrado_por_id TO registrado_por_sub;

-- 5. Login, senha, SSO e recuperação de senha agora são do Keycloak
DROP TABLE tokens_recuperacao;
DROP TABLE usuarios;
