package com.openlab.qualitos.quality.ideas.web;

import com.openlab.qualitos.quality.ideas.application.IdeaDto;
import com.openlab.qualitos.quality.ideas.application.IdeaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * La boîte à idées.
 *
 * <p><b>Déposer et voter sont ouverts à tout utilisateur authentifié ; arbitrer
 * ne l'est pas.</b> Une boîte à idées réservée aux profils qualité cesse d'être
 * une boîte à idées : c'est l'opérateur qui voit le rebut à son poste. Mais
 * retenir une idée engage l'organisation, et l'écarter la retire de la vue de
 * tous : ce sont des actes de pilotage.
 */
@RestController
@RequestMapping("/api/v1/ideas")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Ideas", description = "Idea box")
public class IdeaController {

    /** Qui arbitre. Exactement la liste des autres référentiels de méthode. */
    private static final String ROLES_ARBITRAGE =
            "hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')";

    private final IdeaService service;

    public IdeaController(IdeaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "The tenant's idea board, one column per status")
    public IdeaDto.BoardView board() {
        return service.board();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit an idea, with or without a circle")
    public IdeaDto.IdeaView submit(@Valid @RequestBody IdeaWebDto.SubmitRequest requete) {
        return service.submit(new IdeaDto.SubmitCommand(
                requete.title(), requete.description(), requete.circleId()));
    }

    @PostMapping("/{ideaId}/vote")
    @Operation(summary = "Back an idea")
    public IdeaDto.IdeaView vote(@PathVariable UUID ideaId) {
        return service.vote(ideaId);
    }

    @DeleteMapping("/{ideaId}/vote")
    @Operation(summary = "Withdraw one's backing")
    public IdeaDto.IdeaView unvote(@PathVariable UUID ideaId) {
        return service.unvote(ideaId);
    }

    @PatchMapping("/{ideaId}/review")
    @PreAuthorize(ROLES_ARBITRAGE)
    @Operation(summary = "Move an idea under review")
    public IdeaDto.IdeaView review(@PathVariable UUID ideaId) {
        return service.review(ideaId);
    }

    @PatchMapping("/{ideaId}/approve")
    @PreAuthorize(ROLES_ARBITRAGE)
    @Operation(summary = "Approve an idea")
    public IdeaDto.IdeaView approve(@PathVariable UUID ideaId) {
        return service.approve(ideaId);
    }

    @PatchMapping("/{ideaId}/reject")
    @PreAuthorize(ROLES_ARBITRAGE)
    @Operation(summary = "Turn an idea down, with a reason")
    public IdeaDto.IdeaView reject(@PathVariable UUID ideaId,
                                   @Valid @RequestBody IdeaWebDto.RejectRequest requete) {
        return service.reject(ideaId, new IdeaDto.RejectCommand(requete.reason()));
    }

    @PatchMapping("/{ideaId}/implement")
    @PreAuthorize(ROLES_ARBITRAGE)
    @Operation(summary = "Mark an idea as implemented")
    public IdeaDto.IdeaView implement(@PathVariable UUID ideaId) {
        return service.implement(ideaId);
    }

    @PatchMapping("/{ideaId}/impact")
    @PreAuthorize(ROLES_ARBITRAGE)
    @Operation(summary = "Record the measured impact")
    public IdeaDto.IdeaView measure(@PathVariable UUID ideaId,
                                    @Valid @RequestBody IdeaWebDto.ImpactRequest requete) {
        return service.measure(ideaId, new IdeaDto.ImpactCommand(requete.impactNote()));
    }
}
