# -*- coding: utf-8 -*-
"""Libellés du registre des risques et opportunités (ISO 9001 §6.1, ADR 0076).

Registre à deux onglets (risques, opportunités), formulaire, fiche avec
traitement et historique, et les quatre portes d'entrée : AMDEC, NC, audit,
changement.
"""

TRANSLATIONS = {
    'common.retry': ('Réessayer', 'Retry', 'Reintentar', 'إعادة المحاولة', '再試行', '重试'),

    # ---------- nav ----------
    'nav.risques-opportunites': (
        'Risques & opportunités', 'Risks & opportunities', 'Riesgos y oportunidades',
        'المخاطر والفرص', 'リスクと機会', '风险与机遇'),
    'nav.registre-risques': ('Registre', 'Register', 'Registro', 'السجل', '登録簿', '登记册'),

    # ---------- page du registre ----------
    'rr.eyebrow': (
        'Risques & conformité', 'Risk & compliance', 'Riesgos y conformidad',
        'المخاطر والامتثال', 'リスクとコンプライアンス', '风险与合规'),
    'rr.title': (
        'Risques et opportunités', 'Risks and opportunities', 'Riesgos y oportunidades',
        'المخاطر والفرص', 'リスクと機会', '风险与机遇'),
    'rr.subtitle': (
        "Registre unique du système de management : chaque ligne est cotée, traitée et rattachée aux exigences qu'elle couvre.",
        'Single register for the management system: each line is rated, treated and linked to the requirements it covers.',
        'Registro único del sistema de gestión: cada línea se valora, se trata y se vincula a los requisitos que cubre.',
        'سجل موحّد لنظام الإدارة: كل سطر مُقيَّم ومُعالَج ومرتبط بالمتطلبات التي يغطيها.',
        'マネジメントシステムの統合登録簿：各行は評価・対応され、対象となる要求事項に紐づけられます。',
        '管理体系的统一登记册：每一行都经过评级、处理，并关联到其覆盖的要求。'),
    'rr.crumb.register': ('Registre', 'Register', 'Registro', 'السجل', '登録簿', '登记册'),
    'rr.tabs': ('Registres', 'Registers', 'Registros', 'السجلات', '登録簿', '登记册'),
    'rr.tab-risks': ('Risques', 'Risks', 'Riesgos', 'المخاطر', 'リスク', '风险'),
    'rr.tab-opportunities': ('Opportunités', 'Opportunities', 'Oportunidades', 'الفرص', '機会', '机遇'),
    'rr.new-risk': ('Nouveau risque', 'New risk', 'Nuevo riesgo', 'خطر جديد', '新規リスク', '新建风险'),
    'rr.new-opportunity': (
        'Nouvelle opportunité', 'New opportunity', 'Nueva oportunidad',
        'فرصة جديدة', '新規機会', '新建机遇'),
    'rr.create-risk': ('Créer un risque', 'Create a risk', 'Crear un riesgo', 'إنشاء خطر', 'リスクを作成', '创建风险'),
    'rr.export': ('Exporter', 'Export', 'Exportar', 'تصدير', 'エクスポート', '导出'),
    'rr.load-failed': (
        "Le registre n'a pas pu être chargé.", 'The register could not be loaded.',
        'No se pudo cargar el registro.', 'تعذّر تحميل السجل.',
        '登録簿を読み込めませんでした。', '无法加载登记册。'),
    'rr.search-risk': (
        'Rechercher un risque', 'Search a risk', 'Buscar un riesgo',
        'البحث عن خطر', 'リスクを検索', '搜索风险'),
    'rr.search-opportunity': (
        'Rechercher une opportunité', 'Search an opportunity', 'Buscar una oportunidad',
        'البحث عن فرصة', '機会を検索', '搜索机遇'),
    'rr.filter-type': ('Filtrer par type', 'Filter by type', 'Filtrar por tipo', 'تصفية حسب النوع', '種類で絞り込み', '按类型筛选'),
    'rr.filter-process': (
        'Filtrer par processus', 'Filter by process', 'Filtrar por proceso',
        'تصفية حسب العملية', 'プロセスで絞り込み', '按过程筛选'),
    'rr.filter-status': (
        'Filtrer par statut', 'Filter by status', 'Filtrar por estado',
        'تصفية حسب الحالة', 'ステータスで絞り込み', '按状态筛选'),
    'rr.all-types': ('Tous types', 'All types', 'Todos los tipos', 'جميع الأنواع', 'すべての種類', '所有类型'),
    'rr.all-processes': (
        'Tous processus', 'All processes', 'Todos los procesos',
        'جميع العمليات', 'すべてのプロセス', '所有过程'),
    'rr.all-statuses': ('Tous statuts', 'All statuses', 'Todos los estados', 'جميع الحالات', 'すべてのステータス', '所有状态'),
    'rr.all-opportunities': ('Toutes', 'All', 'Todas', 'الكل', 'すべて', '全部'),
    'rr.priority-filter': (
        'Filtre de priorité', 'Priority filter', 'Filtro de prioridad',
        'مرشّح الأولوية', '優先度フィルター', '优先级筛选'),
    'rr.priority-only': ('Prioritaires', 'Priority', 'Prioritarias', 'ذات الأولوية', '優先', '优先'),
    'rr.rating-view': (
        'Cotation affichée', 'Displayed rating', 'Valoración mostrada',
        'التقييم المعروض', '表示する評価', '显示的评级'),
    'rr.gross': ('Brut', 'Gross', 'Bruto', 'إجمالي', '固有', '固有'),
    'rr.residual': ('Résiduel', 'Residual', 'Residual', 'متبقٍ', '残留', '剩余'),
    'rr.reset-filters': (
        'Effacer les filtres', 'Clear filters', 'Borrar filtros',
        'مسح عوامل التصفية', 'フィルターをクリア', '清除筛选'),
    'rr.no-risk': (
        'Aucun risque dans le registre.', 'No risks in the register.',
        'No hay riesgos en el registro.', 'لا توجد مخاطر في السجل.',
        '登録簿にリスクはありません。', '登记册中没有风险。'),
    'rr.no-opportunity': (
        'Aucune opportunité dans le registre.', 'No opportunities in the register.',
        'No hay oportunidades en el registro.', 'لا توجد فرص في السجل.',
        '登録簿に機会はありません。', '登记册中没有机遇。'),
    'rr.no-match': (
        'Aucune ligne ne correspond aux filtres.', 'No lines match the filters.',
        'Ninguna línea coincide con los filtros.', 'لا توجد أسطر مطابقة لعوامل التصفية.',
        'フィルターに一致する行はありません。', '没有符合筛选条件的行。'),
    'rr.shown-risks': (
        '{$SHOWN} risques affichés sur {$TOTAL}', '{$SHOWN} of {$TOTAL} risks shown',
        '{$SHOWN} de {$TOTAL} riesgos mostrados', 'عرض {$SHOWN} من أصل {$TOTAL} خطر',
        '{$TOTAL} 件中 {$SHOWN} 件のリスクを表示', '显示 {$SHOWN} / {$TOTAL} 项风险'),
    'rr.shown-opportunities': (
        '{$SHOWN} opportunités affichées sur {$TOTAL}', '{$SHOWN} of {$TOTAL} opportunities shown',
        '{$SHOWN} de {$TOTAL} oportunidades mostradas', 'عرض {$SHOWN} من أصل {$TOTAL} فرصة',
        '{$TOTAL} 件中 {$SHOWN} 件の機会を表示', '显示 {$SHOWN} / {$TOTAL} 项机遇'),
    'rr.legend-risk': (
        'Niveau = gravité × probabilité · Faible 1–4 · Moyen 5–9 · Élevé 10–14 · Critique 15–25',
        'Level = severity × likelihood · Low 1–4 · Medium 5–9 · High 10–14 · Critical 15–25',
        'Nivel = gravedad × probabilidad · Bajo 1–4 · Medio 5–9 · Alto 10–14 · Crítico 15–25',
        'المستوى = الشدة × الاحتمالية · منخفض 1–4 · متوسط 5–9 · مرتفع 10–14 · حرج 15–25',
        'レベル = 重大度 × 発生可能性 · 低 1–4 · 中 5–9 · 高 10–14 · 重大 15–25',
        '等级 = 严重度 × 可能性 · 低 1–4 · 中 5–9 · 高 10–14 · 严重 15–25'),
    'rr.legend-opportunity': (
        'Score = gain attendu × faisabilité (1 à 5 chacun) · Moyen 5–9 · Élevé 10–14 · Prioritaire 15–25',
        'Score = expected gain × feasibility (1 to 5 each) · Medium 5–9 · High 10–14 · Priority 15–25',
        'Puntuación = beneficio esperado × viabilidad (1 a 5 cada uno) · Medio 5–9 · Alto 10–14 · Prioritario 15–25',
        'الدرجة = المكسب المتوقع × قابلية التنفيذ (من 1 إلى 5 لكل منهما) · متوسط 5–9 · مرتفع 10–14 · ذو أولوية 15–25',
        'スコア = 期待効果 × 実現可能性（各1〜5） · 中 5–9 · 高 10–14 · 優先 15–25',
        '得分 = 预期收益 × 可行性（各 1 至 5） · 中 5–9 · 高 10–14 · 优先 15–25'),

    # ---------- colonnes ----------
    'rr.col.ref': ('Réf.', 'Ref.', 'Ref.', 'المرجع', '参照', '编号'),
    'rr.col.risk': ('Risque', 'Risk', 'Riesgo', 'الخطر', 'リスク', '风险'),
    'rr.col.opportunity': ('Opportunité', 'Opportunity', 'Oportunidad', 'الفرصة', '機会', '机遇'),
    'rr.col.type': ('Type', 'Type', 'Tipo', 'النوع', '種類', '类型'),
    'rr.col.process': ('Processus', 'Process', 'Proceso', 'العملية', 'プロセス', '过程'),
    'rr.col.owner': ('Propriétaire', 'Owner', 'Responsable', 'المالك', 'オーナー', '负责人'),
    'rr.col.gross': ('Cotation brute', 'Gross rating', 'Valoración bruta', 'التقييم الإجمالي', '固有評価', '固有评级'),
    'rr.col.residual': ('Résiduel', 'Residual', 'Residual', 'متبقٍ', '残留', '剩余'),
    'rr.col.score': (
        'Gain × faisabilité', 'Gain × feasibility', 'Beneficio × viabilidad',
        'المكسب × قابلية التنفيذ', '効果 × 実現可能性', '收益 × 可行性'),
    'rr.col.status': ('Statut', 'Status', 'Estado', 'الحالة', 'ステータス', '状态'),
    'rr.col.due': ('Échéance', 'Due date', 'Vencimiento', 'تاريخ الاستحقاق', '期限', '截止日期'),

    # ---------- portes d'entrée ----------
    'rr.create-from-fmea-tooltip': (
        'Transformer cette ligne en risque du registre', 'Turn this line into a register risk',
        'Convertir esta línea en un riesgo del registro', 'تحويل هذا السطر إلى خطر في السجل',
        'この行を登録簿のリスクに変換', '将此行转为登记册中的风险'),
    'rr.create-from-nc-tooltip': (
        'Inscrire au registre le risque que révèle cet écart',
        'Add the risk revealed by this nonconformity to the register',
        'Registrar el riesgo que revela esta desviación',
        'تسجيل الخطر الذي تكشفه هذه الفجوة في السجل',
        'この不適合が示すリスクを登録簿に登録', '将此偏差揭示的风险登记入册'),
    'rr.create-from-change-tooltip': (
        "Inscrire au registre un risque issu de l'analyse d'impact",
        'Add a risk from the impact analysis to the register',
        'Registrar un riesgo derivado del análisis de impacto',
        'تسجيل خطر ناتج عن تحليل الأثر في السجل',
        '影響分析から特定されたリスクを登録簿に登録', '将影响分析得出的风险登记入册'),

    # ---------- formulaire ----------
    'rr.form.new-risk': ('Nouveau risque', 'New risk', 'Nuevo riesgo', 'خطر جديد', '新規リスク', '新建风险'),
    'rr.form.new-opportunity': (
        'Nouvelle opportunité', 'New opportunity', 'Nueva oportunidad',
        'فرصة جديدة', '新規機会', '新建机遇'),
    'rr.form.edit-risk': ('Modifier le risque', 'Edit risk', 'Editar el riesgo', 'تعديل الخطر', 'リスクを編集', '编辑风险'),
    'rr.form.edit-opportunity': (
        "Modifier l'opportunité", 'Edit opportunity', 'Editar la oportunidad',
        'تعديل الفرصة', '機会を編集', '编辑机遇'),
    'rr.form.read-only': (
        "Votre profil permet de consulter le registre, pas de l'alimenter. Un responsable qualité peut créer cette fiche.",
        'Your profile lets you view the register, not add to it. A quality manager can create this record.',
        'Su perfil permite consultar el registro, no alimentarlo. Un responsable de calidad puede crear este registro.',
        'يتيح لك ملفك الشخصي الاطلاع على السجل دون الإضافة إليه. يمكن لمسؤول الجودة إنشاء هذا السجل.',
        'お使いのプロファイルでは登録簿の閲覧のみ可能で、登録はできません。品質管理者がこの記録を作成できます。',
        '您的角色只能查看登记册，不能录入。质量负责人可创建此记录。'),
    'rr.form.identification': ('Identification', 'Identification', 'Identificación', 'التحديد', '特定', '识别'),
    'rr.form.risk-title': ('Intitulé du risque', 'Risk title', 'Título del riesgo', 'عنوان الخطر', 'リスクの名称', '风险名称'),
    'rr.form.risk-title-placeholder': (
        'Décrire le risque en une phrase', 'Describe the risk in one sentence',
        'Describa el riesgo en una frase', 'صِف الخطر في جملة واحدة',
        'リスクを一文で記述', '用一句话描述风险'),
    'rr.form.opportunity-title': (
        "Intitulé de l'opportunité", 'Opportunity title', 'Título de la oportunidad',
        'عنوان الفرصة', '機会の名称', '机遇名称'),
    'rr.form.opportunity-title-placeholder': (
        "Décrire l'opportunité en une phrase", 'Describe the opportunity in one sentence',
        'Describa la oportunidad en una frase', 'صِف الفرصة في جملة واحدة',
        '機会を一文で記述', '用一句话描述机遇'),
    'rr.form.description': ('Description', 'Description', 'Descripción', 'الوصف', '説明', '描述'),
    'rr.form.cause': ('Cause', 'Cause', 'Causa', 'السبب', '原因', '原因'),
    'rr.form.cause-placeholder': (
        "Qu'est-ce qui peut provoquer ce risque ?", 'What could cause this risk?',
        '¿Qué puede provocar este riesgo?', 'ما الذي قد يتسبب في هذا الخطر؟',
        'このリスクを引き起こし得るものは？', '什么可能引发此风险？'),
    'rr.form.effect': ('Effet', 'Effect', 'Efecto', 'الأثر', '影響', '后果'),
    'rr.form.effect-placeholder': (
        "Quelles conséquences s'il se produit ?", 'What consequences if it occurs?',
        '¿Qué consecuencias si se produce?', 'ما العواقب إذا حدث؟',
        '発生した場合の結果は？', '一旦发生会有什么后果？'),
    'rr.form.context': ('Contexte et levier', 'Context and lever', 'Contexto y palanca', 'السياق والرافعة', '背景と推進要因', '背景与杠杆'),
    'rr.form.context-placeholder': (
        "Qu'est-ce qui rend cette opportunité possible ?", 'What makes this opportunity possible?',
        '¿Qué hace posible esta oportunidad?', 'ما الذي يجعل هذه الفرصة ممكنة؟',
        'この機会を可能にしているものは？', '是什么使这一机遇成为可能？'),
    'rr.form.benefit': ('Bénéfice attendu', 'Expected benefit', 'Beneficio esperado', 'الفائدة المتوقعة', '期待される効果', '预期效益'),
    'rr.form.benefit-placeholder': (
        "Quel gain en attendez-vous ?", 'What gain do you expect?',
        '¿Qué beneficio espera?', 'ما المكسب الذي تتوقعه؟',
        'どのような効果を期待しますか？', '您期望获得什么收益？'),
    'rr.form.site': ('Site', 'Site', 'Sede', 'الموقع', 'サイト', '场所'),
    'rr.form.origin': ('Origine', 'Origin', 'Origen', 'المصدر', '発生源', '来源'),
    'rr.form.origin-ref': (
        'Référence liée (facultatif)', 'Linked reference (optional)', 'Referencia vinculada (opcional)',
        'المرجع المرتبط (اختياري)', '関連参照（任意）', '关联编号（可选）'),
    'rr.form.origin-locked': (
        "Fixée par l'objet dont le risque est issu.", 'Set by the item the risk comes from.',
        'Fijado por el objeto del que procede el riesgo.', 'محدد بواسطة العنصر الذي نشأ عنه الخطر.',
        'リスクの発生元の項目により固定されています。', '由产生该风险的对象确定。'),
    'rr.form.from-source': ('Risque issu de', 'Risk from', 'Riesgo derivado de', 'خطر ناشئ عن', 'リスクの発生元', '风险来源'),
    'rr.form.source': ('Source', 'Source', 'Fuente', 'المصدر', 'ソース', '来源'),
    'rr.form.already-linked': (
        'Déjà au registre depuis cet objet :', 'Already in the register from this item:',
        'Ya en el registro desde este objeto:', 'موجود بالفعل في السجل من هذا العنصر:',
        'この項目から既に登録済み：', '已从此对象登记入册：'),
    'rr.form.not-eligible': (
        'Cet objet ne justifie pas un risque.', 'This item does not warrant a risk.',
        'Este objeto no justifica un riesgo.', 'هذا العنصر لا يستدعي تسجيل خطر.',
        'この項目はリスク登録に該当しません。', '此对象不构成风险。'),
    'rr.form.not-a-gap': (
        "Ce constat n'est pas un écart : il ne justifie pas un risque.",
        'This finding is not a nonconformity: it does not warrant a risk.',
        'Este hallazgo no es una desviación: no justifica un riesgo.',
        'هذه الملاحظة ليست فجوة: فهي لا تستدعي تسجيل خطر.',
        'この所見は不適合ではないため、リスク登録に該当しません。',
        '此发现不是偏差：不构成风险。'),
    'rr.form.below-threshold': (
        "Cette ligne d'AMDEC est sous le seuil de criticité du projet : elle ne justifie pas un risque.",
        'This FMEA line is below the project criticality threshold: it does not warrant a risk.',
        'Esta línea de AMFE está por debajo del umbral de criticidad del proyecto: no justifica un riesgo.',
        'سطر FMEA هذا دون عتبة الحرجية للمشروع: فهو لا يستدعي تسجيل خطر.',
        'このFMEA行はプロジェクトの重要度しきい値未満のため、リスク登録に該当しません。',
        '此 FMEA 行低于项目的关键度阈值：不构成风险。'),
    'rr.form.requirements': (
        'Exigences concernées', 'Requirements concerned', 'Requisitos afectados',
        'المتطلبات المعنية', '対象となる要求事項', '涉及的要求'),
    'rr.form.evaluation': ('Évaluation', 'Evaluation', 'Evaluación', 'التقييم', '評価', '评估'),
    'rr.form.gross-rating': ('Cotation brute', 'Gross rating', 'Valoración bruta', 'التقييم الإجمالي', '固有評価', '固有评级'),
    'rr.form.residual-rating': (
        'Cotation résiduelle visée', 'Target residual rating', 'Valoración residual objetivo',
        'التقييم المتبقي المستهدف', '目標残留評価', '目标剩余评级'),
    'rr.form.residual-too-high': (
        'La cotation résiduelle visée ne peut pas dépasser la cotation brute.',
        'The target residual rating cannot exceed the gross rating.',
        'La valoración residual objetivo no puede superar la valoración bruta.',
        'لا يمكن أن يتجاوز التقييم المتبقي المستهدف التقييم الإجمالي.',
        '目標残留評価は固有評価を超えることはできません。',
        '目标剩余评级不能高于固有评级。'),
    'rr.form.level': ('Niveau', 'Level', 'Nivel', 'المستوى', 'レベル', '等级'),
    'rr.form.score': ('Score', 'Score', 'Puntuación', 'الدرجة', 'スコア', '得分'),
    'rr.form.risk-scale': (
        'Faible 1–4 · Moyen 5–9 · Élevé 10–14 · Critique 15–25',
        'Low 1–4 · Medium 5–9 · High 10–14 · Critical 15–25',
        'Bajo 1–4 · Medio 5–9 · Alto 10–14 · Crítico 15–25',
        'منخفض 1–4 · متوسط 5–9 · مرتفع 10–14 · حرج 15–25',
        '低 1–4 · 中 5–9 · 高 10–14 · 重大 15–25',
        '低 1–4 · 中 5–9 · 高 10–14 · 严重 15–25'),
    'rr.form.opportunity-scale': (
        'Faible 1–4 · Moyen 5–9 · Élevé 10–14 · Prioritaire 15–25',
        'Low 1–4 · Medium 5–9 · High 10–14 · Priority 15–25',
        'Bajo 1–4 · Medio 5–9 · Alto 10–14 · Prioritario 15–25',
        'منخفض 1–4 · متوسط 5–9 · مرتفع 10–14 · ذو أولوية 15–25',
        '低 1–4 · 中 5–9 · 高 10–14 · 優先 15–25',
        '低 1–4 · 中 5–9 · 高 10–14 · 优先 15–25'),
    'rr.form.treatment': ('Traitement', 'Treatment', 'Tratamiento', 'المعالجة', '対応', '处理'),
    'rr.form.implementation': ('Mise en œuvre', 'Implementation', 'Implementación', 'التنفيذ', '実施', '实施'),
    'rr.form.decision': ('Décision', 'Decision', 'Decisión', 'القرار', '決定', '决定'),
    'rr.form.follow-up': ('Suivi', 'Follow-up', 'Seguimiento', 'المتابعة', 'フォローアップ', '跟踪'),
    'rr.form.target-date': ('Échéance visée', 'Target date', 'Fecha objetivo', 'التاريخ المستهدف', '目標期日', '目标日期'),
    'rr.form.next-review': ('Prochaine revue', 'Next review', 'Próxima revisión', 'المراجعة القادمة', '次回レビュー', '下次评审'),
    'rr.form.effectiveness': (
        "Vérification d'efficacité (critère)", 'Effectiveness check (criterion)',
        'Verificación de eficacia (criterio)', 'التحقق من الفعالية (المعيار)',
        '有効性確認（基準）', '有效性验证（准则）'),
    'rr.form.benefit-check': (
        'Vérification du bénéfice (critère)', 'Benefit check (criterion)',
        'Verificación del beneficio (criterio)', 'التحقق من الفائدة (المعيار)',
        '効果確認（基準）', '效益验证（准则）'),
    'rr.form.required': ('Champ obligatoire.', 'Required field.', 'Campo obligatorio.', 'حقل إلزامي.', '必須項目です。', '必填字段。'),
    'rr.form.save-risk': ('Enregistrer le risque', 'Save risk', 'Guardar el riesgo', 'حفظ الخطر', 'リスクを保存', '保存风险'),
    'rr.form.save-opportunity': (
        "Enregistrer l'opportunité", 'Save opportunity', 'Guardar la oportunidad',
        'حفظ الفرصة', '機会を保存', '保存机遇'),
    'rr.form.risk-saved': ('Risque enregistré.', 'Risk saved.', 'Riesgo guardado.', 'تم حفظ الخطر.', 'リスクを保存しました。', '风险已保存。'),
    'rr.form.opportunity-saved': (
        'Opportunité enregistrée.', 'Opportunity saved.', 'Oportunidad guardada.',
        'تم حفظ الفرصة.', '機会を保存しました。', '机遇已保存。'),
    'rr.form.save-failed': (
        "L'enregistrement a échoué.", 'Saving failed.', 'No se pudo guardar.',
        'فشل الحفظ.', '保存に失敗しました。', '保存失败。'),

    # ---------- fiche ----------
    'rr.detail.back': ('Retour au registre', 'Back to register', 'Volver al registro', 'العودة إلى السجل', '登録簿に戻る', '返回登记册'),
    'rr.detail.failed': (
        'La fiche est indisponible.', 'The record is unavailable.', 'El registro no está disponible.',
        'السجل غير متاح.', '記録を利用できません。', '记录不可用。'),
    'rr.detail.rating': ('Cotation', 'Rating', 'Valoración', 'التقييم', '評価', '评级'),
    'rr.detail.gross': ('Brute', 'Gross', 'Bruta', 'إجمالي', '固有', '固有'),
    'rr.detail.residual-target': (
        'Résiduelle visée', 'Target residual', 'Residual objetivo',
        'المتبقي المستهدف', '目標残留', '目标剩余'),
    'rr.detail.not-set': ('Non fixée', 'Not set', 'No definida', 'غير محدد', '未設定', '未设定'),
    'rr.detail.effectiveness': (
        "Vérification d'efficacité", 'Effectiveness check', 'Verificación de eficacia',
        'التحقق من الفعالية', '有効性確認', '有效性验证'),
    'rr.detail.benefit-check': (
        'Vérification du bénéfice', 'Benefit check', 'Verificación del beneficio',
        'التحقق من الفائدة', '効果確認', '效益验证'),
    'rr.detail.requirements': (
        'Exigences couvertes', 'Requirements covered', 'Requisitos cubiertos',
        'المتطلبات المغطاة', '対象の要求事項', '覆盖的要求'),
    'rr.detail.requirements-note': (
        'Cette fiche compte comme preuve pour chacune de ces exigences dans la matrice du SMI.',
        'This record counts as evidence for each of these requirements in the IMS matrix.',
        'Este registro cuenta como evidencia para cada uno de estos requisitos en la matriz del SIG.',
        'يُعَدّ هذا السجل دليلاً لكل من هذه المتطلبات في مصفوفة نظام الإدارة المتكامل.',
        'この記録は、統合マネジメントシステム（IMS）マトリクスにおいて、これらの各要求事項の証拠として扱われます。',
        '此记录在一体化管理体系（IMS）矩阵中作为上述每项要求的证据。'),
    'rr.detail.action': ('Action', 'Action', 'Acción', 'الإجراء', 'アクション', '措施'),
    'rr.detail.row-actions': ('Actions', 'Actions', 'Acciones', 'الإجراءات', 'アクション', '操作'),
    'rr.detail.no-action': (
        "Aucune action pour l'instant.", 'No actions yet.', 'Aún no hay acciones.',
        'لا توجد إجراءات حتى الآن.', 'アクションはまだありません。', '暂无措施。'),
    'rr.detail.no-capa': (
        "Aucune action CAPA liée pour l'instant.", 'No linked CAPA actions yet.',
        'Aún no hay acciones CAPA vinculadas.', 'لا توجد إجراءات CAPA مرتبطة حتى الآن.',
        '関連するCAPAアクションはまだありません。', '暂无关联的 CAPA 措施。'),
    'rr.detail.capa-opened': (
        'Dossier CAPA ouvert.', 'CAPA case opened.', 'Expediente CAPA abierto.',
        'تم فتح ملف CAPA.', 'CAPAケースを開始しました。', 'CAPA 案例已开启。'),
    'rr.detail.delete-action-title': (
        'Supprimer ACT-{$number} ?', 'Delete ACT-{$number}?', '¿Eliminar ACT-{$number}?',
        'حذف ACT-{$number}؟', 'ACT-{$number} を削除しますか？', '删除 ACT-{$number}？'),
    'rr.detail.delete-action-message': (
        "L'action quitte la fiche. La suppression est tracée dans le journal d'audit.",
        'The action is removed from the record. The deletion is logged in the audit trail.',
        'La acción se retira del registro. La eliminación queda registrada en el registro de auditoría.',
        'يُزال الإجراء من السجل. ويُوثَّق الحذف في سجل التدقيق.',
        'アクションは記録から削除されます。削除は監査証跡に記録されます。',
        '该措施将从记录中移除。删除操作会记入审计日志。'),

    # ---------- actions ----------
    'rr.action.new-title': ('Créer une action', 'Create an action', 'Crear una acción', 'إنشاء إجراء', 'アクションを作成', '创建措施'),
    'rr.action.title': ('Intitulé', 'Title', 'Título', 'العنوان', '名称', '名称'),
    'rr.action.description': (
        'Description (facultatif)', 'Description (optional)', 'Descripción (opcional)',
        'الوصف (اختياري)', '説明（任意）', '描述（可选）'),
    'rr.action.capa-title': (
        'Créer une action CAPA', 'Create a CAPA action', 'Crear una acción CAPA',
        'إنشاء إجراء CAPA', 'CAPAアクションを作成', '创建 CAPA 措施'),
    'rr.action.capa-help': (
        'Un dossier CAPA préventif est ouvert dans le module CAPA, à votre nom, avec la criticité du niveau brut du risque. Il apparaîtra dans le tableau « Traitement » de la fiche.',
        'A preventive CAPA case is opened in the CAPA module, in your name, with the criticality of the gross risk level. It will appear in the “Treatment” table of the record.',
        'Se abre un expediente CAPA preventivo en el módulo CAPA, a su nombre, con la criticidad del nivel bruto del riesgo. Aparecerá en la tabla «Tratamiento» del registro.',
        'يُفتح ملف CAPA وقائي في وحدة CAPA باسمك، بدرجة حرجية مساوية للمستوى الإجمالي للخطر. وسيظهر في جدول «المعالجة» في السجل.',
        'CAPAモジュールで、あなたの名前で予防的CAPAケースが開始され、リスクの固有レベルに応じた重要度が設定されます。記録の「対応」表に表示されます。',
        '将在 CAPA 模块中以您的名义开启一个预防性 CAPA 案例，其关键度取风险的固有等级。它将显示在记录的“处理”表中。'),
    'rr.action.open-capa': (
        'Ouvrir le dossier CAPA', 'Open the CAPA case', 'Abrir el expediente CAPA',
        'فتح ملف CAPA', 'CAPAケースを開く', '打开 CAPA 案例'),
    'rr.action-status.to-start': ('À démarrer', 'To start', 'Por iniciar', 'للبدء', '未着手', '待开始'),
    'rr.action-status.in-progress': ('En cours', 'In progress', 'En curso', 'قيد التنفيذ', '進行中', '进行中'),
    'rr.action-status.done': ('Terminée', 'Done', 'Terminada', 'منتهٍ', '完了', '已完成'),
    'rr.action-status.cancelled': ('Annulée', 'Cancelled', 'Cancelada', 'ملغى', '取消', '已取消'),
    'rr.capa-status.open': ('À démarrer', 'To start', 'Por iniciar', 'للبدء', '未着手', '待开始'),
    'rr.capa-status.in-progress': ('En cours', 'In progress', 'En curso', 'قيد التنفيذ', '進行中', '进行中'),
    'rr.capa-status.resolved': ('Résolue', 'Resolved', 'Resuelta', 'تم الحل', '解決済み', '已解决'),
    'rr.capa-status.closed': ('Clôturée', 'Closed', 'Cerrada', 'مغلق', '完了', '已关闭'),
    'rr.capa-status.rejected': ('Rejetée', 'Rejected', 'Rechazada', 'مرفوض', '却下', '已驳回'),

    # ---------- échelles ----------
    'rr.severity': ('Gravité', 'Severity', 'Gravedad', 'الشدة', '重大度', '严重度'),
    'rr.severity.1': ('1 · Négligeable', '1 · Negligible', '1 · Insignificante', '1 · ضئيلة', '1 · 軽微', '1 · 可忽略'),
    'rr.severity.2': ('2 · Mineure', '2 · Minor', '2 · Menor', '2 · طفيفة', '2 · 小', '2 · 轻微'),
    'rr.severity.3': ('3 · Significative', '3 · Significant', '3 · Significativa', '3 · ملموسة', '3 · 中程度', '3 · 显著'),
    'rr.severity.4': ('4 · Grave', '4 · Serious', '4 · Grave', '4 · خطيرة', '4 · 重大', '4 · 严重'),
    'rr.severity.5': ('5 · Catastrophique', '5 · Catastrophic', '5 · Catastrófica', '5 · كارثية', '5 · 壊滅的', '5 · 灾难性'),
    'rr.probability': ('Probabilité', 'Likelihood', 'Probabilidad', 'الاحتمالية', '発生可能性', '可能性'),
    'rr.probability.1': ('1 · Rare', '1 · Rare', '1 · Rara', '1 · نادرة', '1 · まれ', '1 · 罕见'),
    'rr.probability.2': ('2 · Peu probable', '2 · Unlikely', '2 · Poco probable', '2 · غير مرجحة', '2 · 可能性低', '2 · 不太可能'),
    'rr.probability.3': ('3 · Possible', '3 · Possible', '3 · Posible', '3 · ممكنة', '3 · あり得る', '3 · 可能'),
    'rr.probability.4': ('4 · Probable', '4 · Likely', '4 · Probable', '4 · مرجحة', '4 · 可能性高', '4 · 很可能'),
    'rr.probability.5': ('5 · Quasi certaine', '5 · Almost certain', '5 · Casi segura', '5 · شبه مؤكدة', '5 · ほぼ確実', '5 · 几乎确定'),
    'rr.gain': ('Gain attendu', 'Expected gain', 'Beneficio esperado', 'المكسب المتوقع', '期待効果', '预期收益'),
    'rr.gain.1': ('1 · Faible', '1 · Low', '1 · Bajo', '1 · منخفض', '1 · 低', '1 · 低'),
    'rr.gain.2': ('2 · Modéré', '2 · Moderate', '2 · Moderado', '2 · معتدل', '2 · 中程度', '2 · 中等'),
    'rr.gain.3': ('3 · Significatif', '3 · Significant', '3 · Significativo', '3 · ملموس', '3 · 大きい', '3 · 显著'),
    'rr.gain.4': ('4 · Fort', '4 · High', '4 · Alto', '4 · مرتفع', '4 · 非常に大きい', '4 · 高'),
    'rr.gain.5': ('5 · Très fort', '5 · Very high', '5 · Muy alto', '5 · مرتفع جدًا', '5 · 極めて大きい', '5 · 极高'),
    'rr.feasibility': ('Faisabilité', 'Feasibility', 'Viabilidad', 'قابلية التنفيذ', '実現可能性', '可行性'),
    'rr.feasibility.1': ('1 · Très difficile', '1 · Very difficult', '1 · Muy difícil', '1 · صعبة جدًا', '1 · 非常に困難', '1 · 非常困难'),
    'rr.feasibility.2': ('2 · Difficile', '2 · Difficult', '2 · Difícil', '2 · صعبة', '2 · 困難', '2 · 困难'),
    'rr.feasibility.3': ('3 · Réaliste', '3 · Realistic', '3 · Realista', '3 · واقعية', '3 · 現実的', '3 · 可行'),
    'rr.feasibility.4': ('4 · Facile', '4 · Easy', '4 · Fácil', '4 · سهلة', '4 · 容易', '4 · 容易'),
    'rr.feasibility.5': ('5 · Très facile', '5 · Very easy', '5 · Muy fácil', '5 · سهلة جدًا', '5 · 非常に容易', '5 · 非常容易'),

    # ---------- niveaux ----------
    'rr.level.low': ('Faible', 'Low', 'Bajo', 'منخفض', '低', '低'),
    'rr.level.medium': ('Moyen', 'Medium', 'Medio', 'متوسط', '中', '中'),
    'rr.level.high': ('Élevé', 'High', 'Alto', 'مرتفع', '高', '高'),
    'rr.level.critical': ('Critique', 'Critical', 'Crítico', 'حرج', '重大', '严重'),
    'rr.level.priority': ('Prioritaire', 'Priority', 'Prioritario', 'ذو أولوية', '優先', '优先'),

    # ---------- types ----------
    'rr.type.quality': ('Qualité', 'Quality', 'Calidad', 'الجودة', '品質', '质量'),
    'rr.type.environment': ('Environnement', 'Environment', 'Medio ambiente', 'البيئة', '環境', '环境'),
    'rr.type.health-safety': ('SST', 'OHS', 'SST', 'الصحة والسلامة المهنية', '労働安全衛生', '职业健康安全'),
    'rr.type.information-security': (
        "Sécurité de l'information", 'Information security', 'Seguridad de la información',
        'أمن المعلومات', '情報セキュリティ', '信息安全'),
    'rr.type.legal': ('Exigences légales', 'Legal requirements', 'Requisitos legales', 'المتطلبات القانونية', '法的要求事項', '法律要求'),

    # ---------- origines ----------
    'rr.origin.direct': ('Saisie directe', 'Direct entry', 'Entrada directa', 'إدخال مباشر', '直接入力', '直接录入'),
    'rr.origin.fmea': ('AMDEC', 'FMEA', 'AMFE', 'FMEA', 'FMEA', 'FMEA'),
    'rr.origin.non-conformity': ('Non-conformité', 'Nonconformity', 'No conformidad', 'عدم مطابقة', '不適合', '不符合项'),
    'rr.origin.audit': ('Audit', 'Audit', 'Auditoría', 'التدقيق', '監査', '审核'),
    'rr.origin.change': ('Changement (MOC)', 'Change (MOC)', 'Cambio (MOC)', 'تغيير (MOC)', '変更（MOC）', '变更（MOC）'),
    'rr.origin.incident': ('Incident', 'Incident', 'Incidente', 'حادث', 'インシデント', '事件'),
    'rr.origin.customer-feedback': ('Retour client', 'Customer feedback', 'Opinión del cliente', 'ملاحظات العملاء', '顧客フィードバック', '客户反馈'),
    'rr.origin.management-review': ('Revue de direction', 'Management review', 'Revisión por la dirección', 'مراجعة الإدارة', 'マネジメントレビュー', '管理评审'),
    'rr.origin.monitoring': ('Veille', 'Monitoring', 'Vigilancia', 'الرصد', 'モニタリング', '监测'),

    # ---------- décisions et statuts ----------
    'rr.risk-decision.undecided': ('À décider', 'Undecided', 'Por decidir', 'قيد القرار', '未決定', '待决定'),
    'rr.risk-decision.reduce': ('Réduire', 'Reduce', 'Reducir', 'تخفيف', '低減', '降低'),
    'rr.risk-decision.avoid': ('Éviter', 'Avoid', 'Evitar', 'تجنّب', '回避', '规避'),
    'rr.risk-decision.transfer': ('Transférer', 'Transfer', 'Transferir', 'نقل', '移転', '转移'),
    'rr.risk-decision.accept': ('Accepter', 'Accept', 'Aceptar', 'قبول', '受容', '接受'),
    'rr.risk-status.to-treat': ('À traiter', 'To treat', 'Por tratar', 'للمعالجة', '未対応', '待处理'),
    'rr.risk-status.in-treatment': ('En traitement', 'In treatment', 'En tratamiento', 'قيد المعالجة', '対応中', '处理中'),
    'rr.risk-status.monitored': ('Surveillé', 'Monitored', 'Vigilado', 'تحت المراقبة', '監視中', '监控中'),
    'rr.risk-status.accepted': ('Accepté', 'Accepted', 'Aceptado', 'مقبول', '受容済み', '已接受'),
    'rr.risk-status.closed': ('Clos', 'Closed', 'Cerrado', 'مغلق', 'クローズ', '已关闭'),
    'rr.opp-decision.undecided': ('À décider', 'Undecided', 'Por decidir', 'قيد القرار', '未決定', '待决定'),
    'rr.opp-decision.study': ('Étudier', 'Study', 'Estudiar', 'دراسة', '検討', '研究'),
    'rr.opp-decision.plan': ('Planifier', 'Plan', 'Planificar', 'تخطيط', '計画', '计划'),
    'rr.opp-decision.postpone': ('Reporter', 'Postpone', 'Aplazar', 'تأجيل', '延期', '推迟'),
    'rr.opp-decision.discard': ('Écarter', 'Discard', 'Descartar', 'استبعاد', '見送り', '放弃'),
    'rr.opp-status.under-study': ('En étude', 'Under study', 'En estudio', 'قيد الدراسة', '検討中', '研究中'),
    'rr.opp-status.planned': ('Planifiée', 'Planned', 'Planificada', 'مخطط لها', '計画済み', '已计划'),
    'rr.opp-status.in-progress': ('En cours', 'In progress', 'En curso', 'قيد التنفيذ', '進行中', '进行中'),
    'rr.opp-status.done': ('Réalisée', 'Achieved', 'Realizada', 'منجزة', '実現済み', '已实现'),
    'rr.opp-status.discarded': ('Écartée', 'Discarded', 'Descartada', 'مستبعدة', '見送り済み', '已放弃'),

    # ---------- historique ----------
    'rr.event.risk-created': (
        'Risque créé — origine : {$origin}{$ref}', 'Risk created — origin: {$origin}{$ref}',
        'Riesgo creado — origen: {$origin}{$ref}', 'تم إنشاء الخطر — المصدر: {$origin}{$ref}',
        'リスク作成 — 発生源：{$origin}{$ref}', '风险已创建 — 来源：{$origin}{$ref}'),
    'rr.event.opportunity-created': (
        'Opportunité créée — origine : {$origin}{$ref}', 'Opportunity created — origin: {$origin}{$ref}',
        'Oportunidad creada — origen: {$origin}{$ref}', 'تم إنشاء الفرصة — المصدر: {$origin}{$ref}',
        '機会作成 — 発生源：{$origin}{$ref}', '机遇已创建 — 来源：{$origin}{$ref}'),
    'rr.event.rating': (
        'Cotation brute passée de {$from} à {$to}', 'Gross rating changed from {$from} to {$to}',
        'Valoración bruta cambiada de {$from} a {$to}', 'تغيّر التقييم الإجمالي من {$from} إلى {$to}',
        '固有評価が {$from} から {$to} に変更', '固有评级由 {$from} 变为 {$to}'),
    'rr.event.evaluation': (
        'Évaluation passée de {$from} à {$to}', 'Evaluation changed from {$from} to {$to}',
        'Evaluación cambiada de {$from} a {$to}', 'تغيّر التقييم من {$from} إلى {$to}',
        '評価が {$from} から {$to} に変更', '评估由 {$from} 变为 {$to}'),
    'rr.event.residual': (
        'Cotation résiduelle visée : {$from} → {$to}', 'Target residual rating: {$from} → {$to}',
        'Valoración residual objetivo: {$from} → {$to}', 'التقييم المتبقي المستهدف: {$from} → {$to}',
        '目標残留評価：{$from} → {$to}', '目标剩余评级：{$from} → {$to}'),
    'rr.event.decision': (
        'Décision : {$from} → {$to}', 'Decision: {$from} → {$to}', 'Decisión: {$from} → {$to}',
        'القرار: {$from} → {$to}', '決定：{$from} → {$to}', '决定：{$from} → {$to}'),
    'rr.event.status': (
        'Statut : {$from} → {$to}', 'Status: {$from} → {$to}', 'Estado: {$from} → {$to}',
        'الحالة: {$from} → {$to}', 'ステータス：{$from} → {$to}', '状态：{$from} → {$to}'),
    'rr.event.action-opened': (
        'Action {$number} ouverte : {$title}', 'Action {$number} opened: {$title}',
        'Acción {$number} abierta: {$title}', 'تم فتح الإجراء {$number}: {$title}',
        'アクション {$number} を開始：{$title}', '措施 {$number} 已开启：{$title}'),
    'rr.event.capa-opened': (
        'Action CAPA ouverte : {$title}', 'CAPA action opened: {$title}',
        'Acción CAPA abierta: {$title}', 'تم فتح إجراء CAPA: {$title}',
        'CAPAアクションを開始：{$title}', 'CAPA 措施已开启：{$title}'),
}
