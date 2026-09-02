-- Le tarif d'un module, par palier et par périodicité.
--
-- En BASE et non en constante du code : un tarif change sans livraison, et le
-- changer par un déploiement ferait dépendre la politique commerciale du cycle
-- de release.
--
-- Le prix ANNUEL est stocké, jamais calculé depuis le mensuel : une remise
-- annuelle est une décision commerciale, pas une multiplication par douze.
CREATE TABLE module_prices (
    id           UUID PRIMARY KEY,
    module_code  VARCHAR(64)  NOT NULL,
    billing_tier VARCHAR(32)  NOT NULL,
    period       VARCHAR(16)  NOT NULL,
    amount_cents BIGINT       NOT NULL,
    currency     CHAR(3)      NOT NULL DEFAULT 'EUR',
    updated_at   TIMESTAMPTZ  NOT NULL,
    updated_by   UUID         NOT NULL,

    CONSTRAINT uk_module_price UNIQUE (module_code, billing_tier, period),
    CONSTRAINT chk_price_tier   CHECK (billing_tier IN ('FREE','STANDARD','PRO','ENTERPRISE')),
    CONSTRAINT chk_price_period CHECK (period IN ('MONTHLY','ANNUAL')),
    -- Un prix nul est légitime (palier FREE) ; un prix négatif ne l'est pas.
    CONSTRAINT chk_price_amount CHECK (amount_cents >= 0)
);
-- Pas d'index supplémentaire : la contrainte UNIQUE ci-dessus pose déjà
-- l'index composite (module_code, billing_tier, period) qu'exige la
-- recherche de priceOf().
