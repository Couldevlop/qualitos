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

    'apqp.eyebrow': ('APQP', 'APQP', 'APQP', 'APQP', 'APQP', 'APQP'),
    'apqp.title': (
        'Planification qualité produit', 'Advanced product quality planning',
        'Planificación avanzada de la calidad', 'التخطيط المتقدم لجودة المنتج',
        '先行製品品質計画', '产品质量先期策划'),
    'apqp.subtitle': (
        "Cinq phases en V : on descend jusqu'à la conception du processus, on remonte vers la production série.",
        'Five phases in a V: down to process design, then back up to serial production.',
        'Cinco fases en V: se baja hasta el diseño del proceso y se remonta a la producción en serie.',
        'خمس مراحل على شكل V: نزولاً إلى تصميم العملية ثم صعوداً إلى الإنتاج المتسلسل.',
        'V字型の五つのフェーズ。工程設計まで下り、量産へと上がります。',
        'V 形的五个阶段：下行至过程设计，再上行至量产。'),
    'apqp.cycle-aria': (
        'Cycle APQP en cinq phases', 'APQP cycle in five phases', 'Ciclo APQP en cinco fases',
        'دورة APQP في خمس مراحل', 'APQP サイクル（五フェーズ）', 'APQP 五阶段流程'),
    'apqp.pick-a-phase': (
        "Choisissez une phase du schéma pour voir ce qu'elle établit et ce qu'elle doit produire.",
        'Pick a phase on the diagram to see what it establishes and must produce.',
        'Elija una fase del esquema para ver qué establece y qué debe producir.',
        'اختر مرحلة من المخطط لمعرفة ما تُرسيه وما يجب أن تُنتجه.',
        '図からフェーズを選ぶと、その目的と成果物が表示されます。',
        '在示意图中选择一个阶段，查看其确立的内容与应产出的交付物。'),
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
}
