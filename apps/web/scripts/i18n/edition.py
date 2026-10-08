# -*- coding: utf-8 -*-
"""Libellés de l'édition on-premise et de sa licence (ADR 0082) : la page
« Licence », le bandeau d'échéance ou de lecture seule.

Placeholders : `${x}:nom:` côté TS s'écrit {$nom} ; `{{ x // i18n(ph="nom") }}`
côté gabarit s'écrit {$NOM}.
"""

TRANSLATIONS = {
    'nav.admin-license': ('Licence', 'License', 'Licencia', 'الترخيص', 'ライセンス', '许可证'),

    # ---------- page Licence ----------
    'license.title': ('Licence', 'License', 'Licencia', 'الترخيص', 'ライセンス', '许可证'),
    'license.subtitle': (
        "Ce que couvre la licence de cette installation, et jusqu'à quand.",
        'What this installation\'s license covers, and until when.',
        'Lo que cubre la licencia de esta instalación, y hasta cuándo.',
        'ما يغطيه ترخيص هذا التثبيت، وحتى متى.',
        'このインストールのライセンスの対象と有効期限。',
        '本安装的许可证涵盖哪些内容，以及有效期至何时。'),
    'license.saas': (
        "Cette installation est la plateforme QualitOS de l'éditeur : la licence ne s'y applique pas.",
        'This installation is the vendor\'s QualitOS platform: the license does not apply here.',
        'Esta instalación es la plataforma QualitOS del editor: la licencia no se aplica aquí.',
        'هذا التثبيت هو منصة QualitOS الخاصة بالناشر: لا ينطبق الترخيص هنا.',
        'このインストールは提供元の QualitOS プラットフォームです。ライセンスは適用されません。',
        '本安装是发行方的 QualitOS 平台：许可证不适用于此。'),
    'license.until': ("Valable jusqu'au", 'Valid until', 'Válida hasta', 'صالح حتى', '有効期限', '有效期至'),
    'license.grace-until': ("Délai de grâce jusqu'au", 'Grace period until', 'Periodo de gracia hasta',
                            'فترة السماح حتى', '猶予期間の終了', '宽限期至'),
    'license.tier': ('Palier', 'Tier', 'Nivel', 'المستوى', 'プラン', '级别'),
    'license.reference': ('Référence', 'Reference', 'Referencia', 'المرجع', '参照番号', '编号'),
    'license.users': ('Utilisateurs', 'Users', 'Usuarios', 'المستخدمون', 'ユーザー', '用户'),
    'license.users-count': (
        '{$ACTIVE} comptes actifs sur {$MAX}', '{$ACTIVE} active accounts out of {$MAX}',
        '{$ACTIVE} cuentas activas de {$MAX}', '{$ACTIVE} حسابات نشطة من أصل {$MAX}',
        '有効なアカウント {$ACTIVE} / {$MAX}', '活跃账户 {$ACTIVE} / {$MAX}'),
    'license.users-gauge': ('Places utilisées', 'Seats used', 'Plazas utilizadas', 'المقاعد المستخدمة',
                            '使用中の席数', '已用席位'),
    'license.users-help': (
        'Désactiver un compte libère une place.', 'Deactivating an account frees a seat.',
        'Desactivar una cuenta libera una plaza.', 'تعطيل حساب يحرر مقعدًا.',
        'アカウントを無効にすると席が空きます。', '停用账户即可释放一个席位。'),
    'license.users-unlimited': (
        "Nombre d'utilisateurs illimité.", 'Unlimited number of users.', 'Número de usuarios ilimitado.',
        'عدد غير محدود من المستخدمين.', 'ユーザー数無制限。', '用户数量不限。'),
    'license.modules': ('Modules ouverts', 'Modules included', 'Módulos incluidos', 'الوحدات المتاحة',
                        '利用可能なモジュール', '已开通模块'),
    'license.modules-all': (
        'Tous les modules de QualitOS.', 'All QualitOS modules.', 'Todos los módulos de QualitOS.',
        'جميع وحدات QualitOS.', 'QualitOS のすべてのモジュール。', 'QualitOS 全部模块。'),
    'license.modules-help': (
        'Les modules du socle (PDCA, CAPA, documents, audits…) sont toujours ouverts.',
        'Core modules (PDCA, CAPA, documents, audits…) are always included.',
        'Los módulos básicos (PDCA, CAPA, documentos, auditorías…) siempre están incluidos.',
        'الوحدات الأساسية (PDCA وCAPA والمستندات والتدقيق…) متاحة دائمًا.',
        '基本モジュール（PDCA、CAPA、文書、監査など）は常に利用できます。',
        '基础模块（PDCA、CAPA、文档、审核…）始终开通。'),
    'license.renew': ('Renouveler', 'Renew', 'Renovar', 'التجديد', '更新', '续期'),
    'license.renew-help': (
        "Votre prestataire remplace le fichier de licence de l'installation. La nouvelle licence est prise en compte en moins d'une minute, sans redémarrage ni interruption.",
        'Your provider replaces the installation\'s license file. The new license takes effect within a minute, with no restart or interruption.',
        'Su proveedor sustituye el archivo de licencia de la instalación. La nueva licencia se aplica en menos de un minuto, sin reinicio ni interrupción.',
        'يستبدل مزودك ملف ترخيص التثبيت. يُطبَّق الترخيص الجديد خلال أقل من دقيقة، دون إعادة تشغيل أو انقطاع.',
        'プロバイダーがインストールのライセンスファイルを差し替えます。新しいライセンスは再起動や中断なしに 1 分以内に反映されます。',
        '由您的服务商替换安装的许可证文件。新许可证在一分钟内生效，无需重启，不会中断服务。'),
    'license.read-only-help': (
        "Sans licence valable, l'installation passe en lecture seule : vos enregistrements restent consultables et exportables.",
        'Without a valid license, the installation becomes read-only: your records remain viewable and exportable.',
        'Sin una licencia válida, la instalación pasa a solo lectura: sus registros siguen siendo consultables y exportables.',
        'من دون ترخيص صالح، يصبح التثبيت للقراءة فقط: تبقى سجلاتك قابلة للاطلاع والتصدير.',
        '有効なライセンスがない場合、読み取り専用になります。記録の閲覧とエクスポートは引き続き可能です。',
        '没有有效许可证时，安装将变为只读：您的记录仍可查看和导出。'),

    'license.status.valid': ('Valide', 'Valid', 'Válida', 'صالح', '有効', '有效'),
    'license.status.grace': ('Échue — délai de grâce', 'Expired — grace period', 'Vencida — periodo de gracia',
                             'منتهٍ — فترة سماح', '期限切れ — 猶予期間中', '已过期 — 宽限期'),
    'license.status.expired': ('Échue — lecture seule', 'Expired — read-only', 'Vencida — solo lectura',
                               'منتهٍ — للقراءة فقط', '期限切れ — 読み取り専用', '已过期 — 只读'),
    'license.status.not-yet': ('Pas encore en vigueur', 'Not yet in effect', 'Aún no vigente',
                               'لم يدخل حيز التنفيذ بعد', 'まだ有効ではありません', '尚未生效'),
    'license.status.invalid': ('Non vérifiée', 'Not verified', 'No verificada', 'غير متحقق منه', '未検証', '未通过验证'),
    'license.status.missing': ('Absente', 'Missing', 'Ausente', 'غير موجود', '未インストール', '缺失'),
    'license.status.not-required': ('Sans objet', 'Not applicable', 'No aplica', 'لا ينطبق', '対象外', '不适用'),

    # ---------- bandeau ----------
    'license.banner.details': ('Voir la licence', 'View license', 'Ver la licencia', 'عرض الترخيص',
                               'ライセンスを表示', '查看许可证'),
    'license.banner.read-only-tail': (
        'Vos données restent consultables et exportables.', 'Your data remains viewable and exportable.',
        'Sus datos siguen siendo consultables y exportables.', 'تبقى بياناتك قابلة للاطلاع والتصدير.',
        'データは引き続き閲覧・エクスポートできます。', '您的数据仍可查看和导出。'),
    'license.banner.expires-soon': (
        'La licence expire le {$date} : pensez à la renouveler.',
        'The license expires on {$date}: remember to renew it.',
        'La licencia vence el {$date}: recuerde renovarla.',
        'ينتهي الترخيص في {$date}: تذكّر تجديده.',
        'ライセンスは {$date} に期限切れになります。更新をお忘れなく。',
        '许可证将于 {$date} 到期：请及时续期。'),
    'license.banner.grace': (
        "La licence a expiré le {$date}. Tout fonctionne jusqu'au {$grace} ; renouvelez-la avant cette date.",
        'The license expired on {$date}. Everything works until {$grace}; renew it before then.',
        'La licencia venció el {$date}. Todo funciona hasta el {$grace}; renuévela antes de esa fecha.',
        'انتهى الترخيص في {$date}. يعمل كل شيء حتى {$grace}؛ جدّده قبل ذلك التاريخ.',
        'ライセンスは {$date} に期限切れになりました。{$grace} まではすべて利用できます。それまでに更新してください。',
        '许可证已于 {$date} 到期。在 {$grace} 之前一切照常；请在此之前续期。'),
    'license.banner.expired': (
        "Licence expirée : l'installation est en lecture seule.", 'License expired: the installation is read-only.',
        'Licencia vencida: la instalación está en solo lectura.', 'انتهى الترخيص: التثبيت للقراءة فقط.',
        'ライセンス期限切れ：読み取り専用です。', '许可证已过期：安装为只读。'),
    'license.banner.not-yet': (
        "La licence n'est pas encore en vigueur : l'installation est en lecture seule.",
        'The license is not yet in effect: the installation is read-only.',
        'La licencia aún no está vigente: la instalación está en solo lectura.',
        'الترخيص لم يدخل حيز التنفيذ بعد: التثبيت للقراءة فقط.',
        'ライセンスはまだ有効ではありません：読み取り専用です。',
        '许可证尚未生效：安装为只读。'),
    'license.banner.missing': (
        "Aucune licence installée : l'installation est en lecture seule.",
        'No license installed: the installation is read-only.',
        'No hay ninguna licencia instalada: la instalación está en solo lectura.',
        'لا يوجد ترخيص مثبت: التثبيت للقراءة فقط.',
        'ライセンスがインストールされていません：読み取り専用です。',
        '未安装许可证：安装为只读。'),
    'license.banner.invalid': (
        "La licence n'a pas pu être vérifiée : l'installation est en lecture seule.",
        'The license could not be verified: the installation is read-only.',
        'No se pudo verificar la licencia: la instalación está en solo lectura.',
        'تعذّر التحقق من الترخيص: التثبيت للقراءة فقط.',
        'ライセンスを検証できませんでした：読み取り専用です。',
        '无法验证许可证：安装为只读。'),
}
