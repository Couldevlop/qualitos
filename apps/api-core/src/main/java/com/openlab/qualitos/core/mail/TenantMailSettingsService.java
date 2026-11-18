package com.openlab.qualitos.core.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * La messagerie d'un client : la régler, l'éprouver, la retirer.
 *
 * <p>Toutes les lectures et écritures portent sur le client du jeton. Le
 * service ne prend jamais un identifiant de client venu du corps de la requête
 * ({@code CLAUDE.md} §18.2.2) : sans cela, un administrateur pourrait régler la
 * messagerie d'un autre et détourner son courrier.
 */
@Service
@Transactional(readOnly = true)
public class TenantMailSettingsService {

    private static final Logger log = LoggerFactory.getLogger(TenantMailSettingsService.class);

    private final TenantMailSettingsRepository repository;
    private final MailAccountAssembler assembler;
    private final MailProbePort probe;

    public TenantMailSettingsService(TenantMailSettingsRepository repository,
                                     MailAccountAssembler assembler,
                                     MailProbePort probe) {
        this.repository = repository;
        this.assembler = assembler;
        this.probe = probe;
    }

    public MailSettingsDto.Response lire(UUID tenantId) {
        return repository.findById(tenantId)
                .map(s -> MailSettingsDto.Response.from(s.getAccount()))
                .orElseGet(MailSettingsDto.Response::absente);
    }

    /**
     * Enregistre le réglage.
     *
     * <p>Deux règles portent tout le reste.
     *
     * <p><b>Un mot de passe absent n'efface rien.</b> L'API ne renvoie jamais le
     * mot de passe ; le formulaire le réaffiche donc vide, et prendre ce vide
     * pour un effacement couperait la messagerie du client à chaque correction
     * de faute de frappe sur le nom de l'expéditeur. L'effacer se demande
     * explicitement.
     *
     * <p><b>Changer ce qui a été éprouvé efface la preuve.</b> Un essai
     * concluant ne dit plus rien sur un serveur ou un compte qu'on vient de
     * modifier. Garder la mention « vérifiée » serait pire que de ne rien
     * afficher : l'écran affirmerait une chose qu'il n'a pas vérifiée, et
     * personne ne relancerait l'essai.
     */
    @Transactional
    public MailSettingsDto.Response enregistrer(UUID tenantId, MailSettingsDto.Request request) {
        Optional<TenantMailSettings> existant = repository.findById(tenantId);
        MailAccount avant = existant.map(TenantMailSettings::getAccount).orElse(null);
        MailAccount apres = assembler.assembler(request, avant);

        TenantMailSettings entite = existant.orElseGet(() -> new TenantMailSettings(tenantId, apres));
        entite.setAccount(apres);
        TenantMailSettings enregistre = repository.save(entite);

        // Le journal nomme l'hote et l'expediteur, JAMAIS le mot de passe ni
        // son cryptogramme (§22.9 : jamais de donnee sensible en clair).
        log.info("tenant.mail.saved tenant_id={} host={} from={} enabled={}",
                tenantId, apres.getHost(), apres.getFromAddress(), apres.isEnabled());
        return MailSettingsDto.Response.from(enregistre.getAccount());
    }

    /**
     * Envoie un courriel d'essai, et ne marque « vérifiée » que si c'est parti.
     *
     * <p>C'est le seul chemin qui pose {@code verifiedAt}. L'enregistrement ne
     * le pose pas : accepter un réglage n'est pas l'avoir éprouvé, et un écran
     * qui annonce « vérifiée » après une simple sauvegarde ment sur ce qu'il
     * sait.
     */
    @Transactional
    public MailSettingsDto.TestResult eprouver(UUID tenantId, String destinataire) {
        TenantMailSettings reglage = repository.findById(tenantId)
                .orElseThrow(MailSettingsNotConfiguredException::new);

        String motif = probe.essayer(reglage.getAccount(),
                motDePasseClair(reglage.getAccount()), destinataire);

        if (motif != null) {
            // L'echec N'EFFACE PAS une verification anterieure : un serveur
            // momentanement injoignable ne rend pas faux un essai qui a abouti
            // hier, et effacer la preuve ferait clignoter l'ecran au rythme des
            // incidents reseau.
            log.info("tenant.mail.test-failed tenant_id={} host={}",
                    tenantId, reglage.getAccount().getHost());
            return new MailSettingsDto.TestResult(false, motif,
                    reglage.getAccount().getVerifiedAt());
        }

        Instant quand = Instant.now();
        reglage.getAccount().setVerifiedAt(quand);
        repository.save(reglage);
        log.info("tenant.mail.test-ok tenant_id={} host={}", tenantId, reglage.getAccount().getHost());
        return new MailSettingsDto.TestResult(true, null, quand);
    }

    /**
     * Retire la messagerie du client : son courrier repart de celle de la
     * plateforme.
     *
     * <p>On supprime la ligne plutôt que de la désactiver. Garder un serveur et
     * un mot de passe chiffré dont plus personne ne se sert, c'est conserver un
     * secret sans raison — et un secret conservé sans raison finit par fuir.
     */
    @Transactional
    public void retirer(UUID tenantId) {
        if (repository.existsById(tenantId)) {
            repository.deleteById(tenantId);
            log.info("tenant.mail.removed tenant_id={}", tenantId);
        }
    }

    /**
     * Le réglage utilisable à l'envoi, mot de passe déchiffré.
     *
     * <p>Rendu vide quand la messagerie n'est pas <b>activée</b> : un réglage
     * enregistré mais pas activé ne doit pas détourner le courrier. Vide
     * signifie « prends celle de la plateforme ».
     */
    public Optional<MailAccount> pourEnvoi(UUID tenantId) {
        return repository.findById(tenantId)
                .map(TenantMailSettings::getAccount)
                .filter(MailAccount::isEnabled);
    }

    /** Déchiffre le mot de passe, le temps d'un envoi. {@code null} s'il n'y en a pas. */
    public String motDePasseClair(MailAccount reglage) {
        return assembler.motDePasseClair(reglage);
    }
}
