# -*- coding: utf-8 -*-
"""Ce que la clôture d'un écart tranche de l'analyse de risque du produit.

Un écart traité enseigne quelque chose sur l'analyse des risques : un mode de
défaillance que le PFMEA n'avait pas prévu, un contrôle que le plan de
surveillance ne portait pas. L'IATF 16949 en fait une exigence — toute
non-conformité se répercute sur la PFMEA et le plan de surveillance — et c'est
l'écart qu'un auditeur cherche en premier.

Le vocabulaire suit celui des deux documents dans chaque langue : « PFMEA » est
l'acronyme d'usage partout, le plan de surveillance se dit « control plan » en
anglais et garde son nom traduit ailleurs.
"""

TRANSLATIONS = {

    # --- la fenêtre de clôture -------------------------------------------
    'nc.close.title': (
        "Clôturer l'écart",
        'Close the non-conformity',
        'Cerrar la desviación',
        'إغلاق الانحراف',
        '不適合をクローズする',
        '关闭不合格'),
    'nc.close.intro': (
        "L'écart {$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG} va être"
        " clos. Avant cela, deux questions : ce qu'il a enseigné à l'analyse de"
        " risque du produit.",
        'Non-conformity {$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG}'
        ' is about to be closed. First, two questions: what it taught the'
        " product's risk analysis.",
        'La desviación {$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG}'
        ' va a cerrarse. Antes, dos preguntas: qué ha enseñado al análisis de'
        ' riesgos del producto.',
        'الانحراف {$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG} على'
        ' وشك الإغلاق. وقبل ذلك سؤالان: ما الذي أفاده لتحليل مخاطر المنتج.',
        '不適合 {$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG} を'
        'クローズします。その前に二つの質問です。製品のリスク分析に対して'
        '何を教えたか。',
        '不合格 {$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG} 即将关闭。'
        '在此之前有两个问题：它对产品风险分析的启示。'),
    'nc.close.pfmea-legend': (
        'Le PFMEA du produit a-t-il été impacté ?',
        "Was the product's PFMEA affected?",
        '¿Se ha visto afectado el PFMEA del producto?',
        'هل تأثّر تحليل أنماط الإخفاق للمنتج؟',
        '製品の PFMEA に影響がありましたか。',
        '产品的 PFMEA 是否受到影响？'),
    'nc.close.control-plan-legend': (
        'Le plan de surveillance a-t-il été impacté ?',
        'Was the control plan affected?',
        '¿Se ha visto afectado el plan de control?',
        'هل تأثّرت خطة المراقبة؟',
        'コントロールプランに影響がありましたか。',
        '控制计划是否受到影响？'),
    'nc.close.pfmea-required': (
        'Répondez par « oui » ou « non » : la question doit être tranchée pour clore.',
        'Answer yes or no: the question must be settled before closing.',
        'Responda sí o no: la pregunta debe resolverse para poder cerrar.',
        'أجب بنعم أو لا: يجب حسم السؤال قبل الإغلاق.',
        '「はい」か「いいえ」で答えてください。クローズするにはこの問いを'
        '決める必要があります。',
        '请回答“是”或“否”：关闭前必须对该问题作出决定。'),
    'nc.close.control-plan-required': (
        'Répondez par « oui » ou « non » : la question doit être tranchée pour clore.',
        'Answer yes or no: the question must be settled before closing.',
        'Responda sí o no: la pregunta debe resolverse para poder cerrar.',
        'أجب بنعم أو لا: يجب حسم السؤال قبل الإغلاق.',
        '「はい」か「いいえ」で答えてください。クローズするにはこの問いを'
        '決める必要があります。',
        '请回答“是”或“否”：关闭前必须对该问题作出决定。'),
    'nc.close.requires-revision': (
        "La révision du document désigné devient exigible : l'écart ne se clôra"
        ' définitivement qu\'une fois cette révision acceptée.',
        'Revising the named document becomes mandatory: the non-conformity will'
        ' only close for good once that revision is accepted.',
        'La revisión del documento indicado pasa a ser obligatoria: la desviación'
        ' solo se cerrará definitivamente cuando se acepte esa revisión.',
        'تصبح مراجعة المستند المذكور إلزامية: لن يُغلق الانحراف نهائيًا إلا بعد'
        ' قبول تلك المراجعة.',
        '指定した文書の改訂が必須になります。その改訂が承認されるまで、'
        '不適合は最終的にクローズされません。',
        '指定文件的修订成为必须：只有该修订被接受后，不合格才会最终关闭。'),
    'nc.close.hint': (
        'Répondre « non » est une décision, qui se lit : elle atteste qu\'on a'
        " examiné l'analyse de risque et jugé qu'elle n'était pas en cause.",
        'Answering no is a decision, and it reads as one: it records that the risk'
        ' analysis was reviewed and judged not to be involved.',
        'Responder no es una decisión, y se lee como tal: acredita que se ha'
        ' examinado el análisis de riesgos y se ha juzgado que no estaba implicado.',
        'الإجابة بـ «لا» قرار يُقرأ: فهي تُثبت أن تحليل المخاطر قد فُحص ورُئي أنه'
        ' غير معنيّ.',
        '「いいえ」も一つの決定であり、そう読まれます。リスク分析を確認し、'
        '該当しないと判断したことの記録です。',
        '回答“否”也是一项决定，并会被如此解读：它证明已审查风险分析并判定其'
        '与此无关。'),
    'nc.close.confirm': (
        'Clôturer',
        'Close',
        'Cerrar',
        'إغلاق',
        'クローズ',
        '关闭'),

    # --- les deux motifs que le bandeau rédige ---------------------------
    'nc.blocker.pfmea-revision': (
        'Cet écart a été déclaré impactant le PFMEA du produit. Acceptez la'
        " révision du PFMEA qui le concerne pour pouvoir le clôturer — ou"
        " corrigez la réponse si le PFMEA n'était pas en cause.",
        "This non-conformity was declared as affecting the product's PFMEA."
        ' Accept the PFMEA revision raised for it before closing it — or correct'
        ' the answer if the PFMEA was not involved.',
        'Esta desviación se declaró con impacto en el PFMEA del producto. Acepte'
        ' la revisión del PFMEA correspondiente para poder cerrarla, o corrija la'
        ' respuesta si el PFMEA no estaba implicado.',
        'أُعلن أن هذا الانحراف يؤثّر على تحليل أنماط الإخفاق للمنتج. اقبل مراجعة'
        ' التحليل الخاصة به لتتمكّن من إغلاقه، أو صحّح الإجابة إذا لم يكن التحليل'
        ' معنيًّا.',
        'この不適合は製品の PFMEA に影響するものとして申告されました。'
        'クローズするには、該当する PFMEA の改訂を承認してください。'
        'PFMEA が該当しない場合は回答を修正してください。',
        '该不合格已被申报为影响产品的 PFMEA。请接受为其提出的 PFMEA 修订后'
        '再关闭，若 PFMEA 并不涉及，请更正答案。'),
    'nc.blocker.control-plan-revision': (
        'Cet écart a été déclaré impactant le plan de surveillance. Acceptez la'
        ' révision du plan qui le concerne pour pouvoir le clôturer — ou corrigez'
        " la réponse si le plan n'était pas en cause.",
        'This non-conformity was declared as affecting the control plan. Accept'
        ' the control plan revision raised for it before closing it — or correct'
        ' the answer if the plan was not involved.',
        'Esta desviación se declaró con impacto en el plan de control. Acepte la'
        ' revisión del plan correspondiente para poder cerrarla, o corrija la'
        ' respuesta si el plan no estaba implicado.',
        'أُعلن أن هذا الانحراف يؤثّر على خطة المراقبة. اقبل مراجعة الخطة الخاصة'
        ' به لتتمكّن من إغلاقه، أو صحّح الإجابة إذا لم تكن الخطة معنيّة.',
        'この不適合はコントロールプランに影響するものとして申告されました。'
        'クローズするには、該当するプランの改訂を承認してください。'
        'プランが該当しない場合は回答を修正してください。',
        '该不合格已被申报为影响控制计划。请接受为其提出的计划修订后再关闭，'
        '若该计划并不涉及，请更正答案。'),
}
