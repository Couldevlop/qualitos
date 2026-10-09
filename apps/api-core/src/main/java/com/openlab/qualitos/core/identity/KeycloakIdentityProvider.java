package com.openlab.qualitos.core.identity;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Les comptes dans Keycloak, par son API d'administration.
 *
 * <p><b>Un compte de service dédié</b> ({@code qualitos-provisioner}, flux
 * client_credentials) qui n'a QUE les droits de gestion des utilisateurs du
 * realm. Il ne porte aucun client : c'est api-core qui borne chaque opération
 * au client du jeton de l'appelant, avant d'appeler Keycloak.
 *
 * <p><b>Le client est un attribut du compte</b> ({@code tenant_id}) : le mappeur
 * du client web le publie dans le jeton, que tous les services lisent.
 *
 * <p><b>L'entrée du nouveau membre.</b> En mode {@code email}, Keycloak envoie le
 * lien de création du mot de passe (exige un SMTP sur le realm). En mode
 * {@code temporary-password}, un mot de passe provisoire est tiré au hasard,
 * rendu UNE fois à l'administrateur, et Keycloak exige d'en changer à la
 * première connexion.
 *
 * <p>Sans secret configuré, chaque opération échoue en
 * {@link IdentityProviderException} : jamais de compte « créé » qui n'existe pas.
 */
