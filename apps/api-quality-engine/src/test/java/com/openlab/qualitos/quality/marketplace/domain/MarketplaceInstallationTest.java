package com.openlab.qualitos.quality.marketplace.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketplaceInstallationTest {

    static final Instant NOW = Instant.parse("2026-06-22T10:00:00Z");
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID EDITOR = UUID.randomUUID();
    static final String SIG = "deadbeef".repeat(8);

    private MarketplacePack publishedPack() {
        MarketplacePack p = MarketplacePack.submit(
                "iso-9001-base", "1.0", "Pub", "Title", null, "industry", "iso-9001",
                0, "EUR", "https://x.com/y.zip", "{\"name\":\"x\",\"version\":\"1.0\"}",
                SIG, USER, NOW);
        p.assignId(UUID.randomUUID());
        p.takeForReview(EDITOR, NOW);
        p.publish(EDITOR, NOW);
        return p;
    }

    @Test
    void install_published_setsInstalled() {
        MarketplaceInstallation i = MarketplaceInstallation.install(TENANT, publishedPack(), USER, NOW);
        assertThat(i.getStatus()).isEqualTo(InstallationStatus.INSTALLED);
        assertThat(i.getTenantId()).isEqualTo(TENANT);
        assertThat(i.getInstalledBy()).isEqualTo(USER);
        assertThat(i.getPackId()).isEqualTo("iso-9001-base");
        assertThat(i.getPackVersion()).isEqualTo("1.0");
    }

    @Test
    void install_unpublished_throws() {
        MarketplacePack p = MarketplacePack.submit(
                "iso-9001-base", "1.0", "Pub", "Title", null, "industry", null,
                0, "EUR", "https://x.com/y.zip", "{}", SIG, USER, NOW);
        p.assignId(UUID.randomUUID());
        assertThatThrownBy(() -> MarketplaceInstallation.install(TENANT, p, USER, NOW))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void uninstall_flipsStatus() {
        MarketplaceInstallation i = MarketplaceInstallation.install(TENANT, publishedPack(), USER, NOW);
        i.uninstall(USER, NOW.plusSeconds(60));
        assertThat(i.getStatus()).isEqualTo(InstallationStatus.UNINSTALLED);
        assertThat(i.getUninstalledBy()).isEqualTo(USER);
        assertThat(i.getUninstalledAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void uninstall_twice_throws() {
        MarketplaceInstallation i = MarketplaceInstallation.install(TENANT, publishedPack(), USER, NOW);
        i.uninstall(USER, NOW);
        assertThatThrownBy(() -> i.uninstall(USER, NOW))
            .isInstanceOf(MarketplacePackStateException.class);
    }
}
