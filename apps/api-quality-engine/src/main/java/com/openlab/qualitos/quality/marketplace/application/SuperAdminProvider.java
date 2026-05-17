package com.openlab.qualitos.quality.marketplace.application;

import java.util.UUID;

/**
 * Port — only super admins (cross-tenant role) can register / verify packs.
 */
public interface SuperAdminProvider {
    /** Returns the super-admin user id, throws if the caller is not a super admin. */
    UUID requireSuperAdminId();
}
