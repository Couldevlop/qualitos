package com.openlab.qualitos.quality.rls;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La politique de cloisonnement PostgreSQL n'est pas écrite dans les migrations :
 * {@code RlsSchema} la pose <b>dynamiquement</b> sur toute table qui porte une
 * colonne {@code tenant_id} (ADR 0086).
 *
 * <p>Cette mécanique a une conséquence qu'on oublie : <b>une table sans
 * {@code tenant_id} n'est pas protégée, et rien ne le dit</b>. Elle ne lève
 * aucune erreur, elle n'apparaît dans aucun journal, elle est simplement absente
 * de la liste. C'est arrivé : {@code stakeholder_requirements} est née hors
 * politique, et seule une relecture à la main l'a vu.
 *
 * <p>Ce test ferme cette porte. Il énumère les entités JPA et exige que chacune
 * porte un {@code tenantId} — ou figure dans la liste ci-dessous, qui gèle
 * l'état connu. Une entité neuve sans {@code tenantId} fait donc échouer la
 * construction jusqu'à ce que quelqu'un tranche : colonne manquante, ou absence
 * délibérée à inscrire ici avec son motif.
 *
 * <p>Le test ne contrôle pas que les tables listées <em>devraient</em> en avoir
 * une : la plupart sont des tables filles rattachées à un agrégat porteur, ou des
 * référentiels de plateforme partagés par tous les clients. Statuer sur chacune
 * est un travail distinct ; ce test garantit seulement que la liste ne s'allonge
 * plus en silence.
 */
@DisplayName("Couverture du cloisonnement : toute entité porte un tenantId, ou est inscrite")
class TenantColumnCoverageTest {

    private static final String PACKAGE = "com.openlab.qualitos.quality";

    /**
     * Les entités sans {@code tenantId} au 2026-11-04, et elles seules.
     *
     * <p>Deux familles, qu'on ne mélange pas :
     * <ul>
     *   <li><b>Référentiels de plateforme</b> — le catalogue des normes, les packs
     *       sectoriels, le marketplace : des données partagées par tous les clients,
     *       qui n'appartiennent à aucun. Un {@code tenant_id} y serait faux.</li>
     *   <li><b>Tables filles</b> — une action de CAPA, une étape de PDCA, une cause
     *       d'Ishikawa : elles ne se chargent qu'à travers leur agrégat parent, lui
     *       filtré. La seconde barrière leur manque néanmoins, et l'inscrire ici le
     *       dit plutôt que de le taire.</li>
     * </ul>
     */
    private static final Set<String> SANS_TENANT_ID = Set.of(
            // Référentiels de plateforme : aucune donnée de client.
            "IndustryPack",
            "MarketplacePackJpaEntity",
            "NormClauseMappingEntity",
            // `standards` porte un owner_tenant_id — qui possède une norme
            // sur-mesure — et non un tenant_id : la sémantique diffère, et la
            // politique de cloisonnement n'a donc rien à y appliquer.
            "Standard",
            "StandardClause",
            "StandardDocumentTemplate",
            "StandardProcessTemplate",
            "StandardRequirement",
            "StandardRevision",
            "StandardSection",
            // Tables filles : chargées par leur agrégat parent, lui filtré.
            "AuditChecklistItem",
            "AuditFinding",
            "CapaAction",
            "CircleMeeting",
            "CircleMember",
            "DocumentVersion",
            "FiveSAuditItem",
            "IshikawaCause",
            "PdcaStep",
            "ProcessMeasure");

    @Test
    @DisplayName("aucune entité ne sort du cloisonnement sans être inscrite")
    void aucuneEntiteHorsPolitiqueNonInscrite() throws ClassNotFoundException {
        List<String> manquantes = new ArrayList<>();
        for (Class<?> entite : entitesJpa()) {
            String nom = entite.getSimpleName();
            if (!porteUnTenantId(entite) && !SANS_TENANT_ID.contains(nom)) {
                manquantes.add(nom);
            }
        }
        assertThat(manquantes)
                .describedAs("""
                        Ces entités n'ont pas de tenantId : leur table reste donc HORS de la \
                        politique de cloisonnement (ADR 0086), sans que rien ne le signale. \
                        Ajoute la colonne, ou inscris-les dans SANS_TENANT_ID avec son motif.""")
                .isEmpty();
    }

    @Test
    @DisplayName("la liste des exceptions ne garde pas de nom périmé")
    void listeSansNomPerime() throws ClassNotFoundException {
        Collection<Class<?>> entites = entitesJpa();
        Set<String> noms = new TreeSet<>();
        for (Class<?> e : entites) {
            noms.add(e.getSimpleName());
        }
        List<String> perimees = new ArrayList<>();
        for (String inscrite : new TreeSet<>(SANS_TENANT_ID)) {
            // Soit l'entité a disparu, soit elle a GAGNÉ un tenantId : dans les deux
            // cas son inscription ne protège plus rien et masquerait une régression.
            if (!noms.contains(inscrite)) {
                perimees.add(inscrite + " (entité introuvable)");
                continue;
            }
            for (Class<?> e : entites) {
                if (e.getSimpleName().equals(inscrite) && porteUnTenantId(e)) {
                    perimees.add(inscrite + " (porte désormais un tenantId)");
                }
            }
        }
        assertThat(perimees)
                .describedAs("Retire ces noms de SANS_TENANT_ID : l'exception n'a plus d'objet.")
                .isEmpty();
    }

    /** Un {@code tenantId} déclaré par l'entité ou par l'une de ses classes mères. */
    private static boolean porteUnTenantId(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if ("tenantId".equals(f.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Collection<Class<?>> entitesJpa() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        List<Class<?>> out = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(PACKAGE)) {
            out.add(Class.forName(bd.getBeanClassName()));
        }
        // Le filet du filet : si le balayage ne trouvait rien, le test passerait
        // en annonçant une couverture totale, ce qui serait le pire des résultats.
        assertThat(out).describedAs("balayage des entités JPA").hasSizeGreaterThan(100);
        return out;
    }
}
