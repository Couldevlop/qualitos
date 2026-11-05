# -*- coding: utf-8 -*-
"""Table i18n - Natures d'écart (sous-catégories de NC), clôture DMAIC et
endiguement CAPA (ADR 0103).
id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    # --- navigation et écran de paramétrage ---------------------------------
    'nav.admin-nc': (
        'Natures de NC', 'NC natures', 'Naturalezas de NC',
        'أنواع حالات عدم المطابقة', 'NC の性質', '不符合性质'),
    'nc.settings.title': (
        'Natures de non-conformité', 'Non-conformity natures', 'Naturalezas de no conformidad',
        'أنواع حالات عدم المطابقة', '不適合の性質', '不符合性质'),
    'nc.settings.subtitle': (
        "Sous chaque catégorie, les natures d'écart que vous rencontrez.",
        'Under each category, the natures of deviation you encounter.',
        'Bajo cada categoría, las naturalezas de desviación que encuentra.',
        'تحت كل فئة، أنواع الانحراف التي تواجهها.',
        '各カテゴリーの下に、実際に発生する逸脱の性質を並べます。',
        '在每个类别下，列出您实际遇到的偏差性质。'),
    'nc.settings.scope': (
        "Une nature se reconnaît à son code : en changer le libellé ne déclasse pas les écarts déjà "
        "classés ; la retirer laisse les anciens porter leur code.",
        'A nature is identified by its code: changing its label does not unclassify deviations already '
        'classified; removing it leaves the old ones carrying their code.',
        'Una naturaleza se reconoce por su código: cambiar su etiqueta no desclasifica las desviaciones '
        'ya clasificadas; retirarla deja que las antiguas conserven su código.',
        'يُعرَف النوع برمزه: تغيير تسميته لا يُلغي تصنيف الانحرافات المصنَّفة سابقًا؛ وحذفه يُبقي القديمة '
        'حاملةً لرمزها.',
        '性質はコードで識別されます。名称を変えても既存の分類は失われず、削除しても過去のものはコードを保持します。',
        '性质以其代码识别：更改名称不会使已分类的偏差失去分类；删除后，旧记录仍保留其代码。'),
    'nc.settings.defaults': (
        'Ce sont aujourd’hui les natures de la plateforme, transverses à tous les secteurs.',
        'These are currently the platform natures, common to every sector.',
        'Actualmente son las naturalezas de la plataforma, transversales a todos los sectores.',
        'هذه حاليًا أنواع المنصة، وهي مشتركة بين جميع القطاعات.',
        '現在はプラットフォーム標準の性質で、すべての業種に共通です。',
        '目前使用的是平台自带的性质，适用于所有行业。'),
    'nc.settings.catalogue': ('Catalogue', 'Catalogue', 'Catálogo', 'الدليل', 'カタログ', '目录'),
    'nc.settings.catalogue-help': (
        'Les natures de chaque catégorie, dans l’ordre où la saisie les proposera.',
        'The natures of each category, in the order the entry form will offer them.',
        'Las naturalezas de cada categoría, en el orden en que el formulario las propondrá.',
        'أنواع كل فئة، بالترتيب الذي سيعرضها به نموذج الإدخال.',
        '各カテゴリーの性質を、入力画面で提示される順に並べます。',
        '各类别的性质，按录入界面提示的顺序排列。'),
    'nc.settings.categories': ('Catégories', 'Categories', 'Categorías', 'الفئات', 'カテゴリー', '类别'),
    'nc.settings.code': ('Code', 'Code', 'Código', 'الرمز', 'コード', '代码'),
    'nc.settings.code-placeholder': (
        'Ex. : PRD-DIM', 'E.g. PRD-DIM', 'Ej.: PRD-DIM', 'مثال: PRD-DIM',
        '例: PRD-DIM', '例：PRD-DIM'),
    'nc.settings.label': (
        "Nature de l'écart", 'Nature of the deviation', 'Naturaleza de la desviación',
        'نوع الانحراف', '逸脱の性質', '偏差性质'),
    'nc.settings.label-placeholder': (
        'Ex. : Cote ou tolérance hors spécification',
        'E.g. Dimension or tolerance out of specification',
        'Ej.: Cota o tolerancia fuera de especificación',
        'مثال: مقاس أو سماحية خارج المواصفة',
        '例: 寸法または公差が規格外',
        '例：尺寸或公差超出规格'),
    'nc.settings.add': (
        'Ajouter une nature', 'Add a nature', 'Añadir una naturaleza',
        'إضافة نوع', '性質を追加', '添加性质'),
    'nc.settings.remove': (
        'Retirer cette nature', 'Remove this nature', 'Retirar esta naturaleza',
        'إزالة هذا النوع', 'この性質を削除', '删除该性质'),
    'nc.settings.category-empty': (
        'Aucune nature pour cette catégorie : la saisie n’y proposera rien.',
        'No nature for this category: the entry form will offer nothing here.',
        'Ninguna naturaleza para esta categoría: el formulario no propondrá nada aquí.',
        'لا يوجد نوع لهذه الفئة: لن يعرض نموذج الإدخال شيئًا هنا.',
        'このカテゴリーには性質がありません。入力画面では何も提示されません。',
        '该类别下没有性质：录入界面将无可选项。'),
    'nc.settings.pending': (
        'Catalogue modifié', 'Catalogue changed', 'Catálogo modificado',
        'تم تعديل الدليل', 'カタログを変更しました', '目录已修改'),

    # --- règles locales du paramétrage --------------------------------------
    'nc.settings.problem.empty': (
        'Le catalogue ne peut pas être vide. Rétablissez celui de la plateforme.',
        'The catalogue cannot be empty. Restore the platform one.',
        'El catálogo no puede estar vacío. Restablezca el de la plataforma.',
        'لا يمكن أن يكون الدليل فارغًا. استعد دليل المنصة.',
        'カタログを空にはできません。プラットフォームのカタログを復元してください。',
        '目录不能为空。请恢复平台目录。'),
    'nc.settings.problem.too-many': (
        'Pas plus de {$max} natures.', 'No more than {$max} natures.',
        'No más de {$max} naturalezas.', 'لا أكثر من {$max} نوعًا.',
        '性質は最大 {$max} 件までです。', '性质最多 {$max} 条。'),
    'nc.settings.problem.code': (
        'Un code s’écrit en capitales, chiffres, tiret ou point (ex. : PRD-DIM).',
        'A code is written in capitals, digits, hyphen or dot (e.g. PRD-DIM).',
        'Un código se escribe en mayúsculas, cifras, guion o punto (ej.: PRD-DIM).',
        'يُكتَب الرمز بحروف كبيرة وأرقام وشَرطة أو نقطة (مثال: PRD-DIM).',
        'コードは大文字・数字・ハイフン・ピリオドで記述します（例: PRD-DIM）。',
        '代码使用大写字母、数字、连字符或点号（例：PRD-DIM）。'),
    'nc.settings.problem.label': (
        'Chaque nature a un libellé.', 'Every nature has a label.',
        'Cada naturaleza tiene una etiqueta.', 'لكل نوع تسمية.',
        'すべての性質に名称が必要です。', '每条性质都需要名称。'),
    'nc.settings.problem.duplicate': (
        'Deux natures portent le même code.', 'Two natures carry the same code.',
        'Dos naturalezas llevan el mismo código.', 'نوعان يحملان الرمز نفسه.',
        '2 つの性質が同じコードを持っています。', '有两条性质使用了相同的代码。'),
    'nc.settings.problem.empty-category': (
        'Catégorie sans aucune nature : {$count} — la saisie n’y proposerait rien.',
        'Category without any nature: {$count} — the entry form would offer nothing there.',
        'Categoría sin ninguna naturaleza: {$count} — el formulario no propondría nada.',
        'فئة بلا أي نوع: {$count} — لن يعرض نموذج الإدخال شيئًا فيها.',
        '性質が 1 つもないカテゴリー: {$count} — 入力画面では何も提示されません。',
        '没有任何性质的类别：{$count} — 录入界面将无可选项。'),

    # --- enregistrement et rétablissement -----------------------------------
    'nc.settings.load-failed': (
        'Le catalogue des natures n’a pas pu être chargé.',
        'The nature catalogue could not be loaded.',
        'No se ha podido cargar el catálogo de naturalezas.',
        'تعذّر تحميل دليل الأنواع.',
        '性質カタログを読み込めませんでした。', '无法加载性质目录。'),
    'nc.settings.save-failed': (
        'Le catalogue n’a pas pu être enregistré.', 'The catalogue could not be saved.',
        'No se ha podido guardar el catálogo.', 'تعذّر حفظ الدليل.',
        'カタログを保存できませんでした。', '无法保存目录。'),
    'nc.settings.saved': (
        'Catalogue des natures enregistré.', 'Nature catalogue saved.',
        'Catálogo de naturalezas guardado.', 'تم حفظ دليل الأنواع.',
        '性質カタログを保存しました。', '性质目录已保存。'),
    'nc.settings.reset-action': (
        'Catalogue de la plateforme', 'Platform catalogue', 'Catálogo de la plataforma',
        'دليل المنصة', 'プラットフォームのカタログ', '平台目录'),
    'nc.settings.reset': ('Rétablir', 'Restore', 'Restablecer', 'استعادة', '復元', '恢复'),
    'nc.settings.reset-title': (
        'Rétablir le catalogue de la plateforme ?', 'Restore the platform catalogue?',
        '¿Restablecer el catálogo de la plataforma?', 'استعادة دليل المنصة؟',
        'プラットフォームのカタログを復元しますか？', '要恢复平台目录吗？'),
    'nc.settings.reset-message': (
        'Votre catalogue sera remplacé par celui de la plateforme. Les écarts déjà classés gardent '
        'leur code.',
        'Your catalogue will be replaced by the platform one. Deviations already classified keep '
        'their code.',
        'Su catálogo será reemplazado por el de la plataforma. Las desviaciones ya clasificadas '
        'conservan su código.',
        'سيُستبدَل دليلك بدليل المنصة. تحتفظ الانحرافات المصنَّفة سابقًا برمزها.',
        'お客様のカタログはプラットフォームのものに置き換わります。既に分類済みの逸脱はコードを保持します。',
        '您的目录将被平台目录替换。已分类的偏差保留其代码。'),
    'nc.settings.reset-done': (
        'Catalogue de la plateforme rétabli.', 'Platform catalogue restored.',
        'Catálogo de la plataforma restablecido.', 'تمت استعادة دليل المنصة.',
        'プラットフォームのカタログを復元しました。', '已恢复平台目录。'),
    'nc.settings.reset-failed': (
        'Le catalogue n’a pas pu être rétabli.', 'The catalogue could not be restored.',
        'No se ha podido restablecer el catálogo.', 'تعذّرت استعادة الدليل.',
        'カタログを復元できませんでした。', '无法恢复目录。'),

    # --- saisie et registre des NC ------------------------------------------
    'nc.create.subcategory': (
        "Nature de l'écart", 'Nature of the deviation', 'Naturaleza de la desviación',
        'نوع الانحراف', '逸脱の性質', '偏差性质'),
    'nc.create.subcategory-none': (
        '— À classer —', '— To be classified —', '— Por clasificar —',
        '— للتصنيف —', '— 未分類 —', '— 待分类 —'),
    'nc.list.filter-subcategory': ('Nature', 'Nature', 'Naturaleza', 'النوع', '性質', '性质'),

    # --- non-conformité fournisseur ouverte depuis le registre transverse ---
    'suppliers.nc.supplier-required': (
        'Choisissez le fournisseur concerné.', 'Choose the supplier concerned.',
        'Elija el proveedor afectado.', 'اختر المورّد المعني.',
        '対象のサプライヤーを選択してください。', '请选择相关供应商。'),

    # --- endiguement CAPA et clôture DMAIC ----------------------------------
    'capa.open.containment-hint': (
        "Mesure immédiate : elle contient l'effet. Le dossier reste correctif — sa clôture demandera "
        'une action qui supprime la cause.',
        'Immediate measure: it contains the effect. The case stays corrective — closing it will '
        'require an action that removes the cause.',
        'Medida inmediata: contiene el efecto. El expediente sigue siendo correctivo — su cierre '
        'exigirá una acción que elimine la causa.',
        'إجراء فوري: يحتوي الأثر. يبقى الملف تصحيحيًا — وسيقتضي إغلاقه إجراءً يُزيل السبب.',
        '即時措置：影響を封じ込めます。案件は是正のままで、終結には原因を除去する処置が必要です。',
        '即时措施：遏制影响。案卷仍为纠正性——结案需要一项消除原因的措施。'),
    'dmaic.detail.complete-hint': (
        'Le projet se clôture quand toute la fiche est renseignée et que chaque action est à 100 %.',
        'The project closes once the whole sheet is filled in and every action is at 100%.',
        'El proyecto se cierra cuando toda la ficha está completa y cada acción está al 100 %.',
        'يُغلَق المشروع عندما تكون البطاقة كاملة وكل إجراء بنسبة 100٪.',
        'シートがすべて記入され、各アクションが 100 % になった時点でプロジェクトを終結できます。',
        '当整张表单填写完毕且每项措施达到 100 % 时，项目方可结案。'),
    'dmaic.detail.complete': (
        'Clôturer',
        'Close',
        'Cerrar',
        'إغلاق',
        '終結',
        '结案'),
    'dmaic.res.missing-marked': (
        'champ(s) à renseigner, signalés en rouge ci-dessous.',
        'field(s) to fill in, marked in red below.',
        'campo(s) a rellenar, señalados en rojo más abajo.',
        'حقل/حقول يجب تعبئتها، مُعلَّمة بالأحمر أدناه.',
        '件の未入力項目があり、以下で赤く示されています。',
        '处待填写字段，已在下方以红色标出。'),
}
