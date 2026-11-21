# -*- coding: utf-8 -*-
"""Table i18n - une nature d'action par discipline du traitement 8D.

Le D3 endigue, le D5 corrige, le D7 previent. Chaque discipline nomme donc ce
que son bouton ajoute, et la fenetre qu'il ouvre ne propose plus que cette
nature-la : la premiere action de confinement d'un ecart naissait corrective,
parce que la fenetre reposait la question avec « Corrective » pre-cochee.

L'evaluation d'efficacite suit la meme logique : un endiguement ne promet aucun
effet, il n'a donc ni critere ni verification ; une corrective et une preventive
en ont, et quand le dossier a deja tranche, on le RAPPELLE plutot que de se
taire.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- le bouton d'ajout, par nature --------------------------------------
    'capa.actions.add-containment': (
        'Ajouter une action de confinement', 'Add a containment action',
        'Anadir una accion de contencion', 'إضافة إجراء احتواء',
        '封じ込め対策を追加', '添加围堵措施'),
    'capa.actions.add-corrective': (
        'Ajouter une action corrective', 'Add a corrective action',
        'Anadir una accion correctiva', 'إضافة إجراء تصحيحي',
        '是正処置を追加', '添加纠正措施'),
    'capa.actions.add-preventive': (
        'Ajouter une action preventive', 'Add a preventive action',
        'Anadir una accion preventiva', 'إضافة إجراء وقائي',
        '予防処置を追加', '添加预防措施'),

    # --- l'efficacite deja tranchee, rappelee -------------------------------
    'capa.action-dialog.effectiveness-recall': (
        'Deja attendu sur ce dossier', 'Already expected on this case',
        'Ya previsto en este expediente', 'مطلوب بالفعل في هذا الملف',
        'この案件ですでに求められていること', '本案卷已有的要求'),
    'capa.action-dialog.verification-required-yes': (
        "L'efficacite sera verifiee avant la cloture.",
        'Effectiveness will be verified before closure.',
        'La eficacia se verificara antes del cierre.',
        'سيتم التحقق من الفعالية قبل الإغلاق.',
        '有効性は終結前に検証されます。',
        '有效性将在结案前得到验证。'),
    'capa.action-dialog.verification-required-no': (
        "Juge sans verification d'efficacite.",
        'Judged without effectiveness verification.',
        'Juzgado sin verificacion de eficacia.',
        'تم الحكم دون التحقق من الفعالية.',
        '有効性検証なしと判断されました。',
        '已判定为无需有效性验证。'),
}
