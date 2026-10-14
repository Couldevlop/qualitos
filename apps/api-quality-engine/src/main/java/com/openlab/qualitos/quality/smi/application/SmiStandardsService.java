package com.openlab.qualitos.quality.smi.application;

import com.openlab.qualitos.quality.smi.domain.StandardOrder;
import com.openlab.qualitos.quality.smi.domain.StandardUnavailableException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ajouter une norme au système de management intégré, depuis le SMI (§8.9).
 *
 * <p>Le tableau de bord ne savait que montrer les normes adoptées ; en ajouter
 * une demandait de quitter le SMI pour le catalogue du Standards Hub, où les
 * normes de système de management se perdent parmi une soixantaine de
 * référentiels sectoriels et réglementaires. Ici ne sont proposées que celles
 * qui partagent les chapitres communs 4 à 10 — les seules que la matrice des
 * exigences sait croiser.
 *
 * <p>L'adoption elle-même reste celle du Standards Hub : même statut initial,
 * même feuille de route de certification, même contrôle d'unicité.
 *
 * <p>Une norme est « suggérée » quand le catalogue la rattache à une norme déjà
 * pilotée, dans un sens ou dans l'autre (ISO 14001 cite ISO 9001). Les
 * suggestions passent en tête, les plus rattachées d'abord, puis dans l'ordre
 * naturel des codes.
 */
public class SmiStandardsService {

    private final SmiPorts.Standards standards;
    private final SmiPorts.Catalog catalog;

    public SmiStandardsService(SmiPorts.Standards standards, SmiPorts.Catalog catalog) {
        this.standards = standards;
        this.catalog = catalog;
    }

    /** Les normes de système de management que le client n'a pas encore adoptées. */
    public List<SmiDto.Candidate> candidates() {
        List<SmiPorts.CatalogStandard> tout = catalog.managementSystems();
        Set<String> pilotees = standards.adopted().stream()
                .map(a -> norm(a.code()))
                .collect(Collectors.toUnmodifiableSet());
        Map<String, SmiPorts.CatalogStandard> parCode = tout.stream()
                .collect(Collectors.toMap(s -> norm(s.code()), Function.identity(), (x, y) -> x));

        return tout.stream()
                .filter(s -> !s.adopted())
                .map(s -> {
                    List<String> liees = pilotees.stream()
                            .filter(p -> lies(s, p, parCode.get(p)))
                            .sorted(StandardOrder.BY_CODE)
                            .toList();
                    return new SmiDto.Candidate(s.standardId(), s.code(), s.name(), s.version(),
                            !liees.isEmpty(), liees);
                })
                .sorted(Comparator.comparing(SmiDto.Candidate::suggested).reversed()
                        .thenComparing((SmiDto.Candidate c) -> c.sharedWith().size(), Comparator.reverseOrder())
                        .thenComparing(SmiDto.Candidate::code, StandardOrder.BY_CODE))
                .toList();
    }

    /** Adopte la norme ; refuse ce que {@link #candidates()} n'aurait pas proposé. */
    public SmiDto.StandardRef add(UUID standardId) {
        SmiPorts.CatalogStandard norme = catalog.managementSystems().stream()
                .filter(s -> s.standardId().equals(standardId))
                .findFirst()
                .orElseThrow(() -> new StandardUnavailableException(standardId,
                        StandardUnavailableException.Reason.NOT_A_MANAGEMENT_SYSTEM));
        if (norme.adopted()) {
            throw new StandardUnavailableException(standardId, StandardUnavailableException.Reason.ALREADY_ADOPTED);
        }
        SmiPorts.AdoptedStandard a = catalog.adopt(standardId);
        return new SmiDto.StandardRef(a.adoptionId(), a.code(), a.name());
    }

    /** Rattachées si l'une cite l'autre dans ses normes associées. */
    private static boolean lies(SmiPorts.CatalogStandard candidate, String pilotee,
                                SmiPorts.CatalogStandard piloteeAuCatalogue) {
        if (candidate.relatedCodes().stream().map(SmiStandardsService::norm).anyMatch(pilotee::equals)) {
            return true;
        }
        String code = norm(candidate.code());
        return piloteeAuCatalogue != null
                && piloteeAuCatalogue.relatedCodes().stream().map(SmiStandardsService::norm).anyMatch(code::equals);
    }

    private static String norm(String code) {
        return code == null ? "" : code.strip().toLowerCase(Locale.ROOT);
    }
}
