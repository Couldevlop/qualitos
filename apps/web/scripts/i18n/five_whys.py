# -*- coding: utf-8 -*-
"""Table i18n - analyse des 5 Pourquoi (SS3.5). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.cinq-pourquoi': ('5 Pourquoi', '5 Whys', '5 Porqués', 'الأسباب الخمسة', 'なぜなぜ分析', '五问法'),

    'fivewhys.list-title': ('5 Pourquoi', '5 Whys', '5 Porqués', 'الأسباب الخمسة', 'なぜなぜ分析', '五问法'),
    'fivewhys.list-subtitle': (
        "Remonter d'un écart constaté à sa cause racine, un pourquoi après l'autre.",
        'Trace a observed gap back to its root cause, one why at a time.',
        'Remontar de una desviación observada a su causa raíz, un porqué tras otro.',
        'التتبّع من انحراف ملاحَظ إلى سببه الجذري، سؤالاً بعد سؤال.',
        '観察された逸脱から根本原因へ、「なぜ」を一つずつ遡ります。',
        '从观察到的偏差追溯到根本原因，一次一个「为什么」。'),
    'fivewhys.list-empty': (
        "Aucune analyse pour l'instant. Une analyse se lance depuis une non-conformité.",
        'No analysis yet. An analysis starts from a nonconformity.',
        'Aún no hay análisis. Un análisis comienza desde una no conformidad.',
        'لا يوجد تحليل بعد. يبدأ التحليل من حالة عدم مطابقة.',
        'まだ分析はありません。分析は不適合から始めます。',
        '暂无分析。分析从一条不合格记录开始。'),
    'fivewhys.list-failed': (
        'Impossible de charger les analyses.', 'Could not load the analyses.',
        'No se pudieron cargar los análisis.', 'تعذّر تحميل التحليلات.',
        '分析を読み込めませんでした。', '无法加载分析。'),
    'fivewhys.col-nc': ('Non-conformité', 'Nonconformity', 'No conformidad', 'عدم المطابقة', '不適合', '不合格'),
    'fivewhys.col-problem': ('Problème', 'Problem', 'Problema', 'المشكلة', '問題', '问题'),
    'fivewhys.col-depth': ('Pourquoi', 'Whys', 'Porqués', 'الأسباب', 'なぜ', '为什么'),
    'fivewhys.col-root': ('Cause racine', 'Root cause', 'Causa raíz', 'السبب الجذري', '根本原因', '根本原因'),
    'fivewhys.concluded': ('Conclue', 'Concluded', 'Concluida', 'مُستنتَج', '結論あり', '已得出'),
    'fivewhys.in-progress': ('En cours', 'In progress', 'En curso', 'قيد التنفيذ', '進行中', '进行中'),

    'fivewhys.eyebrow': ('5 Pourquoi', '5 Whys', '5 Porqués', 'الأسباب الخمسة', 'なぜなぜ分析', '五问法'),
    'fivewhys.why': ('Pourquoi ?', 'Why?', '¿Por qué?', 'لماذا؟', 'なぜ？', '为什么？'),
    'fivewhys.next-question': ('Pourquoi ?', 'Why?', '¿Por qué?', 'لماذا؟', 'なぜ？', '为什么？'),
    'fivewhys.answer-label': ('Parce que…', 'Because…', 'Porque…', 'لأنّ…', 'なぜなら…', '因为……'),
    'fivewhys.answer-aria': ('Réponse au pourquoi', 'Answer to the why', 'Respuesta al porqué', 'الإجابة على السؤال', 'なぜへの回答', '对该问题的回答'),
    'fivewhys.add': ('Ajouter ce pourquoi', 'Add this why', 'Añadir este porqué', 'إضافة هذا السبب', 'この「なぜ」を追加', '添加此问'),
    'fivewhys.edit-tooltip': ('Corriger cette réponse', 'Edit this answer', 'Corregir esta respuesta', 'تصحيح هذه الإجابة', 'この回答を修正', '修改此回答'),
    'fivewhys.remove-tooltip': ('Retirer ce pourquoi', 'Remove this why', 'Quitar este porqué', 'إزالة هذا السبب', 'この「なぜ」を削除', '移除此问'),
    'fivewhys.chain-full': (
        "Sept pourquoi, c'est la limite : au-delà, on n'remonte plus une cause, on énumère des circonstances. Concluez la cause racine, ou reformulez le problème.",
        'Seven whys is the limit: beyond that you no longer trace a cause, you list circumstances. Conclude the root cause, or restate the problem.',
        'Siete porqués es el límite: más allá ya no se remonta una causa, se enumeran circunstancias. Concluya la causa raíz o reformule el problema.',
        'سبعة أسباب هي الحد: بعدها لم تعد تتتبّع سبباً بل تعدّد ظروفاً. استنتج السبب الجذري أو أعد صياغة المشكلة.',
        '「なぜ」は7つが限界です。それ以上は原因を遡るのではなく、状況を並べているだけです。根本原因を結論づけるか、問題を言い直してください。',
        '七问是上限：再往下已不是追溯原因，而是罗列情形。请给出根本原因，或重新表述问题。'),
    'fivewhys.root-cause-title': ('Cause racine', 'Root cause', 'Causa raíz', 'السبب الجذري', '根本原因', '根本原因'),
    'fivewhys.root-cause-label': ('Cause racine identifiée', 'Identified root cause', 'Causa raíz identificada', 'السبب الجذري المُحدَّد', '特定した根本原因', '已识别的根本原因'),
    'fivewhys.root-cause-locked': (
        "Conclure avant trois pourquoi, c'est nommer un symptôme. Poursuivez la chaîne.",
        'Concluding before three whys names a symptom. Keep going.',
        'Concluir antes de tres porqués es nombrar un síntoma. Continúe la cadena.',
        'الاستنتاج قبل ثلاثة أسباب هو تسمية عَرَض. واصل السلسلة.',
        '3つ未満で結論づけるのは症状に名前を付けるだけです。連鎖を続けてください。',
        '不足三问就下结论，只是给症状命名。请继续追问。'),
    'fivewhys.conclude': ('Conclure', 'Conclude', 'Concluir', 'استنتاج', '結論づける', '得出结论'),
    'fivewhys.load-failed': ('Analyse introuvable.', 'Analysis not found.', 'Análisis no encontrado.', 'لم يُعثر على التحليل.', '分析が見つかりません。', '未找到分析。'),
    'fivewhys.add-failed': ('Ajout du pourquoi refusé.', 'Adding the why was refused.', 'Se rechazó añadir el porqué.', 'تم رفض إضافة السبب.', '「なぜ」の追加は拒否されました。', '添加此问被拒绝。'),
    'fivewhys.update-failed': ('Modification refusée.', 'Change refused.', 'Modificación rechazada.', 'تم رفض التعديل.', '変更は拒否されました。', '修改被拒绝。'),
    'fivewhys.delete-step-failed': ('Suppression refusée.', 'Deletion refused.', 'Eliminación rechazada.', 'تم رفض الحذف.', '削除は拒否されました。', '删除被拒绝。'),
    'fivewhys.root-cause-failed': ('Conclusion refusée.', 'Conclusion refused.', 'Conclusión rechazada.', 'تم رفض الاستنتاج.', '結論は拒否されました。', '结论被拒绝。'),
    'fivewhys.answer-required': (
        'La réponse au pourquoi est obligatoire.', 'The answer to the why is required.',
        'La respuesta al porqué es obligatoria.', 'الإجابة على السؤال إلزامية.',
        '「なぜ」への回答は必須です。', '必须回答该问题。'),

    'fivewhys.open-nc': (
        'Voir la non-conformité', 'View the nonconformity', 'Ver la no conformidad',
        'عرض حالة عدم المطابقة', '不適合を表示', '查看不合格记录'),

    # Entrée depuis la fiche de non-conformité : la méthode part d'un écart
    # constaté, c'est donc de là qu'on la déroule.
    'nc.detail.ishikawa': (
        'Ishikawa', 'Ishikawa', 'Ishikawa', 'إيشيكاوا', '特性要因図', '鱼骨图'),
    'nc.detail.ishikawa-tooltip': (
        'Chercher les causes par familles, à partir de cet écart',
        'Search causes by family, starting from this finding',
        'Buscar las causas por familias, a partir de esta desviación',
        'البحث عن الأسباب حسب العائلات، انطلاقاً من هذا الانحراف',
        'この逸脱を起点に、要因を系統別に洗い出す',
        '以此偏差为起点，按类别查找原因'),
    'nc.detail.ishikawa-error': (
        "Impossible d'ouvrir le diagramme d'Ishikawa.",
        'Cannot open the Ishikawa diagram.',
        'No se puede abrir el diagrama de Ishikawa.',
        'تعذّر فتح مخطط إيشيكاوا.',
        '特性要因図を開けませんでした。',
        '无法打开鱼骨图。'),
    'nc.detail.five-whys': ('5 Pourquoi', '5 Whys', '5 Porqués', 'الأسباب الخمسة', 'なぜなぜ分析', '五问法'),
    'nc.detail.five-whys-tooltip': (
        "Remonter à la cause racine, un pourquoi après l'autre",
        'Trace the root cause, one why at a time',
        'Remontar a la causa raíz, un porqué tras otro',
        'التتبّع إلى السبب الجذري، سؤالاً بعد سؤال',
        '「なぜ」を一つずつ遡って根本原因へ',
        '一次一个「为什么」，追溯根本原因'),
    'nc.detail.five-whys-error': (
        "Impossible d'ouvrir l'analyse des 5 Pourquoi.",
        'Could not open the 5 Whys analysis.',
        'No se pudo abrir el análisis de los 5 Porqués.',
        'تعذّر فتح تحليل الأسباب الخمسة.',
        'なぜなぜ分析を開けませんでした。',
        '无法打开五问法分析。'),
}
