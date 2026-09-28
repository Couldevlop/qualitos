package com.openlab.qualitos.quality.costofquality.application;

import com.openlab.qualitos.quality.costofquality.domain.CoqCategory;
import com.openlab.qualitos.quality.costofquality.domain.CoqEntry;
import com.openlab.qualitos.quality.costofquality.domain.CoqEntryDetails;
import com.openlab.qualitos.quality.costofquality.domain.CoqLabel;
import com.openlab.qualitos.quality.costofquality.domain.CoqNotFoundException;
import com.openlab.qualitos.quality.costofquality.domain.CoqRepositories;
import com.openlab.qualitos.quality.costofquality.domain.CoqValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le coût de la qualité : saisir des imputations, les lire par mois ou par année.
 *
 * <p>Sans Spring ni JPA. Les totaux, le ratio et le rangement des lignes se
 * calculent ici plutôt qu'à l'écran : un rapport de revue de direction ne doit
 * pas dépendre du navigateur qui l'affiche.
 */
public class CostOfQualityService {

    /** Sans réglage, un client compte en euros : c'est la devise de la maquette. */
    static final String DEFAULT_CURRENCY = "EUR";
    static final int MIN_YEAR = 2000;
    static final int MAX_YEAR = 2100;

    private final CoqRepositories.Entries entries;
    private final CoqRepositories.Labels labels;
    private final CoqRepositories.Settings settings;
    private final CoqContext context;
    private final CoqAuditPublisher audit;
    private final Clock clock;

    public CostOfQualityService(CoqRepositories.Entries entries, CoqRepositories.Labels labels,
                                CoqRepositories.Settings settings, CoqContext context,
                                CoqAuditPublisher audit, Clock clock) {
        this.entries = entries;
        this.labels = labels;
        this.settings = settings;
        this.context = context;
        this.audit = audit;
        this.clock = clock;
    }

    // ---------- lecture ----------

