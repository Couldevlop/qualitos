package com.openlab.qualitos.core.platformaudit;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PlatformAuditCounterRepository extends JpaRepository<PlatformAuditCounter, String> {

    /**
     * Charge l'unique compteur sous verrou pessimiste.
     *
     * <p>Une méthode nommée, et non un {@code findById} redéclaré : le verrou
     * est le seul intérêt de cet appel, et un appelant qui écrit {@code
     * findById(SCOPE)} par habitude obtiendrait la ligne SANS verrou, donc deux
     * écritures concurrentes sur le même numéro de séquence. Le nom
     * {@code lock()} ne laisse pas ce malentendu possible.
     *
     * <p>La ligne est posée par la migration V12, et le vide est donc anormal.
     * Il est rendu tel quel plutôt que comblé : un compteur recréé à zéro
     * réattribuerait des numéros déjà employés, et l'appelant doit refuser
     * d'écrire — voir {@link PlatformAuditService}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PlatformAuditCounter c where c.scope = '" + PlatformAuditCounter.SCOPE + "'")
    Optional<PlatformAuditCounter> lock();
}
