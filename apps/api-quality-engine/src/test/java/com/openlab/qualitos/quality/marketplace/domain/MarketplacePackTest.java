package com.openlab.qualitos.quality.marketplace.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketplacePackTest {

    static final Instant NOW = Instant.parse("2026-06-22T10:00:00Z");
    static final UUID PARTNER = UUID.randomUUID();
    static final UUID EDITOR = UUID.randomUUID();
    static final String SIG = "deadbeef".repeat(8);

    private MarketplacePack submitted() {
        return MarketplacePack.submit(
                "iso-13485-startup", "1.0", "Acme Consulting",
                "Pack ISO 13485 startup MedTech", "desc", "healthcare", "iso-13485",
                12000, "EUR",
                "https://packs.example.com/iso-13485-startup-1.0.zip",
                "{\"name\":\"x\",\"version\":\"1.0\"}", SIG, PARTNER, NOW);
    }

    @Test
    void submit_setsDefaults() {
        MarketplacePack p = submitted();
        assertThat(p.getStatus()).isEqualTo(MarketplacePackStatus.SUBMITTED);
        assertThat(p.getSubmittedBy()).isEqualTo(PARTNER);
        assertThat(p.getPriceCents()).isEqualTo(12000);
        assertThat(p.getCurrency()).isEqualTo("EUR");
        assertThat(p.getNormsCsv()).isEqualTo("iso-13485");
        assertThat(p.getRatingCount()).isZero();
        assertThat(p.getStatus().isPubliclyVisible()).isFalse();
    }

    @Test
    void submit_withoutSignature_throws() {
        assertThatThrownBy(() -> MarketplacePack.submit(
                "iso", "1.0", "Pub", "Title", null, "industry", null,
                0, "EUR", "https://x.com/y.zip", "{}", "short", PARTNER, NOW))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void happyPath_submit_review_publish() {
        MarketplacePack p = submitted();
        p.takeForReview(EDITOR, NOW.plusSeconds(10));
        assertThat(p.getStatus()).isEqualTo(MarketplacePackStatus.IN_REVIEW);
        assertThat(p.getReviewedBy()).isEqualTo(EDITOR);

        p.publish(EDITOR, NOW.plusSeconds(20));
        assertThat(p.getStatus()).isEqualTo(MarketplacePackStatus.PUBLISHED);
        assertThat(p.getStatus().isPubliclyVisible()).isTrue();
    }

    @Test
    void publish_withoutReview_throws() {
        MarketplacePack p = submitted();
        assertThatThrownBy(() -> p.publish(EDITOR, NOW))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void takeForReview_twice_throws() {
        MarketplacePack p = submitted();
        p.takeForReview(EDITOR, NOW);
        assertThatThrownBy(() -> p.takeForReview(EDITOR, NOW))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void reject_fromSubmitted_setsReason() {
        MarketplacePack p = submitted();
        p.reject(EDITOR, "manifeste incomplet", NOW);
        assertThat(p.getStatus()).isEqualTo(MarketplacePackStatus.REJECTED);
        assertThat(p.getReviewNotes()).isEqualTo("manifeste incomplet");
    }

    @Test
    void reject_withoutReason_throws() {
        MarketplacePack p = submitted();
        assertThatThrownBy(() -> p.reject(EDITOR, "  ", NOW))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void reject_afterPublish_throws() {
        MarketplacePack p = submitted();
        p.takeForReview(EDITOR, NOW);
        p.publish(EDITOR, NOW);
        assertThatThrownBy(() -> p.reject(EDITOR, "trop tard", NOW))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void deprecate_onlyFromPublished() {
        MarketplacePack p = submitted();
        assertThatThrownBy(() -> p.deprecate(EDITOR, NOW))
            .isInstanceOf(MarketplacePackStateException.class);
        p.takeForReview(EDITOR, NOW);
        p.publish(EDITOR, NOW);
        p.deprecate(EDITOR, NOW.plusSeconds(5));
        assertThat(p.getStatus()).isEqualTo(MarketplacePackStatus.DEPRECATED);
    }

    @Test
    void addRating_recomputesAverage() {
        MarketplacePack p = submitted();
        p.takeForReview(EDITOR, NOW);
        p.publish(EDITOR, NOW);
        p.addRating(4, NOW);
        p.addRating(5, NOW);
        assertThat(p.getRatingCount()).isEqualTo(2);
        assertThat(p.getRatingAvg()).isEqualTo(4.5);
    }

    @Test
    void addRating_onUnpublished_throws() {
        MarketplacePack p = submitted();
        assertThatThrownBy(() -> p.addRating(5, NOW))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void addRating_outOfRange_throws() {
        MarketplacePack p = submitted();
        p.takeForReview(EDITOR, NOW);
        p.publish(EDITOR, NOW);
        assertThatThrownBy(() -> p.addRating(6, NOW))
            .isInstanceOf(MarketplacePackStateException.class);
        assertThatThrownBy(() -> p.addRating(0, NOW))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void invalidPackId_throws() {
        assertThatThrownBy(() -> MarketplacePack.submit(
                "Invalid_ID", "1.0", "Pub", "T", null, "s", null, 0, "EUR",
                "https://x.com/y.zip", "{}", SIG, PARTNER, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownCurrency_throws() {
        assertThatThrownBy(() -> MarketplacePack.submit(
                "iso", "1.0", "Pub", "T", null, "s", null, 0, "XYZ",
                "https://x.com/y.zip", "{}", SIG, PARTNER, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void httpManifestUrl_throws() {
        assertThatThrownBy(() -> MarketplacePack.submit(
                "iso", "1.0", "Pub", "T", null, "s", null, 0, "EUR",
                "http://x.com/y.zip", "{}", SIG, PARTNER, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativePrice_throws() {
        assertThatThrownBy(() -> MarketplacePack.submit(
                "iso", "1.0", "Pub", "T", null, "s", null, -1, "EUR",
                "https://x.com/y.zip", "{}", SIG, PARTNER, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
