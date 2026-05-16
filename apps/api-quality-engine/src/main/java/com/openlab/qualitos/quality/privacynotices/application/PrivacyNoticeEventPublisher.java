package com.openlab.qualitos.quality.privacynotices.application;

import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNotice;

public interface PrivacyNoticeEventPublisher {

    enum Action { CREATED, EDITED, PUBLISHED, ARCHIVED, DELETED }

    void publish(PrivacyNotice notice, Action action);

    final class NoOp implements PrivacyNoticeEventPublisher {
        @Override public void publish(PrivacyNotice n, Action a) { }
    }
}
