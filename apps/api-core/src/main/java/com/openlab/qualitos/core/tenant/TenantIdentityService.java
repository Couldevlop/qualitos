package com.openlab.qualitos.core.tenant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
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
    private final TenantLogoRepository logos;

    public TenantIdentityService(TenantRepository tenants, TenantIdentityRepository identities,
                                 TenantLogoRepository logos) {
        this.tenants = tenants;
        this.identities = identities;
        this.logos = logos;
    }

    public TenantIdentityDto.Response get(UUID tenantId) {
        requireTenant(tenantId);
        boolean logoAvailable = logos.existsById(tenantId);
        return identities.findById(tenantId)
                .map(identity -> TenantIdentityDto.Response.from(identity, logoAvailable))
                .orElseGet(() -> logoAvailable
                        ? TenantIdentityDto.Response.blankWithLogo()
                        : TenantIdentityDto.Response.blank());
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

        return TenantIdentityDto.Response.from(identities.save(identity), logos.existsById(tenantId));
    }

    /**
     * Enregistre le logo téléversé, et ne touche à rien d'autre : remplacer un
     * logo ne doit pas effacer un slogan.
     *
     * <p>Le type ANNONCÉ n'est pas celui qui est enregistré : {@link LogoPolicy}
     * rend le type normalisé, après l'avoir confronté aux premiers octets.
     *
     * @return l'empreinte du contenu enregistré (sert l'ETag de la route)
     */
    @Transactional
    public String saveLogo(UUID tenantId, String declaredContentType, byte[] content) {
        requireTenant(tenantId);
        String contentType = LogoPolicy.check(declaredContentType, content);

        TenantLogo logo = logos.findById(tenantId).orElseGet(TenantLogo::new);
        logo.setTenantId(tenantId);
        logo.setContentType(contentType);
        logo.setSizeBytes(content.length);
        logo.setSha256(sha256(content));
        logo.setContent(content);
        return logos.save(logo).getSha256();
    }

    /** Le logo d'un client, s'il en a posé un. Vide sinon — ce n'est pas une erreur. */
    public Optional<TenantLogo> logo(UUID tenantId) {
        requireTenant(tenantId);
        return logos.findById(tenantId);
    }

    /** Retire le logo. L'identité — slogan, couleurs, mentions — reste intacte. */
    @Transactional
    public void deleteLogo(UUID tenantId) {
        requireTenant(tenantId);
        logos.deleteById(tenantId);
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        // SHA-256 est exigé de toute implémentation de la plateforme Java : ce
        // bras ne s'emprunte pas, et aucun test ne peut l'atteindre.
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponible", impossible);
        }
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
