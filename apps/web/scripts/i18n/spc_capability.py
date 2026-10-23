# -*- coding: utf-8 -*-
"""Table i18n - section SPC : cartes de controle (etudes Cp / Cpk, ADR 0096) et Gage R&R.
id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    # ----- menu -----
    'nav.spc-section': ('SPC', 'SPC', 'SPC', 'SPC', 'SPC', 'SPC'),
    'nav.spc-control-charts': ('Cartes de contrôle', 'Control charts', 'Gráficos de control', 'مخططات التحكم', '管理図', '控制图'),
    'nav.spc-gage-rr': ('Gage R&R', 'Gage R&R', 'Gage R&R', 'Gage R&R', 'Gage R&R', 'Gage R&R'),
    'nav.admin-spc': ('Paramètres SPC', 'SPC settings', 'Parámetros SPC', 'إعدادات SPC', 'SPC 設定', 'SPC 设置'),
    'spc.gage-rr.title': ('Gage R&R', 'Gage R&R', 'Gage R&R', 'Gage R&R', 'Gage R&R', 'Gage R&R'),

    # ----- droits -----
    'authz.perm.spc-manage': (
        'Saisir et supprimer les études de capabilité SPC',
        'Record and delete SPC capability studies',
        'Registrar y eliminar estudios de capacidad SPC',
        'تسجيل دراسات قدرة SPC وحذفها',
        'SPC 工程能力調査の登録と削除',
        '录入和删除 SPC 能力研究'),
    'authz.module.spc': ('SPC', 'SPC', 'SPC', 'SPC', 'SPC', 'SPC'),

    # ----- verdicts -----
    'spc.status.capable': ('Maîtrisé', 'Under control', 'Bajo control', 'تحت السيطرة', '管理状態', '受控'),
    'spc.status.not-capable': ('Non maîtrisé', 'Not under control', 'Fuera de control', 'خارج السيطرة', '管理外', '不受控'),
    'spc.status.watch': ('À surveiller', 'To watch', 'A vigilar', 'تحت المراقبة', '要監視', '需关注'),

    # ----- colonnes -----
    'spc.col.part': ('Référence pièce', 'Part reference', 'Referencia de pieza', 'مرجع القطعة', '部品番号', '零件编号'),
    'spc.col.characteristic': ('Caractéristique', 'Characteristic', 'Característica', 'الخاصية', '特性', '特性'),
    'spc.col.specification': ('Spécification', 'Specification', 'Especificación', 'المواصفة', '仕様', '规格'),
    'spc.col.leader': ('Meneur', 'Study lead', 'Responsable del estudio', 'قائد الدراسة', '調査責任者', '研究负责人'),
    'spc.col.date': ('Date', 'Date', 'Fecha', 'التاريخ', '日付', '日期'),
    'spc.col.machine': ('Machine', 'Machine', 'Máquina', 'الآلة', '設備', '设备'),
    'spc.col.status': ('Statut', 'Status', 'Estado', 'الحالة', 'ステータス', '状态'),
    'spc.col.plan': ("Plan d'actions", 'Action plan', 'Plan de acción', 'خطة العمل', '処置計画', '行动计划'),
    'spc.col.plan-yn': ("Plan d'actions (O/N)", 'Action plan (Y/N)', 'Plan de acción (S/N)', 'خطة العمل (نعم/لا)', '処置計画（有/無）', '行动计划（是/否）'),
    'spc.col.actions': ('Actions', 'Actions', 'Acciones', 'الإجراءات', '操作', '操作'),

    # ----- registre -----
    'spc.register.eyebrow': ('SPC · Cartes de contrôle', 'SPC · Control charts', 'SPC · Gráficos de control', 'SPC · مخططات التحكم', 'SPC · 管理図', 'SPC · 控制图'),
    'spc.register.title': ('Registre des études SPC', 'SPC study register', 'Registro de estudios SPC', 'سجل دراسات SPC', 'SPC 調査台帳', 'SPC 研究登记簿'),
    'spc.register.subtitle': (
        "Toutes les études de capabilité (Cp / Cpk) réalisées. Cliquez sur une ligne pour rouvrir l'étude.",
        'Every capability study (Cp / Cpk) carried out. Click a row to reopen the study.',
        'Todos los estudios de capacidad (Cp / Cpk) realizados. Haga clic en una fila para volver a abrir el estudio.',
        'جميع دراسات القدرة (Cp / Cpk) المنجزة. انقر على سطر لإعادة فتح الدراسة.',
        '実施したすべての工程能力調査（Cp / Cpk）。行をクリックすると調査を再表示します。',
        '已完成的全部能力研究（Cp / Cpk）。单击一行可重新打开研究。'),
    'spc.register.export': ('Exporter CSV', 'Export CSV', 'Exportar CSV', 'تصدير CSV', 'CSV エクスポート', '导出 CSV'),
    'spc.register.new': ('Créer une étude SPC', 'Create an SPC study', 'Crear un estudio SPC', 'إنشاء دراسة SPC', 'SPC 調査を作成', '创建 SPC 研究'),
    'spc.register.kpis': ('Indicateurs du registre', 'Register indicators', 'Indicadores del registro', 'مؤشرات السجل', '台帳の指標', '登记簿指标'),
    'spc.register.kpi-total': ('Études réalisées', 'Studies carried out', 'Estudios realizados', 'الدراسات المنجزة', '実施した調査', '已完成研究'),
    'spc.register.kpi-controlled': ('Procédés maîtrisés', 'Processes under control', 'Procesos bajo control', 'العمليات تحت السيطرة', '管理状態の工程', '受控过程'),
    'spc.register.kpi-plans': ("Plans d'actions en cours", 'Action plans in progress', 'Planes de acción en curso', 'خطط العمل الجارية', '進行中の処置計画', '进行中的行动计划'),
    'spc.register.kpi-watch': ('Procédés à surveiller', 'Processes to watch', 'Procesos a vigilar', 'عمليات تحت المراقبة', '要監視の工程', '需关注的过程'),
    'spc.register.kpi-without-plan': (
        "Non maîtrisées sans plan d'actions", 'Not under control, no action plan', 'Fuera de control sin plan de acción',
        'خارج السيطرة دون خطة عمل', '処置計画のない管理外', '不受控且无行动计划'),
    'spc.register.search': (
        'Rechercher : pièce, meneur, machine…', 'Search: part, lead, machine…', 'Buscar: pieza, responsable, máquina…',
        'بحث: القطعة، القائد، الآلة…', '検索：部品、責任者、設備…', '搜索：零件、负责人、设备…'),
    'spc.register.filter-status': ('Filtrer par statut', 'Filter by status', 'Filtrar por estado', 'تصفية حسب الحالة', 'ステータスで絞り込む', '按状态筛选'),
    'spc.register.all-statuses': ('Tous les statuts', 'All statuses', 'Todos los estados', 'جميع الحالات', 'すべてのステータス', '全部状态'),
    'spc.register.filter-plan': (
        "Filtrer par plan d'actions", 'Filter by action plan', 'Filtrar por plan de acción',
        'تصفية حسب خطة العمل', '処置計画で絞り込む', '按行动计划筛选'),
    'spc.register.all-plans': (
        "Plan d'actions : tous", 'Action plan: all', 'Plan de acción: todos', 'خطة العمل: الكل', '処置計画：すべて', '行动计划：全部'),
    'spc.register.with-plan': ('Avec plan', 'With a plan', 'Con plan', 'مع خطة', '計画あり', '有计划'),
    'spc.register.without-plan': ('Sans plan', 'Without a plan', 'Sin plan', 'بدون خطة', '計画なし', '无计划'),
    'spc.register.load-failed': (
        "Le registre des études n'a pas pu être chargé.", 'The study register could not be loaded.',
        'No se ha podido cargar el registro de estudios.', 'تعذر تحميل سجل الدراسات.',
        '調査台帳を読み込めませんでした。', '无法加载研究登记簿。'),
    'spc.register.plan-failed': (
        "Le plan d'actions n'a pas pu être modifié.", 'The action plan could not be changed.',
        'No se ha podido modificar el plan de acción.', 'تعذر تعديل خطة العمل.',
        '処置計画を変更できませんでした。', '无法修改行动计划。'),
    'spc.register.open': ('Ouvrir', 'Open', 'Abrir', 'فتح', '開く', '打开'),
    'spc.register.delete-title': (
        "Supprimer l'étude {$part} ?", 'Delete study {$part}?', '¿Eliminar el estudio {$part}?',
        'حذف الدراسة {$part}؟', '調査 {$part} を削除しますか？', '删除研究 {$part}？'),
    'spc.register.delete-message': (
        "L'étude et ses mesures quittent le registre. La suppression est tracée dans le journal d'audit.",
        'The study and its measurements leave the register. The deletion is recorded in the audit log.',
        'El estudio y sus mediciones salen del registro. La eliminación queda registrada en el registro de auditoría.',
        'تُحذف الدراسة وقياساتها من السجل. ويُسجَّل الحذف في سجل التدقيق.',
        '調査とその測定値は台帳から削除されます。削除は監査ログに記録されます。',
        '该研究及其测量值将从登记簿中移除。删除操作会记录在审计日志中。'),
    'spc.register.delete-failed': (
        "L'étude n'a pas pu être supprimée.", 'The study could not be deleted.', 'No se ha podido eliminar el estudio.',
        'تعذر حذف الدراسة.', '調査を削除できませんでした。', '无法删除该研究。'),
    'spc.register.empty': (
        'Aucune étude pour le moment. Cliquez sur « Créer une étude SPC » pour commencer.',
        'No studies yet. Click “Create an SPC study” to get started.',
        'Todavía no hay estudios. Haga clic en «Crear un estudio SPC» para empezar.',
        'لا توجد دراسات بعد. انقر على «إنشاء دراسة SPC» للبدء.',
        'まだ調査はありません。「SPC 調査を作成」をクリックして開始してください。',
        '暂无研究。单击“创建 SPC 研究”开始。'),
    'spc.register.no-match': (
        'Aucune étude ne correspond aux filtres.', 'No study matches the filters.', 'Ningún estudio coincide con los filtros.',
        'لا توجد دراسة تطابق عوامل التصفية.', '条件に一致する調査はありません。', '没有符合筛选条件的研究。'),
    'spc.register.reset-filters': ('Effacer les filtres', 'Clear filters', 'Borrar filtros', 'مسح عوامل التصفية', '絞り込みを解除', '清除筛选'),
    'spc.register.legend': (
        'Statut et Cpk en vert : maîtrisé · en jaune : à surveiller · en rouge : non maîtrisé. Le liseré rouge signale une étude à surveiller ou non maîtrisée sans plan d\'actions.',
        'Status and Cpk in green: under control · yellow: to watch · red: not under control. A red edge flags a study to watch or not under control with no action plan.',
        'Estado y Cpk en verde: bajo control · en amarillo: a vigilar · en rojo: fuera de control. El borde rojo señala un estudio a vigilar o fuera de control sin plan de acción.',
        'الحالة و Cpk بالأخضر: تحت السيطرة · بالأصفر: تحت المراقبة · بالأحمر: خارج السيطرة. يشير الحد الأحمر إلى دراسة تحت المراقبة أو خارج السيطرة دون خطة عمل.',
        '状態と Cpk 緑：管理状態 · 黄：要監視 · 赤：管理外。赤い縁取りは処置計画のない要監視または管理外の調査を示します。',
        '状态和 Cpk 绿色：受控 · 黄色：需关注 · 红色：不受控。红色边线表示需关注或不受控且无行动计划的研究。'),

    # ----- fiche d'étude -----
    'spc.study.title-new': (
        'Nouvelle étude de capabilité', 'New capability study', 'Nuevo estudio de capacidad',
        'دراسة قدرة جديدة', '新しい工程能力調査', '新建能力研究'),
    'spc.study.title-edit': (
        'Étude de capabilité {$part}', 'Capability study {$part}', 'Estudio de capacidad {$part}',
        'دراسة القدرة {$part}', '工程能力調査 {$part}', '能力研究 {$part}'),
    'spc.study.subtitle': (
        'Cartes de contrôle X̄ – R. Saisissez les mesures dans les cellules jaunes ; tout le reste se calcule.',
        'X̄ – R control charts. Enter the measurements in the yellow cells; everything else is calculated.',
        'Gráficos de control X̄ – R. Introduzca las mediciones en las celdas amarillas; todo lo demás se calcula.',
        'مخططات التحكم X̄ – R. أدخل القياسات في الخلايا الصفراء؛ ويُحسب كل ما تبقى تلقائياً.',
        'X̄ – R 管理図。黄色のセルに測定値を入力してください。その他はすべて自動計算されます。',
        'X̄ – R 控制图。在黄色单元格中输入测量值，其余内容自动计算。'),
    'spc.study.back': ('Registre', 'Register', 'Registro', 'السجل', '台帳', '登记簿'),
    'spc.study.save': ("Enregistrer l'étude", 'Save the study', 'Guardar el estudio', 'حفظ الدراسة', '調査を保存', '保存研究'),
    'spc.study.saved': ('Étude enregistrée.', 'Study saved.', 'Estudio guardado.', 'تم حفظ الدراسة.', '調査を保存しました。', '研究已保存。'),
    'spc.study.save-failed': (
        "L'étude n'a pas pu être enregistrée.", 'The study could not be saved.', 'No se ha podido guardar el estudio.',
        'تعذر حفظ الدراسة.', '調査を保存できませんでした。', '无法保存该研究。'),
    'spc.study.load-failed': (
        "L'étude n'a pas pu être chargée.", 'The study could not be loaded.', 'No se ha podido cargar el estudio.',
        'تعذر تحميل الدراسة.', '調査を読み込めませんでした。', '无法加载该研究。'),
    'spc.study.not-found': ('Étude introuvable.', 'Study not found.', 'Estudio no encontrado.', 'الدراسة غير موجودة.', '調査が見つかりません。', '未找到该研究。'),
    'spc.study.read-only': (
        'Consultation seule : la saisie des études est réservée aux personnes qui en ont le droit.',
        'Read only: recording studies is reserved for people who have the right to do so.',
        'Solo consulta: el registro de estudios está reservado a las personas autorizadas.',
        'عرض فقط: تسجيل الدراسات مقصور على الأشخاص المخوَّلين.',
        '閲覧のみ：調査の入力は権限を持つ人に限られます。',
        '仅供查看：只有具备权限的人员才能录入研究。'),
    'spc.study.identification': ('Identification', 'Identification', 'Identificación', 'التعريف', '識別情報', '标识'),
    'spc.study.characteristic': ('Paramètre', 'Parameter', 'Parámetro', 'المعامل', 'パラメータ', '参数'),
    'spc.study.specification': ('Taille / tolérance', 'Size / tolerance', 'Medida / tolerancia', 'المقاس / التفاوت', '寸法 / 公差', '尺寸 / 公差'),
    'spc.study.machine': ('N° de machine', 'Machine no.', 'N.º de máquina', 'رقم الآلة', '設備番号', '设备编号'),
    'spc.study.lsl': ('LSL (limite inférieure)', 'LSL (lower limit)', 'LSL (límite inferior)', 'LSL (الحد الأدنى)', 'LSL（下限規格）', 'LSL（下限）'),
    'spc.study.usl': ('USL (limite supérieure)', 'USL (upper limit)', 'USL (límite superior)', 'USL (الحد الأعلى)', 'USL（上限規格）', 'USL（上限）'),
    'spc.study.subgroup-size': ('Taille du sous-groupe (n)', 'Subgroup size (n)', 'Tamaño del subgrupo (n)', 'حجم المجموعة الفرعية (n)', 'サブグループサイズ (n)', '子组大小 (n)'),
    'spc.study.part-required': (
        'La référence de la pièce est obligatoire.', 'The part reference is required.', 'La referencia de la pieza es obligatoria.',
        'مرجع القطعة إلزامي.', '部品番号は必須です。', '零件编号为必填项。'),
    'spc.study.date-required': (
        "La date de l'étude est obligatoire.", 'The study date is required.', 'La fecha del estudio es obligatoria.',
        'تاريخ الدراسة إلزامي.', '調査日は必須です。', '研究日期为必填项。'),
    'spc.study.lsl-required': (
        'La limite inférieure est obligatoire.', 'The lower limit is required.', 'El límite inferior es obligatorio.',
        'الحد الأدنى إلزامي.', '下限規格は必須です。', '下限为必填项。'),
    'spc.study.usl-required': (
        'La limite supérieure est obligatoire.', 'The upper limit is required.', 'El límite superior es obligatorio.',
        'الحد الأعلى إلزامي.', '上限規格は必須です。', '上限为必填项。'),
    'spc.study.measurements': ('Mesures par sous-groupe', 'Measurements by subgroup', 'Mediciones por subgrupo', 'القياسات حسب المجموعة الفرعية', 'サブグループ別の測定値', '按子组的测量值'),
    'spc.study.sample': ('Échantillon', 'Sample', 'Muestra', 'العينة', 'サンプル', '样本'),
    'spc.study.cell-aria': (
        'Échantillon {$sample}, sous-groupe {$subgroup}', 'Sample {$sample}, subgroup {$subgroup}',
        'Muestra {$sample}, subgrupo {$subgroup}', 'العينة {$sample}، المجموعة الفرعية {$subgroup}',
        'サンプル {$sample}、サブグループ {$subgroup}', '样本 {$sample}，子组 {$subgroup}'),
    'spc.study.clear': ('Vider les mesures', 'Clear the measurements', 'Vaciar las mediciones', 'مسح القياسات', '測定値をクリア', '清空测量值'),
    'spc.study.paste-tip': (
        "Astuce : collez un bloc copié depuis Excel dans une cellule pour remplir la grille d'un coup.",
        'Tip: paste a block copied from Excel into a cell to fill the grid in one go.',
        'Consejo: pegue en una celda un bloque copiado de Excel para rellenar la cuadrícula de una vez.',
        'نصيحة: الصق في خلية كتلة منسوخة من Excel لملء الشبكة دفعة واحدة.',
        'ヒント：Excel からコピーした範囲をセルに貼り付けると、表を一度に入力できます。',
        '提示：将从 Excel 复制的数据块粘贴到单元格中，即可一次填满表格。'),
    'spc.study.results': ('Résultats', 'Results', 'Resultados', 'النتائج', '結果', '结果'),
    'spc.study.threshold': ('seuil', 'threshold', 'umbral', 'العتبة', 'しきい値', '阈值'),
    'spc.study.grand-mean': ('Moyenne X̿', 'Mean X̿', 'Media X̿', 'المتوسط X̿', '平均 X̿', '均值 X̿'),
    'spc.study.sigma': ('Sigma (R̄ / d2)', 'Sigma (R̄ / d2)', 'Sigma (R̄ / d2)', 'سيغما (R̄ / d2)', 'シグマ (R̄ / d2)', '西格玛 (R̄ / d2)'),
    'spc.study.controlled': ('Procédé maîtrisé :', 'Process under control:', 'Proceso bajo control:', 'العملية تحت السيطرة:', '工程は管理状態：', '过程受控：'),
    'spc.study.not-controlled': ('Procédé non maîtrisé :', 'Process not under control:', 'Proceso fuera de control:', 'العملية خارج السيطرة:', '工程は管理外：', '过程不受控：'),
    'spc.study.watch': ('Procédé à surveiller :', 'Process to watch:', 'Proceso a vigilar:', 'العملية تحت المراقبة:', '工程は要監視：', '过程需关注：'),
    'spc.study.yes': ('OUI', 'YES', 'SÍ', 'نعم', 'はい', '是'),
    'spc.study.no': ('NON', 'NO', 'NO', 'لا', 'いいえ', '否'),
    'spc.study.need-subgroup': (
        'Saisissez au moins un sous-groupe complet.', 'Enter at least one complete subgroup.',
        'Introduzca al menos un subgrupo completo.', 'أدخل مجموعة فرعية كاملة واحدة على الأقل.',
        '少なくとも 1 つの完全なサブグループを入力してください。', '请至少输入一个完整的子组。'),
    'spc.study.hide-formulas': ('Masquer les formules', 'Hide the formulas', 'Ocultar las fórmulas', 'إخفاء الصيغ', '計算式を隠す', '隐藏公式'),
    'spc.study.show-formulas': ('Voir les formules', 'Show the formulas', 'Ver las fórmulas', 'عرض الصيغ', '計算式を表示', '显示公式'),
    'spc.study.xbar-chart': ('Carte X̄', 'X̄ chart', 'Gráfico X̄', 'مخطط X̄', 'X̄ 管理図', 'X̄ 控制图'),
    'spc.study.range-chart': ('Carte R', 'R chart', 'Gráfico R', 'مخطط R', 'R 管理図', 'R 控制图'),
    'spc.study.xbar-aria': (
        'Carte X̄ : moyenne de chaque sous-groupe et limites de contrôle',
        'X̄ chart: mean of each subgroup and control limits',
        'Gráfico X̄: media de cada subgrupo y límites de control',
        'مخطط X̄: متوسط كل مجموعة فرعية وحدود التحكم',
        'X̄ 管理図：各サブグループの平均と管理限界',
        'X̄ 控制图：各子组均值与控制限'),
    'spc.study.range-aria': (
        'Carte R : étendue de chaque sous-groupe et limites de contrôle',
        'R chart: range of each subgroup and control limits',
        'Gráfico R: rango de cada subgrupo y límites de control',
        'مخطط R: مدى كل مجموعة فرعية وحدود التحكم',
        'R 管理図：各サブグループの範囲と管理限界',
        'R 控制图：各子组极差与控制限'),
    'spc.study.distribution': ('Distribution et tolérance', 'Distribution and tolerance', 'Distribución y tolerancia', 'التوزيع والتفاوت', '分布と公差', '分布与公差'),
    'spc.study.error.part': (
        'La référence de la pièce est obligatoire (120 caractères au plus).',
        'The part reference is required (120 characters at most).',
        'La referencia de la pieza es obligatoria (120 caracteres como máximo).',
        'مرجع القطعة إلزامي (120 حرفاً كحد أقصى).',
        '部品番号は必須です（120 文字以内）。',
        '零件编号为必填项（最多 120 个字符）。'),
    'spc.study.error.date': (
        "La date de l'étude est obligatoire.", 'The study date is required.', 'La fecha del estudio es obligatoria.',
        'تاريخ الدراسة إلزامي.', '調査日は必須です。', '研究日期为必填项。'),
    'spc.study.error.limits': (
        'La limite supérieure (USL) doit dépasser la limite inférieure (LSL).',
        'The upper limit (USL) must be above the lower limit (LSL).',
        'El límite superior (USL) debe superar el límite inferior (LSL).',
        'يجب أن يتجاوز الحد الأعلى (USL) الحد الأدنى (LSL).',
        '上限規格 (USL) は下限規格 (LSL) より大きくなければなりません。',
        '上限 (USL) 必须大于下限 (LSL)。'),
    'spc.study.error.leader': (
        "Le meneur choisi n'a pas pu être retrouvé dans l'annuaire.",
        'The chosen study lead could not be found in the directory.',
        'No se ha encontrado en el directorio al responsable elegido.',
        'تعذر العثور على قائد الدراسة المختار في الدليل.',
        '選択した調査責任者がディレクトリに見つかりませんでした。',
        '在目录中找不到所选的研究负责人。'),
    'spc.study.error.measurements': (
        'Saisissez au moins un sous-groupe complet, avec des mesures qui ne soient pas toutes identiques.',
        'Enter at least one complete subgroup, with measurements that are not all identical.',
        'Introduzca al menos un subgrupo completo, con mediciones que no sean todas idénticas.',
        'أدخل مجموعة فرعية كاملة واحدة على الأقل، بقياسات ليست كلها متطابقة.',
        '少なくとも 1 つの完全なサブグループを、すべて同じではない測定値で入力してください。',
        '请至少输入一个完整的子组，且测量值不能全部相同。'),
    'spc.study.error.generic': (
        "Un champ de l'étude a été refusé : vérifiez la saisie.",
        'A field of the study was rejected: check what you entered.',
        'Se ha rechazado un campo del estudio: revise los datos introducidos.',
        'تم رفض أحد حقول الدراسة: تحقّق من الإدخال.',
        '調査の項目が受け付けられませんでした。入力内容を確認してください。',
        '研究中的某个字段被拒绝：请检查输入内容。'),

    # ----- formules -----
    'spc.formula.group-means': ('Moyennes et dispersion', 'Means and spread', 'Medias y dispersión', 'المتوسطات والتشتت', '平均とばらつき', '均值与离散'),
    'spc.formula.sum-means': ('Somme des X̄', 'Sum of X̄', 'Suma de X̄', 'مجموع X̄', 'X̄ の合計', 'X̄ 之和'),
    'spc.formula.group-capability': ('Capabilité', 'Capability', 'Capacidad', 'القدرة', '工程能力', '能力'),
    'spc.formula.usl': ('Limite supérieure', 'Upper limit', 'Límite superior', 'الحد الأعلى', '上限規格', '上限'),
    'spc.formula.lsl': ('Limite inférieure', 'Lower limit', 'Límite inferior', 'الحد الأدنى', '下限規格', '下限'),
    'spc.formula.group-xbar': ('Carte X̄', 'X̄ chart', 'Gráfico X̄', 'مخطط X̄', 'X̄ 管理図', 'X̄ 控制图'),
    'spc.formula.control-limit': ('Limite de contrôle', 'Control limit', 'Límite de control', 'حد التحكم', '管理限界', '控制限'),
    'spc.formula.group-range': ('Carte R', 'R chart', 'Gráfico R', 'مخطط R', 'R 管理図', 'R 控制图'),

    # ----- graphiques -----
    'spc.chart.no-data': ('Aucune donnée à tracer.', 'No data to plot.', 'No hay datos que trazar.', 'لا توجد بيانات للرسم.', '描画するデータがありません。', '没有可绘制的数据。'),
    'spc.chart.out-of-limits': (
        'Sous-groupes hors limites de contrôle : {$count}', 'Subgroups outside the control limits: {$count}',
        'Subgrupos fuera de los límites de control: {$count}', 'المجموعات الفرعية خارج حدود التحكم: {$count}',
        '管理限界外のサブグループ：{$count}', '超出控制限的子组：{$count}'),
    'spc.distribution.aria': (
        'Histogramme des mesures et limites de tolérance', 'Histogram of the measurements and tolerance limits',
        'Histograma de las mediciones y límites de tolerancia', 'المدرج التكراري للقياسات وحدود التفاوت',
        '測定値のヒストグラムと公差限界', '测量值直方图与公差限'),
    'spc.distribution.bar': (
        'Mesures dans cette classe : {$count}', 'Measurements in this bin: {$count}', 'Mediciones en esta clase: {$count}',
        'القياسات في هذه الفئة: {$count}', 'この階級の測定値：{$count}', '该组内的测量值：{$count}'),
    'spc.distribution.mean': ('moyenne', 'mean', 'media', 'المتوسط', '平均', '均值'),
    'spc.distribution.empty': (
        'Saisissez au moins un sous-groupe complet et des limites valides.',
        'Enter at least one complete subgroup and valid limits.',
        'Introduzca al menos un subgrupo completo y límites válidos.',
        'أدخل مجموعة فرعية كاملة واحدة على الأقل وحدوداً صالحة.',
        '少なくとも 1 つの完全なサブグループと有効な規格限界を入力してください。',
        '请至少输入一个完整的子组和有效的规格限。'),

    # ----- lectures -----
    'spc.reading.dispersion': ('Dispersion :', 'Spread:', 'Dispersión:', 'التشتت:', 'ばらつき：', '离散：'),
    'spc.reading.of-tolerance': ('de la tolérance', 'of the tolerance', 'de la tolerancia', 'من التفاوت', '（公差比）', '（占公差）'),
    'spc.reading.tolerance': ('tolérance', 'tolerance', 'tolerancia', 'التفاوت', '公差', '公差'),
    'spc.reading.centering': ('Centrage :', 'Centring:', 'Centrado:', 'التمركز:', '中心位置：', '对中：'),
    'spc.reading.centered': ('centré', 'centred', 'centrado', 'متمركز', '中央', '居中'),
    'spc.reading.off-center': ('décalé de', 'off by', 'desplazado un', 'منحرف بنسبة', 'ずれ', '偏移'),
    'spc.reading.centered-detail': (
        'La moyenne est au milieu de la tolérance.', 'The mean is in the middle of the tolerance.',
        'La media está en el centro de la tolerancia.', 'المتوسط في منتصف التفاوت.',
        '平均は公差の中央にあります。', '均值位于公差中间。'),
    'spc.reading.mean': ('Moyenne', 'Mean', 'Media', 'المتوسط', '平均', '均值'),
    'spc.reading.above': (
        'au-dessus du milieu, vers la USL', 'above the middle, towards the USL', 'por encima del centro, hacia el USL',
        'فوق المنتصف، نحو USL', '中央より上、USL 側', '高于中间值，偏向 USL'),
    'spc.reading.below': (
        'en dessous du milieu, vers la LSL', 'below the middle, towards the LSL', 'por debajo del centro, hacia el LSL',
        'تحت المنتصف، نحو LSL', '中央より下、LSL 側', '低于中间值，偏向 LSL'),
    'spc.reading.reading': ('Lecture :', 'Reading:', 'Lectura:', 'القراءة:', '判定：', '解读：'),
    'spc.reading.capable': (
        'Le procédé est capable : la dispersion tient largement dans la tolérance et la moyenne est bien placée.',
        'The process is capable: the spread fits well within the tolerance and the mean is well placed.',
        'El proceso es capaz: la dispersión cabe holgadamente en la tolerancia y la media está bien situada.',
        'العملية قادرة: التشتت يقع بارتياح ضمن التفاوت والمتوسط في موضع جيد.',
        '工程は能力があります：ばらつきは公差内に十分収まり、平均も適切な位置にあります。',
        '过程具备能力：离散充分落在公差内，均值位置良好。'),
    'spc.reading.recenter': (
        'La dispersion est suffisante, mais la moyenne est trop décalée : recentrer le procédé suffit à atteindre Cpk {$threshold}.',
        'The spread is good enough, but the mean is too far off: re-centring the process is enough to reach Cpk {$threshold}.',
        'La dispersión es suficiente, pero la media está demasiado desplazada: recentrar el proceso basta para alcanzar Cpk {$threshold}.',
        'التشتت كافٍ، لكن المتوسط منحرف أكثر من اللازم: تكفي إعادة تمركز العملية لبلوغ Cpk {$threshold}.',
        'ばらつきは十分ですが、平均のずれが大きすぎます。工程を中心に戻せば Cpk {$threshold} に達します。',
        '离散足够小，但均值偏移过大：将过程重新对中即可达到 Cpk {$threshold}。'),
    'spc.reading.reduce': (
        'La dispersion est trop grande pour la tolérance : il faut réduire la variabilité, puis vérifier le centrage.',
        'The spread is too wide for the tolerance: reduce the variability, then check the centring.',
        'La dispersión es demasiado grande para la tolerancia: hay que reducir la variabilidad y luego comprobar el centrado.',
        'التشتت كبير جداً بالنسبة إلى التفاوت: يجب تقليل التباين ثم التحقق من التمركز.',
        'ばらつきが公差に対して大きすぎます。変動を減らしてから中心位置を確認してください。',
        '离散相对于公差过大：需先降低变异，再检查对中。'),

    # ----- paramètres -----
    'spc.settings.title': ('Paramètres SPC', 'SPC settings', 'Parámetros SPC', 'إعدادات SPC', 'SPC 設定', 'SPC 设置'),
    'spc.settings.subtitle': (
        'Les seuils de Cpk qui classent les études de capabilité.', 'The Cpk thresholds that rank capability studies.',
        'Los umbrales de Cpk que clasifican los estudios de capacidad.', 'عتبات Cpk التي تصنّف دراسات القدرة.',
        '工程能力調査を判定する Cpk のしきい値。', '用于判定能力研究的 Cpk 阈值。'),
    'spc.settings.load-failed': (
        "Les paramètres SPC n'ont pas pu être chargés.", 'The SPC settings could not be loaded.',
        'No se han podido cargar los parámetros SPC.', 'تعذر تحميل إعدادات SPC.',
        'SPC 設定を読み込めませんでした。', '无法加载 SPC 设置。'),
    'spc.settings.save-failed': (
        "Les paramètres SPC n'ont pas pu être enregistrés.", 'The SPC settings could not be saved.',
        'No se han podido guardar los parámetros SPC.', 'تعذر حفظ إعدادات SPC.',
        'SPC 設定を保存できませんでした。', '无法保存 SPC 设置。'),
    'spc.settings.saved': (
        "Paramètres SPC enregistrés. Ils s'appliquent aux prochaines études.",
        'SPC settings saved. They apply to the next studies.',
        'Parámetros SPC guardados. Se aplican a los próximos estudios.',
        'تم حفظ إعدادات SPC. وهي تنطبق على الدراسات القادمة.',
        'SPC 設定を保存しました。次回以降の調査に適用されます。',
        'SPC 设置已保存，将适用于之后的研究。'),
    'spc.settings.scope': (
        "Les seuils s'appliquent aux études créées après l'enregistrement. Une étude garde ceux qu'elle avait à sa création : son verdict ne change pas après coup.",
        'The thresholds apply to studies created after saving. A study keeps the ones it had when it was created: its verdict does not change afterwards.',
        'Los umbrales se aplican a los estudios creados después de guardar. Un estudio conserva los que tenía al crearse: su veredicto no cambia a posteriori.',
        'تنطبق العتبات على الدراسات المنشأة بعد الحفظ. وتحتفظ كل دراسة بالعتبات التي كانت لها عند إنشائها: فلا يتغير حكمها لاحقاً.',
        'しきい値は保存後に作成される調査に適用されます。調査は作成時のしきい値を保持するため、判定が後から変わることはありません。',
        '阈值适用于保存后创建的研究。每项研究保留其创建时的阈值：其结论不会事后改变。'),
    'spc.settings.thresholds': ('Seuils de capabilité (Cpk)', 'Capability thresholds (Cpk)', 'Umbrales de capacidad (Cpk)', 'عتبات القدرة (Cpk)', '工程能力のしきい値 (Cpk)', '能力阈值 (Cpk)'),
    'spc.settings.thresholds-help': (
        "Au-dessus du seuil de maîtrise, le procédé est maîtrisé (vert). Entre les deux, il est à surveiller (jaune). En deçà du seuil de surveillance, il n'est pas maîtrisé (rouge).",
        'Above the control threshold, the process is under control (green). In between, it is to be watched (yellow). Below the watch threshold, it is not under control (red).',
        'Por encima del umbral de control, el proceso está bajo control (verde). Entre ambos, debe vigilarse (amarillo). Por debajo del umbral de vigilancia, está fuera de control (rojo).',
        'فوق عتبة السيطرة تكون العملية تحت السيطرة (أخضر). وبين العتبتين تكون تحت المراقبة (أصفر). وتحت عتبة المراقبة تكون خارج السيطرة (أحمر).',
        '管理しきい値以上なら工程は管理状態（緑）、その間は要監視（黄）、監視しきい値未満は管理外（赤）です。',
        '高于受控阈值时过程受控（绿色）；介于两者之间需关注（黄色）；低于关注阈值时不受控（红色）。'),
    'spc.settings.defaults': (
        "Ce sont aujourd'hui les seuils de la plateforme.", 'These are currently the platform thresholds.',
        'Actualmente son los umbrales de la plataforma.', 'هذه حالياً عتبات المنصة.',
        '現在はプラットフォームの既定しきい値です。', '当前使用的是平台默认阈值。'),
    'spc.settings.capable': ('Seuil de maîtrise', 'Control threshold', 'Umbral de control', 'عتبة السيطرة', '管理しきい値', '受控阈值'),
    'spc.settings.watch': ('Seuil de surveillance', 'Watch threshold', 'Umbral de vigilancia', 'عتبة المراقبة', '監視しきい値', '关注阈值'),
    'spc.settings.zone-watch': ('À surveiller', 'To watch', 'A vigilar', 'تحت المراقبة', '要監視', '需关注'),
    'spc.settings.summary': (
        'Non maîtrisé < {$watch} ≤ À surveiller < {$capable} ≤ Maîtrisé',
        'Not under control < {$watch} ≤ To watch < {$capable} ≤ Under control',
        'Fuera de control < {$watch} ≤ A vigilar < {$capable} ≤ Bajo control',
        'خارج السيطرة < {$watch} ≤ تحت المراقبة < {$capable} ≤ تحت السيطرة',
        '管理外 < {$watch} ≤ 要監視 < {$capable} ≤ 管理状態',
        '不受控 < {$watch} ≤ 需关注 < {$capable} ≤ 受控'),
    'spc.settings.pending': ('Paramètres modifiés', 'Settings changed', 'Parámetros modificados', 'تم تعديل الإعدادات', '設定が変更されています', '设置已修改'),
    'spc.settings.problem.range': (
        'Chaque seuil est un nombre de 0,01 à 10, avec deux décimales au plus.',
        'Each threshold is a number from 0.01 to 10, with two decimals at most.',
        'Cada umbral es un número de 0,01 a 10, con dos decimales como máximo.',
        'كل عتبة عدد من 0.01 إلى 10، بخانتين عشريتين كحد أقصى.',
        '各しきい値は 0.01〜10 の数値で、小数点以下 2 桁までです。',
        '每个阈值为 0.01 至 10 之间的数字，最多两位小数。'),
    'spc.settings.problem.order': (
        'Le seuil de surveillance doit rester sous le seuil de maîtrise.',
        'The watch threshold must stay below the control threshold.',
        'El umbral de vigilancia debe quedar por debajo del umbral de control.',
        'يجب أن تبقى عتبة المراقبة أدنى من عتبة السيطرة.',
        '監視しきい値は管理しきい値より小さくなければなりません。',
        '关注阈值必须低于受控阈值。'),
    'spc.example.title': ("Jeux d'exemple", 'Sample datasets', 'Conjuntos de ejemplo', 'مجموعات بيانات نموذجية', 'サンプルデータ', '示例数据集'),
    'spc.example.workbook': (
        'Exemple 1 — classeur : poids 500 ± 25 g (maîtrisé)',
        'Example 1 — workbook: weight 500 ± 25 g (under control)',
        'Ejemplo 1 — libro: peso 500 ± 25 g (bajo control)',
        'المثال 1 — المصنف: الوزن 500 ± 25 غ (تحت السيطرة)',
        '例 1 — ワークブック：重量 500 ± 25 g（管理状態）',
        '示例 1 — 工作簿：重量 500 ± 25 g（受控）'),
    'spc.example.turning': (
        'Exemple 2 — diamètre Ø 12 ± 0,1 mm (non maîtrisé)',
        'Example 2 — diameter Ø 12 ± 0.1 mm (not under control)',
        'Ejemplo 2 — diámetro Ø 12 ± 0,1 mm (fuera de control)',
        'المثال 2 — القطر Ø 12 ± 0.1 مم (خارج السيطرة)',
        '例 2 — 直径 Ø 12 ± 0.1 mm（管理外）',
        '示例 2 — 直径 Ø 12 ± 0.1 mm（不受控）'),
    'spc.example.confirm-title': (
        'Remplacer les mesures saisies ?', 'Replace the entered measurements?',
        '¿Reemplazar las mediciones introducidas?', 'استبدال القياسات المُدخلة؟',
        '入力済みの測定値を置き換えますか？', '替换已输入的测量值？'),
    'spc.example.confirm-message': (
        "L'exemple remplace l'identification de la pièce, les limites et toutes les mesures de la grille. Rien n'est enregistré tant que vous n'enregistrez pas l'étude.",
        'The example replaces the part identification, the limits and every measurement in the grid. Nothing is saved until you save the study.',
        'El ejemplo reemplaza la identificación de la pieza, los límites y todas las mediciones de la cuadrícula. No se guarda nada hasta que guarde el estudio.',
        'يستبدل المثال تعريف القطعة والحدود وجميع قياسات الشبكة. لا يُحفظ شيء ما لم تحفظ الدراسة.',
        'サンプルは部品の識別情報、規格限界、グリッドのすべての測定値を置き換えます。調査を保存するまで何も保存されません。',
        '示例将替换零件标识、规格限和网格中的所有测量值。在保存研究之前不会保存任何内容。'),
    'spc.example.confirm': ("Charger l'exemple", 'Load the example', 'Cargar el ejemplo', 'تحميل المثال', 'サンプルを読み込む', '加载示例'),
    'spc.example.empty': (
        "La grille est vide : saisissez vos mesures ou chargez un des jeux d'exemple.",
        'The grid is empty: enter your measurements or load one of the sample datasets.',
        'La cuadrícula está vacía: introduzca sus mediciones o cargue uno de los conjuntos de ejemplo.',
        'الشبكة فارغة: أدخل قياساتك أو حمّل إحدى مجموعات البيانات النموذجية.',
        'グリッドが空です。測定値を入力するか、サンプルデータを読み込んでください。',
        '网格为空：请输入测量值或加载一个示例数据集。'),
    'spc.formula.title': ('Formules', 'Formulas', 'Fórmulas', 'الصيغ', '計算式', '公式'),
    'spc.formula.constants-title': (
        'Table des constantes SPC', 'SPC constants table', 'Tabla de constantes SPC',
        'جدول ثوابت SPC', 'SPC 定数表', 'SPC 常数表'),
    'spc.study.process-status': ('Statut du procédé', 'Process status', 'Estado del proceso', 'حالة العملية', '工程の状態', '过程状态'),
}
