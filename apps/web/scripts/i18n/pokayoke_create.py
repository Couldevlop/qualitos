# -*- coding: utf-8 -*-
"""Une action DMAIC devient un détrompeur inscrit à la bibliothèque (§3.4).

Le catalogue des détrompeurs était livré par la plateforme et se lisait seul :
aucune action d'amélioration ne pouvait y entrer. Une solution qui empêche
l'erreur restait donc attachée à son projet, et la ligne voisine la
réinventait. La bibliothèque devient celle du client, alimentée par ses
actions, et chaque dispositif cite le projet dont il est né.
id: (fr, en, es, ar, ja, zh).
"""

TRANSLATIONS = {

    # --- les mots des détrompeurs -------------------------------------------
    'pokayoke.type.prevention': (
        'Prévention', 'Prevention', 'Prevención', 'المنع', '予防', '预防'),
    'pokayoke.type.detection': (
        'Détection', 'Detection', 'Detección', 'الكشف', '検知', '检测'),

    'pokayoke.mech.physical-shape': (
        'Forme physique', 'Physical shape', 'Forma física',
        'الشكل المادي', '物理形状', '物理形状'),
    'pokayoke.mech.interlock': (
        'Verrouillage', 'Interlock', 'Enclavamiento',
        'التعشيق', 'インターロック', '互锁'),
    'pokayoke.mech.limit-switch': (
        'Fin de course', 'Limit switch', 'Final de carrera',
        'مفتاح نهاية الشوط', 'リミットスイッチ', '限位开关'),
    'pokayoke.mech.sensor': (
        'Capteur', 'Sensor', 'Sensor', 'مستشعر', 'センサー', '传感器'),
    'pokayoke.mech.vision': (
        'Vision', 'Vision', 'Visión', 'رؤية آلية', '画像認識', '视觉'),
    'pokayoke.mech.checklist': (
        'Liste de vérification', 'Checklist', 'Lista de verificación',
        'قائمة تحقّق', 'チェックリスト', '检查表'),
    'pokayoke.mech.color-coding': (
        'Code couleur', 'Colour coding', 'Código de color',
        'ترميز لوني', '色分け', '颜色编码'),
    'pokayoke.mech.position-reference': (
        'Référence de position', 'Position reference', 'Referencia de posición',
        'مرجع الموضع', '位置基準', '位置基准'),
    'pokayoke.mech.counter': (
        'Comptage', 'Counter', 'Conteo', 'العدّ', 'カウント', '计数'),
    'pokayoke.mech.software-validation': (
        'Contrôle logiciel', 'Software validation', 'Control por software',
        'تحقّق برمجي', 'ソフトウェア検証', '软件校验'),
    'pokayoke.mech.other': (
        'Autre', 'Other', 'Otro', 'أخرى', 'その他', '其他'),

    # --- la fenêtre ---------------------------------------------------------
    'pokayoke.create.title': (
        'Inscrire un détrompeur', 'Add a mistake-proofing device',
        'Inscribir un dispositivo antierror', 'تسجيل أداة منع الخطأ',
        'ポカヨケを登録', '登记防错装置'),
    'pokayoke.create.subtitle': (
        "Une action qui empêche l'erreur de se produire devient un dispositif de votre "
        'bibliothèque, réutilisable sur un autre projet.',
        'An action that makes the error impossible becomes a device in your library, '
        'reusable on another project.',
        'Una acción que impide el error pasa a ser un dispositivo de su biblioteca, '
        'reutilizable en otro proyecto.',
        'الإجراء الذي يمنع وقوع الخطأ يصبح أداة في مكتبتك، قابلة لإعادة الاستخدام في مشروع آخر.',
        '誤りを起こり得なくする対策は、ライブラリの装置となり、別のプロジェクトでも使えます。',
        '能让错误无法发生的措施将成为您库中的装置，可在其他项目复用。'),
    'pokayoke.create.submit': (
        'Inscrire et déployer', 'Add and deploy', 'Inscribir y desplegar',
        'التسجيل والنشر', '登録して配備', '登记并部署'),

    'pokayoke.create.origin': (
        'Origine citée', 'Origin cited', 'Origen citado',
        'المصدر المذكور', '記載される出所', '引用的来源'),
    'pokayoke.create.origin-text': (
        "Projet DMAIC « {$project} » — action n° {$position} du plan d'actions",
        'DMAIC project “{$project}” — action no. {$position} of the action plan',
        'Proyecto DMAIC «{$project}» — acción n.º {$position} del plan de acciones',
        'مشروع DMAIC «{$project}» — الإجراء رقم {$position} من خطة الإجراءات',
        'DMAICプロジェクト「{$project}」— 対策計画の第{$position}項',
        'DMAIC 项目《{$project}》— 行动计划第 {$position} 项'),

    'pokayoke.create.name': (
        'Nom du dispositif', 'Device name', 'Nombre del dispositivo',
        'اسم الأداة', '装置の名称', '装置名称'),
    'pokayoke.create.name-hint': (
        "Repris de l'action, corrigez-le : il se lira seul dans la bibliothèque.",
        'Taken from the action; correct it — it will be read on its own in the library.',
        'Tomado de la acción; corríjalo: se leerá solo en la biblioteca.',
        'مأخوذ من الإجراء، فصحّحه: سيُقرأ وحده في المكتبة.',
        '対策文から取りました。ライブラリでは単独で読まれるため、必要なら直してください。',
        '取自该措施，请酌情修改：它将在库中单独呈现。'),

    'pokayoke.create.type': (
        'Il agit', 'It acts by', 'Actúa', 'يعمل بـ', '作用の仕方', '作用方式'),
    'pokayoke.create.type-hint': (
        "Prévention : l'erreur devient impossible. Détection : elle est vue tout de suite.",
        'Prevention: the error becomes impossible. Detection: it is seen at once.',
        'Prevención: el error se vuelve imposible. Detección: se ve de inmediato.',
        'المنع: يصبح الخطأ مستحيلًا. الكشف: يُرى فورًا.',
        '予防：誤りが起こり得なくなる。検知：すぐに見える。',
        '预防：错误无法发生。检测：立刻被发现。'),
    'pokayoke.create.mechanism': (
        'Par quel mécanisme', 'By which mechanism', 'Mediante qué mecanismo',
        'بأي آلية', 'どの仕組みで', '通过何种机制'),
    'pokayoke.create.mechanism-hint': (
        'C’est par là que la bibliothèque se filtre.',
        'This is how the library is filtered.',
        'Es por aquí que se filtra la biblioteca.',
        'من هنا تُرشَّح المكتبة.',
        'ライブラリの絞り込みはここで行います。',
        '库正是按此筛选。'),

    'pokayoke.create.description': (
        'Comment il fonctionne', 'How it works', 'Cómo funciona',
        'كيف تعمل', '仕組みの説明', '工作原理'),
    'pokayoke.create.description-hint': (
        "Repris du texte de l'action. Celui qui le réutilisera n'aura pas votre contexte.",
        'Taken from the action text. Whoever reuses it will not have your context.',
        'Tomado del texto de la acción. Quien lo reutilice no tendrá su contexto.',
        'مأخوذ من نص الإجراء. من يعيد استخدامه لن يملك سياقك.',
        '対策文から取りました。再利用する人にあなたの文脈はありません。',
        '取自措施正文。复用者没有您的上下文。'),
    'pokayoke.create.industries': (
        "Où il s'applique", 'Where it applies', 'Dónde se aplica',
        'حيث تنطبق', '適用範囲', '适用范围'),
    'pokayoke.create.industries-placeholder': (
        'Lignes, ateliers ou secteurs concernés', 'Lines, shops or sectors concerned',
        'Líneas, talleres o sectores afectados', 'الخطوط أو الورش أو القطاعات المعنيّة',
        '対象のライン、工場、業種', '涉及的生产线、车间或行业'),
    'pokayoke.create.examples': (
        'Exemples de pose', 'Examples of use', 'Ejemplos de instalación',
        'أمثلة على التركيب', '設置例', '安装示例'),

    'pokayoke.create.done': (
        'Détrompeur {$code} inscrit à la bibliothèque et déployé sur ce projet.',
        'Device {$code} added to the library and deployed on this project.',
        'Dispositivo {$code} inscrito en la biblioteca y desplegado en este proyecto.',
        'تم تسجيل الأداة {$code} في المكتبة ونشرها على هذا المشروع.',
        'ポカヨケ {$code} をライブラリに登録し、本プロジェクトに配備しました。',
        '防错装置 {$code} 已登记入库并部署到本项目。'),
    'pokayoke.create.failed': (
        "Le détrompeur n'a pas pu être inscrit.", 'The device could not be added.',
        'El dispositivo no pudo inscribirse.', 'لم يتمكّن النظام من تسجيل الأداة.',
        'ポカヨケを登録できませんでした。', '未能登记该防错装置。'),

    # --- la fiche de résolution ---------------------------------------------
    'dmaic.res.pokayoke-add': (
        'Inscrire un détrompeur', 'Add a mistake-proofing device',
        'Inscribir un dispositivo antierror', 'تسجيل أداة منع الخطأ',
        'ポカヨケを登録', '登记防错装置'),
    'dmaic.res.pokayoke-tooltip': (
        'Inscrire cette action comme détrompeur réutilisable, et la déployer sur ce projet',
        'Add this action as a reusable mistake-proofing device, and deploy it on this project',
        'Inscribir esta acción como dispositivo antierror reutilizable y desplegarla en este proyecto',
        'تسجيل هذا الإجراء كأداة منع خطأ قابلة لإعادة الاستخدام، ونشرها على هذا المشروع',
        'この対策を再利用できるポカヨケとして登録し、本プロジェクトに配備する',
        '将该措施登记为可复用的防错装置，并部署到本项目'),
}
