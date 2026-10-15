package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.MocHistoryEvent;
import com.openlab.qualitos.quality.change.domain.MocSettings;
import com.openlab.qualitos.quality.change.domain.ValidationRole;
import com.openlab.qualitos.quality.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rappel aux validateurs d'une demande qui attend leur décision (ADR 0087).
 *
 * <p>Pour chaque carte EN ATTENTE d'une demande EN VALIDATION : si le délai du
 * client (3 jours par défaut, réglé dans « Paramètres MOC ») est écoulé depuis la
 * soumission — puis depuis le dernier rappel —, les titulaires COURANTS du rôle
 * sont prévenus, et l'historique de la demande le dit. Le rappel se répète tous
 * les N jours tant que personne n'a statué ; il cesse de lui-même dès que la
 * demande est approuvée, rejetée ou rouverte (la carte n'est plus en attente, ou
 * n'existe plus).
 *
 * <p><b>Jours calendaires</b>, à l'horloge injectée : de la date de soumission
 * (ou du dernier rappel) à la date du jour.
 *
 * <p><b>Hors requête</b> : la tâche lit les cartes de tous les clients, puis
 * traite client par client en posant SON contexte (et le retire ensuite) — rien
 * ne déborde d'un client sur l'autre.
 *
 * <p><b>Sans doublon</b> : le droit d'envoyer se réserve en base par un UPDATE
 * conditionnel AVANT l'envoi (même dispositif que le rappel d'audit). Deux
 * répliques qui passent à la même minute : une seule gagne.
 */
@Service
public class MocReminderService {

    private static final Logger log = LoggerFactory.getLogger(MocReminderService.class);

    private final ChangeValidationRepository validationRepo;
    private final ChangeRequestRepository requestRepo;
    private final ChangeHistoryRepository historyRepo;
    private final MocSettingsService settings;
    private final MocNotifier notifier;
    private final MocAuditTrail audit;
    private final Clock clock;

    public MocReminderService(ChangeValidationRepository validationRepo, ChangeRequestRepository requestRepo,
                              ChangeHistoryRepository historyRepo, MocSettingsService settings,
                              MocNotifier notifier, MocAuditTrail audit, Clock clock) {
        this.validationRepo = validationRepo;
        this.requestRepo = requestRepo;
        this.historyRepo = historyRepo;
        this.settings = settings;
        this.notifier = notifier;
        this.audit = audit;
        this.clock = clock;
    }

    /** Le compte rendu d'un passage : cartes rappelées, demandes concernées. */
    public record Report(int cards, int requests) {}

    public Report sendDueReminders() {
        Map<UUID, List<ChangeValidation>> parClient = validationRepo
                .findPendingOfStatus(ChangeRequestStatus.IN_VALIDATION, ApprovalDecision.PENDING).stream()
                .collect(Collectors.groupingBy(ChangeValidation::getTenantId, LinkedHashMap::new, Collectors.toList()));
        int cartes = 0;
        int demandes = 0;
        for (Map.Entry<UUID, List<ChangeValidation>> e : parClient.entrySet()) {
            TenantContext.setTenantId(e.getKey().toString());
            try {
                Report r = client(e.getKey(), e.getValue());
                cartes += r.cards();
                demandes += r.requests();
            } catch (RuntimeException ex) {
                // Un client en échec n'emporte pas les suivants.
                log.error("[moc-reminder] passage en échec pour un client : {}", ex.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
        if (cartes > 0) {
            log.info("[moc-reminder] {} rappel(s) de rôle sur {} demande(s)", cartes, demandes);
        }
        return new Report(cartes, demandes);
    }

    private Report client(UUID tenantId, List<ChangeValidation> enAttente) {
        MocSettings s = settings.load(tenantId);
        Map<UUID, ValidationRole> roles = s.roles().stream().filter(r -> r.id() != null)
                .collect(Collectors.toMap(ValidationRole::id, Function.identity()));
        LocalDate today = LocalDate.now(clock);
        int cartes = 0;
        int demandes = 0;
        Map<UUID, List<ChangeValidation>> parDemande = enAttente.stream()
                .collect(Collectors.groupingBy(ChangeValidation::getChangeId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<UUID, List<ChangeValidation>> e : parDemande.entrySet()) {
            ChangeRequest c = requestRepo.findByIdAndTenantId(e.getKey(), tenantId).orElse(null);
            if (c == null || c.getStatus() != ChangeRequestStatus.IN_VALIDATION) {
                continue;
            }
            Map<String, List<ValidationRole.RoleHolder>> aPrevenir = new LinkedHashMap<>();
            long attente = 0;
            for (ChangeValidation v : e.getValue()) {
                Instant depuis = v.getLastRemindedAt() != null ? v.getLastRemindedAt()
                        : (c.getSubmittedAt() != null ? c.getSubmittedAt() : v.getCreatedAt());
                long jours = ChronoUnit.DAYS.between(LocalDate.ofInstant(depuis, clock.getZone()), today);
                if (jours < s.reminderDays() || !reserver(v)) {
                    continue;
                }
                ValidationRole r = roles.get(v.getRoleId());
                aPrevenir.put(v.getRoleLabel(), r == null ? List.of() : r.holders());
                LocalDate soumise = LocalDate.ofInstant(
                        c.getSubmittedAt() != null ? c.getSubmittedAt() : v.getCreatedAt(), clock.getZone());
                attente = Math.max(attente, ChronoUnit.DAYS.between(soumise, today));
                cartes++;
            }
            if (aPrevenir.isEmpty()) {
                continue;
            }
            demandes++;
            String libelles = String.join(", ", aPrevenir.keySet());
            ChangeHistoryEntry h = new ChangeHistoryEntry();
            h.setTenantId(tenantId);
            h.setChangeId(c.getId());
            h.setOccurredAt(clock.instant());
            h.setEvent(MocHistoryEvent.REMINDER_SENT);
            h.setDetail(libelles.length() > 1000 ? libelles.substring(0, 1000) : libelles);
            historyRepo.save(h);
            audit.change(tenantId, null, "reminder_sent", c, libelles);
            notifier.reminder(c, MocNotifier.recipients(aPrevenir, c.getRequesterUserId()), (int) attente);
        }
        return new Report(cartes, demandes);
    }

    private boolean reserver(ChangeValidation v) {
        Instant now = clock.instant();
        int pris = v.getLastRemindedAt() == null
                ? validationRepo.claimFirstReminder(v.getId(), now)
                : validationRepo.claimNextReminder(v.getId(), v.getLastRemindedAt(), now);
        return pris == 1;
    }
}
