package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSupportPorts;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Les quatre petits adaptateurs du module 8D : tenant, acteur, codec d'instantané,
 * URL publique, journal d'audit.
 *
 * <p>Réunis dans un fichier parce que chacun tient en dix lignes et qu'ils
 * n'existent que pour brancher le cas d'usage sur le contexte de la plateforme.
 * Cinq fichiers d'une classe auraient dispersé une seule idée : « voilà où le
 * module touche le reste ».
 */
public final class EightDAdapters {

    private EightDAdapters() {
    }

    /** Le tenant du jeton validé. Jamais le corps de requête (§18.2 #2). */
    @Component("eightDTenantProvider")
    public static class TenantContextProvider implements EightDSupportPorts.TenantProvider {

        @Override
        public UUID requireTenantId() {
            if (!TenantContext.hasTenant()) {
                throw new MissingTenantContextException();
            }
            return UUID.fromString(TenantContext.getTenantId());
        }
    }

    /**
     * L'acteur du jeton.
     *
     * <p>Le nom est recopié dans le rapport au moment de l'émission, et non résolu à
     * la lecture : un 8D doit rester attribuable des années plus tard, quand le
     * compte a été renommé ou retiré de l'annuaire.
     */
    @Component("eightDActorProvider")
    public static class ActorContextProvider implements EightDSupportPorts.ActorProvider {

        @Override
        public UUID currentUserId() {
            return CurrentUser.requireUserId();
        }

        @Override
        public String currentUserName() {
            return CurrentUser.displayName().orElse(null);
        }
    }

    /**
     * Sérialise l'instantané en JSON.
     *
     * <p>Son propre {@link ObjectMapper}, sans module ni configuration héritée : le
     * format de cet instantané est une donnée de preuve, et il ne doit pas changer
     * parce qu'une configuration Jackson globale a bougé ailleurs dans
     * l'application.
     */
    @Component("eightDSnapshotCodec")
    public static class JacksonSnapshotCodec implements EightDSupportPorts.SnapshotCodec {

        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public String encode(EightDSnapshot snapshot) {
            try {
                return mapper.writeValueAsString(snapshot);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("serialisation de l'instantane 8D impossible", e);
            }
        }

        @Override
        public EightDSnapshot decode(String json) {
            try {
                return mapper.readValue(json, EightDSnapshot.class);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("instantane 8D illisible", e);
            }
        }
    }

    /**
     * L'URL publique encodée dans le QR code. Base configurable par environnement,
     * comme pour l'export de dashboard.
     */
    @Component("eightDVerifyUrlBuilder")
    public static class ConfigurableVerifyUrlBuilder implements EightDSupportPorts.VerifyUrlBuilder {

        private final String baseUrl;

        public ConfigurableVerifyUrlBuilder(
                @Value("${qualitos.export.public-base-url:https://app.qualitos.io}") String baseUrl) {
            this.baseUrl = baseUrl.endsWith("/")
                    ? baseUrl.substring(0, baseUrl.length() - 1)
                    : baseUrl;
        }

        @Override
        public String verifyUrl(String verificationCode) {
            String code = UriUtils.encodePathSegment(verificationCode, StandardCharsets.UTF_8);
            return baseUrl + "/api/v1/nc/public/8d/" + code + "/verify";
        }
    }

    /**
     * Inscrit l'émission au journal chaîné du tenant.
     *
     * <p>L'acteur est celui que le service a lu du jeton, jamais un champ de requête :
     * une émission dont l'auteur serait falsifiable ne vaudrait rien devant un
     * auditeur (§11.5, correctif H2 de l'audit du 6 juin).
     */
    @Component("eightDAuditAdapter")
    public static class AuditAdapter implements EightDSupportPorts.AuditPort {

        private static final String RESOURCE_TYPE = "eightd_report";
        private static final String ACTION = "EIGHTD_REPORT_ISSUED";

        private final AuditEventService auditEvents;

        public AuditAdapter(AuditEventService auditEvents) {
            this.auditEvents = auditEvents;
        }

        @Override
        public void recordIssued(UUID tenantId, UUID actorId, UUID ncId,
                                 String summary, String detailsJson) {
            auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                    null,
                    actorId == null ? ActorType.SYSTEM : ActorType.USER,
                    actorId,
                    ACTION,
                    RESOURCE_TYPE,
                    ncId,
                    summary,
                    detailsJson,
                    null,
                    null));
        }
    }
}
