package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.application.DirectoryPort;
import com.openlab.qualitos.quality.derogation.application.DirectoryUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

/**
 * L'annuaire du client, lu dans {@code api-core} (ADR 0091).
 *
 * <p>Le moteur n'a pas d'annuaire : les comptes vivent dans {@code api-core}, dans
 * une autre base. C'est lui qui répond, sur {@code /api/v1/directory/accounts/…},
 * une surface ouverte à tout membre authentifié — et le jeton propagé fait que la
 * lecture est celle de l'appelant, non celle du moteur.
 *
 * <p><b>Un compte absent rend vide, une panne lève.</b> La distinction compte : un
 * manager qui n'est pas dans l'annuaire est un choix invalide, à corriger par
 * celui qui le fait ; {@code api-core} injoignable est une panne, et la taire
 * laisserait désigner n'importe quel compte le temps de l'incident.
 */
@Component
public class CoreDirectoryAdapter implements DirectoryPort {

    private static final Logger log = LoggerFactory.getLogger(CoreDirectoryAdapter.class);

    private final CoreGateway gateway;

    public CoreDirectoryAdapter(CoreGateway gateway) {
        this.gateway = gateway;
    }

    /** Ce que l'annuaire rend. On ne lit que ce dont on se sert. */
    record Entry(UUID id, String accountId, String displayName, String email, boolean active) {}

    @Override
    public Optional<String> displayNameOf(UUID accountId) {
        return lire(accountId).map(Entry::displayName).filter(n -> !n.isBlank());
    }

    @Override
    public Optional<String> emailOf(UUID accountId) {
        return lire(accountId).map(Entry::email).filter(a -> a != null && !a.isBlank());
    }

    private Optional<Entry> lire(UUID accountId) {
        if (accountId == null) {
            return Optional.empty();
        }
        try {
            Entry entree = gateway.rest().get()
                    .uri(gateway.base() + "/api/v1/directory/accounts/" + accountId)
                    .headers(gateway::autoriser)
                    .retrieve()
                    .body(Entry.class);
            return Optional.ofNullable(entree);
        } catch (HttpClientErrorException.NotFound absent) {
            // Un 404 dit « pas dans ce client » : c'est une reponse, pas une panne.
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("derogation.directory.unreachable account={} cause={}",
                    accountId, e.getClass().getSimpleName());
            throw new DirectoryUnavailableException(
                    "L'annuaire du client est momentanément injoignable :"
                            + " réessayez dans un instant.", e);
        }
    }
}
