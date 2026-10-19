package com.openlab.qualitos.quality.docs;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentSpecificationsTest {

    @SuppressWarnings("unchecked") Root<Document> root = mock(Root.class);
    CriteriaQuery<?> query = mock(CriteriaQuery.class);
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    @SuppressWarnings("unchecked") Path<Object> path = mock(Path.class);
    Predicate predicate = mock(Predicate.class);

    @BeforeEach
    void setUp() {
        when(root.get(anyString())).thenReturn(path);
        when(path.in(anyCollection())).thenReturn(predicate);
        when(cb.equal(any(), any(Object.class))).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);
        when(cb.and(any(), any())).thenReturn(predicate);
        when(cb.or(any(Predicate[].class))).thenReturn(predicate);
        when(cb.not(any())).thenReturn(predicate);
        when(cb.disjunction()).thenReturn(predicate);
    }

    Predicate apply(Specification<Document> spec) {
        return spec.toPredicate(root, query, cb);
    }

    @Test
    void escapeLike_neutraliseLesJokers() {
        assertThat(DocumentSpecifications.escapeLike("50%_a\\b")).isEqualTo("50\\%\\_a\\\\b");
        assertThat(DocumentSpecifications.escapeLike("plain")).isEqualTo("plain");
    }

    @Test
    void filtresNull_neFiltrentRien() {
        assertThat(DocumentSpecifications.ownedBy(null)).isNull();
        assertThat(DocumentSpecifications.inFolders(null)).isNull();
        assertThat(DocumentSpecifications.ofType(null)).isNull();
        assertThat(DocumentSpecifications.ofProcess(null)).isNull();
        assertThat(DocumentSpecifications.withDisplayStatus(null, LocalDate.now())).isNull();
        assertThat(DocumentSpecifications.matching(null)).isNull();
        assertThat(DocumentSpecifications.matching("   ")).isNull();
    }

    @Test
    void criteresSimples() {
        UUID id = UUID.randomUUID();
        assertThat(apply(DocumentSpecifications.tenant(id))).isSameAs(predicate);
        assertThat(apply(DocumentSpecifications.active())).isSameAs(predicate);
        assertThat(apply(DocumentSpecifications.archived())).isSameAs(predicate);
        assertThat(apply(DocumentSpecifications.ownedBy(id))).isSameAs(predicate);
        assertThat(apply(DocumentSpecifications.ofType(DocumentType.PROCEDURE))).isSameAs(predicate);
        assertThat(apply(DocumentSpecifications.ofProcess(id))).isSameAs(predicate);
        assertThat(apply(DocumentSpecifications.inFolders(List.of(id)))).isSameAs(predicate);
        assertThat(apply(DocumentSpecifications.idIn(List.of(id)))).isSameAs(predicate);
        verify(cb, times(6)).equal(any(), any(Object.class));
    }

    @Test
    void idIn_vide_neRendRien() {
        assertThat(apply(DocumentSpecifications.idIn(List.of()))).isSameAs(predicate);
        verify(cb).disjunction();
    }

    @Test
    void reviewDueBy() {
        assertThat(apply(DocumentSpecifications.reviewDueBy(LocalDate.of(2026, 10, 19)))).isSameAs(predicate);
        verify(cb).notEqual(path, DocumentLifecycle.DRAFT);
    }

    @Test
    void withDisplayStatus_chaqueStatut() {
        for (DisplayStatus s : DisplayStatus.values()) {
            assertThat(apply(DocumentSpecifications.withDisplayStatus(s, LocalDate.of(2026, 10, 19))))
                    .isSameAs(predicate);
        }
    }

    @Test
    void matching_motifEchappeEnMinuscules() {
        assertThat(apply(DocumentSpecifications.matching("  Taux 50%  "))).isSameAs(predicate);
        verify(cb, times(3)).like(any(), eq("%taux 50\\%%"), eq('\\'));
    }
}
