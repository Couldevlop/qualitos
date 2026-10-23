# -*- coding: utf-8 -*-
"""Libellés de l'efficacité CAPA mesurée et de la matrice de compétences.

Les XLF sont GÉNÉRÉS depuis ces tables : ne jamais éditer `src/locale/*.xlf` à la
main, la CI rejoue la génération et refuse toute divergence.

Ordre des langues : fr, en, es, ar, ja, zh.
"""

TRANSLATIONS = {
    # ---------------------------------------------------------------- CAPA
    'capa.effectiveness.title': (
        "Efficacité des CAPA", 'CAPA effectiveness', 'Eficacia de las CAPA',
        'فعالية الإجراءات التصحيحية', 'CAPA の有効性', 'CAPA 有效性'),
    'capa.effectiveness.subtitle': (
        "Ce que le terrain dit des dossiers clos, et non ce qu'on avait déclaré à la clôture.",
        'What the shop floor says about closed cases — not what was declared at closure.',
        'Lo que dice el terreno sobre los casos cerrados, no lo declarado al cierre.',
        'ما يقوله الواقع عن الملفات المغلقة، لا ما أُعلن عند الإغلاق.',
        'クローズ済み案件について現場が示す事実であり、クローズ時の申告ではありません。',
        '关于已关闭案件，现场给出的事实，而非结案时的声明。'),
    'capa.effectiveness.window-aria': (
        "Fenêtre d'observation", 'Observation window', 'Ventana de observación',
        'نافذة الملاحظة', '観察期間', '观察窗口'),
    'capa.effectiveness.months': ('mois', 'months', 'meses', 'أشهر', 'か月', '个月'),
    'capa.effectiveness.average': (
        'Efficacité moyenne', 'Average effectiveness', 'Eficacia media',
        'متوسط الفعالية', '平均有効性', '平均有效性'),
    'capa.effectiveness.average-hint': (
        'Sur les seuls dossiers mesurables', 'Across measurable cases only',
        'Solo sobre los casos medibles', 'على الملفات القابلة للقياس فقط',
        '測定可能な案件のみ', '仅统计可测量的案件'),
    'capa.effectiveness.measured': ('Mesurées', 'Measured', 'Medidas', 'مقيسة', '測定済み', '已测量'),
    'capa.effectiveness.in-observation': (
        'En observation', 'Under observation', 'En observación',
        'قيد الملاحظة', '観察中', '观察中'),
    'capa.effectiveness.in-observation-hint': (
        'Fenêtre non écoulée', 'Window not yet elapsed', 'Ventana aún no cumplida',
        'لم تنتهِ النافذة بعد', '観察期間が未経過', '观察期尚未结束'),
    'capa.effectiveness.declared-but-failed': (
        'Déclarées efficaces à tort', 'Wrongly declared effective',
        'Declaradas eficaces por error', 'أُعلنت فعّالة خطأً',
        '有効と誤って申告', '被错误声明为有效'),
    'capa.effectiveness.truncated': (
        "Le périmètre dépasse la limite de lecture : seuls les dossiers les plus récents sont "
        "mesurés. La moyenne ne porte donc pas sur la totalité de l'historique.",
        'The scope exceeds the read limit: only the most recent cases are measured, so the '
        'average does not cover the whole history.',
        'El alcance supera el límite de lectura: solo se miden los casos más recientes, por lo '
        'que la media no cubre todo el historial.',
        'النطاق يتجاوز حد القراءة: تُقاس أحدث الملفات فقط، ولذلك لا يشمل المتوسط كامل السجل.',
        '対象が読み取り上限を超えています。直近の案件のみを測定するため、平均は全履歴を表しません。',
        '范围超出读取上限：仅测量最近的案件，因此平均值并未涵盖全部历史。'),
    'capa.effectiveness.none': (
        "Aucun dossier mesurable. L'efficacité se mesure sur une CAPA close née d'une "
        "non-conformité : sans elle, il n'y a pas de récidive à guetter.",
        'No measurable case. Effectiveness is measured on a closed CAPA born from a '
        'non-conformity: without one, there is no recurrence to watch for.',
        'Ningún caso medible. La eficacia se mide sobre una CAPA cerrada nacida de una no '
        'conformidad: sin ella no hay recurrencia que vigilar.',
        'لا يوجد ملف قابل للقياس. تُقاس الفعالية على إجراء مغلق ناتج عن حالة عدم مطابقة؛ وبدونه لا '
        'يوجد تكرار يمكن مراقبته.',
        '測定可能な案件がありません。有効性は不適合から生まれたクローズ済み CAPA で測ります。',
        '没有可测量的案件。有效性基于源自不合格项的已关闭 CAPA 来衡量。'),
    'capa.effectiveness.col-capa': ('CAPA', 'CAPA', 'CAPA', 'الإجراء', 'CAPA', 'CAPA'),
    'capa.effectiveness.col-closed': (
        'Clôturée le', 'Closed on', 'Cerrada el', 'أُغلقت في', 'クローズ日', '关闭日期'),
    'capa.effectiveness.col-recurrence': (
        'Récidives', 'Recurrences', 'Recurrencias', 'التكرارات', '再発', '再发生'),
    'capa.effectiveness.col-rate': (
        'Efficacité', 'Effectiveness', 'Eficacia', 'الفعالية', '有効性', '有效性'),
    'capa.effectiveness.col-declared': (
        'Déclarée', 'Declared', 'Declarada', 'المُعلن', '申告', '声明'),
    'capa.effectiveness.before': ('avant', 'before', 'antes', 'قبل', '前', '之前'),
    'capa.effectiveness.elapsed': ('en', 'in', 'en', 'خلال', '経過', '历时'),
    'capa.effectiveness.too-early': (
        'Trop tôt pour conclure', 'Too early to conclude', 'Demasiado pronto para concluir',
        'من المبكر الحكم', '結論を出すには早い', '尚不能下结论'),
    'capa.effectiveness.not-measurable': (
        'Aucune occurrence antérieure', 'No prior occurrence', 'Ninguna ocurrencia previa',
        'لا توجد حالات سابقة', '過去の発生なし', '此前无发生记录'),
    'capa.effectiveness.declared-yes': (
        'Efficace', 'Effective', 'Eficaz', 'فعّال', '有効', '有效'),
    'capa.effectiveness.declared-no': (
        'Non prononcée', 'Not stated', 'No pronunciada', 'لم يُبتّ فيها', '未判定', '未判定'),
    'capa.effectiveness.contradicted-aria': (
        'Démentie par la mesure', 'Contradicted by the measurement',
        'Desmentida por la medición', 'يناقضه القياس', '測定結果と矛盾', '与测量结果相悖'),
    'capa.effectiveness.approximate': (
        'Rapprochement par catégorie — taux indicatif',
        'Matched by category — rate is indicative',
        'Coincidencia por categoría: tasa indicativa',
        'المطابقة حسب الفئة — النسبة إرشادية',
        'カテゴリー一致のため参考値', '按类别匹配，比率仅供参考'),
    'capa.effectiveness.failed': (
        "Impossible de charger l'efficacité des CAPA.", 'Could not load CAPA effectiveness.',
        'No se ha podido cargar la eficacia de las CAPA.', 'تعذّر تحميل فعالية الإجراءات.',
        'CAPA の有効性を読み込めませんでした。', '无法加载 CAPA 有效性。'),

    'nav.capa-efficacite': ('Efficacité CAPA', 'CAPA effectiveness', 'Eficacia CAPA', 'فعالية الإجراءات', 'CAPA 有効性', 'CAPA 有效性'),
    'nav.competences': ('Matrice de compétences', 'Competency matrix', 'Matriz de competencias', 'مصفوفة الكفاءات', 'スキルマトリクス', '能力矩阵'),

    # ------------------------------------------------------ matrice de compétences
    'training.matrix.title': (
        'Matrice de compétences', 'Competency matrix', 'Matriz de competencias',
        'مصفوفة الكفاءات', 'スキルマトリクス', '能力矩阵'),
    'training.matrix.eyebrow': (
        'Formation', 'Training', 'Formación', 'التدريب', '教育訓練', '培训'),
    'training.matrix.subtitle-editable': (
        'Cliquez sur une case pour changer le niveau. Objectif : au moins deux personnes autonomes '
        '(niveau 2 ou plus) par compétence.',
        'Click a cell to change the level. Goal: at least two autonomous people (level 2 or more) '
        'per skill.',
        'Haga clic en una casilla para cambiar el nivel. Objetivo: al menos dos personas autónomas '
        '(nivel 2 o más) por competencia.',
        'انقر على خانة لتغيير المستوى. الهدف: شخصان مستقلان على الأقل (المستوى 2 أو أكثر) لكل كفاءة.',
        'セルをクリックするとレベルが変わります。目標：各スキルに自立した人（レベル2以上）を2人以上。',
        '点击单元格可更改等级。目标：每项能力至少两名可独立操作的人员（2级及以上）。'),
    'training.matrix.workshops-settings': (
        'Ateliers', 'Workshops', 'Talleres', 'الورش', '作業場', '车间'),
    'training.matrix.add-operator': (
        'Opérateur', 'Operator', 'Operario', 'مشغّل', 'オペレーター', '操作员'),
    'training.matrix.add-skill': (
        'Compétence', 'Skill', 'Competencia', 'كفاءة', 'スキル', '能力'),
    'training.matrix.kpis': (
        'Indicateurs de la matrice', 'Matrix indicators', 'Indicadores de la matriz',
        'مؤشرات المصفوفة', 'マトリクスの指標', '矩阵指标'),
    'training.matrix.kpi-operators': (
        'Opérateurs', 'Operators', 'Operarios', 'المشغّلون', 'オペレーター', '操作员'),
    'training.matrix.kpi-skills': (
        'Compétences suivies', 'Tracked skills', 'Competencias seguidas',
        'الكفاءات المتابَعة', '追跡中のスキル', '跟踪的能力'),
    'training.matrix.kpi-at-risk': (
        'Compétences à risque', 'Skills at risk', 'Competencias en riesgo',
        'الكفاءات المعرّضة للخطر', 'リスクのあるスキル', '有风险的能力'),
    'training.matrix.kpi-with-trainer': (
        'Avec un formateur', 'With a trainer', 'Con un formador',
        'مع مدرّب', 'トレーナーあり', '有培训师'),
    'training.matrix.search': (
        'Rechercher un opérateur…', 'Search for an operator…', 'Buscar un operario…',
        'ابحث عن مشغّل…', 'オペレーターを検索…', '搜索操作员…'),
    'training.matrix.filter-workshop': (
        'Filtrer par atelier', 'Filter by workshop', 'Filtrar por taller',
        'التصفية حسب الورشة', '作業場で絞り込む', '按车间筛选'),
    'training.matrix.all-workshops': (
        'Tous les ateliers', 'All workshops', 'Todos los talleres',
        'كل الورش', 'すべての作業場', '所有车间'),
    'training.matrix.filter-category': (
        'Filtrer par catégorie', 'Filter by category', 'Filtrar por categoría',
        'التصفية حسب الفئة', 'カテゴリで絞り込む', '按类别筛选'),
    'training.matrix.all-categories': (
        'Toutes les catégories', 'All categories', 'Todas las categorías',
        'كل الفئات', 'すべてのカテゴリ', '所有类别'),
    'training.matrix.highlight-risks': (
        'Mettre en évidence les risques', 'Highlight risks', 'Resaltar los riesgos',
        'إبراز المخاطر', 'リスクを強調表示', '突出显示风险'),
    'training.matrix.level-0': (
        'Non formé', 'Not trained', 'No formado', 'غير مدرَّب', '未訓練', '未培训'),
    'training.matrix.level-1': (
        'Débutant', 'Beginner', 'Principiante', 'مبتدئ', '初心者', '初学者'),
    'training.matrix.level-2': (
        'Autonome', 'Autonomous', 'Autónomo', 'مستقل', '自立', '可独立操作'),
    'training.matrix.level-3': (
        'Expert', 'Expert', 'Experto', 'خبير', 'エキスパート', '专家'),
    'training.matrix.level-4': (
        'Formateur', 'Trainer', 'Formador', 'مدرّب', 'トレーナー', '培训师'),
    'training.matrix.risks-title': (
        'Compétences à risque', 'Skills at risk', 'Competencias en riesgo',
        'الكفاءات المعرّضة للخطر', 'リスクのあるスキル', '有风险的能力'),
    'training.matrix.sole-autonomous': (
        'Seule personne autonome : {$name}', 'Only autonomous person: {$name}',
        'Única persona autónoma: {$name}', 'الشخص المستقل الوحيد: {$name}',
        '自立している唯一の人：{$name}', '唯一可独立操作的人员：{$name}'),
    'training.matrix.no-autonomous': (
        'Aucune personne autonome', 'No autonomous person', 'Ninguna persona autónoma',
        'لا يوجد شخص مستقل', '自立している人がいません', '没有可独立操作的人员'),
    'training.matrix.trainer-of': (
        'Formateur : {$trainer}', 'Trainer: {$trainer}', 'Formador: {$trainer}',
        'المدرّب: {$trainer}', 'トレーナー：{$trainer}', '培训师：{$trainer}'),
    'training.matrix.no-risk': (
        'Aucun risque : toutes les compétences sont couvertes.',
        'No risk: every skill is covered.',
        'Ningún riesgo: todas las competencias están cubiertas.',
        'لا مخاطر: كل الكفاءات مغطّاة.',
        'リスクはありません。すべてのスキルがカバーされています。',
        '无风险：所有能力均已覆盖。'),
    'training.matrix.empty': (
        "La matrice est vide. Ajoutez des opérateurs depuis l'annuaire et les compétences à suivre.",
        'The matrix is empty. Add operators from the directory and the skills to track.',
        'La matriz está vacía. Añada operarios desde el directorio y las competencias que seguir.',
        'المصفوفة فارغة. أضف مشغّلين من الدليل والكفاءات المراد متابعتها.',
        'マトリクスは空です。ディレクトリからオペレーターを、そして追跡するスキルを追加してください。',
        '矩阵为空。请从通讯录添加操作员以及需要跟踪的能力。'),
    'training.matrix.operator': (
        'Opérateur', 'Operator', 'Operario', 'المشغّل', 'オペレーター', '操作员'),
    'training.matrix.average': (
        'Moy.', 'Avg.', 'Media', 'المتوسط', '平均', '平均'),
    'training.matrix.cell-label': (
        '{$operator} – {$skill} : {$level}', '{$operator} – {$skill}: {$level}',
        '{$operator} – {$skill}: {$level}', '{$operator} – {$skill}: {$level}',
        '{$operator} – {$skill}：{$level}', '{$operator} – {$skill}：{$level}'),
    'training.matrix.no-operator': (
        'Aucun opérateur dans la matrice pour le moment.', 'No operator in the matrix yet.',
        'Todavía no hay operarios en la matriz.', 'لا يوجد مشغّل في المصفوفة حتى الآن.',
        'マトリクスにはまだオペレーターがいません。', '矩阵中暂无操作员。'),
    'training.matrix.no-match': (
        'Aucun opérateur ne correspond aux filtres.', 'No operator matches the filters.',
        'Ningún operario coincide con los filtros.', 'لا يوجد مشغّل يطابق عوامل التصفية.',
        'フィルターに一致するオペレーターはいません。', '没有符合筛选条件的操作员。'),
    'training.matrix.reset-filters': (
        'Effacer les filtres', 'Clear filters', 'Borrar los filtros',
        'مسح عوامل التصفية', 'フィルターをクリア', '清除筛选'),
    'training.matrix.autonomous-footer': (
        'Personnes autonomes (2+)', 'Autonomous people (2+)', 'Personas autónomas (2+)',
        'الأشخاص المستقلون (2+)', '自立している人（2以上）', '可独立操作人员（2级及以上）'),
    'training.matrix.at-risk': (
        'à risque', 'at risk', 'en riesgo', 'معرّضة للخطر', 'リスクあり', '有风险'),
    'training.matrix.note': (
        "Chaque niveau posé ici est l'évaluation de compétence de la personne, enregistrée et "
        "tracée dans le journal d'audit.",
        "Each level set here is the person's competency assessment, saved and traced in the "
        'audit log.',
        'Cada nivel indicado aquí es la evaluación de competencia de la persona, registrada y '
        'trazada en el registro de auditoría.',
        'كل مستوى يُحدَّد هنا هو تقييم كفاءة الشخص، يُسجَّل ويُتتبَّع في سجل التدقيق.',
        'ここで設定する各レベルは、その人のスキル評価として保存され、監査ログに記録されます。',
        '此处设置的每个等级都是该人员的能力评估，会被保存并记录在审计日志中。'),
    'training.matrix.level-failed': (
        "Le niveau n'a pas pu être enregistré.", 'The level could not be saved.',
        'No se ha podido guardar el nivel.', 'تعذّر حفظ المستوى.',
        'レベルを保存できませんでした。', '无法保存该等级。'),
    'training.matrix.remove-operator': (
        'Retirer {$name} de la matrice', 'Remove {$name} from the matrix',
        'Quitar a {$name} de la matriz', 'إزالة {$name} من المصفوفة',
        '{$name} をマトリクスから外す', '将 {$name} 移出矩阵'),
    'training.matrix.remove-skill': (
        'Retirer la compétence {$name} de la matrice', 'Remove the skill {$name} from the matrix',
        'Quitar la competencia {$name} de la matriz', 'إزالة الكفاءة {$name} من المصفوفة',
        'スキル {$name} をマトリクスから外す', '将能力 {$name} 移出矩阵'),
    'training.matrix.remove-operator-title': (
        'Retirer {$name} de la matrice ?', 'Remove {$name} from the matrix?',
        '¿Quitar a {$name} de la matriz?', 'إزالة {$name} من المصفوفة؟',
        '{$name} をマトリクスから外しますか？', '要将 {$name} 移出矩阵吗？'),
    'training.matrix.remove-operator-message': (
        "La ligne quitte la matrice. Les évaluations de la personne restent à son dossier de "
        "formation, et reviennent si elle est réinscrite.",
        "The row leaves the matrix. The person's assessments stay in their training record, and "
        'come back if they are added again.',
        'La fila sale de la matriz. Las evaluaciones de la persona permanecen en su expediente de '
        'formación y vuelven si se la inscribe de nuevo.',
        'يخرج الصف من المصفوفة. تبقى تقييمات الشخص في ملف تدريبه، وتعود إذا أُعيد تسجيله.',
        '行はマトリクスから外れます。その人の評価は教育記録に残り、再登録すると戻ります。',
        '该行将移出矩阵。此人的评估仍保留在其培训档案中，重新加入后会恢复。'),
    'training.matrix.remove-skill-title': (
        'Retirer « {$name} » de la matrice ?', 'Remove “{$name}” from the matrix?',
        '¿Quitar «{$name}» de la matriz?', 'إزالة «{$name}» من المصفوفة؟',
        '「{$name}」をマトリクスから外しますか？', '要将“{$name}”移出矩阵吗？'),
    'training.matrix.remove-skill-message': (
        'La colonne quitte la matrice. La compétence reste au catalogue, avec ses évaluations.',
        'The column leaves the matrix. The skill stays in the catalogue, with its assessments.',
        'La columna sale de la matriz. La competencia permanece en el catálogo, con sus '
        'evaluaciones.',
        'يخرج العمود من المصفوفة. تبقى الكفاءة في الدليل مع تقييماتها.',
        '列はマトリクスから外れます。スキルは評価とともにカタログに残ります。',
        '该列将移出矩阵。该能力及其评估仍保留在目录中。'),
    'training.matrix.remove': (
        'Retirer', 'Remove', 'Quitar', 'إزالة', '外す', '移除'),
    'training.matrix.remove-failed': (
        "Le retrait n'a pas pu être enregistré.", 'The removal could not be saved.',
        'No se ha podido guardar la retirada.', 'تعذّر حفظ الإزالة.',
        '削除を保存できませんでした。', '无法保存移除操作。'),
    'training.matrix.add': (
        'Ajouter', 'Add', 'Añadir', 'إضافة', '追加', '添加'),
    'training.matrix.add-operator-title': (
        'Ajouter un opérateur', 'Add an operator', 'Añadir un operario',
        'إضافة مشغّل', 'オペレーターを追加', '添加操作员'),
    'training.matrix.add-operator-help': (
        "Choisissez la personne dans l'annuaire, puis son atelier. Ses niveaux partent de « Non "
        "formé », ou reprennent ses évaluations existantes.",
        'Pick the person from the directory, then their workshop. Their levels start at “Not '
        'trained”, or pick up their existing assessments.',
        'Elija a la persona en el directorio y luego su taller. Sus niveles parten de «No '
        'formado» o retoman sus evaluaciones existentes.',
        'اختر الشخص من الدليل ثم ورشته. تبدأ مستوياته من «غير مدرَّب» أو تستأنف تقييماته الحالية.',
        'ディレクトリから人を選び、次に作業場を選びます。レベルは「未訓練」から始まるか、既存の評価を引き継ぎます。',
        '从通讯录中选择人员，再选择其车间。其等级从“未培训”开始，或沿用已有评估。'),
    'training.matrix.workshop': (
        'Atelier', 'Workshop', 'Taller', 'الورشة', '作業場', '车间'),
    'training.matrix.operator-exists': (
        'Cette personne figure déjà dans la matrice.', 'This person is already in the matrix.',
        'Esta persona ya figura en la matriz.', 'هذا الشخص موجود بالفعل في المصفوفة.',
        'この人はすでにマトリクスにいます。', '此人已在矩阵中。'),
    'training.matrix.operator-failed': (
        "L'opérateur n'a pas pu être ajouté.", 'The operator could not be added.',
        'No se ha podido añadir el operario.', 'تعذّرت إضافة المشغّل.',
        'オペレーターを追加できませんでした。', '无法添加操作员。'),
    'training.matrix.add-skill-title': (
        'Ajouter une compétence', 'Add a skill', 'Añadir una competencia',
        'إضافة كفاءة', 'スキルを追加', '添加能力'),
    'training.matrix.add-skill-help': (
        'Une compétence déjà au catalogue sous ce nom est reprise avec ses évaluations. Le '
        'formateur est la personne qui transmettra la compétence quand elle passe à risque.',
        'A skill already in the catalogue under this name is reused with its assessments. The '
        'trainer is the person who will pass the skill on when it falls at risk.',
        'Una competencia que ya figura en el catálogo con este nombre se retoma con sus '
        'evaluaciones. El formador es la persona que transmitirá la competencia cuando esté en '
        'riesgo.',
        'تُستعاد الكفاءة الموجودة في الدليل بهذا الاسم مع تقييماتها. المدرّب هو من سينقل الكفاءة '
        'عندما تصبح معرّضة للخطر.',
        '同じ名前でカタログにあるスキルは、評価とともに引き継がれます。トレーナーは、スキルがリスク状態になったときに伝える人です。',
        '目录中已有同名能力时将连同其评估一并沿用。培训师是在该能力出现风险时负责传授的人。'),
    'training.matrix.skill-name': (
        'Compétence', 'Skill', 'Competencia', 'الكفاءة', 'スキル', '能力'),
    'training.matrix.skill-name-placeholder': (
        'Ex. : Soudure orbitale', 'E.g. Orbital welding', 'Ej.: Soldadura orbital',
        'مثال: اللحام المداري', '例：オービタル溶接', '例如：轨道焊接'),
    'training.matrix.category': (
        'Catégorie', 'Category', 'Categoría', 'الفئة', 'カテゴリ', '类别'),
    'training.matrix.trainer': (
        'Formateur', 'Trainer', 'Formador', 'المدرّب', 'トレーナー', '培训师'),
    'training.matrix.trainer-hint': (
        'Obligatoire : qui forme sur cette compétence ?',
        'Required: who trains on this skill?',
        'Obligatorio: ¿quién forma en esta competencia?',
        'إلزامي: من يدرّب على هذه الكفاءة؟',
        '必須：このスキルを教えるのは誰ですか？',
        '必填：谁负责此能力的培训？'),
    'training.matrix.skill-exists': (
        'Cette compétence figure déjà dans la matrice.', 'This skill is already in the matrix.',
        'Esta competencia ya figura en la matriz.', 'هذه الكفاءة موجودة بالفعل في المصفوفة.',
        'このスキルはすでにマトリクスにあります。', '该能力已在矩阵中。'),
    'training.matrix.skill-failed': (
        "La compétence n'a pas pu être ajoutée.", 'The skill could not be added.',
        'No se ha podido añadir la competencia.', 'تعذّرت إضافة الكفاءة.',
        'スキルを追加できませんでした。', '无法添加能力。'),
    'training.matrix.workshops-title': (
        'Ateliers de la matrice de compétences', 'Competency matrix workshops',
        'Talleres de la matriz de competencias', 'ورش مصفوفة الكفاءات',
        'スキルマトリクスの作業場', '能力矩阵的车间'),
    'training.matrix.workshops-subtitle': (
        'Les ateliers dans lesquels on range les opérateurs, et par lesquels on filtre la matrice.',
        'The workshops operators are assigned to, and by which the matrix is filtered.',
        'Los talleres en los que se clasifica a los operarios y por los que se filtra la matriz.',
        'الورش التي يُصنَّف فيها المشغّلون وتُصفّى بها المصفوفة.',
        'オペレーターを配属し、マトリクスを絞り込むための作業場です。',
        '用于归类操作员并筛选矩阵的车间。'),
    'training.matrix.workshops-load-failed': (
        "Les ateliers n'ont pas pu être chargés.", 'The workshops could not be loaded.',
        'No se han podido cargar los talleres.', 'تعذّر تحميل الورش.',
        '作業場を読み込めませんでした。', '无法加载车间。'),
    'training.matrix.workshops-scope': (
        'Retirer un atelier ne réaffecte personne : les opérateurs qui y sont rangés le gardent, '
        'et le filtre de la matrice le propose encore.',
        'Removing a workshop reassigns nobody: operators assigned to it keep it, and the matrix '
        'filter still offers it.',
        'Quitar un taller no reasigna a nadie: los operarios clasificados en él lo conservan y el '
        'filtro de la matriz lo sigue proponiendo.',
        'إزالة ورشة لا تعيد تعيين أحد: يحتفظ بها المشغّلون المصنَّفون فيها، ويظل مرشّح المصفوفة يقترحها.',
        '作業場を外しても誰も再配属されません。配属済みのオペレーターはそのまま残り、マトリクスのフィルターにも引き続き表示されます。',
        '移除车间不会重新分配任何人：已归入该车间的操作员保留该车间，矩阵筛选中也仍会显示。'),
    'training.matrix.workshops-list': (
        'Ateliers', 'Workshops', 'Talleres', 'الورش', '作業場', '车间'),
    'training.matrix.workshops-help': (
        "L'ordre de la liste est celui des menus et des lignes de la matrice.",
        'The list order is the order of the menus and of the matrix rows.',
        'El orden de la lista es el de los menús y de las filas de la matriz.',
        'ترتيب القائمة هو ترتيب القوائم وصفوف المصفوفة.',
        'リストの順序が、メニューとマトリクスの行の順序になります。',
        '列表顺序即菜单和矩阵行的顺序。'),
    'training.matrix.workshops-defaults': (
        "Ce sont aujourd'hui les ateliers proposés par la plateforme.",
        'These are currently the workshops provided by the platform.',
        'Son actualmente los talleres propuestos por la plataforma.',
        'هذه حاليًا الورش التي تقترحها المنصة.',
        '現在はプラットフォームが用意した作業場です。',
        '目前为平台提供的默认车间。'),
    'training.matrix.workshops-name': (
        "Nom de l'atelier", 'Workshop name', 'Nombre del taller', 'اسم الورشة', '作業場名', '车间名称'),
    'training.matrix.workshops-up': (
        'Monter', 'Move up', 'Subir', 'نقل لأعلى', '上へ移動', '上移'),
    'training.matrix.workshops-down': (
        'Descendre', 'Move down', 'Bajar', 'نقل لأسفل', '下へ移動', '下移'),
    'training.matrix.workshops-remove': (
        "Retirer l'atelier", 'Remove the workshop', 'Quitar el taller',
        'إزالة الورشة', '作業場を外す', '移除车间'),
    'training.matrix.workshops-new': (
        'Nouvel atelier', 'New workshop', 'Nuevo taller', 'ورشة جديدة', '新しい作業場', '新车间'),
    'training.matrix.workshops-pending': (
        'Ateliers modifiés', 'Workshops changed', 'Talleres modificados',
        'تم تعديل الورش', '作業場が変更されました', '车间已修改'),
    'training.matrix.workshops-problem-empty': (
        'Il faut au moins un atelier.', 'At least one workshop is required.',
        'Se necesita al menos un taller.', 'يلزم وجود ورشة واحدة على الأقل.',
        '作業場が少なくとも1つ必要です。', '至少需要一个车间。'),
    'training.matrix.workshops-problem-count': (
        '{$max} ateliers au plus.', 'At most {$max} workshops.', 'Como máximo {$max} talleres.',
        '{$max} ورشة على الأكثر.', '作業場は最大 {$max} 個です。', '最多 {$max} 个车间。'),
    'training.matrix.workshops-problem-length': (
        'Chaque atelier a un nom de 1 à {$max} caractères.',
        'Each workshop has a name of 1 to {$max} characters.',
        'Cada taller tiene un nombre de 1 a {$max} caracteres.',
        'لكل ورشة اسم من 1 إلى {$max} حرفًا.',
        '各作業場の名前は1〜{$max}文字です。',
        '每个车间名称为 1 到 {$max} 个字符。'),
    'training.matrix.workshops-problem-duplicate': (
        'Deux ateliers portent le même nom.', 'Two workshops have the same name.',
        'Dos talleres tienen el mismo nombre.', 'ورشتان تحملان الاسم نفسه.',
        '同じ名前の作業場が2つあります。', '有两个车间名称相同。'),
    'training.matrix.workshops-saved': (
        'Ateliers enregistrés.', 'Workshops saved.', 'Talleres guardados.',
        'تم حفظ الورش.', '作業場を保存しました。', '车间已保存。'),
    'training.matrix.workshops-save-failed': (
        "Les ateliers n'ont pas pu être enregistrés.", 'The workshops could not be saved.',
        'No se han podido guardar los talleres.', 'تعذّر حفظ الورش.',
        '作業場を保存できませんでした。', '无法保存车间。'),
    'authz.perm.training-manage': (
        'Tenir la matrice de compétences', 'Maintain the competency matrix',
        'Mantener la matriz de competencias', 'إدارة مصفوفة الكفاءات',
        'スキルマトリクスを管理する', '维护能力矩阵'),
    'authz.module.training': (
        'Formation', 'Training', 'Formación', 'التدريب', '教育訓練', '培训'),
    'training.matrix.legend': ('Niveaux', 'Levels', 'Niveles', 'المستويات', 'レベル', '等级'),
    'training.matrix.failed': (
        'Impossible de charger la matrice de compétences.', 'Could not load the competency matrix.',
        'No se ha podido cargar la matriz de competencias.', 'تعذّر تحميل مصفوفة الكفاءات.',
        'スキルマトリクスを読み込めませんでした。', '无法加载能力矩阵。'),
}
