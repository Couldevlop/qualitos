# -*- coding: utf-8 -*-
"""Table i18n — referentiel « procedure interne » (SS8). id: (fr, en, es, ar, ja, zh).

Couvre le badge et le filtre du catalogue, la creation d'un referentiel depuis une
procedure de la GED, la saisie de son arborescence, et la generation de la checklist
d'un audit a partir de ses exigences.
"""

TRANSLATIONS = {
    # --- catalogue : distinguer un referentiel du tenant d'une norme livree ---
    'standards.list.scope-label': (
        'Afficher', 'Show', 'Mostrar', 'عرض', '表示', '显示'),
    'standards.list.scope-all': (
        'Tout', 'All', 'Todo', 'الكل', 'すべて', '全部'),
    'standards.list.scope-owned': (
        'Procédures internes', 'Internal procedures', 'Procedimientos internos',
        'الإجراءات الداخلية', '社内手順', '内部程序'),
    'standards.list.scope-platform': (
        'Normes livrées', 'Delivered standards', 'Normas entregadas',
        'المعايير المتاحة', '提供済みの規格', '平台标准'),
    'standards.list.create-procedure': (
        'Créer depuis une procédure', 'Create from a procedure',
        'Crear a partir de un procedimiento', 'إنشاء من إجراء', '手順から作成', '从程序创建'),
    'standards.list.owned-badge': (
        'Procédure interne', 'Internal procedure', 'Procedimiento interno',
        'إجراء داخلي', '社内手順', '内部程序'),
    'standards.list.catalog-empty': (
        "Aucun référentiel sous ce filtre. Créez-en un depuis une procédure approuvée "
        'de votre GED.',
        'No referential under this filter. Create one from an approved procedure in your '
        'document control.',
        'Ningún referencial con este filtro. Cree uno a partir de un procedimiento '
        'aprobado de su gestión documental.',
        'لا يوجد مرجع ضمن هذه التصفية. أنشئ واحداً من إجراء معتمد في إدارة الوثائق.',
        'この絞り込みに該当する基準はありません。文書管理の承認済み手順から作成してください。',
        '此筛选下没有基准。请从文档管理中已批准的程序创建一个。'),

    # --- creation depuis une procedure de la GED ---
    'standards.procedure.title': (
        'Créer un référentiel depuis une procédure', 'Create a referential from a procedure',
        'Crear un referencial a partir de un procedimiento', 'إنشاء مرجع من إجراء',
        '手順から基準を作成', '从程序创建基准'),
    'standards.procedure.subtitle': (
        'Le référentiel reprend le code, le titre et la version de la procédure. Son '
        'arborescence naît vide : vous y saisirez les exigences à auditer.',
        'The referential takes over the code, title and version of the procedure. Its tree '
        'starts empty: you will enter the requirements to audit.',
        'El referencial retoma el código, el título y la versión del procedimiento. Su árbol '
        'nace vacío: usted introducirá los requisitos que se van a auditar.',
        'يأخذ المرجع رمز الإجراء وعنوانه وإصداره. تبدأ شجرته فارغة: ستدخل فيها المتطلبات المراد تدقيقها.',
        '基準は手順のコード・タイトル・版を引き継ぎます。ツリーは空の状態で作成され、監査する要求事項はご自身で入力します。',
        '基准沿用程序的编号、标题和版本。其树状结构初始为空：由您录入要审核的要求。'),
    'standards.procedure.source': (
        'Procédure source', 'Source procedure', 'Procedimiento de origen',
        'الإجراء المصدر', '元となる手順', '源程序'),
    'standards.procedure.source-required': (
        "Choisissez la procédure à auditer.", 'Choose the procedure to audit.',
        'Elija el procedimiento que se va a auditar.', 'اختر الإجراء المراد تدقيقه.',
        '監査する手順を選択してください。', '请选择要审核的程序。'),
    'standards.procedure.none': (
        "Aucune procédure approuvée. Publiez d'abord une version de votre procédure dans la "
        'GED : auditer contre un brouillon ne prouverait rien.',
        'No approved procedure. First publish a version of your procedure in document '
        'control: auditing against a draft would prove nothing.',
        'Ningún procedimiento aprobado. Publique primero una versión de su procedimiento en '
        'la gestión documental: auditar frente a un borrador no probaría nada.',
        'لا يوجد إجراء معتمد. انشر أولاً إصداراً من إجرائك في إدارة الوثائق: التدقيق مقابل مسودة لا يثبت شيئاً.',
        '承認済みの手順がありません。まず文書管理で手順の版を発行してください。草案に対する監査は何も証明しません。',
        '没有已批准的程序。请先在文档管理中发布该程序的一个版本：对照草稿审核证明不了任何事。'),
    'standards.procedure.created': (
        'Référentiel créé — saisissez maintenant ses exigences.',
        'Referential created — now enter its requirements.',
        'Referencial creado: introduzca ahora sus requisitos.',
        'تم إنشاء المرجع — أدخل الآن متطلباته.',
        '基準を作成しました。次に要求事項を入力してください。',
        '基准已创建——现在录入其要求。'),
    'standards.procedure.conflict': (
        'Un référentiel existe déjà pour cette procédure.',
        'A referential already exists for this procedure.',
        'Ya existe un referencial para este procedimiento.',
        'يوجد بالفعل مرجع لهذا الإجراء.', 'この手順にはすでに基準があります。',
        '该程序已存在一个基准。'),
    'standards.procedure.unapproved': (
        'Cette procédure doit être approuvée avant de servir de référentiel.',
        'This procedure must be approved before it can serve as a referential.',
        'Este procedimiento debe estar aprobado antes de servir como referencial.',
        'يجب اعتماد هذا الإجراء قبل أن يصلح كمرجع.',
        'この手順は基準として使う前に承認が必要です。', '该程序必须先获批准才能作为基准。'),
    'standards.procedure.forbidden': (
        "Vous n'avez pas les droits pour créer un référentiel.",
        'You are not allowed to create a referential.',
        'No tiene permisos para crear un referencial.', 'ليست لديك صلاحية إنشاء مرجع.',
        '基準を作成する権限がありません。', '您没有创建基准的权限。'),
    'standards.procedure.error': (
        'Création impossible pour le moment.', 'Creation is not possible at the moment.',
        'La creación no es posible por el momento.', 'الإنشاء غير ممكن في الوقت الحالي.',
        '現在作成できません。', '当前无法创建。'),

    # --- saisie de l'arborescence : sections, clauses, exigences ---
    'standards.tree.add-section': (
        'Ajouter une section', 'Add a section', 'Añadir una sección',
        'إضافة قسم', 'セクションを追加', '添加章节'),
    'standards.tree.add-clause': (
        '+ clause', '+ clause', '+ cláusula', '+ بند', '+ 箇条', '+ 条款'),
    'standards.tree.add-requirement': (
        '+ exigence', '+ requirement', '+ requisito', '+ متطلب', '+ 要求事項', '+ 要求'),
    'standards.tree.empty': (
        "Ce référentiel est vide : saisissez celles de votre procédure que l'audit devra "
        'vérifier.',
        'This referential is empty: enter those of your procedure that the audit will have '
        'to check.',
        'Este referencial está vacío: introduzca los de su procedimiento que la auditoría '
        'deberá verificar.',
        'هذا المرجع فارغ: أدخل متطلبات إجرائك التي سيتحقق منها التدقيق.',
        'この基準は空です。監査で確認する手順の要求事項を入力してください。',
        '该基准为空：请录入审核需要核查的程序要求。'),
    'standards.tree.subtitle': (
        "Le code n'a besoin d'être unique que parmi ses voisins immédiats.",
        'The code only needs to be unique among its immediate siblings.',
        'El código solo necesita ser único entre sus vecinos inmediatos.',
        'يكفي أن يكون الرمز فريداً بين نظرائه المباشرين فقط.',
        'コードは同じ階層の隣接する要素の間でのみ一意であれば十分です。',
        '编号只需在同级相邻项中唯一即可。'),
    'standards.tree.code': ('Code', 'Code', 'Código', 'الرمز', 'コード', '编号'),
    'standards.tree.code-placeholder': (
        'Ex. : 1, 1.1, 1.1.1', 'e.g.: 1, 1.1, 1.1.1', 'Ej.: 1, 1.1, 1.1.1',
        'مثال: 1، 1.1، 1.1.1', '例：1、1.1、1.1.1', '例：1、1.1、1.1.1'),
    'standards.tree.code-required': (
        'Le code est requis.', 'The code is required.', 'El código es obligatorio.',
        'الرمز مطلوب.', 'コードは必須です。', '编号为必填项。'),
    'standards.tree.code-maxlength': (
        'Code trop long pour ce niveau.', 'Code too long for this level.',
        'Código demasiado largo para este nivel.', 'الرمز طويل جداً لهذا المستوى.',
        'この階層にはコードが長すぎます。', '编号对该层级过长。'),
    'standards.tree.title-required': (
        'Le titre est requis.', 'The title is required.', 'El título es obligatorio.',
        'العنوان مطلوب.', 'タイトルは必須です。', '标题为必填项。'),
    'standards.tree.description': (
        'Description (optionnelle)', 'Description (optional)', 'Descripción (opcional)',
        'الوصف (اختياري)', '説明（任意）', '说明（可选）'),
    'standards.tree.text': (
        "Texte de l'exigence", 'Requirement text', 'Texto del requisito',
        'نص المتطلب', '要求事項の本文', '要求正文'),
    'standards.tree.text-placeholder': (
        "Ce que la procédure impose, tel qu'un auditeur le vérifiera.",
        'What the procedure requires, as an auditor will check it.',
        'Lo que exige el procedimiento, tal como lo verificará un auditor.',
        'ما يفرضه الإجراء، بالصيغة التي سيتحقق منها المدقق.',
        '監査員が確認する形で、手順が求めていること。',
        '程序所要求的内容，按审核员核查的方式表述。'),
    'standards.tree.text-required': (
        "Le texte de l'exigence est requis.", 'The requirement text is required.',
        'El texto del requisito es obligatorio.', 'نص المتطلب مطلوب.',
        '要求事項の本文は必須です。', '要求正文为必填项。'),
    'standards.tree.obligation': (
        'Obligation', 'Obligation', 'Obligación', 'الإلزام', '義務レベル', '强制程度'),
    'standards.tree.risk': (
        'Risque si absente', 'Risk if missing', 'Riesgo si falta',
        'الخطر عند غيابه', '欠如した場合のリスク', '缺失时的风险'),
    'standards.tree.evidence': (
        'Preuve attendue (optionnelle)', 'Expected evidence (optional)',
        'Evidencia esperada (opcional)', 'الدليل المتوقع (اختياري)',
        '期待される証跡（任意）', '预期证据（可选）'),
    'standards.tree.evidence-placeholder': (
        "Ex. : programme d'audit signé", 'e.g.: signed audit programme',
        'Ej.: programa de auditoría firmado', 'مثال: برنامج تدقيق موقّع',
        '例：署名済みの監査プログラム', '例：已签署的审核方案'),
    'standards.tree.criteria': (
        'Critère mesurable (optionnel)', 'Measurable criterion (optional)',
        'Criterio medible (opcional)', 'معيار قابل للقياس (اختياري)',
        '測定可能な基準（任意）', '可测量准则（可选）'),

    # --- titres de la boite de saisie : le niveau ET le geste ---
    'standards.tree.title-new-section': (
        'Nouvelle section', 'New section', 'Nueva sección', 'قسم جديد',
        '新しいセクション', '新建章节'),
    'standards.tree.title-edit-section': (
        'Modifier la section', 'Edit the section', 'Modificar la sección',
        'تعديل القسم', 'セクションを編集', '编辑章节'),
    'standards.tree.title-new-clause': (
        'Nouvelle clause', 'New clause', 'Nueva cláusula', 'بند جديد',
        '新しい箇条', '新建条款'),
    'standards.tree.title-edit-clause': (
        'Modifier la clause', 'Edit the clause', 'Modificar la cláusula',
        'تعديل البند', '箇条を編集', '编辑条款'),
    'standards.tree.title-new-requirement': (
        'Nouvelle exigence', 'New requirement', 'Nuevo requisito', 'متطلب جديد',
        '新しい要求事項', '新建要求'),
    'standards.tree.title-edit-requirement': (
        "Modifier l'exigence", 'Edit the requirement', 'Modificar el requisito',
        'تعديل المتطلب', '要求事項を編集', '编辑要求'),

    # --- suppressions : la question NOMME ce qui part ---
    'standards.tree.confirm-section': (
        'Supprimer la section {$code} et les {$count} clauses qu\'elle contient ?',
        'Delete section {$code} and the {$count} clauses it contains?',
        '¿Eliminar la sección {$code} y las {$count} cláusulas que contiene?',
        'حذف القسم {$code} و{$count} من البنود التي يحتويها؟',
        'セクション {$code} と、そこに含まれる {$count} 件の箇条を削除しますか？',
        '删除章节 {$code} 及其包含的 {$count} 个条款？'),
    'standards.tree.confirm-clause': (
        'Supprimer la clause {$code} et les {$count} exigences qu\'elle contient ?',
        'Delete clause {$code} and the {$count} requirements it contains?',
        '¿Eliminar la cláusula {$code} y los {$count} requisitos que contiene?',
        'حذف البند {$code} و{$count} من المتطلبات التي يحتويها؟',
        '箇条 {$code} と、そこに含まれる {$count} 件の要求事項を削除しますか？',
        '删除条款 {$code} 及其包含的 {$count} 条要求？'),
    'standards.tree.confirm-requirement': (
        "Supprimer l'exigence {$code} ?", 'Delete requirement {$code}?',
        '¿Eliminar el requisito {$code}?', 'حذف المتطلب {$code}؟',
        '要求事項 {$code} を削除しますか？', '删除要求 {$code}？'),

    # --- refus du serveur, traduits par le geste qu'ils appellent ---
    'standards.tree.error-conflict': (
        'Ce code est déjà pris à ce niveau.', 'This code is already taken at this level.',
        'Este código ya está ocupado en este nivel.', 'هذا الرمز مستخدم بالفعل في هذا المستوى.',
        'このコードはこの階層で既に使われています。', '该编号在此层级已被占用。'),
    'standards.tree.error-platform': (
        'Une norme de la plateforme ne se modifie pas.',
        'A platform standard cannot be modified.', 'Una norma de la plataforma no se modifica.',
        'لا يمكن تعديل معيار المنصة.', 'プラットフォームの規格は変更できません。',
        '平台标准不可修改。'),
    'standards.tree.error-generic': (
        'Modification impossible pour le moment.',
        'Modification is not possible at the moment.',
        'La modificación no es posible por el momento.', 'التعديل غير ممكن في الوقت الحالي.',
        '現在変更できません。', '当前无法修改。'),

    # --- generation de la checklist d'audit depuis un referentiel ---
    'audits.detail.from-standard': (
        'Générer depuis un référentiel', 'Generate from a referential',
        'Generar desde un referencial', 'إنشاء من مرجع', '基準から生成', '从基准生成'),
    'audits.from-standard.title': (
        'Générer la checklist depuis un référentiel',
        'Generate the checklist from a referential',
        'Generar la lista de verificación desde un referencial',
        'إنشاء قائمة التحقق من مرجع', '基準からチェックリストを生成', '从基准生成检查清单'),
    'audits.from-standard.subtitle': (
        'Chaque exigence du référentiel devient une question. Les questions sont copiées : '
        "le référentiel pourra évoluer ensuite sans réécrire cet audit.",
        'Each requirement of the referential becomes a question. The questions are copied: '
        'the referential can evolve later without rewriting this audit.',
        'Cada requisito del referencial se convierte en una pregunta. Las preguntas se '
        'copian: el referencial podrá evolucionar después sin reescribir esta auditoría.',
        'يصبح كل متطلب في المرجع سؤالاً. الأسئلة منسوخة: يمكن للمرجع أن يتطور لاحقاً دون إعادة كتابة هذا التدقيق.',
        '基準の各要求事項が質問になります。質問は複製されるため、基準が後で変わってもこの監査は書き換わりません。',
        '基准的每条要求都会成为一个问题。问题是复制的：基准之后可以演进，而无需重写本次审核。'),
    'audits.from-standard.submit': (
        'Générer', 'Generate', 'Generar', 'إنشاء', '生成', '生成'),
    'audits.from-standard.standard': (
        'Référentiel', 'Referential', 'Referencial', 'المرجع', '基準', '基准'),
    'audits.from-standard.owned': (
        '(procédure interne)', '(internal procedure)', '(procedimiento interno)',
        '(إجراء داخلي)', '（社内手順）', '（内部程序）'),
    'audits.from-standard.standard-required': (
        'Choisissez le référentiel à auditer.', 'Choose the referential to audit.',
        'Elija el referencial que se va a auditar.', 'اختر المرجع المراد تدقيقه.',
        '監査する基準を選択してください。', '请选择要审核的基准。'),
    'audits.from-standard.none': (
        'Aucun référentiel disponible. Adoptez une norme du catalogue, ou créez un '
        'référentiel depuis une procédure approuvée de votre GED.',
        'No referential available. Adopt a standard from the catalogue, or create a '
        'referential from an approved procedure in your document control.',
        'Ningún referencial disponible. Adopte una norma del catálogo o cree un referencial '
        'a partir de un procedimiento aprobado de su gestión documental.',
        'لا يوجد مرجع متاح. اعتمد معياراً من الكتالوج، أو أنشئ مرجعاً من إجراء معتمد في إدارة الوثائق.',
        '利用できる基準がありません。カタログから規格を採用するか、文書管理の承認済み手順から基準を作成してください。',
        '没有可用的基准。请从目录中采用一项标准，或从文档管理中已批准的程序创建一个基准。'),
    'audits.from-standard.created': (
        '{$count} questions générées.', '{$count} questions generated.',
        '{$count} preguntas generadas.', 'تم إنشاء {$count} سؤالاً.',
        '{$count} 件の質問を生成しました。', '已生成 {$count} 个问题。'),
    'audits.from-standard.empty': (
        'Ce référentiel ne contient encore aucune exigence.',
        'This referential does not contain any requirement yet.',
        'Este referencial todavía no contiene ningún requisito.',
        'لا يحتوي هذا المرجع على أي متطلب بعد.', 'この基準にはまだ要求事項がありません。',
        '该基准尚未包含任何要求。'),
    'audits.from-standard.conflict': (
        "La checklist n'est pas vide, ou cet audit n'est plus au stade de la préparation.",
        'The checklist is not empty, or this audit is no longer being prepared.',
        'La lista de verificación no está vacía, o esta auditoría ya no está en preparación.',
        'قائمة التحقق ليست فارغة، أو لم يعد هذا التدقيق في مرحلة التحضير.',
        'チェックリストが空でないか、この監査は準備段階を過ぎています。',
        '检查清单不为空，或本次审核已不在准备阶段。'),
    'audits.from-standard.not-found': (
        'Ce référentiel est introuvable.', 'This referential cannot be found.',
        'No se encuentra este referencial.', 'تعذر العثور على هذا المرجع.',
        'この基準が見つかりません。', '找不到该基准。'),
    'audits.from-standard.error': (
        'Génération impossible pour le moment.',
        'Generation is not possible at the moment.',
        'La generación no es posible por el momento.', 'الإنشاء غير ممكن في الوقت الحالي.',
        '現在生成できません。', '当前无法生成。'),

    # --- libelle partage ---
    'common.none': ('—', '—', '—', '—', '—', '—'),
}
