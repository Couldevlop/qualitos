package com.openlab.qualitos.quality.revisionrequests.domain;

/**
 * PENDING -> ACCEPTED | REJECTED | SUPERSEDED, tous terminaux.
 *
 * <p>SUPERSEDED plutôt qu'une suppression : l'historique des propositions est
 * lui-même une preuve, et effacer ce qu'on a proposé hier reviendrait à effacer
 * la trace d'une dérive.
 */
public enum RevisionRequestStatus { PENDING, ACCEPTED, REJECTED, SUPERSEDED }
