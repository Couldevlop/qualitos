package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import com.openlab.qualitos.quality.change.domain.MocClassification;
import com.openlab.qualitos.quality.change.domain.MocException;
import com.openlab.qualitos.quality.change.domain.MocSettings;
import com.openlab.qualitos.quality.change.domain.MocThresholds;
import com.openlab.qualitos.quality.change.domain.MocWeights;
import com.openlab.qualitos.quality.change.domain.ValidationRole;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le paramétrage de la gestion des changements d'un client (ADR 0087).
 *
 * <p>Seul l'administrateur du client le lit et l'écrit (droit {@code authz.manage},
 * posé sur le contrôleur). Les autres utilisateurs voient les valeurs APPLIQUÉES
 * dans les demandes : le poids près d'un domaine, les validations requises.
 *
 * <p><b>Ce qu'un enregistrement change.</b>
 * <ul>
 *   <li>Les demandes en brouillon ou en analyse sont reclassées aussitôt (seuils,
 *       poids) : leur analyse est encore ouverte.</li>
 *   <li>Les demandes en validation, approuvées ou rejetées gardent leur score, leur
 *       classification et leurs cartes, figés à la soumission.</li>
 *   <li>Un rôle qu'une demande en validation attend encore ne se retire pas, et
 *       ne se vide pas de ses titulaires (409) : la demande resterait bloquée.</li>
 *   <li>Un nouveau titulaire d'un rôle attendu est prévenu.</li>
 * </ul>
 */
@Service
public class MocSettingsService {

    private final MocSettingsRowRepository settingsRepo;
    private final MocDomainWeightRepository weightRepo;
    private final MocValidationRoleRepository roleRepo;
    private final MocRoleHolderRepository holderRepo;
    private final ChangeRequestRepository requestRepo;
    private final ChangeImpactAssessmentRepository assessmentRepo;
    private final ChangeValidationRepository validationRepo;
    private final MocNotifier notifier;
    private final MocAuditTrail audit;
    private final Clock clock;

    public MocSettingsService(MocSettingsRowRepository settingsRepo, MocDomainWeightRepository weightRepo,
                              MocValidationRoleRepository roleRepo, MocRoleHolderRepository holderRepo,
                              ChangeRequestRepository requestRepo, ChangeImpactAssessmentRepository assessmentRepo,
                              ChangeValidationRepository validationRepo, MocNotifier notifier,
                              MocAuditTrail audit, Clock clock) {
        this.settingsRepo = settingsRepo;
        this.weightRepo = weightRepo;
        this.roleRepo = roleRepo;
        this.holderRepo = holderRepo;
        this.requestRepo = requestRepo;
        this.assessmentRepo = assessmentRepo;
        this.validationRepo = validationRepo;
        this.notifier = notifier;
        this.audit = audit;
        this.clock = clock;
    }

    // ---------- lecture ----------

    /** Le paramétrage d'un client ; celui de la plateforme pour ce qu'il n'a pas réglé. */
    @Transactional(readOnly = true)
    public MocSettings load(UUID tenantId) {
        MocSettings defaut = MocSettings.defaults();
        MocThresholds seuils = defaut.thresholds();
        int rappel = defaut.reminderDays();
        var row = settingsRepo.findById(tenantId);
        if (row.isPresent()) {
            seuils = new MocThresholds(row.get().getMajorThreshold(), row.get().getCriticalThreshold());
            rappel = row.get().getReminderDays();
        }
        List<MocDomainWeightRow> poids = weightRepo.findByTenantId(tenantId);
        MocWeights weights = poids.size() == ImpactDomain.values().length
                ? MocWeights.of(poids.stream().collect(Collectors.toMap(MocDomainWeightRow::getDomain,
                        MocDomainWeightRow::getWeight)))
                : defaut.weights();
        List<MocValidationRoleRow> roles = roleRepo.findByTenantIdOrderByOrdinalAsc(tenantId);
        if (roles.isEmpty()) {
            return new MocSettings(seuils, weights, rappel, defaut.roles());
        }
        Map<UUID, List<ValidationRole.RoleHolder>> titulaires = new HashMap<>();
        for (MocRoleHolderRow h : holderRepo.findByTenantIdOrderByOrdinalAsc(tenantId)) {
            titulaires.computeIfAbsent(h.getRoleId(), k -> new ArrayList<>())
                    .add(new ValidationRole.RoleHolder(h.getUserId(), h.getUserLabel()));
        }
        List<ValidationRole> vr = roles.stream().map(r -> new ValidationRole(r.getId(), r.getLabel(),
                requiredFor(r), titulaires.getOrDefault(r.getId(), List.of()))).toList();
        return new MocSettings(seuils, weights, rappel, vr);
    }

