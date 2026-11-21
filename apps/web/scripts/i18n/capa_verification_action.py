# -*- coding: utf-8 -*-
"""Juger l'efficacite d'une action, et dire ce qui retient la cloture.

Chaque action porte sa propre exigence de verification (ADR 0131), et le
dossier ne se cloture pas tant qu'une action qui en a promis une ne l'a pas
obtenue (ADR 0132). Ces libelles sont ceux du geste qui solde la promesse.

Format : id -> (fr, en, es, ar, ja, zh).
"""

TRANSLATIONS = {
    # --- le geste, dans le tableau des actions ---
    'capa.actions.verify': (
        "Verifier l'efficacite", 'Verify effectiveness',
        'Verificar la eficacia', 'التحقق من الفعالية',
        '有効性を検証', '验证有效性'),
    'capa.actions.verified': (
        'Efficacite verifiee', 'Effectiveness verified',
        'Eficacia verificada', 'تم التحقق من الفعالية',
        '有効性を検証済み', '有效性已验证'),

    # --- la fenetre de jugement ---
    'capa.action-verification.title': (
        "Verifier l'efficacite de l'action", "Verify the action's effectiveness",
        'Verificar la eficacia de la accion', 'التحقق من فعالية الإجراء',
        'アクションの有効性を検証', '验证措施的有效性'),
    'capa.action-verification.subtitle': (
        "Ce qui a ete constate, confronte au critere pose quand l'action a ete decidee.",
        'What was observed, set against the criterion defined when the action was decided.',
        'Lo constatado, confrontado con el criterio fijado cuando se decidio la accion.',
        'ما تم ملاحظته، مقابل المعيار الموضوع عند اتخاذ الإجراء.',
        'アクションを決定した際に定めた基準と照らし合わせた実測値。',
        '将实测结果与决定该措施时设定的准则相对照。'),
    'capa.action-verification.submit': (
        'Enregistrer le verdict', 'Save the verdict',
        'Registrar el veredicto', 'تسجيل الحكم',
        '判定を保存', '保存判定'),
    'capa.action-verification.saved': (
        'Verification enregistree.', 'Verification saved.',
        'Verificacion registrada.', 'تم تسجيل التحقق.',
        '検証を保存しました。', '验证已保存。'),
    'capa.action-verification.failed': (
        "La verification n'a pas pu etre enregistree.",
        'The verification could not be saved.',
        'No se pudo registrar la verificacion.',
        'لم يتم تسجيل التحقق.',
        '検証を保存できませんでした。',
        '无法保存验证。'),
    'capa.action-verification.observed-required': (
        'Le releve constate est obligatoire.', 'The observed reading is required.',
        'El valor constatado es obligatorio.', 'القيمة الملاحظة مطلوبة.',
        '実測値は必須です。', '实测结果为必填项。'),
    'capa.action-verification.no-criterion': (
        "Aucun critere d'efficacite n'a ete pose sur cette action ni sur son dossier : "
        "il n'y a rien a quoi confronter le releve. Reprenez l'action pour y inscrire sa mesure.",
        'No effectiveness criterion was set on this action or on its case: there is nothing '
        'to set the reading against. Edit the action to record its measure.',
        'No se fijo ningun criterio de eficacia en esta accion ni en su expediente: no hay '
        'nada con que confrontar el valor. Edite la accion para inscribir su medida.',
        'لم يُحدَّد أي معيار فعالية لهذا الإجراء ولا لملفه: لا يوجد ما تُقارن به القيمة. '
        'عدّل الإجراء لتدوين قياسه.',
        'このアクションにもその案件にも有効性基準が設定されていません。実測値を照合する対象が'
        'ありません。アクションを編集して測定基準を記録してください。',
        '本措施及其案卷均未设定有效性准则：实测结果无从对照。请编辑该措施以记录其衡量标准。'),

    # --- ce que le verdict choisi declenche ---
    'capa.action-verification.outcome-effective': (
        'Cette action ne retiendra plus la cloture du dossier.',
        'This action will no longer hold back the closure of the case.',
        'Esta accion ya no retendra el cierre del expediente.',
        'لن يُعيق هذا الإجراء إغلاق الملف بعد الآن.',
        'このアクションは案件の終結を妨げなくなります。',
        '本措施将不再阻碍案卷结案。'),
    'capa.action-verification.outcome-partial': (
        "L'effet est insuffisant : l'action continuera de retenir la cloture, le temps qu'une "
        'action complementaire soit decidee.',
        'The effect falls short: the action will keep holding back closure until a '
        'complementary action is decided.',
        'El efecto es insuficiente: la accion seguira retendiendo el cierre hasta que se '
        'decida una accion complementaria.',
        'الأثر غير كافٍ: سيواصل الإجراء إعاقة الإغلاق حتى يُقرَّر إجراء مُكمِّل.',
        '効果が不十分です。補完的なアクションが決まるまで、このアクションは終結を妨げ続けます。',
        '效果不足：在决定补充措施之前，本措施将继续阻碍结案。'),
    'capa.action-verification.outcome-ineffective': (
        "L'action n'a pas produit son effet : elle continuera de retenir la cloture, et la "
        'cause est a reprendre.',
        'The action did not produce its effect: it will keep holding back closure, and the '
        'cause must be revisited.',
        'La accion no produjo su efecto: seguira retendiendo el cierre y la causa debe '
        'revisarse.',
        'لم يُحقِّق الإجراء أثره: سيواصل إعاقة الإغلاق، ويجب إعادة النظر في السبب.',
        'アクションは効果を生みませんでした。終結を妨げ続け、原因の再検討が必要です。',
        '本措施未产生效果：它将继续阻碍结案，须重新分析原因。'),

    # --- le motif de blocage, sur la fiche du dossier ---
    'capa.blocker.action-verification-one': (
        "1 action attend la verification d'efficacite qu'elle a promise.",
        '1 action is awaiting the effectiveness verification it promised.',
        '1 accion espera la verificacion de eficacia que prometio.',
        'إجراء واحد ينتظر التحقق من الفعالية الذي وُعِد به.',
        '1 件のアクションが、約束した有効性の検証を待っています。',
        '有 1 项措施尚待其承诺的有效性验证。'),
    'capa.blocker.action-verification-many': (
        "{$count} actions attendent la verification d'efficacite qu'elles ont promise.",
        '{$count} actions are awaiting the effectiveness verification they promised.',
        '{$count} acciones esperan la verificacion de eficacia que prometieron.',
        '{$count} إجراءات تنتظر التحقق من الفعالية الذي وُعِد به.',
        '{$count} 件のアクションが、約束した有効性の検証を待っています。',
        '有 {$count} 项措施尚待其承诺的有效性验证。'),
}
