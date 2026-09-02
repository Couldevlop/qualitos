package com.openlab.qualitos.core.billing.invoice;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt des factures.
 *
 * <p>Deux lectures portent des règles, pas des commodités :
 *
 * <ul>
 *   <li>{@link #findByTenantAndPeriod} est l'idempotence de l'émission — la
 *       même que l'index {@code uk_invoice_tenant_period} ;</li>
 *   <li>{@link #findLastNumberOfFiscalYear} est la continuité de la
 *       numérotation. Le tri porte sur le NUMÉRO et non sur la date : deux
 *       factures émises la même seconde ne se départageraient pas par
 *       {@code issued_at}, et la séquence sauterait un rang.</li>
 * </ul>
 *
 * <p>Le tri par numéro est lexicographique, ce qui suffit tant que le rang est
 * cadré à quatre chiffres. Au-delà de 9999 (voir {@link InvoiceNumber}),
 * {@code FA-2031-10000} précède {@code FA-2031-9999} en ordre alphabétique.
 * D'où le tri sur la LONGUEUR d'abord : plus de chiffres, plus grand rang.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @Query("""
            SELECT i FROM Invoice i
            WHERE i.tenantId = :tenantId
              AND i.periodYear = :periodYear
              AND i.periodMonth = :periodMonth
            """)
    Optional<Invoice> findByTenantAndPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("periodYear") int periodYear,
            @Param("periodMonth") int periodMonth);

    @Query("""
            SELECT i.number FROM Invoice i
            WHERE i.fiscalYear = :fiscalYear
            ORDER BY LENGTH(i.number) DESC, i.number DESC
            """)
    List<String> findNumbersOfFiscalYearDescending(@Param("fiscalYear") int fiscalYear,
                                                   Pageable pageable);

    /**
     * Le dernier numéro attribué dans l'exercice, ou {@link Optional#empty()}
     * si l'exercice n'a encore rien produit.
     *
     * <p>{@code PageRequest.of(0, 1)} et non un {@code findAll} filtré : la
     * requête ne doit ramener QU'UNE ligne. Charger tous les numéros de
     * l'exercice pour n'en garder qu'un ferait grossir la lecture avec le
     * chiffre d'affaires — silencieusement, puisque le résultat resterait
     * juste.
     */
    default Optional<String> findLastNumberOfFiscalYear(int fiscalYear) {
        return findNumbersOfFiscalYearDescending(fiscalYear, PageRequest.of(0, 1))
                .stream().findFirst();
    }

    @Query("""
            SELECT i FROM Invoice i
            WHERE i.tenantId = :tenantId
            ORDER BY i.number DESC
            """)
    List<Invoice> findByTenantOrderByNumberDesc(@Param("tenantId") UUID tenantId);
}
