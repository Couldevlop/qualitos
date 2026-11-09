package com.openlab.qualitos.core.platformaudit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'empreinte d'un acte : ce qu'elle doit distinguer, et ce qu'elle ne doit
 * pas confondre.
 *
 * <p>Le banc à lire en premier est {@link #deux_actes_ne_se_confondent_pas_par_un_separateur_tape} :
 * c'est la seule faiblesse d'une forme canonique par concaténation, et elle ne
 * se voit pas à la lecture du code.
 */
@DisplayName("PlatformAuditHasher")
class PlatformAuditHasherTest {

    private static final Instant QUAND = Instant.parse("2026-11-08T09:30:00Z");
    private static final UUID ACTEUR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID CIBLE = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    @Test
    @DisplayName("rend 64 caracteres hexadecimaux minuscules")
    void forme() {
        String empreinte = PlatformAuditHasher.hash(acte(1, "Client cree", null));

        assertThat(empreinte).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("le meme acte rend la meme empreinte")
    void deterministe() {
        assertThat(PlatformAuditHasher.hash(acte(1, "Client cree", null)))
                .isEqualTo(PlatformAuditHasher.hash(acte(1, "Client cree", null)));
    }

    @Test
    @DisplayName("un resume different rend une empreinte differente")
    void sensible_au_contenu() {
        assertThat(PlatformAuditHasher.hash(acte(1, "Client cree", null)))
                .isNotEqualTo(PlatformAuditHasher.hash(acte(1, "Client desactive", null)));
    }

    @Test
    @DisplayName("le numero de sequence entre dans l'empreinte")
    void sensible_a_la_sequence() {
        assertThat(PlatformAuditHasher.hash(acte(1, "Client cree", null)))
                .isNotEqualTo(PlatformAuditHasher.hash(acte(2, "Client cree", null)));
    }

    @Test
    @DisplayName("l'empreinte du precedent entre dans l'empreinte : c'est ce qui fait la chaine")
    void chainage() {
        String sansPrecedent = PlatformAuditHasher.hash(acte(2, "Client cree", null));
        String avecPrecedent = PlatformAuditHasher.hash(acte(2, "Client cree", "a".repeat(64)));

        assertThat(sansPrecedent).isNotEqualTo(avecPrecedent);
    }

    @Test
    @DisplayName("deux actes ne se confondent pas quand un champ contient le separateur")
    void deux_actes_ne_se_confondent_pas_par_un_separateur_tape() {
        // Avec un séparateur imprimable (« ; » par exemple), ces deux actes
        // produiraient la MÊME forme canonique : le premier aurait un résumé
        // qui déborde sur le champ suivant. Deux actes différents, une seule
        // empreinte — et l'un des deux pourrait remplacer l'autre sans que la
        // vérification s'en aperçoive. Le caractère de contrôle U+001E n'arrive
        // pas par un formulaire, mais le banc vérifie qu'il ne casse rien même
        // s'il y arrivait.
        PlatformAuditEvent premier = acte(1, "Client cree\u001Esuite", null);
        PlatformAuditEvent second = acte(1, "Client cree", null);

        assertThat(PlatformAuditHasher.hash(premier))
                .isNotEqualTo(PlatformAuditHasher.hash(second));
    }

    @Test
    @DisplayName("un champ nul et un champ vide ne se confondent pas avec un contenu")
    void nuls_et_vides() {
        PlatformAuditEvent sansDetail = new PlatformAuditEvent(
                UUID.randomUUID(), 1L, QUAND, QUAND, PlatformActorType.SYSTEM, null,
                PlatformAuditAction.TENANT_CREATED, "tenant", CIBLE, CIBLE,
                "Client cree", null, null, null, null);
        PlatformAuditEvent avecDetail = new PlatformAuditEvent(
                UUID.randomUUID(), 1L, QUAND, QUAND, PlatformActorType.SYSTEM, null,
                PlatformAuditAction.TENANT_CREATED, "tenant", CIBLE, CIBLE,
                "Client cree", "{}", null, null, null);

        assertThat(PlatformAuditHasher.hash(sansDetail))
                .isNotEqualTo(PlatformAuditHasher.hash(avecDetail));
    }

    @Test
    @DisplayName("l'adresse d'origine entre dans l'empreinte, contrairement au journal du moteur")
    void origine_scellee() {
        PlatformAuditEvent depuisLeBureau = new PlatformAuditEvent(
                UUID.randomUUID(), 1L, QUAND, QUAND, PlatformActorType.USER, ACTEUR,
                PlatformAuditAction.MODULE_PRICE_SET, "module_price", CIBLE, null,
                "Tarif fixe", null, "10.0.0.8", "Chrome", null);
        PlatformAuditEvent depuisAilleurs = new PlatformAuditEvent(
                UUID.randomUUID(), 1L, QUAND, QUAND, PlatformActorType.USER, ACTEUR,
                PlatformAuditAction.MODULE_PRICE_SET, "module_price", CIBLE, null,
                "Tarif fixe", null, "203.0.113.7", "Chrome", null);

        assertThat(PlatformAuditHasher.hash(depuisLeBureau))
                .isNotEqualTo(PlatformAuditHasher.hash(depuisAilleurs));
    }

    @Test
    @DisplayName("sha256 d'une chaine connue : la reference ne derive pas en silence")
    void reference_connue() {
        // Vecteur public du SHA-256. Sans lui, une erreur de conversion en
        // hexadécimal (un octet signé mal masqué, par exemple) resterait
        // invisible : toutes les empreintes seraient fausses de la même façon,
        // donc cohérentes entre elles, et tous les bancs ci-dessus passeraient.
        assertThat(PlatformAuditHasher.sha256("abc")).isEqualTo(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(PlatformAuditHasher.sha256("")).isEqualTo(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    private static PlatformAuditEvent acte(long sequence, String resume, String precedente) {
        return new PlatformAuditEvent(
                UUID.randomUUID(), sequence, QUAND, QUAND,
                PlatformActorType.USER, ACTEUR,
                PlatformAuditAction.TENANT_CREATED, "tenant", CIBLE, CIBLE,
                resume, null, "10.0.0.8", "Chrome/130", precedente);
    }
}
