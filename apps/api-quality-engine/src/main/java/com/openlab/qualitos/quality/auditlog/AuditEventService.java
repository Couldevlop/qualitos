package com.openlab.qualitos.quality.auditlog;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Recording + listing + chain verification + (stub) blockchain anchor pour
 * les événements d'audit (§11.5 — fondation de l'ancrage Hyperledger Fabric).
 *
 * Invariants :
 *  - Pas de modification ni suppression. Seul {@code blockchainTxRef} peut être
 *    écrit après création, et une fois renseigné il n'est plus modifiable
 *    (idempotence ancrage).
 *  - {@code sequenceNo} strictement croissant par tenant, géré via
 *    {@link AuditEventCounter} en {@code PESSIMISTIC_WRITE}.
 *  - {@code previousHash} pointe sur le hash de l'événement N-1 (même tenant) ;
 *    le premier événement a previousHash = null.
 *  - {@code integrityHash} est calculé par {@link AuditEventHasher} et stocké
 *    sur la même transaction que l'insert.
 */
@Service
public class AuditEventService {

    private final AuditEventRepository eventRepo;
    private final AuditEventCounterRepository counterRepo;
    private final Clock clock;

    public AuditEventService(AuditEventRepository eventRepo,
                             AuditEventCounterRepository counterRepo) {
        this(eventRepo, counterRepo, Clock.systemUTC());
    }

    AuditEventService(AuditEventRepository eventRepo,
                      AuditEventCounterRepository counterRepo,
                      Clock clock) {
        this.eventRepo = eventRepo;
        this.counterRepo = counterRepo;
        this.clock = clock;
    }

    // ---------- Recording ----------

    /** API REST/contrôleur : tenant obligatoire dans le contexte. */
    @Transactional
    public AuditEventDto.EventResponse record(AuditEventDto.RecordEventRequest req) {
        UUID tenantId = requireTenantId();
        return toResponse(recordInternal(tenantId, req));
    }

    /**
     * API programmatique pour les autres modules — tenant explicite, ne dépend
     * pas du {@link TenantContext} (utilisable depuis un scheduler / hook
     * webhook). À utiliser avec précaution : le tenant doit être validé en
     * amont (provenance JWT pour les flux REST).
     */
    @Transactional
    public AuditEvent recordForTenant(UUID tenantId, AuditEventDto.RecordEventRequest req) {
        if (tenantId == null) throw new MissingTenantContextException();
        return recordInternal(tenantId, req);
    }

    private AuditEvent recordInternal(UUID tenantId, AuditEventDto.RecordEventRequest req) {
        Instant now = Instant.now(clock);

        // 1. Récupère et incrémente le compteur tenant avec verrou.
        AuditEventCounter counter = counterRepo.findById(tenantId)
                .orElseGet(() -> counterRepo.save(new AuditEventCounter(tenantId, 0L)));
        long nextSeq = counter.getLastSequenceNo() + 1;
        counter.setLastSequenceNo(nextSeq);
        counterRepo.save(counter);

        // 2. Récupère le hash de l'événement précédent (même tenant) — null si premier.
        String previousHash = eventRepo.findTopByTenantIdOrderBySequenceNoDesc(tenantId)
                .map(AuditEvent::getIntegrityHash).orElse(null);

        // 3. Construit la nouvelle ligne et calcule son hash chaîné.
        AuditEvent e = new AuditEvent();
        e.setTenantId(tenantId);
        e.setSequenceNo(nextSeq);
        e.setOccurredAt(req.occurredAt() != null ? req.occurredAt() : now);
        e.setRecordedAt(now);
        e.setActorType(req.actorType());
        e.setActorUserId(req.actorUserId());
        e.setAction(req.action());
        e.setResourceType(req.resourceType());
        e.setResourceId(req.resourceId());
        e.setSummary(req.summary());
        e.setPayloadJson(req.payloadJson());
        e.setIpAddress(req.ipAddress());
        e.setUserAgent(req.userAgent());
        e.setPreviousHash(previousHash);
        e.setIntegrityHash(AuditEventHasher.hash(e));
        return eventRepo.save(e);
    }

    // ---------- Listing ----------

