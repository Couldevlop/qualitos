-- Ce qu'un client a souscrit : la vérité COMMERCIALE.
--
-- L'activation de module dans le moteur qualité en est la conséquence
-- technique. Les deux vivent dans des bases différentes et ne se joignent
-- jamais : l'abonnement décide, l'activation applique. Une jointure entre les
-- deux obligerait à les héberger ensemble, et ferait dépendre la facturation
-- de la disponibilité du moteur.
CREATE TABLE subscriptions (
    id             UUID PRIMARY KEY,
    tenant_id      UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    module_code    VARCHAR(64)  NOT NULL,
    billing_tier   VARCHAR(32)  NOT NULL,
    period         VARCHAR(16)  NOT NULL,
    -- Le prix est FIGÉ à la souscription, et non relu du tarif courant : une
    -- hausse de tarif ne doit réécrire ni le montant d'un contrat déjà signé,
    -- ni celui des factures déjà émises.
    amount_cents   BIGINT       NOT NULL,
    currency       CHAR(3)      NOT NULL,
    started_on     DATE         NOT NULL,
    next_renewal   DATE         NOT NULL,
    cancelled_at   TIMESTAMPTZ,
    cancelled_by   UUID,
    created_at     TIMESTAMPTZ  NOT NULL,
    created_by     UUID         NOT NULL,

    CONSTRAINT chk_sub_tier   CHECK (billing_tier IN ('FREE','STANDARD','PRO','ENTERPRISE')),
    CONSTRAINT chk_sub_period CHECK (period IN ('MONTHLY','ANNUAL')),
    CONSTRAINT chk_sub_amount CHECK (amount_cents >= 0),
    -- L'échéance suit le début : un renouvellement antérieur au début décrirait
    -- un contrat déjà échu le jour de sa signature.
    CONSTRAINT chk_sub_renewal_after_start CHECK (next_renewal > started_on),
    -- Une résiliation est un acte : elle a une date ET un auteur, ou elle n'a
    -- eu lieu ni l'une ni l'autre. Une moitié de résiliation laisserait un
    -- abonnement fermé que personne n'aurait fermé.
    CONSTRAINT chk_sub_cancellation_complete CHECK (
        (cancelled_at IS NULL AND cancelled_by IS NULL)
        OR (cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)
    )
);

-- Un seul abonnement VIVANT par client et par module. L'index partiel dit la
-- règle : un abonnement résilié reste en base — c'est l'historique qui
-- justifie les factures passées — mais il ne fait plus obstacle à une nouvelle
-- souscription. Même dessin que `uk_tma_open_per_tenant_module` côté moteur.
CREATE UNIQUE INDEX uk_subscription_vivante
    ON subscriptions (tenant_id, module_code)
    WHERE cancelled_at IS NULL;

-- L'échéancier : « quels contrats arrivent à terme ». Partiel lui aussi, un
-- abonnement résilié n'ayant plus d'échéance à surveiller.
CREATE INDEX idx_subscriptions_renewal ON subscriptions (next_renewal)
    WHERE cancelled_at IS NULL;

-- La lecture la plus fréquente : les abonnements vivants d'un client, que
-- l'émission d'une facture parcourt pour chaque période.
CREATE INDEX idx_subscriptions_tenant ON subscriptions (tenant_id)
    WHERE cancelled_at IS NULL;
