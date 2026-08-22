package com.openlab.qualitos.quality.product.domain;

public class ProductCodeConflictException extends RuntimeException {
    public ProductCodeConflictException(String code) {
        super("Product code already used: " + code);
    }
}
