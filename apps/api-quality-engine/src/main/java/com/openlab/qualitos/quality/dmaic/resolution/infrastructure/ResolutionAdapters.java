package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.dmaic.DmaicProjectRepository;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionPorts;
import com.openlab.qualitos.quality.dmaic.resolution.domain.FailureType;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionContent;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionNotFoundException;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSettings;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSheet;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SeverityLevel;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetPhoto;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetQuestion;
import com.openlab.qualitos.quality.ishikawa.IshikawaDiagramRepository;
import com.openlab.qualitos.quality.nonconformity.storage.ObjectStorage;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.springframework.beans.factory.ObjectProvider;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les adaptateurs des ports de la fiche de résolution. Des classes simples,
 * déclarées comme beans par {@link ResolutionBeanConfiguration} : testables
 * sans Spring.
 */
final class ResolutionAdapters {

    private ResolutionAdapters() {}

    /** Le tenant et l'acteur viennent du JETON validé, jamais du corps (§18.2). */
    static final class JwtContext implements ResolutionPorts.Context {
        @Override
        public UUID requireTenantId() {
            if (!TenantContext.hasTenant()) {
                throw new MissingTenantContextException();
            }
            return UUID.fromString(TenantContext.getTenantId());
        }

        @Override
        public UUID requireActorId() {
            return CurrentUser.requireUserId();
        }
    }

    /** Lecture et écriture des documents JSON de la fiche et du paramétrage. */
    static final class Json {
        private final ObjectMapper mapper;

        Json(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        String write(Object value) {
            try {
                return mapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("DMAIC resolution data could not be serialized", e);
            }
        }

        <T> T read(String text, TypeReference<T> type) {
            try {
                return mapper.readValue(text, type);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("DMAIC resolution data could not be read", e);
            }
        }

        <T> T read(String text, Class<T> type) {
            try {
                return mapper.readValue(text, type);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("DMAIC resolution data could not be read", e);
            }
        }
    }

    static final class Settings implements ResolutionPorts.Settings {
        private static final TypeReference<List<SheetQuestion>> QUESTIONS = new TypeReference<>() {};
        private static final TypeReference<List<FailureType>> FAILURES = new TypeReference<>() {};
        private static final TypeReference<List<SeverityLevel>> SEVERITIES = new TypeReference<>() {};

        private final ResolutionSettingsJpaRepository jpa;
        private final Json json;

        Settings(ResolutionSettingsJpaRepository jpa, Json json) {
            this.jpa = jpa;
            this.json = json;
        }

        @Override
        public Optional<ResolutionSettings> find(UUID tenantId) {
            return jpa.findByTenantId(tenantId).map(e -> new ResolutionSettings(
                    json.read(e.getQuestions(), QUESTIONS), json.read(e.getFailureTypes(), FAILURES),
                    json.read(e.getSeverities(), SEVERITIES), false));
        }

        @Override
        public void save(UUID tenantId, ResolutionSettings settings, UUID actor, Instant now) {
            ResolutionSettingsJpaEntity e = jpa.findByTenantId(tenantId).orElseGet(() -> {
                ResolutionSettingsJpaEntity neuf = new ResolutionSettingsJpaEntity();
                neuf.setTenantId(tenantId);
                return neuf;
            });
            e.setQuestions(json.write(settings.questions()));
            e.setFailureTypes(json.write(settings.failureTypes()));
            e.setSeverities(json.write(settings.severities()));
            e.setUpdatedBy(actor);
            e.setUpdatedAt(now);
            jpa.save(e);
        }

        @Override
        public void reset(UUID tenantId) {
            jpa.deleteByTenantId(tenantId);
        }
    }

    static final class Sheets implements ResolutionPorts.Sheets {
        private final ResolutionSheetJpaRepository jpa;
        private final Json json;

        Sheets(ResolutionSheetJpaRepository jpa, Json json) {
            this.jpa = jpa;
            this.json = json;
        }

        @Override
        public Optional<ResolutionSheet> findByProject(UUID tenantId, UUID projectId) {
            return jpa.findByTenantIdAndProjectId(tenantId, projectId).map(this::toDomain);
        }

        /**
         * Une fiche déjà identifiée est relue dans SON tenant avant d'être
         * écrite : disparue entre-temps, elle rend 404 plutôt que de renaître.
         */
        @Override
        public ResolutionSheet save(ResolutionSheet sheet) {
            ResolutionSheetJpaEntity e;
            if (sheet.getId() == null) {
                e = new ResolutionSheetJpaEntity();
                e.setTenantId(sheet.getTenantId());
                e.setProjectId(sheet.getProjectId());
                e.setCreatedBy(sheet.getCreatedBy());
                e.setCreatedAt(sheet.getCreatedAt());
            } else {
                e = jpa.findById(sheet.getId())
                        .filter(found -> found.getTenantId().equals(sheet.getTenantId()))
                        .orElseThrow(() -> new ResolutionNotFoundException("Resolution sheet not found"));
            }
            e.setContent(json.write(sheet.getContent()));
            e.setUpdatedAt(sheet.getUpdatedAt());
            return toDomain(jpa.save(e));
        }

