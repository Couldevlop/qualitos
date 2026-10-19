package com.openlab.qualitos.quality.docs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentBlobObjectOwnerTest {

    @Test
    void revendiqueLesClesConnuesDeDocumentBlobs() {
        DocumentBlobRepository blobs = mock(DocumentBlobRepository.class);
        when(blobs.existsByObjectKey("tenants/t/documents/blobs/a")).thenReturn(true);
        DocumentBlobObjectOwner owner = new DocumentBlobObjectOwner(blobs);

        assertThat(owner.isReferenced("tenants/t/documents/blobs/a")).isTrue();
        assertThat(owner.isReferenced("tenants/t/documents/blobs/b")).isFalse();
        assertThat(owner.ownerName()).isEqualTo("document-blob");
    }
}
