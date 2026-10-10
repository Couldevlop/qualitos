package com.openlab.qualitos.core.tenant;

import com.openlab.qualitos.core.billing.ModuleActivationFailedException;
import com.openlab.qualitos.core.billing.ModuleActivationPort;
import com.openlab.qualitos.core.identity.IdentityProvider;
import com.openlab.qualitos.core.identity.PlatformRoles;
import com.openlab.qualitos.core.user.AppUser;
import com.openlab.qualitos.core.user.UserDto;
import com.openlab.qualitos.core.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Créer un client de bout en bout : l'entreprise, le compte de son premier
 * administrateur, puis ses modules (ADR 0079).
 *
 * <p><b>Tout ou rien pour l'entreprise et son administrateur.</b> Le client et
 * le membre s'enregistrent dans une transaction ; le compte de connexion est
 * créé au milieu. Si le compte ne se crée pas, rien n'est enregistré ; si
 * l'enregistrement échoue après la création du compte, le compte est supprimé.
 * Un client sans administrateur, ou un compte sans client, ne survit pas.
 *
 * <p><b>Les modules après coup, un par un.</b> Leur ouverture passe par le
 * moteur qualité, avec le jeton du super administrateur (même chemin que
 * l'abonnement). Un module refusé n'annule pas le client déjà créé : la
 * réponse dit lequel, et pourquoi, pour qu'on le reprenne.
 */
@Service
public class TenantOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(TenantOnboardingService.class);

    private final TenantService tenants;
    private final UserRepository users;
    private final IdentityProvider identity;
    private final ModuleActivationPort modules;
    private final TransactionTemplate tx;

    public TenantOnboardingService(TenantService tenants, UserRepository users, IdentityProvider identity,
                                   ModuleActivationPort modules, TransactionTemplate tx) {
        this.tenants = tenants;
        this.users = users;
        this.identity = identity;
        this.modules = modules;
        this.tx = tx;
    }

    public OnboardingDto.Response onboard(OnboardingDto.Request r) {
        Holder h = new Holder();
        tx.executeWithoutResult(status -> {
            h.tenant = tenants.create(new TenantDto.CreateRequest(r.slug(), r.name().strip(), r.plan()));
            h.account = identity.create(new IdentityProvider.NewAccount(h.tenant.id(), r.admin().email().strip(),
                    r.admin().firstName(), r.admin().lastName(), Set.of(PlatformRoles.TENANT_ADMIN)));
            try {
                h.admin = UserDto.Response.from(users.saveAndFlush(AppUser.builder()
                        .tenantId(h.tenant.id())
                        .keycloakId(h.account.accountId())
                        .email(r.admin().email().strip())
                        .roles(Set.of(PlatformRoles.TENANT_ADMIN))
                        .active(true)
                        .build()));
            } catch (RuntimeException e) {
                identity.delete(h.account.accountId());
                throw e;
            }
        });

        List<OnboardingDto.ModuleOutcome> resultats = new ArrayList<>();
        for (String code : new LinkedHashSet<>(r.modules() == null ? List.of() : r.modules())) {
            try {
                modules.activate(h.tenant.id(), code);
                resultats.add(new OnboardingDto.ModuleOutcome(code, true, null));
            } catch (ModuleActivationFailedException e) {
                log.warn("tenant.onboarding.module-failed tenant_id={} module_code={}", h.tenant.id(), code);
                resultats.add(new OnboardingDto.ModuleOutcome(code, false, e.getMessage()));
            }
        }
        log.info("tenant.onboarded tenant_id={} admin_user_id={} modules={}/{}", h.tenant.id(), h.admin.id(),
                resultats.stream().filter(OnboardingDto.ModuleOutcome::activated).count(), resultats.size());
        return new OnboardingDto.Response(h.tenant, h.admin, h.account.temporaryPassword(),
                h.account.invitationSent(), resultats);
    }

    /** Ce que la transaction produit, relu après elle. */
    private static final class Holder {
        TenantDto.Response tenant;
        IdentityProvider.CreatedAccount account;
        UserDto.Response admin;
    }
}
