package com.openlab.qualitos.core.billing.invoice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La méthode par DÉFAUT du dépôt — la seule logique que porte une interface
 * Spring Data, et donc la seule que les bancs à dépôt simulé ne voient jamais :
 * partout ailleurs, {@code findLastNumberOfFiscalYear} est remplacée par un
 * stub, et son corps n'est jamais exécuté.
 *
 * <p>{@code doCallRealMethod()} force ici l'exécution du vrai corps sur un
 * dépôt simulé : c'est ce qui permet d'affirmer la {@link Pageable} transmise,
 * et pas seulement la valeur rendue. Charger tous les numéros de l'exercice
 * pour n'en garder qu'un ferait grossir la lecture avec le chiffre d'affaires —
 * silencieusement, puisque le résultat resterait juste.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceRepository — numérotation")
class InvoiceRepositoryNumberingTest {

    @Mock InvoiceRepository repo;

    @Test
    void leDernierNumeroEstLuEnUneSeuleLigne() {
        doCallRealMethod().when(repo).findLastNumberOfFiscalYear(anyInt());
        when(repo.findNumbersOfFiscalYearDescending(eq(2026), any()))
                .thenReturn(List.of("FA-2026-0041"));

        Optional<String> dernier = repo.findLastNumberOfFiscalYear(2026);

        assertThat(dernier).contains("FA-2026-0041");
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findNumbersOfFiscalYearDescending(eq(2026), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(1);
        assertThat(page.getValue().getPageNumber()).isZero();
    }

    @Test
    void unExerciceViergeRendVideEtNonUneErreur() {
        // C'est ce vide qui fait ouvrir la sequence a FA-<annee>-0001, dans
        // InvoiceService. Une exception ici bloquerait la premiere facture de
        // chaque exercice — une fois par an, le 2 janvier.
        doCallRealMethod().when(repo).findLastNumberOfFiscalYear(anyInt());
        when(repo.findNumbersOfFiscalYearDescending(eq(2027), any())).thenReturn(List.of());

        assertThat(repo.findLastNumberOfFiscalYear(2027)).isEmpty();
    }
}
