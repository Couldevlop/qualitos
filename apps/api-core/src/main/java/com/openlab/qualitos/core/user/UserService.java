package com.openlab.qualitos.core.user;

import com.openlab.qualitos.core.common.MissingTenantContextException;
import com.openlab.qualitos.core.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Résout le tenant courant depuis le JWT (via TenantContext).
     * Lève MissingTenantContextException si absent — jamais depuis le body.
     */
    private UUID resolveTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(tenantId);
    }

    public Page<UserDto.Response> findAll(Pageable pageable) {
        // Le filtre Hibernate tenantFilter est activé par TenantHibernateFilterInterceptor
        // avant l'exécution de cette requête — isolation garantie au niveau base.
        return userRepository.findAll(pageable).map(UserDto.Response::from);
    }

    public UserDto.Response findById(UUID id) {
        return userRepository.findById(id)
                .map(UserDto.Response::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public UserDto.Response findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .map(UserDto.Response::from)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));
    }

    @Transactional
    public UserDto.Response create(UserDto.CreateRequest request) {
        // Le tenantId vient TOUJOURS du JWT, jamais du body
        UUID tenantId = resolveTenantId();

        if (userRepository.existsByKeycloakId(request.keycloakId())) {
            throw new UserAlreadyExistsException(request.keycloakId());
        }

        AppUser user = AppUser.builder()
                .tenantId(tenantId)
                .keycloakId(request.keycloakId())
                .email(request.email())
                .roles(request.roles())
                .active(true)
                .build();

        AppUser saved = userRepository.save(user);
        log.info("User created: keycloakId={}, tenantId={}", saved.getKeycloakId(), saved.getTenantId());
        return UserDto.Response.from(saved);
    }

    @Transactional
    public UserDto.Response update(UUID id, UserDto.UpdateRequest request) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(request.roles());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }

        AppUser saved = userRepository.save(user);
        log.info("User updated: id={}", saved.getId());
        return UserDto.Response.from(saved);
    }

    @Transactional
    public void deactivate(UUID id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setActive(false);
        userRepository.save(user);
        log.info("User deactivated: id={}", id);
    }
}
