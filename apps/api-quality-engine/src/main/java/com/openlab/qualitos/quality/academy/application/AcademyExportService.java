package com.openlab.qualitos.quality.academy.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.academy.domain.*;
import com.openlab.qualitos.quality.academy.infrastructure.*;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Export du parcours et de la complétion vers un LMS externe (§19.3) :
 * <ul>
 *   <li><b>SCORM 2004</b> : package ZIP {@code imsmanifest.xml} + une page HTML
 *       par leçon, importable dans Moodle/TalentLMS/etc.</li>
 *   <li><b>xAPI</b> : statements JSON (verbes passed/completed) d'une complétion,
 *       ingérables par un LRS.</li>
 * </ul>
 *
 * <p>Multi-tenant strict : tout est filtré par {@code tenantId} du JWT.</p>
 */
@Service
public class AcademyExportService {

    private final AcademyCourseRepository courses;
    private final AcademyModuleRepository modules;
    private final LessonRepository lessons;
    private final AcademyEnrollmentRepository enrollments;
    private final ObjectMapper objectMapper;
    private final XapiStatementBuilder xapi;

    public AcademyExportService(AcademyCourseRepository courses,
                                AcademyModuleRepository modules,
                                LessonRepository lessons,
                                AcademyEnrollmentRepository enrollments,
                                ObjectMapper objectMapper) {
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.enrollments = enrollments;
        this.objectMapper = objectMapper;
        this.xapi = new XapiStatementBuilder(objectMapper);
    }

    /** Construit le ZIP SCORM 2004 du cours (manifeste + une page HTML par leçon). */
    @Transactional(readOnly = true)
    public byte[] exportScorm(UUID courseId) {
        AcademyCourse course = loadCourse(courseId);
        List<AcademyModule> mods = modules.findByCourseIdOrderByOrderIndexAsc(course.getId());

        List<ScormManifestBuilder.Item> items = new ArrayList<>();
        List<LessonPage> pages = new ArrayList<>();
        int seq = 1;
        for (AcademyModule m : mods) {
            for (Lesson l : lessons.findByModuleIdOrderByOrderIndexAsc(m.getId())) {
                String id = "L" + seq;
                String href = "content/lesson-" + seq + ".html";
                items.add(new ScormManifestBuilder.Item(id, l.getTitle(), href));
                pages.add(new LessonPage(href, renderLessonHtml(m, l)));
                seq++;
            }
        }
        String manifest = ScormManifestBuilder.build(course.getCode(), course.getTitle(), items);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bos)) {
            writeEntry(zip, "imsmanifest.xml", manifest);
            for (LessonPage p : pages) {
                writeEntry(zip, p.href(), p.html());
            }
            zip.finish();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot build SCORM package", e);
        }
    }

    /** Statements xAPI de la complétion d'une inscription (JSON). 409 si non terminée. */
    @Transactional(readOnly = true)
    public String exportXapi(UUID enrollmentId) {
        UUID tenantId = requireTenantId();
        AcademyEnrollment e = enrollments.findByTenantIdAndId(tenantId, enrollmentId)
                .orElseThrow(() -> new AcademyNotFoundException("Enrollment", enrollmentId));
        if (e.getStatus() != AcademyEnrollmentStatus.COMPLETED
                && e.getStatus() != AcademyEnrollmentStatus.FAILED) {
            throw new AcademyStateException(
                    "xAPI export requires a finished enrollment (COMPLETED or FAILED), got " + e.getStatus());
        }
        AcademyCourse course = courses.findById(e.getCourseId())
                .orElseThrow(() -> new AcademyNotFoundException("Course", e.getCourseId()));
        int score = e.getFinalScore() == null ? 0 : e.getFinalScore();
        boolean passed = e.getStatus() == AcademyEnrollmentStatus.COMPLETED;
        return xapi.completionStatements(e.getUserId(), course.getCode(), course.getTitle(),
                score, passed, e.getCompletedAt() == null ? e.getUpdatedAt() : e.getCompletedAt());
    }

    private record LessonPage(String href, String html) {}

    private String renderLessonHtml(AcademyModule m, Lesson l) {
        StringBuilder b = new StringBuilder(1024);
        b.append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"><title>")
         .append(esc(l.getTitle())).append("</title></head><body>");
        b.append("<h2>").append(esc(m.getTitle())).append("</h2>");
        b.append("<h3>").append(esc(l.getTitle())).append("</h3>");
        if (l.getContentType() == LessonContentType.VIDEO && l.getMediaUrl() != null) {
            b.append("<p><a href=\"").append(esc(l.getMediaUrl())).append("\">Vidéo</a></p>");
        } else if (l.getContentType() == LessonContentType.EXTERNAL && l.getMediaUrl() != null) {
            b.append("<p><a href=\"").append(esc(l.getMediaUrl())).append("\">Ressource externe</a></p>");
        }
        if (l.getBody() != null && !l.getBody().isBlank()) {
            b.append("<div>").append(esc(l.getBody())).append("</div>");
        }
        b.append("</body></html>");
        return b.toString();
    }

    private void writeEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private AcademyCourse loadCourse(UUID id) {
        UUID tenantId = requireTenantId();
        AcademyCourse c = courses.findById(id).orElseThrow(() -> new AcademyNotFoundException("Course", id));
        if (!c.getTenantId().equals(tenantId)) throw new AcademyNotFoundException("Course", id);
        return c;
    }

    private String esc(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#x27;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
