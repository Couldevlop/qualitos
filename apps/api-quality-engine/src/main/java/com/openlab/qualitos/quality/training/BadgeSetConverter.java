package com.openlab.qualitos.quality.training;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sérialise un {@link Set} de {@link Badge} en CSV pour la colonne {@code badges}.
 *
 * <p>Le stockage CSV (plutôt qu'une table de jointure) suffit : l'ensemble de
 * badges est petit, borné par l'énum, et toujours lu/écrit en bloc avec la
 * progression. La désérialisation ignore tolérante les valeurs inconnues
 * (badge retiré d'une future version) pour ne jamais casser une lecture.</p>
 */
@Converter
public class BadgeSetConverter implements AttributeConverter<Set<Badge>, String> {

    @Override
    public String convertToDatabaseColumn(Set<Badge> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        // Tri par ordre d'énum pour un rendu déterministe (diffs stables).
        return attribute.stream()
                .sorted()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<Badge> convertToEntityAttribute(String dbData) {
        Set<Badge> result = EnumSet.noneOf(Badge.class);
        if (dbData == null || dbData.isBlank()) {
            return result;
        }
        Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(name -> {
                    try {
                        result.add(Badge.valueOf(name));
                    } catch (IllegalArgumentException ignored) {
                        // Valeur inconnue (badge supprimé) : ignorée silencieusement.
                    }
                });
        return result;
    }
}
