package com.openlab.qualitos.quality.authz.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static com.openlab.qualitos.quality.authz.domain.SystemRole.ADMIN_TENANT;
import static com.openlab.qualitos.quality.authz.domain.SystemRole.AUDITOR;
import static com.openlab.qualitos.quality.authz.domain.SystemRole.QUALITY_DIRECTOR;
import static com.openlab.qualitos.quality.authz.domain.SystemRole.QUALITY_MANAGER;
import static com.openlab.qualitos.quality.authz.domain.SystemRole.SUPER_ADMIN;
import static com.openlab.qualitos.quality.authz.domain.SystemRole.USER;

/**
 * Le catalogue des actions qu'un client peut accorder ou retirer, module par module.
 *
 * <p>Chaque action porte les rôles système qui la reçoivent PAR DÉFAUT — le
 * tableau du §16 de CLAUDE.md : l'auditeur constate et lit, l'utilisateur déclare
 * et fait avancer ce qu'on lui confie, le manager et le directeur pilotent,
 * l'administrateur du client administre. Un client qui n'a rien réglé a ces
 * droits-là ; il peut ensuite cocher et décocher chaque case.
 *
 * <p>Le super administrateur (l'éditeur) reçoit tout, toujours.
 *
 * <p>Le code d'une action ({@code capa.close}) est ce que stocke la base et ce
 * que l'écran nomme : il ne change jamais. Renommer une constante Java est
 * libre ; changer son code demande une migration des droits enregistrés.
 */
public enum Permission {

    // ---------- administration ----------
    AUTHZ_MANAGE("authz.manage", "admin", EnumSet.of(ADMIN_TENANT)),

    // ---------- CAPA ----------
    /** Sans elle, on ne voit que les dossiers qu'on pilote, qu'on vérifie ou dont on porte une action (ADR 0081). */
    CAPA_VIEW_ALL("capa.view.all", "capa", tous()),
    CAPA_CREATE("capa.create", "capa", pilotage()),
    CAPA_EDIT("capa.edit", "capa", pilotage()),
    CAPA_RESOLVE("capa.resolve", "capa", pilotage()),
    CAPA_REJECT("capa.reject", "capa", pilotage()),
    CAPA_VERIFY("capa.verify", "capa", plus(pilotage(), AUDITOR)),
    CAPA_DELETE("capa.delete", "capa", EnumSet.of(ADMIN_TENANT, QUALITY_MANAGER)),
    CAPA_ACTION_MANAGE("capa.action.manage", "capa", pilotage()),
    CAPA_ACTION_UPDATE("capa.action.update", "capa", plus(pilotage(), USER)),

    // ---------- dérogations (concessions client) ----------
    /**
     * Sans elle, on ne voit que ses propres dossiers et ceux qu’on doit signer
     * (ADR 0081). Une dérogation nomme un client et une tolérance qu’on a
     * manquée : c’est une information plus sensible qu’un écart interne.
     */
    /**
     * Lire le registre et une fiche.
     *
     * <p>Distincte de {@code derogation.create} : les lectures s'appuyaient sur
     * le droit de CREER, si bien qu'un manager designe qui n'est qu'auditeur
     * voyait le compteur et la liste de ses validations, puis se faisait refuser
     * le dossier qu'il doit signer.
     */
    DEROGATION_VIEW("derogation.view", "derogation", interne()),
    DEROGATION_VIEW_ALL("derogation.view.all", "derogation", interne()),
    DEROGATION_CREATE("derogation.create", "derogation", pilotage()),
    DEROGATION_EDIT("derogation.edit", "derogation", pilotage()),
    /**
     * Demander la validation du manager, et la retirer.
     *
     * <p>Le même droit pour les deux gestes : celui qui demande est celui qui
     * retire, et le domaine vérifie en plus qu’il est bien l’initiateur de CE
     * dossier.
     */
    DEROGATION_REQUEST_APPROVAL("derogation.request.approval", "derogation", pilotage()),
    /**
     * Signer, et renvoyer pour correction.
     *
     * <p><b>Volontairement large</b> : {@code tous()}. Le dossier désigne son
     * manager, et c’est l’agrégat qui vérifie que le signataire est bien celui-là.
     * Restreindre ce droit à {@code pilotage()} laisserait un dossier confié à un
     * compte hors de ces rôles sans personne pour le signer — ni l’initiateur, qui
     * n’est pas le manager, ni un autre manager, que le dossier refuse. Le
     * dossier serait bloqué sans issue.
     */
    DEROGATION_APPROVE("derogation.approve", "derogation", interne()),
    /** Rouvrir un dossier signé, ce qui annule la signature. */
    DEROGATION_REOPEN("derogation.reopen", "derogation", pilotage()),
    /**
     * Envoyer la demande au client : le seul geste qui sort de l’organisation.
     *
     * <p>Au pilotage, et au <b>même ensemble que tous les gestes de
     * l’initiateur</b> : l’agrégat n’accepte que l’initiateur, donc ouvrir et
     * envoyer doivent aller ensemble — sinon un dossier ouvert par un rôle plus
     * large serait inenvoyable par quiconque, ni par lui faute de droit, ni par
     * le pilotage que l’agrégat refuse. C’est le piège évité sur
     * {@code derogation.approve}, et un banc le vérifie sur l’énumération
     * elle-même, parce qu’il ne lève ni à la compilation ni aux tests de
     * domaine.
     *
     * <p>Le pilotage, et non tout utilisateur, parce que la maquette ne nomme
     * que deux rôles — « Responsable qualité fournisseur (demandeur) » et
     * « Manager qualité (signataire) » — et que ce geste fait partir un
     * courriel depuis l’adresse de l’organisation. {@code api-core} pose la même
     * liste au point d’envoi : les deux côtés doivent dire la même chose, et le
     * contrôle est là plutôt que dans l’omission du chemin dans l’ingress, qui
     * n’en est pas un.
     */
    DEROGATION_SEND("derogation.send", "derogation", pilotage()),
    /** Porter la décision du client au dossier. */
    DEROGATION_DECIDE("derogation.decide", "derogation", pilotage()),

