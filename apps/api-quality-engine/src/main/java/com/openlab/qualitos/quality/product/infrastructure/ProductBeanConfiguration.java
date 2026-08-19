package com.openlab.qualitos.quality.product.infrastructure;

import com.openlab.qualitos.quality.product.application.ActorProvider;
import com.openlab.qualitos.quality.product.application.ProductService;
import com.openlab.qualitos.quality.product.application.TenantProvider;
import com.openlab.qualitos.quality.product.domain.ProductRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ProductBeanConfiguration {

    @Bean
    public ProductService productService(
            ProductRepository repo,
            @Qualifier("productTenantContextProvider") TenantProvider tenantProvider,
            ActorProvider actorProvider,
            Clock clock) {
        return new ProductService(repo, tenantProvider, actorProvider, clock);
    }
}
