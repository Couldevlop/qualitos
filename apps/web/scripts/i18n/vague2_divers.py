# -*- coding: utf-8 -*-
"""Table i18n - complements transverses (navigation, accueil, commun). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {

    # --- minutes -----------------------------------------------------
    'circles.minutes.transcribe-action': ('Importer un enregistrement', 'Import a recording', 'Importar una grabación', 'استيراد تسجيل', '録音を取り込む', '导入录音'),
    'circles.minutes.transcribe-aria': ('Importer un enregistrement audio de la réunion', 'Import an audio recording of the meeting', 'Importar una grabación de audio de la reunión', 'استيراد تسجيل صوتي للاجتماع', '会議の録音音声を取り込む', '导入会议录音'),
    'circles.minutes.transcribe-error': ('Transcription audio indisponible.', 'Audio transcription unavailable.', 'Transcripción de audio no disponible.', 'التفريغ النصي للصوت غير متاح.', '音声の文字起こしを利用できません。', '音频转写不可用。'),
    'circles.minutes.transcribe-hint': ("L'audio est transcrit puis relu par vos soins avant génération.", 'The audio is transcribed, then reviewed by you before generation.', 'El audio se transcribe y usted lo revisa antes de generar.', 'يُفرَّغ الصوت نصيًا ثم تراجعه بنفسك قبل التوليد.', '音声を文字起こしし、生成前にご自身で確認します。', '音频将被转写，由您审阅后再生成。'),
    'circles.minutes.transcribing': ('Transcription en cours…', 'Transcribing…', 'Transcribiendo…', 'جارٍ التفريغ النصي…', '文字起こし中…', '正在转写…'),

    # --- common ------------------------------------------------------
    'common.details': ('Détails', 'Details', 'Detalles', 'التفاصيل', '詳細', '详情'),
    'common.progress': ('Avancement', 'Progress', 'Progreso', 'التقدم', '進捗', '进度'),
    'common.refresh': ('Actualiser', 'Refresh', 'Actualizar', 'تحديث', '更新', '刷新'),

    # --- ai ----------------------------------------------------------
    'compliance.ai.systems': ("Registre des systèmes d'IA", 'AI systems register', 'Registro de sistemas de IA', 'سجل أنظمة الذكاء الاصطناعي', 'AIシステム登録簿', '人工智能系统登记册'),

    # --- exec --------------------------------------------------------
    'dashboard.exec.pareto-subtitle': ('Catégories 6M sur 30 jours', '6M categories over 30 days', 'Categorías 6M en 30 días', 'فئات 6M خلال 30 يومًا', '30日間の6Mカテゴリ', '30 天内的 6M 类别'),

    # --- card --------------------------------------------------------
    'home.card.audits-desc': ('Plans + checklists + findings', 'Plans + checklists + findings', 'Planes + listas de verificación + hallazgos', 'خطط + قوائم تحقق + ملاحظات', '計画 + チェックリスト + 指摘事項', '计划 + 检查清单 + 发现项'),
    'home.card.audits-title': ('Audits', 'Audits', 'Auditorías', 'عمليات التدقيق', '監査', '审核'),
    'home.card.capa-desc': ('Actions correctives & préventives', 'Corrective & preventive actions', 'Acciones correctivas y preventivas', 'إجراءات تصحيحية ووقائية', '是正・予防処置', '纠正与预防措施'),
    'home.card.standards-desc': ('Catalogue normatif + adoption + alignment', 'Standards catalogue + adoption + alignment', 'Catálogo normativo + adopción + alineación', 'كتالوج المعايير + التبنّي + المواءمة', '規格カタログ + 導入 + 適合', '标准目录 + 采纳 + 对齐'),

    # --- welcome -----------------------------------------------------
    'home.welcome.soon': ('Bientôt', 'Coming soon', 'Próximamente', 'قريبًا', '近日公開', '即将推出'),

    # --- nav ---------------------------------------------------------
    'nav.admin-api-keys': ("Clés d'API", 'API keys', 'Claves de API', 'مفاتيح API', 'APIキー', 'API 密钥'),
    'nav.admin-audit-log': ("Journal d'audit", 'Audit log', 'Registro de auditoría', 'سجل التدقيق', '監査ログ', '审计日志'),
    'nav.admin-connectors': ('Connecteurs', 'Connectors', 'Conectores', 'الموصّلات', 'コネクタ', '连接器'),
    'nav.admin-modules': ('Modules du tenant', 'Tenant modules', 'Módulos del inquilino', 'وحدات المستأجر', 'テナントのモジュール', '租户模块'),
    'nav.admin-quotas': ("Quotas d'API", 'API quotas', 'Cuotas de API', 'حصص API', 'APIクォータ', 'API 配额'),
    'nav.admin-webhooks': ('Webhooks', 'Webhooks', 'Webhooks', 'خطافات الويب', 'Webhook', 'Webhook'),
    'nav.administration': ('Administration', 'Administration', 'Administración', 'الإدارة', '管理', '管理'),
    'nav.calibration': ('Calibration', 'Calibration', 'Calibración', 'المعايرة', '校正', '校准'),
    'nav.conformite': ('Conformité', 'Compliance', 'Cumplimiento', 'الامتثال', 'コンプライアンス', '合规'),
    'nav.conformite-cyber-nis-2': ('Conformité — Cyber (NIS 2)', 'Compliance — Cyber (NIS 2)', 'Cumplimiento — Ciber (NIS 2)', 'الامتثال — السيبراني (NIS 2)', 'コンプライアンス — サイバー（NIS 2）', '合规 — 网络（NIS 2）'),
    'nav.conformite-donnees-rgpd': ('Conformité — Données (RGPD)', 'Compliance — Data (GDPR)', 'Cumplimiento — Datos (RGPD)', 'الامتثال — البيانات (GDPR)', 'コンプライアンス — データ（GDPR）', '合规 — 数据（GDPR）'),
    'nav.conformite-ia-ai-act': ('Conformité — IA (AI Act)', 'Compliance — AI (AI Act)', 'Cumplimiento — IA (AI Act)', 'الامتثال — الذكاء الاصطناعي (AI Act)', 'コンプライアンス — AI（AI法）', '合规 — AI（AI 法案）'),
    'nav.consentements': ('Consentements', 'Consents', 'Consentimientos', 'الموافقات', '同意管理', '同意管理'),
    'nav.decisions-auto': ('Décisions auto.', 'Automated decisions', 'Decisiones autom.', 'القرارات الآلية', '自動意思決定', '自动化决策'),
    'nav.demandes-dsar': ('Demandes (DSAR)', 'Requests (DSAR)', 'Solicitudes (DSAR)', 'الطلبات (DSAR)', '請求（DSAR）', '请求（DSAR）'),
    'nav.dpia': ('DPIA', 'DPIA', 'EIPD', 'تقييم الأثر (DPIA)', 'DPIA', 'DPIA'),
    'nav.dpo': ('DPO', 'DPO', 'DPD', 'مسؤول حماية البيانات', 'DPO', 'DPO'),
    'nav.eudb': ('EUDB', 'EUDB', 'EUDB', 'EUDB', 'EUDB', 'EUDB'),
    'nav.fournisseurs-competences': ('Fournisseurs & compétences', 'Suppliers & skills', 'Proveedores y competencias', 'الموردون والكفاءات', 'サプライヤーとスキル', '供应商与能力'),
    'nav.fria': ('FRIA', 'FRIA', 'FRIA', 'FRIA', 'FRIA', 'FRIA'),
    'nav.incidents': ('Incidents', 'Incidents', 'Incidentes', 'الحوادث', 'インシデント', '事件'),
    'nav.incidents-cyber': ('Incidents cyber', 'Cyber incidents', 'Incidentes ciber', 'الحوادث السيبرانية', 'サイバーインシデント', '网络事件'),
    'nav.iot': ('Parc IoT', 'IoT fleet', 'Parque IoT', 'أسطول إنترنت الأشياء', 'IoT機器', '物联网设备'),
    'nav.itsm': ('ITSM', 'ITSM', 'ITSM', 'ITSM', 'ITSM', 'ITSM'),
    'nav.mentions': ('Mentions', 'Notices', 'Avisos', 'الإشعارات', 'プライバシー通知', '隐私声明'),
    'nav.mesures': ('Mesures', 'Measures', 'Medidas', 'التدابير', '対策', '措施'),
    'nav.normes-certification': ('Normes & certification', 'Standards & certification', 'Normas y certificación', 'المعايير والاعتماد', '規格と認証', '标准与认证'),
    'nav.pmm': ('PMM', 'PMM', 'PMM', 'PMM', 'PMM', 'PMM'),
    'nav.qms': ('QMS', 'QMS', 'QMS', 'نظام إدارة الجودة', 'QMS', 'QMS'),
    'nav.qualite-operationnelle': ('Qualité opérationnelle', 'Operational quality', 'Calidad operativa', 'الجودة التشغيلية', 'オペレーション品質', '运营质量'),
    'nav.reclamations': ('Réclamations', 'Complaints', 'Reclamaciones', 'الشكاوى', '苦情', '投诉'),
    'nav.registre-ropa': ('Registre (RoPA)', 'Register (RoPA)', 'Registro (RoPA)', 'السجل (RoPA)', '処理記録（RoPA）', '记录（RoPA）'),
    'nav.retention': ('Rétention', 'Retention', 'Retención', 'الاحتفاظ', '保持期間', '保留'),
    'nav.sous-traitants-dpa': ('Sous-traitants (DPA)', 'Processors (DPA)', 'Encargados (DPA)', 'المعالجون (DPA)', '処理者（DPA）', '处理方（DPA）'),
    'nav.standards-ims': ('Co-couverture IMS', 'IMS co-coverage', 'Cocobertura IMS', 'التغطية المشتركة IMS', 'IMS 共通カバレッジ', 'IMS 共同覆盖'),
    'nav.transferts': ('Transferts', 'Transfers', 'Transferencias', 'عمليات النقل', '越境移転', '数据传输'),
    'nav.violations': ('Violations', 'Breaches', 'Brechas', 'الانتهاكات', '侵害', '数据泄露'),

    # --- notifications -----------------------------------------------
    'shell.notifications.empty': ('Aucune notification.', 'No notifications.', 'Sin notificaciones.', 'لا توجد إشعارات.', '通知はありません。', '暂无通知。'),
    'shell.notifications.mark-all': ('Tout marquer comme lu', 'Mark all as read', 'Marcar todo como leído', 'تعليم الكل كمقروء', 'すべて既読にする', '全部标为已读'),
    'shell.notifications.title': ('Notifications', 'Notifications', 'Notificaciones', 'الإشعارات', '通知', '通知'),

    # --- user --------------------------------------------------------
    'shell.user.logout': ('Se déconnecter', 'Sign out', 'Cerrar sesión', 'تسجيل الخروج', 'ログアウト', '退出登录'),
    'shell.user.offline-queue': ('File de synchronisation', 'Sync queue', 'Cola de sincronización', 'قائمة المزامنة', '同期キュー', '同步队列'),
}
