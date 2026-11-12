# -*- coding: utf-8 -*-
"""Table i18n - le dossier CAPA reflete ses actions (ADR 0117).

Deux consequences de langue. Dans la fiche CAPA, le pave vertical qui depliait
chaque action a cede la place au tableau : il lui faut un titre. Dans la fenetre
de declaration d'un ecart, la famille se deduit desormais de la nature choisie,
qui devient la seule question posee -- et le motif de blocage doit nommer un
champ encore present a l'ecran.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- la fiche CAPA ------------------------------------------------------
    'capa.detail.actions-title': (
        'Actions', 'Actions', 'Acciones', 'الإجراءات', '対策', '措施'),

    # --- la nature de l'ecart ----------------------------------------------
    'nc.create.nature': (
        "Nature de l'écart", 'Nature of the finding', 'Naturaleza de la desviación',
        'طبيعة الانحراف', '不適合の種類', '偏差性质'),
    'nc.create.nature-required': (
        "Choisissez la nature de l'écart.", 'Choose the nature of the finding.',
        'Elija la naturaleza de la desviación.', 'اختر طبيعة الانحراف.',
        '不適合の種類を選んでください。', '请选择偏差性质。'),
    'nc.create.nature-empty': (
        "Aucune nature au catalogue : prévenez l'administrateur du compte.",
        'No nature in the catalogue: tell the account administrator.',
        'Ninguna naturaleza en el catálogo: avise al administrador de la cuenta.',
        'لا توجد طبيعة في الكتالوج: أبلغ مسؤول الحساب.',
        'カタログに種類がありません。アカウント管理者に連絡してください。',
        '目录中没有性质：请通知账户管理员。'),
    'nc.create.blocked-nature': (
        "Choisissez la nature de l'écart pour pouvoir déclarer.",
        'Choose the nature of the finding to be able to declare it.',
        'Elija la naturaleza de la desviación para poder declararla.',
        'اختر طبيعة الانحراف لتتمكن من التصريح.',
        '申告するには不適合の種類を選んでください。',
        '请选择偏差性质后才能申报。'),
}
