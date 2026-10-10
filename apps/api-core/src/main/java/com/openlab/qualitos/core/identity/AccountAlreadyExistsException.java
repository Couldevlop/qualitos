package com.openlab.qualitos.core.identity;

/** Un compte porte déjà cette adresse (409). */
public class AccountAlreadyExistsException extends RuntimeException {

    public AccountAlreadyExistsException(String email) {
        super("Un compte existe déjà pour " + email);
    }
}