    @Transactional(readOnly = true)
    public MocDto.SettingsView view() {
        return toView(load(requireTenantId()));
    }

    // ---------- écriture ----------

    @Transactional
    public MocDto.SettingsView replace(MocDto.SettingsForm form) {
        UUID tenantId = requireTenantId();
        UUID acteur = CurrentUser.requireUserId();
        MocSettings avant = load(tenantId);

        MocSettings voulu = new MocSettings(
                new MocThresholds(form.majorThreshold(), form.criticalThreshold()),
                MocWeights.of(form.weights()),
                form.reminderDays(),
                form.roles().stream().map(r -> new ValidationRole(r.id(), r.label().strip(),
                        r.requiredFor(), r.holders().stream()
                        .map(h -> new ValidationRole.RoleHolder(h.userId(), h.label().strip())).toList()))
                        .toList()).validated();

        Map<UUID, MocValidationRoleRow> existants = roleRepo.findByTenantIdOrderByOrdinalAsc(tenantId).stream()
                .collect(Collectors.toMap(MocValidationRoleRow::getId, Function.identity()));
        verifierRoles(tenantId, voulu, existants);

        enregistrerSeuils(tenantId, voulu, acteur);
        enregistrerPoids(tenantId, voulu.weights());
        List<ValidationRole> roles = enregistrerRoles(tenantId, voulu.roles(), existants);
        MocSettings apres = new MocSettings(voulu.thresholds(), voulu.weights(), voulu.reminderDays(), roles);

        reclasserLesDemandesOuvertes(tenantId, apres);
        prevenirLesNouveauxTitulaires(tenantId, avant, apres);
        audit.settings(tenantId, acteur);
        return toView(apres);
    }

    /**
     * Les identifiants envoyés doivent être des rôles DE CE CLIENT ; un rôle retiré
     * ou vidé de ses titulaires ne doit plus être attendu par une demande.
     */
    private void verifierRoles(UUID tenantId, MocSettings voulu, Map<UUID, MocValidationRoleRow> existants) {
        Map<UUID, ValidationRole> gardes = new HashMap<>();
        for (ValidationRole r : voulu.roles()) {
            if (r.id() != null) {
                if (!existants.containsKey(r.id())) {
                    throw MocException.invalid("moc.settings.role-id", "Rôle inconnu : " + r.id() + ".");
                }
                gardes.put(r.id(), r);
            }
        }
        if (existants.isEmpty()) {
            return;
        }
        List<ChangeValidation> attendues = validationRepo.findByTenantIdAndRoleIdInAndDecision(
                tenantId, existants.keySet(), ApprovalDecision.PENDING);
        for (ChangeValidation v : attendues) {
            ValidationRole garde = gardes.get(v.getRoleId());
            if (garde == null) {
                throw new MocException(MocException.Kind.CONFLICT, "moc.settings.role-in-use",
                        "Le rôle « " + existants.get(v.getRoleId()).getLabel()
                                + " » est attendu par une demande en validation : il ne peut pas être retiré.",
                        Map.of("role", existants.get(v.getRoleId()).getLabel()));
            }
            if (garde.holders().isEmpty()) {
                throw new MocException(MocException.Kind.CONFLICT, "moc.settings.role-in-use-holders",
                        "Le rôle « " + garde.label()
                                + " » est attendu par une demande en validation : il doit garder un titulaire.",
                        Map.of("role", garde.label()));
            }
        }
    }

