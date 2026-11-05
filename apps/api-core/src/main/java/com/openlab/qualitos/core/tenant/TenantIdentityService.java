package com.openlab.qualitos.core.tenant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * L'identité d'un client : lecture, enregistrement, logo.
 *
 * <p>Deux gardes, et elles sont ici plutôt que dans l'écran :
 *
 * <ul>
 *   <li><b>le client existe</b> — on ne décore pas un identifiant inventé ;</li>
 *   <li><b>l'apparence est lisible</b> — la couleur d'accent est confrontée au
 *       fond du thème choisi ({@link ContrastGuard}). Un contrôle côté écran
 *       serait une politesse ; celui-ci est une garantie.</li>
 * </ul>
 *
 * <p>Le contraste se mesure sur le fond du thème RETENU, non sur du blanc :
 * un accent clair est illisible sur blanc et juste sur un thème sombre.
 */
@Service
@Transactional(readOnly = true)
public class TenantIdentityService {

    private final TenantRepository tenants;
    private final TenantIdentityRepository identities;

    public TenantIdentityService(TenantRepository tenants, TenantIdentityRepository identities) {
        this.tenants = tenants;
        this.identities = identities;
    }

    public TenantIdentityDto.Response get(UUID tenantId) {
        requireTenant(tenantId);
        return identities.findById(tenantId)
                .map(TenantIdentityDto.Response::from)
                .orElseGet(TenantIdentityDto.Response::blank);
    }

    @Transactional
    public TenantIdentityDto.Response save(UUID tenantId, TenantIdentityDto.Request request) {
        requireTenant(tenantId);
        TenantTheme theme = TenantTheme.of(request.themeCode());
        requireReadableOn(theme, request.primaryColor());
        requireReadableOn(theme, request.secondaryColor());

        TenantIdentity identity = identities.findById(tenantId).orElseGet(TenantIdentity::new);
        identity.setTenantId(tenantId);
        identity.setSlogan(request.slogan());
        identity.setLegalName(request.legalName());
        identity.setLegalForm(request.legalForm());
        identity.setRegistrationNumber(request.registrationNumber());
        identity.setVatNumber(request.vatNumber());
        identity.setAddressLine1(request.addressLine1());
        identity.setAddressLine2(request.addressLine2());
        identity.setPostalCode(request.postalCode());
        identity.setCity(request.city());
        identity.setCountryCode(request.countryCode());
        identity.setContactEmail(request.contactEmail());
        identity.setContactPhone(request.contactPhone());
        identity.setPrimaryColor(request.primaryColor());
        identity.setSecondaryColor(request.secondaryColor());
        identity.setThemeCode(theme);

        return TenantIdentityDto.Response.from(identities.save(identity));
    }

    /**
     * Enregistre le logo téléversé, et ne touche à rien d'autre : remplacer un
     * logo ne doit pas effacer un slogan.
     */
    @Transactional
    public void saveLogo(UUID tenantId, String objectKey, String contentType) {
        requireTenant(tenantId);
        TenantIdentity identity = identities.findById(tenantId).orElseGet(TenantIdentity::new);
        identity.setTenantId(tenantId);
        identity.setLogoObjectKey(objectKey);
        identity.setLogoContentType(contentType);
        identities.save(identity);
    }

    /** La clé de l'objet, pour le service qui sert le fichier. Jamais exposée en API. */
    public String logoObjectKey(UUID tenantId) {
        requireTenant(tenantId);
        return identities.findById(tenantId).map(TenantIdentity::getLogoObjectKey).orElse(null);
    }

    public String logoContentType(UUID tenantId) {
        return identities.findById(tenantId).map(TenantIdentity::getLogoContentType).orElse(null);
    }

    private void requireTenant(UUID tenantId) {
        if (!tenants.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
    }

    private void requireReadableOn(TenantTheme theme, String color) {
        if (color != null) {
            ContrastGuard.requireReadable(color, theme.background());
        }
    }
}
