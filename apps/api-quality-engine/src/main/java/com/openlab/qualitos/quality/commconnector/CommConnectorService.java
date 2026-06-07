package com.openlab.qualitos.quality.commconnector;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import com.openlab.qualitos.quality.itsm.SecretCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gère les connexions de communication sortante et la notification des événements
 * qualité vers Teams / Slack / Mattermost (CLAUDE.md §13.3).
 *
 * <p><b>Sécurité</b> : l'URL d'incoming-webhook (le secret) n'est lue que depuis la
 * requête de création/MAJ, chiffrée via {@link SecretCipher} (réutilisé du module itsm),
 * stockée en ciphertext et JAMAIS ré-exposée. Le déchiffrement n'a lieu qu'au moment de
 * l'envoi, en mémoire, et n'est jamais loggé.
 *
 * <p><b>OFF par défaut</b> : {@link #notify(UUID, CommEvent)} ne fait rien tant qu'aucune
 * connexion ACTIVE n'existe pour le tenant — aucun appel sortant, aucune dépendance.
 *
 * <p><b>Politique d'échec</b> : comme itsm/webhooks, {@code consecutiveFailures} est
 * incrémenté par connexion sur erreur ; au-delà de
 * {@link #MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE}, la connexion bascule en
 * DISABLED_ON_ERRORS pour éviter un effet boule de neige.
 */
@Service
public class CommConnectorService {

    private static final Logger log = LoggerFactory.getLogger(CommConnectorService.class);
    static final int MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE = 10;

    private final CommConnectionRepository connectionRepo;
    private final SecretCipher cipher;
    private final Map<CommProvider, CommProviderClient> clients;

    public CommConnectorService(CommConnectionRepository connectionRepo,
                                SecretCipher cipher,
                                List<CommProviderClient> providerBeans) {
        this.connectionRepo = connectionRepo;
        this.cipher = cipher;
        Map<CommProvider, CommProviderClient> map = new EnumMap<>(CommProvider.class);
        for (CommProviderClient c : providerBeans) map.put(c.provider(), c);
        this.clients = map;
    }

    // ---------- Connections (CRUD) ----------

    @Transactional
    public CommDto.ConnectionResponse createConnection(CommDto.CreateConnectionRequest req) {
        UUID tenantId = requireTenantId();
        CommConnection c = new CommConnection();
        c.setTenantId(tenantId);
        c.setName(req.name());
        c.setProvider(req.provider());
        c.setWebhookUrlCipher(cipher.encrypt(req.webhookUrl()));
        c.setChannel(req.channel());
        c.setStatus(ConnectionStatus.ACTIVE);
        c.setCreatedBy(req.createdBy());
        return toResponse(connectionRepo.save(c));
    }

    @Transactional(readOnly = true)
    public Page<CommDto.ConnectionResponse> listConnections(Pageable pageable) {
        UUID tenantId = requireTenantId();
        return connectionRepo.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CommDto.ConnectionResponse getConnection(UUID id) {
        return toResponse(load(id));
    }

    @Transactional
    public CommDto.ConnectionResponse updateConnection(UUID id, CommDto.UpdateConnectionRequest req) {
        CommConnection c = load(id);
        if (req.name() != null) c.setName(req.name());
        if (req.webhookUrl() != null) c.setWebhookUrlCipher(cipher.encrypt(req.webhookUrl()));
        if (req.channel() != null) c.setChannel(req.channel());
        if (req.status() != null) {
            c.setStatus(req.status());
            // Réactivation manuelle → reset du compteur d'échecs.
            if (req.status() == ConnectionStatus.ACTIVE) c.setConsecutiveFailures(0);
        }
        return toResponse(connectionRepo.save(c));
    }

    @Transactional
    public void deleteConnection(UUID id) {
        connectionRepo.delete(load(id));
    }

    // ---------- Test ----------

    /**
     * Envoie un message de test sur la connexion (vérifie URL + canal depuis l'UI).
     * Met à jour les compteurs comme un envoi réel. Ne propage pas l'échec en exception :
     * renvoie un {@link CommDto.TestResult} avec {@code success=false} et le message.
     */
    @Transactional
    public CommDto.TestResult test(UUID id) {
        CommConnection c = load(id);
        CommProviderClient client = clients.get(c.getProvider());
        if (client == null) {
            return new CommDto.TestResult(c.getId(), false,
                    "No client registered for provider " + c.getProvider());
        }
        CommMessage msg = new CommMessage(
                "QualitOS — test de connexion",
                "Ceci est un message de test envoyé depuis QualitOS. Si vous le voyez, "
                        + "la connexion **" + c.getName() + "** fonctionne.",
                CommSeverity.INFO,
                null, null,
                List.of(new AbstractMap.SimpleEntry<>("Connexion", c.getName()),
                        new AbstractMap.SimpleEntry<>("Fournisseur", c.getProvider().name())));
        try {
            dispatch(c, client, msg);
            markSuccess(c);
            connectionRepo.save(c);
            return new CommDto.TestResult(c.getId(), true, null);
        } catch (CommSendException ex) {
            failOnce(c);
            connectionRepo.save(c);
            return new CommDto.TestResult(c.getId(), false, ex.getMessage());
        }
    }

    // ---------- Notify (event wiring entry point) ----------

    /**
     * Notifie un événement qualité à toutes les connexions ACTIVE du tenant.
     *
     * <p>OFF par défaut : si aucune connexion ACTIVE, retourne immédiatement (aucun appel).
     * Le {@link TenantContext} est positionné par l'appelant (le consumer Kafka) ; on
     * accepte aussi un {@code tenantId} explicite pour découpler du contexte.
     *
     * <p>Best-effort : un échec sur une connexion n'empêche pas les autres (chaque échec
     * est compté/auto-désactivé indépendamment). Ne propage jamais d'exception à
     * l'appelant (notification non bloquante).
     *
     * @return nombre de connexions notifiées avec succès
     */
    @Transactional
    public int notify(UUID tenantId, CommEvent event) {
        if (tenantId == null || event == null) return 0;
        List<CommConnection> active =
                connectionRepo.findByTenantIdAndStatus(tenantId, ConnectionStatus.ACTIVE);
        if (active.isEmpty()) return 0; // OFF par défaut

        CommMessage msg = toMessage(event);
        int sent = 0;
        for (CommConnection c : active) {
            CommProviderClient client = clients.get(c.getProvider());
            if (client == null) continue;
            try {
                dispatch(c, client, msg);
                markSuccess(c);
                sent++;
            } catch (CommSendException ex) {
                log.warn("Comm notify failed for connection {} ({}): {}",
                        c.getId(), c.getProvider(), ex.getMessage());
                failOnce(c);
            }
            connectionRepo.save(c);
        }
        return sent;
    }

    // ---------- helpers ----------

    /** Déchiffre l'URL et délègue au client (le secret n'est jamais loggé). */
    private void dispatch(CommConnection c, CommProviderClient client, CommMessage msg) {
        final String url;
        try {
            url = cipher.decrypt(c.getWebhookUrlCipher());
        } catch (RuntimeException ex) {
            throw new CommSendException("Webhook URL decryption failed");
        }
        client.send(c, url, msg);
    }

    /** Formate un {@link CommEvent} en {@link CommMessage} premium avec deep-link. */
    CommMessage toMessage(CommEvent event) {
        CommSeverity sev = event.severity() != null ? event.severity() : event.kind().defaultSeverity();
        String title = event.title() != null ? event.title() : event.kind().defaultTitle();

        List<Map.Entry<String, String>> facts = new ArrayList<>();
        facts.add(new AbstractMap.SimpleEntry<>("Sévérité", sev.name()));
        if (event.resourceType() != null) {
            facts.add(new AbstractMap.SimpleEntry<>("Ressource", event.resourceType()));
        }

        String linkLabel = null;
        String linkUrl = null;
        if (event.resourceType() != null && event.resourceId() != null) {
            linkLabel = "Ouvrir dans QualitOS";
            linkUrl = deepLink(event.resourceType(), event.resourceId());
        }
        return new CommMessage(title, event.summary(), sev, linkLabel, linkUrl, facts);
    }

    /** Deep-link relatif vers la ressource (le front résout l'origine). */
    private String deepLink(String resourceType, String resourceId) {
        return "/app/" + resourceType.toLowerCase().replace('_', '-') + "/" + resourceId;
    }

    private void markSuccess(CommConnection c) {
        Instant now = Instant.now();
        c.setLastNotifiedAt(now);
        c.setLastSuccessAt(now);
        c.setConsecutiveFailures(0);
    }

    private void failOnce(CommConnection c) {
        c.setLastNotifiedAt(Instant.now());
        c.setConsecutiveFailures(c.getConsecutiveFailures() + 1);
        if (c.getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE
                && c.getStatus() == ConnectionStatus.ACTIVE) {
            c.setStatus(ConnectionStatus.DISABLED_ON_ERRORS);
            log.warn("Comm connection {} auto-disabled after {} consecutive failures",
                    c.getId(), c.getConsecutiveFailures());
        }
    }

    private CommConnection load(UUID id) {
        UUID tenantId = requireTenantId();
        return connectionRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CommConnectionNotFoundException(id));
    }

    private CommDto.ConnectionResponse toResponse(CommConnection c) {
        return new CommDto.ConnectionResponse(
                c.getId(), c.getTenantId(), c.getName(), c.getProvider(),
                c.getChannel(), c.getStatus(), c.getConsecutiveFailures(),
                c.getLastNotifiedAt(), c.getLastSuccessAt(),
                c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt());
    }

    // exposé pour les tests
    Map<CommProvider, CommProviderClient> clientsView() { return new HashMap<>(clients); }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
