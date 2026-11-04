package com.openlab.qualitos.quality.nonconformity.subcategory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NcValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Le catalogue de sous-catégories de non-conformité du client : celui qu'il a
 * réglé, sinon celui de la plateforme.
 *
 * <p>Deux rôles, et pas un de plus : <b>rendre</b> le catalogue aux écrans, et
 * <b>vérifier</b> qu'un code porté par une NC existe bien et dépend bien de la
 * catégorie annoncée. La vérification est ici, et pas dans l'écran : l'API est
 * publique et l'application mobile rejoue sa file hors-ligne par le même
 * chemin (§13.1).
 */
@Service
public class NcSubcategoryCatalog {

    /** Au-delà, ce n'est plus une nomenclature mais un champ libre déguisé. */
    public static final int MAX_ENTRIES = 300;

    private static final TypeReference<List<NcSubcategory>> ENTRIES = new TypeReference<>() {};

    private final NcSubcategorySettingsJpaRepository repository;
    private final ObjectMapper mapper;

    public NcSubcategoryCatalog(NcSubcategorySettingsJpaRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** Le catalogue en vigueur pour le client courant, et d'où il vient. */
    @Transactional(readOnly = true)
    public View view() {
        UUID tenant = requireTenantId();
        Optional<NcSubcategorySettingsJpaEntity> reglage = repository.findByTenantId(tenant);
        return reglage
                .map(e -> new View(read(e.getCatalogue()), false))
                .orElseGet(() -> new View(NcSubcategoryDefaults.CATALOGUE, true));
    }

    /** Le catalogue en vigueur, à plat. */
    @Transactional(readOnly = true)
    public List<NcSubcategory> entries() {
        return view().entries();
    }

    /**
     * Remplace le catalogue du client. Les NC déjà classées sous un code retiré
     * gardent leur code : effacer un classement passé pour cause de changement
     * de nomenclature réécrirait l'histoire. L'écran affiche alors le code brut,
     * ce qui se voit — et se corrige.
     */
    @Transactional
    public View replace(List<NcSubcategory> entries) {
        UUID tenant = requireTenantId();
        List<NcSubcategory> propres = validated(entries);
        NcSubcategorySettingsJpaEntity e = repository.findByTenantId(tenant)
                .orElseGet(() -> {
                    NcSubcategorySettingsJpaEntity neuf = new NcSubcategorySettingsJpaEntity();
                    neuf.setTenantId(tenant);
                    return neuf;
                });
        e.setCatalogue(write(propres));
        e.setUpdatedBy(CurrentUser.userId().orElse(null));
        e.setUpdatedAt(Instant.now());
        repository.save(e);
        return new View(propres, false);
    }

    /** Revient au catalogue de la plateforme. */
    @Transactional
    public View reset() {
        UUID tenant = requireTenantId();
        repository.findByTenantId(tenant).ifPresent(repository::delete);
        return new View(NcSubcategoryDefaults.CATALOGUE, true);
    }

    /**
     * Le code d'une sous-catégorie tel qu'il peut être enregistré sur une NC :
     * {@code null} si rien n'est choisi (la sous-catégorie reste facultative —
     * un écart se saisit d'abord, se classe finement ensuite).
     *
     * @throws NcValidationException code inconnu du catalogue, ou rattaché à une
     *                               autre catégorie que celle de la NC
     */
    @Transactional(readOnly = true)
    public String resolve(String code, NcCategory category) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String recherche = code.strip();
        NcSubcategory trouvee = entries().stream()
                .filter(s -> s.code().equalsIgnoreCase(recherche))
                .findFirst()
                .orElseThrow(() -> new NcValidationException(
                        "Unknown non-conformity subcategory: " + recherche));
        if (category != null && trouvee.category() != category) {
            throw new NcValidationException("Subcategory " + trouvee.code() + " belongs to category "
                    + trouvee.category() + ", not " + category);
        }
        return trouvee.code();
    }

    /** Un catalogue sans doublon de code, borné, et dont chaque entrée tient. */
    private List<NcSubcategory> validated(List<NcSubcategory> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new NcValidationException("The subcategory catalogue cannot be empty; reset it instead");
        }
        if (entries.size() > MAX_ENTRIES) {
            throw new NcValidationException("At most " + MAX_ENTRIES + " subcategories");
        }
        Set<String> vus = new HashSet<>();
        List<NcSubcategory> propres = new ArrayList<>(entries.size());
        for (NcSubcategory s : entries) {
            if (s == null) {
                throw new NcValidationException("A subcategory entry cannot be null");
            }
            if (!vus.add(s.code().toUpperCase(java.util.Locale.ROOT))) {
                throw new NcValidationException("Duplicate subcategory code: " + s.code());
            }
            propres.add(s);
        }
        return List.copyOf(propres);
    }

    private List<NcSubcategory> read(String json) {
        try {
            return List.copyOf(mapper.readValue(json, ENTRIES));
        } catch (Exception e) {
            // Un réglage illisible est une anomalie de la plateforme, pas une
            // erreur de l'utilisateur : on remonte en 500 plutôt que de deviner
            // un catalogue et de classer les écarts sous une nomenclature qui
            // n'est pas celle du client.
            throw new IllegalStateException("unreadable nc subcategory catalogue", e);
        }
    }

    private String write(List<NcSubcategory> entries) {
        try {
            return mapper.writeValueAsString(entries);
        } catch (Exception e) {
            throw new IllegalStateException("unwritable nc subcategory catalogue", e);
        }
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    /**
     * Le catalogue et son origine.
     *
     * @param defaults vrai quand ce sont les valeurs de la plateforme, faute de
     *                 réglage du client — l'écran de paramétrage le dit
     */
    public record View(List<NcSubcategory> entries, boolean defaults) {
        public View {
            entries = List.copyOf(entries);
        }
    }
}
