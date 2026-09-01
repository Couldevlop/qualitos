package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.billing.BillingPeriod;
import com.openlab.qualitos.core.billing.BillingProfileDto;
import com.openlab.qualitos.core.billing.BillingProfileService;
import com.openlab.qualitos.core.billing.Money;
import com.openlab.qualitos.core.billing.SubscriptionDto;
import com.openlab.qualitos.core.billing.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Émettre, lire et envoyer les factures.
 *
 * <p>Une facture n'est jamais saisie : elle est DÉDUITE des abonnements
 * vivants d'un client pour une période. C'est ce qui garantit qu'aucune ligne
 * ne réclame un montant qu'aucun contrat ne justifie.
 *
 * <p>Cinq règles portent l'essentiel :
 *
 * <ol>
 *   <li><b>un client exempté ne reçoit pas de facture</b> — pas même une
 *       facture à zéro euro, qui lui ferait croire à un contrat. C'est le cas
 *       du compte de démonstration ;</li>
 *   <li><b>la facture reprend le prix FIGÉ de l'abonnement</b>, jamais le tarif
 *       courant du catalogue : le client a signé à 99 €, il est facturé 99 €,
 *       même si le tarif est passé à 129 € ;</li>
 *   <li><b>un abonnement annuel n'est pas facturé chaque mois</b> — sinon un
 *       contrat annuel serait réclamé douze fois ;</li>
 *   <li><b>deux émissions pour la même période ne font pas deux factures</b> :
 *       relancer le traitement mensuel, après une panne ou par prudence, ne
 *       double pas la facturation ;</li>
 *   <li><b>une facture déjà envoyée ne se renvoie pas seule</b> : deux
 *       exemplaires de la même facture, c'est un litige.</li>
 * </ol>
 *
 * <p><b>Ce que l'émission ne fait PAS, et qui est un choix.</b> Un abonnement
 * résilié en cours de période n'est pas facturé au prorata : il n'apparaît plus
 * dans les abonnements vivants, donc sa dernière période n'est pas réclamée. Le
 * client bénéficie du doute. L'inverse — facturer un mois entamé puis résilié —
 * demanderait de connaître la date de résiliation ET la politique commerciale
 * qui s'y applique, laquelle n'est pas arrêtée. Tant qu'elle ne l'est pas, on
 * préfère ne pas réclamer que réclamer à tort.
 */
