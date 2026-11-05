# -*- coding: utf-8 -*-
"""Table i18n - console de l'éditeur de la plateforme. id: (fr, en, es, ar, ja, zh).

La console est l'outil de l'éditeur, mais elle est traduite comme le reste : un
intégrateur partenaire, un administrateur de plateforme chez un distributeur ou
une équipe d'exploitation hors de France l'utilisent aussi.
"""

TRANSLATIONS = {

    # --- coquille ----------------------------------------------------
    'platform.brand.console': (
        'Console éditeur', 'Publisher console', 'Consola del editor',
        'وحدة تحكم الناشر', '提供元コンソール', '发行方控制台'),
    'platform.nav.aria': (
        'Navigation de la console', 'Console navigation', 'Navegación de la consola',
        'التنقل في وحدة التحكم', 'コンソールのナビゲーション', '控制台导航'),
    'platform.topbar.badge': (
        'Administration de la plateforme', 'Platform administration',
        'Administración de la plataforma', 'إدارة المنصة',
        'プラットフォーム管理', '平台管理'),
    'platform.nav.clients': ('Clients', 'Clients', 'Clientes', 'العملاء', '顧客', '客户'),
    'platform.nav.clients.hint': (
        'Créer un client, son identité, ses modules',
        'Create a client, its identity, its modules',
        'Crear un cliente, su identidad, sus módulos',
        'إنشاء عميل وهويته ووحداته',
        '顧客の作成、そのアイデンティティとモジュール',
        '创建客户及其标识与模块'),
    'platform.nav.modules': (
        'Catalogue des modules', 'Module catalogue', 'Catálogo de módulos',
        'كتالوج الوحدات', 'モジュールカタログ', '模块目录'),
    'platform.nav.modules.hint': (
        'Ce que la plateforme sait livrer', 'What the platform can deliver',
        'Lo que la plataforma puede entregar', 'ما يمكن للمنصة تقديمه',
        'プラットフォームが提供できるもの', '平台可交付的内容'),
    'platform.nav.scope': (
        'Cette console n’ouvre aucun écran qualité : les données d’un client se '
        'consultent depuis le compte de ce client.',
        'This console opens no quality screen: a client’s data is viewed from that '
        'client’s own account.',
        'Esta consola no abre ninguna pantalla de calidad: los datos de un cliente se '
        'consultan desde la cuenta de ese cliente.',
        'لا تفتح وحدة التحكم هذه أي شاشة جودة: تُستعرض بيانات العميل من حساب ذلك العميل.',
        'このコンソールは品質画面を開きません。顧客のデータはその顧客のアカウントから参照します。',
        '本控制台不打开任何质量界面：客户数据需从该客户自己的账户查看。'),
    'platform.user.logout': (
        'Se déconnecter', 'Sign out', 'Cerrar sesión', 'تسجيل الخروج',
        'ログアウト', '退出登录'),

    # --- catalogue des modules ---------------------------------------
    'platform.modules.title': (
        'Catalogue des modules', 'Module catalogue', 'Catálogo de módulos',
        'كتالوج الوحدات', 'モジュールカタログ', '模块目录'),
    'platform.modules.subtitle': (
        'Tout ce que la plateforme sait livrer : le palier minimal de chaque module et '
        'ce qu’il exige d’autre pour fonctionner.',
        'Everything the platform can deliver: each module’s minimum tier and what else '
        'it requires to work.',
        'Todo lo que la plataforma puede entregar: el nivel mínimo de cada módulo y qué '
        'más necesita para funcionar.',
        'كل ما يمكن للمنصة تقديمه: الحد الأدنى من الفئة لكل وحدة وما تتطلبه للعمل.',
        'プラットフォームが提供できるすべて：各モジュールの最低プランと、動作に必要な前提。',
        '平台可交付的全部内容：每个模块的最低档位及其运行所需的前置模块。'),
    'platform.modules.reload': (
        'Recharger', 'Reload', 'Recargar', 'إعادة التحميل', '再読み込み', '重新加载'),
    'platform.modules.count': (
        '{$INTERPOLATION} modules au catalogue', '{$INTERPOLATION} modules in the catalogue',
        '{$INTERPOLATION} módulos en el catálogo', '{$INTERPOLATION} وحدة في الكتالوج',
        'カタログに {$INTERPOLATION} 件のモジュール', '目录中有 {$INTERPOLATION} 个模块'),
    'platform.modules.core-count': (
        'dont {$INTERPOLATION} acquis d’office', 'of which {$INTERPOLATION} granted by default',
        'de los cuales {$INTERPOLATION} incluidos de serie',
        'منها {$INTERPOLATION} مُمنوحة تلقائيًا',
        'うち {$INTERPOLATION} 件は標準で付与', '其中 {$INTERPOLATION} 个为默认包含'),
    'platform.modules.loading': (
        'Chargement du catalogue…', 'Loading the catalogue…', 'Cargando el catálogo…',
        'جارٍ تحميل الكتالوج…', 'カタログを読み込み中…', '正在加载目录…'),
    'platform.modules.core': (
        'Acquis d’office', 'Granted by default', 'Incluido de serie',
        'مُمنوحة تلقائيًا', '標準付与', '默认包含'),
    'platform.modules.requires': (
        'Exige :', 'Requires:', 'Requiere:', 'يتطلب:', '前提：', '需要：'),
    'platform.modules.empty': (
        'Le moteur qualité n’a renvoyé aucun module. Vérifiez qu’il est joignable.',
        'The quality engine returned no module. Check that it is reachable.',
        'El motor de calidad no devolvió ningún módulo. Compruebe que es accesible.',
        'لم يُرجع محرك الجودة أي وحدة. تحقّق من إمكانية الوصول إليه.',
        '品質エンジンがモジュールを返しませんでした。到達可能か確認してください。',
        '质量引擎未返回任何模块。请确认其可访问。'),
    'platform.modules.load-failed': (
        'Le catalogue des modules n’a pas pu être chargé.',
        'The module catalogue could not be loaded.',
        'No se pudo cargar el catálogo de módulos.',
        'لم يتم تحميل كتالوج الوحدات.',
        'モジュールカタログを読み込めませんでした。',
        '无法加载模块目录。'),
    'platform.modules.family.methodes': (
        'Méthodes de la qualité', 'Quality methods', 'Métodos de la calidad',
        'أساليب الجودة', '品質手法', '质量方法'),
    'platform.modules.family.transverse': (
        'Modules transverses', 'Cross-cutting modules', 'Módulos transversales',
        'الوحدات المشتركة', '横断モジュール', '横向模块'),
    'platform.modules.family.platform': (
        'Plateforme', 'Platform', 'Plataforma', 'المنصة', 'プラットフォーム', '平台'),
    'platform.modules.family.integrations': (
        'Intégrations', 'Integrations', 'Integraciones', 'التكاملات', '連携', '集成'),
}
