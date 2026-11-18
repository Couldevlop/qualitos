package com.openlab.qualitos.core.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.util.Objects;

/**
 * Les réglages d'une boîte d'envoi, partagés par le client et le cabinet.
 *
 * <p>Un seul bloc de champs pour les deux, parce que la règle <b>métier</b> est
 * la même : ce qui diffère entre un client et un cabinet, c'est qui a le droit
 * de lire et d'écrire la ligne, pas ce qu'elle contient. Écrire ces dix champs
 * deux fois ferait diverger les deux moitiés à la première évolution.
 */
@Embeddable
public class MailAccount {

    @Column(name = "from_address", nullable = false, length = 320)
    private String fromAddress;

    @Column(name = "from_name", length = 120)
    private String fromName;

    @Column(name = "reply_to", length = 320)
    private String replyTo;

    @Column(name = "host", nullable = false, length = 255)
    private String host;

    @Column(name = "port", nullable = false)
    private int port;

    @Enumerated(EnumType.STRING)
    @Column(name = "security", nullable = false, length = 16)
    private MailSecurity security;

    @Column(name = "username", length = 320)
    private String username;

    /**
     * Le mot de passe <b>chiffré</b>. Jamais de clair, ici ni ailleurs.
     *
     * <p>Son accesseur est délibérément nommé {@code getPasswordCipher} pour
     * qu'un appelant distrait ne le prenne pas pour un mot de passe utilisable :
     * ce qui en sort doit passer par le coffre avant de servir.
     */
    @Column(name = "password_cipher")
    private String passwordCipher;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    protected MailAccount() {
        // JPA
    }

    public MailAccount(String fromAddress, String fromName, String replyTo, String host, int port,
                       MailSecurity security, String username, String passwordCipher,
                       boolean enabled) {
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.replyTo = replyTo;
        this.host = host;
        this.port = port;
        this.security = security;
        this.username = username;
        this.passwordCipher = passwordCipher;
        this.enabled = enabled;
    }

    /**
     * Dit si un changement rend caduque la preuve d'envoi réussi.
     *
     * <p>Un essai concluant ne prouve rien sur un serveur, un compte ou un mot
     * de passe qu'on vient de changer. Garder la mention « vérifiée » après une
     * telle modification serait pire que de ne rien afficher : l'écran
     * affirmerait une chose qu'il n'a pas vérifiée, et personne ne relancerait
     * l'essai.
     *
     * <p>Le nom de l'expéditeur et l'adresse de réponse n'en font <b>pas</b>
     * partie : ils ne changent rien à ce que le serveur accepte.
     */
    public boolean changeCeQuiAEteVerifie(MailAccount autre) {
        return !Objects.equals(this.host, autre.host)
                || this.port != autre.port
                || this.security != autre.security
                || !Objects.equals(this.username, autre.username)
                || !Objects.equals(this.fromAddress, autre.fromAddress)
                || !Objects.equals(this.passwordCipher, autre.passwordCipher);
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public MailSecurity getSecurity() {
        return security;
    }

    public String getUsername() {
        return username;
    }

    /** Le <b>cryptogramme</b>, pas un mot de passe : il doit passer par le coffre. */
    public String getPasswordCipher() {
        return passwordCipher;
    }

    public void setPasswordCipher(String passwordCipher) {
        this.passwordCipher = passwordCipher;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
