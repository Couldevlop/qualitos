package com.openlab.qualitos.core.platformaudit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La piste d'audit de la plateforme.
 *
 * <p>Les bancs qui comptent le plus ne sont pas ceux qui vérifient qu'une
 * ligne s'écrit, mais ceux qui vérifient que l'appelant ne décide PAS de son
 * contenu : ni l'acteur, ni l'origine, ni l'horodatage ne sont des paramètres.
 * Une piste d'audit dont l'appelant choisit l'auteur ne vaut rien, et c'est le
 * genre de défaut qu'une revue laisse passer parce que le code « a l'air »
 * correct.
 */
@DisplayName("PlatformAuditService")
class PlatformAuditServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-08T09:30:00.123456789Z");
    private static final UUID ACTEUR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID CLIENT = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private PlatformAuditEventRepository events;
    private PlatformAuditCounterRepository counters;
    private PlatformAuditService service;
    private List<PlatformAuditEvent> enregistres;

    @BeforeEach
    void setUp() {
        events = mock(PlatformAuditEventRepository.class);
        counters = mock(PlatformAuditCounterRepository.class);
        service = new PlatformAuditService(events, counters,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));

        enregistres = new ArrayList<>();
        when(counters.lock()).thenReturn(Optional.of(compteur(0L)));
        when(events.findTopByOrderBySequenceNoDesc()).thenReturn(Optional.empty());
        when(events.save(any(PlatformAuditEvent.class))).thenAnswer(appel -> {
            PlatformAuditEvent acte = appel.getArgument(0);
            enregistres.add(acte);
            return acte;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("écriture")
    class Ecriture {

        @Test
        @DisplayName("consigne l'acte avec son numero de sequence reserve sous verrou")
        void sequence_reservee() {
            when(counters.lock()).thenReturn(Optional.of(compteur(41L)));

            PlatformAuditEvent acte = service.record(PlatformAuditAction.TENANT_CREATED,
                    "tenant", CLIENT, CLIENT, "Client cree : Metalux", null);

            assertThat(acte.getSequenceNo()).isEqualTo(42L);
            // Le verrou est le seul intérêt de cet appel : deux écritures
            // simultanées sans lui viseraient le même numéro.
            verify(counters).lock();
        }

        @Test
        @DisplayName("l'acteur vient du jeton, jamais d'un parametre")
        void acteur_du_jeton() {
            authentifie(ACTEUR.toString());

            PlatformAuditEvent acte = service.record(PlatformAuditAction.MODULE_PRICE_SET,
                    "module_price", CLIENT, null, "Tarif fixe", null);

            assertThat(acte.getActorType()).isEqualTo(PlatformActorType.USER);
            assertThat(acte.getActorUserId()).isEqualTo(ACTEUR);
        }

        @Test
        @DisplayName("sans identite exploitable, l'acte est celui de la machine et le dit")
        void acteur_machine() {
            // Le piège serait d'attribuer l'acte à un utilisateur par défaut,
            // ou de laisser USER avec un acteur nul — la base refuserait, mais
            // en production, loin de la ligne fautive.
            PlatformAuditEvent acte = service.record(PlatformAuditAction.TENANT_CREATED,
                    "tenant", CLIENT, CLIENT, "Client cree par provisionnement", null);

            assertThat(acte.getActorType()).isEqualTo(PlatformActorType.SYSTEM);
            assertThat(acte.getActorUserId()).isNull();
        }

        @Test
        @DisplayName("un sujet de jeton qui n'est pas un UUID ne devient pas un acteur")
        void sujet_non_uuid() {
            authentifie("compte-de-service");

            PlatformAuditEvent acte = service.record(PlatformAuditAction.TENANT_CREATED,
                    "tenant", CLIENT, CLIENT, "Client cree", null);

            assertThat(acte.getActorType()).isEqualTo(PlatformActorType.SYSTEM);
            assertThat(acte.getActorUserId()).isNull();
        }

        @Test
        @DisplayName("l'origine est lue de la requete, derriere le repartiteur")
        void origine_transmise() {
            MockHttpServletRequest requete = new MockHttpServletRequest();
            requete.setRemoteAddr("10.42.0.1");
            requete.addHeader("X-Forwarded-For", "203.0.113.7, 10.42.0.1");
            requete.addHeader("User-Agent", "Mozilla/5.0");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requete));

            PlatformAuditEvent acte = service.record(PlatformAuditAction.TENANT_CREATED,
                    "tenant", CLIENT, CLIENT, "Client cree", null);

            // L'adresse du client, pas celle de l'ingress : sans ce découpage,
            // toute la piste d'audit porterait la même adresse interne.
            assertThat(acte.getIpAddress()).isEqualTo("203.0.113.7");
            assertThat(acte.getUserAgent()).isEqualTo("Mozilla/5.0");
        }

        @Test
        @DisplayName("hors requete HTTP, l'origine est absente plutot qu'inventee")
        void origine_absente() {
            PlatformAuditEvent acte = service.record(PlatformAuditAction.TENANT_CREATED,
                    "tenant", CLIENT, CLIENT, "Client cree", null);

            assertThat(acte.getIpAddress()).isNull();
            assertThat(acte.getUserAgent()).isNull();
        }

        @Test
        @DisplayName("l'horodatage est tronque a la microseconde, comme la base le rend")
        void horodatage_stockable() {
            PlatformAuditEvent acte = service.record(PlatformAuditAction.TENANT_CREATED,
                    "tenant", CLIENT, CLIENT, "Client cree", null);

            // .123456789 arrondirait à .123457 en base, et l'empreinte
            // recalculée après relecture ne concorderait plus : le journal
            // s'accuserait lui-même de falsification.
            assertThat(acte.getOccurredAt()).isEqualTo(Instant.parse("2026-11-08T09:30:00.123456Z"));
            assertThat(acte.getRecordedAt()).isEqualTo(acte.getOccurredAt());
        }

        @Test
        @DisplayName("l'empreinte posee est bien celle de l'acte")
        void empreinte_recalculable() {
            PlatformAuditEvent acte = service.record(PlatformAuditAction.TENANT_CREATED,
                    "tenant", CLIENT, CLIENT, "Client cree", null);

            assertThat(acte.getIntegrityHash()).isEqualTo(PlatformAuditHasher.hash(acte));
        }

        @Test
        @DisplayName("le premier acte n'a pas de precedent, le suivant chaine sur lui")
        void chainage() {
            PlatformAuditEvent premier = service.record(PlatformAuditAction.TENANT_CREATED,
                    "tenant", CLIENT, CLIENT, "Client cree", null);
            assertThat(premier.getPreviousHash()).isNull();

            when(events.findTopByOrderBySequenceNoDesc()).thenReturn(Optional.of(premier));
            when(counters.lock()).thenReturn(Optional.of(compteur(1L)));

            PlatformAuditEvent second = service.record(PlatformAuditAction.TENANT_UPDATED,
                    "tenant", CLIENT, CLIENT, "Client modifie", null);

            assertThat(second.getPreviousHash()).isEqualTo(premier.getIntegrityHash());
        }

        @Test
        @DisplayName("compteur absent : l'acte est refuse, pas consigne a zero")
        void compteur_absent() {
            when(counters.lock()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.record(PlatformAuditAction.TENANT_CREATED,
                    "tenant", CLIENT, CLIENT, "Client cree", null))
                    .isInstanceOf(PlatformAuditStateException.class)
                    .hasMessageContaining("V12");

            // Recréer le compteur à zéro réattribuerait des numéros déjà
            // employés : rien n'est écrit, et l'acte sensible tombe avec.
            assertThat(enregistres).isEmpty();
        }

        @Test
        @DisplayName("une empreinte ne se repose pas sur un acte deja scelle")
        void sceau_unique() {
            PlatformAuditEvent acte = service.record(PlatformAuditAction.TENANT_CREATED,
                    "tenant", CLIENT, CLIENT, "Client cree", null);

            assertThatThrownBy(() -> acte.seal("b".repeat(64)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("une fois");
        }
    }

    @Nested
    @DisplayName("consultation")
    class Consultation {

        private final Pageable page = PageRequest.of(0, 50);

        @Test
        @DisplayName("un client demande ce qui l'a touche")
        void par_client() {
            when(events.findByTargetTenantIdOrderBySequenceNoDesc(eq(CLIENT), any()))
                    .thenReturn(new PageImpl<>(List.of(acteScelle(1L))));

            assertThat(service.list(null, CLIENT, null, null, null, page)).hasSize(1);
            verify(events).findByTargetTenantIdOrderBySequenceNoDesc(CLIENT, page);
        }

        @Test
        @DisplayName("sans filtre, tout le journal du plus recent au plus ancien")
        void sans_filtre() {
            when(events.findAllByOrderBySequenceNoDesc(any()))
                    .thenReturn(new PageImpl<>(List.of(acteScelle(1L))));

            assertThat(service.list(null, null, null, null, null, page)).hasSize(1);
            verify(events).findAllByOrderBySequenceNoDesc(page);
        }

        @Test
        @DisplayName("par acte")
        void par_action() {
            when(events.findByActionOrderBySequenceNoDesc(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            service.list(PlatformAuditAction.PARTNER_ENGAGEMENT_GRANTED, null, null, null, null, page);

            verify(events).findByActionOrderBySequenceNoDesc(
                    PlatformAuditAction.PARTNER_ENGAGEMENT_GRANTED, page);
        }

        @Test
        @DisplayName("par auteur")
        void par_auteur() {
            when(events.findByActorUserIdOrderBySequenceNoDesc(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            service.list(null, null, ACTEUR, null, null, page);

            verify(events).findByActorUserIdOrderBySequenceNoDesc(ACTEUR, page);
        }

        @Test
        @DisplayName("par periode")
        void par_periode() {
            Instant debut = Instant.parse("2026-11-01T00:00:00Z");
            Instant fin = Instant.parse("2026-11-30T00:00:00Z");
            when(events.findByOccurredAtBetweenOrderBySequenceNoDesc(any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            service.list(null, null, null, debut, fin, page);

            verify(events).findByOccurredAtBetweenOrderBySequenceNoDesc(debut, fin, page);
        }

        @Test
        @DisplayName("journal vide : la derniere sequence est zero, pas une erreur")
        void derniere_sequence_vide() {
            assertThat(service.derniereSequence()).isZero();
        }

        @Test
        @DisplayName("la derniere sequence est celle du dernier acte")
        void derniere_sequence() {
            when(events.findTopByOrderBySequenceNoDesc()).thenReturn(Optional.of(acteScelle(17L)));

            assertThat(service.derniereSequence()).isEqualTo(17L);
        }
    }

    @Nested
    @DisplayName("vérification de chaîne")
    class Verification {

        @Test
        @DisplayName("une chaine intacte est declaree intacte")
        void intacte() {
            when(events.findBySequenceNoBetweenOrderBySequenceNoAsc(1L, 3L))
                    .thenReturn(chaine(3));

            PlatformAuditDto.Verification verdict = service.verifyChain(1L, 3L);

            assertThat(verdict.intact()).isTrue();
            assertThat(verdict.examined()).isEqualTo(3);
            assertThat(verdict.breaks()).isEmpty();
        }

        @Test
        @DisplayName("un acte retouche est denonce")
        void acte_retouche() {
            List<PlatformAuditEvent> chaine = chaine(2);
            // On remplace le deuxième acte par un acte au résumé différent mais
            // qui porte l'empreinte de l'original : exactement ce qu'un
            // UPDATE ciblé produirait si le déclencheur de la V12 n'existait
            // pas.
            PlatformAuditEvent original = chaine.get(1);
            PlatformAuditEvent maquille = new PlatformAuditEvent(
                    original.getId(), original.getSequenceNo(), original.getOccurredAt(),
                    original.getRecordedAt(), original.getActorType(), original.getActorUserId(),
                    original.getAction(), original.getTargetType(), original.getTargetId(),
                    original.getTargetTenantId(), "Rien de notable", null, null, null,
                    original.getPreviousHash());
            maquille.seal(original.getIntegrityHash());
            chaine.set(1, maquille);
            when(events.findBySequenceNoBetweenOrderBySequenceNoAsc(1L, 2L)).thenReturn(chaine);

            PlatformAuditDto.Verification verdict = service.verifyChain(1L, 2L);

            assertThat(verdict.intact()).isFalse();
            assertThat(verdict.breaks()).extracting(PlatformAuditDto.Break::reason)
                    .anyMatch(raison -> raison.contains("Empreinte invalide"));
        }

        @Test
        @DisplayName("un chainage rompu est denonce, meme quand chaque empreinte est valide")
        void chainage_rompu() {
            List<PlatformAuditEvent> chaine = chaine(2);
            PlatformAuditEvent original = chaine.get(1);
            // Même acte, mais chaîné sur une autre empreinte : sa propre
            // empreinte reste recalculable, donc seul le contrôle de chaînage
            // peut l'attraper.
            PlatformAuditEvent decroche = new PlatformAuditEvent(
                    original.getId(), original.getSequenceNo(), original.getOccurredAt(),
                    original.getRecordedAt(), original.getActorType(), original.getActorUserId(),
                    original.getAction(), original.getTargetType(), original.getTargetId(),
                    original.getTargetTenantId(), original.getSummary(), null, null, null,
                    "f".repeat(64));
            decroche.seal(PlatformAuditHasher.hash(decroche));
            chaine.set(1, decroche);
            when(events.findBySequenceNoBetweenOrderBySequenceNoAsc(1L, 2L)).thenReturn(chaine);

            PlatformAuditDto.Verification verdict = service.verifyChain(1L, 2L);

            assertThat(verdict.breaks()).extracting(PlatformAuditDto.Break::reason)
                    .anyMatch(raison -> raison.contains("Chainage rompu"));
        }

        @Test
        @DisplayName("un acte supprime au milieu laisse un trou, qu'aucune empreinte ne montre")
        void trou_de_sequence() {
            List<PlatformAuditEvent> chaine = chaine(3);
            chaine.remove(1);
            when(events.findBySequenceNoBetweenOrderBySequenceNoAsc(1L, 3L)).thenReturn(chaine);

            PlatformAuditDto.Verification verdict = service.verifyChain(1L, 3L);

            assertThat(verdict.intact()).isFalse();
            assertThat(verdict.breaks()).extracting(PlatformAuditDto.Break::reason)
                    .anyMatch(raison -> raison.contains("Trou de sequence"));
        }

        @Test
        @DisplayName("un journal ampute de sa fin reste chaine : seul le comptage le voit")
        void fin_amputee() {
            // C'est le cas que la chaîne seule ne détecte JAMAIS. Supprimer les
            // derniers actes laisse une chaîne parfaite — c'est pourquoi la
            // vérification compare aussi le nombre d'actes à la fenêtre
            // demandée.
            when(events.findBySequenceNoBetweenOrderBySequenceNoAsc(1L, 5L))
                    .thenReturn(chaine(3));

            PlatformAuditDto.Verification verdict = service.verifyChain(1L, 5L);

            assertThat(verdict.intact()).isFalse();
            assertThat(verdict.breaks()).extracting(PlatformAuditDto.Break::reason)
                    .anyMatch(raison -> raison.contains("manquant"));
        }

        @Test
        @DisplayName("une fenetre qui ne commence pas au debut va chercher l'empreinte d'avant")
        void fenetre_decalee() {
            List<PlatformAuditEvent> complete = chaine(3);
            when(events.findBySequenceNoBetweenOrderBySequenceNoAsc(2L, 3L))
                    .thenReturn(new ArrayList<>(complete.subList(1, 3)));
            when(events.findBySequenceNoBetweenOrderBySequenceNoAsc(1L, 1L))
                    .thenReturn(new ArrayList<>(complete.subList(0, 1)));

            PlatformAuditDto.Verification verdict = service.verifyChain(2L, 3L);

            // Sans l'aller-chercher hors fenêtre, le premier acte examiné
            // paraîtrait décroché et toute vérification partielle crierait au
            // loup.
            assertThat(verdict.intact()).isTrue();
        }

        @Test
        @DisplayName("une fenetre absurde est refusee en clair, pas en 500")
        void fenetre_absurde() {
            assertThatThrownBy(() -> service.verifyChain(0L, 5L))
                    .isInstanceOf(PlatformAuditStateException.class)
                    .hasMessageContaining("commence a 1");
            assertThatThrownBy(() -> service.verifyChain(5L, 2L))
                    .isInstanceOf(PlatformAuditStateException.class)
                    .hasMessageContaining("Fenetre vide");
        }
    }

    @Nested
    @DisplayName("export scellé")
    class Export {

        @BeforeEach
        void chaineDeTrois() {
            when(events.findBySequenceNoBetweenOrderBySequenceNoAsc(1L, 3L)).thenReturn(chaine(3));
            when(counters.lock()).thenReturn(Optional.of(compteur(3L)));
        }

        @Test
        @DisplayName("rend la fenetre, son verdict et un sceau")
        void dossier() {
            PlatformAuditDto.Export dossier = service.export(1L, 3L);

            assertThat(dossier.count()).isEqualTo(3);
            assertThat(dossier.events()).hasSize(3);
            assertThat(dossier.verification().intact()).isTrue();
            assertThat(dossier.seal()).hasSize(64).matches("[0-9a-f]{64}");
            assertThat(dossier.generatedAt()).isEqualTo(Instant.parse("2026-11-08T09:30:00.123456Z"));
        }

        @Test
        @DisplayName("le sceau change quand le dossier perd sa derniere ligne")
        void sceau_fixe_le_tout() {
            String avec = service.export(1L, 3L).seal();

            when(events.findBySequenceNoBetweenOrderBySequenceNoAsc(1L, 3L)).thenReturn(chaine(2));
            String sans = service.export(1L, 3L).seal();

            // C'est toute l'utilité du sceau : chaque empreinte individuelle
            // reste valide dans le dossier amputé.
            assertThat(avec).isNotEqualTo(sans);
        }

        @Test
        @DisplayName("l'export est lui-meme consigne")
        void export_consigne() {
            service.export(1L, 3L);

            assertThat(enregistres).hasSize(1);
            assertThat(enregistres.get(0).getAction())
                    .isEqualTo(PlatformAuditAction.AUDIT_EXPORTED);
            assertThat(enregistres.get(0).getSummary()).contains("1 a 3");
        }
    }

    // ------------------------------------------------------------- plomberie

    private static void authentifie(String sujet) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sujet, "n/a", List.of()));
    }

    /**
     * Un compteur dans l'état voulu. L'entité n'expose pas de constructeur :
     * c'est voulu en production (seule la migration la crée) et la réflexion
     * est ici le prix de cette fermeture.
     */
    private static PlatformAuditCounter compteur(long dernier) {
        try {
            PlatformAuditCounter compteur = new PlatformAuditCounter();
            Field scope = PlatformAuditCounter.class.getDeclaredField("scope");
            scope.setAccessible(true);
            scope.set(compteur, PlatformAuditCounter.SCOPE);
            Field sequence = PlatformAuditCounter.class.getDeclaredField("lastSequenceNo");
            sequence.setAccessible(true);
            sequence.set(compteur, dernier);
            return compteur;
        } catch (ReflectiveOperationException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** Une chaîne de {@code combien} actes, correctement scellée et chaînée. */
    private static List<PlatformAuditEvent> chaine(int combien) {
        List<PlatformAuditEvent> chaine = new ArrayList<>();
        String precedente = null;
        for (int rang = 1; rang <= combien; rang++) {
            PlatformAuditEvent acte = new PlatformAuditEvent(
                    UUID.randomUUID(), rang,
                    MAINTENANT.plusSeconds(rang).truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                    MAINTENANT.plusSeconds(rang).truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                    PlatformActorType.USER, ACTEUR,
                    PlatformAuditAction.TENANT_UPDATED, "tenant", CLIENT, CLIENT,
                    "Acte numero " + rang, null, "10.0.0.8", "Chrome", precedente);
            acte.seal(PlatformAuditHasher.hash(acte));
            precedente = acte.getIntegrityHash();
            chaine.add(acte);
        }
        return chaine;
    }

    private static PlatformAuditEvent acteScelle(long sequence) {
        PlatformAuditEvent acte = new PlatformAuditEvent(
                UUID.randomUUID(), sequence, MAINTENANT, MAINTENANT,
                PlatformActorType.USER, ACTEUR, PlatformAuditAction.TENANT_CREATED,
                "tenant", CLIENT, CLIENT, "Client cree", null, null, null, null);
        acte.seal(PlatformAuditHasher.hash(acte));
        return acte;
    }
}
