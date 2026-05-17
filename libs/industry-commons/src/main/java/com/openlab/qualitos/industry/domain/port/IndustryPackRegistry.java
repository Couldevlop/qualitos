package com.openlab.qualitos.industry.domain.port;

import com.openlab.qualitos.industry.domain.model.IndustryPack;

import java.util.List;
import java.util.Optional;

/** Catalog of all loaded providers (cf. ServiceLoader discovery). */
public interface IndustryPackRegistry {

  List<IndustryPack> listAll();

  Optional<IndustryPack> find(String id);

  Optional<IndustryPackProvider> findProvider(String id);
}
