package com.openlab.qualitos.core.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Les personnes d'un cabinet. */
public interface PartnerConsultantRepository extends JpaRepository<PartnerConsultant, UUID> {

    List<PartnerConsultant> findByFirmIdOrderByDisplayNameAsc(UUID firmId);

    Optional<PartnerConsultant> findByAccountId(UUID accountId);

    boolean existsByFirmIdAndEmailIgnoreCase(UUID firmId, String email);

    /**
     * La fiche qui attend encore sa personne, reconnue a son courriel.
     *
     * <p>Sert au rattachement a la premiere connexion. Les fiches DEJA
     * rattachees sont exclues par la requete elle-meme : sans cela, un meme
     * courriel reutilise dans un autre cabinet pourrait voler le compte d'un
     * consultant en poste.
     */
    Optional<PartnerConsultant> findFirstByAccountIdIsNullAndEmailIgnoreCase(String email);

    long countByFirmIdAndActiveTrue(UUID firmId);
}
