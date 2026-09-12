-- NC : une réclamation non fondée se REJETTE, elle ne s'annule pas.
--
-- Les réclamations ont rejoint les non-conformités externes, et le statut
-- « rejetée » est resté en route. Faute de mieux on annulait — or annuler dit
-- « ce constat n'avait pas lieu d'être », rejeter dit « le constat a été examiné
-- et écarté, voici pourquoi ». Ce n'est pas la même chose, et c'est la seconde
-- qu'un client ou un auditeur vient lire.

ALTER TABLE non_conformities
    -- Le motif, et non un réemploi de `resolution_note` : cette colonne dit ce
    -- qui a résolu l'écart, et y ranger un refus mentirait sur ce qui s'est passé.
    ADD COLUMN rejection_reason VARCHAR(2000),
    ADD COLUMN rejected_at      TIMESTAMPTZ;

-- Un rejet sans motif n'est pas défendable, et une date sans motif ne dit rien :
-- les deux colonnes vivent ou meurent ensemble.
ALTER TABLE non_conformities
    ADD CONSTRAINT ck_nc_rejection_complete
        CHECK ((rejection_reason IS NULL) = (rejected_at IS NULL));

-- La contrainte de statut de la V73 énumère les statuts admis : sans cette
-- reprise, la base refuserait tout rejet — et le code seul aurait laissé croire
-- que le statut existait.
ALTER TABLE non_conformities
    DROP CONSTRAINT chk_non_conformities_status;

ALTER TABLE non_conformities
    ADD CONSTRAINT chk_non_conformities_status CHECK (
        status IN ('OPEN', 'UNDER_ANALYSIS', 'ACTION_DEFINED', 'RESOLVED',
                   'CLOSED', 'CANCELLED', 'REJECTED')
    );
