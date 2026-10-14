# -*- coding: utf-8 -*-
"""Libellés de l'ajout d'une norme au SMI (ISO 14001 à côté d'ISO 9001, §8.9) :
le bouton, la fenêtre de choix, ses refus, et l'annonce de la norme ajoutée ;
et, dans le Standards Hub, ce que lit qui n'est pas administrateur (ADR 0090).

Placeholders : `${x}:nom:` côté TS s'écrit {$nom} (casse conservée) ;
`{{ x // i18n(ph="nom") }}` côté gabarit s'écrit {$NOM} (extrait en majuscules).
"""

TRANSLATIONS = {
    # ---------- entrée ----------
    'smi.add.open': (
        'Ajouter une norme', 'Add a standard', 'Añadir una norma',
        'إضافة معيار', '規格を追加', '添加标准'),
    'smi.add.admin-only': (
        "L'administrateur de votre organisation ajoute les normes.",
        "Your organisation's administrator adds the standards.",
        'El administrador de su organización añade las normas.',
        'يضيف مسؤول مؤسستك المعايير.',
        '規格の追加は組織の管理者が行います。',
        '标准由贵组织的管理员添加。'),

    # Standards Hub : sans bouton « Adopter » pour qui n'est pas administrateur.
    'standards.list.no-adoption': (
        'Aucune adoption.', 'No adoption.', 'Ninguna adopción.', 'لا يوجد اعتماد.', '採用はありません。', '暂无采用。'),

    # ---------- fenêtre ----------
    'smi.add.title': (
        'Ajouter une norme au SMI', 'Add a standard to the IMS', 'Añadir una norma al SGI',
        'إضافة معيار إلى نظام الإدارة المتكامل', '統合マネジメントシステムに規格を追加', '向综合管理体系添加标准'),
    'smi.add.subtitle': (
        'Les normes de système de management partagent les chapitres 4 à 10 : elles se pilotent dans le même tableau de bord et la même matrice des exigences.',
        'Management system standards share clauses 4 to 10: they are managed in the same dashboard and the same requirements matrix.',
        'Las normas de sistemas de gestión comparten los capítulos 4 a 10: se gestionan en el mismo panel y la misma matriz de requisitos.',
        'تشترك معايير أنظمة الإدارة في البنود من 4 إلى 10: تُدار في لوحة المعلومات نفسها ومصفوفة المتطلبات نفسها.',
        'マネジメントシステム規格は箇条4〜10を共有します。同じダッシュボードと同じ要求事項マトリクスで管理されます。',
        '管理体系标准共享第 4 至 10 章：在同一仪表板和同一要求矩阵中管理。'),
    'smi.add.loading': (
        'Chargement des normes du catalogue', 'Loading the catalogue standards', 'Cargando las normas del catálogo',
        'جارٍ تحميل معايير الكتالوج', 'カタログの規格を読み込み中', '正在加载目录中的标准'),
    'smi.add.load-failed': (
        "Les normes du catalogue n'ont pas pu être chargées.", 'The catalogue standards could not be loaded.',
        'No se pudieron cargar las normas del catálogo.', 'تعذّر تحميل معايير الكتالوج.',
        'カタログの規格を読み込めませんでした。', '无法加载目录中的标准。'),
    'smi.add.none': (
        'Toutes les normes de système de management du catalogue sont déjà adoptées.',
        'All management system standards in the catalogue are already adopted.',
        'Todas las normas de sistemas de gestión del catálogo ya están adoptadas.',
        'جميع معايير أنظمة الإدارة في الكتالوج معتمدة بالفعل.',
        'カタログのマネジメントシステム規格はすべて採用済みです。',
        '目录中的所有管理体系标准均已采用。'),
    'smi.add.suggested': (
        'Suggérées pour votre SMI', 'Suggested for your IMS', 'Sugeridas para su SGI',
        'مقترحة لنظام إدارتك المتكامل', 'あなたの統合マネジメントシステムへのおすすめ', '为您的综合管理体系推荐'),
    'smi.add.others': (
        'Autres normes de système de management', 'Other management system standards',
        'Otras normas de sistemas de gestión', 'معايير أخرى لأنظمة الإدارة',
        'その他のマネジメントシステム規格', '其他管理体系标准'),
    'smi.add.all': (
        'Normes de système de management', 'Management system standards', 'Normas de sistemas de gestión',
        'معايير أنظمة الإدارة', 'マネジメントシステム規格', '管理体系标准'),
    'smi.add.suggested-badge': ('Suggérée', 'Suggested', 'Sugerida', 'مقترح', 'おすすめ', '推荐'),
    'smi.add.shared-with': (
        'Rattachée à {$standards}', 'Linked to {$standards}', 'Vinculada a {$standards}',
        'مرتبط بـ {$standards}', '{$standards} と関連', '关联 {$standards}'),
    'smi.add.hint': (
        "L'adoption crée la feuille de route de certification dans le Standards Hub ; les preuves s'y rattachent ensuite, exigence par exigence.",
        'Adoption creates the certification roadmap in the Standards Hub; evidence is then linked to it, requirement by requirement.',
        'La adopción crea la hoja de ruta de certificación en el Standards Hub; las evidencias se vinculan después, requisito por requisito.',
        'يُنشئ الاعتماد خارطة طريق الشهادة في Standards Hub؛ ثم تُربط الأدلة بها متطلبًا تلو الآخر.',
        '採用すると Standards Hub に認証ロードマップが作成され、その後、要求事項ごとに証拠を紐づけます。',
        '采用后会在 Standards Hub 中创建认证路线图；之后逐条要求关联证据。'),
    'smi.add.submit': ('Ajouter', 'Add', 'Añadir', 'إضافة', '追加', '添加'),
    'smi.add.blocked': (
        'Choisissez la norme à ajouter.', 'Choose the standard to add.', 'Elija la norma que desea añadir.',
        'اختر المعيار المراد إضافته.', '追加する規格を選択してください。', '请选择要添加的标准。'),

    # ---------- refus et annonce ----------
    'smi.add.conflict': (
        'Cette norme est déjà adoptée par votre organisation.', 'Your organisation has already adopted this standard.',
        'Su organización ya ha adoptado esta norma.', 'اعتمدت مؤسستك هذا المعيار بالفعل.',
        'この規格は既に組織で採用されています。', '贵组织已采用该标准。'),
    'smi.add.forbidden': (
        "Seul l'administrateur de votre organisation peut ajouter une norme.",
        "Only your organisation's administrator can add a standard.",
        'Solo el administrador de su organización puede añadir una norma.',
        'لا يمكن إضافة معيار إلا لمسؤول مؤسستك.',
        '規格を追加できるのは組織の管理者のみです。',
        '只有贵组织的管理员才能添加标准。'),
    'smi.add.not-found': (
        "Cette norme n'est pas une norme de système de management du catalogue.",
        'This standard is not a management system standard from the catalogue.',
        'Esta norma no es una norma de sistemas de gestión del catálogo.',
        'هذا المعيار ليس من معايير أنظمة الإدارة في الكتالوج.',
        'この規格はカタログのマネジメントシステム規格ではありません。',
        '该标准不是目录中的管理体系标准。'),
    'smi.add.failed': (
        "La norme n'a pas pu être ajoutée.", 'The standard could not be added.', 'No se pudo añadir la norma.',
        'تعذّرت إضافة المعيار.', '規格を追加できませんでした。', '无法添加该标准。'),
    'smi.add.done': (
        '{$standard} ajoutée au SMI — feuille de route de certification créée.',
        '{$standard} added to the IMS — certification roadmap created.',
        '{$standard} añadida al SGI: hoja de ruta de certificación creada.',
        'أُضيف {$standard} إلى نظام الإدارة المتكامل — أُنشئت خارطة طريق الشهادة.',
        '{$standard} を統合マネジメントシステムに追加しました — 認証ロードマップを作成しました。',
        '已将 {$standard} 添加到综合管理体系 — 已创建认证路线图。'),
}
