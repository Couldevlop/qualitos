package com.openlab.qualitos.quality.nonconformity.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le balayeur EFFACE des octets qu'aucune sauvegarde applicative ne rappellera.
 * Ces tests portent donc moins sur ce qu'il supprime que sur ce qu'il refuse de
 * supprimer : c'est là que se jouent les dégâts.
 */
class OrphanObjectSweeperTest {

    static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");
    static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    static final String ANCIEN = "tenants/t1/capa/c1/vieux.pdf";
    static final String RECENT = "tenants/t1/capa/c1/frais.pdf";

    InMemoryObjectStorage storage;
    OrphanSweepProperties props;

    @BeforeEach
    void setUp() {
        storage = new InMemoryObjectStorage();
        props = new OrphanSweepProperties();
        props.setEnabled(true);
    }

    /** Propriétaire qui revendique un jeu de clés fixé. */
    static class FakeOwner implements StoredObjectOwner {
        private final Set<String> owned;
        FakeOwner(Set<String> owned) { this.owned = owned; }
        @Override public boolean isReferenced(String objectKey) { return owned.contains(objectKey); }
        @Override public String ownerName() { return "fake"; }
    }

    /** Propriétaire en panne : il ne doit jamais faire passer un objet pour orphelin. */
    static class BrokenOwner implements StoredObjectOwner {
        @Override public boolean isReferenced(String objectKey) {
            throw new IllegalStateException("dépôt indisponible");
        }
        @Override public String ownerName() { return "broken"; }
    }

    private OrphanObjectSweeper sweeper(List<StoredObjectOwner> owners) {
        return new OrphanObjectSweeper(provider(storage), owners, props, FIXED_CLOCK);
    }

    /** {@link ObjectProvider} réduit à ce que le balayeur en utilise. */
    @SuppressWarnings("unchecked")
    private static ObjectProvider<ObjectStorage> provider(ObjectStorage value) {
        ObjectProvider<ObjectStorage> p = org.mockito.Mockito.mock(ObjectProvider.class);
        org.mockito.Mockito.lenient().when(p.getIfAvailable()).thenReturn(value);
        return p;
    }

    private void putAged(String key, java.time.Duration age) {
        storage.setNow(NOW.minus(age));
        storage.put(key, "application/pdf", new byte[] {1, 2, 3});
    }

    // --- ce qui NE doit pas être supprimé ---------------------------------------

    @Test
    void disabled_touchesNothing() {
        // Le défaut d'usine. Un balayage qui s'allumerait tout seul effacerait des
        // preuves dans un bucket que l'exploitant n'a pas encore examiné.
        props.setEnabled(false);
        putAged(ANCIEN, java.time.Duration.ofDays(30));

        OrphanObjectSweeper.Report report = sweeper(List.of(new FakeOwner(Set.of()))).sweep();

        assertThat(report.ran()).isFalse();
        assertThat(storage.contains(ANCIEN)).isTrue();
    }

    @Test
    void storageOff_reportsNothingRatherThanFailing() {
        OrphanObjectSweeper.Report report =
                new OrphanObjectSweeper(provider(null), List.of(new FakeOwner(Set.of())),
                        props, FIXED_CLOCK).sweep();

        assertThat(report.ran()).isFalse();
    }

    @Test
    void noOwnerDeclared_cancelsInsteadOfEmptyingTheBucket() {
        // Sans propriétaire, TOUT paraît orphelin. Le cas ne devrait pas se
        // produire — et s'il se produit, ne rien faire est la seule issue sûre.
        putAged(ANCIEN, java.time.Duration.ofDays(30));

        OrphanObjectSweeper.Report report = sweeper(List.of()).sweep();

        assertThat(report.ran()).isFalse();
        assertThat(storage.contains(ANCIEN)).isTrue();
    }

    @Test
    void recentObject_isLeftAlone() {
        // Entre le `put` et la validation de la transaction, l'objet existe sans
        // sa ligne : le supprimer effacerait une preuve au moment du dépôt.
        putAged(RECENT, java.time.Duration.ofMinutes(5));

        OrphanObjectSweeper.Report report = sweeper(List.of(new FakeOwner(Set.of()))).sweep();

        assertThat(storage.contains(RECENT)).isTrue();
        assertThat(report.tooRecent()).isEqualTo(1);
        assertThat(report.deleted()).isZero();
    }

