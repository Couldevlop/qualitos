package com.openlab.qualitos.quality.visiongateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link VisionImageValidator} — validation taille / type /
 * magic bytes mutualisée entre {@code VisionController} et {@code FiveSController}.
 */
class VisionImageValidatorTest {

    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x11};

    @Test
    void validPng_isAccepted() throws Exception {
        MockMultipartFile f = new MockMultipartFile("image", "z.png", "image/png", PNG_MAGIC);
        VisionImageValidator.ValidatedImage v = VisionImageValidator.validate(f, null);
        assertThat(v.contentType()).isEqualTo("image/png");
        assertThat(v.filename()).isEqualTo("z.png");
        assertThat(v.bytes()).isEqualTo(PNG_MAGIC);
    }

    @Test
    void validJpeg_isAccepted() throws Exception {
        MockMultipartFile f = new MockMultipartFile("image", "z.jpg", "IMAGE/JPEG", JPEG_MAGIC);
        assertThat(VisionImageValidator.validate(f, null).contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void fileAlias_isUsedWhenImageMissing() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "z.png", "image/png", PNG_MAGIC);
        assertThat(VisionImageValidator.validate(null, f).filename()).isEqualTo("z.png");
    }

    @Test
    void missingPart_throwsValidation() {
        assertThatThrownBy(() -> VisionImageValidator.validate(null, null))
                .isInstanceOf(VisionImageValidationException.class);
    }

    @Test
    void emptyPart_throwsValidation() {
        MockMultipartFile empty = new MockMultipartFile("image", "z.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> VisionImageValidator.validate(empty, null))
                .isInstanceOf(VisionImageValidationException.class);
    }

    @Test
    void unsupportedType_throwsValidation() {
        MockMultipartFile gif = new MockMultipartFile("image", "z.gif", "image/gif", PNG_MAGIC);
        assertThatThrownBy(() -> VisionImageValidator.validate(gif, null))
                .isInstanceOf(VisionImageValidationException.class);
    }

    @Test
    void declaredTypeButWrongMagic_throwsValidation() {
        MockMultipartFile bogus = new MockMultipartFile(
                "image", "fake.png", "image/png", "not an image".getBytes());
        assertThatThrownBy(() -> VisionImageValidator.validate(bogus, null))
                .isInstanceOf(VisionImageValidationException.class);
    }

    @Test
    void oversize_throwsTooLarge() {
        byte[] big = new byte[(int) VisionImageValidator.MAX_SIZE_BYTES + 1];
        big[0] = (byte) 0x89; big[1] = 0x50; big[2] = 0x4E; big[3] = 0x47;
        big[4] = 0x0D; big[5] = 0x0A; big[6] = 0x1A; big[7] = 0x0A;
        MockMultipartFile f = new MockMultipartFile("image", "big.png", "image/png", big);
        assertThatThrownBy(() -> VisionImageValidator.validate(f, null))
                .isInstanceOf(VisionImageTooLargeException.class);
    }

    @Test
    void webp_requiresRiffAndWebpMarker() throws Exception {
        byte[] webp = {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 0x00};
        MockMultipartFile ok = new MockMultipartFile("image", "z.webp", "image/webp", webp);
        assertThat(VisionImageValidator.validate(ok, null).contentType()).isEqualTo("image/webp");

        byte[] riffOnly = {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 'X', 'X', 'X', 'X', 0x00};
        MockMultipartFile bad = new MockMultipartFile("image", "z.webp", "image/webp", riffOnly);
        assertThatThrownBy(() -> VisionImageValidator.validate(bad, null))
                .isInstanceOf(VisionImageValidationException.class);
    }
}
