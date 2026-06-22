package com.openlab.qualitos.quality.academy.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * Sérialise une {@code List<String>} en JSON texte pour les options de QCM.
 *
 * <p>JSON (et non CSV) car les options peuvent contenir des virgules. La
 * désérialisation est tolérante : une valeur nulle/vide redonne une liste vide,
 * un JSON corrompu lève une {@link IllegalStateException} explicite plutôt que
 * de renvoyer silencieusement une liste partielle.</p>
 */
@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize quiz options to JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            List<String> parsed = MAPPER.readValue(dbData, LIST_TYPE);
            return parsed == null ? List.of() : parsed;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize quiz options from JSON", e);
        }
    }
}
