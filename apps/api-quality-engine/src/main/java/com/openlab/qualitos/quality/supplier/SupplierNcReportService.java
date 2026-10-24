package com.openlab.qualitos.quality.supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * La fiche PDF d'une NC fournisseur : ce qu'on envoie au fournisseur avec la
 * demande de 8D, et ce qu'on montre à l'auditeur.
 *
 * <p>Elle se lit à l'instant t : rien n'est stocké, chaque demande la refait à
 * partir de la NC et de ses pièces jointes du moment.
 */
@Service
public class SupplierNcReportService {

    private final SupplierService suppliers;
    private final SupplierDocumentService documents;
    private final SupplierNcReportRenderer renderer;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public SupplierNcReportService(SupplierService suppliers, SupplierDocumentService documents,
                                   SupplierNcReportRenderer renderer) {
        this(suppliers, documents, renderer, Clock.systemUTC());
    }

    SupplierNcReportService(SupplierService suppliers, SupplierDocumentService documents,
                            SupplierNcReportRenderer renderer, Clock clock) {
        this.suppliers = suppliers;
        this.documents = documents;
        this.renderer = renderer;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public byte[] render(UUID supplierId, UUID ncId) {
        Supplier s = suppliers.loadForTenant(supplierId);
        SupplierNonConformity nc = suppliers.loadNonConformity(s, ncId);
        List<SupplierNcReport.Attachment> attachments = documents.forReport(s.getTenantId(), nc.getId()).stream()
                .map(d -> new SupplierNcReport.Attachment(d.getCategory(), d.getOriginalFilename(),
                        d.getSizeBytes(), d.getCreatedAt()))
                .toList();
        return renderer.render(new SupplierNcReport(
                s.getCode(), s.getName(),
                nc.getPartNumber(), nc.getPartQuantity(), nc.getLotReference(),
                nc.getDetectedOn(), nc.getResolvedOn(),
                nc.getSeverity(), nc.getStatus(),
                nc.isEightDRequested(), nc.isEightDReceived(),
                nc.getDescription(), nc.getResolution(),
                attachments,
                Instant.now(clock)));
    }
}
