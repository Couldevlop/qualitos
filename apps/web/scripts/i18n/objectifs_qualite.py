# -*- coding: utf-8 -*-
"""Table i18n — Objectifs qualité (ISO 9001 §6.2, ADR 0133).

La revue « Objectifs vs KPI réels », la fenêtre de définition d'un objectif et
celle du rejet motivé d'une CAPA proposée. id: (fr, en, es, ar, ja, zh).

Les interpolations s'écrivent `{$INTERPOLATION}` : laissées en `{{ }}`, elles
seraient lues comme du texte et le build de production refuserait la table.
"""

TRANSLATIONS = {

    # --- navigation et droits ----------------------------------------------
    'nav.objectifs-qualite': (
        'Objectifs qualité', 'Quality objectives', 'Objetivos de calidad',
        'أهداف الجودة', '品質目標', '质量目标'),
    'authz.perm.objective-manage': (
        "Définir les objectifs qualité et trancher les CAPA qu'ils proposent",
        'Define quality objectives and decide on the CAPAs they propose',
        'Definir los objetivos de calidad y decidir sobre las CAPA que proponen',
        'تحديد أهداف الجودة والبتّ في إجراءات CAPA التي تقترحها',
        '品質目標を定め、それが提案する CAPA の採否を決める',
        '设定质量目标，并裁定其所提议的 CAPA'),

    # --- en-tête de la revue ------------------------------------------------
    'objectives.review.title': (
        'Objectifs vs KPI réels', 'Objectives vs actual KPIs',
        'Objetivos frente a KPI reales', 'الأهداف مقابل المؤشرات الفعلية',
        '品質目標と実績 KPI', '目标与实际 KPI 对比'),
    'objectives.review.subtitle': (
        "Ce que l'organisme s'est engagé à tenir, face à ce que les indicateurs mesurent "
        '(ISO 9001 §6.2).',
        'What the organization committed to, against what the indicators measure '
        '(ISO 9001 §6.2).',
        'Lo que la organización se comprometió a cumplir, frente a lo que miden los '
        'indicadores (ISO 9001 §6.2).',
        'ما تعهّدت المنظمة بتحقيقه، مقابل ما تقيسه المؤشرات (ISO 9001 §6.2).',
        '組織が約束したことと、指標が実際に測っていることの対比（ISO 9001 §6.2）。',
        '组织承诺达成的目标，对照指标实际测得的结果（ISO 9001 §6.2）。'),
    'objectives.review.crumb': ('Revue', 'Review', 'Revisión', 'مراجعة', 'レビュー', '评审'),
    'objectives.new': (
        'Nouvel objectif qualité', 'New quality objective', 'Nuevo objetivo de calidad',
        'هدف جودة جديد', '新しい品質目標', '新建质量目标'),
    'objectives.evaluate-all': (
        'Confronter aux mesures', 'Compare with measurements', 'Comparar con las mediciones',
        'المقارنة بالقياسات', '実測値と照合', '与实测数据比对'),
    'objectives.load-failed': (
        "La revue n'a pas pu être chargée.", 'The review could not be loaded.',
        'No se pudo cargar la revisión.', 'تعذّر تحميل المراجعة.',
        'レビューを読み込めませんでした。', '无法加载评审数据。'),
    'objectives.empty': (
        "Aucun objectif qualité n'est suivi. L'ISO 9001 §6.2 en demande pour les processus "
        'qui comptent.',
        'No quality objective is being tracked. ISO 9001 §6.2 requires them for the '
        'processes that matter.',
        'No se sigue ningún objetivo de calidad. La ISO 9001 §6.2 los exige para los '
        'procesos que importan.',
        'لا يُتابَع أي هدف جودة. يقتضي ISO 9001 §6.2 وجودها للعمليات المهمّة.',
        '追跡中の品質目標がありません。ISO 9001 §6.2 は重要なプロセスについてこれを求めています。',
        '当前没有跟踪任何质量目标。ISO 9001 §6.2 要求对重要过程设定目标。'),

    # --- filtres ------------------------------------------------------------
    'objectives.filter.as-of': ('Période', 'Period', 'Periodo', 'الفترة', '期間', '期间'),
    'objectives.filter.process': (
        'Processus', 'Process', 'Proceso', 'العملية', 'プロセス', '过程'),
    'objectives.filter.process-all': ('Tous', 'All', 'Todos', 'الكل', 'すべて', '全部'),

    # --- tuiles -------------------------------------------------------------
    'objectives.tile.tracked': (
        'Objectifs suivis', 'Objectives tracked', 'Objetivos seguidos',
        'أهداف متابَعة', '追跡中の目標', '跟踪中的目标'),
    'objectives.tile.on-target': (
        'Atteints', 'On target', 'Alcanzados', 'محقَّقة', '達成', '已达成'),
    'objectives.tile.warning': (
        'En alerte', 'Warning', 'En alerta', 'في حالة تنبيه', '警告', '预警中'),
    'objectives.tile.critical': (
        'Critiques', 'Critical', 'Críticos', 'حرجة', '重大', '严重'),

    # --- colonnes du tableau ------------------------------------------------
    'objectives.column.objective': (
        'Objectif', 'Objective', 'Objetivo', 'الهدف', '目標', '目标'),
    'objectives.column.target': ('Cible', 'Target', 'Meta', 'المستهدف', '目標値', '目标值'),
    'objectives.column.actual': ('Réel', 'Actual', 'Real', 'الفعلي', '実績', '实际值'),
    'objectives.column.deviation': ('Écart', 'Deviation', 'Desviación', 'الفارق', '乖離', '偏差'),
    'objectives.column.status': ('Statut', 'Status', 'Estado', 'الحالة', '状態', '状态'),

    # --- statuts et niveaux -------------------------------------------------
    'objectives.status.not-evaluated': (
        'Non mesuré', 'Not measured', 'Sin medir', 'غير مقيس', '未測定', '未测量'),
    'objectives.status.on-target': (
        'Atteint', 'On target', 'Alcanzado', 'محقَّق', '達成', '已达成'),
    'objectives.status.warning': ('Alerte', 'Warning', 'Alerta', 'تنبيه', '警告', '预警'),
    'objectives.status.critical': ('Critique', 'Critical', 'Crítico', 'حرج', '重大', '严重'),
    'objectives.level.warning': ('Alerte', 'Warning', 'Alerta', 'تنبيه', '警告', '预警'),
    'objectives.level.critical': ('Critique', 'Critical', 'Crítico', 'حرج', '重大', '严重'),
    'objectives.proposal.pending': (
        'À trancher', 'To decide', 'Por decidir', 'بانتظار القرار', '要判断', '待裁定'),
    'objectives.proposal.accepted': (
        'Acceptée', 'Accepted', 'Aceptada', 'مقبولة', '受理', '已接受'),
    'objectives.proposal.rejected': (
        'Rejetée', 'Rejected', 'Rechazada', 'مرفوضة', '却下', '已拒绝'),

    # --- périodes -----------------------------------------------------------
    'objectives.period.1': ('1 mois', '1 month', '1 mes', 'شهر واحد', '1 か月', '1 个月'),
    'objectives.period.3': ('3 mois', '3 months', '3 meses', '3 أشهر', '3 か月', '3 个月'),
    'objectives.period.6': ('6 mois', '6 months', '6 meses', '6 أشهر', '6 か月', '6 个月'),
    'objectives.period.12': ('12 mois', '12 months', '12 meses', '12 شهرًا', '12 か月', '12 个月'),

    # --- colonne CAPA -------------------------------------------------------
    'objectives.capa.open': (
        'CAPA en cours · retour sous la cible attendu',
        'CAPA in progress · awaiting return to target',
        'CAPA en curso · se espera el regreso a la meta',
        'إجراء CAPA جارٍ · في انتظار العودة إلى المستهدف',
        'CAPA 進行中 · 目標値への回復を待機',
        'CAPA 进行中 · 等待回到目标值'),
    'objectives.capa.none': ('Aucune', 'None', 'Ninguna', 'لا شيء', 'なし', '无'),
    'objectives.recovery.remaining': (
        '{$tenues} période(s) sous la cible sur {$exigees} — il en reste '
        '{$reste}',
        '{$tenues} period(s) on target out of {$exigees} — '
        '{$reste} to go',
        '{$tenues} periodo(s) en la meta de {$exigees} — faltan '
        '{$reste}',
        '{$tenues} من {$exigees} فترة ضمن المستهدف — تبقّى {$reste}',
        '目標値内の期間は {$exigees} 期間中 {$tenues} 期間 — あと '
        '{$reste} 期間',
        '{$exigees} 个期间中已有 {$tenues} 个达标 — 还差 {$reste} 个'),

    # --- panneau de la CAPA préremplie -------------------------------------

    # --- retours ------------------------------------------------------------
    'objectives.rejected': (
        'Proposition rejetée, avec son motif.', 'Proposal rejected, with its reason.',
        'Propuesta rechazada, con su motivo.', 'رُفض الاقتراح مع بيان سببه.',
        '理由とともに提案を却下しました。', '已拒绝该提议，并记录理由。'),
    'objectives.evaluated.some': (
        '{$count} CAPA proposée(s) à trancher.',
        '{$count} CAPA(s) proposed, awaiting a decision.',
        '{$count} CAPA propuesta(s) pendientes de decisión.',
        '{$count} إجراء CAPA مقترح بانتظار القرار.',
        '判断待ちの CAPA 提案が {$count} 件あります。',
        '有 {$count} 项 CAPA 提议待裁定。'),
    'objectives.evaluated.none': (
        "Aucun écart n'atteint la règle de déclenchement.",
        'No deviation reaches the trigger rule.',
        'Ninguna desviación alcanza la regla de activación.',
        'لا يوجد فارق يستوفي قاعدة التحفيز.',
        'トリガー条件に達した乖離はありません。',
        '没有偏差达到触发规则。'),

    # --- retrait du suivi ---------------------------------------------------
    'objectives.archive.aria': (
        'Retirer du suivi', 'Remove from tracking', 'Retirar del seguimiento',
        'إزالة من المتابعة', '追跡から外す', '移出跟踪'),
    'objectives.archive.title': (
        'Retirer cet objectif du suivi ?', 'Remove this objective from tracking?',
        '¿Retirar este objetivo del seguimiento?', 'إزالة هذا الهدف من المتابعة؟',
        'この目標を追跡から外しますか？', '将该目标移出跟踪？'),
    'objectives.archive.message': (
        '« {$titre} » ne comptera plus dans la revue. Il est conservé, avec les CAPA '
        "qu'il a fait ouvrir : rien n'est effacé.",
        '“{$titre}” will no longer count in the review. It is kept, along with the '
        'CAPAs it opened: nothing is deleted.',
        '«{$titre}» ya no contará en la revisión. Se conserva, junto con las CAPA que '
        'abrió: nada se borra.',
        '«{$titre}» لن يُحتسب في المراجعة. يُحفَظ مع إجراءات CAPA التي فتحها: لا شيء يُمحى.',
        '「{$titre}」はレビューの集計から外れます。開いた CAPA とともに保存され、'
        '何も削除されません。',
        '“{$titre}”将不再计入评审。它与其开立的 CAPA 一并保留，不会删除任何内容。'),
    'objectives.archive.confirm': (
        'Retirer du suivi', 'Remove from tracking', 'Retirar del seguimiento',
        'إزالة من المتابعة', '追跡から外す', '移出跟踪'),

    # --- fenêtre « Nouvel objectif qualité » --------------------------------
    'objectives.dialog.title': (
        'Nouvel objectif qualité', 'New quality objective', 'Nuevo objetivo de calidad',
        'هدف جودة جديد', '新しい品質目標', '新建质量目标'),
    'objectives.dialog.edit-title': (
        "Modifier l'objectif qualité", 'Edit the quality objective',
        'Modificar el objetivo de calidad', 'تعديل هدف الجودة',
        '品質目標を編集', '修改质量目标'),
    'objectives.dialog.subtitle': (
        "Un objectif engage : il dit ce qu'on attend d'un indicateur, et ce qu'on fera quand "
        "l'attente n'est pas tenue (ISO 9001 §6.2).",
        'An objective commits: it states what is expected of an indicator, and what will be '
        'done when the expectation is not met (ISO 9001 §6.2).',
        'Un objetivo compromete: dice qué se espera de un indicador y qué se hará cuando no '
        'se cumpla (ISO 9001 §6.2).',
        'الهدف التزام: يبيّن ما يُتوقَّع من المؤشر، وما سيُفعَل عند عدم تحقّق التوقّع '
        '(ISO 9001 §6.2).',
        '目標は約束です。指標に何を期待し、それが満たされないときに何をするのかを定めます'
        '（ISO 9001 §6.2）。',
        '目标即承诺：它说明对某项指标的期望，以及期望未达成时将采取什么行动（ISO 9001 §6.2）。'),
    'objectives.dialog.submit': (
        "Enregistrer l'objectif", 'Save the objective', 'Guardar el objetivo',
        'حفظ الهدف', '目標を保存', '保存目标'),
    'objectives.section.objective': (
        '1. Objectif', '1. Objective', '1. Objetivo', '1. الهدف', '1. 目標', '1. 目标'),
    'objectives.section.kpi': (
        '2. KPI réel à comparer', '2. Actual KPI to compare', '2. KPI real a comparar',
        '2. المؤشر الفعلي للمقارنة', '2. 比較する実績 KPI', '2. 用于比对的实际 KPI'),
    'objectives.section.rule': (
        '3. Règle de déclenchement CAPA', '3. CAPA trigger rule',
        '3. Regla de activación de la CAPA', '3. قاعدة تحفيز إجراء CAPA',
        '3. CAPA のトリガー条件', '3. CAPA 触发规则'),

    # --- champs de la fenêtre ----------------------------------------------
    'objectives.field.title': ('Intitulé', 'Title', 'Título', 'العنوان', '名称', '名称'),
    'objectives.field.title-placeholder': (
        'Ex. : Taux de NC client', 'E.g. Customer NC rate', 'Ej.: Tasa de NC de cliente',
        'مثال: معدّل حالات عدم المطابقة لدى العميل', '例：顧客不適合率', '例：客户不合格率'),
    'objectives.field.title-required': (
        "L'intitulé est requis.", 'The title is required.', 'El título es obligatorio.',
        'العنوان مطلوب.', '名称は必須です。', '名称为必填项。'),
    'objectives.field.process': (
        'Processus', 'Process', 'Proceso', 'العملية', 'プロセス', '过程'),
    'objectives.field.process-none': ('Aucun', 'None', 'Ninguno', 'لا شيء', 'なし', '无'),
    'objectives.field.iso-clause': (
        'Clause ISO 9001', 'ISO 9001 clause', 'Cláusula ISO 9001',
        'بند ISO 9001', 'ISO 9001 箇条', 'ISO 9001 条款'),
    'objectives.field.kpi-locked': (
        "Le KPI d'un objectif ne change pas : son historique cesserait d'être comparable.",
        "An objective's KPI does not change: its history would stop being comparable.",
        'El KPI de un objetivo no cambia: su historial dejaría de ser comparable.',
        'لا يتغيّر مؤشر الهدف: فتاريخه سيفقد قابليته للمقارنة.',
        '目標の KPI は変更できません。履歴が比較できなくなるためです。',
        '目标的 KPI 不可更改：否则其历史数据将无法比较。'),
    'objectives.field.kpi-required': (
        'Un objectif se mesure contre un KPI du catalogue.',
        'An objective is measured against a KPI from the catalogue.',
        'Un objetivo se mide contra un KPI del catálogo.',
        'يُقاس الهدف مقابل مؤشر من الدليل.',
        '目標はカタログの KPI に対して測ります。',
        '目标需对照目录中的某个 KPI 来衡量。'),
    'objectives.field.direction': (
        "Sens d'amélioration", 'Improvement direction', 'Sentido de mejora',
        'اتجاه التحسين', '改善の方向', '改进方向'),
    'objectives.field.direction-note': (
        'repris du KPI', 'taken from the KPI', 'tomado del KPI',
        'مأخوذ من المؤشر', 'KPI から取得', '取自 KPI'),
    'objectives.direction.lower': (
        'Diminuer', 'Decrease', 'Disminuir', 'التخفيض', '減少', '降低'),
    'objectives.direction.higher': (
        'Augmenter', 'Increase', 'Aumentar', 'الزيادة', '増加', '提高'),
    'objectives.direction.unknown': (
        'Choisissez un KPI', 'Choose a KPI', 'Elija un KPI',
        'اختر مؤشرًا', 'KPI を選択してください', '请选择一个 KPI'),
    'objectives.field.target': ('Cible', 'Target', 'Meta', 'المستهدف', '目標値', '目标值'),
    'objectives.field.target-required': (
        'La cible est requise.', 'The target is required.', 'La meta es obligatoria.',
        'المستهدف مطلوب.', '目標値は必須です。', '目标值为必填项。'),
    'objectives.field.warning': (
        "Seuil d'alerte", 'Warning threshold', 'Umbral de alerta',
        'حدّ التنبيه', '警告しきい値', '预警阈值'),
    'objectives.field.warning-required': (
        "Le seuil d'alerte est requis.", 'The warning threshold is required.',
        'El umbral de alerta es obligatorio.', 'حدّ التنبيه مطلوب.',
        '警告しきい値は必須です。', '预警阈值为必填项。'),
    'objectives.field.critical': (
        'Seuil critique', 'Critical threshold', 'Umbral crítico',
        'الحدّ الحرج', '重大しきい値', '严重阈值'),
    'objectives.field.critical-required': (
        'Le seuil critique est requis.', 'The critical threshold is required.',
        'El umbral crítico es obligatorio.', 'الحدّ الحرج مطلوب.',
        '重大しきい値は必須です。', '严重阈值为必填项。'),
    'objectives.thresholds.order-lower': (
        "Sur un KPI qu'on cherche à diminuer, la cible doit être inférieure ou égale au seuil "
        "d'alerte, lui-même inférieur ou égal au seuil critique.",
        'On a KPI to be decreased, the target must be at or below the warning threshold, '
        'which must be at or below the critical threshold.',
        'En un KPI que se busca disminuir, la meta debe ser menor o igual al umbral de '
        'alerta, y este menor o igual al umbral crítico.',
        'في مؤشر يُراد تخفيضه، يجب أن يكون المستهدف أدنى من حدّ التنبيه أو مساويًا له، وحدّ '
        'التنبيه أدنى من الحدّ الحرج أو مساويًا له.',
        '減少させたい KPI では、目標値は警告しきい値以下、警告しきい値は重大しきい値以下で'
        'なければなりません。',
        '对于希望降低的 KPI，目标值须小于或等于预警阈值，预警阈值须小于或等于严重阈值。'),
    'objectives.thresholds.order-higher': (
        "Sur un KPI qu'on cherche à augmenter, la cible doit être supérieure ou égale au "
        "seuil d'alerte, lui-même supérieur ou égal au seuil critique.",
        'On a KPI to be increased, the target must be at or above the warning threshold, '
        'which must be at or above the critical threshold.',
        'En un KPI que se busca aumentar, la meta debe ser mayor o igual al umbral de alerta, '
        'y este mayor o igual al umbral crítico.',
        'في مؤشر يُراد زيادته، يجب أن يكون المستهدف أعلى من حدّ التنبيه أو مساويًا له، وحدّ '
        'التنبيه أعلى من الحدّ الحرج أو مساويًا له.',
        '増加させたい KPI では、目標値は警告しきい値以上、警告しきい値は重大しきい値以上で'
        'なければなりません。',
        '对于希望提高的 KPI，目标值须大于或等于预警阈值，预警阈值须大于或等于严重阈值。'),
    'objectives.field.period': ('Période', 'Period', 'Periodo', 'الفترة', '期間', '期间'),
    'objectives.field.period-hint': (
        "L'unité dans laquelle se comptent les dépassements consécutifs.",
        'The unit in which consecutive breaches are counted.',
        'La unidad en la que se cuentan los incumplimientos consecutivos.',
        'الوحدة التي تُحتسَب بها التجاوزات المتتالية.',
        '連続した逸脱を数える単位です。',
        '连续超限的计数单位。'),

    # --- règle de déclenchement --------------------------------------------
    'objectives.rule.warning': ('Alerte', 'Warning', 'Alerta', 'تنبيه', '警告', '预警'),
    'objectives.rule.warning-before': (
        "Si le KPI dépasse le seuil d'alerte", 'If the KPI breaches the warning threshold',
        'Si el KPI supera el umbral de alerta', 'إذا تجاوز المؤشر حدّ التنبيه',
        'KPI が警告しきい値を超えた場合', '若 KPI 超出预警阈值'),
    'objectives.rule.warning-after': (
        'périodes consécutives → une CAPA est <strong>proposée</strong> au pilote.',
        'consecutive periods → a CAPA is <strong>proposed</strong> to the owner.',
        'periodos consecutivos → se <strong>propone</strong> una CAPA al responsable.',
        'فترات متتالية ← <strong>يُقترَح</strong> إجراء CAPA على المسؤول.',
        '連続した期間 → CAPA が責任者に<strong>提案</strong>されます。',
        '个连续期间 → 系统向负责人<strong>提议</strong>一项 CAPA。'),
    'objectives.rule.critical': ('Critique', 'Critical', 'Crítico', 'حرج', '重大', '严重'),
    'objectives.rule.critical-before': (
        'Si le KPI dépasse le seuil critique sur',
        'If the KPI breaches the critical threshold over',
        'Si el KPI supera el umbral crítico durante', 'إذا تجاوز المؤشر الحدّ الحرج خلال',
        'KPI が重大しきい値を超えた期間が', '若 KPI 在'),
    'objectives.rule.critical-after': (
        'période → la CAPA est <strong>proposée automatiquement</strong>.',
        'period → the CAPA is <strong>proposed automatically</strong>.',
        'periodo → la CAPA se <strong>propone automáticamente</strong>.',
        'فترة ← <strong>يُقترَح</strong> إجراء CAPA <strong>تلقائيًا</strong>.',
        '期間に達すると、CAPA が<strong>自動的に提案</strong>されます。',
        '个期间内超出严重阈值 → 系统<strong>自动提议</strong> CAPA。'),
    'objectives.rule.periods-aria': (
        'Nombre de périodes', 'Number of periods', 'Número de periodos',
        'عدد الفترات', '期間数', '期间数'),
    'objectives.field.recovery': (
        'Retour à « atteint » après', 'Back to “on target” after',
        'Vuelta a «alcanzado» tras', 'العودة إلى «محقَّق» بعد',
        '「達成」に戻る条件', '恢复“已达成”所需'),
    'objectives.field.periods-suffix': (
        'période(s) sous la cible', 'period(s) on target', 'periodo(s) en la meta',
        'فترة/فترات ضمن المستهدف', '期間（目標値内）', '个达标期间'),
    'objectives.field.recovery-hint': (
        "C'est aussi le critère d'efficacité de la CAPA : elle ne se clôt qu'au retour durable "
        'du KPI sous la cible.',
        "This is also the CAPA's effectiveness criterion: it only closes once the KPI is "
        'durably back on target.',
        'Es también el criterio de eficacia de la CAPA: solo se cierra cuando el KPI vuelve '
        'de forma duradera a la meta.',
        'وهو أيضًا معيار فعالية إجراء CAPA: لا يُغلَق إلا بعودة المؤشر المستدامة إلى المستهدف.',
        'これは CAPA の有効性判定基準でもあります。KPI が持続的に目標値へ戻るまで CAPA は'
        '完了しません。',
        '这同时是 CAPA 的有效性判定标准：只有 KPI 持续回到目标值，CAPA 才会关闭。'),

    # --- fenêtre de rejet motivé -------------------------------------------
    'objectives.reject.title': (
        'Rejeter la CAPA proposée', 'Reject the proposed CAPA', 'Rechazar la CAPA propuesta',
        'رفض إجراء CAPA المقترح', '提案された CAPA を却下', '拒绝所提议的 CAPA'),
    'objectives.reject.subtitle': (
        "L'écart reste écrit. Le motif dit pourquoi il ne donne pas lieu à un dossier — c'est "
        "la question qu'un auditeur posera.",
        'The deviation stays on record. The reason says why it does not open a case — that is '
        'the question an auditor will ask.',
        'La desviación queda registrada. El motivo dice por qué no abre un caso: es la '
        'pregunta que hará un auditor.',
        'يبقى الفارق مسجّلًا. والسبب يبيّن لماذا لا يُفتَح له ملف — وهو السؤال الذي سيطرحه المراجع.',
        '乖離は記録として残ります。理由は、なぜ案件を開かないのかを示します — 監査員が問うのは'
        'まさにそこです。',
        '偏差仍会留档。理由说明为何不为其开立案件——这正是审核员会追问的地方。'),
    'objectives.reject.submit': (
        'Rejeter avec ce motif', 'Reject with this reason', 'Rechazar con este motivo',
        'الرفض بهذا السبب', 'この理由で却下', '以此理由拒绝'),
    'objectives.reject.reason': (
        'Motif du rejet', 'Reason for rejection', 'Motivo del rechazo',
        'سبب الرفض', '却下理由', '拒绝理由'),
    'objectives.reject.reason-placeholder': (
        "Ex. : écart dû à l'arrêt planifié de la ligne 2, déjà traité par la CAPA-2026-0031.",
        'E.g. deviation caused by the planned shutdown of line 2, already handled by '
        'CAPA-2026-0031.',
        'Ej.: desviación por la parada planificada de la línea 2, ya tratada por la '
        'CAPA-2026-0031.',
        'مثال: فارق ناتج عن التوقّف المخطَّط للخطّ 2، وقد عُولج في CAPA-2026-0031.',
        '例：2 号ラインの計画停止による乖離で、CAPA-2026-0031 で既に対応済み。',
        '例：因 2 号线计划停机导致的偏差，已由 CAPA-2026-0031 处理。'),
    'objectives.reject.reason-required': (
        "Le motif est obligatoire : sans lui, un écart écarté ne se distingue pas d'un écart "
        "que personne n'a regardé.",
        'The reason is mandatory: without it, a dismissed deviation is indistinguishable from '
        'one nobody looked at.',
        'El motivo es obligatorio: sin él, una desviación descartada no se distingue de una '
        'que nadie miró.',
        'السبب إلزامي: بدونه لا يمكن تمييز فارق استُبعد عن فارق لم ينظر فيه أحد.',
        '理由は必須です。これがないと、検討の上で見送った乖離と、誰も見ていない乖離を'
        '区別できません。',
        '理由为必填项：否则，经判断不予处理的偏差，与无人查看的偏差无法区分。'),
    'objectives.error.save': (
        "L'objectif n'a pas pu être enregistré.", 'The objective could not be saved.',
        'No se ha podido guardar el objetivo.',
        'لم يتمكن النظام من حفظ الهدف.',
        '目標を保存できませんでした。', '未能保存该目标。'),
    'objectives.error.evaluate': (
        "La confrontation aux mesures n'a pas abouti.",
        'The comparison against the measurements did not complete.',
        'La comparación con las mediciones no ha podido completarse.',
        'لم تكتمل المقارنة مع القياسات.',
        '実測値との照合を完了できませんでした。',
        '与实测值的比对未能完成。'),
    'objectives.error.reject': (
        "Le rejet n'a pas pu être enregistré.", 'The rejection could not be saved.',
        'No se ha podido guardar el rechazo.',
        'لم يتمكن النظام من حفظ الرفض.',
        '却下を保存できませんでした。', '未能保存拒绝。'),
    'objectives.error.archive': (
        "Le retrait du suivi n'a pas abouti.", 'Removing the objective from tracking failed.',
        'No se ha podido retirar el objetivo del seguimiento.',
        'لم ينجح سحب الهدف من المتابعة.',
        '目標を追跡対象から外せませんでした。',
        '未能将该目标移出跟踪。'),
    # --- la colonne « Action » : rien n'est prérédigé (recette 27/11, ADR 0143)
    'objectives.column.action': (
        'Action', 'Action', 'Acción', 'إجراء', 'アクション', '措施'),
    'objectives.action.add': (
        'Ajouter une action', 'Add an action', 'Añadir una acción',
        'إضافة إجراء', 'アクションを追加', '添加措施'),
    'objectives.action.decline': (
        'Aucune action — justifier', 'No action — give a reason',
        'Ninguna acción: justificar', 'لا إجراء — مع التبرير',
        'アクションなし — 理由を記載', '不采取措施 — 说明理由'),

    # --- le filtre de période en mm/aaaa (recette, diapo 5) -----------------
    'objectives.filter.as-of-current': (
        'Période courante', 'Current period', 'Periodo actual',
        'الفترة الحالية', '現在の期間', '当前周期'),

    # --- le code du KPI, et la reprise de ses seuils (recette, diapo 3) -----
    'objectives.field.kpi-code': (
        'Code du KPI', 'KPI code', 'Código del KPI',
        'رمز مؤشر الأداء', 'KPI コード', 'KPI 代码'),
    'objectives.field.kpi-hint': (
        "La source de données de l'objectif. Ses seuils descendent dans les "
        'trois champs ci-dessous.',
        "The objective's data source. Its thresholds drop into the three "
        'fields below.',
        'La fuente de datos del objetivo. Sus umbrales bajan a los tres campos '
        'de abajo.',
        'مصدر بيانات الهدف. تنتقل حدوده إلى الحقول الثلاثة أدناه.',
        '目標のデータ源です。そのしきい値が下の 3 つの項目に入ります。',
        '本目标的数据来源。其阈值会填入下方三个字段。'),
    'objectives.thresholds.differ': (
        "Ces seuils diffèrent de ceux du KPI. À l'enregistrement, la fenêtre "
        'proposera de les y reporter.',
        "These thresholds differ from the KPI's. On saving, the dialog will "
        'offer to carry them over to it.',
        'Estos umbrales difieren de los del KPI. Al guardar, la ventana '
        'propondrá trasladarlos a él.',
        'تختلف هذه الحدود عن حدود مؤشر الأداء. عند الحفظ، ستقترح النافذة نقلها إليه.',
        'これらのしきい値は KPI のものと異なります。保存時に KPI へ反映するか確認します。',
        '这些阈值与 KPI 的阈值不同。保存时窗口会询问是否同步到 KPI。'),
    'objectives.kpi-sync.title': (
        'Reporter ces seuils sur le KPI ?', 'Carry these thresholds over to the KPI?',
        '¿Trasladar estos umbrales al KPI?', 'هل تريد نقل هذه الحدود إلى مؤشر الأداء؟',
        'これらのしきい値を KPI に反映しますか？', '要将这些阈值同步到 KPI 吗？'),
    'objectives.kpi-sync.message': (
        "Le KPI « {$kpi} » ({$code}) porte d'autres seuils. Les remplacer par "
        'ceux de cet objectif évite de les saisir deux fois — mais un autre '
        'objectif adossé au même indicateur garde les siens.',
        'The KPI "{$kpi}" ({$code}) holds different thresholds. Replacing them '
        'with this objective\'s avoids entering them twice — but another '
        'objective backed by the same indicator keeps its own.',
        'El KPI «{$kpi}» ({$code}) tiene otros umbrales. Sustituirlos por los de '
        'este objetivo evita introducirlos dos veces, pero otro objetivo '
        'apoyado en el mismo indicador conserva los suyos.',
        'يحمل مؤشر الأداء «{$kpi}» ({$code}) حدودًا مختلفة. استبدالها بحدود هذا '
        'الهدف يغنيك عن إدخالها مرتين، لكن هدفًا آخر يستند إلى المؤشر نفسه '
        'يحتفظ بحدوده.',
        'KPI「{$kpi}」（{$code}）には別のしきい値が設定されています。この目標の値に '
        '置き換えれば二重入力を避けられますが、同じ指標に紐づく別の目標は自身の値を保ちます。',
        'KPI“{$kpi}”（{$code}）的阈值不同。用本目标的阈值替换可避免重复录入，'
        '但依托同一指标的其他目标仍保留各自的阈值。'),
    'objectives.kpi-sync.confirm': (
        'Reporter sur le KPI', 'Carry over to the KPI', 'Trasladar al KPI',
        'نقل إلى مؤشر الأداء', 'KPI に反映する', '同步到 KPI'),
    'objectives.kpi-sync.cancel': (
        'Laisser le KPI tel quel', 'Leave the KPI as it is',
        'Dejar el KPI como está', 'إبقاء مؤشر الأداء كما هو',
        'KPI はそのままにする', '保持 KPI 不变'),
}
