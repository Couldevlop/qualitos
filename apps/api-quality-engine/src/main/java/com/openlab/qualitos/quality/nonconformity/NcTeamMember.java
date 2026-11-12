package com.openlab.qualitos.quality.nonconformity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Une personne de l'équipe qui prend un écart en charge — la première discipline
 * du 8D (D1 « Constituer l'équipe »).
 *
 * <p><b>Deux colonnes pour une personne</b>, comme pour le signalant de la
 * fiche : l'identifiant de compte, qui ne change pas, et le nom <b>figé à la
 * saisie</b>. Le nom est une photographie : un rapport 8D relu trois ans plus
 * tard doit porter les noms de l'époque, pas ceux d'un annuaire qui a bougé.
 *
 * <p>{@code position} conserve l'ordre de saisie : le premier nommé est, par
 * convention d'atelier, celui qui pilote. Un tri alphabétique le perdrait.
 */
@Entity
@Table(name = "nc_team_members")
@Getter
@Setter
@NoArgsConstructor
public class NcTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "nc_id", nullable = false, updatable = false)
    private UUID ncId;

    /** L'identifiant de compte (le {@code sub} du jeton), jamais un libellé. */
    @Column(name = "account_id", nullable = false, length = 255, updatable = false)
    private String accountId;

    /** Le nom tel qu'il s'affichait au moment de la saisie. */
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    /** Le rang de saisie, à partir de 0. */
    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }
}
