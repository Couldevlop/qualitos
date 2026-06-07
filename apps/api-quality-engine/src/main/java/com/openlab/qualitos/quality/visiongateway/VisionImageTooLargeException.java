package com.openlab.qualitos.quality.visiongateway;

/**
 * Image dépassant le plafond applicatif (aligné sur la limite multipart, 10 Mo).
 * Mappé en {@code 413 Payload Too Large} (type {@code vision-image-too-large}) par le
 * {@code GlobalExceptionHandler}.
 */
public class VisionImageTooLargeException extends RuntimeException {
    public VisionImageTooLargeException(long actualBytes, long maxBytes) {
        super("Image of " + actualBytes + " bytes exceeds the maximum of " + maxBytes + " bytes");
    }
}
