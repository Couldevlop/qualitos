# -*- coding: utf-8 -*-
"""Libellés des droits par client (ADR 0078) : actions du catalogue, modules,
rôles de la plateforme, et la page « Rôles et droits ».

Placeholders : `${x}:nom:` côté TS s'écrit {$nom} (casse conservée) ;
`{{ x // i18n(ph="nom") }}` côté gabarit s'écrit {$NOM}.
"""

TRANSLATIONS = {
    'nav.admin-roles': ('Rôles et droits', 'Roles and permissions', 'Roles y permisos', 'الأدوار والصلاحيات', 'ロールと権限', '角色与权限'),

    # ---------- actions ----------
    'authz.perm.authz-manage': (
        'Administrer les rôles et les droits', 'Manage roles and permissions', 'Administrar roles y permisos',
        'إدارة الأدوار والصلاحيات', 'ロールと権限を管理', '管理角色与权限'),
    'authz.perm.capa-create': ('Ouvrir un dossier CAPA', 'Open a CAPA case', 'Abrir un expediente CAPA', 'فتح ملف CAPA', 'CAPAケースを開始', '开启 CAPA 案例'),
    'authz.perm.capa-edit': (
        'Modifier et démarrer un dossier', 'Edit and start a case', 'Modificar e iniciar un expediente',
        'تعديل ملف وبدء معالجته', 'ケースの編集と開始', '编辑并启动案例'),
    'authz.perm.capa-resolve': ('Résoudre un dossier', 'Resolve a case', 'Resolver un expediente', 'حل ملف', 'ケースを解決', '解决案例'),
    'authz.perm.capa-reject': ('Rejeter un dossier', 'Reject a case', 'Rechazar un expediente', 'رفض ملف', 'ケースを却下', '驳回案例'),
    'authz.perm.capa-verify': ("Vérifier l'efficacité", 'Verify effectiveness', 'Verificar la eficacia', 'التحقق من الفعالية', '有効性を検証', '验证有效性'),
    'authz.perm.capa-delete': ('Supprimer un dossier', 'Delete a case', 'Eliminar un expediente', 'حذف ملف', 'ケースを削除', '删除案例'),
    'authz.perm.capa-action-manage': (
        'Ajouter et retirer des actions', 'Add and remove actions', 'Añadir y quitar acciones',
        'إضافة الإجراءات وإزالتها', 'アクションの追加と削除', '添加和移除措施'),
    'authz.perm.capa-action-update': ('Faire avancer une action', 'Progress an action', 'Avanzar una acción', 'تقدّم إجراء', 'アクションを進める', '推进措施'),
    'authz.perm.nc-create': ('Déclarer une non-conformité', 'Report a nonconformity', 'Declarar una no conformidad', 'الإبلاغ عن عدم مطابقة', '不適合を報告', '报告不合格'),
    'authz.perm.nc-edit': ('Modifier une non-conformité', 'Edit a nonconformity', 'Modificar una no conformidad', 'تعديل عدم مطابقة', '不適合を編集', '编辑不合格'),
    'authz.perm.nc-photo': ('Ajouter des photos', 'Add photos', 'Añadir fotos', 'إضافة صور', '写真を追加', '添加照片'),
    'authz.perm.nc-process': ('Analyser et résoudre', 'Analyse and resolve', 'Analizar y resolver', 'التحليل والحل', '分析と解決', '分析并解决'),
    'authz.perm.nc-close': ('Clôturer', 'Close', 'Cerrar', 'إغلاق', 'クローズ', '关闭'),
    'authz.perm.nc-reject': ('Annuler ou rejeter', 'Cancel or reject', 'Anular o rechazar', 'إلغاء أو رفض', '取消または却下', '取消或驳回'),
    'authz.perm.nc-escalate': (
        'Poser une action corrective', 'Raise a corrective action', 'Plantear una acción correctiva',
        'وضع إجراء تصحيحي', '是正処置を起こす', '提出纠正措施'),
    'authz.perm.document-edit': ('Rédiger et archiver', 'Write and archive', 'Redactar y archivar', 'التحرير والأرشفة', '作成とアーカイブ', '编写与归档'),
    'authz.perm.document-submit': ('Soumettre à revue', 'Submit for review', 'Enviar a revisión', 'إرسال للمراجعة', 'レビューに提出', '提交审阅'),
    'authz.perm.document-approve': ('Approuver', 'Approve', 'Aprobar', 'الموافقة', '承認', '批准'),
    'authz.perm.document-publish': ('Publier', 'Publish', 'Publicar', 'النشر', '公開', '发布'),
    'authz.perm.document-acknowledge': ('Acquitter une lecture', 'Acknowledge reading', 'Confirmar la lectura', 'الإقرار بالقراءة', '閲読を確認', '确认阅读'),
    'authz.perm.risk-manage': (
        'Tenir le registre des risques', 'Maintain the risk register', 'Llevar el registro de riesgos',
        'إدارة سجل المخاطر', 'リスク登録簿を管理', '维护风险登记册'),
    'authz.perm.opportunity-manage': (
        'Tenir le registre des opportunités', 'Maintain the opportunity register', 'Llevar el registro de oportunidades',
        'إدارة سجل الفرص', '機会登録簿を管理', '维护机遇登记册'),

    # ---------- modules et rôles ----------
    'authz.module.admin': ('Administration', 'Administration', 'Administración', 'الإدارة', '管理', '管理'),
    'authz.module.capa': ('CAPA', 'CAPA', 'CAPA', 'CAPA', 'CAPA', 'CAPA'),
    'authz.module.nc': ('Non-conformités', 'Nonconformities', 'No conformidades', 'حالات عدم المطابقة', '不適合', '不合格'),
    'authz.module.document': ('Documents', 'Documents', 'Documentos', 'الوثائق', '文書', '文件'),
    'authz.module.risk': ('Risques et opportunités', 'Risks and opportunities', 'Riesgos y oportunidades', 'المخاطر والفرص', 'リスクと機会', '风险与机遇'),
    'authz.role.admin-tenant': ('Administrateur', 'Administrator', 'Administrador', 'المسؤول', '管理者', '管理员'),
    'authz.role.quality-director': ('Directeur qualité', 'Quality director', 'Director de calidad', 'مدير الجودة', '品質責任者', '质量总监'),
    'authz.role.quality-manager': ('Manager qualité', 'Quality manager', 'Responsable de calidad', 'مسؤول الجودة', '品質マネージャー', '质量经理'),
    'authz.role.auditor': ('Auditeur', 'Auditor', 'Auditor', 'المدقق', '監査員', '审核员'),
    'authz.role.user': ('Utilisateur', 'User', 'Usuario', 'المستخدم', 'ユーザー', '用户'),
    'authz.role.external-auditor': ('Auditeur externe', 'External auditor', 'Auditor externo', 'مدقق خارجي', '外部監査員', '外部审核员'),

    # ---------- page ----------
    'authz.matrix.title': ('Rôles et droits', 'Roles and permissions', 'Roles y permisos', 'الأدوار والصلاحيات', 'ロールと権限', '角色与权限'),
    'authz.matrix.subtitle': (
        'Ce que chaque rôle peut faire, et qui le porte. Glissez un membre sur un rôle pour le lui donner.',
        'What each role can do, and who holds it. Drag a member onto a role to give it to them.',
        'Lo que cada rol puede hacer y quién lo tiene. Arrastre un miembro sobre un rol para asignárselo.',
        'ما يستطيع كل دور فعله ومن يحمله. اسحب عضوًا إلى دور لمنحه إياه.',
        '各ロールができることと、その担当者。メンバーをロールにドラッグして付与します。',
        '每个角色能做什么、由谁担任。将成员拖到角色上即可授予。'),
    'authz.matrix.new-role': ('Nouveau rôle', 'New role', 'Nuevo rol', 'دور جديد', '新しいロール', '新建角色'),
    'authz.matrix.load-failed': (
        "Les droits n'ont pas pu être chargés.", 'The permissions could not be loaded.',
        'No se pudieron cargar los permisos.', 'تعذّر تحميل الصلاحيات.', '権限を読み込めませんでした。', '无法加载权限。'),
    'authz.matrix.failed': (
        "L'opération sur les droits a échoué.", 'The permissions operation failed.',
        'La operación sobre los permisos ha fallado.', 'فشلت العملية على الصلاحيات.', '権限の操作に失敗しました。', '权限操作失败。'),
    'authz.matrix.role-name': ('Nom du rôle', 'Role name', 'Nombre del rol', 'اسم الدور', 'ロール名', '角色名称'),
    'authz.matrix.role-name-placeholder': ('Pilote de site', 'Site lead', 'Responsable de sitio', 'مسؤول الموقع', '拠点リーダー', '现场负责人'),
    'authz.matrix.role-code': ('Code', 'Code', 'Código', 'الرمز', 'コード', '代码'),
    'authz.matrix.role-code-hint': (
        'Lettres, chiffres et _ ; il ne change plus ensuite.', 'Letters, digits and _; it cannot change later.',
        'Letras, cifras y _; no cambia después.', 'أحرف وأرقام و _؛ لا يتغيّر بعد ذلك.',
        '英字、数字、_。後から変更できません。', '字母、数字和 _；之后不可更改。'),
    'authz.matrix.role-code-invalid': (
        'Lettres, chiffres et _, en commençant par une lettre.', 'Letters, digits and _, starting with a letter.',
        'Letras, cifras y _, empezando por una letra.', 'أحرف وأرقام و _ ، تبدأ بحرف.',
        '英字で始まる英字、数字、_。', '以字母开头的字母、数字和 _。'),
    'authz.matrix.role-description': ('Description (facultatif)', 'Description (optional)', 'Descripción (opcional)', 'الوصف (اختياري)', '説明（任意）', '描述（可选）'),
    'authz.matrix.create': ('Créer le rôle', 'Create the role', 'Crear el rol', 'إنشاء الدور', 'ロールを作成', '创建角色'),
    'authz.matrix.created': (
        'Rôle créé : cochez maintenant ses droits.', 'Role created: now tick its permissions.',
        'Rol creado: marque ahora sus permisos.', 'تم إنشاء الدور: حدّد الآن صلاحياته.',
        'ロールを作成しました。権限をチェックしてください。', '角色已创建：现在勾选其权限。'),
    'authz.matrix.team': ('Équipe', 'Team', 'Equipo', 'الفريق', 'チーム', '团队'),
    'authz.matrix.team-help': (
        'Glissez une carte sur une colonne, ou ouvrez son menu.', 'Drag a card onto a column, or open its menu.',
        'Arrastre una tarjeta sobre una columna o abra su menú.', 'اسحب بطاقة إلى عمود أو افتح قائمتها.',
        'カードを列にドラッグするか、メニューを開きます。', '将卡片拖到某一列，或打开其菜单。'),
    'authz.matrix.team-failed': (
        "L'annuaire de l'équipe ne répond pas : la matrice reste modifiable, l'attribution attendra.",
        'The team directory is not responding: the matrix can still be edited, assignment will have to wait.',
        'El directorio del equipo no responde: la matriz sigue editable, la asignación tendrá que esperar.',
        'دليل الفريق لا يستجيب: تبقى المصفوفة قابلة للتعديل، وسينتظر الإسناد.',
        'チームの名簿が応答しません。マトリクスは編集できますが、割り当ては後になります。',
        '团队目录无响应：矩阵仍可编辑，分配需稍后进行。'),
    'authz.matrix.team-empty': ('Aucun membre actif pour l\'instant.', 'No active member yet.', 'Ningún miembro activo por ahora.', 'لا يوجد عضو نشط حاليًا.', 'アクティブなメンバーはまだいません。', '暂无活跃成员。'),
    'authz.matrix.account-roles-only': (
        'Rôles de son compte seulement', 'Account roles only', 'Solo los roles de su cuenta',
        'أدوار حسابه فقط', 'アカウントのロールのみ', '仅账户角色'),
    'authz.matrix.assign': ('Attribuer un rôle', 'Assign a role', 'Asignar un rol', 'إسناد دور', 'ロールを割り当て', '分配角色'),
    'authz.matrix.assigned': (
        '{$member} a désormais le rôle {$role}.', '{$member} now has the {$role} role.',
        '{$member} tiene ahora el rol {$role}.', 'أصبح لدى {$member} الدور {$role}.',
        '{$member} に {$role} ロールを付与しました。', '{$member} 现在拥有 {$role} 角色。'),
    'authz.matrix.unassigned': (
        "{$member} n'a plus le rôle {$role}.", '{$member} no longer has the {$role} role.',
        '{$member} ya no tiene el rol {$role}.', 'لم يعد لدى {$member} الدور {$role}.',
        '{$member} から {$role} ロールを外しました。', '{$member} 不再拥有 {$role} 角色。'),
    'authz.matrix.unassign-aria': (
        'Retirer le rôle {$role} à {$member}', 'Remove the {$role} role from {$member}',
        'Quitar el rol {$role} a {$member}', 'إزالة الدور {$role} من {$member}',
        '{$member} から {$role} ロールを外す', '移除 {$member} 的 {$role} 角色'),
    'authz.matrix.grid': ('Matrice des droits', 'Permission matrix', 'Matriz de permisos', 'مصفوفة الصلاحيات', '権限マトリクス', '权限矩阵'),
    'authz.matrix.actions': ('Actions', 'Actions', 'Acciones', 'الإجراءات', 'アクション', '操作'),
    'authz.matrix.role-menu': ('Options du rôle', 'Role options', 'Opciones del rol', 'خيارات الدور', 'ロールのオプション', '角色选项'),
    'authz.matrix.reset': ('Rétablir', 'Restore', 'Restablecer', 'استعادة', '元に戻す', '恢复'),
    'authz.matrix.custom': ('Sur mesure', 'Custom', 'A medida', 'مخصّص', 'カスタム', '自定义'),
    'authz.matrix.customized': ('Réglé', 'Adjusted', 'Ajustado', 'معدّل', '調整済み', '已调整'),
    'authz.matrix.delivered': ('Droits livrés', 'Default permissions', 'Permisos predeterminados', 'الصلاحيات الافتراضية', '既定の権限', '默认权限'),
    'authz.matrix.granted-count': ('{$COUNT} actions', '{$COUNT} actions', '{$COUNT} acciones', '{$COUNT} إجراءات', '{$COUNT} 件のアクション', '{$COUNT} 项操作'),
    'authz.matrix.drop-here': ('Déposer ici', 'Drop here', 'Soltar aquí', 'أفلت هنا', 'ここにドロップ', '拖放到此处'),
    'authz.matrix.locked': (
        "L'administrateur garde toujours ce droit : sans lui, plus personne ne pourrait rétablir les autres.",
        'The administrator always keeps this permission: without it, no one could restore the others.',
        'El administrador conserva siempre este permiso: sin él, nadie podría restablecer los demás.',
        'يحتفظ المسؤول دائمًا بهذه الصلاحية: بدونها لن يتمكن أحد من استعادة الصلاحيات الأخرى.',
        '管理者は常にこの権限を保持します。これがないと、他の権限を誰も元に戻せません。',
        '管理员始终保留此权限：没有它，任何人都无法恢复其他权限。'),
    'authz.matrix.pending': ('Rôles modifiés : {$COUNT}', 'Roles changed: {$COUNT}', 'Roles modificados: {$COUNT}', 'الأدوار المعدّلة: {$COUNT}', '変更したロール：{$COUNT}', '已修改角色：{$COUNT}'),
    'authz.matrix.discard': ('Annuler les changements', 'Discard changes', 'Descartar los cambios', 'تجاهل التغييرات', '変更を破棄', '放弃更改'),
    'authz.matrix.saved': ('Droits enregistrés.', 'Permissions saved.', 'Permisos guardados.', 'تم حفظ الصلاحيات.', '権限を保存しました。', '权限已保存。'),
    'authz.matrix.reset-title': (
        "Rendre au rôle {$role} ses droits d'origine ?", 'Restore the default permissions of the {$role} role?',
        '¿Restablecer los permisos de origen del rol {$role}?', 'استعادة الصلاحيات الأصلية للدور {$role}؟',
        '{$role} ロールの権限を既定に戻しますか？', '将 {$role} 角色恢复为默认权限？'),
    'authz.matrix.reset-message': (
        'Les droits livrés par la plateforme remplacent vos réglages. Ses membres le gardent.',
        'The platform defaults replace your adjustments. Its members keep the role.',
        'Los permisos de la plataforma sustituyen sus ajustes. Sus miembros lo conservan.',
        'تحل صلاحيات المنصة الافتراضية محل تعديلاتك. ويحتفظ أعضاؤه به.',
        'プラットフォームの既定権限が調整内容を置き換えます。メンバーはロールを保持します。',
        '平台默认权限将替换您的调整。其成员保留该角色。'),
    'authz.matrix.delete-title': ('Supprimer le rôle {$role} ?', 'Delete the {$role} role?', '¿Eliminar el rol {$role}?', 'حذف الدور {$role}؟', '{$role} ロールを削除しますか？', '删除 {$role} 角色？'),
    'authz.matrix.delete-message': (
        "Ses membres perdent les droits qu'il leur donnait. La suppression est tracée dans le journal d'audit.",
        'Its members lose the permissions it gave them. The deletion is recorded in the audit log.',
        'Sus miembros pierden los permisos que les daba. La eliminación queda registrada en el registro de auditoría.',
        'يفقد أعضاؤه الصلاحيات التي كان يمنحها لهم. ويُسجَّل الحذف في سجل التدقيق.',
        'メンバーはこのロールの権限を失います。削除は監査ログに記録されます。',
        '其成员将失去该角色授予的权限。删除操作会记录在审计日志中。'),
}
