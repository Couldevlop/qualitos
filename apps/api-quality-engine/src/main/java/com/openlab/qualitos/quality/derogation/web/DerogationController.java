package com.openlab.qualitos.quality.derogation.web;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.derogation.application.DerogationDto;
import com.openlab.qualitos.quality.derogation.application.DerogationService;
import com.openlab.qualitos.quality.derogation.domain.DerogationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * La surface HTTP des dérogations.
 *
 * <p>C'est ici que vivent les transactions et les droits : le service est une
 * classe simple qui ignore l'un et l'autre. Chaque geste porte <b>son</b> droit —
 * la permission dit qui peut signer des dérogations en général, et l'agrégat dit
 * qui peut signer celle-ci. Les deux sont nécessaires.
 */
@RestController
@RequestMapping("/api/v1/derogations")
@Tag(name = "Derogations", description = "Customer concession requests")
public class DerogationController {

    private final DerogationService service;
    private final RecordScope scope;

    public DerogationController(DerogationService service, RecordScope scope) {
        this.service = service;
        this.scope = scope;
    }

    /**
     * Voit-on tout le registre, ou seulement ce qui nous concerne ?
     *
     * <p>Sans {@code derogation.view.all} : ses propres dossiers et ceux qu'on
     * doit signer. Hors de portée répond 404, jamais 403 (ADR 0081).
     */
    private boolean voitTout() {
        return scope.restrictTo(Permission.DEROGATION_VIEW_ALL).isEmpty();
    }

    @GetMapping
    @Transactional(readOnly = true)
    @RequiresPermission(Permission.DEROGATION_VIEW)
    @Operation(summary = "The concession register, filtered by owner, part, customer and status")
    public DerogationDto.PageView<DerogationDto.RowView> list(
            @RequestParam(required = false) UUID owner,
            @RequestParam(required = false) String partReference,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) DerogationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(new DerogationDto.SearchCommand(
                owner, partReference, customerName, status, page, size), voitTout());
    }

    /** « Mes validations » : les dossiers qui attendent ma signature. */
    @GetMapping("/my-approvals")
    @Transactional(readOnly = true)
    @RequiresPermission(Permission.DEROGATION_APPROVE)
    @Operation(summary = "Concessions awaiting my signature")
    public DerogationDto.PageView<DerogationDto.RowView> myApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.myPendingApprovals(page, size);
    }

    /** L'alerte rouge et son nombre, au-dessus de l'onglet. */
    @GetMapping("/my-approvals/count")
    @Transactional(readOnly = true)
    @RequiresPermission(Permission.DEROGATION_APPROVE)
    @Operation(summary = "How many concessions await my signature")
    public DerogationDto.PendingCount myApprovalCount() {
        return service.myPendingApprovalCount();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @RequiresPermission(Permission.DEROGATION_VIEW)
    @Operation(summary = "One concession; 404 when out of reach")
    public DerogationDto.View get(@PathVariable UUID id) {
        return service.get(id, voitTout());
    }

    /**
     * Le document, tel qu'il part au client.
     *
     * <p>{@code inline} : on le regarde avant de l'envoyer. C'est le seul moyen
     * de vérifier ce qu'un tiers va lire.
     */
    @GetMapping("/{id}/pdf")
    @Transactional(readOnly = true)
    @RequiresPermission(Permission.DEROGATION_VIEW)
    @Operation(summary = "The concession as a PDF, in the language frozen on the dossier")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        byte[] document = service.renderPdf(id, voitTout());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("derogation-" + id + ".pdf").build().toString())
                .body(document);
    }

    @PostMapping
    @Transactional
    @RequiresPermission(Permission.DEROGATION_CREATE)
    @Operation(summary = "Opens a concession, as a draft")
    public ResponseEntity<DerogationDto.View> create(
            @Valid @RequestBody DerogationWebDto.SavePayload payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(payload.toCommand()));
    }

    @PutMapping("/{id}")
    @Transactional
    @RequiresPermission(Permission.DEROGATION_EDIT)
    @Operation(summary = "Saves the draft")
    public DerogationDto.View update(@PathVariable UUID id,
                                     @Valid @RequestBody DerogationWebDto.SavePayload payload) {
        return service.update(id, payload.toCommand());
    }

    /** « Validation manager » : le libellé du bouton, et ce qu'il déclenche. */
    @PostMapping("/{id}/request-approval")
    @Transactional
    @RequiresPermission(Permission.DEROGATION_REQUEST_APPROVAL)
    @Operation(summary = "Asks the designated quality manager to sign")
    public DerogationDto.View requestApproval(
            @PathVariable UUID id,
            @Valid @RequestBody DerogationWebDto.RequestApprovalPayload payload) {
        return service.requestApproval(id, payload.toCommand());
    }

    @PostMapping("/{id}/withdraw")
    @Transactional
    @RequiresPermission(Permission.DEROGATION_REQUEST_APPROVAL)
    @Operation(summary = "Withdraws the pending approval request")
    public DerogationDto.View withdraw(@PathVariable UUID id) {
        return service.withdraw(id);
    }

    @PostMapping("/{id}/send-back")
    @Transactional
    @RequiresPermission(Permission.DEROGATION_APPROVE)
    @Operation(summary = "Sends the dossier back for amendment, with a mandatory reason")
    public DerogationDto.View sendBack(@PathVariable UUID id,
                                       @Valid @RequestBody DerogationWebDto.SendBackPayload p) {
        return service.sendBack(id, p.toCommand());
    }

    @PostMapping("/{id}/approve")
    @Transactional
    @RequiresPermission(Permission.DEROGATION_APPROVE)
    @Operation(summary = "The designated manager signs")
    public DerogationDto.View approve(@PathVariable UUID id,
                                      @Valid @RequestBody DerogationWebDto.ApprovePayload p) {
        return service.approve(id, p.toCommand());
    }

    @PostMapping("/{id}/reopen")
    @Transactional
    @RequiresPermission(Permission.DEROGATION_REOPEN)
    @Operation(summary = "Reopens a signed dossier, which clears the signature")
    public DerogationDto.View reopen(@PathVariable UUID id) {
        return service.reopen(id, voitTout());
    }

    /** « Validation client » : le seul geste qui sort de l'organisation. */
    @PostMapping("/{id}/send-to-customer")
    @Transactional
    @RequiresPermission(Permission.DEROGATION_SEND)
    @Operation(summary = "Mails the concession to the customer, PDF attached")
    public DerogationDto.View sendToCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody DerogationWebDto.SendToCustomerPayload payload) {
        return service.sendToCustomer(id, payload.toCommand());
    }

    @PostMapping("/{id}/customer-decision")
    @Transactional
    @RequiresPermission(Permission.DEROGATION_DECIDE)
    @Operation(summary = "Records the customer's answer on the dossier")
    public DerogationDto.View customerDecision(
            @PathVariable UUID id,
            @Valid @RequestBody DerogationWebDto.CustomerDecisionPayload payload) {
        return service.recordCustomerDecision(id, payload.toCommand());
    }
}
