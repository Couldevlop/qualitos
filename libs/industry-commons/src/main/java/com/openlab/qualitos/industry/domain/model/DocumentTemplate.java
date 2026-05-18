package com.openlab.qualitos.industry.domain.model;

import java.util.List;

/** Template shipped with the pack (manual, procedure, audit checklist, ...). */
public record DocumentTemplate(
    String id,
    String name,
    String kind,               // manual | procedure | audit-checklist | record
    String mediaType,          // application/vnd.openxmlformats... etc.
    String path,               // classpath-relative path inside the pack
    List<String> mapsToNorms
) {
  public DocumentTemplate {
    mapsToNorms = mapsToNorms == null ? List.of() : List.copyOf(mapsToNorms);
  }
}
