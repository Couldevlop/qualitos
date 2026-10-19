package com.openlab.qualitos.quality.docs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentCatalogSeederTest {

    @Mock DocumentSettingsRowRepository settings;
    @Mock DocumentFolderRepository folders;
    @Mock DocumentProcessRepository processes;

    static final UUID TENANT = UUID.randomUUID();

    @Test
    void dejaSeme_neFaitRien() {
        when(settings.existsById(TENANT)).thenReturn(true);
        new DocumentCatalogSeeder(settings, folders, processes).seed(TENANT);
        verify(settings, never()).saveAndFlush(any());
        verifyNoInteractions(folders, processes);
    }

    @Test
    void premierUsage_semeParametrageArborescenceEtProcessus() {
        when(settings.existsById(TENANT)).thenReturn(false);
        when(folders.save(any())).thenAnswer(inv -> {
            DocumentFolder f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        new DocumentCatalogSeeder(settings, folders, processes).seed(TENANT);

        ArgumentCaptor<DocumentSettingsRow> row = ArgumentCaptor.forClass(DocumentSettingsRow.class);
        verify(settings).saveAndFlush(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(TENANT);

        ArgumentCaptor<DocumentFolder> f = ArgumentCaptor.forClass(DocumentFolder.class);
        verify(folders, times(8)).save(f.capture());
        List<DocumentFolder> saved = f.getAllValues();
        assertThat(saved).allMatch(x -> x.getTenantId().equals(TENANT));
        assertThat(saved.stream().filter(x -> x.getParentId() == null).map(DocumentFolder::getName))
                .containsExactly("Manuel qualité", "Procédures", "Instructions de travail",
                        "Formulaires et modèles", "Enregistrements", "Documents externes");
        assertThat(saved.stream().filter(x -> x.getParentId() == null).map(DocumentFolder::getPosition))
                .containsExactly(0, 1, 2, 3, 4, 5);
        DocumentFolder procedures = saved.get(1);
        assertThat(saved.get(2).getParentId()).isEqualTo(procedures.getId());
        assertThat(saved.get(2).getName()).isEqualTo("Processus de réalisation");
        assertThat(saved.get(2).getPosition()).isZero();
        assertThat(saved.get(3).getParentId()).isEqualTo(procedures.getId());
        assertThat(saved.get(3).getPosition()).isEqualTo(1);

        ArgumentCaptor<DocumentProcess> p = ArgumentCaptor.forClass(DocumentProcess.class);
        verify(processes, times(6)).save(p.capture());
        assertThat(p.getAllValues().stream().map(DocumentProcess::getName))
                .containsExactlyElementsOf(DocumentCatalogSeeder.DEFAULT_PROCESSES);
        assertThat(p.getAllValues().stream().map(DocumentProcess::getPosition)).containsExactly(0, 1, 2, 3, 4, 5);
        assertThat(p.getAllValues()).allMatch(x -> x.getTenantId().equals(TENANT));
    }
}
