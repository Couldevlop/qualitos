package com.openlab.qualitos.quality.derogation.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.audit.AuditReminderMailer;
import com.openlab.qualitos.quality.derogation.application.DerogationMailException;
import com.openlab.qualitos.quality.derogation.application.DerogationMailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Optional;
import java.util.UUID;

/**
 * Les deux courriels du processus, chacun par le chemin qui lui convient.
 *
 * <p><b>Celui du manager est interne</b> : il part de la messagerie du
 * déploiement, par le dispositif SMTP déjà en place, et une panne ne doit pas
 * faire échouer la demande — la notification interne porte déjà l'alerte rouge.
 * Sans configuration SMTP, aucun {@code AuditReminderMailer} n'existe et la
 * demande se contente de la notification : c'est le comportement voulu.
 *
 * <p><b>Celui du client engage le client</b> : il part de la messagerie que son
 * administrateur a réglée (ADR 0127), avec l'initiateur en copie cachée. Ces
 * réglages — et leur secret — vivent dans {@code api-core}, dans une autre base :
 * c'est donc {@code api-core} qui envoie, et toute panne remonte, pour que le
 * dossier ne passe pas « En attente client » si rien n'est parti.
 */
@Component
public class CoreMailAdapter implements DerogationMailPort {

    private static final Logger log = LoggerFactory.getLogger(CoreMailAdapter.class);

    private final CoreGateway gateway;
    private final ObjectProvider<AuditReminderMailer> smtpInterne;
    private final CoreDirectoryAdapter annuaire;

    public CoreMailAdapter(CoreGateway gateway,
                           ObjectProvider<AuditReminderMailer> smtpInterne,
                           CoreDirectoryAdapter annuaire) {
        this.gateway = gateway;
        this.smtpInterne = smtpInterne;
        this.annuaire = annuaire;
    }

    /**
     * Au manager : le lien qui le conduit dans QualitOS.
     *
     * <p>Avale et journalise. Le contrat du port le dit, et sans cela une panne
     * de relais SMTP annulerait la demande de validation elle-même — perdre la
     * demande parce qu'un courriel n'est pas parti serait l'inverse de ce qu'on
     * veut, puisque l'alerte rouge est déjà posée.
     */
    @Override
    public void notifyManager(UUID managerAccountId, String reference, String link) {
        AuditReminderMailer mailer = smtpInterne.getIfAvailable();
        if (mailer == null) {
            return;
        }
        try {
            Optional<String> adresse = annuaire.emailOf(managerAccountId);
            if (adresse.isEmpty()) {
                log.info("derogation.manager-mail.skipped reference={} reason=no-address",
                        reference);
                return;
            }
            mailer.send(adresse.get(),
                    "Dérogation à valider : " + reference,
                    "Une demande de dérogation attend votre validation dans QualitOS.\n\n"
                            + link + "\n");
        } catch (RuntimeException e) {
            log.warn("derogation.manager-mail.failed reference={} cause={}",
                    reference, e.getClass().getSimpleName());
        }
    }

    /**
     * Au client : un simple courriel, la dérogation en pièce jointe.
     *
     * <p>Ni le client (tenant) ni l'adresse en copie cachée ne sont transmis dans
     * le corps : {@code api-core} les tire du jeton propagé. Les lui dire ici
     * laisserait choisir de quelle organisation un courriel paraît partir, et à
     * qui il est discrètement recopié (§18.2 #2).
     */
    @Override
    public void sendToCustomer(CustomerMail courriel) {
        verifierDestinataire(courriel.to());
        Map<String, Object> corps = Map.of(
                "to", courriel.to(),
                "subject", courriel.subject(),
                "body", courriel.body(),
                "attachmentName", courriel.attachmentName(),
                "attachmentBase64", Base64.getEncoder().encodeToString(courriel.pdf()));
        try {
            gateway.restEnvoi().post()
                    .uri(gateway.base() + "/api/v1/mail/derogation")
                    .headers(gateway::autoriser)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corps)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException refus) {
            // Le message d'api-core est destine a l'utilisateur : messagerie non
            // configuree, hote refuse, adresse rejetee. On le laisse passer --
            // la PHRASE, et elle seule. Recopier le corps entier revenait a ne
            // rien laisser passer du tout : l'ecran refuse par principe
            // d'afficher un detail de serveur (OWASP A09), et la raison partait
            // avec le bruit. L'utilisateur lisait « Erreur serveur, reessayez »
            // pour un reglage qu'il etait le seul a pouvoir faire.
            throw new DerogationMailException(raisonDeclaree(refus), refus);
        } catch (RestClientException panne) {
            throw new DerogationMailException(
                    "Le service de messagerie est injoignable : la demande n'est pas"
                            + " partie, et reste renvoyable.", panne);
        }
    }

    /** Ce qui trahit une trace technique plutot qu'une phrase ecrite pour un humain. */
    private static final Pattern RETRACE = Pattern.compile("[\\r\\n]|\\bat [\\w.$]+\\(");

    /** La longueur au-dela de laquelle l'ecran ecarte un message : meme borne des deux cotes. */
    private static final int LONGUEUR_MAX = 300;

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * La raison du refus, telle qu'api-core l'a DECLAREE.
     *
     * <p>Seul {@code detail} est lu, parce que c'est le champ ou api-core ecrit
     * la phrase destinee a un humain -- « la messagerie de votre organisation
     * n'est pas configuree ». Les memes garde-fous que l'ecran s'appliquent ici,
     * et non la-bas : une seule ligne, longueur bornee, rien qui ressemble a une
     * trace. Un amont qui deborderait ce cadre retombe sur la phrase generique
     * plutot que de projeter ses entrailles jusqu'au navigateur.
     *
     * <p>Le corps brut n'est jamais relaye : il porte {@code instance}, le
     * chemin interne appele, et ce que l'amont aurait mis dans ses propres
     * champs.
     */
    private static String raisonDeclaree(HttpStatusCodeException refus) {
        String generique = "Le courriel n'est pas parti : le service de messagerie a refuse"
                + " l'envoi. Le dossier reste renvoyable.";
        try {
            JsonNode corps = JSON.readTree(refus.getResponseBodyAsString());
            JsonNode detail = corps.get("detail");
            if (detail == null || !detail.isTextual()) {
                return generique;
            }
            String phrase = detail.asText().trim();
            if (phrase.isEmpty() || phrase.length() > LONGUEUR_MAX
                    || RETRACE.matcher(phrase).find()) {
                return generique;
            }
            return phrase;
        } catch (com.fasterxml.jackson.core.JacksonException illisible) {
            return generique;
        }
    }

    /**
     * Une adresse, une seule, et sans saut de ligne.
     *
     * <p>Un retour chariot dans une adresse permettrait de forger d'autres
     * en-têtes à la suite — un destinataire de plus, par exemple — et le courrier
     * partirait où l'auteur de l'adresse l'aurait décidé (OWASP A03).
     * {@code api-core} refait le contrôle ; on ne lui envoie pas pour autant ce
     * qu'on sait mauvais.
     */
    private static void verifierDestinataire(String adresse) {
        if (adresse == null || adresse.isBlank()) {
            throw new DerogationMailException("L'adresse du client est obligatoire.");
        }
        if (adresse.chars().anyMatch(c -> c == '\r' || c == '\n')) {
            throw new DerogationMailException(
                    "L'adresse du client ne peut pas contenir de saut de ligne.");
        }
        if (adresse.indexOf(',') >= 0 || adresse.indexOf(';') >= 0) {
            throw new DerogationMailException(
                    "Une seule adresse : la demande part à un client, pas à une liste.");
        }
    }
}