    private void enregistrerSeuils(UUID tenantId, MocSettings s, UUID acteur) {
        MocSettingsRow row = settingsRepo.findById(tenantId).orElseGet(() -> {
            MocSettingsRow n = new MocSettingsRow();
            n.setTenantId(tenantId);
            return n;
        });
        row.setMajorThreshold(s.thresholds().major());
        row.setCriticalThreshold(s.thresholds().critical());
        row.setReminderDays(s.reminderDays());
        row.setUpdatedBy(acteur);
        row.setUpdatedAt(clock.instant());
        settingsRepo.save(row);
    }

    /** Mise à jour en place : un supprimer-puis-insérer heurterait l'unicité (client, domaine). */
    private void enregistrerPoids(UUID tenantId, MocWeights weights) {
        Map<ImpactDomain, MocDomainWeightRow> lignes = new EnumMap<>(ImpactDomain.class);
        for (MocDomainWeightRow w : weightRepo.findByTenantId(tenantId)) {
            lignes.put(w.getDomain(), w);
        }
        for (ImpactDomain d : ImpactDomain.values()) {
            MocDomainWeightRow w = lignes.computeIfAbsent(d, k -> {
                MocDomainWeightRow n = new MocDomainWeightRow();
                n.setTenantId(tenantId);
                n.setDomain(k);
                return n;
            });
            w.setWeight(weights.weightOf(d));
            weightRepo.save(w);
        }
    }

    /** Les rôles gardés sont mis à jour en place, les retirés supprimés, les nouveaux créés. */
    private List<ValidationRole> enregistrerRoles(UUID tenantId, List<ValidationRole> voulus,
                                                  Map<UUID, MocValidationRoleRow> existants) {
        Set<UUID> gardes = new HashSet<>();
        List<ValidationRole> resultat = new ArrayList<>();
        List<MocRoleHolderRow> anciens = holderRepo.findByTenantIdOrderByOrdinalAsc(tenantId);
        if (!anciens.isEmpty()) {
            // Immédiat (et non au flush) : les mêmes couples rôle–personne vont être réinsérés.
            holderRepo.deleteAllInBatch(anciens);
        }
        int ordinal = 0;
        for (ValidationRole r : voulus) {
            MocValidationRoleRow row = r.id() == null ? null : existants.get(r.id());
            if (row == null) {
                row = new MocValidationRoleRow();
                row.setId(UUID.randomUUID());
                row.setTenantId(tenantId);
            }
            row.setOrdinal(ordinal++);
            row.setLabel(r.label());
            row.setRequiredMinor(r.requiredFor(MocClassification.MINOR));
            row.setRequiredMajor(r.requiredFor(MocClassification.MAJOR));
            row.setRequiredCritical(r.requiredFor(MocClassification.CRITICAL));
            roleRepo.save(row);
            gardes.add(row.getId());
            int rang = 0;
            for (ValidationRole.RoleHolder h : r.holders()) {
                MocRoleHolderRow hr = new MocRoleHolderRow();
                hr.setTenantId(tenantId);
                hr.setRoleId(row.getId());
                hr.setUserId(h.userId());
                hr.setUserLabel(h.label());
                hr.setOrdinal(rang++);
                holderRepo.save(hr);
            }
            resultat.add(new ValidationRole(row.getId(), r.label(), r.requiredFor(), r.holders()));
        }
        List<MocValidationRoleRow> retires = existants.values().stream()
                .filter(e -> !gardes.contains(e.getId())).toList();
        if (!retires.isEmpty()) {
            roleRepo.deleteAll(retires);
        }
        return resultat;
    }

