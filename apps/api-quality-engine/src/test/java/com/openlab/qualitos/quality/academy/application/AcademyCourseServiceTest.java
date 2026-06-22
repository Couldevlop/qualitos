package com.openlab.qualitos.quality.academy.application;

import com.openlab.qualitos.quality.academy.domain.*;
import com.openlab.qualitos.quality.academy.infrastructure.*;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademyCourseServiceTest {

    @Mock AcademyCourseRepository courses;
    @Mock AcademyModuleRepository modules;
    @Mock LessonRepository lessons;
    @Mock QuizRepository quizzes;
    @Mock QuizQuestionRepository questions;
    AcademyCourseService service;

    static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    static final UUID ACTOR = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new AcademyCourseService(courses, modules, lessons, quizzes, questions);
        TenantContext.setTenantId(TENANT.toString());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ACTOR.toString(), "n/a", List.of()));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCourse_setsTenantAndDefaults() {
        when(courses.findByTenantIdAndCode(TENANT, "c1")).thenReturn(Optional.empty());
        when(courses.save(any(AcademyCourse.class))).thenAnswer(inv -> {
            AcademyCourse c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        AcademyDto.CourseResponse r = service.createCourse(new AcademyDto.CreateCourseRequest(
                "c1", "Cours 1", "desc", "auditor", "manufacturing", null, null, 12));

        assertThat(r.tenantId()).isEqualTo(TENANT);
        assertThat(r.createdBy()).isEqualTo(ACTOR);
        assertThat(r.passingScore()).isEqualTo(70);
        assertThat(r.pointsReward()).isEqualTo(50);
        assertThat(r.status()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void createCourse_duplicateCode_conflict() {
        when(courses.findByTenantIdAndCode(TENANT, "c1")).thenReturn(Optional.of(new AcademyCourse()));
        assertThatThrownBy(() -> service.createCourse(new AcademyDto.CreateCourseRequest(
                "c1", "x", null, null, null, null, null, null)))
                .isInstanceOf(AcademyConflictException.class);
    }

    @Test
    void getCourse_otherTenant_notFound() {
        AcademyCourse c = new AcademyCourse();
        c.setId(UUID.randomUUID());
        c.setTenantId(UUID.randomUUID()); // autre tenant
        when(courses.findById(c.getId())).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.getCourse(c.getId()))
                .isInstanceOf(AcademyNotFoundException.class);
    }

    @Test
    void publishCourse_requiresAtLeastOneModule() {
        AcademyCourse c = draftCourse();
        when(courses.findById(c.getId())).thenReturn(Optional.of(c));
        when(modules.countByCourseId(c.getId())).thenReturn(0L);
        assertThatThrownBy(() -> service.publishCourse(c.getId()))
                .isInstanceOf(AcademyStateException.class);
    }

    @Test
    void publishCourse_succeedsWithModule() {
        AcademyCourse c = draftCourse();
        when(courses.findById(c.getId())).thenReturn(Optional.of(c));
        when(modules.countByCourseId(c.getId())).thenReturn(2L);
        when(courses.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AcademyDto.CourseResponse r = service.publishCourse(c.getId());
        assertThat(r.status()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void setQuiz_rejectsCorrectIndexOutOfBounds() {
        AcademyModule m = module();
        when(modules.findByTenantIdAndId(TENANT, m.getId())).thenReturn(Optional.of(m));
        when(quizzes.findByModuleId(m.getId())).thenReturn(Optional.empty());
        when(quizzes.save(any())).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });
        AcademyDto.CreateQuizRequest req = new AcademyDto.CreateQuizRequest("Quiz", 70,
                List.of(new AcademyDto.CreateQuestionRequest("Q?", List.of("A", "B"), 5, 1, 0)));
        assertThatThrownBy(() -> service.setQuiz(m.getId(), req))
                .isInstanceOf(AcademyStateException.class);
    }

    @Test
    void setQuiz_persistsQuestions() {
        AcademyModule m = module();
        when(modules.findByTenantIdAndId(TENANT, m.getId())).thenReturn(Optional.of(m));
        Quiz saved = new Quiz();
        saved.setId(UUID.randomUUID());
        saved.setModuleId(m.getId());
        saved.setTitle("Quiz");
        saved.setPassScore(70);
        // 1er appel (suppression de l'existant) : vide ; 2e appel (getQuiz final) : le quiz sauvé.
        when(quizzes.findByModuleId(m.getId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(saved));
        when(quizzes.save(any())).thenReturn(saved);
        when(questions.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questions.findByQuizIdOrderByOrderIndexAsc(saved.getId())).thenReturn(List.of());

        AcademyDto.CreateQuizRequest req = new AcademyDto.CreateQuizRequest("Quiz", 70,
                List.of(new AcademyDto.CreateQuestionRequest("Q?", List.of("A", "B"), 0, 1, 0)));
        AcademyDto.QuizResponse resp = service.setQuiz(m.getId(), req);

        verify(questions).save(any(QuizQuestion.class));
        assertThat(resp.id()).isEqualTo(saved.getId());
    }

    @Test
    void requireTenant_missing_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.listCourses(null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    private AcademyCourse draftCourse() {
        AcademyCourse c = new AcademyCourse();
        c.setId(UUID.randomUUID());
        c.setTenantId(TENANT);
        c.setStatus(CourseStatus.DRAFT);
        return c;
    }

    private AcademyModule module() {
        AcademyModule m = new AcademyModule();
        m.setId(UUID.randomUUID());
        m.setTenantId(TENANT);
        m.setCourseId(UUID.randomUUID());
        return m;
    }
}
