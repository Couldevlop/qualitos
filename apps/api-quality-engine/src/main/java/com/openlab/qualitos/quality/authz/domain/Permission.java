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
    CAPA_CREATE("capa.create", "capa", pilotage()),
    CAPA_EDIT("capa.edit", "capa", pilotage()),
    CAPA_RESOLVE("capa.resolve", "capa", pilotage()),
    CAPA_REJECT("capa.reject", "capa", pilotage()),
    CAPA_VERIFY("capa.verify", "capa", plus(pilotage(), AUDITOR)),
    CAPA_DELETE("capa.delete", "capa", EnumSet.of(ADMIN_TENANT, QUALITY_MANAGER)),
    CAPA_ACTION_MANAGE("capa.action.manage", "capa", pilotage()),
    CAPA_ACTION_UPDATE("capa.action.update", "capa", plus(pilotage(), USER)),

    // ---------- non-conformités ----------
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
    RISK_MANAGE("risk.manage", "risk", pilotage()),
    OPPORTUNITY_MANAGE("opportunity.manage", "risk", pilotage());

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

    private static EnumSet<SystemRole> plus(EnumSet<SystemRole> base, SystemRole... autres) {
        EnumSet<SystemRole> r = EnumSet.copyOf(base);
        r.addAll(Arrays.asList(autres));
        return r;
    }
}
