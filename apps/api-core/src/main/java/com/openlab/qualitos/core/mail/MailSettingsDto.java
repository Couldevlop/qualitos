package com.openlab.qualitos.core.mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Ce qui entre et ce qui sort pour la messagerie d'une organisation. */
public final class MailSettingsDto {

    private MailSettingsDto() {
    }

    /**
     * Ce qu'un administrateur envoie.
     *
     * <p>Le mot de passe est <b>facultatif à la modification</b>, et c'est
     * délibéré. Un formulaire qui ne peut pas réafficher le mot de passe — il ne
     * le peut pas, l'API ne le renvoie jamais — renverrait un champ vide, et
     * traiter ce vide comme « efface le mot de passe » couperait la messagerie
     * d'un client à chaque correction de faute de frappe sur le nom de
     * l'expéditeur.
     *
     * <p>Vide signifie donc « n'y touche pas ». Pour l'effacer vraiment, il
     * existe {@code clearPassword}, qui le dit.
     */
    public record Request(
            @NotBlank @Email @Size(max = 320) String fromAddress,
            @Size(max = 120) String fromName,
            @Email @Size(max = 320) String replyTo,
            @NotBlank @Size(max = 255) String host,
            @Min(1) @Max(65535) int port,
            @NotNull MailSecurity security,
            @Size(max = 320) String username,
            @Size(max = 512) String password,
            boolean clearPassword,
            boolean enabled) {
    }

    /**
     * Ce que l'API rend.
     *
     * <p>Jamais le mot de passe, ni même son cryptogramme — rendre le
     * cryptogramme offrirait à qui lit l'écran une cible à attaquer hors ligne,
     * pour aucun gain : personne n'a besoin de le voir. Un booléen suffit à
     * dire qu'il y en a un.
     */
    public record Response(
            boolean configured,
            String fromAddress,
            String fromName,
            String replyTo,
            String host,
            Integer port,
            MailSecurity security,
            String username,
            boolean hasPassword,
            boolean enabled,
            Instant verifiedAt) {

        /** Une organisation qui n'a rien réglé : elle emprunte celle de la plateforme. */
        public static Response absente() {
            return new Response(false, null, null, null, null, null, null, null, false, false, null);
        }

        public static Response from(MailAccount c) {
            return new Response(true, c.getFromAddress(), c.getFromName(), c.getReplyTo(),
                    c.getHost(), c.getPort(), c.getSecurity(), c.getUsername(),
                    c.getPasswordCipher() != null && !c.getPasswordCipher().isBlank(),
                    c.isEnabled(), c.getVerifiedAt());
        }
    }

    /**
     * Le résultat d'un envoi d'essai.
     *
     * <p>Il porte le motif d'échec en clair. Un « échec » sans motif oblige à
     * deviner entre un nom de serveur faux, un port fermé, un mot de passe
     * refusé et un pare-feu — et c'est exactement ce qui fait abandonner le
     * réglage d'une messagerie.
     */
    public record TestResult(boolean success, String detail, Instant verifiedAt) {
    }
}
