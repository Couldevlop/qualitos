package com.openlab.qualitos.quality.controlplan.web;

import com.openlab.qualitos.quality.controlplan.domain.CharacteristicType;
import com.openlab.qualitos.quality.controlplan.domain.InputOutput;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.risk.CharacteristicClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Requêtes HTTP du control plan. Aucune ne porte de {@code tenantId} : le tenant
 * vient toujours du JWT côté service (règle 18.2 #2). Les bornes Jakarta reprennent
 * les maxima des colonnes de la migration V112 — sans elles, une valeur trop longue
 * franchirait la frontière et ressortirait en exception de persistance brute plutôt
 * qu'en 400 lisible.
 */
public final class ControlPlanWebDto {

    private ControlPlanWebDto() {}

    public record CreateRequest(
            @NotNull ControlPlanPhase phase,
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$",
                    message = "code must match [A-Za-z0-9][A-Za-z0-9._-]{0,63}")
            String code,
            UUID ownerUserId) {}

    public record LineRequest(
            @PositiveOrZero int sequenceNo,
            UUID operationId,
            @Size(max = 250) String machine,
            @Size(max = 32) String characteristicNo,
            @NotBlank @Size(max = 500) String characteristicLabel,
            // La caracteristique qui porte la specification (« Specification
            // characteristic » de la trame) : on surveille une longueur de fil,
            // on specifie une cote de coupe. Facultative — un plan se remplit
            // par passes, et exiger la colonne empecherait d'ouvrir la ligne.
            @Size(max = 500) String specifiedCharacteristic,
            @NotNull CharacteristicType characteristicType,
            CharacteristicClass specialClass,
            @Size(max = 500) String specification,
            BigDecimal toleranceLower,
            BigDecimal toleranceUpper,
            @Size(max = 24) String unit,
            @Size(max = 250) String measurementTechnique,
            // Texte et non nombre : « 100 % », « 5 au réglage puis 1 sur 50 »
            // sont des tailles d'échantillon parfaitement valides, et un entier
            // obligeait à les tronquer ou à les écrire ailleurs.
            @Size(max = 120) String sampleSize,
            @Size(max = 120) String sampleFrequency,
            @Size(max = 500) String controlMethod,
            @Size(max = 1000) String reactionPlan,
            UUID fmeaItemId,
            @Size(max = 64) String sopReference,
            InputOutput inputOutput,
            @Size(max = 250) String whoMeasures,
            @Size(max = 250) String recordingLocation) {}
}
