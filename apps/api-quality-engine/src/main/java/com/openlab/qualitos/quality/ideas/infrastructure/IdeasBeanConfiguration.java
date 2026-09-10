package com.openlab.qualitos.quality.ideas.infrastructure;

import com.openlab.qualitos.quality.ideas.application.ActorProvider;
import com.openlab.qualitos.quality.ideas.application.IdeaService;
import com.openlab.qualitos.quality.ideas.application.TenantProvider;
import com.openlab.qualitos.quality.ideas.domain.IdeaRepository;
import com.openlab.qualitos.quality.ideas.domain.IdeaVoteRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage Spring du module idées.
 *
 * <p>Le {@code Clock} n'est pas déclaré ici : il existe déjà, unique et
 * {@code @Primary}, dans {@code CommonBeansConfiguration}. En déclarer un
 * second créerait une ambiguïté de bean pour tout autre module qui en
 * dépend.
 */
@Configuration
public class IdeasBeanConfiguration {

    @Bean
    public IdeaService ideaService(
            IdeaRepository ideas,
            IdeaVoteRepository votes,
            @Qualifier("ideasTenantContextProvider") TenantProvider tenants,
            @Qualifier("ideasJwtActorProvider") ActorProvider actors,
            Clock clock) {
        return new IdeaService(ideas, votes, tenants, actors, clock);
    }
}