    // ---------- non-conformités ----------
    /** Sans elle, on ne voit que les non-conformités qu'on a déclarées (ADR 0081). */
    NC_VIEW_ALL("nc.view.all", "nc", tous()),
    NC_CREATE("nc.create", "nc", plus(pilotage(), USER, AUDITOR)),
    NC_EDIT("nc.edit", "nc", pilotage()),
    NC_PHOTO("nc.photo", "nc", plus(pilotage(), USER, AUDITOR)),
    NC_PROCESS("nc.process", "nc", pilotage()),
    NC_CLOSE("nc.close", "nc", EnumSet.of(ADMIN_TENANT, QUALITY_DIRECTOR, QUALITY_MANAGER)),
    NC_REJECT("nc.reject", "nc", pilotage()),
    NC_ESCALATE("nc.escalate", "nc", pilotage()),

    // ---------- documents ----------
    DOCUMENT_EDIT("document.edit", "document", pilotage()),
    DOCUMENT_SUBMIT("document.submit", "document", pilotage()),
    DOCUMENT_APPROVE("document.approve", "document", EnumSet.of(ADMIN_TENANT, QUALITY_DIRECTOR, QUALITY_MANAGER)),
    DOCUMENT_PUBLISH("document.publish", "document", EnumSet.of(ADMIN_TENANT, QUALITY_DIRECTOR, QUALITY_MANAGER)),
    DOCUMENT_ACKNOWLEDGE("document.acknowledge", "document",
            plus(pilotage(), USER, AUDITOR, SystemRole.EXTERNAL_AUDITOR)),

    // ---------- registre des risques et opportunités ----------
    /** Sans elle, on ne voit que les risques et opportunités qu'on a inscrits (ADR 0081). */
    RISK_VIEW_ALL("risk.view.all", "risk", tous()),
    RISK_MANAGE("risk.manage", "risk", pilotage()),
    OPPORTUNITY_MANAGE("opportunity.manage", "risk", pilotage()),

    // ---------- gestion des changements (MOC, ADR 0087) ----------
    /** Demander un changement, et conduire SES demandes : brouillon, analyse, soumission, réouverture. */
    CHANGE_REQUEST("change.request", "change", plus(pilotage(), USER)),
    /** Gérer les changements : conduire aussi les demandes des autres (modifier, analyser, soumettre, rouvrir, supprimer un brouillon). */
    CHANGE_ANALYZE("change.analyze", "change", pilotage()),

    // ---------- SPC (ADR 0096) ----------
    /**
     * Créer, corriger et supprimer une étude de capabilité, cocher son plan d'actions.
     *
     * <p>Rangée sous {@code dmaic} et non sous un module « spc » : la capabilité
     * relève du pilotage Six Sigma, c'est ce que déclarent déjà les contrôleurs
     * ({@code @RequiresModule("dmaic")}) et la barre de navigation. Le catalogue
     * des modules ne contient AUCUN code {@code spc} — l'écran des droits
     * groupait donc cette action sous un module inexistant, dont il ne pouvait
     * nommer ni le libellé ni le périmètre.
     */
    SPC_MANAGE("spc.manage", "dmaic", pilotage()),

    // ---------- indicateurs (KPI) ----------
    /**
     * Définir un indicateur : son intitulé, sa formule, sa CIBLE et ses SEUILS,
     * son propriétaire, son cycle de vie (activer, rouvrir, archiver, supprimer).
     *
     * <p>Un acte de pilotage, et le mot « cible » dit pourquoi : déplacer la
     * cible d'un indicateur qu'on n'atteint pas le fait passer au vert sans que
     * rien n'ait changé sur le terrain. C'est la revue de direction qui fixe les
     * objectifs (ISO 9001 §6.2, §9.3), pas celui que l'indicateur mesure.
     *
     * <p>La LECTURE reste ouverte à tout authentifié : chacun doit pouvoir voir
     * où en est ce sur quoi on le juge.
     */
    KPI_MANAGE("kpi.manage", "kpi", pilotage()),

