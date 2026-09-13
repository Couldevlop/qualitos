package com.openlab.qualitos.quality.nonconformity.eightd.application;

import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;

import java.util.UUID;

/**
 * Les quatre petits ports du cas d'usage, regroupés pour ne pas semer quatre
 * fichiers d'une interface chacun : l'acteur, le tenant, le codec de l'instantané,
 * l'URL publique et le journal d'audit.
 *
 * <p>Regroupés, mais pas fusionnés : ce sont bien cinq contrats distincts, chacun
 * implémenté par son adaptateur. Les réunir en une seule interface aurait obligé un
 * test à simuler le journal d'audit pour vérifier la lecture du tenant.
 */
public final class EightDSupportPorts {

    private EightDSupportPorts() {
    }

    /** Le tenant courant, lu du jeton validé — jamais du corps (§18.2 #2). */
    public interface TenantProvider {
        UUID requireTenantId();
    }

    /**
     * L'acteur courant, lu du jeton.
     *
     * <p>Qui émet un 8D signé vient TOUJOURS de l'identité authentifiée : un nom
     * d'émetteur que l'appelant pourrait écrire lui-même ne vaudrait rien devant un
     * auditeur (OWASP A01).
     */
    public interface ActorProvider {

        UUID currentUserId();

        /** Le nom lisible, figé dans le rapport ; vide si le jeton ne le porte pas. */
        String currentUserName();
    }

    /**
     * Sérialise et relit l'instantané figé.
     *
     * <p>Derrière un port parce que le format est un choix d'infrastructure, et
     * surtout parce que le cas d'usage ne doit pas dépendre de Jackson pour rester
     * testable sans contexte Spring.
     */
    public interface SnapshotCodec {

        String encode(EightDSnapshot snapshot);

        EightDSnapshot decode(String json);
    }

    /** Construit l'URL publique de vérification encodée dans le QR code. */
    public interface VerifyUrlBuilder {
        String verifyUrl(String verificationCode);
    }

    /**
     * Consigne au journal chaîné du tenant l'émission d'un rapport 8D.
     *
     * <p>Émettre rend un document opposable : un auditeur demandera qui l'a émis et
     * quand. Le journal étant lui-même ancré par arbre de Merkle, l'y inscrire suffit
     * à rendre la trace infalsifiable (§11.5).
     */
    public interface AuditPort {
        void recordIssued(UUID tenantId, UUID actorId, UUID ncId, String summary, String detailsJson);
    }
}
