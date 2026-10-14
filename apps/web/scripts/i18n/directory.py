# -*- coding: utf-8 -*-
"""Libellés du sélecteur de personne de l'annuaire (ADR 0091) : le champ qui
désigne un responsable d'audit, de CAPA ou de changement.

Placeholders : `${x}:nom:` côté TS s'écrit {$nom} ; `{{ x // i18n(ph="nom") }}`
côté gabarit s'écrit {$NOM}.
"""

TRANSLATIONS = {
    'directory.picker.label': ('Personne', 'Person', 'Persona', 'الشخص', '担当者', '人员'),
    'directory.picker.placeholder': (
        'Rechercher par nom ou adresse', 'Search by name or e-mail', 'Buscar por nombre o correo',
        'البحث بالاسم أو البريد الإلكتروني', '名前またはメールアドレスで検索', '按姓名或邮箱搜索'),
    'directory.picker.clear': (
        'Retirer la personne choisie', 'Remove the selected person', 'Quitar la persona seleccionada',
        'إزالة الشخص المحدد', '選択した担当者を解除', '移除所选人员'),
    'directory.picker.loading': ('Recherche…', 'Searching…', 'Buscando…', 'جارٍ البحث…', '検索中…', '正在搜索…'),
    'directory.picker.error': (
        'Annuaire indisponible pour le moment. Réessayez dans un instant.',
        'The directory is unavailable right now. Please try again in a moment.',
        'El directorio no está disponible en este momento. Inténtelo de nuevo en un instante.',
        'الدليل غير متاح حاليًا. حاول مرة أخرى بعد قليل.',
        '現在ディレクトリを利用できません。しばらくしてから再度お試しください。',
        '目录暂时不可用，请稍后重试。'),
    'directory.picker.empty': (
        'Aucune personne ne correspond.', 'No one matches.', 'Ninguna persona coincide.',
        'لا يوجد شخص مطابق.', '該当する担当者はいません。', '没有匹配的人员。'),
    'directory.picker.more': (
        '{$COUNT} autres résultats : précisez la recherche.',
        '{$COUNT} more results: refine your search.',
        '{$COUNT} resultados más: precise la búsqueda.',
        '{$COUNT} نتائج أخرى: حدّد البحث بدقة أكبر.',
        'ほかに {$COUNT} 件あります。検索条件を絞り込んでください。',
        '还有 {$COUNT} 条结果：请细化搜索。'),
    'directory.picker.resolving': (
        'Chargement du nom…', 'Loading name…', 'Cargando el nombre…', 'جارٍ تحميل الاسم…',
        '名前を読み込み中…', '正在加载姓名…'),
    'directory.picker.unknown': (
        "Cette personne ne fait plus partie de l'annuaire.",
        'This person is no longer in the directory.',
        'Esta persona ya no forma parte del directorio.',
        'لم يعد هذا الشخص موجودًا في الدليل.',
        'この担当者はディレクトリに存在しません。',
        '此人已不在目录中。'),
    'directory.picker.former': (
        "Ancien membre de l'organisation.", 'Former member of the organization.',
        'Antiguo miembro de la organización.', 'عضو سابق في المؤسسة.',
        '組織の元メンバーです。', '组织的前成员。'),
    'directory.picker.live-error': (
        'Annuaire indisponible', 'Directory unavailable', 'Directorio no disponible', 'الدليل غير متاح',
        'ディレクトリを利用できません', '目录不可用'),
    'directory.picker.live-none': (
        'Aucune personne trouvée', 'No one found', 'No se encontró a nadie', 'لم يتم العثور على أي شخص',
        '該当者なし', '未找到任何人员'),
    'directory.picker.live-count': (
        '{$COUNT} personne(s) proposée(s)', '{$COUNT} person(s) suggested', '{$COUNT} persona(s) sugerida(s)',
        'الأشخاص المقترحون: {$COUNT}', '候補 {$COUNT} 件', '建议人员 {$COUNT} 名'),
}