    @Test
    void referencedObject_isLeftAlone() {
        putAged(ANCIEN, java.time.Duration.ofDays(30));

        OrphanObjectSweeper.Report report = sweeper(List.of(new FakeOwner(Set.of(ANCIEN)))).sweep();

        assertThat(storage.contains(ANCIEN)).isTrue();
        assertThat(report.deleted()).isZero();
        assertThat(report.examined()).isEqualTo(1);
    }

    @Test
    void ownerFailure_keepsTheObjectAndCountsTheFailure() {
        // En cas de doute, on ne supprime pas : une panne de dépôt ne doit pas se
        // traduire par « personne ne le revendique ».
        putAged(ANCIEN, java.time.Duration.ofDays(30));

        OrphanObjectSweeper.Report report = sweeper(List.of(new BrokenOwner())).sweep();

        assertThat(storage.contains(ANCIEN)).isTrue();
        assertThat(report.failures()).isEqualTo(1);
        assertThat(report.deleted()).isZero();
    }

    @Test
    void oneFailure_doesNotStopTheRun() {
        putAged("tenants/t1/capa/c1/a.pdf", java.time.Duration.ofDays(30));
        putAged("tenants/t1/capa/c1/b.pdf", java.time.Duration.ofDays(30));
        // Le premier propriétaire tombe sur toutes les clés ; le balayage doit
        // néanmoins examiner les deux objets.
        OrphanObjectSweeper.Report report = sweeper(List.of(new BrokenOwner())).sweep();

        assertThat(report.examined()).isEqualTo(2);
        assertThat(report.failures()).isEqualTo(2);
    }

    // --- ce qui DOIT être supprimé ----------------------------------------------

    @Test
    void unreferencedAgedObject_isDeletedAndAccounted() {
        putAged(ANCIEN, java.time.Duration.ofDays(30));

        OrphanObjectSweeper.Report report = sweeper(List.of(new FakeOwner(Set.of()))).sweep();

        assertThat(storage.contains(ANCIEN)).isFalse();
        assertThat(report.ran()).isTrue();
        assertThat(report.deleted()).isEqualTo(1);
        assertThat(report.bytesReclaimed()).isEqualTo(3);
    }

    @Test
    void severalOwners_anyClaimIsEnough() {
        // La clé appartient au second module : le premier ne la connaît pas, et
        // s'arrêter à lui supprimerait une pièce vivante.
        putAged(ANCIEN, java.time.Duration.ofDays(30));

        OrphanObjectSweeper.Report report = sweeper(List.of(
                new FakeOwner(Set.of("tenants/t1/nc/n1/photo.jpg")),
                new FakeOwner(Set.of(ANCIEN)))).sweep();

        assertThat(storage.contains(ANCIEN)).isTrue();
        assertThat(report.deleted()).isZero();
    }

    @Test
    void objectsOutsideTheKeyRoot_areNeverExamined() {
        // Le bucket peut être partagé : effacer ce qu'un autre outil y dépose
        // serait exactement le dégât qu'on cherche à éviter.
        putAged("autre-outil/sauvegarde.tar", java.time.Duration.ofDays(30));

        OrphanObjectSweeper.Report report = sweeper(List.of(new FakeOwner(Set.of()))).sweep();

        assertThat(storage.contains("autre-outil/sauvegarde.tar")).isTrue();
        assertThat(report.examined()).isZero();
    }

    @Test
    void batchSize_boundsWhatASinglePassExamines() {
        for (int i = 0; i < 5; i++) {
            putAged("tenants/t1/capa/c1/f" + i + ".pdf", java.time.Duration.ofDays(30));
        }
        props.setBatchSize(2);

        OrphanObjectSweeper.Report report = sweeper(List.of(new FakeOwner(Set.of()))).sweep();

        assertThat(report.examined()).isEqualTo(2);
        assertThat(report.deleted()).isEqualTo(2);
    }

    @Test
    void ownersAreConsultedOnlyForAgedObjects() {
        // Interroger la base pour un objet trop récent serait une requête perdue :
        // la décision est déjà prise par la date.
        putAged(RECENT, java.time.Duration.ofMinutes(1));
        List<String> asked = new ArrayList<>();
        StoredObjectOwner spy = new StoredObjectOwner() {
            @Override public boolean isReferenced(String key) { asked.add(key); return false; }
            @Override public String ownerName() { return "spy"; }
        };

        sweeper(List.of(spy)).sweep();

        assertThat(asked).isEmpty();
    }
}
