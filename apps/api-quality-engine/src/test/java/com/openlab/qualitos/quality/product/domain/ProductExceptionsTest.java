package com.openlab.qualitos.quality.product.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductExceptionsTest {

    @Test
    void notFoundException_carriesTheMissingId() {
        UUID id = UUID.randomUUID();

        ProductNotFoundException ex = new ProductNotFoundException(id);

        assertThat(ex.getMessage()).contains(id.toString());
    }

    @Test
    void codeConflictException_carriesTheDuplicatedCode() {
        ProductCodeConflictException ex = new ProductCodeConflictException("REF-4471");

        assertThat(ex.getMessage()).contains("REF-4471");
    }
}
