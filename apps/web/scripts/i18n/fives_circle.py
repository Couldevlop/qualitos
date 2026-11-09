# -*- coding: utf-8 -*-
"""Un audit 5S alimente un Cercle de Qualité (§3.6).

L'audit relève ce qui ne va pas, pilier par pilier, zone par zone. Il ne dit
pas comment y remédier — et le cercle de qualité est fait précisément pour ça.
Les deux modules existaient, sans porte de l'un vers l'autre : un pilier noté
2/10 restait une note dans un rapport.
id: (fr, en, es, ar, ja, zh).
"""

TRANSLATIONS = {

    # --- la fenêtre ---------------------------------------------------------
    'fives.circle.title': (
        'Porter au cercle de qualité', 'Take to a quality circle',
        'Llevar al círculo de calidad', 'الإحالة إلى حلقة الجودة',
        '品質サークルへ提起', '提交品质圈'),
    'fives.circle.subtitle': (
        "L'audit dit ce qui ne va pas. Le cercle cherche comment y remédier.",
        'The audit says what is wrong. The circle looks for how to fix it.',
        'La auditoría dice qué falla. El círculo busca cómo remediarlo.',
        'يقول التدقيق ما الخلل. والحلقة تبحث في كيفية معالجته.',
        '監査は何が悪いかを示します。サークルはその直し方を探します。',
        '审核指出问题所在，品质圈寻找改进办法。'),
    'fives.circle.submit': (
        "Déposer l'idée", 'Submit the idea', 'Presentar la idea',
        'تقديم الفكرة', '提案を登録', '提交想法'),

    # --- ce que l'idée reprend du relevé ------------------------------------
    'fives.circle.zone': (
        'Zone auditée', 'Audited zone', 'Zona auditada',
        'المنطقة المدقّقة', '監査対象エリア', '受审区域'),
    'fives.circle.pillar': (
        'Pilier', 'Pillar', 'Pilar', 'الركن', '柱', '支柱'),
    'fives.circle.score': (
        'Note relevée', 'Score recorded', 'Puntuación registrada',
        'الدرجة المسجّلة', '記録された点数', '记录的评分'),

    # --- le cercle qui prend le sujet ---------------------------------------
    'fives.circle.circle': (
        'Cercle qui prend le sujet', 'Circle taking the subject',
        'Círculo que asume el tema', 'الحلقة التي تتولّى الموضوع',
        '担当するサークル', '承接该议题的品质圈'),
    'fives.circle.circle-hint': (
        'Seuls les cercles actifs peuvent prendre une idée.',
        'Only active circles can take an idea.',
        'Solo los círculos activos pueden asumir una idea.',
        'الحلقات النشطة وحدها يمكنها تولّي فكرة.',
        '活動中のサークルのみが提案を受け取れます。',
        '仅活跃的品质圈可承接想法。'),
    'fives.circle.loading': (
        'Chargement des cercles…', 'Loading circles…', 'Cargando los círculos…',
        'جارٍ تحميل الحلقات…', 'サークルを読み込み中…', '正在加载品质圈…'),
    'fives.circle.load-failed': (
        "La liste des cercles n'a pas pu être chargée.",
        'The list of circles could not be loaded.',
        'No se pudo cargar la lista de círculos.',
        'لم يتمكّن النظام من تحميل قائمة الحلقات.',
        'サークルの一覧を読み込めませんでした。', '未能加载品质圈列表。'),
    'fives.circle.none': (
        "Aucun cercle actif. Créez-en un, ou reprenez un cercle en pause, pour y porter ce sujet.",
        'No active circle. Create one, or resume a paused circle, to take this subject there.',
        'Ningún círculo activo. Cree uno, o reactive un círculo en pausa, para llevar este tema.',
        'لا توجد حلقة نشطة. أنشئ واحدة أو استأنف حلقة موقوفة لإحالة هذا الموضوع.',
        '活動中のサークルがありません。新規に作成するか、停止中のサークルを再開してください。',
        '暂无活跃的品质圈。请新建一个，或恢复已暂停的品质圈，再提交该议题。'),

    # --- ce qui est proposé -------------------------------------------------
    'fives.circle.idea': (
        'Ce qui est proposé', 'What is proposed', 'Lo que se propone',
        'المقترَح', '提案の内容', '所提议的内容'),
    'fives.circle.idea-hint': (
        "Une ligne, lisible à l'ordre du jour.",
        'One line, readable on the agenda.',
        'Una línea, legible en el orden del día.',
        'سطر واحد يُقرأ في جدول الأعمال.',
        '議題に載せて読める一行で。', '一行即可，便于列入议程。'),
    'fives.circle.title-default': (
        'Améliorer {$pillar} en zone {$zone}',
        'Improve {$pillar} in zone {$zone}',
        'Mejorar {$pillar} en la zona {$zone}',
        'تحسين {$pillar} في منطقة {$zone}',
        'ゾーン{$zone}の{$pillar}を改善',
        '改善 {$zone} 区域的 {$pillar}'),
    'fives.circle.detail': (
        "Ce que l'audit a constaté", 'What the audit found',
        'Lo que constató la auditoría', 'ما لاحظه التدقيق',
        '監査が確認した内容', '审核的发现'),
    'fives.circle.detail-hint': (
        "Reprise de la remarque de l'auditeur. Le cercle n'était pas sur place.",
        "Taken from the auditor's remark. The circle was not there.",
        'Tomado de la observación del auditor. El círculo no estaba allí.',
        'مأخوذ من ملاحظة المدقّق. لم تكن الحلقة حاضرة.',
        '監査員の所見から取りました。サークルは現場にいませんでした。',
        '取自审核员的备注。品质圈当时不在现场。'),

    # --- ce que l'écran dit après coup --------------------------------------
    'fives.circle.done': (
        'Idée déposée au cercle. Elle y arrive avec la note relevée.',
        'Idea submitted to the circle. It arrives there with the recorded score.',
        'Idea presentada al círculo. Llega con la puntuación registrada.',
        'تم تقديم الفكرة إلى الحلقة، وتصل مرفقة بالدرجة المسجّلة.',
        '提案をサークルに登録しました。記録された点数とともに届きます。',
        '想法已提交品质圈，并附带所记录的评分。'),
    'fives.circle.failed': (
        "L'idée n'a pas pu être déposée.", 'The idea could not be submitted.',
        'La idea no pudo presentarse.', 'لم يتمكّن النظام من تقديم الفكرة.',
        '提案を登録できませんでした。', '未能提交该想法。'),

    # --- la fiche de l'audit ------------------------------------------------
    'fives.detail.to-circle': (
        'Porter au cercle', 'Take to a circle', 'Llevar al círculo',
        'الإحالة إلى حلقة', 'サークルへ提起', '提交品质圈'),
    'fives.detail.to-circle-tooltip': (
        'Porter ce pilier à un cercle de qualité, avec sa note',
        'Take this pillar to a quality circle, with its score',
        'Llevar este pilar a un círculo de calidad, con su puntuación',
        'إحالة هذا الركن إلى حلقة جودة، مع درجته',
        'この柱を点数とともに品質サークルへ提起する',
        '将该支柱连同评分提交品质圈'),
}
