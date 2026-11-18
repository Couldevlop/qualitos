package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * LE SECOND REGARD sur la clôture d'un écart.
 *
 * <p>Clore, c'est déclarer « plus rien à faire ici » — donc valider que la
 * résolution tient. La même personne pouvait écrire la résolution puis la
 * clôturer dans la minute : le registre déclarait l'écart traité ET vérifié
 * sans qu'aucun contrôle indépendant n'ait eu lieu, et l'organisation n'avait
 * rien à montrer à un auditeur (ISO 9001 §10.2).
 *
 * <p>Deux mécanismes, et ces bancs les séparent :
 * <ul>
 *   <li>le <b>droit</b> de clore — rôles de pilotage, ou personne désignée sur
 *       cet écart ;</li>
 *   <li>la <b>séparation</b> — personne ne valide son propre travail, quel que
 *       soit son rôle.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class NcSecondRegardTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID MARIE = UUID.randomUUID();
    private static final UUID ALI = UUID.randomUUID();

    @Mock NonConformityRepository repo;
    @Mock NcTeamMemberRepository teamMembers;
    @Mock CapaService capaService;
    @Mock com.openlab.qualitos.quality.pdca.PdcaService pdcaService;
    @Mock com.openlab.qualitos.quality.dmaic.DmaicService dmaicService;
    @Mock org.springframework.context.ApplicationEventPublisher events;
    @Mock RecordScope scope;
    @Mock com.openlab.qualitos.quality.nonconformity.subcategory.NcSubcategoryCatalog subcategories;
    @Mock AuthorizationService authz;
    @InjectMocks NcService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ---------- la trace des deux gestes ----------

    /**
     * Sans la trace de QUI a résolu, la règle n'a aucune prise : rien ne
     * distingue la personne qui écrit la résolution de celle qui la valide.
     */
    @Test
    void resolve_traceQuiAResolu() {
        NonConformity n = ecart(NcStatus.ACTION_DEFINED);
        enregistre(n);
        connecte(MARIE, "Marie Durand");

        service.resolve(n.getId(), new NcDto.ResolveRequest("remplacé"));

        assertThat(n.getResolvedById()).isEqualTo(MARIE);
        assertThat(n.getResolvedByName()).isEqualTo("Marie Durand");
    }

    /** La trace du second regard lui-même : c'est elle que l'auditeur vient lire. */
    @Test
    void close_traceQuiAClos() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        n.setResolvedById(MARIE);
        enregistre(n);
        jePuisClore();
        connecte(ALI, "Ali Benali");

        service.close(n.getId());

        assertThat(n.getStatus()).isEqualTo(NcStatus.CLOSED);
        assertThat(n.getClosedById()).isEqualTo(ALI);
        assertThat(n.getClosedByName()).isEqualTo("Ali Benali");
    }

    // ---------- la séparation ----------

    /** Le cœur de la règle : on ne valide pas son propre travail. */
    @Test
    void close_parLaPersonneQuiAResolu_refuse() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        n.setResolvedById(MARIE);
        enregistre(n);
        jePuisClore();
        connecte(MARIE, "Marie Durand");

        assertThatThrownBy(() -> service.close(n.getId()))
                .isInstanceOf(NcStateException.class)
                // Le refus DIT quoi faire. « Accès refusé » laisserait croire à
                // un défaut de droits, alors que l'écart est parfaitement
                // clôturable -- par quelqu'un d'autre.
                .hasMessageContaining("second pair of eyes")
                .hasMessageContaining("cannot close it");
        assertThat(n.getStatus()).isEqualTo(NcStatus.RESOLVED);
    }

    /** Et la même clôture passe dès qu'une autre personne la porte. */
    @Test
    void close_parQuelquUnDAutre_passe() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        n.setResolvedById(MARIE);
        enregistre(n);
        jePuisClore();
        connecte(ALI, "Ali Benali");

        service.close(n.getId());

        assertThat(n.getStatus()).isEqualTo(NcStatus.CLOSED);
    }

    /**
     * La DÉSIGNATION n'exempte pas de la séparation.
     *
     * <p>Le cas se présentera : on désigne le valideur à l'ouverture, et c'est
     * lui qui finit par mener le traitement. Si la désignation l'autorisait à
     * clore ce qu'il a résolu, elle offrirait le moyen exact de contourner la
     * règle qu'elle sert.
     */
    @Test
    void close_parLeValideurDesigneQuiAResolu_refuseQuandMeme() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        n.setResolvedById(MARIE);
        n.setValidatorId(MARIE);
        enregistre(n);
        connecte(MARIE, "Marie Durand");

        assertThatThrownBy(() -> service.close(n.getId()))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("second pair of eyes");
    }

    /**
     * Les écarts d'AVANT la règle n'ont pas de {@code resolvedById} : ils se
     * closent sans obstacle. Les bloquer faute de trace punirait l'organisation
     * pour une donnée que le logiciel ne collectait pas encore.
     */
    @Test
    void close_ecartDAvantLaRegle_passe() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        enregistre(n);
        jePuisClore();
        connecte(MARIE, "Marie Durand");

        service.close(n.getId());

        assertThat(n.getStatus()).isEqualTo(NcStatus.CLOSED);
    }

    // ---------- le droit de clore ----------

    /**
     * Sans rôle ni désignation : 403 MUET. On n'explique pas le modèle de droits
     * à qui n'en a pas, et le refus ne dit rien de l'état de l'écart.
     */
    @Test
    void close_sansDroitNiDesignation_refuse() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        enregistre(n);
        when(authz.has(Permission.NC_CLOSE)).thenReturn(false);
        connecte(ALI, "Ali Benali");

        assertThatThrownBy(() -> service.close(n.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(n.getStatus()).isEqualTo(NcStatus.RESOLVED);
    }

    /**
     * La seconde voie, et la raison d'être de la désignation : une organisation
     * qui ne compte qu'un responsable qualité ne pourrait plus rien clore si le
     * droit s'arrêtait aux rôles de pilotage — c'est lui qui résout, et lui seul
     * qui pourrait clore.
     */
    @Test
    void close_parLeValideurDesigne_passe_memeSansLeRole() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        n.setResolvedById(MARIE);
        n.setValidatorId(ALI);
        n.setValidatorName("Ali Benali");
        enregistre(n);
        // Aucun droit de pilotage : c'est la désignation, et elle seule, qui
        // ouvre la clôture.
        lenient().when(authz.has(Permission.NC_CLOSE)).thenReturn(false);
        connecte(ALI, "Ali Benali");

        service.close(n.getId());

        assertThat(n.getStatus()).isEqualTo(NcStatus.CLOSED);
    }

    /** Être désigné sur un écart n'ouvre rien sur les autres. */
    @Test
    void close_designeSurUnAutreEcart_refuse() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        n.setValidatorId(UUID.randomUUID());
        enregistre(n);
        when(authz.has(Permission.NC_CLOSE)).thenReturn(false);
        connecte(ALI, "Ali Benali");

        assertThatThrownBy(() -> service.close(n.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- le motif, DIT avant le clic ----------

    /**
     * Le bouton s'éteint avec son explication, au lieu de laisser découvrir le
     * refus à qui croyait en avoir fini.
     */
    @Test
    void laFiche_porteLeMotifPourLaPersonneQuiAResolu() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        n.setResolvedById(MARIE);
        enregistre(n);
        when(capaService.casesOf(null, "NC-2026-0001")).thenReturn(List.of());
        when(capaService.unsettledCasesFor(null, "NC-2026-0001")).thenReturn(List.of());
        connecte(MARIE, "Marie Durand");

        NcDto.Response fiche = service.findById(n.getId());

        assertThat(fiche.blockers())
                .extracting(NcDto.Blocker::code)
                .containsExactly(NcBlockerCode.SECOND_LOOK_REQUIRED);
    }

    /**
     * Et le MÊME écart n'en porte aucun pour quelqu'un d'autre : c'est le seul
     * motif qui dépende de qui lit la fiche, parce que l'écart, lui, est
     * clôturable.
     */
    @Test
    void laFiche_nePorteAucunMotifPourQuelquUnDAutre() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        n.setResolvedById(MARIE);
        enregistre(n);
        when(capaService.casesOf(null, "NC-2026-0001")).thenReturn(List.of());
        when(capaService.unsettledCasesFor(null, "NC-2026-0001")).thenReturn(List.of());
        connecte(ALI, "Ali Benali");

        assertThat(service.findById(n.getId()).blockers()).isEmpty();
    }

    // ---------- la désignation elle-même ----------

    /** Le valideur se désigne à l'ouverture, depuis l'annuaire. */
    @Test
    void create_rangeLeValideurDesigne() {
        jePuisClore();
        when(repo.saveAndFlush(org.mockito.ArgumentMatchers.any(NonConformity.class)))
                .thenAnswer(i -> i.getArgument(0));
        connecte(MARIE, "Marie Durand");

        NcDto.Response cree = service.create(new NcDto.CreateRequest(
                "Joint défectueux", "détail", NcCategory.PRODUCT, null, NcSeverity.MAJOR,
                Instant.now(), null, null, null, null, null, NcOrigin.INTERNAL, null,
                null, null, ALI, "Ali Benali", null,
                List.of(new NcDto.TeamMemberRef(MARIE.toString(), "Marie Durand"))));

        assertThat(cree.validatorId()).isEqualTo(ALI);
        assertThat(cree.validatorName()).isEqualTo("Ali Benali");
    }

    // ---------- designer est un acte de gouvernance ----------

    /**
     * ON NE DONNE PAS UN POUVOIR QU'ON N'A PAS.
     *
     * <p>Declarer un ecart est ouvert a tout utilisateur. Sans cette garde,
     * n'importe qui se designait valideur de l'ecart qu'il declare, et gagnait
     * sur lui un droit que le catalogue reserve au pilotage : il suffisait
     * d'attendre qu'un responsable le resolve pour le cloturer soi-meme.
     */
    @Test
    void create_sansLeDroitDeClore_refuseLaDesignation() {
        when(authz.has(Permission.NC_CLOSE)).thenReturn(false);
        connecte(ALI, "Ali Benali");

        assertThatThrownBy(() -> service.create(declaration(ALI, "Ali Benali")))
                .isInstanceOf(AccessDeniedException.class);
    }

    /** Sans designation, la declaration reste ouverte a tous : rien n'y change. */
    @Test
    void create_sansDesignation_resteOuverteATous() {
        when(repo.saveAndFlush(org.mockito.ArgumentMatchers.any(NonConformity.class)))
                .thenAnswer(i -> i.getArgument(0));
        connecte(ALI, "Ali Benali");

        assertThat(service.create(declaration(null, null)).validatorId()).isNull();
        // Le droit de clore n'a meme pas ete demande.
        org.mockito.Mockito.verify(authz, org.mockito.Mockito.never())
                .has(Permission.NC_CLOSE);
    }

    @Test
    void update_sansLeDroitDeClore_refuseLaDesignation() {
        NonConformity n = ecart(NcStatus.OPEN);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(authz.has(Permission.NC_CLOSE)).thenReturn(false);
        connecte(ALI, "Ali Benali");

        assertThatThrownBy(() -> service.update(n.getId(), modificationValideur(ALI, "Ali Benali")))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(n.getValidatorId()).isNull();
    }

    /**
     * ON NE CHOISIT PAS CELUI QUI NOUS RELIT.
     *
     * <p>Apres la resolution, celui qui l'a ecrite y nommerait un complaisant,
     * et le second regard deviendrait un tampon qu'il aurait choisi lui-meme.
     */
    @Test
    void update_parLAuteurDeLaResolution_refuseDeChangerLaDesignation() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        n.setResolvedById(MARIE);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        jePuisClore();
        connecte(MARIE, "Marie Durand");

        assertThatThrownBy(() -> service.update(n.getId(), modificationValideur(ALI, "Ali Benali")))
                .isInstanceOf(NcStateException.class)
                .hasMessageContaining("cannot choose who reviews");
        assertThat(n.getValidatorId()).isNull();
    }

    /**
     * Mais un AUTRE detenteur du droit peut encore designer : c'est ce qui
     * debloque un ecart resolu dont le valideur a quitte l'organisation.
     */
    @Test
    void update_parQuelquUnDAutre_designeEncoreApresLaResolution() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        n.setResolvedById(MARIE);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        lenient().when(repo.save(n)).thenReturn(n);
        jePuisClore();
        connecte(ALI, "Ali Benali");

        service.update(n.getId(), modificationValideur(ALI, "Ali Benali"));

        assertThat(n.getValidatorId()).isEqualTo(ALI);
    }

    /**
     * Une modification qui NE TOUCHE PAS la designation ne reclame pas le droit
     * de clore : corriger la zone d'un ecart resolu reste le geste de qui
     * l'instruit.
     */
    @Test
    void update_quiNeToucheRienALaDesignation_neReclameAucunDroitDeClore() {
        NonConformity n = ecart(NcStatus.RESOLVED);
        n.setResolvedById(MARIE);
        n.setValidatorId(ALI);
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        lenient().when(repo.save(n)).thenReturn(n);
        connecte(MARIE, "Marie Durand");

        // La zone est en 6e position ; la designation en 14e et 15e, intouchee.
        service.update(n.getId(), new NcDto.UpdateRequest(
                null, null, null, null, null, "Atelier 7", null, null, null, null,
                null, null, null, null, null, null, null));

        assertThat(n.getZone()).isEqualTo("Atelier 7");
        assertThat(n.getValidatorId()).isEqualTo(ALI);
        org.mockito.Mockito.verify(authz, org.mockito.Mockito.never())
                .has(Permission.NC_CLOSE);
    }

    // ---------- outils ----------

    private NcDto.CreateRequest declaration(UUID valideur, String nomValideur) {
        return new NcDto.CreateRequest(
                "Joint défectueux", "détail", NcCategory.PRODUCT, null, NcSeverity.MAJOR,
                Instant.now(), null, null, null, null, null, NcOrigin.INTERNAL, null,
                null, null, valideur, nomValideur, null,
                List.of(new NcDto.TeamMemberRef(MARIE.toString(), "Marie Durand")));
    }

    private NcDto.UpdateRequest modificationValideur(UUID valideur, String nomValideur) {
        return new NcDto.UpdateRequest(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, valideur, nomValideur, null, null);
    }

    private void jePuisClore() {
        lenient().when(authz.has(Permission.NC_CLOSE)).thenReturn(true);
    }

    private void enregistre(NonConformity n) {
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        lenient().when(repo.save(n)).thenReturn(n);
    }

    private NonConformity ecart(NcStatus statut) {
        NonConformity n = new NonConformity();
        n.setId(UUID.randomUUID());
        n.setTenantId(TENANT);
        n.setReference("NC-2026-0001");
        n.setTitle("Joint défectueux");
        n.setCategory(NcCategory.PRODUCT);
        n.setSeverity(NcSeverity.MAJOR);
        n.setStatus(statut);
        n.setDetectedAt(Instant.now());
        n.setCreatedAt(Instant.now());
        n.setUpdatedAt(Instant.now());
        return n;
    }

    /** Le sub du jeton, et jamais un identifiant venu du corps de la requête. */
    private static void connecte(UUID sub, String nom) {
        Jwt jeton = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", sub.toString())
                .claim("name", nom)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jeton, List.of(), sub.toString()));
    }
}
