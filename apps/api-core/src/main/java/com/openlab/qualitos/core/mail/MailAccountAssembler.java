package com.openlab.qualitos.core.mail;

import com.openlab.qualitos.core.secret.SecretCipher;
import org.springframework.stereotype.Component;

/**
 * Construit un réglage à partir d'une demande, en gardant ce qui doit l'être.
 *
 * <p>Les deux règles qui vivent ici sont celles qui peuvent couper le courrier
 * d'une organisation, et elles valent autant pour un client que pour un
 * cabinet. Les écrire dans chacun des deux services les aurait fait diverger à
 * la première évolution — et la moitié qui aurait divergé est celle qu'on
 * teste le moins.
 */
@Component
public class MailAccountAssembler {

    private final SecretCipher cipher;
    private final MailHostPolicy policy;

    public MailAccountAssembler(SecretCipher cipher, MailHostPolicy policy) {
        this.cipher = cipher;
        this.policy = policy;
    }

    /**
     * @param avant le réglage en place, ou {@code null} s'il n'y en avait pas
     */
    public MailAccount assembler(MailSettingsDto.Request request, MailAccount avant) {
        // Le refus se dit AU MOMENT DE LA SAISIE. La fabrique d'envoyeurs le
        // refuserait de toute facon, mais plus tard : l'ecran aurait accepte le
        // reglage, et la faute reviendrait sous la forme d'un envoi de facture
        // echoue, des jours apres, loin de l'ecran ou on peut la corriger.
        String refus = policy.refus(request.host());
        if (refus != null) {
            throw new MailHostRefusedException(refus);
        }
        MailAccount apres = new MailAccount(
                normalise(request.fromAddress()), normalise(request.fromName()),
                normalise(request.replyTo()), normalise(request.host()), request.port(),
                request.security(), normalise(request.username()),
                cryptogrammeRetenu(request, avant), request.enabled());

        // La preuve d'essai survit UNIQUEMENT si rien de ce qu'elle prouvait n'a
        // bouge. Garder « verifiee » apres un changement de serveur serait pire
        // que de ne rien afficher : l'ecran affirmerait une chose qu'il n'a pas
        // verifiee, et personne ne relancerait l'essai.
        if (avant != null && !avant.changeCeQuiAEteVerifie(apres)) {
            apres.setVerifiedAt(avant.getVerifiedAt());
        }
        return apres;
    }

    /** Déchiffre le mot de passe, le temps d'un envoi. {@code null} s'il n'y en a pas. */
    public String motDePasseClair(MailAccount reglage) {
        String scelle = reglage.getPasswordCipher();
        if (scelle == null || scelle.isBlank()) {
            // Un relais interne authentifie parfois par adresse IP : exiger un
            // mot de passe interdirait ce cas parfaitement legitime.
            return null;
        }
        return cipher.dechiffrer(scelle);
    }

    /**
     * Quel cryptogramme garder.
     *
     * <p>Un mot de passe <b>absent</b> n'efface rien. L'API ne renvoie jamais le
     * mot de passe ; le formulaire le réaffiche donc vide, et prendre ce vide
     * pour un effacement couperait la messagerie à chaque correction de faute de
     * frappe sur le nom de l'expéditeur. L'effacer se demande explicitement.
     */
    private String cryptogrammeRetenu(MailSettingsDto.Request request, MailAccount avant) {
        if (request.clearPassword()) {
            return null;
        }
        if (request.password() != null && !request.password().isBlank()) {
            return cipher.chiffrer(request.password());
        }
        return avant == null ? null : avant.getPasswordCipher();
    }

    /** Une chaîne vide n'est pas une valeur : « renseigné à vide » n'est pas un serveur. */
    private static String normalise(String valeur) {
        if (valeur == null) {
            return null;
        }
        String net = valeur.strip();
        return net.isEmpty() ? null : net;
    }
}
