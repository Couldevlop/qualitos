package com.openlab.qualitos.core.user;

import com.openlab.qualitos.core.common.MissingTenantContextException;
import com.openlab.qualitos.core.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private AppUser sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = AppUser.builder()
                .id(USER_ID)
                .tenantId(TENANT_ID)
                .keycloakId("kc-user-abc")
                .email("alice@acme.com")
                .roles(Set.of("QUALITY_MANAGER"))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Returns page of user DTOs")
        void returnsPageOfDtos() {
            Pageable pageable = PageRequest.of(0, 20);
            given(userRepository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(sampleUser)));

            Page<UserDto.Response> result = userService.findAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).email()).isEqualTo("alice@acme.com");
        }

        @Test
        @DisplayName("Returns empty page when no users exist")
        void returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            given(userRepository.findAll(pageable)).willReturn(Page.empty());

            Page<UserDto.Response> result = userService.findAll(pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Returns user DTO when found")
        void returnsDtoWhenFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser));

            UserDto.Response result = userService.findById(USER_ID);

            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.tenantId()).isEqualTo(TENANT_ID);
            assertThat(result.email()).isEqualTo("alice@acme.com");
        }

        @Test
        @DisplayName("Throws UserNotFoundException when not found")
        void throwsWhenNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(USER_ID))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(USER_ID.toString());
        }
    }

    @Nested
    @DisplayName("findByKeycloakId")
    class FindByKeycloakId {

        @Test
        @DisplayName("Returns user DTO when keycloakId found")
        void returnsDtoWhenFound() {
            given(userRepository.findByKeycloakId("kc-user-abc"))
                    .willReturn(Optional.of(sampleUser));

            UserDto.Response result = userService.findByKeycloakId("kc-user-abc");

            assertThat(result.keycloakId()).isEqualTo("kc-user-abc");
        }

        @Test
        @DisplayName("Throws UserNotFoundException when keycloakId not found")
        void throwsWhenNotFound() {
            given(userRepository.findByKeycloakId("unknown")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByKeycloakId("unknown"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("unknown");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Creates user with tenantId from TenantContext (JWT) — never from body")
        void createsTenantIdFromContext() {
            TenantContext.setTenantId(TENANT_ID.toString());

            UserDto.CreateRequest request = new UserDto.CreateRequest(
                    "kc-new-user", "bob@acme.com", Set.of("USER"));

            given(userRepository.existsByKeycloakId("kc-new-user")).willReturn(false);
            given(userRepository.save(any(AppUser.class))).willAnswer(inv -> {
                AppUser u = inv.getArgument(0);
                return AppUser.builder()
                        .id(UUID.randomUUID())
                        .tenantId(u.getTenantId())
                        .keycloakId(u.getKeycloakId())
                        .email(u.getEmail())
                        .roles(u.getRoles())
                        .active(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
            });

            UserDto.Response result = userService.create(request);

            ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
            verify(userRepository).save(captor.capture());

            // tenant_id doit venir du contexte JWT, pas du body
            assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.email()).isEqualTo("bob@acme.com");
        }

        @Test
        @DisplayName("Throws MissingTenantContextException when no tenant in context")
        void throwsWhenNoTenantContext() {
            // TenantContext vide — aucun JWT tenant_id
            UserDto.CreateRequest request = new UserDto.CreateRequest(
                    "kc-no-tenant", "test@test.com", Set.of("USER"));

            assertThatThrownBy(() -> userService.create(request))
                    .isInstanceOf(MissingTenantContextException.class);
        }

        @Test
        @DisplayName("Throws UserAlreadyExistsException when keycloakId already exists")
        void throwsWhenKeycloakIdExists() {
            TenantContext.setTenantId(TENANT_ID.toString());
            given(userRepository.existsByKeycloakId("kc-existing")).willReturn(true);

            UserDto.CreateRequest request = new UserDto.CreateRequest(
                    "kc-existing", "other@acme.com", Set.of("USER"));

            assertThatThrownBy(() -> userService.create(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("kc-existing");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Updates roles when roles are provided")
        void updatesRoles() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser));
            given(userRepository.save(any(AppUser.class))).willAnswer(inv -> inv.getArgument(0));

            UserDto.UpdateRequest request = new UserDto.UpdateRequest(
                    Set.of("QUALITY_DIRECTOR", "AUDITOR"), null);

            UserDto.Response result = userService.update(USER_ID, request);

            assertThat(result.roles()).containsExactlyInAnyOrder("QUALITY_DIRECTOR", "AUDITOR");
        }

        @Test
        @DisplayName("Updates active status")
        void updatesActiveStatus() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser));
            given(userRepository.save(any(AppUser.class))).willAnswer(inv -> inv.getArgument(0));

            UserDto.UpdateRequest request = new UserDto.UpdateRequest(
                    Set.of("USER"), false);

            UserDto.Response result = userService.update(USER_ID, request);

            assertThat(result.active()).isFalse();
        }

        @Test
        @DisplayName("Throws UserNotFoundException when user not found")
        void throwsWhenNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            UserDto.UpdateRequest request = new UserDto.UpdateRequest(Set.of("USER"), null);
            assertThatThrownBy(() -> userService.update(USER_ID, request))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deactivate")
    class Deactivate {

        @Test
        @DisplayName("Sets active to false on user")
        void deactivatesUser() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser));
            given(userRepository.save(any(AppUser.class))).willAnswer(inv -> inv.getArgument(0));

            userService.deactivate(USER_ID);

            ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("Throws UserNotFoundException when user not found")
        void throwsWhenNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deactivate(USER_ID))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
