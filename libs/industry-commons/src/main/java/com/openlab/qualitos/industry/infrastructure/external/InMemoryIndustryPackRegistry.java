package com.openlab.qualitos.industry.infrastructure.external;

import com.openlab.qualitos.industry.domain.model.IndustryPack;
import com.openlab.qualitos.industry.domain.port.IndustryPackProvider;
import com.openlab.qualitos.industry.domain.port.IndustryPackRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Trivial in-memory registry. Wired up by Spring in api-core via a @Configuration. */
public final class InMemoryIndustryPackRegistry implements IndustryPackRegistry {

  private final Map<String, IndustryPackProvider> byId = new LinkedHashMap<>();

  public InMemoryIndustryPackRegistry(Collection<IndustryPackProvider> providers) {
    for (IndustryPackProvider p : providers) {
      if (p == null || p.id() == null) continue;
      byId.put(p.id(), p);
    }
  }

  @Override
  public List<IndustryPack> listAll() {
    return byId.values().stream().map(IndustryPackProvider::getPack).toList();
  }

  @Override
  public Optional<IndustryPack> find(String id) {
    return findProvider(id).map(IndustryPackProvider::getPack);
  }

  @Override
  public Optional<IndustryPackProvider> findProvider(String id) {
    return Optional.ofNullable(byId.get(id));
  }
}
