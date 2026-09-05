# -*- coding: utf-8 -*-
"""Table i18n - referentiel Produit, PFMEA, Control Plan et revisions proposees.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {

    # --- navigation --------------------------------------------------
    'nav.produits': ('Produits', 'Products', 'Productos', 'المنتجات', '製品', '产品'),

    # --- commun ------------------------------------------------------
    'common.edit-aria': ('Modifier', 'Edit', 'Modificar', 'تعديل', '編集', '编辑'),
    'common.delete-aria': ('Supprimer', 'Delete', 'Eliminar', 'حذف', '削除', '删除'),

    # --- produit : liste ---------------------------------------------
    'product.list-title': ('Produits', 'Products', 'Productos', 'المنتجات', '製品', '产品'),
    'product.list-subtitle': ("Le sujet du PFMEA et du control plan : ce qu'on fabrique, sa nomenclature et sa gamme.", 'The subject of the PFMEA and the control plan: what is made, its bill of materials and its routing.', 'El objeto del PFMEA y del plan de control: lo que se fabrica, su lista de materiales y su ruta.', 'موضوع تحليل PFMEA وخطة التحكم: ما يُصنَّع، وقائمة موادّه، ومسار تصنيعه.', 'PFMEA と管理計画書の対象：何を作るか、その部品表と工程順序。', 'PFMEA 与控制计划的对象：所制造的产品、其物料清单与工艺路线。'),
    'product.list-empty': ("Aucun produit. Créez-en un pour bâtir son PFMEA et son control plan.", 'No product yet. Create one to build its PFMEA and its control plan.', 'Ningún producto. Cree uno para construir su PFMEA y su plan de control.', 'لا توجد منتجات. أنشئ منتجًا لبناء تحليل PFMEA وخطة التحكم الخاصة به.', '製品がありません。PFMEA と管理計画書を作成するには、まず製品を登録してください。', '暂无产品。请先创建产品，再建立其 PFMEA 与控制计划。'),
    'product.list-failed': ('Impossible de charger les produits.', 'Products could not be loaded.', 'No se han podido cargar los productos.', 'تعذّر تحميل المنتجات.', '製品を読み込めませんでした。', '无法加载产品。'),
    'product.new': ('Nouveau produit', 'New product', 'Nuevo producto', 'منتج جديد', '新規製品', '新建产品'),
    'product.filter-status': ('Statut', 'Status', 'Estado', 'الحالة', 'ステータス', '状态'),
    'product.filter-all': ('Tous', 'All', 'Todos', 'الكل', 'すべて', '全部'),
    'product.col-code': ('Référence', 'Reference', 'Referencia', 'المرجع', '品番', '编号'),
    'product.col-designation': ('Désignation', 'Designation', 'Designación', 'التسمية', '品名', '名称'),
    'product.col-family': ('Famille', 'Family', 'Familia', 'العائلة', '製品群', '系列'),
    'product.col-status': ('Statut', 'Status', 'Estado', 'الحالة', 'ステータス', '状态'),
    'product.col-revisions': ('À réviser', 'To review', 'Por revisar', 'بحاجة إلى مراجعة', '要見直し', '待修订'),
    'product.edit-aria': ('Modifier le produit', 'Edit the product', 'Modificar el producto', 'تعديل المنتج', '製品を編集', '编辑产品'),

    # --- produit : fiche ---------------------------------------------
    'product.detail-failed': ('Produit introuvable.', 'Product not found.', 'Producto no encontrado.', 'المنتج غير موجود.', '製品が見つかりません。', '未找到该产品。'),
    'product.activate': ('Activer', 'Activate', 'Activar', 'تفعيل', '有効化', '启用'),
    # Extraction Excel du dossier produit : le PFMEA et le plan de surveillance
    # dans un seul classeur, ce qu'un audit client réclame et ce qu'on ne peut
    # pas transmettre sans sortir de la plateforme.
    'product.export-xlsx': (
        'Exporter (Excel)', 'Export (Excel)', 'Exportar (Excel)',
        'تصدير (Excel)', 'エクスポート（Excel）', '导出（Excel）'),
    'product.export-failed': (
        'Export impossible.', 'Export failed.', 'No se pudo exportar.',
        'تعذّر التصدير.', 'エクスポートできませんでした。', '导出失败。'),

    'product.obsolete': ('Rendre obsolète', 'Mark obsolete', 'Marcar como obsoleto', 'جعله ملغى', '廃止にする', '标记为作废'),
    'product.no-revision': ('Rien en attente', 'Nothing pending', 'Nada pendiente', 'لا شيء قيد الانتظار', '保留中なし', '无待处理项'),
    'product.tab-summary': ('Synthèse', 'Summary', 'Síntesis', 'الملخّص', '概要', '概览'),
    'product.tab-bom': ('Nomenclature', 'Bill of materials', 'Lista de materiales', 'قائمة المواد', '部品表', '物料清单'),
    'product.tab-routing': ('Gamme', 'Routing', 'Ruta de fabricación', 'مسار التصنيع', '工程順序', '工艺路线'),
    'product.tab-pfmea': ('PFMEA', 'PFMEA', 'PFMEA', 'PFMEA', 'PFMEA', 'PFMEA'),
    'product.tab-control-plan': ('Control Plan', 'Control plan', 'Plan de control', 'خطة التحكم', '管理計画書', '控制计划'),
    'product.tab-nc': ('NC liées', 'Linked NCs', 'NC vinculadas', 'حالات عدم المطابقة المرتبطة', '関連する不適合', '关联的不合格'),
    'product.tab-revisions': ('Révisions proposées', 'Proposed revisions', 'Revisiones propuestas', 'المراجعات المقترحة', '提案された改訂', '建议的修订'),

    # --- produit : nomenclature et gamme -----------------------------
    'product.bom-empty': ('Aucun composant déclaré.', 'No component declared.', 'Ningún componente declarado.', 'لم يُعلَن عن أي مكوّن.', '構成部品が登録されていません。', '尚未声明任何组件。'),
    'product.bom-failed': ('Nomenclature indisponible.', 'Bill of materials unavailable.', 'Lista de materiales no disponible.', 'قائمة المواد غير متاحة.', '部品表を取得できません。', '物料清单不可用。'),
    'product.routing-empty': ("Aucune opération. C'est la gamme qui relie le PFMEA au control plan.", 'No operation yet. The routing is what ties the PFMEA to the control plan.', 'Ninguna operación. La ruta de fabricación es lo que une el PFMEA con el plan de control.', 'لا توجد عمليات. مسار التصنيع هو ما يربط تحليل PFMEA بخطة التحكم.', '工程がありません。PFMEA と管理計画書をつなぐのは工程順序です。', '暂无工序。工艺路线正是连接 PFMEA 与控制计划的纽带。'),
    'product.routing-failed': ('Gamme indisponible.', 'Routing unavailable.', 'Ruta de fabricación no disponible.', 'مسار التصنيع غير متاح.', '工程順序を取得できません。', '工艺路线不可用。'),
    'product.component-new': ('Ajouter un composant', 'Add a component', 'Añadir un componente', 'إضافة مكوّن', '構成部品を追加', '添加组件'),
    'product.component-edit': ('Modifier le composant', 'Edit the component', 'Modificar el componente', 'تعديل المكوّن', '構成部品を編集', '编辑组件'),
    'product.operation-new': ('Ajouter une opération', 'Add an operation', 'Añadir una operación', 'إضافة عملية', '工程を追加', '添加工序'),
    'product.operation-edit': ("Modifier l'opération", 'Edit the operation', 'Modificar la operación', 'تعديل العملية', '工程を編集', '编辑工序'),
    'product.operation-conflict': ("Ce code d'opération existe déjà sur ce produit.", 'This operation code already exists on this product.', 'Este código de operación ya existe en este producto.', 'رمز العملية هذا موجود بالفعل لهذا المنتج.', 'この工程コードはこの製品に既に存在します。', '该工序代码在此产品中已存在。'),

    # --- produit : formulaires ---------------------------------------
    'product.dialog-new': ('Nouveau produit', 'New product', 'Nuevo producto', 'منتج جديد', '新規製品', '新建产品'),
    'product.dialog-edit': ('Modifier le produit', 'Edit the product', 'Modificar el producto', 'تعديل المنتج', '製品を編集', '编辑产品'),
    'product.field-code': ('Référence', 'Reference', 'Referencia', 'المرجع', '品番', '编号'),
    'product.field-code-hint': ("Lettres, chiffres, point, tiret ou souligné. Elle ne se modifiera plus.", 'Letters, digits, dot, hyphen or underscore. It cannot be changed afterwards.', 'Letras, cifras, punto, guion o guion bajo. Ya no podrá modificarse.', 'حروف وأرقام ونقطة وشرطة أو شرطة سفلية. لن يكون بالإمكان تعديله لاحقًا.', '英数字、ピリオド、ハイフン、アンダースコア。登録後は変更できません。', '字母、数字、点、连字符或下划线。创建后不可再修改。'),
    'product.field-designation': ('Désignation', 'Designation', 'Designación', 'التسمية', '品名', '名称'),
    'product.field-family': ('Famille', 'Family', 'Familia', 'العائلة', '製品群', '系列'),
    'product.field-revision': ('Indice', 'Revision index', 'Índice', 'رقم المراجعة', '改訂記号', '版本号'),
    'product.field-customer': ('Client', 'Customer', 'Cliente', 'العميل', '顧客', '客户'),
    'product.field-site': ('Site', 'Site', 'Centro', 'الموقع', '拠点', '工厂'),
    'product.field-sequence': ('Rang', 'Rank', 'Orden', 'الترتيب', '順序', '序号'),
    'product.field-reference': ('Référence', 'Reference', 'Referencia', 'المرجع', '品番', '编号'),
    'product.field-label': ('Libellé', 'Label', 'Etiqueta', 'التسمية', '名称', '名称'),
    'product.field-quantity': ('Quantité', 'Quantity', 'Cantidad', 'الكمية', '数量', '数量'),
    'product.field-unit': ('Unité', 'Unit', 'Unidad', 'الوحدة', '単位', '单位'),
    'product.field-op-code': ('Code', 'Code', 'Código', 'الرمز', 'コード', '代码'),
    'product.field-workstation': ('Poste', 'Workstation', 'Puesto', 'محطة العمل', '工程設備', '工位'),
    'product.save-failed': ('Enregistrement impossible.', 'Could not save.', 'No se ha podido guardar.', 'تعذّر الحفظ.', '保存できませんでした。', '无法保存。'),
    'product.delete-failed': ('Suppression impossible.', 'Could not delete.', 'No se ha podido eliminar.', 'تعذّر الحذف.', '削除できませんでした。', '无法删除。'),
    'product.code-conflict': ('Cette référence est déjà utilisée.', 'This reference is already in use.', 'Esta referencia ya está en uso.', 'هذا المرجع مستخدم بالفعل.', 'この品番は既に使用されています。', '该编号已被使用。'),

    # --- produit : onglet NC -----------------------------------------
    'product.nc-empty': ('Aucune non-conformité rattachée à ce produit.', 'No non-conformity linked to this product.', 'Ninguna no conformidad vinculada a este producto.', 'لا توجد حالات عدم مطابقة مرتبطة بهذا المنتج.', 'この製品に紐づく不適合はありません。', '尚无与该产品关联的不合格。'),
    'product.nc-failed': ('Non-conformités indisponibles.', 'Non-conformities unavailable.', 'No conformidades no disponibles.', 'حالات عدم المطابقة غير متاحة.', '不適合を取得できません。', '不合格数据不可用。'),
    'product.nc-unexplained': ("Défauts que l'analyse n'explique pas", 'Defects the analysis does not explain', 'Defectos que el análisis no explica', 'عيوب لا يفسّرها التحليل', '分析で説明できない不具合', '分析未能解释的缺陷'),
    'product.nc-unexplained-hint': ("Aucun mode de défaillance ne leur est rattaché : soit le PFMEA en ignore un, soit le rattachement reste à faire.", 'No failure mode is linked to them: either the PFMEA is missing one, or the link has yet to be made.', 'No tienen ningún modo de fallo asociado: o el PFMEA omite uno, o el vínculo está pendiente.', 'لا يرتبط بها أي نمط فشل: إمّا أن تحليل PFMEA يغفل نمطًا، وإمّا أن الربط لم يُنجَز بعد.', '故障モードが紐づいていません。PFMEA に抜けがあるか、紐づけがまだ行われていないかのどちらかです。', '它们没有关联任何失效模式：要么 PFMEA 遗漏了某一项，要么关联尚未建立。'),
    'product.nc-explained': ('Défauts rattachés à un mode de défaillance', 'Defects linked to a failure mode', 'Defectos vinculados a un modo de fallo', 'عيوب مرتبطة بنمط فشل', '故障モードに紐づいた不具合', '已关联失效模式的缺陷'),
    'product.nc-col-ref': ('Référence', 'Reference', 'Referencia', 'المرجع', '番号', '编号'),
    'product.nc-col-title': ('Intitulé', 'Title', 'Título', 'العنوان', '件名', '标题'),
    'product.nc-col-severity': ('Gravité', 'Severity', 'Gravedad', 'الخطورة', '重大度', '严重度'),
    'product.nc-col-detected': ('Détectée le', 'Detected on', 'Detectada el', 'تاريخ الاكتشاف', '検出日', '发现日期'),

    # --- PFMEA -------------------------------------------------------
    'pfmea.load-failed': ('PFMEA indisponible.', 'PFMEA unavailable.', 'PFMEA no disponible.', 'تحليل PFMEA غير متاح.', 'PFMEA を取得できません。', 'PFMEA 不可用。'),
    'pfmea.no-project': ("Aucun PFMEA rattaché à ce produit. Créez-en un depuis le module FMEA, puis rattachez-le.", 'No PFMEA linked to this product. Create one in the FMEA module, then link it.', 'Ningún PFMEA vinculado a este producto. Cree uno en el módulo FMEA y vincúlelo.', 'لا يوجد تحليل PFMEA مرتبط بهذا المنتج. أنشئ واحدًا من وحدة FMEA ثم اربطه.', 'この製品に紐づく PFMEA がありません。FMEA モジュールで作成してから紐づけてください。', '该产品尚未关联 PFMEA。请在 FMEA 模块中创建后再进行关联。'),
    'pfmea.no-item': ("Ce PFMEA ne contient encore aucune ligne d'analyse.", 'This PFMEA contains no analysis line yet.', 'Este PFMEA todavía no contiene ninguna línea de análisis.', 'لا يحتوي تحليل PFMEA هذا على أي سطر تحليل بعد.', 'この PFMEA にはまだ分析行がありません。', '该 PFMEA 尚无任何分析行。'),
    'pfmea.revision': ('révision {$INTERPOLATION}', 'revision {$INTERPOLATION}', 'revisión {$INTERPOLATION}', 'المراجعة {$INTERPOLATION}', '改訂 {$INTERPOLATION}', '第 {$INTERPOLATION} 版'),
    'pfmea.col-failure-mode': ('Mode de défaillance', 'Failure mode', 'Modo de fallo', 'نمط الفشل', '故障モード', '失效模式'),
    'pfmea.col-sod': ('S / O / D', 'S / O / D', 'S / O / D', 'S / O / D', 'S / O / D', 'S / O / D'),
    'pfmea.col-rpn': ('RPN', 'RPN', 'NPR', 'RPN', 'RPN', 'RPN'),
    'pfmea.col-ap': ("Priorité d'action", 'Action priority', 'Prioridad de acción', 'أولوية الإجراء', '処置優先度', '措施优先级'),
    'pfmea.not-rated': ('non cotée', 'not rated', 'sin puntuar', 'غير مُقيَّم', '未評価', '未评分'),
    'pfmea.col-flag': ('Révision', 'Revision', 'Revisión', 'المراجعة', '改訂', '修订'),
    'pfmea.flag-tooltip': ('Une révision est proposée sur cette ligne', 'A revision is proposed on this line', 'Se propone una revisión en esta línea', 'تُقترح مراجعة على هذا السطر', 'この行に改訂が提案されています', '此行有修订建议'),

    # --- control plan ------------------------------------------------
    'controlplan.load-failed': ('Control plan indisponible.', 'Control plan unavailable.', 'Plan de control no disponible.', 'خطة التحكم غير متاحة.', '管理計画書を取得できません。', '控制计划不可用。'),
    'controlplan.none': ("Aucun control plan. C'est lui qui traduit le PFMEA en contrôles réellement exécutés au poste.", 'No control plan. It is what turns the PFMEA into checks actually performed at the workstation.', 'Ningún plan de control. Es lo que convierte el PFMEA en controles realmente ejecutados en el puesto.', 'لا توجد خطة تحكم. هي ما يحوّل تحليل PFMEA إلى عمليات فحص تُنفَّذ فعليًا في محطة العمل.', '管理計画書がありません。PFMEA を工程で実際に行う検査へ翻訳するのがこの文書です。', '暂无控制计划。它正是把 PFMEA 转化为工位上真正执行的检验的文件。'),
    'controlplan.no-line': ('Ce plan ne contient encore aucune ligne.', 'This plan contains no line yet.', 'Este plan todavía no contiene ninguna línea.', 'لا تحتوي هذه الخطة على أي سطر بعد.', 'この計画書にはまだ行がありません。', '该计划尚无任何行。'),
    'controlplan.plans-aria': ('Plans', 'Plans', 'Planes', 'الخطط', '計画書', '计划'),
    'controlplan.new': ('Nouveau brouillon', 'New draft', 'Nuevo borrador', 'مسودة جديدة', '新規ドラフト', '新建草稿'),
    'controlplan.draft-exists': ('Un brouillon existe déjà pour cette phase.', 'A draft already exists for this phase.', 'Ya existe un borrador para esta fase.', 'توجد مسودة بالفعل لهذه المرحلة.', 'この段階のドラフトは既に存在します。', '该阶段已存在草稿。'),
    'controlplan.open-revision': ('Ouvrir une révision', 'Open a revision', 'Abrir una revisión', 'فتح مراجعة', '改訂を開く', '开启修订'),
    'controlplan.revision-failed': ('Ouverture de révision impossible.', 'Could not open a revision.', 'No se ha podido abrir la revisión.', 'تعذّر فتح مراجعة.', '改訂を開けませんでした。', '无法开启修订。'),
    'controlplan.approve': ('Approuver', 'Approve', 'Aprobar', 'اعتماد', '承認', '批准'),
    'controlplan.approve-failed': ('Approbation impossible.', 'Could not approve.', 'No se ha podido aprobar.', 'تعذّر الاعتماد.', '承認できませんでした。', '无法批准。'),
    'controlplan.add-line': ('Ajouter une ligne', 'Add a line', 'Añadir una línea', 'إضافة سطر', '行を追加', '添加行'),
    'controlplan.line-new': ('Ajouter une ligne', 'Add a line', 'Añadir una línea', 'إضافة سطر', '行を追加', '添加行'),
    'controlplan.line-edit': ('Modifier la ligne', 'Edit the line', 'Modificar la línea', 'تعديل السطر', '行を編集', '编辑行'),
    'controlplan.seal-title': ('Document scellé et ancré', 'Sealed and anchored document', 'Documento sellado y anclado', 'مستند مختوم وموثّق', '封印・アンカー済みの文書', '已封存并上链的文件'),
    'controlplan.seal-hash': ('Empreinte', 'Fingerprint', 'Huella', 'البصمة', 'ハッシュ', '指纹'),
    'controlplan.seal-tx': ('Transaction', 'Transaction', 'Transacción', 'المعاملة', 'トランザクション', '交易'),
    'controlplan.field-sample-size-hint': ("Une règle, pas seulement un nombre : « 100 % », « 5 au réglage puis 1 sur 50 ».", 'A rule, not just a number: "100%", "5 at setup then 1 in 50".', 'Una regla, no solo un número: «100 %», «5 al ajuste y luego 1 de cada 50».', 'قاعدة وليست رقمًا فقط: «100٪»، «5 عند الضبط ثم 1 من كل 50».', '数値だけでなく規則も可：「100%」「段取り時5個、以降50個に1個」。', '不只是数字，也可以是规则：“100%”、“调机时5件，之后每50件1件”。'),
    'controlplan.field-sop': ('Référence de procédure', 'Procedure reference', 'Referencia de procedimiento', 'مرجع الإجراء', '手順書番号', '程序编号'),
    'controlplan.field-flow': ('Entrée ou sortie', 'Input or output', 'Entrada o salida', 'مدخل أم مخرج', '入力か出力か', '输入或输出'),
    'controlplan.field-flow-none': ('Non précisé', 'Not specified', 'Sin precisar', 'غير محدد', '未指定', '未指定'),
    'controlplan.field-flow-hint': ("Contrôler une entrée empêche le défaut ; contrôler une sortie le constate.", 'Checking an input prevents the defect; checking an output records it.', 'Controlar una entrada evita el defecto; controlar una salida lo constata.', 'فحص المدخل يمنع العيب، وفحص المخرج يسجّله فقط.', '入力の管理は不良を防ぎ、出力の管理は不良を確認するにとどまります。', '控制输入可预防缺陷，控制输出只是发现缺陷。'),
    'controlplan.field-who': ('Qui ou quoi mesure', 'Who or what measures', 'Quién o qué mide', 'من أو ما الذي يقيس', '測定者または測定手段', '由谁或由何测量'),
    'controlplan.field-recording': ("Lieu d'enregistrement", 'Recording location', 'Lugar de registro', 'مكان التسجيل', '記録の保管場所', '记录存放位置'),
    'controlplan.field-recording-hint': ("Où l'auditeur ira chercher la preuve que le contrôle a eu lieu.", 'Where the auditor will look for evidence that the check took place.', 'Donde el auditor buscará la prueba de que el control se realizó.', 'حيث سيبحث المدقق عن دليل على إجراء الفحص.', '検査が行われた証拠を監査員が探す場所です。', '审核员将在此查找检验已执行的证据。'),
    'controlplan.col-measured-by': ('Qui mesure', 'Measured by', 'Quién mide', 'من يقيس', '測定者', '测量方'),
    'controlplan.locked': ('Ce plan est approuvé : ouvrez une révision pour le modifier.', 'This plan is approved: open a revision to change it.', 'Este plan está aprobado: abra una revisión para modificarlo.', 'هذه الخطة معتمدة: افتح مراجعة لتعديلها.', 'この計画書は承認済みです。変更するには改訂を開いてください。', '该计划已批准：如需修改，请开启修订。'),
    'controlplan.col-characteristic': (
        "Ce qui est contrôlé", "What's controlled", 'Qué se controla',
        'ما يخضع للمراقبة', '管理対象', '受控对象'),
    'controlplan.col-spec': ('Spécification', 'Specification', 'Especificación', 'المواصفة', '規格', '规范'),
    'controlplan.col-control': (
        'Méthode de contrôle', 'Method of control', 'Método de control',
        'طريقة الضبط', '管理方法', '控制方法'),
    'controlplan.col-reaction': (
        'Décision / action corrective', 'Decision / corrective action',
        'Decisión / acción correctiva', 'القرار / الإجراء التصحيحي',
        '判定・是正処置', '判定 / 纠正措施'),
    # --- les colonnes de la trame qui n'etaient pas affichees ----------------
    # Elles existaient en base depuis la V116 mais restaient invisibles,
    # regroupees sous des en-tetes de synthese. Une colonne du document = une
    # colonne d'ecran.
    'controlplan.col-sop': (
        'Procédure', 'Procedure', 'Procedimiento',
        'الإجراء', '手順書', '作业指导书'),
    'controlplan.col-process-step': (
        'Étape du procédé', 'Process step', 'Etapa del proceso',
        'خطوة العملية', '工程ステップ', '过程步骤'),
    'controlplan.col-input-output': (
        'Entrée / sortie', 'Input / output', 'Entrada / salida',
        'مدخل / مخرج', '入力・出力', '输入 / 输出'),
    'controlplan.col-specified': (
        'Caractéristique spécifiée', 'Specification characteristic',
        'Característica especificada', 'الخاصية المواصفة',
        '規格特性', '规范特性'),
    'controlplan.col-measurement': (
        'Moyen de mesure', 'Method of measurement', 'Método de medición',
        'وسيلة القياس', '測定方法', '测量方法'),
    'controlplan.col-sample-size': (
        'Échantillon', 'Sample size', 'Tamaño de muestra',
        'حجم العينة', 'サンプル数', '样本量'),
    'controlplan.col-frequency': (
        'Fréquence', 'Frequency', 'Frecuencia',
        'التكرار', '頻度', '频次'),
    'controlplan.col-recording': (
        'Enregistrement', 'Recording location', 'Registro',
        'مكان الحفظ', '記録場所', '记录位置'),
    'controlplan.field-specified': (
        'Caractéristique spécifiée', 'Specification characteristic',
        'Característica especificada', 'الخاصية المواصفة',
        '規格特性', '规范特性'),
    'controlplan.field-specified-placeholder': (
        'Ex. : cote de coupe ; hauteur de sertissage',
        'E.g. cut length; crimp height',
        'Ej.: longitud de corte; altura de engaste',
        'مثال: طول القص؛ ارتفاع الكبس',
        '例：切断寸法、圧着高さ',
        '例：下料长度；压接高度'),
    'controlplan.col-justification': ('Justification', 'Justification', 'Justificación', 'المبرّر', '根拠', '依据'),
    'controlplan.justified': ('PFMEA', 'PFMEA', 'PFMEA', 'PFMEA', 'PFMEA', 'PFMEA'),
    'controlplan.unjustified': ('sans justification', 'unjustified', 'sin justificación', 'بلا مبرّر', '根拠なし', '无依据'),
    'controlplan.field-operation': ('Opération', 'Operation', 'Operación', 'العملية', '工程', '工序'),
    'controlplan.field-none': ('Aucune', 'None', 'Ninguna', 'لا شيء', 'なし', '无'),
    'controlplan.field-characteristic': (
        "Ce qui est contrôlé", "What's controlled", 'Qué se controla',
        'ما يخضع للمراقبة', '管理対象', '受控对象'),
    'controlplan.field-type': ('Type', 'Type', 'Tipo', 'النوع', '種別', '类型'),
    'controlplan.field-class': ('Classement', 'Classification', 'Clasificación', 'التصنيف', '区分', '分类'),
    'controlplan.field-spec': ('Spécification', 'Specification', 'Especificación', 'المواصفة', '規格', '规范'),
    'controlplan.field-tol-lower': ('Tolérance mini', 'Lower tolerance', 'Tolerancia mínima', 'الحد الأدنى للتفاوت', '下限公差', '下公差'),
    'controlplan.field-tol-upper': ('Tolérance maxi', 'Upper tolerance', 'Tolerancia máxima', 'الحد الأعلى للتفاوت', '上限公差', '上公差'),
    'controlplan.field-machine': ('Moyen', 'Equipment', 'Medio', 'الوسيلة', '設備', '设备'),
    'controlplan.field-technique': ('Technique de mesure', 'Measurement technique', 'Técnica de medición', 'أسلوب القياس', '測定方法', '测量方法'),
    'controlplan.field-sample-size': ("Taille d'échantillon", 'Sample size', 'Tamaño de muestra', 'حجم العيّنة', 'サンプルサイズ', '样本量'),
    'controlplan.field-frequency': ('Fréquence', 'Frequency', 'Frecuencia', 'التكرار', '頻度', '频次'),
    'controlplan.field-method': ('Méthode de contrôle', 'Control method', 'Método de control', 'طريقة الفحص', '管理方法', '控制方法'),
    'controlplan.field-reaction': ('Plan de réaction', 'Reaction plan', 'Plan de reacción', 'خطة الاستجابة', '異常時の処置', '反应计划'),
    'controlplan.field-fmea': ('Ligne de PFMEA qui justifie ce contrôle', 'PFMEA line that justifies this control', 'Línea de PFMEA que justifica este control', 'سطر PFMEA الذي يبرّر هذا الفحص', 'この管理の根拠となる PFMEA の行', '为该控制提供依据的 PFMEA 行'),
    'controlplan.field-no-justification': ("Aucune — ce contrôle n'est justifié par aucune analyse", 'None — this control is justified by no analysis', 'Ninguna — este control no se justifica con ningún análisis', 'لا شيء — لا يستند هذا الفحص إلى أي تحليل', 'なし — この管理はいかなる分析にも基づいていません', '无 — 此控制没有任何分析依据'),

    # --- propositions de revision ------------------------------------
    'revision.load-failed': ('Propositions indisponibles.', 'Proposals unavailable.', 'Propuestas no disponibles.', 'الاقتراحات غير متاحة.', '提案を取得できません。', '建议不可用。'),
    'revision.none': ('Aucune révision proposée. Les non-conformités et les CAPA closes en déposent ici.', 'No revision proposed. Non-conformities and closed CAPAs file theirs here.', 'Ninguna revisión propuesta. Las no conformidades y las CAPA cerradas las depositan aquí.', 'لا توجد مراجعات مقترحة. حالات عدم المطابقة وملفات CAPA المغلقة تودع اقتراحاتها هنا.', '提案された改訂はありません。不適合と是正処置の完了がここに提案を残します。', '暂无修订建议。不合格与已关闭的 CAPA 会在此提出建议。'),
    'revision.accept': ('Accepter', 'Accept', 'Aceptar', 'قبول', '承認', '接受'),
    'revision.reject': ('Refuser', 'Reject', 'Rechazar', 'رفض', '却下', '拒绝'),
    'revision.note': ('Motif de refus', 'Reason for rejection', 'Motivo del rechazo', 'سبب الرفض', '却下理由', '拒绝理由'),
    'revision.note-hint': ("Un refus sans motif écrit est l'écart que l'auditeur cherche.", 'A rejection with no written reason is exactly the finding an auditor looks for.', 'Un rechazo sin motivo escrito es justamente el hallazgo que busca un auditor.', 'الرفض دون سبب مكتوب هو تمامًا ما يبحث عنه المدقّق.', '理由を書かない却下こそ、監査員が探している指摘です。', '没有书面理由的拒绝，正是审核员要找的不符合项。'),
    'revision.accept-failed': ('Acceptation impossible.', 'Could not accept.', 'No se ha podido aceptar.', 'تعذّر القبول.', '承認できませんでした。', '无法接受。'),
    'revision.reject-failed': ('Refus impossible.', 'Could not reject.', 'No se ha podido rechazar.', 'تعذّر الرفض.', '却下できませんでした。', '无法拒绝。'),
    'revision.already-decided': ('Cette proposition a déjà été tranchée.', 'This proposal has already been decided.', 'Esta propuesta ya se ha resuelto.', 'تم البتّ في هذا الاقتراح مسبقًا.', 'この提案は既に判断済みです。', '该建议已作出决定。'),
    'revision.create-pfmea': ('Créer une ligne de PFMEA', 'Create a PFMEA line', 'Crear una línea de PFMEA', 'إنشاء سطر PFMEA', 'PFMEA の行を作成', '新建一条 PFMEA 行'),
    'revision.create-line': ('Créer une ligne de control plan', 'Create a control plan line', 'Crear una línea del plan de control', 'إنشاء سطر في خطة التحكم', '管理計画書の行を作成', '新建一条控制计划行'),

    # --- non-conformite : produit et mode de defaillance --------------
    'nc.create.product': ('Produit concerné', 'Product concerned', 'Producto afectado', 'المنتج المعني', '対象製品', '相关产品'),
    'nc.create.no-product': ('Aucun — cette NC ne vise pas un produit', 'None — this NC does not target a product', 'Ninguno — esta NC no se refiere a un producto', 'لا شيء — لا تخصّ حالة عدم المطابقة هذه منتجًا', 'なし — この不適合は製品を対象としていません', '无 — 此不合格不针对产品'),
    'nc.create.failure-mode': ('Mode de défaillance du PFMEA', 'PFMEA failure mode', 'Modo de fallo del PFMEA', 'نمط الفشل في PFMEA', 'PFMEA の故障モード', 'PFMEA 失效模式'),
    'nc.create.suggest': ('Proposer', 'Suggest', 'Proponer', 'اقتراح', '候補を出す', '提出建议'),
    'nc.create.matched-terms': ('termes communs : {$INTERPOLATION}', 'shared terms: {$INTERPOLATION}', 'términos comunes: {$INTERPOLATION}', 'المصطلحات المشتركة: {$INTERPOLATION}', '共通する語：{$INTERPOLATION}', '共同词项：{$INTERPOLATION}'),
    'nc.create.none-matches': ('Aucun mode ne correspond', 'No failure mode matches', 'Ningún modo coincide', 'لا يطابق أي نمط', '該当する故障モードなし', '没有匹配的失效模式'),
    'nc.create.none-matches-hint': ("Un défaut que l'analyse n'avait pas prévu : une ligne de PFMEA sera proposée.", 'A defect the analysis had not foreseen: a PFMEA line will be proposed.', 'Un defecto que el análisis no había previsto: se propondrá una línea de PFMEA.', 'عيب لم يتوقّعه التحليل: سيُقترح إضافة سطر إلى PFMEA.', '分析が想定していなかった不具合です。PFMEA の行が提案されます。', '分析未曾预见的缺陷：系统将建议新增一条 PFMEA 行。'),
    'nc.create.no-suggestion': ("Aucune suggestion pour ce texte. « Aucun mode ne correspond » reste un choix valable.", 'No suggestion for this text. "No failure mode matches" remains a valid answer.', 'Ninguna sugerencia para este texto. «Ningún modo coincide» sigue siendo una respuesta válida.', 'لا توجد اقتراحات لهذا النص. يبقى خيار «لا يطابق أي نمط» صالحًا.', 'この文章に対する候補はありません。「該当する故障モードなし」も正当な回答です。', '针对此文本没有候选项。“没有匹配的失效模式”仍是有效答案。'),

    # --- second facteur -----------------------------------------------
    'stepup.required': ('Cette signature exige votre code à usage unique.', 'This signature requires your one-time code.', 'Esta firma exige su código de un solo uso.', 'يتطلّب هذا التوقيع رمزك ذا الاستخدام الواحد.', 'この署名にはワンタイムコードが必要です。', '此签名需要您的一次性验证码。'),
    'stepup.reauthenticate': ('Se réauthentifier', 'Re-authenticate', 'Volver a autenticarse', 'إعادة المصادقة', '再認証する', '重新认证'),
    'stepup.unavailable': ('Second facteur indisponible sur cet environnement.', 'Second factor unavailable in this environment.', 'Segundo factor no disponible en este entorno.', 'العامل الثاني غير متاح في هذه البيئة.', 'この環境では第二要素を利用できません。', '此环境不支持第二因素。'),

    # --- CAPA : impact sur les documents ------------------------------
    'capa.impact-title': ('Impact PFMEA / Control Plan', 'PFMEA / control plan impact', 'Impacto PFMEA / plan de control', 'الأثر على PFMEA وخطة التحكم', 'PFMEA・管理計画書への影響', '对 PFMEA / 控制计划的影响'),
    'capa.impact-subtitle': ('Ce que la clôture de ce dossier a proposé de réviser.', 'What closing this case proposed to revise.', 'Lo que el cierre de este caso propuso revisar.', 'ما اقترحه إغلاق هذا الملف من مراجعات.', 'この案件の完了が改訂を提案した内容。', '本案关闭后提出修订的内容。'),
}
