package com.openlab.qualitos.quality.costofquality.web;

import com.openlab.qualitos.quality.costofquality.domain.CoqCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Les corps de requête.
 *
 * <p>Ces contraintes sont une première barrière, lisible dans l'OpenAPI. La
 * règle qui compte — les champs pièces exigés selon le libellé — ne peut pas
 * s'écrire ici, parce qu'elle dépend du libellé choisi : c'est le domaine qui
 * la tient ({@code CoqEntry}).
 *
 * <p>Aucun champ tenant ni auteur : ils viennent du jeton (§18.2).
 */
public final class CostOfQualityWebDto {

    private CostOfQualityWebDto() {}

    public record EntryRequest(
            @NotNull UUID labelId,
            @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotBlank @Size(max = 150) String responsible,
            @NotNull LocalDate imputationDate,
            @Size(max = 2000) String comment,
            @Size(max = 120) String partReference,
            @Min(1) Integer partQuantity,
            @Size(max = 120) String lot,
            LocalDate receivedOrMadeOn) {}

    public record LabelRequest(
            @NotNull CoqCategory category,
            @NotBlank @Size(max = 150) String name,
            boolean partControl) {}

    public record CurrencyRequest(@NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currency) {}

    public record CurrencyView(String currency) {}
}
