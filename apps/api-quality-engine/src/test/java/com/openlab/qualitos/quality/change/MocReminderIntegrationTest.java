package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import com.openlab.qualitos.quality.change.domain.ImpactLevel;
import com.openlab.qualitos.quality.change.domain.MocHistoryEvent;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.openlab.qualitos.quality.change.domain.MocClassification.CRITICAL;
import static com.openlab.qualitos.quality.change.domain.MocClassification.MAJOR;
import static com.openlab.qualitos.quality.change.domain.MocClassification.MINOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Le rappel aux validateurs : délai, répétition, absence de doublon, arrêt à la
 * décision, délai réglé par le client, isolation entre clients. L'horloge du
 * rappel est décalée de N jours par rapport à celle de la soumission.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class MocReminderIntegrationTest {

    @Autowired MocService service;
    @Autowired MocSettingsService settings;
    @Autowired ChangeValidationRepository validations;
    @Autowired ChangeRequestRepository requests;
    @Autowired ChangeHistoryRepository history;
    @Autowired MocAuditTrail audit;
    @MockitoBean MocNotifier notifier;

    final UUID demandeur = UUID.randomUUID();
    final UUID hse = UUID.randomUUID();
    final UUID chef = UUID.randomUUID();

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    static void connecte(UUID user) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(user.toString()).claim("name", "U").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    MocReminderService rappel(int joursPlusTard) {
        return new MocReminderService(validations, requests, history, settings, notifier, audit,
                Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(joursPlusTard)));
    }

    /** Un client paramétré (délai donné) et une demande mineure soumise : deux cartes, chef et HSE. */
    UUID soumise(UUID tenant, int delai) {
        TenantContext.setTenantId(tenant.toString());
        connecte(UUID.randomUUID());
        Map<ImpactDomain, Integer> poids = new EnumMap<>(ImpactDomain.class);
        for (ImpactDomain d : ImpactDomain.values()) poids.put(d, 1);
        settings.replace(new MocDto.SettingsForm(25, 50, delai, poids, List.of(
                new MocDto.RoleForm(null, "Chef de service", EnumSet.of(MINOR, MAJOR, CRITICAL),
                        List.of(new MocDto.HolderForm(chef, "chef@acme.test"))),
                new MocDto.RoleForm(null, "HSE", EnumSet.of(MINOR, MAJOR, CRITICAL),
                        List.of(new MocDto.HolderForm(hse, "hse@acme.test"))))));
        connecte(demandeur);
        MocDto.Detail d = service.create(new MocDto.RequestForm("Four", ChangeRequestType.PERMANENT,
                null, null, null, null));
        service.sendToAnalysis(d.id());
        connecte(chef);
        for (ImpactDomain dom : ImpactDomain.values()) {
            service.assess(d.id(), dom, new MocDto.ImpactForm(ImpactLevel.NONE, null));
        }
        service.submit(d.id());
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        clearInvocations(notifier);
        return d.id();
    }

    long rappelsTraces(UUID tenant, UUID changeId) {
        return history.findByTenantIdAndChangeIdOrderByOccurredAtDesc(tenant, changeId).stream()
                .filter(h -> h.getEvent() == MocHistoryEvent.REMINDER_SENT).count();
    }

    @Test
    void rienAvantLeDelaiPuisUnRappelAJ3RepeteAJ6SansDoublon() {
        UUID tenant = UUID.randomUUID();
        UUID id = soumise(tenant, 3);

        rappel(2).sendDueReminders();
        verify(notifier, never()).reminder(argThat(c -> c.getId().equals(id)), any(), anyInt());

        rappel(3).sendDueReminders();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MocNotifier.Recipient>> dest = ArgumentCaptor.forClass(Collection.class);
        verify(notifier).reminder(argThat(c -> c.getId().equals(id)), dest.capture(), eq(3));
        assertThat(dest.getValue()).extracting(MocNotifier.Recipient::userId).containsExactlyInAnyOrder(chef, hse);
        assertThat(rappelsTraces(tenant, id)).isEqualTo(1);
        assertThat(history.findByTenantIdAndChangeIdOrderByOccurredAtDesc(tenant, id).get(0).getDetail())
                .isEqualTo("Chef de service, HSE");

        // Même jour : pas de doublon.
        rappel(3).sendDueReminders();
        rappel(4).sendDueReminders();
        verify(notifier, times(1)).reminder(argThat(c -> c.getId().equals(id)), any(), anyInt());

        rappel(6).sendDueReminders();
        verify(notifier).reminder(argThat(c -> c.getId().equals(id)), any(), eq(6));
        assertThat(rappelsTraces(tenant, id)).isEqualTo(2);
    }

    @Test
    void leRappelCesseDesQueLeRoleAStatueOuQueLaDemandeEstClose() {
        UUID tenant = UUID.randomUUID();
        UUID id = soumise(tenant, 3);

        TenantContext.setTenantId(tenant.toString());
        connecte(hse);
        UUID carteHse = service.get(id).validations().stream().filter(v -> v.roleLabel().equals("HSE"))
                .findFirst().orElseThrow().id();
        service.decide(id, carteHse, new MocDto.DecisionForm(ApprovalDecision.APPROVED, null));
        clear();

        rappel(3).sendDueReminders();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MocNotifier.Recipient>> dest = ArgumentCaptor.forClass(Collection.class);
        verify(notifier).reminder(argThat(c -> c.getId().equals(id)), dest.capture(), eq(3));
        assertThat(dest.getValue()).extracting(MocNotifier.Recipient::userId).containsExactly(chef);

        TenantContext.setTenantId(tenant.toString());
        connecte(chef);
        UUID carteChef = service.get(id).validations().stream().filter(v -> v.roleLabel().equals("Chef de service"))
                .findFirst().orElseThrow().id();
        service.decide(id, carteChef, new MocDto.DecisionForm(ApprovalDecision.REJECTED, "Non"));
        clear();
        clearInvocations(notifier);

        rappel(9).sendDueReminders();
        verify(notifier, never()).reminder(argThat(c -> c.getId().equals(id)), any(), anyInt());
    }

    @Test
    void leDelaiDuClientEstRespecte() {
        UUID tenant = UUID.randomUUID();
        UUID id = soumise(tenant, 5);
        rappel(3).sendDueReminders();
        rappel(4).sendDueReminders();
        verify(notifier, never()).reminder(argThat(c -> c.getId().equals(id)), any(), anyInt());
        rappel(5).sendDueReminders();
        verify(notifier).reminder(argThat(c -> c.getId().equals(id)), any(), eq(5));
    }

    @Test
    void chaqueClientEstTraiteDansSonContexteEtRienNeDeborde() {
        UUID clientA = UUID.randomUUID();
        UUID clientB = UUID.randomUUID();
        UUID a = soumise(clientA, 3);
        UUID b = soumise(clientB, 10);

        List<String> contextes = new ArrayList<>();
        AtomicReference<String> vu = new AtomicReference<>();
        doAnswer(inv -> {
            ChangeRequest c = inv.getArgument(0);
            contextes.add(c.getTenantId() + "=" + TenantContext.getTenantId());
            vu.set(TenantContext.getTenantId());
            return null;
        }).when(notifier).reminder(any(), any(), anyInt());

        rappel(3).sendDueReminders();

        // A (délai 3) est rappelé dans SON contexte ; B (délai 10) ne l'est pas encore.
        assertThat(contextes).contains(clientA + "=" + clientA);
        assertThat(contextes).noneMatch(s -> s.startsWith(clientB.toString()));
        verify(notifier).reminder(argThat(c -> c.getId().equals(a)), any(), eq(3));
        verify(notifier, never()).reminder(argThat(c -> c.getId().equals(b)), any(), anyInt());
        assertThat(rappelsTraces(clientB, b)).isZero();
        // Le contexte est retiré après le passage.
        assertThat(TenantContext.hasTenant()).isFalse();
        assertThat(vu.get()).isEqualTo(clientA.toString());
    }
}
