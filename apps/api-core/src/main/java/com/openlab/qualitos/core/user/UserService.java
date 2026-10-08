package com.openlab.qualitos.core.user;

import com.openlab.qualitos.core.common.MissingTenantContextException;
import com.openlab.qualitos.core.identity.IdentityProvider;
import com.openlab.qualitos.core.identity.PlatformRoles;
import com.openlab.qualitos.core.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Les membres d'un client et leurs comptes de connexion (ADR 0079).
 *
 * <p>Le client vient TOUJOURS du jeton. Une lecture ou une écriture par
 * identifiant vérifie que le membre est du client du jeton : le filtre Hibernate
 * par client ne s'applique pas à une lecture par clé primaire, et sans ce
 * contrôle un administrateur pouvait modifier le membre d'un autre client
 * (404 indiscernable de l'absence).
 *
 * <p>Les rôles se règlent dans le fournisseur d'identité ET ici : c'est le
 * fournisseur qui les met dans le jeton, donc c'est lui qui fait foi ; la table
 * en garde la copie que l'écran affiche.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final IdentityProvider identity;

    public UserService(UserRepository userRepository, IdentityProvider identity) {
        this.userRepository = userRepository;
        this.identity = identity;
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
        return UserDto.Response.from(memberOfTenant(id));
    }

    public UserDto.Response findByKeycloakId(String keycloakId) {
        UUID tenant = resolveTenantId();
        return userRepository.findByKeycloakId(keycloakId)
                .filter(u -> tenant.equals(u.getTenantId()))
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

    /**
     * Invite un membre dans le client du jeton : son compte est créé avec ses
     * rôles, puis enregistré ici. Si l'enregistrement échoue, le compte créé est
     * supprimé — pas de compte orphelin capable de se connecter sans être membre.
     */
    @Transactional
    public UserDto.InviteResponse invite(UserDto.InviteRequest request) {
        UUID tenantId = resolveTenantId();
        Set<String> roles = PlatformRoles.validated(request.roles());
        IdentityProvider.CreatedAccount compte = identity.create(new IdentityProvider.NewAccount(
                tenantId, request.email().strip(), request.firstName(), request.lastName(), roles));
        try {
            AppUser saved = userRepository.saveAndFlush(AppUser.builder()
                    .tenantId(tenantId)
                    .keycloakId(compte.accountId())
                    .email(request.email().strip())
                    .roles(roles)
                    .active(true)
                    .build());
            log.info("user.invited user_id={} tenant_id={}", saved.getId(), tenantId);
            return new UserDto.InviteResponse(UserDto.Response.from(saved), compte.temporaryPassword(),
                    compte.invitationSent());
        } catch (RuntimeException e) {
            identity.delete(compte.accountId());
            throw e;
        }
    }

    /**
     * Change les rôles ou l'état d'un membre du client du jeton — dans son compte
     * de connexion d'abord : c'est lui que le jeton reflète.
     */
    @Transactional
    public UserDto.Response update(UUID id, UserDto.UpdateRequest request) {
        AppUser user = memberOfTenant(id);

        if (request.roles() != null && !request.roles().isEmpty()) {
            Set<String> roles = PlatformRoles.validated(request.roles());
            identity.setRoles(user.getKeycloakId(), roles);
            user.setRoles(roles);
        }
        if (request.active() != null && request.active() != user.isActive()) {
            identity.setEnabled(user.getKeycloakId(), request.active());
            user.setActive(request.active());
        }

        AppUser saved = userRepository.save(user);
        log.info("User updated: id={}", saved.getId());
        return UserDto.Response.from(saved);
    }

    /** Retire l'accès sans effacer l'historique : le compte est désactivé, pas supprimé. */
    @Transactional
    public void deactivate(UUID id) {
        AppUser user = memberOfTenant(id);
        identity.setEnabled(user.getKeycloakId(), false);
        user.setActive(false);
        userRepository.save(user);
        log.info("User deactivated: id={}", id);
    }

    private AppUser memberOfTenant(UUID id) {
        UUID tenant = resolveTenantId();
        return userRepository.findById(id)
                .filter(u -> tenant.equals(u.getTenantId()))
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
