package com.openlab.qualitos.quality.industry;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/industry-packs")
public class IndustryPackController {

    private final IndustryPackService service;

    public IndustryPackController(IndustryPackService service) { this.service = service; }

    @GetMapping
    public Page<IndustryPackDto.PackResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return service.listAll(pageable);
    }

    @GetMapping("/{code}")
    public IndustryPackDto.PackResponse get(@PathVariable String code) {
        return service.getByCode(code);
    }

    @PostMapping("/{code}/activate")
    @ResponseStatus(HttpStatus.OK)
    public IndustryPackDto.ActivationResponse activate(
            @PathVariable String code,
            @Valid @RequestBody IndustryPackDto.ActivateRequest req) {
        return service.activate(code, req);
    }

    @DeleteMapping("/{code}/activate")
    public IndustryPackDto.ActivationResponse deactivate(@PathVariable String code) {
        // H2 (OWASP A01) : l'acteur de la désactivation est dérivé du JWT côté service,
        // plus du paramètre de requête falsifiable 'deactivatedBy' (retiré).
        return service.deactivate(code);
    }

    @GetMapping("/my")
    public List<IndustryPackDto.ActivationResponse> mine() {
        return service.myActiveActivations();
    }

    @GetMapping("/my/history")
    public List<IndustryPackDto.ActivationResponse> myHistory() {
        return service.myActivationHistory();
    }
}
