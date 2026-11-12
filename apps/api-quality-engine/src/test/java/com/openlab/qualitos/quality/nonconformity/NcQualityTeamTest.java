package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'équipe qui prend l'écart en charge — la première discipline du 8D.
 *
 * <p>Elle se retapait dans le rapport, alors que la fiche sait depuis le premier
 * jour qui s'en occupe : une discipline qu'on ressaisit est une discipline qu'on
 * oublie. Ce banc tient ce que l'écran et le rapport attendent d'elle :
 * <b>l'ordre de saisie est de l'information</b> (le premier nommé pilote), le
 * <b>nom est figé</b> à la saisie, et une mise à jour qui ne parle pas de
 * l'équipe ne la touche pas.
 */
@ExtendWith(MockitoExtension.class)
class NcQualityTeamTest {

    @Mock NonConformityRepository repo;
    @Mock NcTeamMemberRepository teamMembers;
    @Mock com.openlab.qualitos.quality.capa.CapaService capaService;
    @Mock com.openlab.qualitos.quality.pdca.PdcaService pdcaService;
    @Mock com.openlab.qualitos.quality.dmaic.DmaicService dmaicService;
    @Mock org.springframework.context.ApplicationEventPublisher events;
    @Mock RecordScope scope;
    @Mock com.openlab.qualitos.quality.nonconformity.subcategory.NcSubcategoryCatalog subcategories;
    @InjectMocks NcService service;

    static final UUID TENANT = UUID.randomUUID();

    static final NcDto.TeamMemberRef MARIE = new NcDto.TeamMemberRef("kc-marie", "Marie Durand");
    static final NcDto.TeamMemberRef ALI = new NcDto.TeamMemberRef("kc-ali", "Ali Benali");

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // ---------- à l'ouverture de la fiche ----------

    @Test
    void creation_ecritUneLigneParPersonne_dansLOrdreDeSaisie() {
        creationPasseLaReference();

        service.create(creation(List.of(MARIE, ALI)));

        ArgumentCaptor<NcTeamMember> lignes = ArgumentCaptor.forClass(NcTeamMember.class);
        verify(teamMembers, times(2)).save(lignes.capture());
        assertThat(lignes.getAllValues()).extracting(NcTeamMember::getAccountId)
                .containsExactly("kc-marie", "kc-ali");
        // Le rang porte le pilote : un tri alphabétique le perdrait.
        assertThat(lignes.getAllValues()).extracting(NcTeamMember::getPosition)
                .containsExactly(0, 1);
        assertThat(lignes.getAllValues()).extracting(NcTeamMember::getDisplayName)
                .containsExactly("Marie Durand", "Ali Benali");
    }

    @Test
    void creation_porteLeTenantDuJeton_etLEcartCree() {
        creationPasseLaReference();

        NcDto.Response reponse = service.create(creation(List.of(MARIE)));

        ArgumentCaptor<NcTeamMember> ligne = ArgumentCaptor.forClass(NcTeamMember.class);
        verify(teamMembers).save(ligne.capture());
        assertThat(ligne.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(ligne.getValue().getNcId()).isEqualTo(reponse.id());
    }

    // Un double-clic dans le sélecteur ne doit pas se finir en 400 : la deuxième
    // ligne n'apprendrait rien et la contrainte d'unicité la refuserait.
    @Test
    void creation_ecarteLesDoublons_sansEchouer() {
        creationPasseLaReference();

        service.create(creation(List.of(MARIE, MARIE, ALI)));

        verify(teamMembers, times(2)).save(any());
    }

    // Les espaces d'un copier-coller ne doivent pas créer deux personnes.
    @Test
    void creation_rogneLesEspaces_etIgnoreUnCompteVide() {
        creationPasseLaReference();

        service.create(creation(List.of(
                new NcDto.TeamMemberRef("  kc-marie  ", "  Marie Durand  "),
                new NcDto.TeamMemberRef("   ", "Fantôme"))));

        ArgumentCaptor<NcTeamMember> ligne = ArgumentCaptor.forClass(NcTeamMember.class);
        verify(teamMembers, times(1)).save(ligne.capture());
        assertThat(ligne.getValue().getAccountId()).isEqualTo("kc-marie");
        assertThat(ligne.getValue().getDisplayName()).isEqualTo("Marie Durand");
    }

    // ---------- le remaniement ----------

    @Test
    void miseAJour_remplaceLEquipeEnBloc() {
        NonConformity n = ecart();
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);

        service.update(n.getId(), majEquipe(List.of(ALI)));

        // Remplacement et non fusion : sans la suppression, retirer quelqu'un
        // serait impossible depuis l'écran.
        verify(teamMembers).deleteByTenantIdAndNcId(TENANT, n.getId());
        verify(teamMembers).save(any());
    }

