package com.openlab.qualitos.core.mail;

import com.openlab.qualitos.core.partner.PartnerExceptions;
import com.openlab.qualitos.core.partner.PartnerFirmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * La messagerie d'un cabinet partenaire.
 *
 * <p>Mêmes règles que celle d'un client — l'assemblage est partagé — mais deux
 * différences qui comptent.
 *
 * <p><b>Le cabinet est nommé, pas déduit.</b> Il n'y a pas de « cabinet
 * courant » dans un jeton : la console de plateforme en gère plusieurs. L'appel
 * porte donc l'identifiant, et le contrôleur garde la route par le rôle.
 *
 * <p><b>On vérifie que le cabinet existe.</b> Sans cela, une ligne de
 * messagerie survivrait à un cabinet supprimé — ou pire, serait créée pour un
 * identifiant inventé, et personne ne la verrait jamais dans un écran.
 */
@Service
@Transactional(readOnly = true)
public class PartnerFirmMailSettingsService {

    private static final Logger log = LoggerFactory.getLogger(PartnerFirmMailSettingsService.class);

    private final PartnerFirmMailSettingsRepository repository;
    private final PartnerFirmRepository firms;
    private final MailAccountAssembler assembler;
    private final MailProbePort probe;

    public PartnerFirmMailSettingsService(PartnerFirmMailSettingsRepository repository,
                                          PartnerFirmRepository firms,
                                          MailAccountAssembler assembler,
                                          MailProbePort probe) {
        this.repository = repository;
        this.firms = firms;
        this.assembler = assembler;
        this.probe = probe;
    }

    public MailSettingsDto.Response lire(UUID firmId) {
        exigeLeCabinet(firmId);
        return repository.findById(firmId)
                .map(s -> MailSettingsDto.Response.from(s.getAccount()))
                .orElseGet(MailSettingsDto.Response::absente);
    }

    @Transactional
    public MailSettingsDto.Response enregistrer(UUID firmId, MailSettingsDto.Request request) {
        exigeLeCabinet(firmId);
        Optional<PartnerFirmMailSettings> existant = repository.findById(firmId);
        MailAccount avant = existant.map(PartnerFirmMailSettings::getAccount).orElse(null);
        MailAccount apres = assembler.assembler(request, avant);

        PartnerFirmMailSettings entite =
                existant.orElseGet(() -> new PartnerFirmMailSettings(firmId, apres));
        entite.setAccount(apres);
        PartnerFirmMailSettings enregistre = repository.save(entite);

        // Le journal nomme l'hote et l'expediteur, JAMAIS le mot de passe ni
        // son cryptogramme.
        log.info("firm.mail.saved firm_id={} host={} from={} enabled={}",
                firmId, apres.getHost(), apres.getFromAddress(), apres.isEnabled());
        return MailSettingsDto.Response.from(enregistre.getAccount());
    }

    @Transactional
    public MailSettingsDto.TestResult eprouver(UUID firmId, String destinataire) {
        exigeLeCabinet(firmId);
        PartnerFirmMailSettings reglage = repository.findById(firmId)
                .orElseThrow(MailSettingsNotConfiguredException::new);

        String motif = probe.essayer(reglage.getAccount(),
                assembler.motDePasseClair(reglage.getAccount()), destinataire);

        if (motif != null) {
            // L'echec n'efface pas une verification anterieure : un serveur
            // momentanement injoignable ne rend pas faux un essai qui a abouti.
            log.info("firm.mail.test-failed firm_id={} host={}",
                    firmId, reglage.getAccount().getHost());
            return new MailSettingsDto.TestResult(false, motif,
                    reglage.getAccount().getVerifiedAt());
        }

        Instant quand = Instant.now();
        reglage.getAccount().setVerifiedAt(quand);
        repository.save(reglage);
        log.info("firm.mail.test-ok firm_id={} host={}", firmId, reglage.getAccount().getHost());
        return new MailSettingsDto.TestResult(true, null, quand);
    }

    @Transactional
    public void retirer(UUID firmId) {
        exigeLeCabinet(firmId);
        if (repository.existsById(firmId)) {
            repository.deleteById(firmId);
            log.info("firm.mail.removed firm_id={}", firmId);
        }
    }

    /** Le réglage utilisable à l'envoi, vide quand il n'est pas activé. */
    public Optional<MailAccount> pourEnvoi(UUID firmId) {
        return repository.findById(firmId)
                .map(PartnerFirmMailSettings::getAccount)
                .filter(MailAccount::isEnabled);
    }

    private void exigeLeCabinet(UUID firmId) {
        if (!firms.existsById(firmId)) {
            throw new PartnerExceptions.FirmNotFound(firmId);
        }
    }
}
