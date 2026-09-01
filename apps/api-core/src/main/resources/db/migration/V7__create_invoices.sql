-- Les factures émises, et leurs lignes.
--
-- Une facture est une pièce COMPTABLE : une fois émise, elle ne se corrige
-- plus, elle s'annule par une autre pièce. C'est pourquoi tout ce qui la
-- compose est figé à l'émission — le montant, le libellé du module, le palier —
-- et recopié depuis l'abonnement plutôt que joint à lui. Une jointure ferait
-- changer une facture de l'an dernier le jour où le contrat change.
CREATE TABLE invoices (
    id            UUID PRIMARY KEY,
    -- PAS de ON DELETE CASCADE, contrairement à `subscriptions` : une facture
    -- survit à son client. Supprimer un tenant échouera désormais tant qu'il
    -- reste des factures à son nom, et c'est le comportement voulu — effacer
    -- une pièce comptable pour faire de la place est précisément ce qu'un
    -- contrôle fiscal cherche.
    tenant_id     UUID         NOT NULL REFERENCES tenants(id),

    -- Le numéro. Unique sur TOUTE la table, tous clients confondus : la
    -- séquence est celle de l'éditeur, pas celle d'un client. Une séquence par
    -- client produirait autant de « facture n° 1 » que de clients.
    number        VARCHAR(24)  NOT NULL,
    fiscal_year   INTEGER      NOT NULL,

    -- La période facturée, en année + mois. Deux colonnes entières plutôt
    -- qu'une DATE au premier du mois : une DATE inviterait à y écrire un jour
    -- quelconque, et deux factures de « septembre » ne se ressembleraient plus.
    period_year   INTEGER      NOT NULL,
    period_month  INTEGER      NOT NULL,

    currency      CHAR(3)      NOT NULL,
    total_cents   BIGINT       NOT NULL,

    issued_at     TIMESTAMPTZ  NOT NULL,
    issued_by     UUID         NOT NULL,
    -- L'envoi : sa date et son destinataire, ou ni l'un ni l'autre. Le
    -- destinataire est recopié parce que l'adresse de facturation du profil
    -- peut changer, et qu'il faut pouvoir dire à QUI la facture est partie.
    sent_at       TIMESTAMPTZ,
    sent_to       VARCHAR(320),

    CONSTRAINT chk_invoice_total  CHECK (total_cents >= 0),
    CONSTRAINT chk_invoice_month  CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT chk_invoice_year   CHECK (period_year BETWEEN 2000 AND 2999),
    CONSTRAINT chk_invoice_number CHECK (number ~ '^FA-[0-9]{4}-[0-9]{4,}$'),
    CONSTRAINT chk_invoice_sending_complete CHECK (
        (sent_at IS NULL AND sent_to IS NULL)
        OR (sent_at IS NOT NULL AND sent_to IS NOT NULL)
    ),
    -- Le numéro dit l'exercice ; les deux ne doivent pas se contredire.
    CONSTRAINT uk_invoice_number UNIQUE (number)
);

-- L'IDEMPOTENCE de l'émission, portée par la base et pas seulement par le
-- service : une seule facture par client et par période. Relancer le traitement
-- mensuel — après une panne, ou parce que personne ne sait s'il a tourné — ne
-- doit pas doubler la facturation. Le contrôle applicatif lit avant d'écrire ;
-- entre les deux, une seconde exécution peut passer. Cet index est ce qui
-- tient à ce moment-là.
CREATE UNIQUE INDEX uk_invoice_tenant_period
    ON invoices (tenant_id, period_year, period_month);

-- La numérotation continue se calcule en cherchant le dernier numéro de
-- l'exercice : c'est la lecture la plus fréquente de la table.
CREATE INDEX idx_invoices_fiscal_year ON invoices (fiscal_year, number DESC);

-- Le détail : une ligne par abonnement facturé.
CREATE TABLE invoice_lines (
    id                UUID         PRIMARY KEY,
    -- CASCADE ici, à l'inverse de la facture : une ligne n'existe pas sans sa
    -- facture, et une facture orpheline de ses lignes ne dit plus ce qu'elle
    -- facture.
    invoice_id        UUID         NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,

    -- L'abonnement d'origine, pour la piste d'audit. Pas de clé étrangère :
    -- un abonnement peut être purgé lors du départ d'un client, la facture,
    -- elle, reste. Un lien contraint imposerait de garder l'un pour l'autre.
    subscription_id   UUID         NOT NULL,

    line_no           INTEGER      NOT NULL,
    module_code       VARCHAR(64)  NOT NULL,
    billing_tier      VARCHAR(32)  NOT NULL,
    period            VARCHAR(16)  NOT NULL,
    quantity          INTEGER      NOT NULL,
    unit_amount_cents BIGINT       NOT NULL,
    line_total_cents  BIGINT       NOT NULL,

    CONSTRAINT chk_line_quantity CHECK (quantity > 0),
    CONSTRAINT chk_line_unit     CHECK (unit_amount_cents >= 0),
    CONSTRAINT chk_line_total    CHECK (line_total_cents >= 0),
    -- Le total de la ligne est le produit, pas un nombre saisi à côté. Sans
    -- cette contrainte, une facture peut afficher 2 × 99,00 € = 150,00 € et
    -- rester parfaitement valide pour la base.
    CONSTRAINT chk_line_product  CHECK (line_total_cents = unit_amount_cents * quantity),
    -- Un seul numéro de ligne par facture, et un seul abonnement par facture :
    -- deux lignes pour le même contrat le factureraient deux fois.
    CONSTRAINT uk_line_no           UNIQUE (invoice_id, line_no),
    CONSTRAINT uk_line_subscription UNIQUE (invoice_id, subscription_id)
);

CREATE INDEX idx_invoice_lines_invoice ON invoice_lines (invoice_id);
