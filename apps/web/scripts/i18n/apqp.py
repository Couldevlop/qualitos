# -*- coding: utf-8 -*-
"""Libellés APQP — l'interface seulement.

Les cinq phases et leurs livrables ne sont PAS ici : ils vivent dans
`app/features/apqp/apqp.reference.ts`, comme les barèmes FMEA, et pour la même
raison. Un livrable normatif traduit librement n'est plus le même livrable —
« Control Plan » désigne un document précis, pas un plan de contrôle
quelconque. Seule la coquille autour est traduite.

Les intitulés de navigation, eux, portent les titres courts des phases : ils
doivent se lire dans la langue de l'utilisateur, et ne servent pas de référence
normative.
"""

TRANSLATIONS = {
    'nav.apqp': ('APQP', 'APQP', 'APQP', 'APQP', 'APQP', 'APQP'),
    'nav.apqp-cycle': (
        'Le cycle', 'The cycle', 'El ciclo', 'الدورة', 'サイクル', '流程总览'),
    'nav.apqp-1': (
        '1 · Planifier et définir', '1 · Plan and define', '1 · Planificar y definir',
        '١ · التخطيط والتحديد', '1・計画と定義', '1 · 计划与定义'),
    'nav.apqp-2': (
        '2 · Conception du produit', '2 · Product design', '2 · Diseño del producto',
        '٢ · تصميم المنتج', '2・製品設計', '2 · 产品设计'),
    'nav.apqp-3': (
        '3 · Conception du processus', '3 · Process design', '3 · Diseño del proceso',
        '٣ · تصميم العملية', '3・工程設計', '3 · 过程设计'),
    'nav.apqp-4': (
        '4 · Validation', '4 · Validation', '4 · Validación',
        '٤ · التحقق', '4・妥当性確認', '4 · 验证'),
    'nav.apqp-5': (
        "5 · Retour d'expérience", '5 · Feedback', '5 · Retroalimentación',
        '٥ · التغذية الراجعة', '5・フィードバック', '5 · 反馈'),

    'apqp.eyebrow': ('APQP', 'APQP', 'APQP', 'APQP', 'APQP', 'APQP'),
    'apqp.title': (
        'Planification qualité produit', 'Advanced product quality planning',
        'Planificación avanzada de la calidad', 'التخطيط المتقدم لجودة المنتج',
        '先行製品品質計画', '产品质量先期策划'),
    'apqp.subtitle': (
        "Cinq phases, de la voix du client au retour d'expérience. Chacune s'appuie sur la précédente.",
        'Five phases, from the voice of the customer to feedback. Each builds on the one before.',
        'Cinco fases, de la voz del cliente a la retroalimentación. Cada una se apoya en la anterior.',
        'خمس مراحل، من صوت العميل إلى التغذية الراجعة. تبني كل مرحلة على سابقتها.',
        '顧客の声からフィードバックまでの五つのフェーズ。各段階は前段を土台とします。',
        '从客户之声到反馈的五个阶段，每一阶段以前一阶段为基础。'),
    'apqp.deliverables-suffix': (
        ' livrables', ' deliverables', ' entregables', ' مُخرجات', '件の成果物', ' 项交付物'),
    'apqp.deliverables-title': (
        'Livrables attendus', 'Expected deliverables', 'Entregables esperados',
        'المُخرجات المتوقعة', '想定される成果物', '预期交付物'),
    'apqp.deliverables-subtitle': (
        "Ce que la phase doit avoir produit avant de passer à la suivante.",
        'What the phase must have produced before moving on.',
        'Lo que la fase debe haber producido antes de pasar a la siguiente.',
        'ما يجب أن تنتجه المرحلة قبل الانتقال إلى التالية.',
        '次のフェーズに進む前に、この段階が生み出しておくべきもの。',
        '进入下一阶段前，本阶段必须完成的内容。'),
    'apqp.back-to-cycle': (
        'Voir le cycle', 'View the cycle', 'Ver el ciclo',
        'عرض الدورة', 'サイクルを表示', '查看流程'),
    'apqp.neighbours-aria': (
        'Phases voisines', 'Adjacent phases', 'Fases contiguas',
        'المراحل المجاورة', '前後のフェーズ', '相邻阶段'),
}
