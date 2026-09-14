package com.openlab.qualitos.quality.apqp;

import java.util.UUID;

/**
 * Projet APQP introuvable — inexistant, ou appartenant à un autre client.
 *
 * <p>Les deux cas se confondent délibérément : distinguer « il n'existe pas » de
 * « il ne vous appartient pas » dirait à qui essaie qu'un projet de ce nom
 * existe ailleurs (OWASP A01).
 */
public class ApqpProjectNotFoundException extends RuntimeException {

    public ApqpProjectNotFoundException(UUID id) {
        super("APQP project not found: " + id);
    }
}
