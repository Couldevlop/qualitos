package com.openlab.qualitos.core.platformaudit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Le compteur de séquence de la piste d'audit de plateforme : une seule ligne,
 * pour toute la plateforme.
 *
 * <p><b>Pourquoi une ligne en base plutôt qu'un {@code MAX(sequence_no) + 1}.</b>
 * Deux écritures simultanées liraient le même maximum et viseraient le même
 * numéro ; l'une des deux échouerait sur la contrainte d'unicité, et l'acte
 * qu'elle consignait serait perdu avec sa transaction. Une ligne dédiée se
 * verrouille ({@link PlatformAuditCounterRepository#lock()}), et les écritures
 * se mettent en file au lieu de se percuter.
 *
 * <p>La clé est une chaîne constante plutôt qu'un entier : {@code 'PLATFORM'}
 * se lit dans un {@code SELECT} et dit ce qu'il compte. La contrainte
 * {@code ck_platform_audit_counter_scope} interdit toute autre valeur, pour
 * qu'une seconde chaîne ne puisse pas apparaître par accident.
 */
@Entity
@Table(name = "platform_audit_counters")
public class PlatformAuditCounter {

    /** La seule valeur admise par la base. */
    public static final String SCOPE = "PLATFORM";

    @Id
    @Column(name = "scope", nullable = false, length = 16, updatable = false)
    private String scope;

    @Column(name = "last_sequence_no", nullable = false)
    private long lastSequenceNo;

    protected PlatformAuditCounter() {
    }

    /**
     * Réserve le numéro suivant et le rend.
     *
     * <p>Appelé sous le verrou du dépôt : c'est l'endroit où la monotonicité se
     * décide.
     */
    long reserveNext() {
        return ++lastSequenceNo;
    }

    public String getScope() {
        return scope;
    }

    public long getLastSequenceNo() {
        return lastSequenceNo;
    }
}
