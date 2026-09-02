package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Contrôleur d'administration des factures.
 *
 * <p>Réservé au SEUL {@code SUPER_ADMIN}, comme le reste de la facturation :
 * émettre une facture est un acte d'éditeur, et un client qui pourrait émettre
 * les siennes pourrait s'en dispenser.
 *
 * <p><b>Deux racines dans un même contrôleur</b>, d'où le
 * {@code @RequestMapping} à {@code /api/v1/admin} et des chemins complets sur
 * chaque méthode. Ce qui concerne un CLIENT passe par
 * {@code /clients/{tenantId}/invoices} — le client vient du chemin, §18.2
 * règle 2, comme pour le profil et les abonnements. Ce qui concerne une facture
 * DÉJÀ ÉMISE passe par {@code /invoices/{invoiceId}} : elle porte son client en
 * elle, et le redemander dans l'URL inviterait à les faire diverger.
 *
 * <p>L'acteur vient du jeton ({@link CurrentUser#requireUserId()}), jamais du
 * corps : émettre et envoyer sont des actes commerciaux, ils doivent rester
 * attribuables (§18.2 règle 5).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Invoices", description = "Invoice issuance, rendering and sending — Super Admin only")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * Émet la facture d'un client pour une période, ou répond 204.
     *
     * <p>204 « No Content » quand il n'y a rien à facturer — client exempté, ou
     * aucun abonnement dû ce mois-là. Ce n'est pas une erreur : c'est le cas
     * ordinaire du compte de démonstration, et celui d'un client dont les
     * contrats sont tous annuels un mois qui n'est pas leur anniversaire. Un
     * 404 laisserait croire à une adresse fausse, un 200 avec un corps vide
     * obligerait l'appelant à deviner.
     *
     * <p>Réémettre la même période rend la facture DÉJÀ émise, avec son numéro
     * d'origine : le traitement mensuel peut être relancé sans doubler la
     * facturation.
     */
    @PostMapping("/clients/{tenantId}/invoices")
    @Operation(summary = "Issue the invoice of a client for a given period (idempotent)")
    public ResponseEntity<InvoiceDto.View> issue(
            @PathVariable UUID tenantId,
            @RequestParam("period") @DateTimeFormat(pattern = "yyyy-MM") YearMonth period) {
        UUID actor = CurrentUser.requireUserId();
        return invoiceService.issueFor(tenantId, period, actor)
                .map(invoice -> ResponseEntity.status(HttpStatus.CREATED).body(invoice))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/clients/{tenantId}/invoices")
    @Operation(summary = "List the invoices of a client, most recent first")
    public ResponseEntity<List<InvoiceDto.View>> listForClient(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(invoiceService.findByTenant(tenantId));
    }

    @GetMapping("/invoices/{invoiceId}")
    @Operation(summary = "Get one invoice, lines included")
    public ResponseEntity<InvoiceDto.View> get(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(invoiceService.get(invoiceId));
    }

    /**
     * Le PDF de la facture.
     *
     * <p>{@code inline} et non {@code attachment} : l'éditeur veut RELIRE la
     * pièce avant de l'envoyer, et une pièce jointe forcerait un aller-retour
     * par le disque à chaque vérification. Le nom de fichier reste posé, pour
     * l'enregistrement volontaire.
     */
    @GetMapping(value = "/invoices/{invoiceId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Render one invoice as a PDF")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID invoiceId) {
        InvoiceDto.View invoice = invoiceService.get(invoiceId);
        byte[] pdf = invoiceService.renderPdf(invoiceId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(invoice.number() + ".pdf")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/invoices/{invoiceId}/send")
    @Operation(summary = "Send the invoice to the client billing contact")
    public ResponseEntity<InvoiceDto.View> send(@PathVariable UUID invoiceId) {
        UUID actor = CurrentUser.requireUserId();
        return ResponseEntity.ok(invoiceService.send(invoiceId, actor));
    }
}
