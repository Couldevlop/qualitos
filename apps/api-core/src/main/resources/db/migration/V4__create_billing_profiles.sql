-- L'identité de FACTURATION d'un client, distincte de son identité technique.
--
-- `tenants` porte ce qui fait tourner l'application : un slug, un plan, un
-- drapeau actif. Rien de ce qu'il faut pour émettre une facture — ni raison
-- sociale, ni adresse, ni numéro de TVA, ni destinataire. On ne facture pas un
-- UUID.
--
-- Table SÉPARÉE et non colonnes ajoutées à `tenants` : un tenant existe dès
-- l'inscription, son profil de facturation n'arrive qu'à la signature. Les
-- fondre obligerait à inventer des valeurs vides le jour de la création.
CREATE TABLE billing_profiles (
    id                UUID PRIMARY KEY,
    -- Un profil par client. La contrainte le dit, plutôt que de laisser deux
    -- profils cohabiter et facturer deux fois.
    tenant_id         UUID         NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    legal_name        VARCHAR(250) NOT NULL,
    -- Numéro de TVA intracommunautaire : facultatif (un client hors UE n'en a
    -- pas), mais contraint quand il est là.
    vat_number        VARCHAR(64),
    address_line1     VARCHAR(250) NOT NULL,
    address_line2     VARCHAR(250),
    postal_code       VARCHAR(32)  NOT NULL,
    city              VARCHAR(120) NOT NULL,
    country_code      CHAR(2)      NOT NULL,
    billing_email     VARCHAR(320) NOT NULL,
    -- Devise du contrat. Portée par le CLIENT et non par la ligne : un même
    -- client ne se facture pas en euros un mois et en dollars le suivant.
    currency          CHAR(3)      NOT NULL DEFAULT 'EUR',
    -- Exemption commerciale : le compte de démonstration ne se facture pas.
    -- Colonne et non code en dur : demain un client pilote ou un partenaire
    -- pourra l'être aussi, sans livraison.
    billing_exempt    BOOLEAN      NOT NULL DEFAULT FALSE,
    exemption_reason  VARCHAR(250),
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_billing_currency  CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_billing_country   CHECK (country_code ~ '^[A-Z]{2}$'),
    -- "@" tout seul passait le LIKE '%@%' initial sans etre une adresse : au
    -- moins un caractere de part et d'autre du "@", et un point dans le
    -- domaine, sans quoi la case reste vide plus tard.
    CONSTRAINT chk_billing_email     CHECK (
        billing_email ~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$'),
    -- Une exemption sans motif est une anomalie qu'un audit relèvera : on ne
    -- renonce pas à facturer sans dire pourquoi. btrim(...) <> '' : un motif
    -- vide ou compose uniquement d'espaces n'en est pas un — IS NOT NULL
    -- seul laissait passer '' et '   '.
    CONSTRAINT chk_exemption_motivee CHECK (
        billing_exempt = FALSE
        OR (exemption_reason IS NOT NULL AND btrim(exemption_reason) <> ''))
);

CREATE INDEX idx_billing_profiles_tenant ON billing_profiles (tenant_id);