    @Transactional(readOnly = true)
    public Page<AuditEventDto.EventResponse> list(String action, String resourceType,
                                                  UUID resourceId, UUID actorUserId,
                                                  Instant from, Instant to,
                                                  Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<AuditEvent> page;
        if (resourceType != null && resourceId != null) {
            page = eventRepo.findByTenantIdAndResourceTypeAndResourceIdOrderBySequenceNoDesc(
                    tenantId, resourceType, resourceId, pageable);
        } else if (action != null) {
            page = eventRepo.findByTenantIdAndActionOrderBySequenceNoDesc(tenantId, action, pageable);
        } else if (actorUserId != null) {
            page = eventRepo.findByTenantIdAndActorUserIdOrderBySequenceNoDesc(
                    tenantId, actorUserId, pageable);
        } else if (from != null && to != null) {
            page = eventRepo.findByTenantIdAndOccurredAtBetweenOrderBySequenceNoDesc(
                    tenantId, from, to, pageable);
        } else {
            page = eventRepo.findByTenantIdOrderBySequenceNoDesc(tenantId, pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AuditEventDto.EventResponse get(UUID id) {
        AuditEvent e = loadForTenant(id);
        return toResponse(e);
    }

    // ---------- Chain verification ----------

    /**
     * Vérifie une fenêtre [fromSeq, toSeq] inclusive : la séquence est
     * contiguë (pas de trou), chaque hash est recalculable, et chaque
     * {@code previousHash} pointe sur le hash du précédent.
     */
    @Transactional(readOnly = true)
    public AuditEventDto.ChainVerification verifyChain(long fromSeq, long toSeq) {
        UUID tenantId = requireTenantId();
        if (toSeq < fromSeq) {
            throw new AuditEventStateException("toSeq must be >= fromSeq");
        }
        List<AuditEvent> events = eventRepo
                .findByTenantIdAndSequenceNoBetweenOrderBySequenceNoAsc(tenantId, fromSeq, toSeq);
        List<AuditEventDto.ChainBreak> breaks = new ArrayList<>();

        long expectedSeq = fromSeq;
        String expectedPrevious;
        if (fromSeq == 1) {
            expectedPrevious = null;
        } else {
            // On a besoin du hash de fromSeq-1 pour valider le chaînage initial.
            expectedPrevious = eventRepo.findByTenantIdAndSequenceNoBetweenOrderBySequenceNoAsc(
                            tenantId, fromSeq - 1, fromSeq - 1)
                    .stream().findFirst().map(AuditEvent::getIntegrityHash).orElse(null);
        }

        for (AuditEvent e : events) {
            if (e.getSequenceNo() != expectedSeq) {
                breaks.add(new AuditEventDto.ChainBreak(e.getId(), e.getSequenceNo(),
                        "Sequence gap: expected " + expectedSeq));
                // ré-synchronise pour continuer l'analyse sur le reste.
                expectedSeq = e.getSequenceNo();
            }
            // Vérifie le previousHash.
            String storedPrev = e.getPreviousHash();
            if (!equalsNullable(storedPrev, expectedPrevious)) {
                breaks.add(new AuditEventDto.ChainBreak(e.getId(), e.getSequenceNo(),
                        "Previous hash mismatch (chain break)"));
            }
            // Vérifie le hash de la ligne courante.
            String recomputed = AuditEventHasher.hash(e);
            if (!recomputed.equals(e.getIntegrityHash())) {
                breaks.add(new AuditEventDto.ChainBreak(e.getId(), e.getSequenceNo(),
                        "Integrity hash mismatch (tamper)"));
            }
            expectedPrevious = e.getIntegrityHash();
            expectedSeq++;
        }
        if (events.size() < (toSeq - fromSeq + 1)) {
            breaks.add(new AuditEventDto.ChainBreak(null, expectedSeq,
                    "Missing event(s) up to seq " + toSeq));
        }
        return new AuditEventDto.ChainVerification(
                tenantId, fromSeq, toSeq, events.size(), breaks.isEmpty(), breaks);
    }

    // ---------- Anchoring ----------

    @Transactional
    public AuditEventDto.EventResponse anchor(UUID id, AuditEventDto.AnchorRequest req) {
        AuditEvent e = loadForTenant(id);
        if (e.getBlockchainTxRef() != null) {
            throw new AuditEventStateException(
                    "Event already anchored with tx ref: " + e.getBlockchainTxRef());
        }
        e.setBlockchainTxRef(req.blockchainTxRef());
        return toResponse(eventRepo.save(e));
    }

    // ---------- helpers ----------

    AuditEvent loadForTenant(UUID id) {
        UUID tenantId = requireTenantId();
        AuditEvent e = eventRepo.findById(id)
                .orElseThrow(() -> new AuditEventNotFoundException(id));
        if (!e.getTenantId().equals(tenantId)) throw new AuditEventNotFoundException(id);
        return e;
    }

    private static boolean equalsNullable(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private AuditEventDto.EventResponse toResponse(AuditEvent e) {
        return new AuditEventDto.EventResponse(
                e.getId(), e.getTenantId(), e.getSequenceNo(),
                e.getOccurredAt(), e.getRecordedAt(),
                e.getActorType(), e.getActorUserId(),
                e.getAction(), e.getResourceType(), e.getResourceId(),
                e.getSummary(), e.getPayloadJson(),
                e.getIpAddress(), e.getUserAgent(),
                e.getIntegrityHash(), e.getPreviousHash(),
                e.getBlockchainTxRef());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
