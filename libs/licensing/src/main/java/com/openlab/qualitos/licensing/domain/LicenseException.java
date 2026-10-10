package com.openlab.qualitos.licensing.domain;

/** Une licence illisible, mal formée ou mal signée. Le message dit pourquoi, sans secret. */
public class LicenseException extends RuntimeException {

    public LicenseException(String message) {
        super(message);
    }

    public LicenseException(String message, Throwable cause) {
        super(message, cause);
    }
}
