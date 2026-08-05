# -*- coding: utf-8 -*-
"""Table i18n - ecran « Equipe & habilitations » du tenant (SS16). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.admin-team': ('Équipe & habilitations', 'Team & permissions', 'Equipo y permisos', 'الفريق والصلاحيات', 'チームと権限', '团队与权限'),

    'admin.team.eyebrow': ('Administration', 'Administration', 'Administración', 'الإدارة', '管理', '管理'),
    'admin.team.title': ('Équipe & habilitations', 'Team & permissions', 'Equipo y permisos', 'الفريق والصلاحيات', 'チームと権限', '团队与权限'),
    'admin.team.subtitle': (
        "Composez votre équipe qualité : qui accède à la plateforme, et avec quels rôles. L'autorisation est appliquée par le serveur — cet écran ne fait qu'en donner la clé.",
        'Build your quality team: who reaches the platform, and with which roles. Authorisation is enforced by the server — this screen only hands out the key.',
        'Componga su equipo de calidad: quién accede a la plataforma y con qué roles. La autorización la aplica el servidor; esta pantalla solo entrega la llave.',
        'كوّن فريق الجودة لديك: من يصل إلى المنصة، وبأي أدوار. التفويض يطبّقه الخادم — هذه الشاشة تمنح المفتاح فحسب.',
        '品質チームを構成します：誰がプラットフォームにアクセスし、どの役割を持つか。認可はサーバーが適用します。この画面は鍵を渡すだけです。',
        '组建您的质量团队：谁可以访问平台，以及拥有哪些角色。授权由服务端强制执行，本页面只负责发放钥匙。'),
    'admin.team.search': ('Rechercher un membre ou un rôle', 'Search a member or a role', 'Buscar un miembro o un rol', 'ابحث عن عضو أو دور', 'メンバーまたは役割を検索', '搜索成员或角色'),
    'admin.team.loading': ("Chargement de l'équipe", 'Loading the team', 'Cargando el equipo', 'جارٍ تحميل الفريق', 'チームを読み込み中', '正在加载团队'),
    'admin.team.empty': ('Aucun membre ne correspond.', 'No member matches.', 'Ningún miembro coincide.', 'لا يوجد عضو مطابق.', '該当するメンバーはいません。', '没有匹配的成员。'),
    'admin.team.col-member': ('Membre', 'Member', 'Miembro', 'العضو', 'メンバー', '成员'),
    'admin.team.col-roles': ('Rôles', 'Roles', 'Roles', 'الأدوار', '役割', '角色'),
    'admin.team.active': ('Actif', 'Active', 'Activo', 'نشط', '有効', '启用'),
    'admin.team.inactive': ('Désactivé', 'Deactivated', 'Desactivado', 'مُعطَّل', '無効', '已停用'),
    'admin.team.revoke': ("Retirer l'accès", 'Revoke access', 'Retirar el acceso', 'سحب الوصول', 'アクセスを取り消す', '撤销访问'),
    'admin.team.restore': ("Rétablir l'accès", 'Restore access', 'Restaurar el acceso', 'استعادة الوصول', 'アクセスを回復する', '恢复访问'),
    'admin.team.load-failed': ("Impossible de charger l'équipe.", 'Could not load the team.', 'No se pudo cargar el equipo.', 'تعذّر تحميل الفريق.', 'チームを読み込めませんでした。', '无法加载团队。'),
    'admin.team.needs-a-role': ('Un membre doit conserver au moins un rôle.', 'A member must keep at least one role.', 'Un miembro debe conservar al menos un rol.', 'يجب أن يحتفظ العضو بدور واحد على الأقل.', 'メンバーには少なくとも1つの役割が必要です。', '成员必须至少保留一个角色。'),
    'admin.team.saved': ('Habilitations mises à jour.', 'Permissions updated.', 'Permisos actualizados.', 'تم تحديث الصلاحيات.', '権限を更新しました。', '权限已更新。'),
    'admin.team.save-failed': ('Modification refusée.', 'Change refused.', 'Modificación rechazada.', 'تم رفض التعديل.', '変更は拒否されました。', '修改被拒绝。'),

    # Libelles des roles du realm (SS16).
    'role.admin-tenant': ('Administrateur du tenant', 'Tenant administrator', 'Administrador del tenant', 'مسؤول المستأجر', 'テナント管理者', '租户管理员'),
    'role.quality-director': ('Directeur qualité', 'Quality director', 'Director de calidad', 'مدير الجودة', '品質責任者', '质量总监'),
    'role.quality-manager': ('Manager qualité', 'Quality manager', 'Gerente de calidad', 'مدير جودة', '品質マネージャー', '质量经理'),
    'role.auditor': ('Auditeur', 'Auditor', 'Auditor', 'مدقّق', '監査員', '审核员'),
    'role.user': ('Utilisateur', 'User', 'Usuario', 'مستخدم', 'ユーザー', '用户'),
    'role.external-auditor': ('Auditeur externe', 'External auditor', 'Auditor externo', 'مدقّق خارجي', '外部監査員', '外部审核员'),
    'role.super-admin': ('Super administrateur', 'Super administrator', 'Superadministrador', 'المسؤول الأعلى', 'スーパー管理者', '超级管理员'),
}
