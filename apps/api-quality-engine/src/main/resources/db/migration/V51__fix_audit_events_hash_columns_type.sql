-- Fix: l'entite JPA AuditEvent declare integrity_hash et previous_hash en
-- @Column(length=64) (defaut = VARCHAR). La migration V26 les avait crees
-- en CHAR(64), ce qui cassait la validation de schema Hibernate au demarrage
-- (Schema-validation: found [bpchar], expecting [varchar(64)]).
-- On normalise vers VARCHAR(64) pour aligner sur l'entite.

ALTER TABLE audit_events ALTER COLUMN integrity_hash TYPE varchar(64);
ALTER TABLE audit_events ALTER COLUMN previous_hash  TYPE varchar(64);
