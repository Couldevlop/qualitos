package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.dmaic.DmaicProject;
import com.openlab.qualitos.quality.dmaic.DmaicProjectRepository;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionPorts;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ActionLine;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ActionStatus;
import com.openlab.qualitos.quality.dmaic.resolution.domain.Person;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionContent;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionNotFoundException;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSettings;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSheet;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetPhoto;
import com.openlab.qualitos.quality.dmaic.resolution.domain.WhyBranch;
import com.openlab.qualitos.quality.dmaic.resolution.domain.WhyChain;
import com.openlab.qualitos.quality.ishikawa.IshikawaDiagramRepository;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les adaptateurs sur un vrai mapping JPA (contexte complet, H2 en mode
 * PostgreSQL) : fiche et paramétrage aller-retour en JSON, photos par
 * emplacement, et la frontière entre clients à chaque lecture.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
@Transactional
class ResolutionAdaptersTest {

    @Autowired ResolutionSheetJpaRepository sheetJpa;
    @Autowired ResolutionSettingsJpaRepository settingsJpa;
    @Autowired ResolutionPhotoJpaRepository photoJpa;
    @Autowired DmaicProjectRepository projectJpa;
    @Autowired IshikawaDiagramRepository ishikawaJpa;
    @Autowired ObjectMapper objectMapper;

    ResolutionAdapters.Json json;
    UUID tenant;
    UUID projet;

    @BeforeEach
    void setup() {
        json = new ResolutionAdapters.Json(objectMapper);
        tenant = UUID.randomUUID();
        DmaicProject p = new DmaicProject();
        p.setTenantId(tenant);
        p.setTitle("Fissures carter");
        p.setProblemStatement("Carters fissurés");
        p.setBlackBeltId(UUID.randomUUID());
        projet = projectJpa.save(p).getId();
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    static ResolutionContent contenu() {
        Person alice = new Person(UUID.randomUUID(), "Alice Martin");
        return new ResolutionContent("Acme", "CL-7", alice, List.of(new Person(UUID.randomUUID(), "Bruno")),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), 2, "V10", "Fissure",
                Map.of("DEF-04", "YES", "DEF-01", "Rayure"), "Mesures", "Tri", "Outil", "Nouvel outil",
                "Contrôle", List.of(new WhyChain(WhyBranch.DETECTION, "Contrôle absent", List.of("Pas prévu"),
                        true, "Ajouter le contrôle")),
                List.of(new ActionLine(LocalDate.of(2026, 10, 2), "Changer", alice, ActionStatus.IN_REVIEW,
                        LocalDate.of(2026, 10, 9), null, 70, "Exemple terminé à 70 %")),
                UUID.randomUUID());
    }

    @Test
    void uneFicheSEnregistreEtSeRelitAChampsEgaux() {
        ResolutionAdapters.Sheets sheets = new ResolutionAdapters.Sheets(sheetJpa, json);
        Instant t0 = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        ResolutionContent contenu = contenu();
        ResolutionSheet neuve = new ResolutionSheet(null, tenant, projet, contenu, UUID.randomUUID(), t0, t0);

        ResolutionSheet saved = sheets.save(neuve);

        assertThat(saved.getId()).isNotNull();
        ResolutionSheet relue = sheets.findByProject(tenant, projet).orElseThrow();
        assertThat(relue.getContent()).isEqualTo(contenu);
        assertThat(relue.getContent().actions().get(0).status()).isEqualTo(ActionStatus.IN_REVIEW);
        assertThat(relue.getContent().whys().get(0).branch()).isEqualTo(WhyBranch.DETECTION);
        assertThat(sheets.findByProject(UUID.randomUUID(), projet)).isEmpty();

        ResolutionSheet modifiee = new ResolutionSheet(saved.getId(), tenant, projet, ResolutionContent.empty(),
                null, t0, t0.plusSeconds(5));
        assertThat(sheets.save(modifiee).getContent()).isEqualTo(ResolutionContent.empty());
        assertThat(sheetJpa.count()).isEqualTo(1);
    }

    @Test
    void uneFicheDUnAutreClientNeSEcritPas() {
        ResolutionAdapters.Sheets sheets = new ResolutionAdapters.Sheets(sheetJpa, json);
        Instant t0 = Instant.now();
        ResolutionSheet saved = sheets.save(new ResolutionSheet(null, tenant, projet, contenu(), null, t0, t0));

        ResolutionSheet usurpee = new ResolutionSheet(saved.getId(), UUID.randomUUID(), projet, contenu(), null, t0, t0);
        assertThatThrownBy(() -> sheets.save(usurpee)).isInstanceOf(ResolutionNotFoundException.class);
    }

    @Test
    void leParametrageSEnregistreSeRelitEtSeRetire() {
        ResolutionAdapters.Settings settings = new ResolutionAdapters.Settings(settingsJpa, json);
        ResolutionSettings d = ResolutionSettings.platformDefaults();
        assertThat(settings.find(tenant)).isEmpty();

        settings.save(tenant, d, UUID.randomUUID(), Instant.now());
        settings.save(tenant, d, UUID.randomUUID(), Instant.now());

        ResolutionSettings relu = settings.find(tenant).orElseThrow();
        assertThat(relu.defaults()).isFalse();
        assertThat(relu.questions()).isEqualTo(d.questions());
        assertThat(relu.failureTypes()).isEqualTo(d.failureTypes());
        assertThat(relu.severities()).isEqualTo(d.severities());
        assertThat(settingsJpa.count()).isEqualTo(1);
        assertThat(settings.find(UUID.randomUUID())).isEmpty();

        settings.reset(tenant);
        assertThat(settings.find(tenant)).isEmpty();
    }

