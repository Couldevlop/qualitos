# -*- coding: utf-8 -*-
"""Table i18n - representations graphiques des methodes (SS3.5).

Cascade des 5 Pourquoi et arete de poisson Ishikawa : ces chaines ne sont pas
des libelles d'interface mais l'EQUIVALENT ACCESSIBLE des dessins (title/desc
du SVG). Elles doivent donc dire ce que la figure montre, et renvoyer vers la
liste ou les cartes qui portent le texte integral.

id: (fr, en, es, ar, ja, zh).
"""

TRANSLATIONS = {

    # --- Cascade des 5 Pourquoi ------------------------------------------------
    'fivewhys.diagram.title': (
        'Cascade des pourquoi',
        'Chain of whys',
        'Cascada de porqués',
        'سلسلة الأسئلة «لماذا»',
        'なぜの連鎖',
        '为什么的层层递进'),
    'fivewhys.diagram.desc-concluded': (
        "Descente de {$count} pourquoi jusqu'à la cause racine. Le détail de chaque réponse "
        'est repris dans la liste qui suit.',
        'A descent of {$count} whys down to the root cause. Each answer is written out in '
        'full in the list below.',
        'Descenso de {$count} porqués hasta la causa raíz. El detalle de cada respuesta se '
        'recoge en la lista siguiente.',
        'تسلسل من {$count} أسئلة «لماذا» وصولًا إلى السبب الجذري. تفاصيل كل إجابة واردة في '
        'القائمة التالية.',
        '根本原因に至るまでの「なぜ」{$count} 段の連鎖です。各回答の全文は下のリストにあります。',
        '从表象逐层追问 {$count} 次直至根本原因。每条回答的完整内容见下方列表。'),
    'fivewhys.diagram.desc-open': (
        "Descente de {$count} pourquoi, sans cause racine conclue à ce jour. Le détail de "
        'chaque réponse est repris dans la liste qui suit.',
        'A descent of {$count} whys, with no root cause concluded yet. Each answer is '
        'written out in full in the list below.',
        'Descenso de {$count} porqués, sin causa raíz concluida por ahora. El detalle de '
        'cada respuesta se recoge en la lista siguiente.',
        'تسلسل من {$count} أسئلة «لماذا»، دون تحديد سبب جذري حتى الآن. تفاصيل كل إجابة '
        'واردة في القائمة التالية.',
        '「なぜ」{$count} 段の連鎖です。根本原因はまだ結論づけられていません。'
        '各回答の全文は下のリストにあります。',
        '已追问 {$count} 次，尚未得出根本原因。每条回答的完整内容见下方列表。'),
    'fivewhys.diagram.step': (
        'Pourquoi n°{$rank}',
        'Why no. {$rank}',
        'Porqué n.º {$rank}',
        'لماذا رقم {$rank}',
        'なぜ その{$rank}',
        '第 {$rank} 个为什么'),

    # --- Arête de poisson (Ishikawa) -------------------------------------------
    'ishikawa.diagram.title': (
        'Diagramme en arête de poisson',
        'Fishbone diagram',
        'Diagrama de espina de pescado',
        'مخطط عظم السمكة',
        '特性要因図（フィッシュボーン）',
        '鱼骨图'),
    'ishikawa.diagram.desc': (
        'Arête de poisson : {$branches} familles de causes et {$causes} causes de premier '
        'niveau convergeant vers le problème. Le détail, sous-causes comprises, est repris '
        'dans les cartes qui suivent.',
        'Fishbone: {$branches} cause families and {$causes} first-level causes converging '
        'on the problem. The detail, sub-causes included, is written out in the cards below.',
        'Espina de pescado: {$branches} familias de causas y {$causes} causas de primer '
        'nivel que convergen en el problema. El detalle, subcausas incluidas, figura en las '
        'tarjetas siguientes.',
        'مخطط عظم السمكة: {$branches} عائلات أسباب و{$causes} سببًا من المستوى الأول تلتقي '
        'عند المشكلة. التفاصيل، بما فيها الأسباب الفرعية، واردة في البطاقات التالية.',
        '特性要因図：{$branches} 系統の要因分類と、第一階層の要因 {$causes} 件が問題へ収束します。'
        '下位要因を含む詳細は下のカードにあります。',
        '鱼骨图：{$branches} 类原因、{$causes} 条一级原因汇聚于该问题。'
        '含子原因的完整明细见下方卡片。'),
}
