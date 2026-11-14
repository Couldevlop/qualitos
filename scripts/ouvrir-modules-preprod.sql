-- Ouvre les modules optionnels d'un client en préproduction.
--
-- POURQUOI CE FICHIER EXISTE, ET POURQUOI IL EST TEMPORAIRE.
-- La voie normale est la surface plateforme du moteur
-- (`POST /api/v1/platform/tenants/{id}/modules/{code}`, réservée au super
-- administrateur). Elle répond 500 depuis que l'isolation par la base est
-- active : la connexion annonce le client du super administrateur pendant
-- qu'on écrit la ligne d'un autre, et la politique refuse. Le correctif
-- (`TenantScope`) attend son déploiement ; d'ici là, la préproduction reste
-- vide et indémontrable. Ce fichier écrit exactement ce que la route aurait
-- écrit, rien de plus.
--
-- Idempotent : relançable sans rien casser, grâce à l'index d'unicité
-- `uk_tma_open_per_tenant_module` (une seule ligne non terminale par client et
-- par module).
--
--   kubectl cp scripts/ouvrir-modules-preprod.sql <pod>:/tmp/ouvrir-modules.sql
--   psql -U qualitos -d qualitos_quality -f /tmp/ouvrir-modules.sql

\set ON_ERROR_STOP on

BEGIN;

-- Le client visé et l'acteur, nommés une fois.
CREATE TEMP TABLE _cible (tenant_id uuid, acteur uuid) ON COMMIT DROP;
INSERT INTO _cible VALUES (
    'fd3c738c-e111-4ec1-956b-04c5aa18cb55',   -- OPENLAB CONSULTING
    'f7281691-93c1-4e93-9ecc-5a9ff1ef522b');  -- le super administrateur qui l'a décidé

-- Le palier de chaque module est celui du catalogue (`ModuleCatalog`) : la
-- ligne doit dire la même chose que ce que la route aurait inscrit.
CREATE TEMP TABLE _modules (code varchar(64), palier varchar(32)) ON COMMIT DROP;
INSERT INTO _modules VALUES
    ('circle', 'STANDARD'), ('dmaic', 'STANDARD'),
    ('product', 'STANDARD'), ('risk', 'STANDARD'), ('controlplan', 'STANDARD'),
    ('supplier', 'STANDARD'), ('training', 'STANDARD'), ('change', 'STANDARD'),
    ('complaints', 'STANDARD'), ('calibration', 'STANDARD'), ('ehs', 'STANDARD'),
    ('kpi', 'STANDARD'), ('webhooks', 'STANDARD'),
    ('standards', 'PRO'), ('industry', 'PRO'), ('iot', 'PRO'), ('itsm', 'PRO');

INSERT INTO tenant_module_activations (
    id, tenant_id, module_code, status, billing_tier, configuration_json,
    trial_ends_at, expires_at, activated_at, activated_by,
    status_changed_at, last_changed_by, updated_at)
SELECT gen_random_uuid(), c.tenant_id, m.code, 'ACTIVE', m.palier, NULL,
       NULL, NULL, now(), c.acteur, now(), c.acteur, now()
  FROM _modules m CROSS JOIN _cible c
 WHERE NOT EXISTS (
       SELECT 1 FROM tenant_module_activations a
        WHERE a.tenant_id = c.tenant_id
          AND a.module_code = m.code
          AND a.status NOT IN ('EXPIRED', 'DISABLED'));

COMMIT;

SELECT module_code, status, billing_tier
  FROM tenant_module_activations
 WHERE tenant_id = 'fd3c738c-e111-4ec1-956b-04c5aa18cb55'
 ORDER BY module_code;
