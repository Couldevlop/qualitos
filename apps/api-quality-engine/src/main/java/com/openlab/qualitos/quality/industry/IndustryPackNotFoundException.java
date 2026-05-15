package com.openlab.qualitos.quality.industry;

public class IndustryPackNotFoundException extends RuntimeException {
    public IndustryPackNotFoundException(String code) {
        super("Industry pack not found: " + code);
    }
}