@Service
@Transactional(readOnly = true)
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoices;
    private final SubscriptionService subscriptions;
    private final BillingProfileService profiles;
    private final InvoiceRenderPort renderer;
    private final InvoiceMailPort mailer;
    private final Clock clock;

    public InvoiceService(InvoiceRepository invoices,
                          SubscriptionService subscriptions,
                          BillingProfileService profiles,
                          InvoiceRenderPort renderer,
                          InvoiceMailPort mailer,
                          Clock clock) {
        this.invoices = invoices;
        this.subscriptions = subscriptions;
        this.profiles = profiles;
        this.renderer = renderer;
        this.mailer = mailer;
        this.clock = clock;
    }

    /**
     * Émet la facture d'un client pour une période, ou n'en émet aucune.
     *
     * <p>{@link Optional} et non {@code null} : « pas de facture » est un
     * résultat ordinaire et fréquent — client exempté, aucun abonnement dû ce
     * mois-là. Un {@code null} rendu par une méthode dont la signature promet
     * une facture finit en {@code NullPointerException} chez l'appelant, à
     * l'exécution, un premier du mois.
     *
     * @param actor l'éditeur qui émet, lu du jeton (§18.2 règle 5)
     */
    @Transactional
    public Optional<InvoiceDto.View> issueFor(UUID tenantId, YearMonth period, UUID actor) {
        // Règle 4, en premier : si la facture existe, on la rend telle quelle.
        // Avant même de regarder l'exemption — un client exempté APRÈS l'émission
        // ne fait pas disparaître la facture qu'il a déjà reçue.
        Optional<Invoice> existing = invoices.findByTenantAndPeriod(
                tenantId, period.getYear(), period.getMonthValue());
        if (existing.isPresent()) {
            return existing.map(InvoiceDto.View::from);
        }
        // Règle 1 : le compte de démonstration. Émettre une facture à zéro euro
        // lui ferait croire à un contrat.
        if (profiles.isExempt(tenantId)) {
            return Optional.empty();
        }

        List<SubscriptionDto.View> due = subscriptions.activeFor(tenantId).stream()
                .filter(subscription -> isDue(subscription, period))
                .toList();
        if (due.isEmpty()) {
            // Aucun abonnement dû : une facture vide n'a rien à dire, et son
            // numéro consommerait un rang de la séquence pour rien.
            return Optional.empty();
        }

        Instant now = Instant.now(clock);
        int fiscalYear = period.getYear();
        String number = invoices.findLastNumberOfFiscalYear(fiscalYear)
                .map(InvoiceNumber::next)
                .orElseGet(() -> InvoiceNumber.first(fiscalYear));

        UUID invoiceId = UUID.randomUUID();
        List<InvoiceLine> lines = new ArrayList<>();
        // Le total part de ZÉRO dans la devise du premier abonnement dû, et non
        // d'un `null` accumulé : un total nul est un total légitime (palier
        // FREE), et une variable qui commence à null oblige chaque lecteur —
        // et le compilateur — à se demander si elle peut le rester.
        Money total = Money.of(0, due.get(0).currency());
        int lineNo = 0;
        for (SubscriptionDto.View subscription : due) {
            lineNo++;
            // Règle 2 : le montant vient de l'ABONNEMENT, jamais du catalogue.
            // C'est pourquoi ce service ne connaît même pas ModulePriceService :
            // ne pas avoir la dépendance est plus sûr que se rappeler de ne pas
            // s'en servir.
            Money unit = subscription.amount();
            lines.add(InvoiceLine.builder()
                    .id(UUID.randomUUID())
                    .subscriptionId(subscription.id())
                    .lineNo(lineNo)
                    .moduleCode(subscription.moduleCode())
                    .billingTier(subscription.billingTier())
                    .period(subscription.period())
                    .quantity(1)
                    .unitAmountCents(unit.cents())
                    .lineTotalCents(unit.cents())
                    .build());
            // Money.plus refuse d'additionner deux devises : un client dont deux
            // abonnements ne partagent pas la devise fait échouer l'émission
            // plutôt que produire un total qui ne veut rien dire.
            total = total.plus(unit);
        }

        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .tenantId(tenantId)
                .number(number)
                .fiscalYear(fiscalYear)
                .periodYear(period.getYear())
                .periodMonth(period.getMonthValue())
                .currency(total.currency())
                .totalCents(total.cents())
                .issuedAt(now)
                .issuedBy(actor)
                .lines(lines)
                .build();

        Invoice saved = invoices.save(invoice);
        log.info("billing.invoice.issued tenant_id={} number={} period={} lines={}",
                tenantId, saved.getNumber(), period, lines.size());
        return Optional.of(InvoiceDto.View.from(saved));
    }

    /**
     * Un abonnement est-il dû pour cette période ?
     *
     * <p>Mensuel : chaque mois à partir de celui du début. Annuel :
     * uniquement le mois ANNIVERSAIRE. Sans cette distinction, un contrat
     * annuel serait réclamé douze fois — la faute la plus coûteuse que ce
     * fichier puisse contenir, et celle qu'aucune contrainte de base ne peut
     * rattraper.
     *
     * <p>La période antérieure au début n'est jamais due : réémettre les
     * factures d'un exercice ancien ne doit pas facturer un contrat signé
     * depuis.
     */
    static boolean isDue(SubscriptionDto.View subscription, YearMonth period) {
        YearMonth start = YearMonth.from(subscription.startedOn());
        if (period.isBefore(start)) {
            return false;
        }
        return subscription.period() != BillingPeriod.ANNUAL
                || subscription.startedOn().getMonthValue() == period.getMonthValue();
    }

    /** Les factures d'un client, la plus récente en tête. */
    public List<InvoiceDto.View> findByTenant(UUID tenantId) {
        return invoices.findByTenantOrderByNumberDesc(tenantId).stream()
                .map(InvoiceDto.View::from)
                .toList();
    }

    public InvoiceDto.View get(UUID invoiceId) {
        return InvoiceDto.View.from(load(invoiceId));
    }

    /**
     * Le PDF de la facture, rendu à la demande et non stocké.
     *
     * <p>Rendu plutôt que conservé : la facture, elle, est en base — le PDF
     * n'en est qu'une mise en page, reproductible à l'identique tant que la
     * pièce ne change pas (et elle ne change pas, tout y est
     * {@code updatable = false}). Stocker le PDF ajouterait un second endroit
     * où la vérité peut diverger, pour la seule économie d'un rendu.
     */
    public byte[] renderPdf(UUID invoiceId) {
        Invoice invoice = load(invoiceId);
        return renderer.render(invoice, requireProfile(invoice.getTenantId()));
    }

    /**
     * Envoie la facture au destinataire de FACTURATION du client.
     *
     * <p>Et non à l'administrateur du tenant : la comptabilité n'est pas
     * l'informatique, et une facture qui arrive dans la boîte de l'admin
     * système attend souvent qu'on la relance pour être payée.
     */
    @Transactional
    public InvoiceDto.View send(UUID invoiceId, UUID actor) {
        Invoice invoice = load(invoiceId);
        // Règle 5 : le refus AVANT tout envoi. Vérifier après aurait laissé
        // partir le second exemplaire.
        if (invoice.isSent()) {
            throw new IllegalStateException(
                    "Facture " + invoice.getNumber() + " deja envoyee le " + invoice.getSentAt());
        }
        BillingProfileDto.View profile = requireProfile(invoice.getTenantId());
        byte[] pdf = renderer.render(invoice, profile);

        mailer.send(profile.billingEmail(), subjectOf(invoice), bodyOf(invoice, profile), pdf);

        invoice.markSent(profile.billingEmail(), Instant.now(clock));
        Invoice saved = invoices.save(invoice);
        // L'acteur figure au journal applicatif : c'est lui qui a decide de
        // l'envoi, et une facture partie deux fois se remonte a quelqu'un.
        log.info("billing.invoice.sent tenant_id={} number={} actor={}",
                invoice.getTenantId(), invoice.getNumber(), actor);
        return InvoiceDto.View.from(saved);
    }

    private Invoice load(UUID invoiceId) {
        return invoices.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }

    /**
     * Le profil de facturation, sans lequel on ne peut ni rendre ni envoyer.
     *
     * <p>« On ne facture pas un UUID » : sans profil, la facture n'aurait ni
     * raison sociale, ni adresse, ni destinataire. C'est un refus explicite —
     * le profil est à remplir — et non un rendu dégradé qui produirait une
     * pièce inutilisable en comptabilité.
     */
    private BillingProfileDto.View requireProfile(UUID tenantId) {
        return profiles.find(tenantId)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun profil de facturation pour le client " + tenantId
                                + " : une facture ne peut pas etre editee sans raison sociale"));
    }

    private static String subjectOf(Invoice invoice) {
        return "Facture " + invoice.getNumber() + " - " + invoice.period();
    }

    private static String bodyOf(Invoice invoice, BillingProfileDto.View profile) {
        // Texte brut, meme discipline que les rappels d'audit du moteur : le
        // corps porte la raison sociale, saisie par un humain. En HTML il
        // faudrait l'echapper, et un oubli d'echappement dans un courriel se
        // voit rarement avant qu'il ne serve (OWASP A03). Le texte brut retire
        // le probleme au lieu de le gerer.
        return """
                Bonjour,

                Veuillez trouver ci-joint la facture %s pour la periode %s.

                Montant total : %s
                Etabli au nom de : %s

                Cordialement,
                Le service facturation QualitOS
                """.formatted(
                        invoice.getNumber(),
                        invoice.period(),
                        InvoiceAmounts.format(invoice.total()),
                        profile.legalName());
    }
}
