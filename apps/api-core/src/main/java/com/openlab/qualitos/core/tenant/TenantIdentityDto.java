package com.openlab.qualitos.core.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Ce qui entre et ce qui sort de l'identité d'un client.
 *
 * <p>Les octets du logo ne figurent dans aucune réponse : l'écran apprend
 * seulement qu'un logo existe ({@code logoAvailable}) et passe par la route
 * dédiée pour l'obtenir. Un logo encodé dans la réponse d'identité
 * alourdirait de 512 Kio une lecture faite à chaque ouverture d'écran, et
 * priverait le navigateur de son cache.
 */
public sealed interface TenantIdentityDto permits TenantIdentityDto.Request, TenantIdentityDto.Response {

    record Request(
            @Size(max = 160) String slogan,
            @Size(max = 255) String legalName,
            @Size(max = 64) String legalForm,
            @Size(max = 64) String registrationNumber,
            @Size(max = 32) String vatNumber,
            @Size(max = 255) String addressLine1,
            @Size(max = 255) String addressLine2,
            @Size(max = 16) String postalCode,
            @Size(max = 128) String city,
            @Pattern(regexp = "^[A-Z]{2}$", message = "code pays sur deux lettres majuscules (ISO 3166-1)")
            String countryCode,
            @Email @Size(max = 254) String contactEmail,
            @Size(max = 32) String contactPhone,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "couleur attendue en #RRGGBB")
            String primaryColor,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "couleur attendue en #RRGGBB")
            String secondaryColor,
            String themeCode
    ) implements TenantIdentityDto {}

    record Response(
            String slogan,
            String legalName,
            String legalForm,
            String registrationNumber,
            String vatNumber,
            String addressLine1,
            String addressLine2,
            String postalCode,
            String city,
            String countryCode,
            String contactEmail,
            String contactPhone,
            String primaryColor,
            String secondaryColor,
            TenantTheme themeCode,
            boolean logoAvailable
    ) implements TenantIdentityDto {

        static Response from(TenantIdentity identity, boolean logoAvailable) {
            return new Response(
                    identity.getSlogan(), identity.getLegalName(), identity.getLegalForm(),
                    identity.getRegistrationNumber(), identity.getVatNumber(),
                    identity.getAddressLine1(), identity.getAddressLine2(),
                    identity.getPostalCode(), identity.getCity(), identity.getCountryCode(),
                    identity.getContactEmail(), identity.getContactPhone(),
                    identity.getPrimaryColor(), identity.getSecondaryColor(),
                    identity.getThemeCode(),
                    logoAvailable);
        }

        /** L'identité du socle : ce que voit un client qui n'a rien encore choisi. */
        static Response blank() {
            return new Response(null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, TenantTheme.AIRY_LIGHT, false);
        }

        /**
         * Le cas du client qui a posé son logo sans rien remplir d'autre : le
         * logo doit s'afficher quand même, sinon le téléversement semble perdu.
         */
        static Response blankWithLogo() {
            return new Response(null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, TenantTheme.AIRY_LIGHT, true);
        }
    }
}
