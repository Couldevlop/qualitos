package com.openlab.qualitos.core.mail;

import com.openlab.qualitos.core.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Envoie la demande de dérogation au client, depuis la messagerie de son
 * organisation.
 *
 * <p><b>Pourquoi cette surface est ici et non dans le moteur de qualité.</b> Les
 * réglages de messagerie d'un client — hôte, identifiants, adresse d'expédition —
 * vivent dans cette application, avec leur mot de passe chiffré, dans sa propre
 * base. Le moteur ne peut pas les lire, et recopier ce secret chez lui en ferait
 * un second endroit à protéger pour rien. C'est donc {@code api-core} qui envoie,
 * et le moteur qui appelle.
 *
 * <p><b>Ce n'est pas un relais de messagerie générique, et cela compte.</b> Un
 * point d'entrée qui accepterait n'importe quel expéditeur, n'importe quels
 * destinataires et n'importe quelle pièce jointe donnerait à tout membre d'un
 * client le moyen d'écrire au nom de son organisation. Ici :
 *
 * <ul>
 *   <li>le <b>client</b> et l'adresse en <b>copie cachée</b> viennent du jeton,
 *       jamais du corps (§18.2 #2) : l'appelant ne choisit ni l'organisation dont
 *       le courriel paraît partir, ni à qui il est discrètement recopié ;</li>
 *   <li><b>un seul</b> destinataire, validé, et sans saut de ligne — un retour
 *       chariot dans une adresse permettrait de forger d'autres en-têtes à la
 *       suite (OWASP A03) ;</li>
 *   <li>le corps part en <b>texte brut</b> : il porte un commentaire saisi par un
 *       utilisateur, qu'il faudrait échapper en HTML, et un oubli d'échappement
 *       dans un courriel se voit rarement avant qu'il ne serve ;</li>
 *   <li>la pièce jointe doit être un <b>PDF</b>, vérifié sur ses octets d'en-tête
 *       et borné en taille ;</li>
 *   <li>aucun repli sur la messagerie de la plateforme : sans réglage du client,
 *       on <b>refuse</b>. Un courriel qui engage un client ne part pas sous
 *       l'identité de QualitOS parce que son administrateur a oublié de
 *       configurer sa messagerie.</li>
 * </ul>
 *
 * <p>Cette surface est destinée aux appels <b>en grappe</b> du moteur : elle n'a
 * pas à être nommée dans l'ingress.
 */
@RestController
@RequestMapping("/api/v1/mail")
@Tag(name = "Derogation mail", description = "Sends a concession request from the tenant's own mailbox")
public class DerogationMailController {

    private static final Logger log = LoggerFactory.getLogger(DerogationMailController.class);

    /** Les quatre premiers octets d'un PDF. */
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};

    /** La fiche tient en quelques pages ; au-delà, c'est autre chose. */
    private static final int PIECE_MAX_OCTETS = 8 * 1024 * 1024;

    private final TenantMailSettingsService settings;
    private final MailSenderFactory factory;
    private final MailHostPolicy hostPolicy;

    public DerogationMailController(TenantMailSettingsService settings,
                                    MailSenderFactory factory,
                                    MailHostPolicy hostPolicy) {
        this.settings = settings;
        this.factory = factory;
        this.hostPolicy = hostPolicy;
    }

    /**
     * De quoi poster un message : l'expediteur technique, l'adresse affichee, et
     * l'hote, pour le journal.
     */
    private record Expedition(JavaMailSender envoyeur, String expediteur, String hote,
                              String repondreA) {}

    /**
     * Ce que le moteur transmet.
     *
     * <p>Ni client, ni expéditeur, ni copie cachée : ils viennent du jeton.
     */
    public record SendRequest(
            @NotBlank @Email @Size(max = 320) String to,
            @NotBlank @Size(max = 300) String subject,
            @NotBlank @Size(max = 20000) String body,
            @NotBlank @Size(max = 200) String attachmentName,
            @NotBlank @Size(max = 12_000_000) String attachmentBase64
    ) {}

    /**
     * Les rôles qui ont le droit d'écrire à un client au nom de l'organisation.
     *
     * <p><b>Le contrôle est ici, au point d'envoi, et non dans l'omission du
     * chemin dans l'ingress.</b> Un appel interne reste joignable depuis la
     * grappe, et surtout : le jeton propagé est celui d'un <i>utilisateur</i>.
     * Avec {@code isAuthenticated()}, n'importe quel membre du client pouvait
     * faire partir un courriel depuis l'adresse de son organisation, avec le
     * sujet, le corps et la pièce jointe de son choix — un hameçonnage depuis un
     * domaine de confiance, et {@code api-core} n'a aucun moyen de vérifier qu'un
     * dossier de dérogation signé existe derrière, puisque ce dossier vit dans
     * une autre base.
     *
     * <p>C'est exactement la liste de {@code derogation.send} dans le moteur, ni
     * plus large ni plus étroite. Plus étroite, elle rendrait inenvoyable un
     * dossier que le moteur a laissé ouvrir — l'agrégat n'accepte que
     * l'initiateur. Plus large, elle ouvrirait à tout utilisateur le droit
     * d'écrire à un tiers sous l'identité de son organisation.
     *
     * <p>Ce qui est contrôlé ici : un destinataire unique et vérifié, le corps en
     * texte brut, la pièce jointe vérifiée PDF sur ses octets et bornée, la copie
     * cachée tirée du jeton, et le refus net quand la messagerie du client n'est
     * pas configurée.
     */
    private static final String ROLES_CORRESPONDANCE_CLIENT =
            "hasAnyRole('ADMIN','ADMIN_TENANT','QUALITY_DIRECTOR','DIRECTOR_QUALITY',"
                    + "'QUALITY_MANAGER','SUPER_ADMIN')";

    @PostMapping("/derogation")
    @PreAuthorize(ROLES_CORRESPONDANCE_CLIENT)
    @Operation(summary = "Sends a concession request to a customer from the tenant's mailbox")
    public ResponseEntity<Void> send(@Valid @RequestBody SendRequest requete) {
        UUID tenantId = clientDuJeton();
        Expedition expedition = expedition(tenantId);

        String destinataire = adresseUnique(requete.to());
        byte[] piece = pieceJointePdf(requete.attachmentBase64());

        JavaMailSender envoyeur = expedition.envoyeur();
        MimeMessage message = envoyeur.createMimeMessage();
        try {
            // true : message multipart, condition necessaire a la piece jointe.
            MimeMessageHelper aide = new MimeMessageHelper(message, true, "UTF-8");
            aide.setFrom(expedition.expediteur());
            aide.setTo(destinataire);
            if (expedition.repondreA() != null && !expedition.repondreA().isBlank()) {
                aide.setReplyTo(expedition.repondreA());
            }
            // L'initiateur en COPIE CACHEE, et son adresse vient du jeton : il
            // garde une trace de ce qu'il a envoye sans que le client voie une
            // adresse interne.
            adresseDuJeton().ifPresent(adresse -> {
                try {
                    aide.setBcc(adresse);
                } catch (MessagingException e) {
                    log.warn("derogation.mail.bcc-ignored cause={}", e.getClass().getSimpleName());
                }
            });
            aide.setSubject(uneSeuleLigne(requete.subject()));
            aide.setText(requete.body(), false);
            aide.addAttachment(nomDeFichier(requete.attachmentName()),
                    new ByteArrayResource(piece), "application/pdf");
        } catch (MessagingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Préparation du courriel impossible.", e);
        }
        try {
            envoyeur.send(message);
        } catch (MailException panne) {
            // Le serveur de messagerie refuse ou ne repond pas. Sans ce filet, la
            // panne tombait dans le gestionnaire generique et ressortait en
            // « An unexpected error occurred » : une phrase en anglais, sans
            // rapport avec le geste, pour un incident dont l'utilisateur doit
            // seulement savoir qu'il peut renvoyer. La cause technique reste au
            // journal, elle n'a rien a faire a l'ecran (OWASP A09).
            log.warn("derogation.mail.send-failed host={} cause={}",
                    expedition.hote(), panne.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Le serveur de messagerie n'a pas accepté l'envoi."
                            + " Le dossier n'est pas parti, et reste renvoyable.", panne);
        }
        // Ni le destinataire ni le corps ne sont journalises (section 22 #9).
        log.info("derogation.mail.sent host={} from={}",
                expedition.hote(), expedition.expediteur());
        return ResponseEntity.accepted().build();
    }

    /**
     * De quelle messagerie part ce courriel.
     *
     * <p><b>De celle du client, et d'aucune autre.</b> Le reglage vit dans les
     * parametres de son organisation, comme tout parametrage (ADR 0127), et il
     * y designe une adresse generique — celle du service qualite, non celle
     * d'une personne. Le courriel engage l'organisation vis-a-vis de son
     * client : il doit partir d'une adresse que ce client reconnait.
     *
     * <p><b>Pas de repli sur une messagerie de plateforme</b>, meme quand il en
     * existe une. C'est ce qui distingue cet envoi de celui d'une facture : une
     * facture emane de QualitOS, une derogation emane du fournisseur de son
     * destinataire. Partir d'une adresse generique de plateforme ferait arriver
     * chez le client un courrier qui l'engage sous un nom qui n'est pas celui
     * de son fournisseur.
     *
     * <p>Faute de reglage, le refus nomme ce que l'utilisateur peut faire. Le
     * dossier reste « Approuvee par la qualite », donc renvoyable des que la
     * messagerie est reglee.
     */
    private Expedition expedition(UUID tenantId) {
        MailAccount reglage = settings.pourEnvoi(tenantId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT,
                        "La messagerie de votre organisation n'est pas configurée :"
                                + " réglez-la dans l'administration avant d'envoyer une"
                                + " dérogation à un client."));
        String refus = hostPolicy.refus(reglage.getHost());
        if (refus != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, refus);
        }
        return new Expedition(factory.creer(reglage, settings.motDePasseClair(reglage)),
                reglage.getFromAddress(), reglage.getHost(), reglage.getReplyTo());
    }

    // ---- ce qui vient du jeton, et de lui seul -------------------------------

    private static UUID clientDuJeton() {
        String brut = TenantContext.getTenantId();
        if (brut == null || brut.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Aucun client dans le jeton.");
        }
        try {
            return UUID.fromString(brut);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Client du jeton invalide.");
        }
    }

    /**
     * L'adresse de l'appelant, pour la copie cachée.
     *
     * <p>Absente quand le jeton n'en porte pas : on envoie alors sans copie,
     * plutôt que de refuser. Le courriel au client est l'objet de l'appel ; la
     * copie est un confort pour l'initiateur.
     */
    private static Optional<String> adresseDuJeton() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        String adresse = jwt.getClaimAsString("email");
        if (adresse == null || adresse.isBlank()
                || adresse.chars().anyMatch(c -> c == '\r' || c == '\n')) {
            return Optional.empty();
        }
        return Optional.of(adresse.trim());
    }

    // ---- les contrôles ------------------------------------------------------

    private static String adresseUnique(String adresse) {
        String net = adresse.trim();
        if (net.chars().anyMatch(c -> c == '\r' || c == '\n')
                || net.indexOf(',') >= 0 || net.indexOf(';') >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Une seule adresse, sans saut de ligne.");
        }
        return net;
    }

    /**
     * La pièce jointe doit être un PDF.
     *
     * <p>Vérifié sur les octets, pas sur le nom : le nom est une étiquette, et
     * cette pièce part à l'extérieur sous l'identité du client.
     */
    private static byte[] pieceJointePdf(String base64) {
        byte[] octets;
        try {
            octets = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La pièce jointe n'est pas un base64 valide.");
        }
        if (octets.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La pièce jointe est vide.");
        }
        if (octets.length > PIECE_MAX_OCTETS) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "La pièce jointe dépasse " + (PIECE_MAX_OCTETS / 1024 / 1024) + " Mo.");
        }
        // La LONGUEUR se verifie avant les octets : trois octets suffisaient a
        // sortir du tableau dans la boucle de comparaison.
        if (octets.length < PDF_MAGIC.length
                || !Arrays.equals(Arrays.copyOf(octets, PDF_MAGIC.length), PDF_MAGIC)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La pièce jointe doit être un PDF.");
        }
        return octets;
    }

    /**
     * Le sujet sur UNE ligne.
     *
     * <p>Un en-tête de courriel se termine par CRLF : y glisser un saut de ligne
     * permettrait d'en forger d'autres à la suite — un {@code Bcc:} vers un
     * tiers — et le message partirait où l'auteur du sujet l'aurait décidé.
     */
    static String uneSeuleLigne(String sujet) {
        return sujet == null ? "" : sujet.replaceAll("[\\r\\n]+", " ").strip();
    }

    /** Un nom de fichier, et rien qui puisse désigner un chemin. */
    static String nomDeFichier(String nom) {
        String net = nom == null || nom.isBlank() ? "derogation.pdf" : nom.strip();
        String propre = net.replaceAll("[^A-Za-z0-9._-]", "_");
        return propre.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
                ? propre : propre + ".pdf";
    }
}
