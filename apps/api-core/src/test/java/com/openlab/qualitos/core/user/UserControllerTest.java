package com.openlab.qualitos.core.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.core.common.GlobalExceptionHandler;
import com.openlab.qualitos.core.common.MissingTenantContextException;
import com.openlab.qualitos.core.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // JwtDecoder requis par SecurityConfig (oauth2ResourceServer + TenantJwtFilter)
    @MockBean
    private JwtDecoder jwtDecoder;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();

    private UserDto.Response sampleResponse() {
        return new UserDto.Response(
                USER_ID, TENANT_ID, "kc-abc", "alice@acme.com",
                Set.of("QUALITY_MANAGER"), true, Instant.now(), Instant.now());
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor superAdminJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor userJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class ListUsers {

        @Test
        @DisplayName("Returns 200 for ADMIN")
        void returns200ForAdmin() throws Exception {
            given(userService.findAll(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(sampleResponse())));

            mockMvc.perform(get("/api/v1/users").with(adminJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].email").value("alice@acme.com"));
        }

        @Test
        @DisplayName("Returns 200 for SUPER_ADMIN")
        void returns200ForSuperAdmin() throws Exception {
            given(userService.findAll(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(sampleResponse())));

            mockMvc.perform(get("/api/v1/users").with(superAdminJwt()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Returns 403 for USER role")
        void returns403ForUserRole() throws Exception {
            mockMvc.perform(get("/api/v1/users").with(userJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 when unauthenticated")
        void returns401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUser {

        @Test
        @DisplayName("Returns 200 with user for ADMIN")
        void returns200WithUser() throws Exception {
            given(userService.findById(USER_ID)).willReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/users/{id}", USER_ID).with(adminJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.email").value("alice@acme.com"));
        }

        @Test
        @DisplayName("Returns 404 when user not found")
        void returns404WhenNotFound() throws Exception {
            given(userService.findById(USER_ID))
                    .willThrow(new UserNotFoundException(USER_ID));

            mockMvc.perform(get("/api/v1/users/{id}", USER_ID).with(adminJwt()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/by-keycloak/{keycloakId}")
    class GetUserByKeycloakId {

        @Test
        @DisplayName("Returns 200 with user for ADMIN")
        void returns200WithUser() throws Exception {
            given(userService.findByKeycloakId("kc-abc")).willReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/users/by-keycloak/kc-abc").with(adminJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.keycloakId").value("kc-abc"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users")
    class CreateUser {

        @Test
        @DisplayName("Returns 201 with Location header when created")
        void returns201WithLocation() throws Exception {
            UserDto.CreateRequest request = new UserDto.CreateRequest(
                    "kc-new", "bob@acme.com", Set.of("USER"));

            given(userService.create(any())).willReturn(sampleResponse());

            mockMvc.perform(post("/api/v1/users")
                            .with(adminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("Returns 400 when keycloakId is blank")
        void returns400WhenKeycloakIdBlank() throws Exception {
            String body = """
                    {"keycloakId": "", "email": "bob@acme.com", "roles": ["USER"]}
                    """;

            mockMvc.perform(post("/api/v1/users")
                            .with(adminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 400 when email is invalid")
        void returns400WhenEmailInvalid() throws Exception {
            String body = """
                    {"keycloakId": "kc-new", "email": "not-an-email", "roles": ["USER"]}
                    """;

            mockMvc.perform(post("/api/v1/users")
                            .with(adminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 409 when keycloakId already exists")
        void returns409WhenConflict() throws Exception {
            UserDto.CreateRequest request = new UserDto.CreateRequest(
                    "kc-existing", "other@acme.com", Set.of("USER"));
            given(userService.create(any()))
                    .willThrow(new UserAlreadyExistsException("kc-existing"));

            mockMvc.perform(post("/api/v1/users")
                            .with(adminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Returns 401 when no tenant context in JWT")
        void returns401WhenMissingTenantContext() throws Exception {
            UserDto.CreateRequest request = new UserDto.CreateRequest(
                    "kc-new", "bob@acme.com", Set.of("USER"));
            given(userService.create(any()))
                    .willThrow(new MissingTenantContextException());

            mockMvc.perform(post("/api/v1/users")
                            .with(adminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id}")
    class UpdateUser {

        @Test
        @DisplayName("Returns 200 with updated user")
        void returns200WhenUpdated() throws Exception {
            UserDto.UpdateRequest request = new UserDto.UpdateRequest(
                    Set.of("AUDITOR"), true);
            UserDto.Response updated = new UserDto.Response(
                    USER_ID, TENANT_ID, "kc-abc", "alice@acme.com",
                    Set.of("AUDITOR"), true, Instant.now(), Instant.now());

            given(userService.update(eq(USER_ID), any())).willReturn(updated);

            mockMvc.perform(put("/api/v1/users/{id}", USER_ID)
                            .with(adminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles[0]").value("AUDITOR"));
        }

        @Test
        @DisplayName("Returns 400 when roles are empty")
        void returns400WhenRolesEmpty() throws Exception {
            String body = """
                    {"roles": []}
                    """;

            mockMvc.perform(put("/api/v1/users/{id}", USER_ID)
                            .with(adminJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class DeactivateUser {

        @Test
        @DisplayName("Returns 204 when deactivated")
        void returns204WhenDeactivated() throws Exception {
            doNothing().when(userService).deactivate(USER_ID);

            mockMvc.perform(delete("/api/v1/users/{id}", USER_ID).with(adminJwt()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Returns 404 when user not found")
        void returns404WhenNotFound() throws Exception {
            doThrow(new UserNotFoundException(USER_ID)).when(userService).deactivate(USER_ID);

            mockMvc.perform(delete("/api/v1/users/{id}", USER_ID).with(adminJwt()))
                    .andExpect(status().isNotFound());
        }
    }
}
