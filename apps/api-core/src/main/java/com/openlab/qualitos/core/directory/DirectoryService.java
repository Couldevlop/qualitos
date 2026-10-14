package com.openlab.qualitos.core.directory;

import com.openlab.qualitos.core.common.MissingTenantContextException;
import com.openlab.qualitos.core.security.TenantContext;
import com.openlab.qualitos.core.user.DirectoryRow;
import com.openlab.qualitos.core.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * L'annuaire du client en lecture restreinte (ADR 0091) : retrouver une personne
 * pour la désigner responsable, sans rien voir de ce qui sert à l'administrer.
 *
 * <p>Le client vient TOUJOURS du jeton ; il est passé explicitement à chaque
 * requête, en plus du filtre Hibernate et de la RLS, parce qu'une lecture par
 * identifiant échappe au filtre Hibernate.
 */
@Service
@Transactional(readOnly = true)
public class DirectoryService {

    private final UserRepository users;

    public DirectoryService(UserRepository users) {
        this.users = users;
    }

    public DirectoryDto.Page search(DirectoryQuery query) {
        UUID tenant = currentTenant();
        Page<DirectoryRow> rows = users.searchDirectory(tenant, query.pattern(),
                PageRequest.of(query.page(), query.size()));
        return new DirectoryDto.Page(
                rows.getContent().stream().map(DirectoryDto.Entry::from).toList(),
                rows.getNumber(), rows.getSize(), rows.getTotalElements(), rows.getTotalPages());
    }

    public DirectoryDto.Entry get(UUID id) {
        UUID tenant = currentTenant();
        return users.findDirectoryRow(tenant, id)
                .map(DirectoryDto.Entry::from)
                .orElseThrow(DirectoryEntryNotFoundException::new);
    }

    /**
     * Une personne du client par son compte de connexion, active ou non. Même 404
     * neutre qu'un compte inconnu quand il appartient à un autre client.
     */
    public DirectoryDto.Entry getByAccount(UUID accountId) {
        UUID tenant = currentTenant();
        return users.findDirectoryRowByAccount(tenant, accountId.toString())
                .map(DirectoryDto.Entry::from)
                .orElseThrow(DirectoryEntryNotFoundException::new);
    }

    private static UUID currentTenant() {
        String tenant = TenantContext.getTenantId();
        if (tenant == null) {
            throw new MissingTenantContextException();
        }
        try {
            return UUID.fromString(tenant);
        } catch (IllegalArgumentException e) {
            // Un client illisible dans le jeton n'est pas une faute de saisie (400) :
            // l'identité elle-même est inexploitable.
            throw new MissingTenantContextException();
        }
    }
}
