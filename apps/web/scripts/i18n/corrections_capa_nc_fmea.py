# -*- coding: utf-8 -*-
"""Table i18n - lot de corrections CAPA / NC / FMEA.

Trois sujets, un seul lot de retours terrain :

 * CAPA : le dossier peut desormais etre ouvert en ENDIGUEMENT, et des pieces
   jointes se deposent des l'ouverture (avant, il fallait creer le dossier puis
   revenir chercher la photo du defaut).
 * NC : la liste nomme enfin l'auteur du signalement.
 * FMEA : le referentiel de cotation (baremes S/O/D et exemple de PFMEA)
   s'ouvre depuis l'ecran, sans aller chercher un classeur ailleurs.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- CAPA : raison d'ouverture du dossier --------------------------------
    'capa.type.containment': (
        'Endiguement', 'Containment', 'Contención',
        'احتواء', '暫定対応', '围堵'),
    'capa.type.corrective': (
        'Corrective', 'Corrective', 'Correctiva',
        'تصحيحي', '是正', '纠正'),
    'capa.type.preventive': (
        'Préventive', 'Preventive', 'Preventiva',
        'وقائي', '予防', '预防'),

    # --- CAPA : pieces jointes deposees des l'ouverture ----------------------
    'capa.create.attachments': (
        'Pièces jointes (optionnel)', 'Attachments (optional)',
        'Archivos adjuntos (opcional)', 'المرفقات (اختياري)',
        '添付ファイル（任意）', '附件（可选）'),
    'capa.create.attachments-hint': (
        "Ce qui documente l'écart dès l'ouverture : photo, relevé, courriel. "
        'PDF, image, Word ou Excel, 10 Mo par pièce.',
        'Whatever documents the gap from the outset: photo, reading, email. '
        'PDF, image, Word or Excel, 10 MB per file.',
        'Lo que documenta la desviación desde el inicio: foto, medición, correo. '
        'PDF, imagen, Word o Excel, 10 MB por archivo.',
        'ما يوثّق الانحراف منذ الفتح: صورة، قياس، بريد إلكتروني. '
        'PDF أو صورة أو Word أو Excel، ‏10 ميغابايت لكل ملف.',
        '起票時点で逸脱を裏づけるもの（写真、測定値、メール）。'
        'PDF・画像・Word・Excel、1件あたり10MBまで。',
        '开立时即可佐证偏差的材料：照片、记录、邮件。'
        'PDF、图片、Word 或 Excel，每份 10 MB。'),
    'capa.create.attachment-add': (
        'Ajouter un fichier', 'Add a file', 'Añadir un archivo',
        'إضافة ملف', 'ファイルを追加', '添加文件'),
    'capa.create.attachment-remove-tooltip': (
        'Retirer ce fichier', 'Remove this file', 'Quitar este archivo',
        'إزالة هذا الملف', 'このファイルを削除', '移除该文件'),
    'capa.create.attachment-progress': (
        'Dépôt des pièces', 'Uploading files', 'Subiendo archivos',
        'جارٍ رفع الملفات', 'ファイルを送信中', '正在上传附件'),
    'capa.create.evidence-limit': (
        'Dix pièces au maximum par dossier.', 'Ten files at most per case.',
        'Diez archivos como máximo por caso.',
        'عشرة ملفات كحد أقصى لكل ملف تصحيحي.',
        '1件あたり添付は10件までです。', '每个案卷最多十份附件。'),
    'capa.create.success-with-evidence': (
        'Cas CAPA créé, pièces jointes déposées.',
        'CAPA case created, attachments uploaded.',
        'Caso CAPA creado, archivos adjuntos subidos.',
        'تم إنشاء ملف CAPA ورفع المرفقات.',
        'CAPA案件を作成し、添付ファイルを登録しました。',
        'CAPA 案卷已创建，附件已上传。'),
    'capa.create.success-partial-evidence': (
        "Cas CAPA créé, mais des pièces n'ont pas pu être déposées — "
        'reprends-les depuis la fiche.',
        'CAPA case created, but some files could not be uploaded — '
        'add them again from the case.',
        'Caso CAPA creado, pero algunos archivos no se pudieron subir: '
        'vuelve a añadirlos desde la ficha.',
        'تم إنشاء ملف CAPA، لكن تعذّر رفع بعض المرفقات — أعد إضافتها من البطاقة.',
        'CAPA案件は作成されましたが、一部の添付ファイルを送信できませんでした。'
        '案件画面から再度添付してください。',
        'CAPA 案卷已创建，但部分附件未能上传 — 请在案卷页面重新添加。'),

    # --- NC : qui a vu l'ecart ----------------------------------------------
    'nc.list.col-reporter': (
        'Détecté par', 'Reported by', 'Detectado por',
        'اكتشفه', '発見者', '发现人'),

    # --- chrome partage -------------------------------------------------------
    # Le libelle du bouton « Fermer » existe deja ; l'etiquette vocale de la
    # croix a besoin de son propre id, l'attribut n'etant pas le contenu.
    'common.close-aria': (
        'Fermer', 'Close', 'Cerrar', 'إغلاق', '閉じる', '关闭'),

    # --- referentiel de cotation propre au tenant ----------------------------
    # « Perturbation majeure du service » ne recouvre pas la meme realite dans
    # un atelier de sertissage et dans un centre d'appels : le bareme appartient
    # a l'organisation, et l'ecran doit dire lequel est en vigueur.
    'fmea.reference.custom-badge': (
        "Barème de l'organisation", 'Organisation scale', 'Escala de la organización',
        'مقياس المؤسسة', '自社の評価基準', '本组织标尺'),
    'fmea.reference.default-badge': (
        'Barème de référence', 'Reference scale', 'Escala de referencia',
        'المقياس المرجعي', '標準の評価基準', '参考标尺'),
    'fmea.reference.updated': (
        'Modifié le', 'Updated on', 'Modificado el',
        'عُدّل في', '更新日', '修改于'),
    'fmea.reference.edit': (
        'Adapter ce barème', 'Adapt this scale', 'Adaptar esta escala',
        'تكييف هذا المقياس', 'この基準を調整', '调整该标尺'),
    'fmea.reference.revert': (
        'Rétablir la référence', 'Restore the reference', 'Restaurar la referencia',
        'استعادة المرجع', '標準に戻す', '恢复参考标尺'),
    'fmea.reference.incomplete': (
        'Chaque score doit porter un intitulé. Manquent les scores :',
        'Every score needs a label. Missing scores:',
        'Cada puntuación necesita un título. Faltan las puntuaciones:',
        'كل درجة تحتاج إلى عنوان. الدرجات الناقصة:',
        'すべての点数に見出しが必要です。未記入の点数：',
        '每个分值都需要名称。缺少的分值：'),
    'fmea.reference.edit-hint': (
        "Le barème adopté ici sert à toute l'organisation. Les cotations déjà "
        "enregistrées ne changent pas de valeur, mais elles ont été portées sur "
        "l'échelle précédente : ne le modifiez pas au milieu d'une analyse.",
        'The scale adopted here applies to the whole organisation. Ratings already '
        'recorded keep their value, but they were given on the previous scale: '
        'do not change it in the middle of an analysis.',
        'La escala adoptada aquí se aplica a toda la organización. Las puntuaciones '
        'ya registradas conservan su valor, pero se dieron con la escala anterior: '
        'no la cambie en mitad de un análisis.',
        'المقياس المعتمد هنا يسري على المؤسسة كلها. التقييمات المسجَّلة سابقًا تحتفظ '
        'بقيمتها، لكنها أُعطيت وفق المقياس السابق: لا تغيّره في منتصف تحليل جارٍ.',
        'ここで採用した基準は組織全体に適用されます。記録済みの評点は値を変えませんが、'
        '前の基準で付けられたものです。分析の途中で変更しないでください。',
        '此处采用的标尺适用于整个组织。已记录的评分数值不变，但它们是按旧标尺给出的：'
        '请勿在分析进行中修改。'),
    'fmea.reference.saved': (
        'Barème enregistré.', 'Scale saved.', 'Escala guardada.',
        'تم حفظ المقياس.', '評価基準を保存しました。', '标尺已保存。'),
    'fmea.reference.reverted': (
        'Barème de référence rétabli.', 'Reference scale restored.',
        'Escala de referencia restaurada.', 'تمت استعادة المقياس المرجعي.',
        '標準の評価基準に戻しました。', '已恢复参考标尺。'),
    'fmea.reference.save-failed': (
        'Barème refusé.', 'Scale rejected.', 'Escala rechazada.',
        'تم رفض المقياس.', '評価基準が拒否されました。', '标尺被拒绝。'),
    'fmea.reference.load-failed': (
        'Référentiel de cotation indisponible.', 'Rating reference unavailable.',
        'Referencia de puntuación no disponible.', 'مرجع التقييم غير متاح.',
        '評価基準を読み込めません。', '评分基准不可用。'),

    # --- FMEA : referentiel de cotation --------------------------------------
    'fmea.reference.open': (
        'Référentiel de cotation', 'Rating reference', 'Referencia de puntuación',
        'مرجع التقييم', '評価基準', '评分基准'),
    'fmea.reference.title': (
        'Référentiel de cotation FMEA', 'FMEA rating reference',
        'Referencia de puntuación FMEA', 'مرجع تقييم FMEA',
        'FMEA 評価基準', 'FMEA 评分基准'),
    'fmea.reference.subtitle': (
        'Les échelles sur lesquelles se cotent Sévérité, Occurrence et Détection. '
        "Deux RPN ne se comparent que s'ils sortent du même barème.",
        'The scales on which Severity, Occurrence and Detection are rated. '
        'Two RPNs compare only if they come from the same scale.',
        'Las escalas con las que se puntúan Severidad, Ocurrencia y Detección. '
        'Dos RPN solo se comparan si proceden de la misma escala.',
        'المقاييس التي تُقيَّم بها الخطورة والتكرار والاكتشاف. '
        'لا يُقارَن رقمان للأولوية إلا إذا صدرا عن المقياس نفسه.',
        '厳しさ・発生頻度・検出度を評価する尺度です。'
        'RPNは同じ尺度で算出したもの同士でしか比較できません。',
        '严重度、发生频度与探测度所依据的评分标尺。'
        '只有出自同一标尺的 RPN 才具可比性。'),
    'fmea.reference.tab-severity': (
        'Sévérité', 'Severity', 'Severidad', 'الخطورة', '厳しさ', '严重度'),
    'fmea.reference.tab-occurrence': (
        'Occurrence', 'Occurrence', 'Ocurrencia', 'التكرار', '発生頻度', '发生频度'),
    'fmea.reference.tab-detection': (
        'Détection', 'Detection', 'Detección', 'الاكتشاف', '検出度', '探测度'),
    'fmea.reference.tab-example': (
        'Exemple de PFMEA', 'PFMEA example', 'Ejemplo de PFMEA',
        'مثال على PFMEA', 'PFMEAの例', 'PFMEA 示例'),
    'fmea.reference.example-hint': (
        'Modèle de rédaction : le niveau de détail attendu dans un mode de '
        'défaillance, un effet et une action recommandée.',
        'A writing model: the level of detail expected in a failure mode, '
        'an effect and a recommended action.',
        'Modelo de redacción: el nivel de detalle esperado en un modo de fallo, '
        'un efecto y una acción recomendada.',
        'نموذج للصياغة: مستوى التفصيل المتوقَّع في نمط الفشل والأثر والإجراء الموصى به.',
        '記載例：故障モード・影響・推奨処置に求められる詳しさの目安です。',
        '撰写范例：失效模式、影响与建议措施应有的详细程度。'),
    'fmea.reference.col-effect': (
        'Effet', 'Effect', 'Efecto', 'الأثر', '影響', '影响'),
    'fmea.reference.col-effects': (
        'Effets', 'Effects', 'Efectos', 'الآثار', '影響', '影响'),
    'fmea.reference.col-description': (
        'Description', 'Description', 'Descripción', 'الوصف', '説明', '说明'),
    'fmea.reference.col-score': (
        'Score', 'Score', 'Puntuación', 'الدرجة', '点数', '分值'),
    'fmea.reference.col-probability': (
        'Probabilité', 'Probability', 'Probabilidad', 'الاحتمال', '発生確率', '概率'),
    'fmea.reference.col-period': (
        'Période', 'Time period', 'Periodo', 'الفترة', '期間', '周期'),
    'fmea.reference.col-rate': (
        'Taux de défaillance', 'Failure rate', 'Tasa de fallos',
        'معدل الفشل', '故障率', '失效率'),
    'fmea.reference.col-chance': (
        'Chance de détection', 'Chance of detection', 'Posibilidad de detección',
        'فرصة الاكتشاف', '検出の可能性', '探测可能性'),
    'fmea.reference.col-step': (
        'Étape du processus', 'Process step', 'Etapa del proceso',
        'خطوة العملية', '工程ステップ', '过程步骤'),
    'fmea.reference.col-failure-mode': (
        'Mode de défaillance', 'Failure mode', 'Modo de fallo',
        'نمط الفشل', '故障モード', '失效模式'),
    'fmea.reference.col-causes': (
        'Causes', 'Causes', 'Causas', 'الأسباب', '原因', '原因'),
    'fmea.reference.col-controls': (
        'Contrôles actuels', 'Current controls', 'Controles actuales',
        'الضوابط الحالية', '現行の管理', '现行控制'),
    'fmea.reference.col-action': (
        'Action recommandée', 'Recommended action', 'Acción recomendada',
        'الإجراء الموصى به', '推奨処置', '建议措施'),
    'fmea.reference.col-responsible': (
        'Responsable', 'Responsible', 'Responsable',
        'المسؤول', '担当', '责任人'),
}
