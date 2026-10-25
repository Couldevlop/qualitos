package com.openlab.qualitos.quality.supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** La fiche se refait à chaque demande, à partir de la NC et de ses pièces du moment. */
@ExtendWith(MockitoExtension.class)
class SupplierNcReportServiceTest {

    @Mock SupplierService suppliers;
    @Mock SupplierDocumentService documents;
    @Mock SupplierNcReportRenderer renderer;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID SUP = UUID.randomUUID();
    static final UUID NC = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-10-24T08:00:00Z");

    @Test
    void assembleLaFicheAvecLesPiecesJointes() {
        Supplier s = new Supplier();
        s.setId(SUP); s.setTenantId(TENANT); s.setCode("ACME"); s.setName("Acme");
        SupplierNonConformity nc = new SupplierNonConformity();
        nc.setId(NC); nc.setTenantId(TENANT); nc.setSupplierId(SUP);
        nc.setPartNumber("PN-1"); nc.setPartQuantity(5); nc.setEightDRequested(true);
        nc.setSeverity(NonConformitySeverity.MAJOR); nc.setStatus(NonConformityStatus.OPEN);
        nc.setDetectedOn(LocalDate.parse("2026-10-01")); nc.setDescription("Rayures");
        SupplierDocument d = new SupplierDocument();
        d.setCategory(SupplierDocumentCategory.EIGHT_D); d.setOriginalFilename("8D.pdf");
        d.setSizeBytes(10); d.setCreatedAt(NOW);
        when(suppliers.loadForTenant(SUP)).thenReturn(s);
        when(suppliers.loadNonConformity(s, NC)).thenReturn(nc);
        when(documents.forReport(TENANT, NC)).thenReturn(List.of(d));
        when(renderer.render(any())).thenReturn(new byte[]{1});

        SupplierNcReportService service = new SupplierNcReportService(suppliers, documents, renderer,
                Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(service.render(SUP, NC)).containsExactly(1);

        ArgumentCaptor<SupplierNcReport> report = ArgumentCaptor.forClass(SupplierNcReport.class);
        verify(renderer).render(report.capture());
        assertThat(report.getValue().supplierCode()).isEqualTo("ACME");
        assertThat(report.getValue().partNumber()).isEqualTo("PN-1");
        assertThat(report.getValue().partQuantity()).isEqualTo(5);
        assertThat(report.getValue().eightDRequested()).isTrue();
        assertThat(report.getValue().editedAt()).isEqualTo(NOW);
        assertThat(report.getValue().attachments()).singleElement()
                .satisfies(a -> assertThat(a.filename()).isEqualTo("8D.pdf"));
    }
}
