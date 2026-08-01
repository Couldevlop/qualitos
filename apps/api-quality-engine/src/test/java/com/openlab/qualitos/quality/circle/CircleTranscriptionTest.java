package com.openlab.qualitos.quality.circle;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.aigateway.AiTranscriptionResult;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Transcription d'un enregistrement de réunion de Cercle (§3.3).
 *
 * <p>La capacité Whisper existait côté ai-service mais aucun appelant ne l'utilisait :
 * l'animateur devait saisir le transcript à la main. Ces tests couvrent le garde-fou de
 * taille (LLM04) et la validation d'entrée, qui doivent s'appliquer AVANT tout appel
 * réseau.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CircleTranscriptionTest {

    @Mock QualityCircleRepository circleRepository;
    @Mock CircleMemberRepository memberRepository;
    @Mock CircleMeetingRepository meetingRepository;
    @Mock CircleProposalRepository proposalRepository;
    @Mock AiGatewayClient ai;

    CircleService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID CIRCLE_ID = UUID.randomUUID();
    static final UUID MEETING_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT.toString());
        service = new CircleService(circleRepository, memberRepository, meetingRepository,
                proposalRepository, ai, new com.fasterxml.jackson.databind.ObjectMapper());

        QualityCircle circle = new QualityCircle();
        circle.setId(CIRCLE_ID);
        circle.setTenantId(TENANT);
        when(circleRepository.findByIdAndTenantId(CIRCLE_ID, TENANT)).thenReturn(Optional.of(circle));

        CircleMeeting meeting = new CircleMeeting();
        meeting.setId(MEETING_ID);
        meeting.setCircle(circle);
        when(meetingRepository.findByIdAndCircleId(MEETING_ID, CIRCLE_ID))
                .thenReturn(Optional.of(meeting));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void transcrit_l_enregistrement_et_renvoie_le_texte_pour_relecture() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "reunion.webm", "audio/webm", new byte[] { 1, 2, 3 });
        when(ai.transcribe(eq("reunion.webm"), any(), eq("fr")))
                .thenReturn(new AiTranscriptionResult("Bonjour à tous.", "fr", 4200));

        CircleDto.MeetingTranscript transcript =
                service.transcribeMeeting(CIRCLE_ID, MEETING_ID, file, "fr");

        assertThat(transcript.text()).isEqualTo("Bonjour à tous.");
        assertThat(transcript.language()).isEqualTo("fr");
        assertThat(transcript.durationMs()).isEqualTo(4200);
    }

    @Test
    void refuse_un_fichier_absent_sans_appeler_la_passerelle() {
        assertThatThrownBy(() -> service.transcribeMeeting(CIRCLE_ID, MEETING_ID, null, null))
                .isInstanceOf(CircleStateException.class);
        verify(ai, never()).transcribe(any(), any(), any());
    }

    @Test
    void refuse_un_fichier_vide_sans_appeler_la_passerelle() {
        MockMultipartFile empty = new MockMultipartFile("file", "vide.webm", "audio/webm", new byte[0]);

        assertThatThrownBy(() -> service.transcribeMeeting(CIRCLE_ID, MEETING_ID, empty, null))
                .isInstanceOf(CircleStateException.class);
        verify(ai, never()).transcribe(any(), any(), any());
    }

    @Test
    void refuse_un_enregistrement_trop_volumineux_avant_tout_appel_reseau() {
        // Le garde-fou doit rejeter AVANT de transporter le fichier (anti-DoS, LLM04).
        byte[] tooBig = new byte[(int) CircleService.MAX_AUDIO_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile("file", "long.webm", "audio/webm", tooBig);

        assertThatThrownBy(() -> service.transcribeMeeting(CIRCLE_ID, MEETING_ID, file, null))
                .isInstanceOf(CircleStateException.class)
                .hasMessageContaining("volumineux");
        verify(ai, never()).transcribe(any(), any(), any());
    }
}