    private void reclasserLesDemandesOuvertes(UUID tenantId, MocSettings s) {
        List<ChangeRequest> ouvertes = requestRepo.findByTenantIdAndStatusIn(tenantId,
                EnumSet.of(ChangeRequestStatus.DRAFT, ChangeRequestStatus.IN_ANALYSIS));
        if (ouvertes.isEmpty()) {
            return;
        }
        Map<UUID, List<ChangeImpactAssessment>> parDemande = assessmentRepo
                .findByTenantIdAndChangeIdIn(tenantId, ouvertes.stream().map(ChangeRequest::getId).toList())
                .stream().collect(Collectors.groupingBy(ChangeImpactAssessment::getChangeId));
        for (ChangeRequest c : ouvertes) {
            MocAnalysis.apply(c, parDemande.getOrDefault(c.getId(), List.of()), s);
            requestRepo.save(c);
        }
    }

    /** 5 — Un titulaire ajouté à un rôle qu'une demande attend est prévenu, une fois par demande. */
    private void prevenirLesNouveauxTitulaires(UUID tenantId, MocSettings avant, MocSettings apres) {
        Map<UUID, Set<UUID>> anciens = new HashMap<>();
        for (ValidationRole r : avant.roles()) {
            if (r.id() != null) {
                anciens.put(r.id(), r.holders().stream().map(ValidationRole.RoleHolder::userId)
                        .collect(Collectors.toSet()));
            }
        }
        Map<UUID, ValidationRole> roles = apres.roles().stream()
                .collect(Collectors.toMap(ValidationRole::id, Function.identity()));
        if (roles.isEmpty()) {
            return;
        }
        Map<UUID, Map<String, List<ValidationRole.RoleHolder>>> parDemande = new LinkedHashMap<>();
        for (ChangeValidation v : validationRepo.findByTenantIdAndRoleIdInAndDecision(
                tenantId, roles.keySet(), ApprovalDecision.PENDING)) {
            ValidationRole r = roles.get(v.getRoleId());
            Set<UUID> deja = anciens.getOrDefault(v.getRoleId(), Set.of());
            List<ValidationRole.RoleHolder> nouveaux = r.holders().stream()
                    .filter(h -> !deja.contains(h.userId())).toList();
            if (!nouveaux.isEmpty()) {
                parDemande.computeIfAbsent(v.getChangeId(), k -> new LinkedHashMap<>())
                        .put(v.getRoleLabel(), nouveaux);
            }
        }
        parDemande.forEach((changeId, titulaires) -> requestRepo.findByIdAndTenantId(changeId, tenantId)
                .filter(c -> c.getStatus() == ChangeRequestStatus.IN_VALIDATION)
                .ifPresent(c -> notifier.awaitingValidation(c,
                        MocNotifier.recipients(titulaires, c.getRequesterUserId()))));
    }

    // ---------- vues ----------

    static MocDto.SettingsView toView(MocSettings s) {
        Map<ImpactDomain, Integer> poids = new EnumMap<>(s.weights().asMap());
        return new MocDto.SettingsView(s.thresholds().major(), s.thresholds().critical(), s.reminderDays(), poids,
                s.roles().stream().map(r -> new MocDto.RoleView(r.id(), r.label(),
                        r.requiredFor().isEmpty() ? Set.of() : EnumSet.copyOf(r.requiredFor()),
                        r.holders().stream().map(h -> new MocDto.HolderView(h.userId(), h.label())).toList()))
                        .toList());
    }

    private static Set<MocClassification> requiredFor(MocValidationRoleRow r) {
        Set<MocClassification> s = EnumSet.noneOf(MocClassification.class);
        if (r.isRequiredMinor()) s.add(MocClassification.MINOR);
        if (r.isRequiredMajor()) s.add(MocClassification.MAJOR);
        if (r.isRequiredCritical()) s.add(MocClassification.CRITICAL);
        return s;
    }

    static UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
