package com.openlab.qualitos.quality.fmeascale;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Le référentiel de cotation FMEA du tenant (§4.5).
 *
 * <p>La LECTURE est ouverte à tout utilisateur authentifié : on ne peut pas
 * demander à quelqu'un de coter de 1 à 10 en lui cachant ce que valent les
 * chiffres.
 *
 * <p>L'ÉCRITURE est réservée à la direction qualité et à l'administration du
 * tenant. Changer un barème rend incomparables les RPN cotés avant et après —
 * c'est une décision de politique qualité, pas un réglage d'écran. Le manager
 * qualité en est volontairement exclu : il cote, il ne redéfinit pas l'échelle
 * sur laquelle il cote.
 */
@RestController
@RequestMapping("/api/v1/fmea/rating-scales")
@Validated
@PreAuthorize("isAuthenticated()")
public class FmeaScaleController {

    private static final String EDIT_ROLES =
            "hasAnyRole('DIRECTOR_QUALITY','QUALITY_DIRECTOR','ADMIN_TENANT','SUPER_ADMIN')";

    private final FmeaScaleService service;

    public FmeaScaleController(FmeaScaleService service) {
        this.service = service;
    }

    /** Les trois échelles, redéfinies par le tenant ou de référence. */
    @GetMapping
    public FmeaScaleDto.ReferenceView findAll() {
        return service.findAll();
    }

    @GetMapping("/{kind}")
    public FmeaScaleDto.ScaleView find(@PathVariable FmeaScaleKind kind) {
        return service.find(kind);
    }

    /**
     * Remplace le barème d'une échelle, d'un bloc. Les dix lignes partent
     * ensemble : un remplacement partiel laisserait un score sans définition.
     */
    @PutMapping("/{kind}")
    @PreAuthorize(EDIT_ROLES)
    public FmeaScaleDto.ScaleView replace(@PathVariable FmeaScaleKind kind,
                                          @Valid @RequestBody FmeaScaleDto.ScaleRequest request) {
        return service.replace(kind, request);
    }

    /** Revient au barème de référence. */
    @DeleteMapping("/{kind}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize(EDIT_ROLES)
    public FmeaScaleDto.ScaleView revert(@PathVariable FmeaScaleKind kind) {
        return service.revertToReference(kind);
    }
}
