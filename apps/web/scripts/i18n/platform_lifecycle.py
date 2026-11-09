# -*- coding: utf-8 -*-
"""La vie d'un client et d'un cabinet, après leur création.

Un client et un cabinet se créaient et ne se reprenaient plus : une raison
sociale mal orthographiée restait, un passage d'offre se faisait en base, et
aucun écran ne savait fermer l'accès d'un client. Les deux blocs du tableau
d'habilitation (socle, additionnels) sont ici aussi : ils disent, au-dessus de
chaque groupe, ce qui est acquis et ce qui se négocie.
id: (fr, en, es, ar, ja, zh).
"""

TRANSLATIONS = {

    # --- corriger un client -------------------------------------------------
    'platform.clients.edit': (
        'Corriger', 'Edit', 'Corregir', 'تصحيح', '修正', '修改'),
    'platform.clients.edit-title': (
        'Corriger le client', 'Edit the client', 'Corregir el cliente',
        'تصحيح بيانات العميل', '顧客を修正', '修改客户'),
    'platform.clients.plan-hint': (
        "L'offre borne les modules ouvrables ; elle ne ferme pas ceux déjà ouverts.",
        'The plan caps which modules can be opened; it does not close those already open.',
        'La oferta limita los módulos que pueden abrirse; no cierra los ya abiertos.',
        'تحدّ الخطة الوحدات القابلة للفتح، ولا تغلق ما سبق فتحه.',
        'プランは開放できるモジュールの上限を定めるもので、既に開放済みのものを閉じることはありません。',
        '订阅方案限定可开通的模块，但不会关闭已开通的模块。'),
    'platform.clients.slug-frozen': (
        'ne change plus', 'cannot be changed', 'ya no cambia',
        'لا يتغيّر بعد الآن', '以後変更不可', '不可再更改'),
    'platform.clients.saved': (
        'Client mis à jour.', 'Client updated.', 'Cliente actualizado.',
        'تم تحديث بيانات العميل.', '顧客を更新しました。', '客户已更新。'),
    'platform.clients.save-failed': (
        "La correction n'a pas pu être enregistrée.",
        'The change could not be saved.',
        'La corrección no pudo guardarse.',
        'لم يتمكّن النظام من حفظ التصحيح.',
        '修正を保存できませんでした。',
        '修改未能保存。'),

    # --- suspendre, réactiver ----------------------------------------------
    'platform.clients.suspend': (
        'Suspendre', 'Suspend', 'Suspender', 'تعليق', '停止', '暂停'),
    'platform.clients.resume': (
        'Réactiver', 'Reactivate', 'Reactivar', 'إعادة التنشيط', '再開', '恢复'),
    'platform.clients.suspend-title': (
        'Suspendre ce client ?', 'Suspend this client?', '¿Suspender este cliente?',
        'تعليق هذا العميل؟', 'この顧客を停止しますか？', '暂停该客户？'),
    'platform.clients.suspend-message': (
        "Ses utilisateurs ne pourront plus se connecter. Rien n'est effacé : ses dossiers, "
        "ses preuves et sa piste d'audit restent en place, et tout reprend si vous le réactivez.",
        'Its users will no longer be able to sign in. Nothing is deleted: its records, its '
        'evidence and its audit trail stay in place, and everything resumes if you reactivate it.',
        'Sus usuarios ya no podrán conectarse. Nada se borra: sus expedientes, sus pruebas y su '
        'pista de auditoría permanecen, y todo se reanuda si lo reactiva.',
        'لن يتمكّن مستخدموه من تسجيل الدخول. لا يُحذف شيء: تبقى ملفاته وأدلّته ومسار تدقيقه في '
        'مكانها، ويعود كل شيء إذا أعدت تنشيطه.',
        '所属ユーザーはサインインできなくなります。削除は行われません。記録、証拠、監査証跡はそのまま残り、'
        '再開すればすべて元に戻ります。',
        '其用户将无法登录。不会删除任何内容：档案、证据与审计轨迹均保留，恢复后一切照旧。'),
    'platform.clients.suspend-action': (
        'Suspendre', 'Suspend', 'Suspender', 'تعليق', '停止する', '暂停'),
    'platform.clients.suspended': (
        'Client suspendu ; ses données sont conservées.',
        'Client suspended; its data is kept.',
        'Cliente suspendido; sus datos se conservan.',
        'تم تعليق العميل؛ وتُحفظ بياناته.',
        '顧客を停止しました。データは保持されます。',
        '客户已暂停，其数据予以保留。'),
    'platform.clients.suspend-failed': (
        "La suspension n'a pas pu être appliquée.",
        'The suspension could not be applied.',
        'La suspensión no pudo aplicarse.',
        'لم يتمكّن النظام من تطبيق التعليق.',
        '停止を適用できませんでした。',
        '暂停未能生效。'),
    'platform.clients.resumed': (
        'Client réactivé.', 'Client reactivated.', 'Cliente reactivado.',
        'تم إعادة تنشيط العميل.', '顧客を再開しました。', '客户已恢复。'),
    'platform.clients.resume-failed': (
        "La réactivation n'a pas pu être appliquée.",
        'The reactivation could not be applied.',
        'La reactivación no pudo aplicarse.',
        'لم يتمكّن النظام من تطبيق إعادة التنشيط.',
        '再開を適用できませんでした。',
        '恢复未能生效。'),

    # --- corriger un cabinet ------------------------------------------------
    'platform.firms.edit': (
        'Corriger', 'Edit', 'Corregir', 'تصحيح', '修正', '修改'),
    'platform.firms.edit-title': (
        'Corriger le cabinet', 'Edit the firm', 'Corregir el gabinete',
        'تصحيح بيانات المكتب', '事務所を修正', '修改事务所'),
    'platform.firms.saved': (
        'Fiche du cabinet mise à jour.', 'Firm details updated.',
        'Ficha del gabinete actualizada.', 'تم تحديث بطاقة المكتب.',
        '事務所の情報を更新しました。', '事务所资料已更新。'),
    'platform.firms.save-failed': (
        "La fiche du cabinet n’a pas pu être enregistrée.",
        'The firm details could not be saved.',
        'La ficha del gabinete no pudo guardarse.',
        'لم يتمكّن النظام من حفظ بطاقة المكتب.',
        '事務所の情報を保存できませんでした。',
        '事务所资料未能保存。'),

    # --- les deux blocs de l'habilitation -----------------------------------
    'admin.modules.group-core': (
        'Socle', 'Core', 'Base', 'الأساس', '基本機能', '基础'),
    'admin.modules.group-core-hint': (
        "Livré avec la plateforme : toujours actif, non facturé à l'unité.",
        'Shipped with the platform: always active, not billed per module.',
        'Incluido con la plataforma: siempre activo, no se factura por unidad.',
        'يأتي مع المنصّة: يبقى مفعّلًا دائمًا، ولا يُفاتر لكل وحدة.',
        'プラットフォームに同梱：常時有効で、モジュール単位の課金はありません。',
        '随平台提供：始终启用，不按模块计费。'),
    'admin.modules.group-extra': (
        'Modules additionnels', 'Additional modules', 'Módulos adicionales',
        'الوحدات الإضافية', '追加モジュール', '附加模块'),
    'admin.modules.group-extra-hint': (
        "Ouverts, essayés ou fermés client par client, selon l'offre.",
        'Opened, trialled or closed client by client, according to the plan.',
        'Abiertos, probados o cerrados cliente por cliente, según la oferta.',
        'تُفتح أو تُجرَّب أو تُغلق لكل عميل على حدة، وفق الخطة.',
        'プランに応じて、顧客ごとに開放・試用・終了します。',
        '按订阅方案逐客户开通、试用或关闭。'),
}
