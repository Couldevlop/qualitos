-- Le catalogue normatif était exclusivement PLATEFORME : livré par migrations,
-- partagé par tous les tenants, jamais écrit par eux. Il accueille désormais
-- aussi les référentiels d'un tenant — la procédure interne qu'il audite.
--
-- NULL = norme de la plateforme (ISO 9001…), inchangée et lisible par tous.
-- Renseigné = référentiel appartenant à ce tenant, invisible aux autres.
ALTER TABLE standards ADD COLUMN owner_tenant_id UUID;

-- Le document GED dont le référentiel est né. On garde AUSSI la version publiée
-- au moment de la création : un rapport d'audit doit pouvoir citer la version de
-- la procédure qui était en vigueur, pas celle d'aujourd'hui.
ALTER TABLE standards ADD COLUMN source_document_id UUID;
ALTER TABLE standards ADD COLUMN source_document_version INTEGER;

-- L'unicité globale du code n'a plus de sens : deux tenants peuvent parfaitement
-- appeler leur procédure « PRO-002 ». La contrainte d'origine (V9) s'appelle
-- uk_standards_code ; on couvre aussi le nom par défaut PostgreSQL au cas où
-- l'environnement l'aurait générée sans nom explicite.
ALTER TABLE standards DROP CONSTRAINT IF EXISTS uk_standards_code;
ALTER TABLE standards DROP CONSTRAINT IF EXISTS standards_code_key;
DROP INDEX IF EXISTS uk_standards_code;
DROP INDEX IF EXISTS standards_code_key;

-- DEUX index partiels, et non un seul UNIQUE (owner_tenant_id, code).
-- PIÈGE : en PostgreSQL, NULL n'est jamais égal à NULL. Un index composite
-- laisserait donc passer DEUX normes plateforme portant le même code — exactement
-- la garantie qu'on croyait conserver.
CREATE UNIQUE INDEX uk_standards_platform_code
    ON standards (code) WHERE owner_tenant_id IS NULL;
CREATE UNIQUE INDEX uk_standards_tenant_code
    ON standards (owner_tenant_id, code) WHERE owner_tenant_id IS NOT NULL;

-- Un tenant ne crée qu'un seul référentiel par procédure : le second serait un
-- doublon silencieux, avec deux scores d'alignement divergents pour un même texte.
CREATE UNIQUE INDEX uk_standards_source_document
    ON standards (source_document_id) WHERE source_document_id IS NOT NULL;

CREATE INDEX idx_standards_owner_tenant ON standards (owner_tenant_id);
