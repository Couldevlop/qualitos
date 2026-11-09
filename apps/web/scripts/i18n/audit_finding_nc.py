# -*- coding: utf-8 -*-
"""Déclarer au registre l'écart découvert pendant un audit.

Un constat d'audit non conforme et une non-conformité du registre sont le même
fait vu de deux endroits. Ce chemin les relie une fois pour toutes, plutôt que
de laisser l'auditeur ressaisir la même phrase dans deux écrans.
"""

TRANSLATIONS = {
    # --- le point d'entrée, dans le tableau des constats ----------------------
    'audits.detail.declare-nc': (
        'Déclarer la NC', 'Declare the NC', 'Declarar la NC',
        'الإبلاغ عن عدم المطابقة', '不適合を登録', '登记不符合项'),
    'audits.detail.open-nc': (
        'Voir la NC', 'View the NC', 'Ver la NC',
        'عرض عدم المطابقة', '不適合を表示', '查看不符合项'),

    # --- la fenêtre de déclaration -------------------------------------------
    'audits.declare-nc.title': (
        'Déclarer la non-conformité du constat',
        'Declare the finding’s non-conformity',
        'Declarar la no conformidad del hallazgo',
        'الإبلاغ عن عدم مطابقة الملاحظة',
        '所見の不適合を登録',
        '登记该发现项的不符合项'),
    'audits.declare-nc.subtitle': (
        'Le constat devient un écart du registre : il comptera dans les indicateurs, '
        'portera son analyse de cause et se clôra sur une preuve.',
        'The finding becomes a registry entry: it will count in the indicators, carry '
        'its root-cause analysis and close on evidence.',
        'El hallazgo pasa al registro: contará en los indicadores, llevará su análisis '
        'de causa raíz y se cerrará con una prueba.',
        'تصبح الملاحظة سجلاً لعدم المطابقة: تُحتسب في المؤشرات، وتحمل تحليل سببها '
        'الجذري، وتُغلق بدليل.',
        '所見は登録簿の不適合になります。指標に計上され、原因分析を伴い、証拠によって'
        '完了します。',
        '发现项进入不符合项登记册：计入指标、附带根本原因分析，并以证据关闭。'),
    'audits.declare-nc.submit': (
        'Inscrire au registre', 'Add to the registry', 'Inscribir en el registro',
        'التسجيل في السجل', '登録簿に記載', '登记入册'),
    'audits.declare-nc.from': (
        'Constat d’audit', 'Audit finding', 'Hallazgo de auditoría',
        'ملاحظة تدقيق', '監査所見', '审核发现项'),
    'audits.declare-nc.description': (
        'Description reprise', 'Description carried over', 'Descripción retomada',
        'الوصف المنقول', '引き継がれる記述', '沿用的描述'),
    'audits.declare-nc.detected': (
        'Constaté le', 'Observed on', 'Constatado el',
        'تاريخ الملاحظة', '確認日', '发现日期'),
    'audits.declare-nc.nc-title': (
        'Intitulé de l’écart', 'Non-conformity title', 'Título de la no conformidad',
        'عنوان عدم المطابقة', '不適合の件名', '不符合项标题'),
    'audits.declare-nc.nc-title-hint': (
        'Court et citable : c’est ce que lira la revue de direction.',
        'Short and quotable: this is what the management review will read.',
        'Corto y citable: es lo que leerá la revisión por la dirección.',
        'قصير وقابل للاقتباس: هذا ما ستقرأه مراجعة الإدارة.',
        '短く引用しやすい表現で。経営レビューが読む文です。',
        '简短可引用：管理评审将阅读此标题。'),
    'audits.declare-nc.nc-title-required': (
        'Intitulé requis.', 'Title required.', 'Título obligatorio.',
        'العنوان مطلوب.', '件名は必須です。', '标题为必填项。'),
    'audits.declare-nc.saved': (
        'Écart {$reference} inscrit au registre.',
        'Non-conformity {$reference} added to the registry.',
        'No conformidad {$reference} inscrita en el registro.',
        'تم تسجيل عدم المطابقة {$reference} في السجل.',
        '不適合 {$reference} を登録簿に記載しました。',
        '不符合项 {$reference} 已登记入册。'),
    'audits.declare-nc.already': (
        'Ce constat a déjà son écart au registre.',
        'This finding already has its registry entry.',
        'Este hallazgo ya tiene su registro de no conformidad.',
        'لهذه الملاحظة سجل عدم مطابقة بالفعل.',
        'この所見にはすでに登録簿の不適合があります。',
        '该发现项已有对应的不符合项记录。'),
    'audits.declare-nc.error': (
        'L’écart n’a pas pu être inscrit au registre.',
        'The non-conformity could not be added to the registry.',
        'No se ha podido inscribir la no conformidad en el registro.',
        'لم يتمكن النظام من تسجيل عدم المطابقة.',
        '不適合を登録簿に記載できませんでした。',
        '无法将不符合项登记入册。'),
}
