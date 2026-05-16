package com.openlab.qualitos.quality.blockchain.domain;

import java.util.UUID;

/**
 * Identité d'un événement candidat à l'ancrage (id pour update, hash pour Merkle).
 * Permet à la couche application de manipuler des objets purs et non l'entité JPA.
 */
public record Anchorable(UUID id, long sequenceNo, String integrityHash) {}
