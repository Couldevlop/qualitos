package com.openlab.qualitos.quality.nonconformity.eightd.web;

import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDDto;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vérification PUBLIQUE d'un rapport 8D, atteinte en scannant le QR code du PDF.
 *
 * <p>Pas de jeton, pas de contexte de tenant : le code opaque EST l'autorité — même
 * modèle que la vérification d'un certificat de formation ou d'un export de
 * dashboard. La réponse ne rend que des faits d'intégrité (validité, empreinte,
 * référence de l'ancrage, référence de la NC), jamais le contenu du rapport.
 *
 * <p>Un code inconnu répond {@code valid=false} et non 404 : sans cela, la route
 * permettrait de distinguer un code existant d'un code inventé, donc d'énumérer
 * (OWASP A01).
 */
@RestController
@RequestMapping("/api/v1/nc/public/8d")
@Validated
@Tag(name = "8D", description = "Public verification of an issued 8D report")
public class EightDPublicController {

    private final EightDService service;

    public EightDPublicController(EightDService service) {
        this.service = service;
    }

    @GetMapping("/{code}/verify")
    @Operation(summary = "Verify the signature sealed over an issued 8D report's fingerprint")
    public EightDDto.VerificationResult verifier(
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$", message = "invalid verification code")
            String code) {
        return service.verifier(code);
    }
}
