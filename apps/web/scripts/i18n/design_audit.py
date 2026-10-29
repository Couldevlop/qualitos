# -*- coding: utf-8 -*-
"""Table i18n - corrections de l'audit UI/UX (barre du haut, énumérations,
paginateur, états vides, accès réservé…). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    # Barre du haut sur téléphone : recherche repliée et thème dans le profil.
    'shell.search.open-aria': ('Rechercher un écran', 'Search for a screen', 'Buscar una pantalla', 'البحث عن شاشة', '画面を検索', '搜索界面'),
    'theme.toggle.aria': ('Préférence de thème', 'Theme preference', 'Preferencia de tema', 'تفضيل السمة', 'テーマの設定', '主题偏好'),
    'theme.toggle.light': ('Thème clair', 'Light theme', 'Tema claro', 'السمة الفاتحة', 'ライトテーマ', '浅色主题'),
    'theme.toggle.system': ('Suivre le système', 'Follow system', 'Seguir el sistema', 'اتباع النظام', 'システムに従う', '跟随系统'),
    'theme.toggle.dark': ('Thème sombre', 'Dark theme', 'Tema oscuro', 'السمة الداكنة', 'ダークテーマ', '深色主题'),

    # Nom accessible des lignes et cartes cliquables.
    'common.open-item': ('Ouvrir {$name}', 'Open {$name}', 'Abrir {$name}', 'فتح {$name}', '{$name} を開く', '打开 {$name}'),

    # Modes d'Ishikawa.
    'enum.ishikawa-mode.six-m': ('6M', '6M', '6M', '6M', '6M', '6M'),
    'enum.ishikawa-mode.seven-m': ('7M', '7M', '7M', '7M', '7M', '7M'),
    'enum.ishikawa-mode.eight-m': ('8M', '8M', '8M', '8M', '8M', '8M'),

    # Catalogue des modules (§10.4).
    'enum.module.pdca': ('Cycles PDCA', 'PDCA cycles', 'Ciclos PDCA', 'دورات PDCA', 'PDCAサイクル', 'PDCA 循环'),
    'enum.module.ishikawa': ("Diagrammes d'Ishikawa", 'Ishikawa diagrams', 'Diagramas de Ishikawa', 'مخططات إيشيكاوا', '特性要因図', '石川图'),
    'enum.module.fives': ('Audits 5S', '5S audits', 'Auditorías 5S', 'تدقيقات 5S', '5S監査', '5S 审核'),
    'enum.module.circle': ('Cercles de qualité', 'Quality circles', 'Círculos de calidad', 'حلقات الجودة', 'QCサークル', '质量圈'),
    'enum.module.dmaic': ('DMAIC et Poka-Yoke', 'DMAIC and Poka-Yoke', 'DMAIC y Poka-Yoke', 'DMAIC وبوكا يوكي', 'DMAICとポカヨケ', 'DMAIC 与防错'),
    'enum.module.capa': ('CAPA', 'CAPA', 'CAPA', 'CAPA', 'CAPA', 'CAPA'),
    'enum.module.docs': ('Maîtrise documentaire', 'Document control', 'Control documental', 'التحكم في الوثائق', '文書管理', '文件控制'),
    'enum.module.audit': ('Gestion des audits', 'Audit management', 'Gestión de auditorías', 'إدارة التدقيق', '監査管理', '审核管理'),
    'enum.module.risk': ('Risques et AMDEC', 'Risks and FMEA', 'Riesgos y AMFE', 'المخاطر وتحليل FMEA', 'リスクとFMEA', '风险与 FMEA'),
    'enum.module.product': ('Référentiel produit', 'Product referential', 'Referencial de productos', 'مرجع المنتجات', '製品マスター', '产品参考库'),
    'enum.module.controlplan': ('Plan de surveillance', 'Control plan', 'Plan de control', 'خطة الضبط', 'コントロールプラン', '控制计划'),
    'enum.module.supplier': ('Qualité fournisseurs', 'Supplier quality', 'Calidad de proveedores', 'جودة الموردين', 'サプライヤー品質', '供应商质量'),
    'enum.module.training': ('Formation et compétences', 'Training and competency', 'Formación y competencias', 'التدريب والكفاءات', '教育と力量', '培训与能力'),
    'enum.module.change': ('Gestion des changements', 'Change management', 'Gestión de cambios', 'إدارة التغيير', '変更管理', '变更管理'),
    'enum.module.complaints': ('Réclamations clients', 'Customer complaints', 'Reclamaciones de clientes', 'شكاوى العملاء', '顧客苦情', '客户投诉'),
    'enum.module.calibration': ('Étalonnage et équipements', 'Calibration and equipment', 'Calibración y equipos', 'المعايرة والمعدات', '校正と設備', '校准与设备'),
    'enum.module.ehs': ('Incidents HSE', 'EHS incidents', 'Incidentes de SSMA', 'حوادث الصحة والسلامة والبيئة', 'EHSインシデント', 'EHS 事件'),
    'enum.module.standards': ('Standards Hub', 'Standards Hub', 'Standards Hub', 'Standards Hub', 'Standards Hub', 'Standards Hub'),
    'enum.module.kpi': ('Moteur de KPI', 'KPI engine', 'Motor de KPI', 'محرك مؤشرات الأداء', 'KPIエンジン', 'KPI 引擎'),
    'enum.module.industry': ('Packs sectoriels', 'Industry packs', 'Paquetes sectoriales', 'الحزم القطاعية', '業界パック', '行业包'),
    'enum.module.iot': ('Hub IoT', 'IoT hub', 'Hub IoT', 'مركز إنترنت الأشياء', 'IoTハブ', '物联网中心'),
    'enum.module.auditlog': ("Journal d'audit", 'Audit log', 'Registro de auditoría', 'سجل التدقيق', '監査ログ', '审计日志'),
    'enum.module.blockchain': ('Ancrage blockchain', 'Blockchain anchoring', 'Anclaje en blockchain', 'الإرساء على سلسلة الكتل', 'ブロックチェーン・アンカリング', '区块链锚定'),
    'enum.module.webhooks': ('Webhooks sortants', 'Outbound webhooks', 'Webhooks salientes', 'خطافات الويب الصادرة', '送信Webhook', '出站 Webhook'),
    'enum.module.itsm': ('Connecteurs ITSM', 'ITSM connectors', 'Conectores ITSM', 'موصلات ITSM', 'ITSMコネクタ', 'ITSM 连接器'),
    'enum.module-category.methodes': ('Méthodes qualité', 'Quality methods', 'Métodos de calidad', 'أساليب الجودة', '品質手法', '质量方法'),
    'enum.module-category.transverse': ('Modules transverses', 'Cross-functional modules', 'Módulos transversales', 'وحدات مشتركة', '横断モジュール', '横向模块'),
    'enum.module-category.platform': ('Plateforme', 'Platform', 'Plataforma', 'المنصة', 'プラットフォーム', '平台'),
    'enum.module-category.integrations': ('Intégrations', 'Integrations', 'Integraciones', 'التكاملات', '連携', '集成'),
    'enum.billing-tier.free': ('Gratuit', 'Free', 'Gratuito', 'مجاني', '無料', '免费'),
    'enum.billing-tier.standard': ('Standard', 'Standard', 'Estándar', 'قياسي', 'スタンダード', '标准版'),
    'enum.billing-tier.pro': ('Pro', 'Pro', 'Pro', 'احترافي', 'プロ', '专业版'),
    'enum.billing-tier.enterprise': ('Entreprise', 'Enterprise', 'Empresa', 'المؤسسات', 'エンタープライズ', '企业版'),

    # Accueil : indicateurs lus dans les modules (plus de valeurs inventées).
    'home.kpi.open-nc-internal': ('Non-conformités internes ouvertes', 'Open internal non-conformances', 'No conformidades internas abiertas', 'حالات عدم المطابقة الداخلية المفتوحة', '未対応の社内不適合', '未结内部不合格项'),
    'home.kpi.fives-sample': ('Sur {$count} audits terminés', 'Across {$count} completed audits', 'Sobre {$count} auditorías finalizadas', 'على {$count} تدقيقات مكتملة', '完了した監査 {$count} 件', '基于 {$count} 次已完成审核'),
    'home.kpi.fives-none': ('Aucun audit terminé', 'No completed audit', 'Ninguna auditoría finalizada', 'لا يوجد تدقيق مكتمل', '完了した監査はありません', '暂无已完成审核'),
    'home.kpi.unavailable': ('Indisponible — ouvrir le module', 'Unavailable — open the module', 'No disponible — abrir el módulo', 'غير متاح — افتح الوحدة', '取得できません — モジュールを開く', '暂不可用 — 打开模块'),

    # Tableau de bord exécutif.
    'dashboard.exec.generated-at': ('Données du {$INTERPOLATION}', 'Data as of {$INTERPOLATION}', 'Datos del {$INTERPOLATION}', 'بيانات بتاريخ {$INTERPOLATION}', '{$INTERPOLATION} 時点のデータ', '数据时间：{$INTERPOLATION}'),
    'dashboard.exec.trend-empty-title': ('Pas encore de mesure', 'No measurement yet', 'Aún no hay mediciones', 'لا توجد قياسات بعد', 'まだ測定値がありません', '暂无测量数据'),
    'dashboard.exec.trend-empty-hint': ('La courbe se trace dès que les indicateurs de qualité reçoivent leurs premières valeurs.', 'The curve appears as soon as the quality indicators receive their first values.', 'La curva aparece en cuanto los indicadores de calidad reciben sus primeros valores.', 'يظهر المنحنى بمجرد أن تتلقى مؤشرات الجودة قيمها الأولى.', '品質指標に最初の値が入ると曲線が表示されます。', '质量指标获得首批数值后即会绘制曲线。'),
    'dashboard.exec.norms-tracked-count': ('Normes suivies : {$count}', 'Standards tracked: {$count}', 'Normas seguidas: {$count}', 'المعايير المتابعة: {$count}', '追跡中の規格：{$count}', '跟踪的标准：{$count}'),
    'dashboard.chart.overall-quality': ('Qualité globale', 'Overall quality', 'Calidad global', 'الجودة الإجمالية', '総合品質', '整体质量'),
    'dashboard.chart.target': ('Cible', 'Target', 'Objetivo', 'الهدف', '目標', '目标'),
    'dashboard.chart.defects': ('Défauts', 'Defects', 'Defectos', 'العيوب', '不具合', '缺陷'),
    'dashboard.chart.cumulative': ('Cumul', 'Cumulative', 'Acumulado', 'التراكمي', '累積', '累计'),
    'dashboard.chart.cumulative-pct': ('Cumul %', 'Cumulative %', 'Acumulado %', 'التراكمي %', '累積 %', '累计 %'),
    'dashboard.csv.indicator': ('Indicateur', 'Indicator', 'Indicador', 'المؤشر', '指標', '指标'),
    'dashboard.csv.value': ('Valeur', 'Value', 'Valor', 'القيمة', '値', '数值'),
    'dashboard.csv.unit': ('Unité', 'Unit', 'Unidad', 'الوحدة', '単位', '单位'),
    'dashboard.csv.target': ('Cible', 'Target', 'Objetivo', 'الهدف', '目標', '目标'),
    'dashboard.csv.trend': ('Variation', 'Change', 'Variación', 'التغير', '変化', '变化'),
    'dashboard.csv.state': ('État', 'Status', 'Estado', 'الحالة', '状態', '状态'),

    # Écran « accès réservé » (garde de rôle).
    'restricted.title': ("Accès réservé à l'administrateur", 'Restricted to administrators', 'Acceso reservado al administrador', 'الوصول مقصور على المسؤول', '管理者専用ページです', '仅限管理员访问'),
    'restricted.hint': ("Cette page relève de l'administration de votre organisation. Si vous en avez besoin, demandez à votre administrateur de vous attribuer le rôle correspondant.", "This page belongs to your organisation's administration. If you need it, ask your administrator to grant you the matching role.",'Esta página forma parte de la administración de su organización. Si la necesita, pida a su administrador que le asigne el rol correspondiente.', 'تخص هذه الصفحة إدارة مؤسستك. إذا كنت بحاجة إليها، فاطلب من المسؤول منحك الدور المناسب.', 'このページは組織の管理機能です。必要な場合は、管理者に該当するロールの付与を依頼してください。', '此页面属于贵组织的管理功能。如需使用，请联系管理员为您分配相应角色。'),
    # Paginateur Material (MatPaginatorIntl global).
    'paginator.items-per-page': ('Éléments par page :', 'Items per page:', 'Elementos por página:', 'العناصر في كل صفحة:', '1ページあたりの件数：', '每页条数：'),
    'paginator.next': ('Page suivante', 'Next page', 'Página siguiente', 'الصفحة التالية', '次のページ', '下一页'),
    'paginator.previous': ('Page précédente', 'Previous page', 'Página anterior', 'الصفحة السابقة', '前のページ', '上一页'),
    'paginator.first': ('Première page', 'First page', 'Primera página', 'الصفحة الأولى', '最初のページ', '第一页'),
    'paginator.last': ('Dernière page', 'Last page', 'Última página', 'الصفحة الأخيرة', '最後のページ', '最后一页'),
    'paginator.range-empty': ('0 sur {$total}', '0 of {$total}', '0 de {$total}', '0 من {$total}', '{$total} 件中 0 件', '共 {$total} 条，显示 0 条'),
    'paginator.range': ('{$start} – {$end} sur {$total}', '{$start} – {$end} of {$total}', '{$start} – {$end} de {$total}', '{$start} – {$end} من {$total}', '{$total} 件中 {$start} – {$end} 件', '第 {$start} – {$end} 条，共 {$total} 条'),
    # Parcours de formation.
    'training.path-detail.passing': ('Seuil de réussite ≥ {$INTERPOLATION} %', 'Pass mark ≥ {$INTERPOLATION} %', 'Umbral de aprobación ≥ {$INTERPOLATION} %', 'عتبة النجاح ≥ {$INTERPOLATION} %', '合格基準 ≥ {$INTERPOLATION} %', '通过线 ≥ {$INTERPOLATION} %'),
    'training.path-detail.validity': ('Validité {$INTERPOLATION} mois', 'Valid for {$INTERPOLATION} months', 'Validez {$INTERPOLATION} meses', 'الصلاحية {$INTERPOLATION} شهرًا', '有効期間 {$INTERPOLATION} か月', '有效期 {$INTERPOLATION} 个月'),
}
