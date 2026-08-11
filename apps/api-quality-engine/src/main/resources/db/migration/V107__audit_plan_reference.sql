-- Référence lisible des plans d'audit (§4.4), sur le modèle des non-conformités.
--
-- Un audit ne se désignait que par son identifiant technique. Or un audit se cite :
-- dans un compte rendu de revue de direction, dans un courriel de convocation, dans
-- un rapport remis à un organisme certificateur, à l'oral en réunion. « L'audit
-- 3f2a91c4-… » ne se dit pas, ne se lit pas, ne se recopie pas sans erreur — et deux
-- audits intitulés « Audit interne ISO 9001 » à six mois d'écart deviennent
-- indiscernables dès qu'on en parle.
--
-- Format AUD-{année}-{séquence par tenant}, identique dans son esprit à NC-2026-0001 :
-- l'année situe, la séquence ordonne, et la remise à zéro annuelle garde des numéros
-- courts. La séquence est par TENANT : deux organisations ne partagent pas leur
-- numérotation, et rien ne doit laisser deviner le volume d'audits d'un voisin.
ALTER TABLE audit_plans
    ADD COLUMN reference VARCHAR(40);

-- Rétro-numérotation des plans existants. L'ordre suit la date de création, qui est
-- l'ordre dans lequel l'organisation les a réellement ouverts ; `id` ne départage
-- que les créations de la même microseconde, pour rendre le résultat déterministe
-- (sans quoi deux exécutions sur deux répliques pourraient différer).
--
-- On numérote par (tenant, année de création) afin que la rétro-numérotation
-- produise exactement ce que le service produirait aujourd'hui : sans quoi le
-- premier audit créé après la migration entrerait en collision avec un ancien.
UPDATE audit_plans p
SET reference = s.generated
FROM (
    SELECT id,
           'AUD-' || EXTRACT(YEAR FROM created_at)::int || '-' ||
           LPAD(
               ROW_NUMBER() OVER (
                   PARTITION BY tenant_id, EXTRACT(YEAR FROM created_at)
                   ORDER BY created_at, id
               )::text, 4, '0') AS generated
    FROM audit_plans
) s
WHERE p.id = s.id;

ALTER TABLE audit_plans
    ALTER COLUMN reference SET NOT NULL;

-- Unicité par tenant, comme pour les non-conformités : c'est cette contrainte qui
-- fait de la référence une désignation, et non un simple libellé. Le service
-- regénère et retente sur collision, ce que seule cette contrainte peut détecter
-- de façon fiable quand deux créations concurrentes tombent sur le même numéro.
ALTER TABLE audit_plans
    ADD CONSTRAINT uq_audit_plans_reference UNIQUE (tenant_id, reference);
