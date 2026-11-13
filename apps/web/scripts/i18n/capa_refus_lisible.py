# -*- coding: utf-8 -*-
"""Table i18n - le refus de la fenetre d'ouverture se lit.

Un ecran qui refuse en silence passe pour un ecran en panne. Ces libelles sont
ceux que l'on nomme quand l'enregistrement ne part pas : le nom d'un controle de
formulaire (`assigneeId`, `verificationRequired`) n'est pas un mot de
l'utilisateur, et le message doit designer le champ comme l'ecran le designe.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- ce qui manque, dit avec les mots de l'ecran ------------------------
    'capa.open.missing': (
        "À compléter avant d'enregistrer : {$champs}",
        'To complete before saving: {$champs}',
        'Por completar antes de guardar: {$champs}',
        'يجب إكمالها قبل الحفظ: {$champs}',
        '保存前に入力が必要な項目：{$champs}',
        '保存前需补充：{$champs}'),
    'capa.open.invalid': (
        'La demande est incomplète : vérifiez les champs signalés.',
        'The request is incomplete: check the highlighted fields.',
        'La solicitud está incompleta: revise los campos señalados.',
        'الطلب غير مكتمل: تحقق من الحقول المشار إليها.',
        '入力が不完全です。指摘された項目を確認してください。',
        '请求不完整：请检查标出的字段。'),

    # --- les champs, nommes comme l'ecran les nomme -------------------------
    'capa.open.field-title': (
        'Intitulé', 'Title', 'Título', 'العنوان', '件名', '标题'),
    'capa.open.field-type': (
        'Nature', 'Nature', 'Naturaleza', 'الطبيعة', '区分', '性质'),
    'capa.open.field-criticity': (
        'Criticité', 'Criticality', 'Criticidad', 'الخطورة', '重大度', '严重度'),
    'capa.open.field-owner': (
        'Responsable', 'Owner', 'Responsable', 'المسؤول', '責任者', '负责人'),
    'capa.open.field-due': (
        'Échéance', 'Due date', 'Fecha límite', 'تاريخ الاستحقاق', '期限', '到期日'),
    'capa.open.field-source': (
        'Source', 'Source', 'Origen', 'المصدر', '発生源', '来源'),
    'capa.open.field-source-ref': (
        'Référence de la source', 'Source reference', 'Referencia del origen',
        'مرجع المصدر', '発生源の参照', '来源引用'),
    'capa.open.field-verification': (
        "Vérification d'efficacité requise", 'Effectiveness verification required',
        'Verificación de eficacia requerida', 'التحقق من الفعالية مطلوب',
        '有効性検証の要否', '是否需要有效性验证'),
    'capa.open.field-verifier': (
        'Vérificateur', 'Verifier', 'Verificador', 'المدقق', '検証者', '验证者'),

    # --- l'erreur, sur la question elle-meme --------------------------------
    'capa.verification.decision-required': (
        'Répondez par « oui » ou « non » : la question doit être tranchée pour '
        'ouvrir le dossier.',
        'Answer yes or no: the question must be settled to open the case.',
        'Responda sí o no: la pregunta debe resolverse para abrir el caso.',
        'أجب بنعم أو لا: يجب حسم السؤال لفتح الملف.',
        '「はい」か「いいえ」で回答してください。案件を開くにはこの判断が必要です。',
        '请回答“是”或“否”：开立档案前必须作出决定。'),
}