    @Test
    void miseAJour_sansEquipe_neTouchePasALEquipe() {
        NonConformity n = ecart();
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenReturn(n);

        service.update(n.getId(), new NcDto.UpdateRequest("Nouveau titre", null, null, null, null,
                null, null, null, null, null, null, null, null));

        verify(teamMembers, never()).deleteByTenantIdAndNcId(any(), any());
        verify(teamMembers, never()).save(any());
    }

    // ---------- la lecture ----------

    @Test
    void laFiche_renditLEquipeDansSonOrdre() {
        NonConformity n = ecart();
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(teamMembers.findByTenantIdAndNcIdOrderByPositionAsc(TENANT, n.getId()))
                .thenReturn(List.of(membre(n.getId(), "kc-marie", "Marie Durand", 0),
                                    membre(n.getId(), "kc-ali", "Ali Benali", 1)));

        NcDto.Response reponse = service.findById(n.getId());

        assertThat(reponse.teamMembers()).containsExactly(MARIE, ALI);
    }

    @Test
    void laFicheSansEquipe_renditUneListeVide_jamaisNull() {
        NonConformity n = ecart();
        when(repo.findByIdAndTenantId(n.getId(), TENANT)).thenReturn(Optional.of(n));
        when(teamMembers.findByTenantIdAndNcIdOrderByPositionAsc(TENANT, n.getId()))
                .thenReturn(List.of());

        assertThat(service.findById(n.getId()).teamMembers()).isEmpty();
    }

    /**
     * La liste lit TOUTES les équipes en une requête.
     *
     * <p>Une requête par ligne ferait vingt et une requêtes pour une page de
     * vingt écarts, et la liste est l'écran le plus consulté de la plateforme.
     */
    @Test
    void laListe_litLesEquipesEnUneSeuleRequete() {
        NonConformity un = ecart();
        NonConformity deux = ecart();
        Pageable page = PageRequest.of(0, 20);
        Page<NonConformity> resultats = new PageImpl<>(List.of(un, deux), page, 2);
        when(repo.findAll(any(Specification.class), eq(page))).thenReturn(resultats);
        when(teamMembers.findByTenantIdAndNcIdInOrderByPositionAsc(eq(TENANT), any()))
                .thenReturn(List.of(membre(un.getId(), "kc-marie", "Marie Durand", 0),
                                    membre(deux.getId(), "kc-ali", "Ali Benali", 0)));

        Page<NcDto.Response> rendu = service.findAll(null, null, null, null, null, null, page);

        verify(teamMembers, times(1)).findByTenantIdAndNcIdInOrderByPositionAsc(eq(TENANT), any());
        verify(teamMembers, never()).findByTenantIdAndNcIdOrderByPositionAsc(any(), any());
        assertThat(rendu.getContent().get(0).teamMembers()).containsExactly(MARIE);
        assertThat(rendu.getContent().get(1).teamMembers()).containsExactly(ALI);
    }

    @Test
    void laListeVide_nInterrogePasLesEquipes() {
        Pageable page = PageRequest.of(0, 20);
        when(repo.findAll(any(Specification.class), eq(page)))
                .thenReturn(new PageImpl<>(List.of(), page, 0));

        service.findAll(null, null, null, null, null, null, page);

        verify(teamMembers, never()).findByTenantIdAndNcIdInOrderByPositionAsc(any(), any());
    }

    // ---------- harnais ----------

    private void creationPasseLaReference() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> {
            NonConformity n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
    }

    private NcDto.CreateRequest creation(List<NcDto.TeamMemberRef> equipe) {
        return new NcDto.CreateRequest(
                "Joint hors tolérance", "Relevé au poste 12", NcCategory.PRODUCT, null,
                NcSeverity.MAJOR, Instant.now(), "Atelier 3", null, null, null, null, null, null,
                null, equipe);
    }

    private NcDto.UpdateRequest majEquipe(List<NcDto.TeamMemberRef> equipe) {
        return new NcDto.UpdateRequest(null, null, null, null, null, null, null, null, null, null,
                null, null, equipe);
    }

    private NonConformity ecart() {
        NonConformity n = new NonConformity();
        n.setId(UUID.randomUUID());
        n.setTenantId(TENANT);
        n.setReference("NC-2026-0001");
        n.setTitle("Joint hors tolérance");
        n.setCategory(NcCategory.PRODUCT);
        n.setSeverity(NcSeverity.MAJOR);
        n.setStatus(NcStatus.OPEN);
        n.setOrigin(NcOrigin.INTERNAL);
        n.setDetectedAt(Instant.now());
        return n;
    }

    private NcTeamMember membre(UUID ncId, String compte, String nom, int rang) {
        NcTeamMember m = new NcTeamMember();
        m.setId(UUID.randomUUID());
        m.setTenantId(TENANT);
        m.setNcId(ncId);
        m.setAccountId(compte);
        m.setDisplayName(nom);
        m.setPosition(rang);
        return m;
    }
}
