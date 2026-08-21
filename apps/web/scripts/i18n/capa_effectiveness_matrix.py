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
    'training.matrix.subtitle': (
        "Qui sait faire quoi. Une ligne dont une seule case est remplie signale une compétence "
        "qui ne tient qu'à une personne.",
        'Who can do what. A row with a single filled cell flags a skill held by one person only.',
        'Quién sabe hacer qué. Una fila con una sola casilla señala una competencia que depende '
        'de una sola persona.',
        'من يجيد ماذا. صف بخانة واحدة معبّأة يشير إلى كفاءة يملكها شخص واحد فقط.',
        '誰が何をできるか。1マスだけ埋まった行は、担い手が1人しかいないスキルを示します。',
        '谁会做什么。某行只有一格被填，说明该能力仅由一人掌握。'),
    'training.matrix.only-at-risk': (
        'Seulement les compétences à risque', 'Only skills at risk',
        'Solo las competencias en riesgo', 'الكفاءات المعرّضة للخطر فقط',
        'リスクのあるスキルのみ', '仅显示有风险的能力'),
    'training.matrix.col-skill': (
        'Compétence', 'Skill', 'Competencia', 'الكفاءة', 'スキル', '能力'),
    'training.matrix.ungrouped': (
        'Sans famille', 'Ungrouped', 'Sin familia', 'بدون عائلة', '未分類', '未分组'),
    'training.matrix.single-holder': (
        'Un seul détenteur', 'Single holder', 'Un solo titular',
        'حامل واحد فقط', '担い手は1人', '仅一名掌握者'),
    'training.matrix.unassessed': (
        'Non évalué', 'Not assessed', 'No evaluado', 'غير مُقيَّم', '未評価', '未评估'),
    'training.matrix.unassessed-legend': (
        'non évalué', 'not assessed', 'no evaluado', 'غير مُقيَّم', '未評価', '未评估'),
    'training.matrix.legend': ('Niveaux', 'Levels', 'Niveles', 'المستويات', 'レベル', '等级'),
    'training.matrix.none': (
        'Aucune compétence au catalogue. Créez-en, puis évaluez les collaborateurs : la matrice '
        'se construit à partir des évaluations.',
        'No skill in the catalogue. Create some, then assess people: the matrix is built from '
        'the assessments.',
        'Ninguna competencia en el catálogo. Cree algunas y evalúe a las personas: la matriz se '
        'construye a partir de las evaluaciones.',
        'لا توجد كفاءات في الدليل. أنشئ بعضها ثم قيّم الأشخاص: تُبنى المصفوفة من التقييمات.',
        'カタログにスキルがありません。作成してから評価してください。マトリクスは評価から作られます。',
        '目录中暂无能力项。请先创建，再评估人员：矩阵由评估结果构建。'),
    'training.matrix.no-people': (
        "Le catalogue existe, mais personne n'a encore été évalué. Les colonnes sont les "
        "personnes évaluées — la matrice reste vide tant qu'aucune ne l'est.",
        'The catalogue exists, but nobody has been assessed yet. Columns are the assessed '
        'people — the matrix stays empty until someone is.',
        'El catálogo existe, pero nadie ha sido evaluado todavía. Las columnas son las personas '
        'evaluadas: la matriz permanece vacía hasta que haya alguna.',
        'الدليل موجود، لكن لم يُقيَّم أحد بعد. الأعمدة هي الأشخاص المقيَّمون، وتبقى المصفوفة فارغة حتى ذلك.',
        'カタログはありますが、まだ誰も評価されていません。列は評価済みの人です。',
        '目录已存在，但尚无人被评估。列即为已评估的人员。'),
    'training.matrix.failed': (
        'Impossible de charger la matrice de compétences.', 'Could not load the competency matrix.',
        'No se ha podido cargar la matriz de competencias.', 'تعذّر تحميل مصفوفة الكفاءات.',
        'スキルマトリクスを読み込めませんでした。', '无法加载能力矩阵。'),
}
