package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.openlab.qualitos.quality.capa.CapaTransition;
import com.openlab.qualitos.quality.capa.CapaTransitionEvent;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'écouteur qui referme la boucle entre un dossier CAPA clôturé et l'exigence
 * qui l'avait motivé.
 */
class StakeholderCapaListenerTest {

    private static final UUID TENANT = UUID.randomUUID();

    private StakeholderService service;
    private StakeholderCapaListener listener;

    @BeforeEach
    void setUp() {
        service = mock(StakeholderService.class);
        listener = new StakeholderCapaListener(service);
    }

    @Test
    void uneClotureRequalifieLesExigencesDuDossier() {
        UUID capa = UUID.randomUUID();
        when(service.onCapaClosed(TENANT, capa)).thenReturn(1);

        listener.onCapaTransition(event(CapaTransition.CLOSED, capa.toString()));

        verify(service).onCapaClosed(TENANT, capa);
    }

    @Test
    void lIdentifiantSeLitAussiCommeUuid() {
        UUID capa = UUID.randomUUID();

        listener.onCapaTransition(event(CapaTransition.CLOSED, capa));

        verify(service).onCapaClosed(TENANT, capa);
    }

    @Test
    void lesAutresTransitionsNeTouchentRien() {
        listener.onCapaTransition(event(CapaTransition.OPENED, UUID.randomUUID().toString()));
        listener.onCapaTransition(event(CapaTransition.RESOLVED, UUID.randomUUID().toString()));

        verify(service, never()).onCapaClosed(any(), any());
    }

    @Test
    void unIdentifiantAbsentOuIllisibleNeDeclencheRien() {
        listener.onCapaTransition(event(CapaTransition.CLOSED, null));
        listener.onCapaTransition(event(CapaTransition.CLOSED, "  "));
        listener.onCapaTransition(event(CapaTransition.CLOSED, "pas-un-uuid"));
        listener.onCapaTransition(event(CapaTransition.CLOSED, 42));

        verify(service, never()).onCapaClosed(any(), any());
    }

    @Test
    void unEchecDeRequalificationNeRemontePas() {
        // La clôture est validée et ne se rejoue pas : l'échec d'un effet de
        // bord ne doit pas la faire paraître manquée.
        UUID capa = UUID.randomUUID();
        doThrow(new IllegalStateException("base indisponible"))
                .when(service).onCapaClosed(TENANT, capa);

        assertThatCode(() -> listener.onCapaTransition(event(CapaTransition.CLOSED, capa.toString())))
                .doesNotThrowAnyException();
        verify(service).onCapaClosed(TENANT, capa);
    }

    private static CapaTransitionEvent event(CapaTransition transition, Object id) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id);
        return new CapaTransitionEvent(TENANT, transition, payload);
    }
}
