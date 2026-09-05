package com.openlab.qualitos.quality.product.export;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Extraction Excel du dossier d'un produit : PFMEA et plan de surveillance.
 *
 * <p>Séparé de {@code ProductController} parce qu'il ne rend pas du JSON : il
 * rend un fichier, avec ses en-têtes de téléchargement et son type MIME. Les
 * mélanger obligerait à répéter cette plomberie au milieu d'un contrôleur de
 * ressources.
 *
 * <p><b>Lecture simple, et non rôle d'édition.</b> L'export ne montre rien que
 * l'utilisateur ne puisse déjà lire à l'écran : le réserver aux profils
 * habilités à MODIFIER reviendrait à interdire à un auditeur — dont le métier
 * est justement d'emporter les preuves — de sortir ce qu'il a sous les yeux.
 * Le filtrage par client, lui, tient : les services appelés ne rendent que ce
 * qui appartient au tenant du jeton.
 *
 * <p>Pas de {@code RequiresModule("controlplan")} sur la classe : le PFMEA doit
 * pouvoir sortir même chez un client qui n'a pas souscrit le plan de
 * surveillance. C'est le service qui traite l'absence, feuille vide à l'appui.
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/export")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Product export", description = "Excel extraction of a product PFMEA and control plan")
public class ProductExportController {

    private final ProductExportService service;

    public ProductExportController(ProductExportService service) {
        this.service = service;
    }

    @GetMapping(value = "/xlsx",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Download the product PFMEA and control plan as one Excel workbook")
    public ResponseEntity<byte[]> xlsx(@PathVariable UUID productId) {
        ProductExportService.Export export = service.export(productId);
        // `attachment` et non `inline` : un classeur ne se lit pas dans un
        // navigateur, il s'ouvre dans un tableur. `inline` déclencherait selon
        // les postes un téléchargement muet ou une page d'octets.
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(export.filename())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(export.content());
    }
}