        ResolutionSheet toDomain(ResolutionSheetJpaEntity e) {
            return new ResolutionSheet(e.getId(), e.getTenantId(), e.getProjectId(),
                    json.read(e.getContent(), ResolutionContent.class), e.getCreatedBy(), e.getCreatedAt(),
                    e.getUpdatedAt());
        }
    }

    static final class Photos implements ResolutionPorts.Photos {
        private final ResolutionPhotoJpaRepository jpa;

        Photos(ResolutionPhotoJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public List<SheetPhoto> list(UUID tenantId, UUID projectId) {
            return jpa.findByTenantIdAndProjectIdOrderBySlotAsc(tenantId, projectId).stream()
                    .map(Photos::toDomain).toList();
        }

        @Override
        public Optional<SheetPhoto> find(UUID tenantId, UUID projectId, int slot) {
            return jpa.findByTenantIdAndProjectIdAndSlot(tenantId, projectId, slot).map(Photos::toDomain);
        }

        @Override
        public SheetPhoto save(SheetPhoto p) {
            ResolutionPhotoJpaEntity e = p.id() == null
                    ? new ResolutionPhotoJpaEntity()
                    : jpa.findById(p.id()).filter(found -> found.getTenantId().equals(p.tenantId()))
                            .orElseThrow(() -> new ResolutionNotFoundException("Photo not found"));
            if (p.id() == null) {
                e.setTenantId(p.tenantId());
                e.setProjectId(p.projectId());
                e.setSlot(p.slot());
                e.setObjectKey(p.objectKey());
                e.setContentType(p.contentType());
                e.setSizeBytes(p.sizeBytes());
                e.setCreatedBy(p.createdBy());
                e.setCreatedAt(p.createdAt());
            }
            e.setRemark(p.remark());
            return toDomain(jpa.saveAndFlush(e));
        }

        /**
         * Écrit tout de suite : remplacer la photo d'un emplacement supprime puis
         * insère sur la même clé d'unicité, et l'ordre des écritures doit suivre.
         */
        @Override
        public void delete(SheetPhoto p) {
            jpa.findById(p.id()).filter(found -> found.getTenantId().equals(p.tenantId()))
                    .ifPresent(e -> {
                        jpa.delete(e);
                        jpa.flush();
                    });
        }

        static SheetPhoto toDomain(ResolutionPhotoJpaEntity e) {
            return new SheetPhoto(e.getId(), e.getTenantId(), e.getProjectId(), e.getSlot(), e.getObjectKey(),
                    e.getContentType(), e.getSizeBytes(), e.getRemark(), e.getCreatedBy(), e.getCreatedAt());
        }
    }

    /** Le stockage objet de la plateforme ; éteint, il répond 503 à l'écriture. */
    static final class Storage implements ResolutionPorts.PhotoStorage {
        /** Durée de vie des adresses de lecture des photos. */
        static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

        private final ObjectProvider<ObjectStorage> provider;

        Storage(ObjectProvider<ObjectStorage> provider) {
            this.provider = provider;
        }

        @Override
        public boolean available() {
            return provider.getIfAvailable() != null;
        }

        @Override
        public void put(String key, String contentType, byte[] content) {
            require().put(key, contentType, content);
        }

        @Override
        public URL presignGet(String key) {
            return require().presignGet(key, PRESIGN_TTL);
        }

        @Override
        public void delete(String key) {
            require().delete(key);
        }

        private ObjectStorage require() {
            ObjectStorage storage = provider.getIfAvailable();
            if (storage == null) {
                throw new StorageDisabledException();
            }
            return storage;
        }
    }

    static final class Projects implements ResolutionPorts.Projects {
        private final DmaicProjectRepository projects;

        Projects(DmaicProjectRepository projects) {
            this.projects = projects;
        }

        @Override
        public Optional<ResolutionPorts.ProjectHeader> find(UUID tenantId, UUID projectId) {
            return projects.findByIdAndTenantId(projectId, tenantId)
                    .map(p -> new ResolutionPorts.ProjectHeader(p.getId(), p.getTitle(), p.getProblemStatement()));
        }
    }

    static final class Ishikawas implements ResolutionPorts.Ishikawas {
        private final IshikawaDiagramRepository diagrams;

        Ishikawas(IshikawaDiagramRepository diagrams) {
            this.diagrams = diagrams;
        }

        @Override
        public boolean exists(UUID tenantId, UUID ishikawaId) {
            return diagrams.findByIdAndTenantId(ishikawaId, tenantId).isPresent();
        }
    }
}