    @Test
    void lesPhotosSeRangentParEmplacement() {
        ResolutionAdapters.Photos photos = new ResolutionAdapters.Photos(photoJpa);
        SheetPhoto p3 = photos.save(photo(3, "k3"));
        photos.save(photo(1, "k1"));

        assertThat(photos.list(tenant, projet)).extracting(SheetPhoto::slot).containsExactly(1, 3);
        assertThat(photos.find(tenant, projet, 3)).get().extracting(SheetPhoto::objectKey).isEqualTo("k3");
        assertThat(photos.find(UUID.randomUUID(), projet, 3)).isEmpty();

        SheetPhoto commentee = photos.save(p3.withRemark("Vue de dessus"));
        assertThat(commentee.remark()).isEqualTo("Vue de dessus");
        assertThat(commentee.objectKey()).isEqualTo("k3");

        assertThatThrownBy(() -> photos.save(new SheetPhoto(p3.id(), UUID.randomUUID(), projet, 3, "k3",
                "image/png", 1, null, null, Instant.now()))).isInstanceOf(ResolutionNotFoundException.class);

        photos.delete(new SheetPhoto(p3.id(), UUID.randomUUID(), projet, 3, "k3", "image/png", 1, null, null,
                Instant.now()));
        assertThat(photos.list(tenant, projet)).hasSize(2);
        photos.delete(p3);
        assertThat(photos.list(tenant, projet)).hasSize(1);

        ResolutionPhotoObjectOwner owner = new ResolutionPhotoObjectOwner(photoJpa);
        assertThat(owner.isReferenced("k1")).isTrue();
        assertThat(owner.isReferenced("k3")).isFalse();
        assertThat(owner.ownerName()).isEqualTo("dmaic-resolution-photo");
    }

    SheetPhoto photo(int slot, String key) {
        return new SheetPhoto(null, tenant, projet, slot, key, "image/png", 42, null, UUID.randomUUID(),
                Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void leProjetEtLIshikawaSeLisentDansLeTenant() {
        ResolutionAdapters.Projects projects = new ResolutionAdapters.Projects(projectJpa);
        assertThat(projects.find(tenant, projet)).get()
                .isEqualTo(new ResolutionPorts.ProjectHeader(projet, "Fissures carter", "Carters fissurés"));
        assertThat(projects.find(UUID.randomUUID(), projet)).isEmpty();

        ResolutionAdapters.Ishikawas ishikawas = new ResolutionAdapters.Ishikawas(ishikawaJpa);
        assertThat(ishikawas.exists(tenant, UUID.randomUUID())).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void leStockageEteintRepond503() throws Exception {
        ObjectProvider<ObjectStorage> vide = mock(ObjectProvider.class);
        ResolutionAdapters.Storage eteint = new ResolutionAdapters.Storage(vide);
        assertThat(eteint.available()).isFalse();
        assertThatThrownBy(() -> eteint.put("k", "image/png", new byte[1])).isInstanceOf(StorageDisabledException.class);
        assertThatThrownBy(() -> eteint.presignGet("k")).isInstanceOf(StorageDisabledException.class);
        assertThatThrownBy(() -> eteint.delete("k")).isInstanceOf(StorageDisabledException.class);

        ObjectStorage s3 = mock(ObjectStorage.class);
        ObjectProvider<ObjectStorage> plein = mock(ObjectProvider.class);
        when(plein.getIfAvailable()).thenReturn(s3);
        when(s3.presignGet(eq("k"), any(Duration.class))).thenReturn(URI.create("https://s3.test/k").toURL());
        ResolutionAdapters.Storage allume = new ResolutionAdapters.Storage(plein);
        assertThat(allume.available()).isTrue();
        allume.put("k", "image/png", new byte[1]);
        allume.delete("k");
        assertThat(allume.presignGet("k").toString()).isEqualTo("https://s3.test/k");
        verify(s3).put(eq("k"), eq("image/png"), any(byte[].class));
        verify(s3).delete("k");
        verify(s3).presignGet("k", ResolutionAdapters.Storage.PRESIGN_TTL);
    }

    @Test
    void leContexteVientDuJeton() {
        ResolutionAdapters.JwtContext context = new ResolutionAdapters.JwtContext();
        assertThatThrownBy(context::requireTenantId).isInstanceOf(MissingTenantContextException.class);
        TenantContext.setTenantId(tenant.toString());
        assertThat(context.requireTenantId()).isEqualTo(tenant);
    }

    @Test
    void unJsonIlisibleEstUneErreurFranche() {
        assertThatThrownBy(() -> json.read("{", ResolutionContent.class)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> json.read("[", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}))
                .isInstanceOf(IllegalStateException.class);
        ResolutionAdapters.Json casse = new ResolutionAdapters.Json(new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
                throw new com.fasterxml.jackson.core.JsonGenerationException("panne", (com.fasterxml.jackson.core.JsonGenerator) null);
            }
        });
        assertThatThrownBy(() -> casse.write(Map.of())).isInstanceOf(IllegalStateException.class);
    }
}
