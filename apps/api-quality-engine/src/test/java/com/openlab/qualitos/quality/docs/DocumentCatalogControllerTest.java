package com.openlab.qualitos.quality.docs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = DocumentCatalogController.class)
class DocumentCatalogControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DocumentCatalogService catalog;
    @MockitoBean DocumentFileService files;

    static final UUID ID = UUID.randomUUID();
    static final UUID PARENT = UUID.randomUUID();

    // ---------- arborescence ----------

    @Test @WithMockUser(roles = "USER")
    void folders_200() throws Exception {
        when(catalog.folders()).thenReturn(List.of(new DocumentDto.FolderView(ID, null, "Procédures", 1)));
        mockMvc.perform(get("/api/v1/documents/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Procédures"));
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void createFolder_201() throws Exception {
        when(catalog.createFolder(any())).thenReturn(new DocumentDto.FolderView(ID, PARENT, "Achats", 0));
        mockMvc.perform(post("/api/v1/documents/folders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Achats\",\"parentId\":\"" + PARENT + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));
        verify(catalog).createFolder(new DocumentDto.FolderRequest("Achats", PARENT, null));
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void createFolder_nomVide_400() throws Exception {
        mockMvc.perform(post("/api/v1/documents/folders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(catalog);
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void createFolder_parentInconnu_404() throws Exception {
        when(catalog.createFolder(any())).thenThrow(new DocumentCatalogNotFoundException("Folder", PARENT));
        mockMvc.perform(post("/api/v1/documents/folders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"parentId\":\"" + PARENT + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void createFolder_tropProfond_409() throws Exception {
        when(catalog.createFolder(any())).thenThrow(new DocumentStateException("too deep"));
        mockMvc.perform(post("/api/v1/documents/folders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void updateFolder_200() throws Exception {
        when(catalog.updateFolder(eq(ID), any())).thenReturn(new DocumentDto.FolderView(ID, null, "Renommé", 2));
        mockMvc.perform(put("/api/v1/documents/folders/" + ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Renommé\",\"position\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2));
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void deleteFolder_204() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/folders/" + ID).with(csrf()))
                .andExpect(status().isNoContent());
        verify(catalog).deleteFolder(ID);
    }

    // ---------- processus ----------

    @Test @WithMockUser(roles = "USER")
    void processes_200() throws Exception {
        when(catalog.processes()).thenReturn(List.of(new DocumentDto.ProcessView(ID, "Achats", 0)));
        mockMvc.perform(get("/api/v1/documents/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Achats"));
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void createProcess_201_et_nomVide_400() throws Exception {
        when(catalog.createProcess(any())).thenReturn(new DocumentDto.ProcessView(ID, "Achats", 0));
        mockMvc.perform(post("/api/v1/documents/processes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Achats\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/documents/processes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        verify(catalog, times(1)).createProcess(any());
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void updateProcess_200() throws Exception {
        when(catalog.updateProcess(eq(ID), any())).thenReturn(new DocumentDto.ProcessView(ID, "Support", 1));
        mockMvc.perform(put("/api/v1/documents/processes/" + ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Support\",\"position\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Support"));
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void deleteProcess_204_et_utilise_409() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/processes/" + ID).with(csrf()))
                .andExpect(status().isNoContent());
        UUID used = UUID.randomUUID();
        doThrow(new DocumentStateException("used")).when(catalog).deleteProcess(used);
        mockMvc.perform(delete("/api/v1/documents/processes/" + used).with(csrf()))
                .andExpect(status().isConflict());
    }

    // ---------- paramétrage ----------

    @Test @WithMockUser(roles = "USER")
    void uploadPolicy_200() throws Exception {
        when(catalog.uploadPolicy()).thenReturn(new DocumentDto.UploadPolicy(25,
                List.of("application/pdf"), List.of("pdf")));
        mockMvc.perform(get("/api/v1/documents/upload-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxFileMb").value(25))
                .andExpect(jsonPath("$.allowedExtensions[0]").value("pdf"));
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void settings_200() throws Exception {
        when(catalog.settings()).thenReturn(new DocumentDto.SettingsView(24, 30, 25, 50, null, null));
        mockMvc.perform(get("/api/v1/documents/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformMaxFileMb").value(50));
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void replaceSettings_200_et_horsBornes_400() throws Exception {
        when(catalog.replaceSettings(any())).thenReturn(new DocumentDto.SettingsView(12, 15, 20, 50, 1000, 7));
        mockMvc.perform(put("/api/v1/documents/settings").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewPeriodMonths\":12,\"reviewWarningDays\":15,\"maxFileMb\":20,"
                                + "\"quotaMb\":1000,\"obsoleteRetentionYears\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotaMb").value(1000));
        verify(catalog).replaceSettings(new DocumentDto.SettingsForm(12, 15, 20, 1000, 7));

        mockMvc.perform(put("/api/v1/documents/settings").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewPeriodMonths\":12,\"reviewWarningDays\":15,\"maxFileMb\":51}"))
                .andExpect(status().isBadRequest());
        verify(catalog, times(1)).replaceSettings(any());
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void storage_200() throws Exception {
        when(files.usage()).thenReturn(new DocumentDto.StorageUsage(3, 300, 200, 100, 2048L));
        mockMvc.perform(get("/api/v1/documents/storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storedBytes").value(100))
                .andExpect(jsonPath("$.quotaBytes").value(2048));
    }
}
