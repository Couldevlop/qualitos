package com.openlab.qualitos.core.partner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Un cabinet partenaire : il accompagne des clients, il n'en est pas un.
 *
 * <p>La distinction n'est pas de vocabulaire. Un client a un espace, des
 * modules souscrits, une facture et une identite visuelle ; un cabinet n'a rien
 * de tout cela. Il a des consultants et un portefeuille de missions. Le ranger
 * dans {@code tenants} aurait fait apparaitre les cabinets dans les
 * statistiques clients, dans la facturation et dans le selecteur de client —
 * trois endroits ou ils n'ont rien a faire.
 *
 * <p>Suspendre un cabinet coupe l'acces de tous ses consultants d'un coup, sans
 * toucher aux accords : une mesure conservatoire doit pouvoir se lever.
 */
@Entity
@Table(name = "partner_firms")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerFirm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(min = 3, max = 63)
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$",
            message = "slug attendu en minuscules, chiffres et tirets")
    @Column(name = "slug", nullable = false, length = 63)
    private String slug;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 64)
    @Column(name = "registration_number", length = 64)
    private String registrationNumber;

    @Email
    @Size(max = 254)
    @Column(name = "contact_email", length = 254)
    private String contactEmail;

    @Size(max = 32)
    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
