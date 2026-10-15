-- =============================================================================
-- V9 : les rôles des membres s'écrivent comme le realm les émet
--
-- La V3 attendait des MAJUSCULES (« QUALITY_MANAGER ») et un rôle « ADMIN » que
-- le realm ne connaît pas ; elle ignorait « ADMIN_TENANT ». Or tout le reste de
-- la chaîne parle la langue du realm, en minuscules : le jeton
-- (realm_access.roles), l'écran d'administration, l'invitation (PlatformRoles).
-- Inviter un membre échouait donc en 500 sur chk_app_user_roles_role.
--
-- Une seule orthographe désormais, celle du realm :
--   super_admin, admin_tenant, quality_director, quality_manager, auditor,
--   user, external_auditor
-- Les anciennes lignes sont converties (ADMIN devient admin_tenant) ; un doublon
-- né de la conversion est absorbé par la clé primaire (user_id, role).
-- =============================================================================

ALTER TABLE app_user_roles DROP CONSTRAINT chk_app_user_roles_role;

INSERT INTO app_user_roles (user_id, role)
SELECT user_id,
       CASE upper(role)
           WHEN 'ADMIN'            THEN 'admin_tenant'
           WHEN 'DIRECTOR_QUALITY' THEN 'quality_director'
           ELSE lower(role)
       END
FROM app_user_roles
WHERE role <> lower(role) OR lower(role) IN ('admin', 'director_quality')
ON CONFLICT ON CONSTRAINT pk_app_user_roles DO NOTHING;

DELETE FROM app_user_roles
WHERE role <> lower(role) OR role IN ('admin', 'director_quality');

ALTER TABLE app_user_roles ADD CONSTRAINT chk_app_user_roles_role
    CHECK (role IN (
        'super_admin',
        'admin_tenant',
        'quality_director',
        'quality_manager',
        'auditor',
        'user',
        'external_auditor'
    ));

COMMENT ON COLUMN app_user_roles.role IS 'Rôle de plateforme, tel que le realm l''émet (minuscules)';
