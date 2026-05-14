package com.openlab.qualitos.core.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TenantContext")
class TenantContextTest {

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Returns null when no tenant is set")
    void returnsNullWhenEmpty() {
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Returns tenant_id after setTenantId")
    void returnsTenantIdAfterSet() {
        TenantContext.setTenantId("tenant-abc");
        assertThat(TenantContext.getTenantId()).isEqualTo("tenant-abc");
    }

    @Test
    @DisplayName("hasTenant returns false when context is empty")
    void hasTenantReturnsFalseWhenEmpty() {
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    @DisplayName("hasTenant returns true after setTenantId")
    void hasTenantReturnsTrueAfterSet() {
        TenantContext.setTenantId("tenant-xyz");
        assertThat(TenantContext.hasTenant()).isTrue();
    }

    @Test
    @DisplayName("Returns null after clear")
    void returnsNullAfterClear() {
        TenantContext.setTenantId("tenant-abc");
        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("hasTenant returns false after clear")
    void hasTenantReturnsFalseAfterClear() {
        TenantContext.setTenantId("tenant-abc");
        TenantContext.clear();
        assertThat(TenantContext.hasTenant()).isFalse();
    }
}