    /**
     * Le rapport d'un mois ({@code month} de 1 à 12) ou d'une année ({@code month} nul).
     *
     * <p>Les libellés du catalogue livré apparaissent même sans imputation, à
     * zéro, comme sur la maquette : l'absence de coût de prévention est une
     * information, pas une ligne à cacher. Les libellés saisis n'apparaissent
     * que s'ils ont servi dans la période — sinon la liste ne ferait que
     * s'allonger.
     */
    public CoqDto.ReportView report(int year, Integer month) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new CoqValidationException("year", "Année hors de la plage admise.");
        }
        if (month != null && (month < 1 || month > 12)) {
            throw new CoqValidationException("month", "Le mois va de 1 à 12.");
        }
        UUID tenant = context.requireTenantId();
        LocalDate from = month == null ? LocalDate.of(year, 1, 1) : YearMonth.of(year, month).atDay(1);
        LocalDate to = month == null ? LocalDate.of(year, 12, 31) : YearMonth.of(year, month).atEndOfMonth();

        List<CoqLabel> catalogue = sorted(labels.findVisible(tenant));
        Map<UUID, CoqLabel> parId = catalogue.stream()
                .collect(Collectors.toMap(CoqLabel::getId, Function.identity()));
        List<CoqEntry> periode = entries.findByTenantBetween(tenant, from, to);

        List<CoqDto.BlockView> blocs = new ArrayList<>();
        BigDecimal conformite = BigDecimal.ZERO;
        BigDecimal nonConformite = BigDecimal.ZERO;
        for (CoqCategory famille : CoqCategory.values()) {
            List<CoqEntry> lignes = periode.stream().filter(e -> e.getCategory() == famille).toList();
            List<CoqDto.LineView> vues = month == null
                    ? cumulParLibelle(famille, catalogue, lignes)
                    : detail(famille, catalogue, parId, lignes);
            BigDecimal total = somme(lignes);
            blocs.add(new CoqDto.BlockView(famille, vues, total));
            if (famille.conformance()) {
                conformite = conformite.add(total);
            } else {
                nonConformite = nonConformite.add(total);
            }
        }

        BigDecimal ratio = nonConformite.signum() == 0
                ? null
                : conformite.divide(nonConformite, 2, RoundingMode.HALF_UP);
        List<CoqDto.MonthView> mois = month == null ? parMois(periode) : List.of();

        return new CoqDto.ReportView(year, month, currency(tenant), blocs,
                conformite, nonConformite, conformite.add(nonConformite), ratio, mois);
    }

    /** Le catalogue que propose la liste déroulante, dans l'ordre d'affichage. */
    public List<CoqDto.LabelView> labels() {
        UUID tenant = context.requireTenantId();
        return sorted(labels.findVisible(tenant)).stream().map(CostOfQualityService::vue).toList();
    }

    // ---------- écriture ----------

    /**
     * Ajoute un libellé tapé en texte libre.
     *
     * <p>Si un libellé de même nom existe déjà dans la famille — livré ou
     * saisi, casse et espaces ignorés —, c'est lui qui est rendu. Deux
     * « Rebuts » dans la même liste déroulante couperaient le cumul annuel en
     * deux lignes qu'on croirait différentes.
     */
    public CoqDto.LabelView createLabel(CoqDto.LabelCommand commande) {
        UUID tenant = context.requireTenantId();
        CoqLabel candidat = CoqLabel.custom(tenant, commande.category(), commande.name(),
                commande.partControl());
        String cle = cle(candidat.getName());
        Optional<CoqLabel> existant = labels.findVisible(tenant).stream()
                .filter(l -> l.getCategory() == candidat.getCategory() && cle(l.getName()).equals(cle))
                .findFirst();
        return vue(existant.orElseGet(() -> labels.save(candidat)));
    }

    public CoqDto.LineView record(CoqDto.EntryCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        CoqLabel libelle = libelle(commande.labelId(), tenant);
        CoqEntry ligne = entries.save(CoqEntry.record(tenant, libelle, details(commande), acteur,
                clock.instant()));
        audit.recorded(ligne, acteur);
        return ligne(ligne, libelle);
    }

    public CoqDto.LineView revise(UUID entryId, CoqDto.EntryCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        CoqEntry ligne = charger(entryId, tenant);
        CoqLabel libelle = libelle(commande.labelId(), tenant);
        ligne.revise(libelle, details(commande), clock.instant());
        CoqEntry enregistree = entries.save(ligne);
        audit.revised(enregistree, acteur);
        return ligne(enregistree, libelle);
    }

    public void delete(UUID entryId) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        CoqEntry ligne = charger(entryId, tenant);
        entries.delete(ligne);
        audit.deleted(ligne, acteur);
    }

    /** Fixe la devise d'affichage du client. Aucune conversion : les montants restent tels quels. */
    public String setCurrency(String code) {
        UUID tenant = context.requireTenantId();
        String devise = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(devise);
        } catch (IllegalArgumentException e) {
            throw new CoqValidationException("currency", "Devise inconnue : " + code);
        }
        settings.saveCurrency(tenant, devise);
        return devise;
    }

    // ---------- interne ----------

    private String currency(UUID tenant) {
        return settings.currency(tenant).orElse(DEFAULT_CURRENCY);
    }

    private CoqLabel libelle(UUID labelId, UUID tenant) {
        if (labelId == null) {
            throw new CoqValidationException("labelId", "Le libellé est obligatoire.");
        }
        return labels.findVisibleById(labelId, tenant)
                .orElseThrow(() -> new CoqNotFoundException("Label", labelId));
    }

    private CoqEntry charger(UUID entryId, UUID tenant) {
        return entries.findByIdAndTenant(entryId, tenant)
                .orElseThrow(() -> new CoqNotFoundException("Entry", entryId));
    }

    private static CoqEntryDetails details(CoqDto.EntryCommand c) {
        return new CoqEntryDetails(c.amount(), c.responsible(), c.imputationDate(), c.comment(),
                c.partReference(), c.partQuantity(), c.lot(), c.receivedOrMadeOn());
    }

    /** Vue mois : une ligne par imputation, puis les libellés livrés restés sans imputation. */
    private static List<CoqDto.LineView> detail(CoqCategory famille, List<CoqLabel> catalogue,
                                                Map<UUID, CoqLabel> parId, List<CoqEntry> lignes) {
        Map<UUID, Integer> rang = rangs(catalogue);
        List<CoqDto.LineView> vues = new ArrayList<>(lignes.stream()
                .sorted(Comparator
                        .comparing((CoqEntry e) -> rang.getOrDefault(e.getLabelId(), Integer.MAX_VALUE))
                        .thenComparing(CoqEntry::getImputationDate)
                        .thenComparing(CoqEntry::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(e -> ligne(e, parId.get(e.getLabelId())))
                .toList());
        ajouterLibellesVides(famille, catalogue, lignes, vues, rang);
        return vues;
    }

    /** Vue année : une ligne par libellé, cumulée sur les douze mois. */
    private static List<CoqDto.LineView> cumulParLibelle(CoqCategory famille, List<CoqLabel> catalogue,
                                                         List<CoqEntry> lignes) {
        Map<UUID, List<CoqEntry>> parLibelle = lignes.stream()
                .collect(Collectors.groupingBy(CoqEntry::getLabelId, LinkedHashMap::new, Collectors.toList()));
        List<CoqDto.LineView> vues = new ArrayList<>();
        for (CoqLabel l : catalogue) {
            if (l.getCategory() != famille) {
                continue;
            }
            List<CoqEntry> siennes = parLibelle.get(l.getId());
            if (siennes != null) {
                vues.add(new CoqDto.LineView(null, l.getId(), l.getCode(), l.getName(), l.isPartControl(),
                        somme(siennes), siennes.size(), null, null, null, null, null, null, null));
            } else if (l.builtIn()) {
                vues.add(vide(l));
            }
        }
        return vues;
    }

    private static void ajouterLibellesVides(CoqCategory famille, List<CoqLabel> catalogue,
                                             List<CoqEntry> lignes, List<CoqDto.LineView> vues,
                                             Map<UUID, Integer> rang) {
        Set<UUID> servis = lignes.stream().map(CoqEntry::getLabelId).collect(Collectors.toSet());
        List<CoqDto.LineView> vides = catalogue.stream()
                .filter(l -> l.getCategory() == famille && l.builtIn() && !servis.contains(l.getId()))
                .map(CostOfQualityService::vide)
                .toList();
        vues.addAll(vides);
        // Les lignes à zéro reprennent leur place dans l'ordre du catalogue,
        // au lieu de s'entasser en bas du bloc.
        vues.sort(Comparator.comparing(v -> rang.getOrDefault(v.labelId(), Integer.MAX_VALUE)));
    }

    private static List<CoqDto.MonthView> parMois(List<CoqEntry> periode) {
        List<CoqDto.MonthView> mois = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            final int cible = m;
            List<CoqEntry> duMois = periode.stream()
                    .filter(e -> e.getImputationDate().getMonthValue() == cible).toList();
            mois.add(new CoqDto.MonthView(m,
                    somme(duMois.stream().filter(e -> e.getCategory().conformance()).toList()),
                    somme(duMois.stream().filter(e -> !e.getCategory().conformance()).toList())));
        }
        return mois;
    }

    private static Map<UUID, Integer> rangs(List<CoqLabel> catalogue) {
        Map<UUID, Integer> rang = new LinkedHashMap<>();
        for (int i = 0; i < catalogue.size(); i++) {
            rang.put(catalogue.get(i).getId(), i);
        }
        return rang;
    }

    private static List<CoqLabel> sorted(List<CoqLabel> catalogue) {
        return catalogue.stream()
                .sorted(Comparator.comparing(CoqLabel::getCategory)
                        .thenComparingInt(CoqLabel::getPosition)
                        .thenComparing(l -> cle(l.getName())))
                .toList();
    }

    private static String cle(String nom) {
        return nom == null ? "" : nom.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static BigDecimal somme(List<CoqEntry> lignes) {
        return lignes.stream().map(CoqEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static CoqDto.LineView vide(CoqLabel l) {
        return new CoqDto.LineView(null, l.getId(), l.getCode(), l.getName(), l.isPartControl(),
                BigDecimal.ZERO, 0, null, null, null, null, null, null, null);
    }

    private static CoqDto.LineView ligne(CoqEntry e, CoqLabel l) {
        return new CoqDto.LineView(e.getId(), e.getLabelId(),
                l == null ? null : l.getCode(), l == null ? null : l.getName(), e.isPartControl(),
                e.getAmount(), 1, e.getResponsible(), e.getImputationDate(), e.getComment(),
                e.getPartReference(), e.getPartQuantity(), e.getLot(), e.getReceivedOrMadeOn());
    }

    private static CoqDto.LabelView vue(CoqLabel l) {
        return new CoqDto.LabelView(l.getId(), l.getCategory(), l.getCode(), l.getName(),
                l.isPartControl(), l.builtIn());
    }
}
