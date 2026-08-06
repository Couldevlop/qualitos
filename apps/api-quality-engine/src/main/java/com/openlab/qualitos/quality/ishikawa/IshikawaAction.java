package com.openlab.qualitos.quality.ishikawa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Action décidée à partir d'un diagramme Ishikawa.
 *
 * <p>Identifier les causes est un moyen ; décider qui fait quoi et pour quand est
 * la fin. Sans ce plan, les décisions prises devant le diagramme vivaient ailleurs
 * — dans un compte rendu, un tableur, une mémoire — et le diagramme restait un
 * exercice.
 *
 * <p>Ce n'est PAS une action CAPA. Une CAPA est un dossier formel, avec instruction
 * et preuve d'efficacité ; en ouvrir un pour noter « refaire le réglage de la
 * butée, Karim, vendredi » découragerait la saisie et remplirait le registre de
 * broutilles. L'escalade vers une CAPA reste possible — c'est un lien, pas une
 * contrainte (§3.6).
 */
@Entity
@Table(name = "ishikawa_actions")
@Getter
@Setter
@NoArgsConstructor
public class IshikawaAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant porté sur la ligne elle-même, et pas seulement déduit du diagramme :
     * toute lecture peut ainsi être bornée sans jointure, ce qui rend impossible
     * une requête qui oublierait le cloisonnement (§18.2 #2).
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false, updatable = false)
    private IshikawaDiagram diagram;

    /** Ce qu'il y a à faire. C'est la colonne que l'on édite dans la cellule. */
    @Column(nullable = false, length = 500)
    private String label;

    /** Qui s'en charge. Un nom libre : tous les responsables n'ont pas de compte. */
    @Column(length = 255)
    private String responsible;

    /**
     * Date à laquelle l'action a été DÉCIDÉE — la date de la réunion, du comité,
     * de la revue. À ne pas confondre avec une échéance : ce qui est demandé ici,
     * c'est de savoir depuis quand une décision attend.
     */
    @Column(name = "decided_on")
    private LocalDate decidedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IshikawaActionStatus status = IshikawaActionStatus.TODO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
