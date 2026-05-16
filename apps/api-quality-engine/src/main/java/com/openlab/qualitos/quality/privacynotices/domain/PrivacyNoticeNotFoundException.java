package com.openlab.qualitos.quality.privacynotices.domain;

import java.util.UUID;

public class PrivacyNoticeNotFoundException extends RuntimeException {
    public PrivacyNoticeNotFoundException(UUID id) {
        super("Privacy notice not found: " + id);
    }
}
