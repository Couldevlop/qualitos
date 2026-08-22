package com.openlab.qualitos.quality.product.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextProviderTest {

    final TenantContextProvider provider = new TenantContextProvider();

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void requireTenantId_readsTheThreadLocalTenant() {
        UUID tenant = UUID.randomUUID();
        TenantContext.setTenantId(tenant.toString());

        assertThat(provider.requireTenantId()).isEqualTo(tenant);
    }

    @Test
    void requireTenantId_withoutTenant_throws() {
        TenantContext.clear();

        assertThatThrownBy(provider::requireTenantId)
                .isInstanceOf(MissingTenantContextException.class);
    }
}
