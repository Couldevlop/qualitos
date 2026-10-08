# -*- coding: utf-8 -*-
"""Libellés du tableau de bord SMI (docs/Tableau de bord SMI.pptx, ADR 0077) et
des retouches du registre : action CAPA d'un risque (nature, responsable,
échéance obligatoire), action unique d'une CAPA issue d'un risque.

Placeholders : `${x}:nom:` côté TS s'écrit {$nom} (casse conservée) ;
`{{ x // i18n(ph="nom") }}` côté gabarit s'écrit {$NOM} (extrait en majuscules).
"""

TRANSLATIONS = {
    # ---------- navigation ----------
    'nav.smi': (
        'Tableau de bord SMI', 'IMS dashboard', 'Panel del SGI',
        'لوحة نظام الإدارة المتكامل', '統合マネジメントシステム ダッシュボード', '综合管理体系仪表板'),

    # ---------- registre : action CAPA d'un risque ----------
    'rr.action.kind': (
        "Nature de l'action", 'Type of action', 'Naturaleza de la acción',
        'طبيعة الإجراء', 'アクションの種類', '措施性质'),
    'rr.action.kind-short': ('Nature', 'Type', 'Naturaleza', 'الطبيعة', '種類', '性质'),
    'rr.action.kind-hint': (
        "Préventive : l'écart ne s'est pas encore produit. Corrective : il s'est déjà produit, on en supprime la cause.",
        'Preventive: the deviation has not occurred yet. Corrective: it has already occurred, and its cause is removed.',
        'Preventiva: la desviación aún no se ha producido. Correctiva: ya se ha producido y se elimina su causa.',
        'وقائي: لم يقع الانحراف بعد. تصحيحي: وقع بالفعل ويُزال سببه.',
        '予防：逸脱はまだ発生していない。是正：既に発生しており、その原因を取り除く。',
        '预防：偏差尚未发生。纠正：偏差已经发生，需消除其原因。'),
    'rr.action.assignee': (
        "Responsable de l'action", 'Action owner', 'Responsable de la acción',
        'مسؤول الإجراء', 'アクション責任者', '措施负责人'),
    'rr.action.assignee-short': ('Responsable', 'Owner', 'Responsable', 'المسؤول', '責任者', '负责人'),
    'rr.capa-kind.preventive': ('Préventive', 'Preventive', 'Preventiva', 'وقائي', '予防', '预防'),
    'rr.capa-kind.corrective': ('Corrective', 'Corrective', 'Correctiva', 'تصحيحي', '是正', '纠正'),

    # ---------- fiche CAPA : action unique d'un dossier issu d'un risque ----------
    'capa.detail.single-action': ('Action', 'Action', 'Acción', 'الإجراء', 'アクション', '措施'),
    'capa.detail.single-action-assignee': ('Responsable', 'Owner', 'Responsable', 'المسؤول', '責任者', '负责人'),

    # ---------- tableau de bord ----------
    'smi.eyebrow': ('Pilotage', 'Steering', 'Pilotaje', 'القيادة', '運営管理', '管理驾驶'),
    'smi.title': (
        'Système de Management Intégré', 'Integrated Management System', 'Sistema de Gestión Integrado',
        'نظام الإدارة المتكامل', '統合マネジメントシステム', '综合管理体系'),
    'smi.subtitle': (
        "Vue consolidée : une preuve compte pour toutes les normes qu'elle couvre.",
        'Consolidated view: one piece of evidence counts for every standard it covers.',
        'Vista consolidada: una evidencia cuenta para todas las normas que cubre.',
        'عرض موحّد: يُحتسب الدليل لكل المعايير التي يغطيها.',
        '統合ビュー：1つの証拠は、それが対象とするすべての規格に有効です。',
        '综合视图：一项证据适用于其覆盖的所有标准。'),
    'smi.filter': (
        'Filtrer par norme', 'Filter by standard', 'Filtrar por norma',
        'تصفية حسب المعيار', '規格で絞り込み', '按标准筛选'),
    'smi.filter-all': ('Tous', 'All', 'Todas', 'الكل', 'すべて', '全部'),
    'smi.load-failed': (
        "Le tableau de bord SMI n'a pas pu être chargé.", 'The IMS dashboard could not be loaded.',
        'No se pudo cargar el panel del SGI.', 'تعذّر تحميل لوحة نظام الإدارة المتكامل.',
        '統合マネジメントシステムのダッシュボードを読み込めませんでした。', '无法加载综合管理体系仪表板。'),
    'smi.kpis': ('Indicateurs clés', 'Key indicators', 'Indicadores clave', 'المؤشرات الرئيسية', '主要指標', '关键指标'),
    'smi.kpi.compliance': ('Conformité globale', 'Overall compliance', 'Conformidad global', 'الامتثال الإجمالي', '総合適合率', '总体符合率'),
    'smi.kpi.no-standard': (
        'Aucune norme adoptée.', 'No standard adopted.', 'Ninguna norma adoptada.',
        'لم يُعتمد أي معيار.', '採用された規格はありません。', '尚未采用任何标准。'),
    'smi.kpi.adopt': ('Adopter une norme', 'Adopt a standard', 'Adoptar una norma', 'اعتماد معيار', '規格を採用', '采用标准'),
    'smi.kpi.overdue': ('Actions en retard', 'Overdue actions', 'Acciones atrasadas', 'إجراءات متأخرة', '期限超過のアクション', '逾期措施'),
    'smi.kpi.overdue-critical': (
        'dont {$CRITICAL} critiques', 'including {$CRITICAL} critical', 'de ellas {$CRITICAL} críticas',
        'منها {$CRITICAL} حرجة', 'うち重大 {$CRITICAL} 件', '其中 {$CRITICAL} 项严重'),
    'smi.kpi.major-risks': (
        'Risques majeurs ouverts', 'Open major risks', 'Riesgos mayores abiertos',
        'مخاطر كبرى مفتوحة', '未解決の重大リスク', '未关闭的重大风险'),
    'smi.kpi.major-risks-detail': (
        '{$CRITICAL} critiques · {$HIGH} élevés (niveau ≥ 10)',
        '{$CRITICAL} critical · {$HIGH} high (level ≥ 10)',
        '{$CRITICAL} críticos · {$HIGH} altos (nivel ≥ 10)',
        '{$CRITICAL} حرجة · {$HIGH} مرتفعة (المستوى ≥ 10)',
        '重大 {$CRITICAL} · 高 {$HIGH}（レベル ≥ 10）',
        '{$CRITICAL} 项严重 · {$HIGH} 项高（等级 ≥ 10）'),
    'smi.kpi.next-audit': ('Prochain audit', 'Next audit', 'Próxima auditoría', 'التدقيق القادم', '次回監査', '下次审核'),
    'smi.kpi.in-days': ('dans {$DAYS} j', 'in {$DAYS} d', 'en {$DAYS} d', 'خلال {$DAYS} يوم', '{$DAYS} 日後', '{$DAYS} 天后'),
    'smi.kpi.no-audit': (
        'Aucun audit planifié.', 'No audit planned.', 'Ninguna auditoría planificada.',
        'لا يوجد تدقيق مخطط.', '計画された監査はありません。', '没有计划的审核。'),
    'smi.kpi.plan-audit': ('Planifier', 'Plan one', 'Planificar', 'التخطيط', '計画する', '安排'),
    'smi.by-standard': (
        'Conformité par référentiel', 'Compliance by standard', 'Conformidad por norma',
        'الامتثال حسب المرجع', '規格別の適合率', '按标准的符合率'),
    'smi.by-standard-note': (
        'Une même preuve (procédure, audit, action) est comptée pour tous les référentiels concernés.',
        'A single piece of evidence (procedure, audit, action) counts for every standard concerned.',
        'Una misma evidencia (procedimiento, auditoría, acción) cuenta para todas las normas afectadas.',
        'يُحتسب الدليل الواحد (إجراء موثّق، تدقيق، إجراء تصحيحي) لكل المراجع المعنية.',
        '同じ証拠（手順、監査、アクション）が関係するすべての規格に有効です。',
        '同一证据（程序、审核、措施）适用于所有相关标准。'),
    'smi.see-matrix': (
        'Voir la matrice des exigences', 'See the requirements matrix', 'Ver la matriz de requisitos',
        'عرض مصفوفة المتطلبات', '要求事項マトリクスを見る', '查看要求矩阵'),
    'smi.matrix.title': (
        'Matrice des risques · {$OPEN} risques ouverts', 'Risk matrix · {$OPEN} open risks',
        'Matriz de riesgos · {$OPEN} riesgos abiertos', 'مصفوفة المخاطر · {$OPEN} مخاطر مفتوحة',
        'リスクマトリクス · 未解決 {$OPEN} 件', '风险矩阵 · {$OPEN} 项未关闭风险'),
    'smi.matrix.view': (
        'Cotation affichée', 'Displayed rating', 'Valoración mostrada',
        'التقييم المعروض', '表示する評価', '显示的评级'),
    'smi.matrix.grid': (
        'Matrice gravité × probabilité', 'Severity × probability matrix', 'Matriz gravedad × probabilidad',
        'مصفوفة الخطورة × الاحتمال', '重大度 × 発生確率マトリクス', '严重度 × 可能性矩阵'),
    'smi.matrix.cell-label': (
        'Gravité {$severity} × probabilité {$probability} : {$count} risque(s), niveau {$level}',
        'Severity {$severity} × probability {$probability}: {$count} risk(s), level {$level}',
        'Gravedad {$severity} × probabilidad {$probability}: {$count} riesgo(s), nivel {$level}',
        'الخطورة {$severity} × الاحتمال {$probability}: {$count} خطر، المستوى {$level}',
        '重大度 {$severity} × 発生確率 {$probability}：リスク {$count} 件、レベル {$level}',
        '严重度 {$severity} × 可能性 {$probability}：{$count} 项风险，等级 {$level}'),
    'smi.matrix.residual-missing': (
        "{$MISSING} risque(s) sans cotation résiduelle visée n'apparaissent pas.",
        '{$MISSING} risk(s) without a target residual rating are not shown.',
        'No aparecen {$MISSING} riesgo(s) sin valoración residual objetivo.',
        'لا يظهر {$MISSING} خطر بدون تقييم متبقٍ مستهدف.',
        '目標残留評価のないリスク {$MISSING} 件は表示されません。',
        '{$MISSING} 项没有目标剩余评级的风险未显示。'),
    'smi.matrix.legend': ('Légende des niveaux', 'Level legend', 'Leyenda de niveles', 'مفتاح المستويات', 'レベルの凡例', '等级图例'),
    'smi.matrix.low': ('Faible 1–4', 'Low 1–4', 'Bajo 1–4', 'منخفض 1–4', '低 1–4', '低 1–4'),
    'smi.matrix.medium': ('Moyen 5–9', 'Medium 5–9', 'Medio 5–9', 'متوسط 5–9', '中 5–9', '中 5–9'),
    'smi.matrix.high': ('Élevé 10–14', 'High 10–14', 'Alto 10–14', 'مرتفع 10–14', '高 10–14', '高 10–14'),
    'smi.matrix.critical': ('Critique 15–25', 'Critical 15–25', 'Crítico 15–25', 'حرج 15–25', '重大 15–25', '严重 15–25'),
    'smi.week.title': (
        'À traiter cette semaine', 'To handle this week', 'Por tratar esta semana',
        'للمعالجة هذا الأسبوع', '今週の対応事項', '本周待处理'),
    'smi.week.empty': (
        "Rien d'échu ni d'attendu d'ici sept jours.", 'Nothing overdue or due within seven days.',
        'Nada vencido ni previsto en los próximos siete días.', 'لا شيء متأخر أو مستحق خلال سبعة أيام.',
        '期限超過も7日以内の期限もありません。', '没有逾期项，也没有七天内到期的事项。'),
    'smi.requirements.title': (
        'Matrice des exigences · une preuve, plusieurs référentiels',
        'Requirements matrix · one piece of evidence, several standards',
        'Matriz de requisitos · una evidencia, varias normas',
        'مصفوفة المتطلبات · دليل واحد لعدة مراجع',
        '要求事項マトリクス · 1つの証拠で複数の規格',
        '要求矩阵 · 一项证据，多项标准'),
    'smi.requirements.teaser': (
        'Chapitres communs 4 à 10, croisés avec chaque norme adoptée : couvert, partiel ou en écart.',
        'Common clauses 4 to 10, crossed with each adopted standard: covered, partial or gap.',
        'Capítulos comunes 4 a 10, cruzados con cada norma adoptada: cubierto, parcial o brecha.',
        'الفصول المشتركة من 4 إلى 10 مقابل كل معيار معتمد: مغطّى أو جزئي أو فجوة.',
        '共通の箇条 4〜10 と採用した各規格の対照：適合、部分的、ギャップ。',
        '通用章节 4 至 10 与每项已采用标准交叉对照：已覆盖、部分或差距。'),

    # ---------- échéances ----------
    'smi.upcoming.capa-action': ('Action CAPA', 'CAPA action', 'Acción CAPA', 'إجراء CAPA', 'CAPAアクション', 'CAPA 措施'),
    'smi.upcoming.calibration': ('Étalonnage', 'Calibration', 'Calibración', 'معايرة', '校正', '校准'),
    'smi.upcoming.change': (
        'Validation de changement', 'Change approval', 'Validación de cambio',
        'اعتماد تغيير', '変更承認', '变更审批'),
    'smi.upcoming.risk-review': ('Revue de risque', 'Risk review', 'Revisión de riesgo', 'مراجعة خطر', 'リスクレビュー', '风险评审'),
    'smi.upcoming.audit': ('Audit', 'Audit', 'Auditoría', 'تدقيق', '監査', '审核'),
    'smi.due.overdue': ('Échu', 'Overdue', 'Vencido', 'متأخر', '期限超過', '已逾期'),
    'smi.due.today': ("Aujourd'hui", 'Today', 'Hoy', 'اليوم', '今日', '今天'),
    'smi.due.in-days': ('J-{$days}', 'D-{$days}', 'D-{$days}', 'ي-{$days}', '残り{$days}日', '剩 {$days} 天'),

    # ---------- chapitres communs, modules, états ----------
    'smi.chapter.4': (
        "Contexte de l'organisme", 'Context of the organization', 'Contexto de la organización',
        'سياق المنظمة', '組織の状況', '组织环境'),
    'smi.chapter.5': ('Leadership et politique', 'Leadership and policy', 'Liderazgo y política', 'القيادة والسياسة', 'リーダーシップと方針', '领导作用与方针'),
    'smi.chapter.6': (
        'Risques, objectifs, exigences légales', 'Risks, objectives, legal requirements',
        'Riesgos, objetivos, requisitos legales', 'المخاطر والأهداف والمتطلبات القانونية',
        'リスク、目標、法的要求事項', '风险、目标、法律要求'),
    'smi.chapter.7': (
        'Ressources, compétences, documents', 'Resources, competence, documents',
        'Recursos, competencias, documentos', 'الموارد والكفاءات والوثائق',
        '資源、力量、文書', '资源、能力、文件'),
    'smi.chapter.8': ('Réalisation opérationnelle', 'Operation', 'Operación', 'التشغيل', '運用', '运行'),
    'smi.chapter.9': (
        'Évaluation des performances', 'Performance evaluation', 'Evaluación del desempeño',
        'تقييم الأداء', 'パフォーマンス評価', '绩效评价'),
    'smi.chapter.10': ('Amélioration', 'Improvement', 'Mejora', 'التحسين', '改善', '改进'),
    'smi.module.process-map': ('Cartographie', 'Process map', 'Mapa de procesos', 'خريطة العمليات', 'プロセスマップ', '过程图'),
    'smi.module.risk-register': ('Risques', 'Risks', 'Riesgos', 'المخاطر', 'リスク', '风险'),
    'smi.module.documents': ('GED', 'Documents', 'Documentos', 'الوثائق', '文書管理', '文件管理'),
    'smi.module.policy': ('Politique', 'Policy', 'Política', 'السياسة', '方針', '方针'),
    'smi.module.objectives': ('Objectifs', 'Objectives', 'Objetivos', 'الأهداف', '目標', '目标'),
    'smi.module.training': ('Compétences', 'Competence', 'Competencias', 'الكفاءات', '力量', '能力'),
    'smi.module.calibration': ('Métrologie', 'Metrology', 'Metrología', 'القياسة', '計測管理', '计量'),
    'smi.module.apqp': ('APQP', 'APQP', 'APQP', 'APQP', 'APQP', 'APQP'),
    'smi.module.spc': ('SPC', 'SPC', 'SPC', 'SPC', 'SPC', 'SPC'),
    'smi.module.changes': ('MOC', 'MOC', 'MOC', 'إدارة التغيير', '変更管理', '变更管理'),
    'smi.module.audits': ('Audits', 'Audits', 'Auditorías', 'التدقيقات', '監査', '审核'),
    'smi.module.kpi': ('Indicateurs', 'KPIs', 'Indicadores', 'المؤشرات', '指標', '指标'),
    'smi.module.management-review': ('Revue', 'Review', 'Revisión', 'المراجعة', 'レビュー', '评审'),
    'smi.module.nc': ('NC', 'NC', 'NC', 'عدم المطابقة', '不適合', '不合格'),
    'smi.module.capa': ('CAPA', 'CAPA', 'CAPA', 'CAPA', 'CAPA', 'CAPA'),
    'smi.coverage.covered': ('Couvert', 'Covered', 'Cubierto', 'مغطّى', '適合', '已覆盖'),
    'smi.coverage.partial': ('Partiel', 'Partial', 'Parcial', 'جزئي', '部分的', '部分'),
    'smi.coverage.gap': ('Écart', 'Gap', 'Brecha', 'فجوة', 'ギャップ', '差距'),
    'smi.coverage.not-applicable': ('Sans objet', 'Not applicable', 'No aplica', 'لا ينطبق', '該当なし', '不适用'),

    # ---------- risques d'une case ----------
    'smi.crumb': ('Tableau de bord SMI', 'IMS dashboard', 'Panel del SGI', 'لوحة نظام الإدارة المتكامل', '統合MS ダッシュボード', '综合管理体系仪表板'),
    'smi.crumb-matrix': ('Matrice des risques', 'Risk matrix', 'Matriz de riesgos', 'مصفوفة المخاطر', 'リスクマトリクス', '风险矩阵'),
    'smi.cell.crumb': (
        'Gravité {$severity} × probabilité {$probability}', 'Severity {$severity} × probability {$probability}',
        'Gravedad {$severity} × probabilidad {$probability}', 'الخطورة {$severity} × الاحتمال {$probability}',
        '重大度 {$severity} × 発生確率 {$probability}', '严重度 {$severity} × 可能性 {$probability}'),
    'smi.cell.title': (
        '{$count} risque(s) · niveau {$level} ({$score})', '{$count} risk(s) · {$level} level ({$score})',
        '{$count} riesgo(s) · nivel {$level} ({$score})', '{$count} خطر · المستوى {$level} ({$score})',
        'リスク {$count} 件 · レベル {$level}（{$score}）', '{$count} 项风险 · 等级 {$level}（{$score}）'),
    'smi.cell.filter-site': ('Filtrer par site', 'Filter by site', 'Filtrar por sitio', 'تصفية حسب الموقع', '拠点で絞り込み', '按场所筛选'),
    'smi.cell.all-sites': ('Tous sites', 'All sites', 'Todos los sitios', 'جميع المواقع', 'すべての拠点', '所有场所'),
    'smi.cell.failed': (
        "Les risques n'ont pas pu être chargés.", 'The risks could not be loaded.',
        'No se pudieron cargar los riesgos.', 'تعذّر تحميل المخاطر.',
        'リスクを読み込めませんでした。', '无法加载风险。'),
    'smi.cell.col-actions': ('Actions', 'Actions', 'Acciones', 'الإجراءات', 'アクション', '措施'),
    'smi.cell.capa-count': ('{$COUNT} en cours', '{$COUNT} in progress', '{$COUNT} en curso', '{$COUNT} قيد التنفيذ', '進行中 {$COUNT}', '{$COUNT} 项进行中'),
    'smi.cell.empty': (
        'Aucun risque ouvert dans cette case.', 'No open risk in this cell.',
        'Ningún riesgo abierto en esta casilla.', 'لا يوجد خطر مفتوح في هذه الخانة.',
        'このセルに未解決のリスクはありません。', '此单元格中没有未关闭的风险。'),
    'smi.cell.detail': ('Détail', 'Detail', 'Detalle', 'التفاصيل', '詳細', '详情'),
    'smi.cell.gross': ('Cotation brute', 'Gross rating', 'Valoración bruta', 'التقييم الإجمالي', '固有評価', '固有评级'),
    'smi.cell.linked-actions': ('Actions liées', 'Linked actions', 'Acciones vinculadas', 'الإجراءات المرتبطة', '関連アクション', '关联措施'),
    'smi.cell.open-risk': ('Ouvrir la fiche risque', 'Open the risk record', 'Abrir la ficha del riesgo', 'فتح سجل الخطر', 'リスク記録を開く', '打开风险记录'),

    # ---------- matrice des exigences ----------
    'smi.req.subtitle': (
        'Les chapitres communs à toutes les normes de système de management, croisés avec chaque norme adoptée.',
        'The clauses shared by all management system standards, crossed with each adopted standard.',
        'Los capítulos comunes a todas las normas de sistemas de gestión, cruzados con cada norma adoptada.',
        'الفصول المشتركة بين جميع معايير أنظمة الإدارة، مقابل كل معيار معتمد.',
        'すべてのマネジメントシステム規格に共通する箇条と、採用した各規格との対照。',
        '所有管理体系标准共有的章节，与每项已采用标准交叉对照。'),
    'smi.req.crumb': ('Matrice des exigences', 'Requirements matrix', 'Matriz de requisitos', 'مصفوفة المتطلبات', '要求事項マトリクス', '要求矩阵'),
    'smi.req.failed': (
        "La matrice des exigences n'a pas pu être chargée.", 'The requirements matrix could not be loaded.',
        'No se pudo cargar la matriz de requisitos.', 'تعذّر تحميل مصفوفة المتطلبات.',
        '要求事項マトリクスを読み込めませんでした。', '无法加载要求矩阵。'),
    'smi.req.col-chapter': ('Chapitre commun (HLS)', 'Common clause (HLS)', 'Capítulo común (HLS)', 'الفصل المشترك (HLS)', '共通箇条（HLS）', '通用章节（HLS）'),
    'smi.req.col-module': ('Module QualitOS', 'QualitOS module', 'Módulo QualitOS', 'وحدة QualitOS', 'QualitOS モジュール', 'QualitOS 模块'),
    'smi.req.cell-label': (
        '{$standard}, chapitre {$chapter} : {$status}, {$covered} sur {$total}',
        '{$standard}, clause {$chapter}: {$status}, {$covered} of {$total}',
        '{$standard}, capítulo {$chapter}: {$status}, {$covered} de {$total}',
        '{$standard}، الفصل {$chapter}: {$status}، {$covered} من {$total}',
        '{$standard}、箇条 {$chapter}：{$status}、{$total} 中 {$covered}',
        '{$standard}，第 {$chapter} 章：{$status}，{$total} 项中 {$covered} 项'),
    'smi.req.legend': ('Légende :', 'Legend:', 'Leyenda:', 'المفتاح:', '凡例：', '图例：'),
    'smi.req.legend-covered': (
        'couvert : toutes les exigences du chapitre sont prouvées',
        'covered: every requirement of the clause is evidenced',
        'cubierto: todos los requisitos del capítulo están evidenciados',
        'مغطّى: كل متطلبات الفصل مدعومة بأدلة',
        '適合：箇条のすべての要求事項に証拠がある',
        '已覆盖：该章节的所有要求均有证据'),
    'smi.req.legend-partial': (
        'partiel : une partie seulement', 'partial: only some of them', 'parcial: solo una parte',
        'جزئي: جزء منها فقط', '部分的：一部のみ', '部分：仅部分要求'),
    'smi.req.legend-gap': (
        'écart : aucune preuve', 'gap: no evidence', 'brecha: ninguna evidencia',
        'فجوة: لا يوجد دليل', 'ギャップ：証拠なし', '差距：无任何证据'),
    'smi.req.detail-chapter': (
        'Détail · chapitre {$CHAPTER}', 'Detail · clause {$CHAPTER}', 'Detalle · capítulo {$CHAPTER}',
        'التفاصيل · الفصل {$CHAPTER}', '詳細 · 箇条 {$CHAPTER}', '详情 · 第 {$CHAPTER} 章'),
    'smi.req.detail-count': (
        '{$COVERED} exigence(s) prouvée(s) sur {$TOTAL}.', '{$COVERED} requirement(s) evidenced out of {$TOTAL}.',
        '{$COVERED} requisito(s) evidenciado(s) de {$TOTAL}.', '{$COVERED} متطلب مدعوم بدليل من أصل {$TOTAL}.',
        '{$TOTAL} 件中 {$COVERED} 件の要求事項に証拠あり。', '{$TOTAL} 项要求中有 {$COVERED} 项有证据。'),
    'smi.req.detail-missing': ('Clauses à prouver', 'Clauses to evidence', 'Cláusulas por evidenciar', 'البنود التي تحتاج أدلة', '証拠が必要な細分箇条', '待提供证据的条款'),
    'smi.req.detail-failed': (
        "Le détail des clauses n'a pas pu être chargé.", 'The clause details could not be loaded.',
        'No se pudo cargar el detalle de las cláusulas.', 'تعذّر تحميل تفاصيل البنود.',
        '細分箇条の詳細を読み込めませんでした。', '无法加载条款详情。'),
    'smi.req.complete': ('Compléter les preuves', 'Add evidence', 'Completar las evidencias', 'استكمال الأدلة', '証拠を追加', '补充证据'),
    'smi.req.reuse': (
        'Une preuve rattachée à une exigence commune compte pour toutes les normes qui partagent ce chapitre.',
        'Evidence linked to a common requirement counts for every standard sharing this clause.',
        'Una evidencia vinculada a un requisito común cuenta para todas las normas que comparten este capítulo.',
        'الدليل المرتبط بمتطلب مشترك يُحتسب لكل المعايير التي تشترك في هذا الفصل.',
        '共通の要求事項に紐づく証拠は、この箇条を共有するすべての規格に有効です。',
        '关联到通用要求的证据，适用于共享该章节的所有标准。'),
    'smi.req.all-covered': (
        'Tout est couvert : choisissez une case pour en voir le détail.',
        'Everything is covered: choose a cell to see its detail.',
        'Todo está cubierto: elija una casilla para ver su detalle.',
        'كل شيء مغطّى: اختر خانة لعرض تفاصيلها.',
        'すべて適合しています。セルを選んで詳細を表示してください。',
        '全部已覆盖：选择一个单元格查看详情。'),
    'smi.req.capa-title': (
        '{$status} {$standard} · {$chapter} {$title}', '{$status} {$standard} · {$chapter} {$title}',
        '{$status} {$standard} · {$chapter} {$title}', '{$status} {$standard} · {$chapter} {$title}',
        '{$status} {$standard} · {$chapter} {$title}', '{$status} {$standard} · {$chapter} {$title}'),
    'smi.req.capa-description': (
        '{$covered} exigence(s) prouvée(s) sur {$total} au chapitre {$chapter} de {$standard}.',
        '{$covered} requirement(s) evidenced out of {$total} in clause {$chapter} of {$standard}.',
        '{$covered} requisito(s) evidenciado(s) de {$total} en el capítulo {$chapter} de {$standard}.',
        '{$covered} متطلب مدعوم بدليل من أصل {$total} في الفصل {$chapter} من {$standard}.',
        '{$standard} の箇条 {$chapter}：{$total} 件中 {$covered} 件の要求事項に証拠あり。',
        '{$standard} 第 {$chapter} 章：{$total} 项要求中有 {$covered} 项有证据。'),
}
