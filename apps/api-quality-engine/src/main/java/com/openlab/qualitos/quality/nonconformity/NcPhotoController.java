package com.openlab.qualitos.quality.nonconformity;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Endpoints de stockage binaire des photos d'une Non-Conformité (§4.3).
 * Tenant toujours issu du JWT (via le service). Quand le stockage objet est
 * désactivé, toutes les routes répondent 503 (StorageDisabledException).
 */
@RestController
@RequestMapping("/api/v1/nc/{id}/photos")
public class NcPhotoController {

    private final NcPhotoService service;

    public NcPhotoController(NcPhotoService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public NcPhotoDto.Response upload(@PathVariable UUID id,
                                      @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new NcPhotoValidationException("Missing or empty 'file' part");
        }
        return service.upload(id, file.getContentType(), file.getOriginalFilename(), file.getBytes());
    }

    @GetMapping
    public List<NcPhotoDto.ListItem> list(@PathVariable UUID id) {
        return service.list(id);
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @PathVariable UUID photoId) {
        service.delete(id, photoId);
    }
}
