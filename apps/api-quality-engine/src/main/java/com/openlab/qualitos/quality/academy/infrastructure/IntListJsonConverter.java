package com.openlab.qualitos.quality.academy.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * Sérialise une {@code List<Integer>} (index de réponses choisies) en JSON texte.
 */
@Converter
public class IntListJsonConverter implements AttributeConverter<List<Integer>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Integer>> LIST_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
        try {
            return MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize quiz answers to JSON", e);
        }
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            List<Integer> parsed = MAPPER.readValue(dbData, LIST_TYPE);
            return parsed == null ? List.of() : parsed;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize quiz answers from JSON", e);
        }
    }
}