public class KeycloakIdentityProvider implements IdentityProvider {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentityProvider.class);
    static final String TENANT_ATTRIBUTE = "tenant_id";
    /** Sans caractères ambigus (0/O, 1/l/I) : le mot de passe se recopie à la main. */
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final int PASSWORD_LENGTH = 16;

    private final IdentityProperties props;
    private final RestClient http;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    private String token;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public KeycloakIdentityProvider(IdentityProperties props, RestClient.Builder builder, Clock clock) {
        this.props = props;
        this.http = builder.build();
        this.clock = clock;
    }

    @Override
    public CreatedAccount create(NewAccount account) {
        Map<String, Object> corps = Map.of(
                "username", account.email(),
                "email", account.email(),
                "firstName", nonNull(account.firstName()),
                "lastName", nonNull(account.lastName()),
                "enabled", true,
                "emailVerified", false,
                "attributes", Map.of(TENANT_ATTRIBUTE, List.of(account.tenantId().toString())),
                "requiredActions", List.of("UPDATE_PASSWORD"));
        URI lieu;
        try {
            lieu = http.post().uri(admin("/users"))
                    .headers(h -> h.setBearerAuth(bearer()))
                    .contentType(MediaType.APPLICATION_JSON).body(corps)
                    .retrieve().toBodilessEntity().getHeaders().getLocation();
        } catch (HttpClientErrorException.Conflict e) {
            throw new AccountAlreadyExistsException(account.email());
        } catch (RestClientException e) {
            throw new IdentityProviderException("Le fournisseur d'identité a refusé la création du compte.", e);
        }
        if (lieu == null) {
            throw new IdentityProviderException("Le fournisseur d'identité n'a pas rendu l'identifiant du compte créé.");
        }
        String id = lieu.getPath().substring(lieu.getPath().lastIndexOf('/') + 1);
        try {
            setRoles(id, account.roles());
            if (props.emailInvitations()) {
                call(() -> http.put().uri(admin("/users/" + id + "/execute-actions-email"))
                        .headers(h -> h.setBearerAuth(bearer()))
                        .contentType(MediaType.APPLICATION_JSON).body(List.of("UPDATE_PASSWORD", "VERIFY_EMAIL"))
                        .retrieve().toBodilessEntity(), "l'envoi de l'invitation");
                log.info("identity.account.created account_id={} tenant_id={} invitation=email", id, account.tenantId());
                return new CreatedAccount(id, null, true);
            }
            String provisoire = temporaryPassword();
            call(() -> http.put().uri(admin("/users/" + id + "/reset-password"))
                    .headers(h -> h.setBearerAuth(bearer()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("type", "password", "value", provisoire, "temporary", true))
                    .retrieve().toBodilessEntity(), "la pose du mot de passe provisoire");
            // Journal structuré : le compte et le client, jamais le mot de passe (§22-9).
            log.info("identity.account.created account_id={} tenant_id={} invitation=temporary-password",
                    id, account.tenantId());
            return new CreatedAccount(id, provisoire, false);
        } catch (RuntimeException e) {
            // Un compte à moitié réglé (sans rôle, sans moyen d'entrer) ne reste pas.
            delete(id);
            throw e;
        }
    }

    @Override
    public void setRoles(String accountId, Set<String> roles) {
        List<JsonNode> actuels = list(http.get().uri(admin("/users/" + accountId + "/role-mappings/realm"))
                .headers(h -> h.setBearerAuth(bearer())), "la lecture des rôles du compte");
        List<JsonNode> aRetirer = new ArrayList<>();
        for (JsonNode r : actuels) {
            String nom = r.path("name").asText();
            if (PlatformRoles.ASSIGNABLE.contains(nom) && !roles.contains(nom)) {
                aRetirer.add(r);
            }
        }
        List<String> manquants = roles.stream()
                .filter(nom -> actuels.stream().noneMatch(r -> nom.equals(r.path("name").asText())))
                .toList();
        List<JsonNode> aAjouter = new ArrayList<>();
        if (!manquants.isEmpty()) {
            // Les rôles attribuables À CE COMPTE : lecture permise par la seule gestion
            // des utilisateurs, sans droit de lecture sur tout le realm (moindre privilège).
            List<JsonNode> disponibles = list(http.get()
                    .uri(admin("/users/" + accountId + "/role-mappings/realm/available"))
                    .headers(h -> h.setBearerAuth(bearer())), "la lecture des rôles attribuables");
            for (String nom : manquants) {
                aAjouter.add(disponibles.stream().filter(r -> nom.equals(r.path("name").asText())).findFirst()
                        .orElseThrow(() -> new IdentityProviderException(
                                "Rôle introuvable dans le fournisseur d'identité : " + nom)));
            }
        }
        if (!aRetirer.isEmpty()) {
            call(() -> http.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(admin("/users/" + accountId + "/role-mappings/realm"))
                    .headers(h -> h.setBearerAuth(bearer()))
                    .contentType(MediaType.APPLICATION_JSON).body(aRetirer)
                    .retrieve().toBodilessEntity(), "le retrait de rôles");
        }
        if (!aAjouter.isEmpty()) {
            call(() -> http.post().uri(admin("/users/" + accountId + "/role-mappings/realm"))
                    .headers(h -> h.setBearerAuth(bearer()))
                    .contentType(MediaType.APPLICATION_JSON).body(aAjouter)
                    .retrieve().toBodilessEntity(), "l'ajout de rôles");
        }
    }

    @Override
    public void setEnabled(String accountId, boolean enabled) {
        call(() -> http.put().uri(admin("/users/" + accountId))
                .headers(h -> h.setBearerAuth(bearer()))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("enabled", enabled))
                .retrieve().toBodilessEntity(), enabled ? "la réactivation du compte" : "la désactivation du compte");
    }

    @Override
    public Optional<UUID> tenantOf(String accountId) {
        JsonNode compte;
        try {
            compte = http.get().uri(admin("/users/" + accountId))
                    .headers(h -> h.setBearerAuth(bearer()))
                    .retrieve().body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new IdentityProviderException("Le fournisseur d'identité ne répond pas sur la lecture du compte.", e);
        }
        JsonNode valeurs = compte == null ? null : compte.path("attributes").path(TENANT_ATTRIBUTE);
        if (valeurs == null || !valeurs.isArray() || valeurs.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(valeurs.get(0).asText()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String accountId) {
        try {
            http.delete().uri(admin("/users/" + accountId))
                    .headers(h -> h.setBearerAuth(bearer()))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            // La compensation qui échoue ne masque pas l'échec d'origine : on le dit.
            log.error("identity.account.compensation-failed account_id={}", accountId);
        }
    }

    // ---------- interne ----------

    private List<JsonNode> list(RestClient.RequestHeadersSpec<?> requete, String quoi) {
        try {
            JsonNode tableau = requete.retrieve().body(JsonNode.class);
            return tableau == null ? List.of()
                    : StreamSupport.stream(tableau.spliterator(), false).toList();
        } catch (RestClientException e) {
            throw new IdentityProviderException("Le fournisseur d'identité a refusé " + quoi + ".", e);
        }
    }

    private void call(Runnable requete, String quoi) {
        try {
            requete.run();
        } catch (RestClientException e) {
            throw new IdentityProviderException("Le fournisseur d'identité a refusé " + quoi + ".", e);
        }
    }

    private String admin(String chemin) {
        return props.baseUrl() + "/admin/realms/" + props.realm() + chemin;
    }

    /** Le jeton du compte de service, gardé jusqu'à trente secondes avant son expiration. */
    synchronized String bearer() {
        if (!props.configured()) {
            throw new IdentityProviderException(
                    "Le fournisseur d'identité n'est pas configuré (secret du compte de service absent) : "
                            + "impossible de créer ou de modifier un compte.");
        }
        Instant maintenant = clock.instant();
        if (token != null && maintenant.isBefore(tokenExpiresAt)) {
            return token;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        JsonNode reponse;
        try {
            reponse = http.post()
                    .uri(props.baseUrl() + "/realms/" + props.realm() + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            throw new IdentityProviderException("Le compte de service n'a pas pu s'authentifier auprès du fournisseur d'identité.", e);
        }
        if (reponse == null || !reponse.hasNonNull("access_token")) {
            throw new IdentityProviderException("Le fournisseur d'identité n'a pas rendu de jeton.");
        }
        token = reponse.get("access_token").asText();
        long duree = Math.max(0, reponse.path("expires_in").asLong(60) - 30);
        tokenExpiresAt = maintenant.plusSeconds(duree);
        return token;
    }

    /** 16 caractères, au moins une majuscule, une minuscule et un chiffre (politique du realm). */
    String temporaryPassword() {
        String tous = UPPER + LOWER + DIGITS;
        char[] p = new char[PASSWORD_LENGTH];
        p[0] = UPPER.charAt(random.nextInt(UPPER.length()));
        p[1] = LOWER.charAt(random.nextInt(LOWER.length()));
        p[2] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        for (int i = 3; i < PASSWORD_LENGTH; i++) {
            p[i] = tous.charAt(random.nextInt(tous.length()));
        }
        for (int i = PASSWORD_LENGTH - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        return new String(p);
    }

    private static String nonNull(String s) {
        return s == null ? "" : s.strip();
    }
}
