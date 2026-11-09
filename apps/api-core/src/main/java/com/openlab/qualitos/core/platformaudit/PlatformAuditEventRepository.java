package com.openlab.qualitos.core.platformaudit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformAuditEventRepository extends JpaRepository<PlatformAuditEvent, UUID> {

    /** Le dernier acte consigné, dont l'empreinte chaîne le suivant. */
    Optional<PlatformAuditEvent> findTopByOrderBySequenceNoDesc();

    Page<PlatformAuditEvent> findAllByOrderBySequenceNoDesc(Pageable pageable);

    /** « Tout ce qui a touché ce client » — la question d'un client audité. */
    Page<PlatformAuditEvent> findByTargetTenantIdOrderBySequenceNoDesc(UUID targetTenantId,
                                                                       Pageable pageable);

    Page<PlatformAuditEvent> findByActionOrderBySequenceNoDesc(PlatformAuditAction action,
                                                               Pageable pageable);

    Page<PlatformAuditEvent> findByActorUserIdOrderBySequenceNoDesc(UUID actorUserId,
                                                                    Pageable pageable);

    Page<PlatformAuditEvent> findByOccurredAtBetweenOrderBySequenceNoDesc(Instant from, Instant to,
                                                                          Pageable pageable);

    /**
     * La fenêtre à vérifier ou à exporter, dans l'ordre de la chaîne.
     *
     * <p>Bornes incluses, ordre croissant : la vérification lit chaque
     * empreinte après celle qu'elle prolonge, ce qui n'est possible que dans
     * ce sens.
     */
    List<PlatformAuditEvent> findBySequenceNoBetweenOrderBySequenceNoAsc(long fromSeq, long toSeq);
}
