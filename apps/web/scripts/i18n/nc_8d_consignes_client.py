# -*- coding: utf-8 -*-
"""Table i18n - les corrections demandees sur la fiche 8D.

Dix phrases neuves. La date de D2 porte deux noms selon l'origine de l'ecart --
une reclamation se RECOIT, un constat interne se DETECTE -- d'ou un libelle de
plus. La date de debut d'une action parait au tableau, et l'ordre des deux dates
se signale en mots, parce que la couleur ne peut pas porter seule l'information.
Le selecteur de responsable propose en tete les personnes deja sur le dossier.
Et la demande d'evaluation d'efficacite est posee la ou l'action corrective
s'ecrit.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- D2 : la date, selon l'origine de l'ecart --------------------------
    'nc.detail.received': (
        'Date de réception', 'Date received', 'Fecha de recepción',
        'تاريخ الاستلام', '受領日', '接收日期'),

    # --- la date de debut d'une action, et l'ordre des deux dates ----------
    'capa.action.decided-on': (
        'Date de début', 'Start date', 'Fecha de inicio',
        'تاريخ البدء', '開始日', '开始日期'),
    'capa.actions.due-before-start': (
        'échéance antérieure à la date de début',
        'due date before the start date',
        'fecha límite anterior a la fecha de inicio',
        'الموعد النهائي قبل تاريخ البدء',
        '期限が開始日より前です',
        '截止日期早于开始日期'),
    'capa.actions.due-before-start-title': (
        "L'échéance précède la date de début.",
        'The due date precedes the start date.',
        'La fecha límite es anterior a la fecha de inicio.',
        'الموعد النهائي يسبق تاريخ البدء.',
        '期限が開始日より前になっています。',
        '截止日期早于开始日期。'),
    'capa.action-dialog.due-before-start': (
        "L'échéance précède la date de début : vérifiez l'une des deux dates.",
        'The due date precedes the start date: check one of the two dates.',
        'La fecha límite es anterior a la fecha de inicio: compruebe una de las dos fechas.',
        'الموعد النهائي يسبق تاريخ البدء: تحقق من إحدى التاريخين.',
        '期限が開始日より前です。いずれかの日付を確認してください。',
        '截止日期早于开始日期：请核对其中一个日期。'),

    # --- le responsable : l'equipe du dossier proposee en tete -------------
    'directory.picker.suggested': (
        'Déjà sur ce dossier', 'Already on this record', 'Ya en este expediente',
        'موجود بالفعل في هذا الملف', 'この案件の担当者', '已参与此记录'),
    'capa.action-dialog.team-suggested': (
        "Équipe de l'écart", 'Non-conformity team', 'Equipo de la no conformidad',
        'فريق عدم المطابقة', '不適合の対応チーム', '不合格项团队'),

    # --- la demande d'evaluation d'efficacite, posee a l'avance ------------
    'capa.action-dialog.criterion-placeholder': (
        'À quoi verra-t-on que la cause a disparu ? Ex. : aucun rebut sur ce défaut pendant 3 mois',
        'How will we know the cause is gone? E.g. no scrap for this defect for 3 months',
        '¿Cómo sabremos que la causa ha desaparecido? Ej.: ningún rechazo por este defecto durante 3 meses',
        'كيف سنعرف أن السبب قد اختفى؟ مثال: لا مرفوضات لهذا العيب خلال ٣ أشهر',
        '原因が消えたことをどう確認しますか。例：この不良による廃棄が3か月間ゼロ',
        '如何确认原因已消除？例如：该缺陷连续 3 个月无废品'),
    'capa.action-dialog.criterion-hint': (
        "Posé maintenant, il servira à juger l'efficacité de cette action ; il ne se réécrit plus ensuite.",
        'Set now, it will be used to judge this action’s effectiveness; it cannot be rewritten afterwards.',
        'Definido ahora, servirá para juzgar la eficacia de esta acción; después ya no se puede reescribir.',
        'يُحدَّد الآن ليُستخدم في الحكم على فعالية هذا الإجراء، ولا يُعاد كتابته بعد ذلك.',
        '今ここで定めた基準でこの処置の有効性を判断します。後から書き換えることはできません。',
        '现在设定，用于日后判断该措施的有效性；此后不可更改。'),

    # --- la borne de saisie du critere -------------------------------------
    'common.maxlength-2000': (
        '2000 caractères maximum.', '2000 characters maximum.',
        '2000 caracteres como máximo.', '٢٠٠٠ حرف كحد أقصى.',
        '最大2000文字です。', '最多 2000 个字符。'),
}
