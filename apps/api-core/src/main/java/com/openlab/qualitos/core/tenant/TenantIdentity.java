package com.openlab.qualitos.core.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * L'identité d'un client : ce qui habille son espace et ce qui figure sur ses
 * documents signés.
 *
 * <p>Table séparée de {@code tenants} à dessein : {@code tenants} est lu à
 * chaque requête par tous les services (routage, facturation, accès), l'identité
 * une fois par session pour l'affichage. La clé primaire est le
 * {@code tenant_id} — un client a une identité, pas une liste.
 *
 * <p>La table porte {@code tenant_id}, donc elle est sous la politique de
 * cloisonnement (ADR 0086) : voir {@code V10__tenant_identity.sql}.
 */
@Entity
@Table(name = "tenant_identities")
@EntityListeners(AuditingEntityListener.class)
public class TenantIdentity {

    @Id
    @Column(name = "tenant_id", updatable = false, nullable = false)
    private UUID tenantId;

    /** Clé de l'objet dans le stockage. Ne sort jamais de l'API. */
    @Size(max = 512)
    @Column(name = "logo_object_key", length = 512)
    private String logoObjectKey;

    @Size(max = 64)
    @Column(name = "logo_content_type", length = 64)
    private String logoContentType;

    @Size(max = 160)
    @Column(name = "slogan", length = 160)
    private String slogan;

    @Size(max = 255)
    @Column(name = "legal_name")
    private String legalName;

    @Size(max = 64)
    @Column(name = "legal_form", length = 64)
    private String legalForm;

    @Size(max = 64)
    @Column(name = "registration_number", length = 64)
    private String registrationNumber;

    @Size(max = 32)
    @Column(name = "vat_number", length = 32)
    private String vatNumber;

    @Size(max = 255)
    @Column(name = "address_line1")
    private String addressLine1;

    @Size(max = 255)
    @Column(name = "address_line2")
    private String addressLine2;

    @Size(max = 16)
    @Column(name = "postal_code", length = 16)
    private String postalCode;

    @Size(max = 128)
    @Column(name = "city", length = 128)
    private String city;

    @Pattern(regexp = "^[A-Z]{2}$", message = "code pays attendu sur deux lettres majuscules (ISO 3166-1)")
    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Email
    @Size(max = 254)
    @Column(name = "contact_email", length = 254)
    private String contactEmail;

    @Size(max = 32)
    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$")
    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$")
    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_code", nullable = false, length = 20)
    private TenantTheme themeCode = TenantTheme.AIRY_LIGHT;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getLogoObjectKey() { return logoObjectKey; }
    public void setLogoObjectKey(String logoObjectKey) { this.logoObjectKey = logoObjectKey; }

    public String getLogoContentType() { return logoContentType; }
    public void setLogoContentType(String logoContentType) { this.logoContentType = logoContentType; }

    public String getSlogan() { return slogan; }
    public void setSlogan(String slogan) { this.slogan = slogan; }

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }

    public String getLegalForm() { return legalForm; }
    public void setLegalForm(String legalForm) { this.legalForm = legalForm; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getVatNumber() { return vatNumber; }
    public void setVatNumber(String vatNumber) { this.vatNumber = vatNumber; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }

    public TenantTheme getThemeCode() { return themeCode; }
    public void setThemeCode(TenantTheme themeCode) { this.themeCode = themeCode; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
