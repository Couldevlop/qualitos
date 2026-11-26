package com.openlab.qualitos.quality.nonconformity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "non_conformities")
@Getter
@Setter
@NoArgsConstructor
public class NonConformity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /** Code lisible généré côté service (ex. NC-2026-0001), unique par tenant. */
    @Column(nullable = false, length = 40, updatable = false)
    private String reference;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NcCategory category;

    /**
     * La nature fine de l'écart, sous sa catégorie — le code d'une entrée du
     * catalogue du client (voir {@code NcSubcategoryCatalog}).
     *
     * <p>Facultative : un écart se saisit d'abord, souvent sur le terrain, et se
     * classe finement ensuite. Un code conservé alors que le client a retiré
     * l'entrée de son catalogue reste affiché tel quel : effacer un classement
     * passé pour cause de changement de nomenclature réécrirait l'histoire.
     */
    @Column(name = "subcategory_code", length = 30)
    private String subcategoryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NcSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NcStatus status;

    /**
     * Interne (détectée par l'organisation) ou externe (signalée du dehors).
     * Non nul : une NC sans origine connue est comptée comme interne, jamais
     * laissée dans un état indéterminé qui fausserait les deux compteurs.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NcOrigin origin = NcOrigin.INTERNAL;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(length = 255)
    private String zone;

    /** Géolocalisation de la saisie terrain (optionnelle). */
    @Column(name = "geo_lat")
    private Double geoLat;

    @Column(name = "geo_lng")
    private Double geoLng;

    /** URLs des photos terrain, séparées par des retours ligne (pas de BLOB). */
    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls;

    @Column(name = "reporter_id")
    private UUID reporterId;

    /**
     * Nom lisible de la personne qui a signalé l'écart, figé au moment du
     * signalement.
     *
     * <p>Une copie, pas une jointure : le nom doit rester lisible des années
     * plus tard, quand le compte a été renommé, désactivé ou supprimé de
     * l'annuaire. Nul sur les NC antérieures à cette colonne — on ne devine pas
     * qui a signalé quoi.
     */
    @Column(name = "reporter_name", length = 255)
    private String reporterName;

    /**
     * Produit concerné, facultatif : une NC de service ou administrative n'en a
     * pas, et l'exiger empêcherait simplement de la saisir.
     */
    @Column(name = "product_id")
    private UUID productId;

    /**
     * Le client qui réclame, par le nom qu'il s'est donné.
     *
     * <p>Du texte, et non un lien vers un référentiel client : la plateforme n'en
     * a pas, et une réclamation arrive souvent avec le seul nom entendu au
     * téléphone. Facultatif — un constat interne n'a pas de client.
     */
    @Column(name = "customer_name", length = 255)
    private String customerName;

    /**
     * Le lot incriminé, tel qu'il est marqué sur la pièce ou l'emballage.
     *
     * <p>Du texte : « L2611-08 » est une référence d'atelier, pas un nombre, et
     * son format change d'une ligne de production à l'autre.
     */
    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    /**
     * Mode de défaillance du PFMEA que ce défaut illustre. Son absence est une
     * information en soi : un défaut que l'analyse n'avait pas prévu.
     */
    @Column(name = "fmea_item_id")
    private UUID fmeaItemId;

    /** Lien d'escalade vers la CAPA créée à partir de cette NC. */
    @Column(name = "capa_case_id")
    private UUID capaCaseId;

    /**
     * Le cycle PDCA ouvert depuis cet écart, s'il l'a été (§3.6).
     *
     * <p>Le lien part d'ici et non du cycle : c'est la fiche de l'écart qu'on
     * ouvre pour savoir s'il est traité, et une seconde conversion doit pouvoir
     * être refusée sans parcourir tous les cycles du client.
     */
    @Column(name = "pdca_cycle_id")
    private UUID pdcaCycleId;

    /** Le projet DMAIC ouvert depuis cet écart, s'il l'a été (§3.6). */
    @Column(name = "dmaic_project_id")
    private UUID dmaicProjectId;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    /**
     * Le commentaire libre de l'analyse des causes (D4 du traitement 8D).
     *
     * <p>Ce qui ne tient ni dans un diagramme, ni dans une chaîne de pourquoi,
     * ni dans la cause retenue : les observations, et les hypothèses écartées
     * avec la raison de leur abandon. C'est ce qu'un auditeur demande quand il
     * conteste la cause racine, et cela s'écrivait dans la description de
     * l'écart, où plus personne ne le rapprochait de l'analyse.
     *
     * <p>Ne remplace pas {@link #rootCause}, qui est la CONCLUSION publiée par
     * le rapport 8D : un dossier qui n'aurait que le commentaire n'aurait pas
     * conclu.
     */
    @Column(name = "cause_analysis_comment", columnDefinition = "TEXT")
    private String causeAnalysisComment;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Pourquoi la réclamation a été écartée.
     *
     * <p>Colonne propre, et non un réemploi de {@code resolutionNote} : celle-ci
     * dit ce qui a résolu l'écart, et y ranger un refus mentirait sur ce qui s'est
     * passé.
     */
    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    /**
     * Quand l'écart a été REPRIS après avoir été refermé, s'il l'a été.
     *
     * <p>Ce qui l'a refermé n'est pas effacé pour autant : {@code resolvedAt},
     * {@code closedAt} et {@code rejectedAt} restent ce qu'ils furent. Ce sont
     * des faits datés, et le statut dit à lui seul où en est l'écart aujourd'hui
     * — les remettre à zéro effacerait la réponse que le client a lue.
     */
    /**
     * Qui a resolu, qui a clos, qui valide : le SECOND REGARD sur la cloture.
     *
     * <p>Sans la trace de qui a resolu, la regle n a pas de prise : rien ne
     * distingue la personne qui ecrit la resolution de celle qui la valide, et
     * l organisation ne peut pas montrer a un auditeur qu un controle
     * independant a eu lieu (ISO 9001 §10.2).
     *
     * <p>Le nom est COPIE au moment du geste, a cote de l identifiant
     * d annuaire : une fiche de 2026 doit rester lisible en 2030, quand la
     * personne a quitte l organisation et que l annuaire ne la rend plus.
     */
    @Column(name = "resolved_by_id")
    private UUID resolvedById;

    @Column(name = "resolved_by_name", length = 255)
    private String resolvedByName;

    @Column(name = "closed_by_id")
    private UUID closedById;

    @Column(name = "closed_by_name", length = 255)
    private String closedByName;

    /**
     * L ECART A-T-IL IMPACTE L ANALYSE DE RISQUE DU PRODUIT ?
     *
     * <p>Deux questions tranchees a la cloture : le PFMEA, et le plan de
     * surveillance. Un ecart traite enseigne quelque chose sur l analyse des
     * risques -- un mode de defaillance qu elle n avait pas prevu, un controle
     * qu elle ne portait pas -- et l IATF 16949 en fait une exigence : toute
     * non-conformite se repercute sur la PFMEA et le plan de surveillance.
     *
     * <p>NULL N EST PAS FALSE. NULL veut dire que la question n a pas encore
     * ete posee ; FALSE veut dire qu elle l a ete, et qu on a juge que non. La
     * premiere ne prouve rien, la seconde est une decision datee et imputable
     * -- et c est elle que l auditeur vient lire.
     */
    @Column(name = "pfmea_impacted")
    private Boolean pfmeaImpacted;

    @Column(name = "control_plan_impacted")
    private Boolean controlPlanImpacted;

    /**
     * La personne DESIGNEE pour porter le second regard sur cet ecart.
     *
     * <p>Facultative. Sans elle, le second regard revient aux roles de pilotage
     * -- ce qui suffit a une organisation qui en compte plusieurs. La designer
     * permet a une petite organisation, ou celle qui veut un regard venu d un
     * autre service, de confier la cloture a quelqu un qui n a pas le role.
     */
    @Column(name = "validator_id")
    private UUID validatorId;

    @Column(name = "validator_name", length = 255)
    private String validatorName;

    /**
     * LE RENVOI EN MODIFICATION : ce que le second regard repond quand il ne
     * clot pas.
     *
     * <p>Distinct de la reprise, et pas par souci de nuance. Rouvrir dit « le
     * probleme est revenu apres la cloture » et ramene l'ecart au DEBUT de
     * l'instruction ; renvoyer dit « le traitement n'est pas encore bon »,
     * laisse l'analyse en place et rend l'ecart a son pilote. Les confondre
     * ferait compter comme recidive un ecart qui n'a jamais ete clos.
     *
     * <p>Le nom est COPIE au moment du geste, comme celui de qui clot : le
     * second regard ne se demontre que signe (ISO 9001 §10.2).
     */
    @Column(name = "sent_back_at")
    private Instant sentBackAt;

    /** Pourquoi le traitement revient au pilote. Facultatif. */
    @Column(name = "sent_back_reason", columnDefinition = "TEXT")
    private String sentBackReason;

    @Column(name = "sent_back_by_id")
    private UUID sentBackById;

    @Column(name = "sent_back_by_name", length = 255)
    private String sentBackByName;

    @Column(name = "reopened_at")
    private Instant reopenedAt;

    /**
     * Pourquoi l'écart a été repris : récidive constatée, client qui relance,
     * constat écarté à tort.
     *
     * <p>Colonne propre : {@code rejectionReason} et {@code resolutionNote} disent
     * pourquoi le dossier s'est FERMÉ, celle-ci pourquoi il se ROUVRE. Les
     * confondre réécrirait l'histoire de la clôture.
     */
    @Column(name = "reopen_reason", columnDefinition = "TEXT")
    private String reopenReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = NcStatus.OPEN;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
