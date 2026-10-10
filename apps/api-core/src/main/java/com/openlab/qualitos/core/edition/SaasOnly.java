package com.openlab.qualitos.core.edition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Un point d'entrée qui n'existe que sur la plateforme de l'éditeur (ADR 0082) :
 * créer des clients, les facturer, fixer les prix. Dans une installation
 * on-premise, il répond 404 — il n'y a qu'un client, et pas de facture.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SaasOnly {
}