    /**
     * Consigner un relevé : la valeur mesurée sur une période.
     *
     * <p>Ouverte aussi à l'utilisateur, parce qu'un relevé est un FAIT, consigné
     * par celui qui a mesuré — contrairement à la cible, qui est une décision.
     * En RETIRER un relève en revanche de {@link #KPI_MANAGE} : effacer un relevé
     * gênant change la courbe sans laisser de trace à l'écran.
     */
    KPI_MEASURE("kpi.measure", "kpi", plus(pilotage(), USER)),

    /**
     * Définir un objectif qualité, et trancher la CAPA qu'un écart propose
     * (ISO 9001 §6.2, ADR 0133).
     *
     * <p>Rattachée au module {@code kpi} : un objectif n'existe que contre un KPI
     * du catalogue, et un client qui n'a pas activé les indicateurs n'a rien à
     * quoi comparer un objectif.
     *
     * <p>Réservée au pilotage, pour la même raison que {@link #KPI_MANAGE} :
     * fixer une cible est une décision, et déplacer la cible d'un objectif qu'on
     * n'atteint pas le fait passer au vert sans que rien n'ait changé sur le
     * terrain. ACCEPTER ou REJETER une proposition en relève aussi — c'est
     * l'arbitrage du pilote, et le rejet doit porter un nom.
     *
     * <p>La LECTURE reste ouverte à tout authentifié : la revue des objectifs est
     * ce qu'une organisation montre à ses équipes.
     */
    OBJECTIVE_MANAGE("objective.manage", "kpi", pilotage()),

    // ---------- DMAIC (ADR 0101) ----------
    /** Remplir la fiche de résolution d'un projet : réponses, causes, plan d'actions, photos. */
    DMAIC_MANAGE("dmaic.manage", "dmaic", pilotage()),

    // ---------- formation (ADR 0098) ----------
    /** Tenir la matrice de compétences : opérateurs, compétences, formateurs, niveaux. */
    TRAINING_MANAGE("training.manage", "training", pilotage()),

    // ---------- parties intéressées (ADR 0104) ----------
    /**
     * Tenir le registre des parties intéressées : inscrire, analyser, coter,
     * et porter leurs exigences.
     *
     * <p>Un acte de pilotage : déterminer les parties intéressées pertinentes,
     * c'est fixer le périmètre du système de management (ISO 9001 §4.2). La
     * LECTURE, elle, reste ouverte à tout authentifié — chacun doit pouvoir
     * savoir à quoi l'organisme s'est engagé.
     */
    STAKEHOLDER_MANAGE("stakeholder.manage", "stakeholder", pilotage());

    private final String code;
    private final String module;
    private final Set<SystemRole> defaultRoles;

    Permission(String code, String module, Set<SystemRole> defaultRoles) {
        this.code = code;
        this.module = module;
        EnumSet<SystemRole> roles = EnumSet.copyOf(defaultRoles);
        roles.add(SUPER_ADMIN);
        this.defaultRoles = Set.copyOf(roles);
    }

    public String code() {
        return code;
    }

    /** Le module qui porte l'action : l'écran regroupe la matrice par module. */
    public String module() {
        return module;
    }

    public boolean grantedByDefaultTo(SystemRole role) {
        return defaultRoles.contains(role);
    }

    public static Optional<Permission> fromCode(String code) {
        return Arrays.stream(values()).filter(p -> p.code.equals(code)).findFirst();
    }

    /** Ceux qui pilotent la qualité : administrateur du client, directeur, manager. */
    private static EnumSet<SystemRole> pilotage() {
        return EnumSet.of(ADMIN_TENANT, QUALITY_DIRECTOR, QUALITY_MANAGER);
    }

    /**
     * Tous les rôles d'un client. Voir tout le registre d'un module reste le défaut :
     * c'est ce que chacun voyait avant que la visibilité se règle, et le client
     * restreint ensuite qui il veut.
     */
    private static EnumSet<SystemRole> tous() {
        return EnumSet.complementOf(EnumSet.of(SUPER_ADMIN));
    }

    /**
     * Tous les rôles du client SAUF l'auditeur tiers.
     *
     * <p>{@link #tous()} l'inclut, et c'est voulu pour les registres internes :
     * un auditeur de cabinet doit pouvoir constater. Mais une dérogation est une
     * correspondance avec un <b>client</b> : le registre porte les noms des
     * clients et les écarts négociés avec eux, et un cabinet qui travaille aussi
     * pour le concurrent n'a pas à les lire. Lui accorder la signature serait
     * plus grave encore — un tiers pourrait approuver un dossier s'il en était
     * désigné manager.
     *
     * <p>Son accès reste possible dossier par dossier, si le client l'y désigne
     * ou lui accorde explicitement le droit : c'est le propre d'un défaut
     * d'octroi, qui se règle dans la console.
     */
    private static EnumSet<SystemRole> interne() {
        return EnumSet.complementOf(EnumSet.of(SUPER_ADMIN, SystemRole.EXTERNAL_AUDITOR));
    }

    private static EnumSet<SystemRole> plus(EnumSet<SystemRole> base, SystemRole... autres) {
        EnumSet<SystemRole> r = EnumSet.copyOf(base);
        r.addAll(Arrays.asList(autres));
        return r;
    }
}
