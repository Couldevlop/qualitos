package com.openlab.qualitos.quality.settings.catalog;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Les listes déroulantes du client : celles qu'il a réglées, sinon celles de la
 * plateforme.
 *
 * <p>Un seul service pour les douze listes, et pour celles à venir. Trois
 * garanties, qui sont la raison d'être de ce socle :
 *
 * <ol>
 *   <li><b>Aucune régression.</b> Un client qui n'a rien réglé lit exactement ce
 *       qu'il lisait avant : les valeurs de la plateforme. Aucune migration de
 *       données, aucun écran à remplir avant de travailler.</li>
 *   <li><b>Le code ne meurt jamais.</b> Retirer une entrée ne reclasse pas
 *       l'historique : les enregistrements gardent leur code, et l'écran
 *       l'affiche tel quel — ce qui se voit, et se corrige.</li>
 *   <li><b>Le code ne mentira pas.</b> Sur une liste adossée à une énumération
 *       du code ({@link CatalogMode#RENAME}), ajouter une valeur est refusé :
 *       la plateforme accepterait une donnée qu'aucun chemin ne sait traiter.</li>
 * </ol>
 */
@Service
public class TenantCatalogService {

    private final TenantCatalogEntryRepository repository;

    public TenantCatalogService(TenantCatalogEntryRepository repository) {
        this.repository = repository;
    }

    /** Toutes les listes ouvertes au paramétrage, avec leur état pour ce client. */
    @Transactional(readOnly = true)
    public List<Summary> summaries() {
        Set<String> reglees = new HashSet<>(repository.findCustomisedCatalogues(requireTenantId()));
        List<Summary> resume = new ArrayList<>(CatalogId.all().size());
        for (CatalogId id : CatalogId.all()) {
            resume.add(new Summary(id.slug(), id.mode(), !reglees.contains(id.slug()),
                    id.defaults().size()));
        }
        return List.copyOf(resume);
    }

    /**
     * Les listes que ce client a réglées, et elles seules, en un seul appel.
     *
     * <p>C'est ce que l'application charge au démarrage. Un client qui n'a rien
     * réglé reçoit un objet vide, et ses écrans gardent leurs propres libellés
     * traduits : douze appels pour apprendre qu'il n'y a rien à savoir seraient
     * douze appels de trop.
     */
    @Transactional(readOnly = true)
    public Map<String, List<CatalogEntry>> customised() {
        Map<String, List<CatalogEntry>> reglees = new LinkedHashMap<>();
        UUID tenant = requireTenantId();
        for (String slug : repository.findCustomisedCatalogues(tenant)) {
            CatalogId.bySlug(slug).ifPresent(id -> reglees.put(id.slug(),
                    repository.findByTenantIdAndCatalogueOrderByPositionAsc(tenant, id.slug())
                            .stream()
                            .map(l -> new CatalogEntry(l.getCode(), l.getLabel(), l.isActive()))
                            .toList()));
        }
        return Map.copyOf(reglees);
    }

    /** La liste en vigueur pour ce client, et d'où elle vient. */
    @Transactional(readOnly = true)
    public View view(CatalogId id) {
        List<TenantCatalogEntryRow> lignes =
                repository.findByTenantIdAndCatalogueOrderByPositionAsc(requireTenantId(), id.slug());
        if (lignes.isEmpty()) {
            return new View(id, true, id.defaults());
        }
        return new View(id, false, lignes.stream()
                .map(l -> new CatalogEntry(l.getCode(), l.getLabel(), l.isActive()))
                .toList());
    }

    /**
     * Les entrées actives de la liste, dans l'ordre : ce qu'un formulaire propose.
     * Les entrées désactivées en sont exclues — elles ne servent plus qu'à
     * relire l'historique.
     */
    @Transactional(readOnly = true)
    public List<CatalogEntry> active(CatalogId id) {
        return view(id).entries().stream().filter(CatalogEntry::active).toList();
    }

    /**
     * Le libellé d'un code, pour un rapport ou un export rendu côté serveur.
     * Rend le code lui-même quand il n'est plus au catalogue : mieux vaut un code
     * visible qu'un vide, qui laisserait croire que la donnée est absente.
     */
    @Transactional(readOnly = true)
    public String label(CatalogId id, String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        String recherche = code.strip().toUpperCase(Locale.ROOT);
        return view(id).entries().stream()
                .filter(e -> e.code().equals(recherche))
                .map(CatalogEntry::label)
                .findFirst()
                .orElse(recherche);
    }

    /**
     * Remplace la liste du client. L'ordre reçu est l'ordre d'affichage.
     *
     * <p>Remplacement complet, et non modification entrée par entrée : une liste
     * déroulante se lit comme un tout, et un enregistrement partiel laisserait
     * des trous d'ordre au milieu.
     */
    @Transactional
    public View replace(CatalogId id, List<CatalogEntry> entries) {
        UUID tenant = requireTenantId();
        List<CatalogEntry> propres = validated(id, entries);
        repository.deleteByTenantIdAndCatalogue(tenant, id.slug());
        repository.flush();
        Instant maintenant = Instant.now();
        UUID acteur = CurrentUser.userId().orElse(null);
        List<TenantCatalogEntryRow> lignes = new ArrayList<>(propres.size());
        for (int i = 0; i < propres.size(); i++) {
            CatalogEntry e = propres.get(i);
            TenantCatalogEntryRow ligne = new TenantCatalogEntryRow();
            ligne.setTenantId(tenant);
            ligne.setCatalogue(id.slug());
            ligne.setCode(e.code());
            ligne.setLabel(e.label());
            ligne.setPosition(i);
            ligne.setActive(e.active());
            ligne.setUpdatedBy(acteur);
            ligne.setUpdatedAt(maintenant);
            lignes.add(ligne);
        }
        repository.saveAll(lignes);
        return new View(id, false, propres);
    }

    /** Revient aux valeurs de la plateforme. */
    @Transactional
    public View reset(CatalogId id) {
        repository.deleteByTenantIdAndCatalogue(requireTenantId(), id.slug());
        return new View(id, true, id.defaults());
    }

    /**
     * Une liste non vide, bornée, sans doublon de code — et, sur une liste
     * adossée au code, strictement les codes de la plateforme, tous actifs.
     */
    private List<CatalogEntry> validated(CatalogId id, List<CatalogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new CatalogValidationException(
                    "A catalogue cannot be empty; reset it to the platform values instead");
        }
        if (entries.size() > CatalogId.MAX_ENTRIES) {
            throw new CatalogValidationException("At most " + CatalogId.MAX_ENTRIES + " entries");
        }
        Map<String, CatalogEntry> parCode = new LinkedHashMap<>();
        for (CatalogEntry e : entries) {
            if (e == null) {
                throw new CatalogValidationException("A catalogue entry cannot be null");
            }
            if (parCode.put(e.code(), e) != null) {
                throw new CatalogValidationException("Duplicate entry code: " + e.code());
            }
        }
        if (id.mode() == CatalogMode.RENAME) {
            requireSameCodes(id, parCode.keySet());
            parCode.values().stream()
                    .filter(e -> !e.active())
                    .findFirst()
                    .ifPresent(e -> {
                        throw new CatalogValidationException(
                                "Entry " + e.code() + " cannot be deactivated: the code drives "
                                        + "platform behaviour, only its label and order are yours");
                    });
        }
        if (parCode.values().stream().noneMatch(CatalogEntry::active)) {
            throw new CatalogValidationException("At least one entry must stay active");
        }
        return List.copyOf(new ArrayList<>(parCode.values()));
    }

    /** Sur une liste adossée au code, l'ensemble des codes est celui de la plateforme. */
    private void requireSameCodes(CatalogId id, Set<String> recus) {
        Set<String> attendus = new HashSet<>();
        id.defaults().forEach(e -> attendus.add(e.code()));
        for (String code : recus) {
            if (!attendus.contains(code)) {
                throw new CatalogValidationException("Unknown code '" + code + "' for catalogue "
                        + id.slug() + ": its codes belong to the platform, you rename and reorder them");
            }
        }
        for (String code : attendus) {
            if (!recus.contains(code)) {
                throw new CatalogValidationException("Missing code '" + code + "' for catalogue "
                        + id.slug() + ": every platform code must stay listed");
            }
        }
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    /**
     * L'état d'une liste pour l'écran de paramétrage.
     *
     * @param defaults vrai quand le client n'a rien réglé — les écrans gardent
     *                 alors leurs propres traductions
     */
    public record Summary(String catalogue, CatalogMode mode, boolean defaults, int entryCount) {
    }

    /** Une liste rendue : son nom, son mode, son origine et ses entrées ordonnées. */
    public record View(CatalogId id, boolean defaults, List<CatalogEntry> entries) {
        public View {
            entries = List.copyOf(entries);
        }
    }
}
