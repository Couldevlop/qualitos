package com.openlab.qualitos.quality.visiongateway;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("web")
@WebMvcTest(controllers = VisionController.class)
class VisionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean VisionGatewayClient gateway;

    /** PNG minimal valide (magic bytes corrects). */
    private static byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private MockMultipartFile image(String contentType, byte[] bytes) {
        return new MockMultipartFile("image", "shop.png", contentType, bytes);
    }

    private static VisionDto.VisionAnalysis sample() {
        return new VisionDto.VisionAnalysis("sha", 1280, 720,
                new VisionDto.VisionScore(80, 70, 60, 90, 50, 70),
                List.of(new VisionDto.VisionFinding("SEIRI", "clutter", "HIGH", 0.9, List.of(1, 2, 3, 4))));
    }

    @Test @WithMockUser
    void analyze_validImage_returns200() throws Exception {
        when(gateway.analyze(eq("image/png"), any(), any())).thenReturn(sample());
        mockMvc.perform(multipart("/api/v1/vision/5s/analyze").file(image("image/png", png())).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageSha256").value("sha"))
                .andExpect(jsonPath("$.width").value(1280))
                .andExpect(jsonPath("$.score.overall").value(70))
                .andExpect(jsonPath("$.findings[0].pillar").value("SEIRI"))
                .andExpect(jsonPath("$.findings[0].bbox[2]").value(3));
    }

    @Test @WithMockUser
    void analyze_fileFieldAlias_returns200() throws Exception {
        when(gateway.analyze(eq("image/png"), any(), any())).thenReturn(sample());
        MockMultipartFile asFile = new MockMultipartFile("file", "shop.png", "image/png", png());
        mockMvc.perform(multipart("/api/v1/vision/5s/analyze").file(asFile).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score.overall").value(70));
    }

    @Test @WithMockUser
    void analyze_missingPart_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/vision/5s/analyze").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/vision-image-invalid"));
        verifyNoInteractions(gateway);
    }

    @Test @WithMockUser
    void analyze_unsupportedType_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/vision/5s/analyze")
                        .file(image("application/pdf", new byte[]{1, 2, 3, 4})).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/vision-image-invalid"));
        verifyNoInteractions(gateway);
    }

    @Test @WithMockUser
    void analyze_typeMismatchMagicBytes_returns400() throws Exception {
        // content-type image/png mais octets non-PNG → rejet (falsification du type déclaré).
        mockMvc.perform(multipart("/api/v1/vision/5s/analyze")
                        .file(image("image/png", new byte[]{0x00, 0x01, 0x02, 0x03})).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/vision-image-invalid"));
        verifyNoInteractions(gateway);
    }

    @Test @WithMockUser
    void analyze_tooLarge_returns413() throws Exception {
        byte[] big = new byte[(int) VisionController.MAX_SIZE_BYTES + 1];
        big[0] = (byte) 0x89; big[1] = 0x50; big[2] = 0x4E; big[3] = 0x47;
        mockMvc.perform(multipart("/api/v1/vision/5s/analyze")
                        .file(image("image/png", big)).with(csrf()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/vision-image-too-large"));
        verifyNoInteractions(gateway);
    }

    @Test @WithMockUser
    void analyze_serviceOff_returns503() throws Exception {
        when(gateway.analyze(any(), any(), any()))
                .thenThrow(new VisionUnavailableException("disabled"));
        mockMvc.perform(multipart("/api/v1/vision/5s/analyze").file(image("image/png", png())).with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/vision-unavailable"));
    }

    @Test @WithMockUser
    void analyze_gatewayError_returns502() throws Exception {
        when(gateway.analyze(any(), any(), any()))
                .thenThrow(new VisionGatewayException("empty response"));
        mockMvc.perform(multipart("/api/v1/vision/5s/analyze").file(image("image/png", png())).with(csrf()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/vision-gateway-error"));
    }
}
