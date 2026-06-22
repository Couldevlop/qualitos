package com.openlab.qualitos.quality.academy.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.academy.domain.*;
import com.openlab.qualitos.quality.academy.infrastructure.*;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademyExportServiceTest {

    @Mock AcademyCourseRepository courses;
    @Mock AcademyModuleRepository modules;
    @Mock LessonRepository lessons;
    @Mock AcademyEnrollmentRepository enrollments;
    AcademyExportService service;
    final ObjectMapper mapper = new ObjectMapper();

    static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000ee");

    @BeforeEach
    void setup() {
        service = new AcademyExportService(courses, modules, lessons, enrollments, mapper);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void clear() { TenantContext.clear(); }

    private AcademyCourse course() {
        AcademyCourse c = new AcademyCourse();
        c.setId(UUID.randomUUID());
        c.setTenantId(TENANT);
        c.setCode("c1");
        c.setTitle("Cours 1");
        return c;
    }

    @Test
    void exportScorm_producesZipWithManifestAndLessonPages() throws Exception {
        AcademyCourse c = course();
        AcademyModule m = new AcademyModule();
        m.setId(UUID.randomUUID());
        m.setTenantId(TENANT);
        m.setCourseId(c.getId());
        m.setTitle("Module 1");
        Lesson l = new Lesson();
        l.setId(UUID.randomUUID());
        l.setTenantId(TENANT);
        l.setModuleId(m.getId());
        l.setTitle("Leçon 1");
        l.setContentType(LessonContentType.TEXT);
        l.setBody("Contenu pédagogique");

        when(courses.findById(c.getId())).thenReturn(Optional.of(c));
        when(modules.findByCourseIdOrderByOrderIndexAsc(c.getId())).thenReturn(List.of(m));
        when(lessons.findByModuleIdOrderByOrderIndexAsc(m.getId())).thenReturn(List.of(l));

        byte[] zip = service.exportScorm(c.getId());
        Map<String, String> entries = unzip(zip);

        assertThat(entries).containsKey("imsmanifest.xml");
        assertThat(entries).containsKey("content/lesson-1.html");
        assertThat(entries.get("imsmanifest.xml")).contains("2004 4th Edition").contains("Cours 1");
        assertThat(entries.get("content/lesson-1.html")).contains("Leçon 1").contains("Contenu pédagogique");
    }

    @Test
    void exportXapi_completedEnrollment_buildsPassedStatements() throws Exception {
        AcademyCourse c = course();
        AcademyEnrollment e = new AcademyEnrollment();
        e.setId(UUID.randomUUID());
        e.setTenantId(TENANT);
        e.setUserId(UUID.randomUUID());
        e.setCourseId(c.getId());
        e.setStatus(AcademyEnrollmentStatus.COMPLETED);
        e.setFinalScore(90);
        e.setCompletedAt(Instant.parse("2026-06-22T10:00:00Z"));

        when(enrollments.findByTenantIdAndId(TENANT, e.getId())).thenReturn(Optional.of(e));
        when(courses.findById(c.getId())).thenReturn(Optional.of(c));

        String json = service.exportXapi(e.getId());
        JsonNode arr = mapper.readTree(json);
        assertThat(arr).hasSize(2);
        assertThat(arr.get(0).path("verb").path("id").asText()).contains("passed");
        assertThat(arr.get(0).path("result").path("score").path("raw").asInt()).isEqualTo(90);
    }

    @Test
    void exportXapi_unfinishedEnrollment_state409() {
        AcademyEnrollment e = new AcademyEnrollment();
        e.setId(UUID.randomUUID());
        e.setTenantId(TENANT);
        e.setStatus(AcademyEnrollmentStatus.IN_PROGRESS);
        when(enrollments.findByTenantIdAndId(TENANT, e.getId())).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.exportXapi(e.getId()))
                .isInstanceOf(AcademyStateException.class);
    }

    @Test
    void exportScorm_otherTenant_notFound() {
        AcademyCourse c = course();
        c.setTenantId(UUID.randomUUID());
        when(courses.findById(c.getId())).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.exportScorm(c.getId()))
                .isInstanceOf(AcademyNotFoundException.class);
    }

    private Map<String, String> unzip(byte[] zip) throws Exception {
        Map<String, String> out = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                out.put(entry.getName(), new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return out;
    }
}
