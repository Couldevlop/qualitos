package com.openlab.qualitos.quality.academy.presentation;

import com.openlab.qualitos.quality.academy.application.AcademyCertificateService;
import com.openlab.qualitos.quality.academy.application.AcademyDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vérification PUBLIQUE d'un certificat de formation par code (QR), §19.3.
 *
 * <p>Endpoint non authentifié (whitelisté dans SecurityConfig) : l'autorité est
 * le code lui-même (UUID non devinable). Aucune donnée personnelle interne n'est
 * exposée ; on revalide la signature hybride contre la clé plateforme épinglée.</p>
 */
@RestController
@RequestMapping("/api/v1/academy/public/certificates")
@Validated
public class AcademyCertificateVerificationController {

    private final AcademyCertificateService certificates;

    public AcademyCertificateVerificationController(AcademyCertificateService certificates) {
        this.certificates = certificates;
    }

    @GetMapping("/{code}/verify")
    public AcademyDto.CertificateVerification verify(
            @PathVariable @NotBlank @Size(max = 64) String code) {
        return certificates.verify(code);
    }
}
