# -*- coding: utf-8 -*-
"""Inscrire au registre des risques le risque qu'une exigence fait courir.

Le risque énoncé sur une exigence de partie intéressée (§4.2) restait dans sa
fiche : il ne se cotait pas, ne se revoyait pas et ne comptait dans aucun
indicateur. Ce chemin le porte au registre (§6.1), et l'exigence le cite.
id: (fr, en, es, ar, ja, zh).
"""

TRANSLATIONS = {

    # --- le point d'entrée, dans le tableau des exigences --------------------
    'stakeholders.register-risk': (
        'Inscrire le risque', 'Register the risk', 'Inscribir el riesgo',
        'تسجيل الخطر', 'リスクを登録', '登记风险'),

    # --- la fenêtre ---------------------------------------------------------
    'stakeholders.risk.title': (
        'Inscrire le risque au registre', 'Add the risk to the register',
        'Inscribir el riesgo en el registro', 'تسجيل الخطر في السجل',
        'リスクを登録簿に記載', '将风险登记入册'),
    'stakeholders.risk.subtitle': (
        'Exigence : {$exigence}', 'Requirement: {$exigence}', 'Requisito: {$exigence}',
        'المتطلب: {$exigence}', '要求事項：{$exigence}', '要求：{$exigence}'),
    'stakeholders.risk.submit': (
        'Inscrire au registre', 'Add to the register', 'Inscribir en el registro',
        'التسجيل في السجل', '登録簿に記載', '登记入册'),
    'stakeholders.risk.from-risk': (
        'Risque énoncé', 'Stated risk', 'Riesgo enunciado',
        'الخطر المذكور', '記載済みのリスク', '已陈述的风险'),
    'stakeholders.risk.from-requirement': (
        "Cause reprise (l’exigence)", 'Cause carried over (the requirement)',
        'Causa retomada (el requisito)', 'السبب المنقول (المتطلب)',
        '引き継がれる原因（要求事項）', '沿用的原因（该要求）'),
    'stakeholders.risk.kind': (
        'Nature du risque', 'Kind of risk', 'Naturaleza del riesgo',
        'نوع الخطر', 'リスクの種別', '风险性质'),
    'stakeholders.risk.kind-hint': (
        "C’est elle qui range la ligne dans le registre.",
        'It is what files the entry in the register.',
        'Es lo que clasifica la línea en el registro.',
        'هو ما يصنّف السجل داخل السجل العام.',
        'このリスクが登録簿のどこに並ぶかを決めます。',
        '它决定该条目在登记册中的归类。'),
    'stakeholders.risk.process-hint': (
        'Là où le risque se matérialise.', 'Where the risk materialises.',
        'Donde el riesgo se materializa.', 'حيث يتحقق الخطر.',
        'リスクが現れる場所です。', '风险实际发生的地方。'),
    'stakeholders.risk.score': (
        'Criticité brute', 'Gross criticality', 'Criticidad bruta',
        'الخطورة الإجمالية', '固有の重大度', '固有危害度'),
    'stakeholders.risk.owner-hint': (
        "La personne qui répond du risque en revue : le registre n’accepte pas de "
        'ligne sans elle.',
        'The person who answers for the risk at review: the register takes no entry '
        'without one.',
        'La persona que responde del riesgo en la revisión: el registro no acepta '
        'ninguna línea sin ella.',
        'الشخص المسؤول عن الخطر في المراجعة: لا يقبل السجل أي بند بدونه.',
        'レビューでこのリスクに責任を負う担当者。登録簿は担当者のない行を受け付けません。',
        '在评审中对该风险负责的人：登记册不接受没有责任人的条目。'),

    # --- l'issue ------------------------------------------------------------
    'stakeholders.risk-opened': (
        "Risque inscrit au registre ; l’exigence le cite.",
        'Risk added to the register; the requirement now cites it.',
        'Riesgo inscrito en el registro; el requisito lo cita.',
        'تم تسجيل الخطر في السجل، والمتطلب يشير إليه الآن.',
        'リスクを登録簿に記載しました。要求事項がこれを参照します。',
        '风险已登记入册；该要求现引用它。'),
    'stakeholders.risk-failed': (
        "Le risque n’a pas pu être inscrit au registre.",
        'The risk could not be added to the register.',
        'No se ha podido inscribir el riesgo en el registro.',
        'لم يتمكن النظام من تسجيل الخطر في السجل.',
        'リスクを登録簿に記載できませんでした。',
        '无法将该风险登记入册。'),

    # --- l'origine, telle que le registre des risques la nomme --------------
    'rr.origin.stakeholder': (
        'Exigence de partie intéressée', 'Interested-party requirement',
        'Requisito de parte interesada', 'متطلب طرف معني',
        '利害関係者の要求事項', '相关方要求'),
}
