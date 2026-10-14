package com.openlab.qualitos.quality.smi.web;

import com.openlab.qualitos.quality.smi.application.SmiDto;
import com.openlab.qualitos.quality.smi.application.SmiStandardsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Ajouter une norme au système de management intégré.
 *
 * <p>Choisir les normes que le client pilote est un PARAMÉTRAGE, et tout
 * paramétrage revient à l'administrateur du client — et à lui seul : ni le
 * manager qualité ni l'éditeur de la plateforme. La règle porte sur le jeton
 * seul ; elle est donc décidée avant la lecture du corps (ADR 0065) et un
 * appelant sans ce rôle reçoit 403 quoi qu'il envoie. Les alias acceptés sont
 * ceux du rôle système {@code ADMIN_TENANT} (ADR 0020).
 *
 * <p>Le client vient du jeton ; rien dans la requête ne le désigne.
 */
@RestController
@RequestMapping("/api/v1/smi/standards")
@PreAuthorize(SmiStandardsController.TENANT_ADMIN)
public class SmiStandardsController {

    static final String TENANT_ADMIN = "hasAnyRole('ADMIN_TENANT','ADMIN','TENANT_ADMIN')";

    private final SmiStandardsService service;

    public SmiStandardsController(SmiStandardsService service) {
        this.service = service;
    }

    /** Les normes de système de management encore à adopter, les suggérées en tête. */
    @GetMapping("/candidates")
    @Transactional(readOnly = true)
    public List<SmiDto.Candidate> candidates() {
        return service.candidates();
    }

    /** Adopte la norme par le Standards Hub ; rend l'adoption créée. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SmiDto.StandardRef add(@Valid @RequestBody AddStandardRequest request) {
        return service.add(request.standardId());
    }

    public record AddStandardRequest(@NotNull UUID standardId) {}
}
