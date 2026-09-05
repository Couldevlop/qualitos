package com.openlab.qualitos.quality.product.export;

import com.openlab.qualitos.quality.controlplan.application.ControlPlanDto;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanService;
import com.openlab.qualitos.quality.product.application.ProductDto;
import com.openlab.qualitos.quality.product.application.ProductService;
import com.openlab.qualitos.quality.risk.FmeaDto;
import com.openlab.qualitos.quality.risk.FmeaService;
import com.openlab.qualitos.quality.risk.FmeaStatus;
import com.openlab.qualitos.quality.risk.FmeaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Rassemble ce qu'un produit a d'exportable et le rend en classeur Excel.
 *
 * <p><b>Un seul fichier pour les deux tableaux.</b> Le PFMEA dit ce qui peut
 * mal tourner, le plan de surveillance dit ce qu'on contrôle pour l'empêcher :
 * les lire séparément, c'est perdre le lien entre les deux. Un audit client qui
 * réclame « le dossier du produit » attend les deux dans la même main.
 *
 * <p><b>Le PFMEA retenu est celui que l'écran montre</b> — l'ACTIF s'il existe,
 * sinon le plus récent. Exporter les brouillons abandonnés à côté du bon
 * donnerait un classeur que personne ne saurait lire.
 *
 * <p><b>Un plan de surveillance absent n'est pas une erreur.</b> La feuille est
 * créée vide, avec ses en-têtes : elle dit « il n'y en a pas », là où un
 * classeur à une seule feuille laisserait croire à un export tronqué. Le module
 * peut aussi être fermé pour ce client — l'export du PFMEA n'a pas à tomber
 * avec lui.
 */
@Service
public class ProductExportService {

    private static final Logger log = LoggerFactory.getLogger(ProductExportService.class);

    /**
     * Plafond de lignes lues par tableau.
     *
     * <p>Une page, et non un {@code findAll} : l'export est déclenché par un
     * clic, et un produit dont l'analyse aurait dérivé à des dizaines de
     * milliers de lignes ne doit pas pouvoir immobiliser le service. Mille
     * lignes dépassent très largement le plus gros PFMEA rencontré ; au-delà,
     * le classeur ne serait de toute façon plus exploitable à la main.
     */
    static final int MAX_ROWS = 1000;

    private final ProductService products;
    private final FmeaService fmea;
    private final ControlPlanService controlPlans;

    public ProductExportService(ProductService products, FmeaService fmea,
                                ControlPlanService controlPlans) {
        this.products = products;
        this.fmea = fmea;
        this.controlPlans = controlPlans;
    }

    /**
     * Le classeur du produit.
     *
     * @throws com.openlab.qualitos.quality.product.domain.ProductNotFoundException
     *         produit inconnu — ou appartenant à un autre client, ce qui revient
     *         au même vu d'ici (le service produit filtre par tenant).
     */
    @Transactional(readOnly = true)
    public Export export(UUID productId) {
        ProductDto.View product = products.get(productId);

        List<FmeaDto.ItemResponse> items = pfmeaItems(productId);
        ControlPlanDto.Detail plan = latestControlPlan(productId);

        byte[] bytes = ProductWorkbook.build(label(product), items, plan);
        log.info("product.export.xlsx product_id={} pfmea_rows={} control_plan_rows={}",
                productId, items.size(), plan == null ? 0 : plan.lines().size());
        return new Export(filename(product), bytes);
    }

    /**
     * Les lignes du PFMEA que l'écran du produit affiche : l'analyse ACTIVE si
     * elle existe, sinon la plus récente.
     */
    private List<FmeaDto.ItemResponse> pfmeaItems(UUID productId) {
        List<FmeaDto.ProjectResponse> projects = fmea
                .listProjects(null, FmeaType.PROCESS_FMEA, productId,
                        PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
        if (projects.isEmpty()) {
            return List.of();
        }
        FmeaDto.ProjectResponse retained = projects.stream()
                .filter(p -> p.status() == FmeaStatus.ACTIVE)
                .findFirst()
                .orElse(projects.get(0));

        return fmea.listItems(retained.id(),
                        PageRequest.of(0, MAX_ROWS, Sort.by(Sort.Direction.ASC, "sequenceNo")))
                .getContent();
    }

    /**
     * Le plan de surveillance le plus avancé du produit.
     *
     * <p>Un produit peut en porter plusieurs — un approuvé, une révision en
     * cours. On retient le plus grand numéro de révision : c'est celui que
     * l'écran ouvre, et celui qu'un auditeur demande.
     *
     * <p>Toute défaillance de ce côté est AVALÉE, à dessein : le module peut
     * être fermé pour ce client (le garde {@code RequiresModule} refuserait
     * alors la lecture), ou le produit n'avoir aucun plan. Dans les deux cas,
     * faire tomber l'export du PFMEA avec lui serait disproportionné — la
     * feuille sort vide, avec ses en-têtes, et dit ce qu'elle est.
     */
    private ControlPlanDto.Detail latestControlPlan(UUID productId) {
        try {
            List<ControlPlanDto.View> plans = controlPlans.listForProduct(productId);
            return plans.stream()
                    .max(Comparator.comparingInt(ControlPlanDto.View::revision))
                    .map(p -> controlPlans.get(productId, p.id()))
                    .orElse(null);
        } catch (RuntimeException e) {
            log.info("product.export.control-plan-skipped product_id={} reason={}",
                    productId, e.getClass().getSimpleName());
            return null;
        }
    }

    private static String label(ProductDto.View product) {
        String designation = product.designation() == null ? "" : product.designation();
        return (product.code() + " — " + designation).trim();
    }

    /**
     * Nom du fichier proposé au téléchargement.
     *
     * <p>Réduit à l'ASCII et aux caractères sûrs : un code produit accentué ou
     * porteur d'un {@code /} produirait un en-tête {@code Content-Disposition}
     * que les navigateurs interprètent chacun à leur façon — et un {@code /}
     * y ouvre une traversée de chemin au moment de l'enregistrement.
     */
    static String filename(ProductDto.View product) {
        String ascii = Normalizer.normalize(product.code() == null ? "" : product.code(),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");
        String base = ascii.isBlank() ? "produit" : ascii;
        return base.toLowerCase(Locale.ROOT) + "-pfmea-plan-surveillance.xlsx";
    }

    /** Le classeur et le nom sous lequel il doit arriver chez l'utilisateur. */
    public record Export(String filename, byte[] content) {}
}
