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
}
