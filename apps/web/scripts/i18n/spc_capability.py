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
    # ----- Gage R&R (ADR 0097) : registre -----
    'spc.gage-rr.eyebrow': (
        'SPC · Analyse du système de mesure', 'SPC · Measurement system analysis', 'SPC · Análisis del sistema de medición',
        'SPC · تحليل نظام القياس', 'SPC · 測定システム解析', 'SPC · 测量系统分析'),
    'spc.gage-rr.register.subtitle': (
        "Répétabilité et reproductibilité des moyens de mesure (AIAG MSA). Cliquez sur une ligne pour rouvrir l'étude.",
        'Repeatability and reproducibility of measuring equipment (AIAG MSA). Click a row to reopen the study.',
        'Repetibilidad y reproducibilidad de los medios de medición (AIAG MSA). Haga clic en una fila para reabrir el estudio.',
        'قابلية التكرار وقابلية إعادة الإنتاج لوسائل القياس (AIAG MSA). انقر على سطر لإعادة فتح الدراسة.',
        '測定器の繰返し性と再現性（AIAG MSA）。行をクリックすると調査を再度開きます。',
        '测量器具的重复性与再现性（AIAG MSA）。点击一行即可重新打开研究。'),
    'spc.gage-rr.register.new': ('Nouvelle étude R&R', 'New R&R study', 'Nuevo estudio R&R', 'دراسة R&R جديدة', '新規 R&R 調査', '新建 R&R 研究'),
    'spc.gage-rr.register.kpi-total': ('Études R&R', 'R&R studies', 'Estudios R&R', 'دراسات R&R', 'R&R 調査', 'R&R 研究'),
    'spc.gage-rr.register.kpi-acceptable': (
        'Acceptables (%GRR < 10 %)', 'Acceptable (%GRR < 10%)', 'Aceptables (%GRR < 10 %)',
        'مقبولة (%GRR < 10 %)', '合格（%GRR < 10 %）', '可接受（%GRR < 10%）'),
    'spc.gage-rr.register.kpi-conditional': (
        'Sous conditions (10 – 30 %)', 'Conditional (10 – 30%)', 'Bajo condiciones (10 – 30 %)',
        'مشروطة (10 – 30 %)', '条件付き（10 – 30 %）', '有条件（10 – 30%）'),
    'spc.gage-rr.register.kpi-not-acceptable': (
        'Non acceptables (> 30 %)', 'Not acceptable (> 30%)', 'No aceptables (> 30 %)',
        'غير مقبولة (> 30 %)', '不合格（> 30 %）', '不可接受（> 30%）'),
    'spc.gage-rr.register.kpi-ndc': (
        'Discrimination insuffisante (ndc < 5)', 'Insufficient discrimination (ndc < 5)',
        'Discriminación insuficiente (ndc < 5)', 'تمييز غير كافٍ (ndc < 5)', '識別能力不足（ndc < 5）', '分辨力不足（ndc < 5）'),
    'spc.gage-rr.register.search': (
        'Rechercher : pièce, instrument, opérateur…', 'Search: part, instrument, operator…',
        'Buscar: pieza, instrumento, operador…', 'بحث: القطعة، الأداة، المشغّل…', '検索：部品、測定器、測定者…', '搜索：零件、量具、操作员…'),
    'spc.gage-rr.register.filter-decision': (
        'Filtrer par décision', 'Filter by decision', 'Filtrar por decisión', 'التصفية حسب القرار', '判定で絞り込む', '按判定筛选'),
    'spc.gage-rr.register.all-decisions': (
        'Toutes les décisions', 'All decisions', 'Todas las decisiones', 'كل القرارات', 'すべての判定', '全部判定'),
    'spc.gage-rr.register.load-failed': (
        "Le registre des études R&R n'a pas pu être chargé.", 'The R&R study register could not be loaded.',
        'No se pudo cargar el registro de estudios R&R.', 'تعذّر تحميل سجل دراسات R&R.',
        'R&R 調査の一覧を読み込めませんでした。', '无法加载 R&R 研究登记册。'),
    'spc.gage-rr.register.empty': (
        'Aucune étude R&R pour le moment. Cliquez sur « Nouvelle étude R&R » pour commencer.',
        'No R&R study yet. Click “New R&R study” to start.',
        'Todavía no hay estudios R&R. Haga clic en «Nuevo estudio R&R» para empezar.',
        'لا توجد دراسة R&R حتى الآن. انقر على «دراسة R&R جديدة» للبدء.',
        'R&R 調査はまだありません。「新規 R&R 調査」をクリックして開始してください。',
        '尚无 R&R 研究。点击“新建 R&R 研究”开始。'),
    'spc.gage-rr.register.legend': (
        '%GRR de la variation totale (méthode moyenne et étendue, AIAG) : vert sous 10 %, jaune de 10 à 30 %, rouge au-delà. Le liseré rouge signale un système de mesure non acceptable.',
        '%GRR of total variation (average and range method, AIAG): green below 10%, yellow from 10 to 30%, red above. The red edge flags an unacceptable measurement system.',
        '%GRR de la variación total (método de promedio y rango, AIAG): verde por debajo del 10 %, amarillo del 10 al 30 %, rojo por encima. El borde rojo señala un sistema de medición no aceptable.',
        '%GRR من التباين الكلي (طريقة المتوسط والمدى، AIAG): أخضر دون 10 %، أصفر من 10 إلى 30 %، أحمر فوق ذلك. يشير الحد الأحمر إلى نظام قياس غير مقبول.',
        '全変動に対する %GRR（平均・範囲法、AIAG）：10 % 未満は緑、10～30 % は黄、それを超えると赤。赤い縁取りは不合格の測定システムを示します。',
        '占总变差的 %GRR（均值极差法，AIAG）：低于 10% 为绿色，10% 至 30% 为黄色，高于 30% 为红色。红色边线表示测量系统不可接受。'),
    'spc.gage-rr.register.delete-title': (
        "Supprimer l'étude R&R {$part} ?", 'Delete R&R study {$part}?', '¿Eliminar el estudio R&R {$part}?',
        'حذف دراسة R&R {$part}؟', 'R&R 調査 {$part} を削除しますか？', '删除 R&R 研究 {$part}？'),
    'spc.gage-rr.register.delete-message': (
        "L'étude et ses mesures quittent le registre. La suppression est tracée dans le journal d'audit.",
        'The study and its measurements leave the register. The deletion is recorded in the audit log.',
        'El estudio y sus mediciones salen del registro. La eliminación queda registrada en el registro de auditoría.',
        'تخرج الدراسة وقياساتها من السجل. ويُسجَّل الحذف في سجل التدقيق.',
        '調査とその測定値は一覧から削除されます。削除は監査ログに記録されます。',
        '该研究及其测量值将从登记册中移除。删除操作会记入审计日志。'),
    'spc.gage-rr.register.delete-failed': (
        "L'étude n'a pas pu être supprimée.", 'The study could not be deleted.', 'No se pudo eliminar el estudio.',
        'تعذّر حذف الدراسة.', '調査を削除できませんでした。', '无法删除该研究。'),

    # ----- Gage R&R : colonnes -----
    'spc.gage-rr.col.instrument': ('Instrument', 'Instrument', 'Instrumento', 'أداة القياس', '測定器', '量具'),
    'spc.gage-rr.col.responsible': ('Responsable', 'Owner', 'Responsable', 'المسؤول', '責任者', '负责人'),
    'spc.gage-rr.col.operators': ('Opérateurs', 'Operators', 'Operadores', 'المشغّلون', '測定者', '操作员'),
    'spc.gage-rr.col.plan': ('Plan (k × n × r)', 'Design (k × n × r)', 'Plan (k × n × r)', 'الخطة (k × n × r)', '計画（k × n × r）', '方案（k × n × r）'),
    'spc.gage-rr.col.multiplier': ('Multiplicateur σ', 'σ multiplier', 'Multiplicador σ', 'مضاعف σ', 'σ 乗数', 'σ 倍数'),
    'spc.gage-rr.col.grr-tolerance': ('%GRR tolérance', '%GRR of tolerance', '%GRR de tolerancia', '%GRR من التفاوت', '公差に対する %GRR', '%GRR 公差'),
    'spc.gage-rr.col.decision': ('Décision', 'Decision', 'Decisión', 'القرار', '判定', '判定'),
    'spc.gage-rr.col.quantity': ('Grandeur', 'Quantity', 'Magnitud', 'المقدار', '項目', '量'),
    'spc.gage-rr.col.formula': ('Formule', 'Formula', 'Fórmula', 'الصيغة', '計算式', '公式'),
    'spc.gage-rr.col.application': ('Application numérique', 'Worked calculation', 'Aplicación numérica', 'التطبيق العددي', '数値計算', '数值计算'),
    'spc.gage-rr.col.value': ('Valeur', 'Value', 'Valor', 'القيمة', '値', '值'),
    'spc.gage-rr.col.constant': ('Constante', 'Constant', 'Constante', 'الثابت', '定数', '常数'),
    'spc.gage-rr.col.role': ('Rôle', 'Role', 'Función', 'الدور', '役割', '作用'),
    'spc.gage-rr.col.read-for': ('Valeur lue pour…', 'Value read for…', 'Valor leído para…', 'القيمة المقروءة لـ…', '参照条件…', '取值依据…'),
    'spc.gage-rr.col.source': ('Source de variation', 'Source of variation', 'Fuente de variación', 'مصدر التباين', '変動要因', '变差来源'),
    'spc.gage-rr.col.percent-total': ('% variation totale', '% total variation', '% variación total', '% من التباين الكلي', '全変動比 %', '% 总变差'),
    'spc.gage-rr.col.percent-contribution': ('% contribution', '% contribution', '% contribución', '% المساهمة', '寄与率 %', '% 贡献率'),
    'spc.gage-rr.col.percent-tolerance': ('% tolérance', '% tolerance', '% tolerancia', '% التفاوت', '公差比 %', '% 公差'),
    'spc.gage-rr.col.indicator': ('Indicateur', 'Indicator', 'Indicador', 'المؤشر', '指標', '指标'),
    'spc.gage-rr.col.rule': ('Règle', 'Rule', 'Regla', 'القاعدة', '基準', '规则'),
    'spc.gage-rr.col.df': ('ddl', 'df', 'gl', 'د.ح', '自由度', '自由度'),
    'spc.gage-rr.col.ss': ('Somme des carrés', 'Sum of squares', 'Suma de cuadrados', 'مجموع المربعات', '平方和', '平方和'),
    'spc.gage-rr.col.ms': ('Carré moyen', 'Mean square', 'Cuadrado medio', 'متوسط المربعات', '平均平方', '均方'),
    'spc.gage-rr.col.variance': ('Variance', 'Variance', 'Varianza', 'التباين', '分散', '方差'),
    'spc.gage-rr.col.spread': (
        'Dispersion ({$MULTIPLIER} σ)', 'Spread ({$MULTIPLIER} σ)', 'Dispersión ({$MULTIPLIER} σ)',
        'التشتت ({$MULTIPLIER} σ)', 'ばらつき（{$MULTIPLIER} σ）', '离散（{$MULTIPLIER} σ）'),

    # ----- Gage R&R : verdicts et libellés -----
    'spc.gage-rr.decision.acceptable': ('Acceptable', 'Acceptable', 'Aceptable', 'مقبول', '合格', '可接受'),
    'spc.gage-rr.decision.conditional': (
        'Acceptable sous conditions', 'Conditionally acceptable', 'Aceptable bajo condiciones',
        'مقبول بشروط', '条件付き合格', '有条件可接受'),
    'spc.gage-rr.decision.not-acceptable': ('Non acceptable', 'Not acceptable', 'No aceptable', 'غير مقبول', '不合格', '不可接受'),
    'spc.gage-rr.ndc.sufficient': (
        'Discrimination suffisante', 'Sufficient discrimination', 'Discriminación suficiente',
        'تمييز كافٍ', '識別能力は十分', '分辨力充分'),
    'spc.gage-rr.ndc.insufficient': (
        'Discrimination insuffisante', 'Insufficient discrimination', 'Discriminación insuficiente',
        'تمييز غير كافٍ', '識別能力が不足', '分辨力不足'),
    'spc.gage-rr.operator-n': ('Opérateur {$letter}', 'Operator {$letter}', 'Operador {$letter}', 'المشغّل {$letter}', '測定者 {$letter}', '操作员 {$letter}'),
    'spc.gage-rr.anova.part': ('Pièces', 'Parts', 'Piezas', 'القطع', '部品', '零件'),
    'spc.gage-rr.anova.operator': ('Opérateurs', 'Operators', 'Operadores', 'المشغّلون', '測定者', '操作员'),
    'spc.gage-rr.anova.interaction': (
        'Interaction pièce × opérateur', 'Part × operator interaction', 'Interacción pieza × operador',
        'تفاعل القطعة × المشغّل', '部品 × 測定者の交互作用', '零件 × 操作员交互作用'),
    'spc.gage-rr.anova.repeatability': ('Répétabilité', 'Repeatability', 'Repetibilidad', 'قابلية التكرار', '繰返し性', '重复性'),
    'spc.gage-rr.anova.repeatability-pooled': (
        'Répétabilité (interaction regroupée)', 'Repeatability (interaction pooled)', 'Repetibilidad (interacción agrupada)',
        'قابلية التكرار (مع دمج التفاعل)', '繰返し性（交互作用をプール）', '重复性（合并交互作用）'),
    'spc.gage-rr.anova.total': ('Total', 'Total', 'Total', 'المجموع', '合計', '合计'),
    'spc.gage-rr.error.part': (
        'La référence de la pièce est obligatoire (120 caractères au plus).',
        'The part reference is required (120 characters at most).',
        'La referencia de la pieza es obligatoria (120 caracteres como máximo).',
        'مرجع القطعة إلزامي (120 حرفًا كحد أقصى).',
        '部品番号は必須です（120 文字以内）。', '零件编号为必填项（最多 120 个字符）。'),
    'spc.gage-rr.error.date': (
        "La date de l'étude est obligatoire.", 'The study date is required.', 'La fecha del estudio es obligatoria.',
        'تاريخ الدراسة إلزامي.', '調査日は必須です。', '研究日期为必填项。'),
    'spc.gage-rr.error.limits': (
        'La limite supérieure (USL) doit dépasser la limite inférieure (LSL).',
        'The upper limit (USL) must exceed the lower limit (LSL).',
        'El límite superior (USL) debe superar el límite inferior (LSL).',
        'يجب أن يتجاوز الحد الأعلى (USL) الحد الأدنى (LSL).',
        '上限（USL）は下限（LSL）より大きくなければなりません。', '上限（USL）必须大于下限（LSL）。'),
    'spc.gage-rr.error.operators': (
        "Choisissez chaque opérateur dans l'annuaire, sans doublon.",
        'Choose each operator from the directory, without duplicates.',
        'Elija cada operador en el directorio, sin duplicados.',
        'اختر كل مشغّل من الدليل، دون تكرار.',
        '各測定者を名簿から重複なく選択してください。', '请从通讯录中选择每位操作员，不得重复。'),
    'spc.gage-rr.error.responsible': (
        "Le responsable choisi n'a pas pu être retrouvé dans l'annuaire.",
        'The selected owner could not be found in the directory.',
        'No se encontró en el directorio al responsable elegido.',
        'تعذّر العثور على المسؤول المختار في الدليل.',
        '選択した責任者が名簿に見つかりませんでした。', '在通讯录中找不到所选负责人。'),
    'spc.gage-rr.error.plan': (
        'Le plan doit compter 2 ou 3 opérateurs, 2 à 10 pièces, 2 ou 3 essais, à 5,15 ou 6 σ.',
        'The design must have 2 or 3 operators, 2 to 10 parts, 2 or 3 trials, at 5.15 or 6 σ.',
        'El plan debe tener 2 o 3 operadores, de 2 a 10 piezas, 2 o 3 ensayos, a 5,15 o 6 σ.',
        'يجب أن تضم الخطة 2 أو 3 مشغّلين، ومن 2 إلى 10 قطع، و2 أو 3 محاولات، عند 5.15 أو 6 σ.',
        '計画は測定者 2～3 名、部品 2～10 個、試行 2～3 回、5.15 または 6 σ でなければなりません。',
        '方案须包含 2 或 3 名操作员、2 至 10 个零件、2 或 3 次试验，采用 5.15 或 6 σ。'),
    'spc.gage-rr.error.measurements': (
        'Toutes les mesures de la grille sont obligatoires, et elles ne doivent pas être toutes identiques.',
        'Every measurement in the grid is required, and they must not all be identical.',
        'Todas las mediciones de la tabla son obligatorias y no deben ser todas idénticas.',
        'جميع قياسات الجدول إلزامية، ويجب ألا تكون كلها متطابقة.',
        'グリッドのすべての測定値が必須で、すべてが同じ値であってはなりません。',
        '表格中的所有测量值均为必填项，且不能全部相同。'),
    'spc.gage-rr.error.generic': (
        "Un champ de l'étude a été refusé : vérifiez la saisie.", 'A field of the study was rejected: check your entry.',
        'Se rechazó un campo del estudio: revise los datos introducidos.', 'رُفض أحد حقول الدراسة: تحقّق من الإدخال.',
        '調査の項目が拒否されました。入力内容を確認してください。', '研究的某个字段被拒绝：请检查输入。'),

    # ----- Gage R&R : interprétation -----
    'spc.gage-rr.read.ev': (
        "Répétabilité (EV) = {$ev}, soit {$pct} % de la variation totale : dispersion d'un même opérateur mesurant plusieurs fois la même pièce avec le même instrument.",
        'Repeatability (EV) = {$ev}, i.e. {$pct}% of total variation: spread of one operator measuring the same part several times with the same instrument.',
        'Repetibilidad (EV) = {$ev}, es decir, {$pct} % de la variación total: dispersión de un mismo operador que mide varias veces la misma pieza con el mismo instrumento.',
        'قابلية التكرار (EV) = {$ev}، أي {$pct} % من التباين الكلي: تشتت قياسات المشغّل نفسه للقطعة نفسها عدة مرات بالأداة نفسها.',
        '繰返し性（EV）= {$ev}、全変動の {$pct} %：同じ測定者が同じ測定器で同じ部品を繰り返し測定したときのばらつきです。',
        '重复性（EV）= {$ev}，占总变差的 {$pct}%：同一操作员用同一量具多次测量同一零件时的离散。'),
    'spc.gage-rr.read.av': (
        'Reproductibilité (AV) = {$av}, soit {$pct} % : écart entre opérateurs (X̄diff = {$xdiff}).',
        'Reproducibility (AV) = {$av}, i.e. {$pct}%: difference between operators (X̄diff = {$xdiff}).',
        'Reproducibilidad (AV) = {$av}, es decir, {$pct} %: diferencia entre operadores (X̄diff = {$xdiff}).',
        'قابلية إعادة الإنتاج (AV) = {$av}، أي {$pct} %: الفرق بين المشغّلين (X̄diff = {$xdiff}).',
        '再現性（AV）= {$av}、{$pct} %：測定者間の差（X̄diff = {$xdiff}）です。',
        '再现性（AV）= {$av}，占 {$pct}%：操作员之间的差异（X̄diff = {$xdiff}）。'),
    'spc.gage-rr.read.grr': (
        'GRR = {$grr} : {$pct} % de la variation totale et {$tol} % de la tolérance. Décision : {$decision}.',
        'GRR = {$grr}: {$pct}% of total variation and {$tol}% of tolerance. Decision: {$decision}.',
        'GRR = {$grr}: {$pct} % de la variación total y {$tol} % de la tolerancia. Decisión: {$decision}.',
        'GRR = {$grr}: {$pct} % من التباين الكلي و{$tol} % من التفاوت. القرار: {$decision}.',
        'GRR = {$grr}：全変動の {$pct} %、公差の {$tol} %。判定：{$decision}。',
        'GRR = {$grr}：占总变差的 {$pct}%，占公差的 {$tol}%。判定：{$decision}。'),
    'spc.gage-rr.read.ndc-ok': (
        "Le système distingue {$ndc} catégories de pièces (ndc). C'est suffisant pour piloter un procédé.",
        'The system distinguishes {$ndc} categories of parts (ndc). That is enough to control a process.',
        'El sistema distingue {$ndc} categorías de piezas (ndc). Es suficiente para controlar un proceso.',
        'يميّز النظام {$ndc} فئات من القطع (ndc). وهذا كافٍ لقيادة عملية.',
        'このシステムは部品を {$ndc} 区分に識別できます（ndc）。工程管理には十分です。',
        '该系统可区分 {$ndc} 个零件类别（ndc），足以用于过程控制。'),
    'spc.gage-rr.read.ndc-ko': (
        "Le système distingue {$ndc} catégories de pièces (ndc). C'est insuffisant : améliorer l'instrument ou la méthode.",
        'The system distinguishes {$ndc} categories of parts (ndc). That is not enough: improve the instrument or the method.',
        'El sistema distingue {$ndc} categorías de piezas (ndc). Es insuficiente: mejore el instrumento o el método.',
        'يميّز النظام {$ndc} فئات من القطع (ndc). وهذا غير كافٍ: حسّن الأداة أو الطريقة.',
        'このシステムは部品を {$ndc} 区分に識別できます（ndc）。不十分です。測定器または方法を改善してください。',
        '该系统可区分 {$ndc} 个零件类别（ndc），不足：请改进量具或方法。'),
    'spc.gage-rr.read.dominant-ev': (
        "Le poste dominant est la répétabilité (instrument ou méthode) : vérifier la résolution, la fixation de la pièce, l'état de l'instrument.",
        "The dominant source is repeatability (instrument or method): check the resolution, how the part is held, and the instrument's condition.",
        'La fuente dominante es la repetibilidad (instrumento o método): verifique la resolución, la sujeción de la pieza y el estado del instrumento.',
        'المصدر الغالب هو قابلية التكرار (الأداة أو الطريقة): تحقّق من الدقة وتثبيت القطعة وحالة الأداة.',
        '主な要因は繰返し性（測定器または方法）です。分解能、部品の固定、測定器の状態を確認してください。',
        '主要来源是重复性（量具或方法）：请检查分辨率、零件装夹和量具状态。'),
    'spc.gage-rr.read.dominant-av': (
        "Le poste dominant est la reproductibilité (opérateurs) : harmoniser la méthode de mesure, former les opérateurs, vérifier la lisibilité de l'instrument.",
        "The dominant source is reproducibility (operators): harmonise the measuring method, train the operators, check the instrument's readability.",
        'La fuente dominante es la reproducibilidad (operadores): armonice el método de medición, forme a los operadores y verifique la legibilidad del instrumento.',
        'المصدر الغالب هو قابلية إعادة الإنتاج (المشغّلون): وحّد طريقة القياس ودرّب المشغّلين وتحقّق من وضوح قراءة الأداة.',
        '主な要因は再現性（測定者）です。測定方法を統一し、測定者を教育し、測定器の読みやすさを確認してください。',
        '主要来源是再现性（操作员）：请统一测量方法、培训操作员并检查量具的可读性。'),

    # ----- Gage R&R : graphiques -----
    'spc.gage-rr.chart.repeatability': ('Répétabilité (EV)', 'Repeatability (EV)', 'Repetibilidad (EV)', 'قابلية التكرار (EV)', '繰返し性（EV）', '重复性（EV）'),
    'spc.gage-rr.chart.reproducibility': ('Reproductibilité (AV)', 'Reproducibility (AV)', 'Reproducibilidad (AV)', 'قابلية إعادة الإنتاج (AV)', '再現性（AV）', '再现性（AV）'),
    'spc.gage-rr.chart.parts': ('Pièces (PV)', 'Parts (PV)', 'Piezas (PV)', 'القطع (PV)', '部品（PV）', '零件（PV）'),
    'spc.gage-rr.chart.contribution': ('% contribution', '% contribution', '% contribución', '% المساهمة', '寄与率 %', '% 贡献率'),
    'spc.gage-rr.chart.total': ('% variation totale', '% total variation', '% variación total', '% من التباين الكلي', '全変動比 %', '% 总变差'),
    'spc.gage-rr.chart.tolerance': ('% tolérance', '% tolerance', '% tolerancia', '% التفاوت', '公差比 %', '% 公差'),
    'spc.gage-rr.chart.part-mean': ('Moyenne pièce', 'Part mean', 'Media de la pieza', 'متوسط القطعة', '部品平均', '零件均值'),
    'spc.gage-rr.chart.part-axis': ('Pièce', 'Part', 'Pieza', 'القطعة', '部品', '零件'),

    # ----- Gage R&R : fiche d'étude -----
    'spc.gage-rr.study.title-new': ('Nouvelle étude R&R', 'New R&R study', 'Nuevo estudio R&R', 'دراسة R&R جديدة', '新規 R&R 調査', '新建 R&R 研究'),
    'spc.gage-rr.study.title-edit': ('Étude R&R {$part}', 'R&R study {$part}', 'Estudio R&R {$part}', 'دراسة R&R {$part}', 'R&R 調査 {$part}', 'R&R 研究 {$part}'),
    'spc.gage-rr.study.subtitle': (
        'Répétabilité et reproductibilité, méthode moyenne et étendue puis ANOVA. Saisissez les mesures dans les cellules jaunes ; tout le reste se calcule.',
        'Repeatability and reproducibility, average and range method then ANOVA. Enter the measurements in the yellow cells; everything else is calculated.',
        'Repetibilidad y reproducibilidad, método de promedio y rango y luego ANOVA. Introduzca las mediciones en las celdas amarillas; todo lo demás se calcula.',
        'قابلية التكرار وإعادة الإنتاج، بطريقة المتوسط والمدى ثم ANOVA. أدخل القياسات في الخلايا الصفراء؛ ويُحسب الباقي تلقائيًا.',
        '繰返し性と再現性：平均・範囲法、続いて ANOVA。黄色のセルに測定値を入力すると、残りはすべて計算されます。',
        '重复性与再现性：先用均值极差法，再用 ANOVA。在黄色单元格中输入测量值，其余均自动计算。'),
    'spc.gage-rr.study.back': ('Registre R&R', 'R&R register', 'Registro R&R', 'سجل R&R', 'R&R 一覧', 'R&R 登记册'),
    'spc.gage-rr.study.save': ("Enregistrer l'étude", 'Save the study', 'Guardar el estudio', 'حفظ الدراسة', '調査を保存', '保存研究'),
    'spc.gage-rr.study.saved': ('Étude R&R enregistrée.', 'R&R study saved.', 'Estudio R&R guardado.', 'تم حفظ دراسة R&R.', 'R&R 調査を保存しました。', 'R&R 研究已保存。'),
    'spc.gage-rr.study.save-failed': (
        "L'étude R&R n'a pas pu être enregistrée.", 'The R&R study could not be saved.', 'No se pudo guardar el estudio R&R.',
        'تعذّر حفظ دراسة R&R.', 'R&R 調査を保存できませんでした。', '无法保存 R&R 研究。'),
    'spc.gage-rr.study.load-failed': (
        "L'étude R&R n'a pas pu être chargée.", 'The R&R study could not be loaded.', 'No se pudo cargar el estudio R&R.',
        'تعذّر تحميل دراسة R&R.', 'R&R 調査を読み込めませんでした。', '无法加载 R&R 研究。'),
    'spc.gage-rr.study.not-found': ('Étude R&R introuvable.', 'R&R study not found.', 'Estudio R&R no encontrado.', 'دراسة R&R غير موجودة.', 'R&R 調査が見つかりません。', '未找到 R&R 研究。'),
    'spc.gage-rr.study.read-only': (
        'Consultation seule : la saisie des études R&R est réservée aux personnes qui en ont le droit.',
        'Read only: entering R&R studies is reserved for people with that permission.',
        'Solo consulta: el registro de estudios R&R está reservado a las personas autorizadas.',
        'للاطلاع فقط: إدخال دراسات R&R مقصور على الأشخاص المخوَّلين.',
        '閲覧のみ：R&R 調査の入力は権限を持つ人に限られます。', '仅可查看：仅有权限的人员可以录入 R&R 研究。'),
    'spc.gage-rr.study.identification': ('Identification et plan', 'Identification and design', 'Identificación y plan', 'التعريف والخطة', '識別と計画', '标识与方案'),
    'spc.gage-rr.study.characteristic': ('Caractéristique mesurée', 'Measured characteristic', 'Característica medida', 'الخاصية المقيسة', '測定特性', '测量特性'),
    'spc.gage-rr.study.part-required': (
        'La référence de la pièce est obligatoire.', 'The part reference is required.', 'La referencia de la pieza es obligatoria.',
        'مرجع القطعة إلزامي.', '部品番号は必須です。', '零件编号为必填项。'),
    'spc.gage-rr.study.date-required': (
        "La date de l'étude est obligatoire.", 'The study date is required.', 'La fecha del estudio es obligatoria.',
        'تاريخ الدراسة إلزامي.', '調査日は必須です。', '研究日期为必填项。'),
    'spc.gage-rr.study.lsl': ('LSL (limite inférieure)', 'LSL (lower limit)', 'LSL (límite inferior)', 'LSL (الحد الأدنى)', 'LSL（下限）', 'LSL（下限）'),
    'spc.gage-rr.study.lsl-required': (
        'La limite inférieure est obligatoire.', 'The lower limit is required.', 'El límite inferior es obligatorio.',
        'الحد الأدنى إلزامي.', '下限は必須です。', '下限为必填项。'),
    'spc.gage-rr.study.usl': ('USL (limite supérieure)', 'USL (upper limit)', 'USL (límite superior)', 'USL (الحد الأعلى)', 'USL（上限）', 'USL（上限）'),
    'spc.gage-rr.study.usl-required': (
        'La limite supérieure est obligatoire.', 'The upper limit is required.', 'El límite superior es obligatorio.',
        'الحد الأعلى إلزامي.', '上限は必須です。', '上限为必填项。'),
    'spc.gage-rr.study.tolerance': ('Tolérance (USL − LSL)', 'Tolerance (USL − LSL)', 'Tolerancia (USL − LSL)', 'التفاوت (USL − LSL)', '公差（USL − LSL）', '公差（USL − LSL）'),
    'spc.gage-rr.study.operator-count': ('Opérateurs (k)', 'Operators (k)', 'Operadores (k)', 'المشغّلون (k)', '測定者（k）', '操作员（k）'),
    'spc.gage-rr.study.part-count': ('Pièces (n)', 'Parts (n)', 'Piezas (n)', 'القطع (n)', '部品（n）', '零件（n）'),
    'spc.gage-rr.study.trial-count': ('Essais (r)', 'Trials (r)', 'Ensayos (r)', 'المحاولات (r)', '試行（r）', '试验（r）'),
    'spc.gage-rr.study.measurements': (
        'Mesures — {$K} opérateurs × {$N} pièces × {$R} essais', 'Measurements — {$K} operators × {$N} parts × {$R} trials',
        'Mediciones — {$K} operadores × {$N} piezas × {$R} ensayos', 'القياسات — {$K} مشغّلين × {$N} قطع × {$R} محاولات',
        '測定値 — 測定者 {$K} 名 × 部品 {$N} 個 × 試行 {$R} 回', '测量值 — {$K} 名操作员 × {$N} 个零件 × {$R} 次试验'),
    'spc.gage-rr.study.clear': ('Vider la grille', 'Clear the grid', 'Vaciar la tabla', 'إفراغ الجدول', 'グリッドをクリア', '清空表格'),
    'spc.gage-rr.study.grid-help': (
        "« Moyenne » : moyenne des essais d'un opérateur sur une pièce. « Étendue » : max − min de ces essais (sert à estimer la répétabilité). Un bloc copié depuis un tableur se colle d'un coup.",
        '“Mean”: average of one operator’s trials on a part. “Range”: max − min of those trials (used to estimate repeatability). A block copied from a spreadsheet pastes in one go.',
        '«Media»: media de los ensayos de un operador sobre una pieza. «Rango»: máx − mín de esos ensayos (sirve para estimar la repetibilidad). Un bloque copiado de una hoja de cálculo se pega de una vez.',
        '«المتوسط»: متوسط محاولات المشغّل على قطعة واحدة. «المدى»: الأقصى − الأدنى لتلك المحاولات (يُستخدم لتقدير قابلية التكرار). تُلصق الكتلة المنسوخة من جدول بيانات دفعة واحدة.',
        '「平均」：ある部品に対する測定者の試行の平均。「範囲」：その試行の最大 − 最小（繰返し性の推定に使用）。表計算ソフトからコピーしたブロックは一度に貼り付けられます。',
        '“均值”：某操作员对一个零件各次试验的平均值。“极差”：这些试验的最大值 − 最小值（用于估计重复性）。从电子表格复制的数据块可一次性粘贴。'),
    'spc.gage-rr.study.part-no': ('Pièce n°', 'Part no.', 'Pieza n.º', 'رقم القطعة', '部品 No.', '零件编号'),
    'spc.gage-rr.study.part-mean': ('Moyenne pièce', 'Part mean', 'Media de la pieza', 'متوسط القطعة', '部品平均', '零件均值'),
    'spc.gage-rr.study.trial': ('Essai {$TRIAL}', 'Trial {$TRIAL}', 'Ensayo {$TRIAL}', 'المحاولة {$TRIAL}', '試行 {$TRIAL}', '试验 {$TRIAL}'),
    'spc.gage-rr.study.mean': ('Moyenne', 'Mean', 'Media', 'المتوسط', '平均', '均值'),
    'spc.gage-rr.study.range': ('Étendue', 'Range', 'Rango', 'المدى', '範囲', '极差'),
    'spc.gage-rr.study.cell-aria': (
        '{$operator}, pièce {$part}, essai {$trial}', '{$operator}, part {$part}, trial {$trial}',
        '{$operator}, pieza {$part}, ensayo {$trial}', '{$operator}، القطعة {$part}، المحاولة {$trial}',
        '{$operator}、部品 {$part}、試行 {$trial}', '{$operator}，零件 {$part}，试验 {$trial}'),
    'spc.gage-rr.study.waiting': (
        'Encore {$MISSING} mesure(s) à saisir avant le calcul.', '{$MISSING} measurement(s) still to enter before the calculation.',
        'Faltan {$MISSING} medición(es) por introducir antes del cálculo.', 'بقي {$MISSING} قياس (قياسات) للإدخال قبل الحساب.',
        '計算の前に、あと {$MISSING} 件の測定値を入力してください。', '计算前还需录入 {$MISSING} 个测量值。'),
    'spc.gage-rr.study.no-spread': (
        'Toutes les mesures sont identiques : aucune variation à décomposer.', 'All measurements are identical: no variation to break down.',
        'Todas las mediciones son idénticas: no hay variación que descomponer.', 'جميع القياسات متطابقة: لا يوجد تباين لتفكيكه.',
        'すべての測定値が同じです。分解する変動がありません。', '所有测量值均相同：没有可分解的变差。'),
    'spc.gage-rr.study.summary': ("Synthèse de l'étude", 'Study summary', 'Resumen del estudio', 'ملخص الدراسة', '調査の概要', '研究摘要'),
    'spc.gage-rr.study.interpretation': ('Interprétation', 'Interpretation', 'Interpretación', 'التفسير', '解釈', '解读'),
    'spc.gage-rr.example.load': ("Charger l'exemple", 'Load the example', 'Cargar el ejemplo', 'تحميل المثال', '例を読み込む', '载入示例'),
    'spc.gage-rr.example.confirm-title': (
        'Remplacer les mesures saisies ?', 'Replace the entered measurements?', '¿Reemplazar las mediciones introducidas?',
        'استبدال القياسات المُدخلة؟', '入力済みの測定値を置き換えますか？', '替换已录入的测量值？'),
    'spc.gage-rr.example.confirm-message': (
        "L'exemple remplace l'identification, les limites, le plan et toutes les mesures de la grille. Rien n'est enregistré tant que vous n'enregistrez pas l'étude.",
        'The example replaces the identification, limits, design and every measurement in the grid. Nothing is saved until you save the study.',
        'El ejemplo reemplaza la identificación, los límites, el plan y todas las mediciones de la tabla. No se guarda nada hasta que guarde el estudio.',
        'يستبدل المثال التعريف والحدود والخطة وجميع قياسات الجدول. لا يُحفظ شيء ما لم تحفظ الدراسة.',
        '例により、識別、限界、計画、グリッドのすべての測定値が置き換えられます。調査を保存するまで何も保存されません。',
        '示例将替换标识、界限、方案以及表格中的所有测量值。在您保存研究之前不会保存任何内容。'),
    'spc.gage-rr.kpi.grr': ('%GRR de la variation totale', '%GRR of total variation', '%GRR de la variación total', '%GRR من التباين الكلي', '全変動に対する %GRR', '占总变差的 %GRR'),
    'spc.gage-rr.kpi.tolerance': ('%GRR de la tolérance', '%GRR of tolerance', '%GRR de la tolerancia', '%GRR من التفاوت', '公差に対する %GRR', '占公差的 %GRR'),
    'spc.gage-rr.kpi.anova': ("%GRR selon l'ANOVA", '%GRR by ANOVA', '%GRR según el ANOVA', '%GRR وفق ANOVA', 'ANOVA による %GRR', 'ANOVA 计算的 %GRR'),
    'spc.gage-rr.kpi.decision': ('Décision AIAG sur le %GRR', 'AIAG decision on %GRR', 'Decisión AIAG sobre el %GRR', 'قرار AIAG بشأن %GRR', '%GRR に対する AIAG 判定', '基于 %GRR 的 AIAG 判定'),

    # ----- Gage R&R : étapes du calcul -----
    'spc.gage-rr.step1.title': (
        'Étape 1 – Moyennes et étendues par opérateur', 'Step 1 – Means and ranges per operator',
        'Paso 1 – Medias y rangos por operador', 'الخطوة 1 – المتوسطات والمديات لكل مشغّل',
        'ステップ 1 – 測定者ごとの平均と範囲', '第 1 步 – 各操作员的均值与极差'),
    'spc.gage-rr.step1.xbar-formula': ('Moyenne des {$N} pièces', 'Mean of the {$N} parts', 'Media de las {$N} piezas', 'متوسط القطع الـ {$N}', '{$N} 個の部品の平均', '{$N} 个零件的均值'),
    'spc.gage-rr.step1.xdiff': (
        'X̄diff (écart entre opérateurs)', 'X̄diff (difference between operators)', 'X̄diff (diferencia entre operadores)',
        'X̄diff (الفرق بين المشغّلين)', 'X̄diff（測定者間の差）', 'X̄diff（操作员之间的差异）'),
    'spc.gage-rr.step1.rbar-formula': ('Moyenne des {$N} étendues', 'Mean of the {$N} ranges', 'Media de los {$N} rangos', 'متوسط المديات الـ {$N}', '{$N} 個の範囲の平均', '{$N} 个极差的均值'),
    'spc.gage-rr.step1.rbarbar': ('R̄̄ (étendue moyenne)', 'R̄̄ (average range)', 'R̄̄ (rango medio)', 'R̄̄ (متوسط المدى)', 'R̄̄（平均範囲）', 'R̄̄（平均极差）'),
    'spc.gage-rr.step1.rp': ('Rp (étendue des pièces)', 'Rp (range of parts)', 'Rp (rango de las piezas)', 'Rp (مدى القطع)', 'Rp（部品の範囲）', 'Rp（零件极差）'),
    'spc.gage-rr.step1.rp-formula': (
        'max − min des moyennes pièces', 'max − min of the part means', 'máx − mín de las medias de las piezas',
        'الأقصى − الأدنى لمتوسطات القطع', '部品平均の最大 − 最小', '零件均值的最大值 − 最小值'),
    'spc.gage-rr.step1.ucl': ('UCLR (limite de contrôle)', 'UCLR (control limit)', 'UCLR (límite de control)', 'UCLR (حد التحكم)', 'UCLR（管理限界）', 'UCLR（控制限）'),
    'spc.gage-rr.step1.check': ('Contrôle des étendues', 'Range check', 'Control de los rangos', 'فحص المديات', '範囲の確認', '极差检查'),
    'spc.gage-rr.step1.check-rule': (
        'Aucune étendue ne doit dépasser UCLR', 'No range may exceed UCLR', 'Ningún rango debe superar UCLR',
        'يجب ألا يتجاوز أي مدى UCLR', 'どの範囲も UCLR を超えてはなりません', '任何极差都不得超过 UCLR'),
    'spc.gage-rr.step1.check-ok': (
        'OK : aucune étendue hors limite', 'OK: no range beyond the limit', 'OK: ningún rango fuera del límite',
        'سليم: لا يوجد مدى خارج الحد', 'OK：限界外の範囲はありません', '正常：没有极差超限'),
    'spc.gage-rr.step1.check-ko': (
        '{$COUNT} étendue(s) au-dessus de UCLR : vérifier les mesures', '{$COUNT} range(s) above UCLR: check the measurements',
        '{$COUNT} rango(s) por encima de UCLR: verifique las mediciones', '{$COUNT} مدى (مديات) فوق UCLR: تحقّق من القياسات',
        'UCLR を超える範囲が {$COUNT} 件：測定値を確認してください', '{$COUNT} 个极差高于 UCLR：请检查测量值'),
    'spc.gage-rr.step2.title': (
        'Étape 2 – Constantes, ajustées au multiplicateur σ choisi', 'Step 2 – Constants, scaled to the chosen σ multiplier',
        'Paso 2 – Constantes, ajustadas al multiplicador σ elegido', 'الخطوة 2 – الثوابت، معدّلة حسب مضاعف σ المختار',
        'ステップ 2 – 選択した σ 乗数に合わせた定数', '第 2 步 – 按所选 σ 倍数调整的常数'),
    'spc.gage-rr.step2.k1': ('Répétabilité', 'Repeatability', 'Repetibilidad', 'قابلية التكرار', '繰返し性', '重复性'),
    'spc.gage-rr.step2.k2': ('Reproductibilité', 'Reproducibility', 'Reproducibilidad', 'قابلية إعادة الإنتاج', '再現性', '再现性'),
    'spc.gage-rr.step2.d4': ('Limite des étendues', 'Range limit', 'Límite de los rangos', 'حد المديات', '範囲の限界', '极差界限'),
    'spc.gage-rr.step2.k3': ('Variation pièces', 'Part variation', 'Variación de las piezas', 'تباين القطع', '部品変動', '零件变差'),
    'spc.gage-rr.step3.title': (
        'Étape 3 – Décomposition de la variation', 'Step 3 – Breakdown of variation', 'Paso 3 – Descomposición de la variación',
        'الخطوة 3 – تفكيك التباين', 'ステップ 3 – 変動の分解', '第 3 步 – 变差分解'),
    'spc.gage-rr.step3.ev': ('EV – Répétabilité', 'EV – Repeatability', 'EV – Repetibilidad', 'EV – قابلية التكرار', 'EV – 繰返し性', 'EV – 重复性'),
    'spc.gage-rr.step3.av': ('AV – Reproductibilité', 'AV – Reproducibility', 'AV – Reproducibilidad', 'AV – قابلية إعادة الإنتاج', 'AV – 再現性', 'AV – 再现性'),
    'spc.gage-rr.step3.grr': (
        'GRR – Répétabilité et reproductibilité', 'GRR – Repeatability and reproducibility', 'GRR – Repetibilidad y reproducibilidad',
        'GRR – قابلية التكرار وإعادة الإنتاج', 'GRR – 繰返し性と再現性', 'GRR – 重复性与再现性'),
    'spc.gage-rr.step3.pv': ('PV – Variation entre pièces', 'PV – Part-to-part variation', 'PV – Variación entre piezas', 'PV – التباين بين القطع', 'PV – 部品間変動', 'PV – 零件间变差'),
    'spc.gage-rr.step3.tv': ('TV – Variation totale', 'TV – Total variation', 'TV – Variación total', 'TV – التباين الكلي', 'TV – 全変動', 'TV – 总变差'),
    'spc.gage-rr.step3.note': (
        'Les « % variation totale » ne s\'additionnent pas à 100 % (somme quadratique) ; les « % contribution », rapports de variances, s\'additionnent : EV + AV + PV = 100 %.',
        'The “% total variation” values do not add up to 100% (quadratic sum); the “% contribution” values, ratios of variances, do: EV + AV + PV = 100%.',
        'Los «% variación total» no suman 100 % (suma cuadrática); los «% contribución», cocientes de varianzas, sí suman: EV + AV + PV = 100 %.',
        'لا تُجمع «% من التباين الكلي» إلى 100 % (مجموع تربيعي)؛ أما «% المساهمة»، وهي نسب تباينات، فتُجمع: EV + AV + PV = 100 %.',
        '「全変動比 %」は合計 100 % になりません（二乗和）。分散の比である「寄与率 %」は合計されます：EV + AV + PV = 100 %。',
        '“% 总变差”相加不等于 100%（平方和关系）；“% 贡献率”是方差之比，可以相加：EV + AV + PV = 100%。'),
    'spc.gage-rr.step4.title': (
        "Étape 4 – Indicateurs d'aptitude et conclusion", 'Step 4 – Capability indicators and conclusion',
        'Paso 4 – Indicadores de aptitud y conclusión', 'الخطوة 4 – مؤشرات الأهلية والخلاصة',
        'ステップ 4 – 適合性指標と結論', '第 4 步 – 能力指标与结论'),
    'spc.gage-rr.step4.grr-total': (
        '%GRR (de la variation totale)', '%GRR (of total variation)', '%GRR (de la variación total)',
        '%GRR (من التباين الكلي)', '%GRR（全変動に対する）', '%GRR（占总变差）'),
    'spc.gage-rr.step4.grr-tolerance': (
        '%GRR (de la tolérance)', '%GRR (of tolerance)', '%GRR (de la tolerancia)', '%GRR (من التفاوت)', '%GRR（公差に対する）', '%GRR（占公差）'),
    'spc.gage-rr.step4.ndc': (
        'ndc (catégories distinctes)', 'ndc (distinct categories)', 'ndc (categorías distintas)', 'ndc (الفئات المتمايزة)', 'ndc（識別区分数）', 'ndc（可区分类别数）'),
    'spc.gage-rr.step4.ndc-rule': (
        "1,41 × PV / GRR, arrondi à l'entier inférieur", '1.41 × PV / GRR, rounded down', '1,41 × PV / GRR, redondeado hacia abajo',
        '1.41 × PV / GRR، مقرَّبًا إلى العدد الصحيح الأدنى', '1.41 × PV / GRR、小数点以下切り捨て', '1.41 × PV / GRR，向下取整'),
    'spc.gage-rr.step4.decision-grr': ('Décision sur %GRR', 'Decision on %GRR', 'Decisión sobre el %GRR', 'القرار بشأن %GRR', '%GRR の判定', '%GRR 判定'),
    'spc.gage-rr.step4.decision-grr-rule': (
        '< 10 % acceptable · 10 – 30 % sous conditions · > 30 % non acceptable',
        '< 10% acceptable · 10 – 30% conditional · > 30% not acceptable',
        '< 10 % aceptable · 10 – 30 % bajo condiciones · > 30 % no aceptable',
        '< 10 % مقبول · 10 – 30 % بشروط · > 30 % غير مقبول',
        '< 10 % 合格 · 10 – 30 % 条件付き · > 30 % 不合格',
        '< 10% 可接受 · 10 – 30% 有条件 · > 30% 不可接受'),
    'spc.gage-rr.step4.decision-ndc': ('Décision sur ndc', 'Decision on ndc', 'Decisión sobre el ndc', 'القرار بشأن ndc', 'ndc の判定', 'ndc 判定'),
    'spc.gage-rr.step4.decision-ndc-rule': (
        'ndc ≥ 5 : le système distingue assez de catégories', 'ndc ≥ 5: the system distinguishes enough categories',
        'ndc ≥ 5: el sistema distingue suficientes categorías', 'ndc ≥ 5: يميّز النظام عددًا كافيًا من الفئات',
        'ndc ≥ 5：十分な区分を識別できる', 'ndc ≥ 5：系统可区分足够多的类别'),

    # ----- Gage R&R : ANOVA -----
    'spc.gage-rr.anova.title': (
        'ANOVA à deux facteurs avec interaction', 'Two-way ANOVA with interaction', 'ANOVA de dos factores con interacción',
        'ANOVA ثنائي العوامل مع التفاعل', '交互作用を含む二元配置分散分析', '含交互作用的双因素方差分析'),
    'spc.gage-rr.anova.pooled': (
        "L'interaction pièce × opérateur n'est pas significative (p > 0,25) : elle est regroupée avec la répétabilité.",
        'The part × operator interaction is not significant (p > 0.25): it is pooled with repeatability.',
        'La interacción pieza × operador no es significativa (p > 0,25): se agrupa con la repetibilidad.',
        'تفاعل القطعة × المشغّل غير دال (p > 0.25): يُدمج مع قابلية التكرار.',
        '部品 × 測定者の交互作用は有意ではありません（p > 0.25）。繰返し性にプールします。',
        '零件 × 操作员交互作用不显著（p > 0.25）：将其并入重复性。'),
    'spc.gage-rr.anova.not-pooled': (
        "L'interaction pièce × opérateur est significative (p ≤ 0,25) : elle compte dans la reproductibilité.",
        'The part × operator interaction is significant (p ≤ 0.25): it counts towards reproducibility.',
        'La interacción pieza × operador es significativa (p ≤ 0,25): cuenta en la reproducibilidad.',
        'تفاعل القطعة × المشغّل دال (p ≤ 0.25): يُحتسب ضمن قابلية إعادة الإنتاج.',
        '部品 × 測定者の交互作用は有意です（p ≤ 0.25）。再現性に含めます。',
        '零件 × 操作员交互作用显著（p ≤ 0.25）：计入再现性。'),
    'spc.gage-rr.anova.full': (
        "Tableau d'ANOVA avec interaction", 'ANOVA table with interaction', 'Tabla ANOVA con interacción',
        'جدول ANOVA مع التفاعل', '交互作用ありの分散分析表', '含交互作用的方差分析表'),
    'spc.gage-rr.anova.pooled-table': (
        "Tableau d'ANOVA sans interaction (regroupée)", 'ANOVA table without interaction (pooled)', 'Tabla ANOVA sin interacción (agrupada)',
        'جدول ANOVA دون تفاعل (مدموج)', '交互作用なしの分散分析表（プール）', '不含交互作用的方差分析表（已合并）'),
    'spc.gage-rr.anova.components': (
        'Composantes de la variance', 'Variance components', 'Componentes de la varianza', 'مكوّنات التباين', '分散成分', '方差分量'),
    'spc.gage-rr.anova.ev': ('Répétabilité (EV)', 'Repeatability (EV)', 'Repetibilidad (EV)', 'قابلية التكرار (EV)', '繰返し性（EV）', '重复性（EV）'),
    'spc.gage-rr.anova.av': ('Reproductibilité (AV)', 'Reproducibility (AV)', 'Reproducibilidad (AV)', 'قابلية إعادة الإنتاج (AV)', '再現性（AV）', '再现性（AV）'),
    'spc.gage-rr.anova.av-operator': ('dont opérateurs', 'of which operators', 'de ella, operadores', 'منها المشغّلون', 'うち測定者', '其中操作员'),
    'spc.gage-rr.anova.av-interaction': ('dont interaction', 'of which interaction', 'de ella, interacción', 'منها التفاعل', 'うち交互作用', '其中交互作用'),
    'spc.gage-rr.anova.pv': ('Pièces (PV)', 'Parts (PV)', 'Piezas (PV)', 'القطع (PV)', '部品（PV）', '零件（PV）'),
    'spc.gage-rr.anova.tv': ('Totale (TV)', 'Total (TV)', 'Total (TV)', 'الكلي (TV)', '全体（TV）', '总计（TV）'),
    'spc.gage-rr.method.title': (
        'Comparaison des deux méthodes', 'Comparison of the two methods', 'Comparación de los dos métodos',
        'مقارنة الطريقتين', '2 つの手法の比較', '两种方法的比较'),
    'spc.gage-rr.method.col': ('Méthode', 'Method', 'Método', 'الطريقة', '手法', '方法'),
    'spc.gage-rr.method.average-range': (
        'Moyenne et étendue', 'Average and range', 'Promedio y rango', 'المتوسط والمدى', '平均・範囲法', '均值极差法'),
    'spc.gage-rr.method.anova': ('ANOVA', 'ANOVA', 'ANOVA', 'ANOVA', 'ANOVA', 'ANOVA'),
    'spc.gage-rr.charts.title': ('Graphiques', 'Charts', 'Gráficos', 'الرسوم البيانية', 'グラフ', '图表'),
    'spc.gage-rr.charts.components': (
        'Composantes de la variation', 'Components of variation', 'Componentes de la variación', 'مكوّنات التباين', '変動の成分', '变差分量'),
    'spc.gage-rr.charts.range': (
        'Carte R par opérateur (étendues et UCLR)', 'R chart by operator (ranges and UCLR)', 'Gráfico R por operador (rangos y UCLR)',
        'مخطط R لكل مشغّل (المديات وUCLR)', '測定者別 R 管理図（範囲と UCLR）', '按操作员的 R 图（极差与 UCLR）'),
    'spc.gage-rr.charts.mean': (
        'Carte X̄ par opérateur : un bon système a la plupart des points hors limites',
        'X̄ chart by operator: a good system has most points outside the limits',
        'Gráfico X̄ por operador: un buen sistema tiene la mayoría de los puntos fuera de los límites',
        'مخطط X̄ لكل مشغّل: النظام الجيد تقع معظم نقاطه خارج الحدود',
        '測定者別 X̄ 管理図：良い測定システムでは大半の点が限界外になります',
        '按操作员的 X̄ 图：好的测量系统大部分点位于控制限之外'),
    'spc.gage-rr.charts.by-part': (
        'Mesures par pièce : des traits parallèles disent des opérateurs cohérents',
        'Measurements by part: parallel lines mean consistent operators',
        'Mediciones por pieza: líneas paralelas indican operadores coherentes',
        'القياسات لكل قطعة: الخطوط المتوازية تدل على اتساق المشغّلين',
        '部品別の測定値：線が平行なら測定者間で一貫しています',
        '按零件的测量值：线条平行表示操作员之间一致'),

    # ----- Gage R&R : guide -----
    'spc.gage-rr.guide.title': ("Guide de l'étude R&R", 'R&R study guide', 'Guía del estudio R&R', 'دليل دراسة R&R', 'R&R 調査ガイド', 'R&R 研究指南'),
    'spc.gage-rr.guide.show': ('Lire le guide', 'Read the guide', 'Leer la guía', 'قراءة الدليل', 'ガイドを読む', '阅读指南'),
    'spc.gage-rr.guide.hide': ('Masquer le guide', 'Hide the guide', 'Ocultar la guía', 'إخفاء الدليل', 'ガイドを閉じる', '隐藏指南'),
    'spc.gage-rr.guide.purpose-title': (
        'À quoi sert une étude R&R ?', 'What is an R&R study for?', '¿Para qué sirve un estudio R&R?',
        'ما فائدة دراسة R&R؟', 'R&R 調査の目的は？', 'R&R 研究有什么用？'),
    'spc.gage-rr.guide.purpose': (
        'Savoir quelle part de la variation observée vient du système de mesure (instrument et opérateurs) et non des pièces. Si le système de mesure varie trop, on ne peut ni juger ni piloter le procédé : les Cp et Cpk sont faussés.',
        'To know how much of the observed variation comes from the measurement system (instrument and operators) rather than from the parts. If the measurement system varies too much, the process can be neither judged nor controlled: Cp and Cpk are distorted.',
        'Saber qué parte de la variación observada proviene del sistema de medición (instrumento y operadores) y no de las piezas. Si el sistema de medición varía demasiado, no se puede ni juzgar ni controlar el proceso: los Cp y Cpk quedan falseados.',
        'معرفة نسبة التباين المرصود الناتجة عن نظام القياس (الأداة والمشغّلين) لا عن القطع. إذا تباين نظام القياس كثيرًا، فلا يمكن الحكم على العملية ولا قيادتها: إذ تصبح Cp وCpk مضلِّلة.',
        '観測された変動のうち、部品ではなく測定システム（測定器と測定者）に起因する割合を知るためです。測定システムの変動が大きすぎると、工程を評価することも管理することもできず、Cp や Cpk が歪みます。',
        '了解观测到的变差中有多少来自测量系统（量具和操作员）而非零件。如果测量系统变差过大，就既无法判断也无法控制过程：Cp 和 Cpk 会失真。'),
    'spc.gage-rr.guide.plan-title': (
        "Réaliser l'étude (plan standard)", 'Running the study (standard design)', 'Realizar el estudio (plan estándar)',
        'إجراء الدراسة (الخطة القياسية)', '調査の実施（標準計画）', '开展研究（标准方案）'),
    'spc.gage-rr.guide.plan-1': (
        "Choisir 10 pièces prises dans la production, couvrant toute l'étendue de la tolérance (de bonne à limite), numérotées en secret.",
        'Choose 10 parts taken from production, covering the whole tolerance range (from good to borderline), numbered in secret.',
        'Elegir 10 piezas tomadas de la producción que cubran todo el rango de tolerancia (de buenas a límite), numeradas en secreto.',
        'اختر 10 قطع مأخوذة من الإنتاج تغطي كامل مدى التفاوت (من الجيدة إلى الحدّية)، مرقّمة سرًّا.',
        '生産から公差範囲全体（良品から限界品まで）をカバーする部品を 10 個選び、伏せて番号を付けます。',
        '从生产中选取 10 个零件，覆盖整个公差范围（从良好到临界），并秘密编号。'),
    'spc.gage-rr.guide.plan-2': (
        "Choisir 3 opérateurs, ceux qui mesurent réellement en production, avec l'instrument habituel.",
        'Choose 3 operators, those who actually measure in production, with the usual instrument.',
        'Elegir 3 operadores, los que miden realmente en producción, con el instrumento habitual.',
        'اختر 3 مشغّلين ممن يقيسون فعلًا في الإنتاج، بالأداة المعتادة.',
        '実際に生産で測定している測定者を 3 名、普段の測定器で選びます。',
        '选取 3 名实际在生产中进行测量的操作员，使用常用量具。'),
    'spc.gage-rr.guide.plan-3': (
        'Mesurer 3 fois chaque pièce : chaque opérateur mesure les pièces dans un ordre aléatoire, sans voir ses résultats précédents ni ceux des autres.',
        'Measure each part 3 times: each operator measures the parts in random order, without seeing their previous results or those of the others.',
        'Medir cada pieza 3 veces: cada operador mide las piezas en orden aleatorio, sin ver sus resultados anteriores ni los de los demás.',
        'قِس كل قطعة 3 مرات: يقيس كل مشغّل القطع بترتيب عشوائي دون أن يرى نتائجه السابقة ولا نتائج الآخرين.',
        '各部品を 3 回測定します。各測定者はランダムな順序で測定し、自分の前回の結果や他の人の結果を見てはいけません。',
        '每个零件测量 3 次：每位操作员按随机顺序测量，不得查看自己之前的结果或他人的结果。'),
    'spc.gage-rr.guide.plan-4': (
        'Saisir les mesures dans les cellules jaunes, ainsi que les limites LSL et USL.',
        'Enter the measurements in the yellow cells, along with the LSL and USL limits.',
        'Introducir las mediciones en las celdas amarillas, así como los límites LSL y USL.',
        'أدخل القياسات في الخلايا الصفراء، وكذلك الحدّين LSL وUSL.',
        '黄色のセルに測定値と、LSL・USL の限界値を入力します。',
        '在黄色单元格中输入测量值以及 LSL 和 USL 界限。'),
    'spc.gage-rr.guide.plan-5': (
        'Lire les résultats : méthode moyenne et étendue, puis ANOVA (EV, AV, GRR, PV, TV, %GRR, ndc, décision).',
        'Read the results: average and range method, then ANOVA (EV, AV, GRR, PV, TV, %GRR, ndc, decision).',
        'Leer los resultados: método de promedio y rango, luego ANOVA (EV, AV, GRR, PV, TV, %GRR, ndc, decisión).',
        'اقرأ النتائج: طريقة المتوسط والمدى، ثم ANOVA (EV وAV وGRR وPV وTV و%GRR وndc والقرار).',
        '結果を読みます：平均・範囲法、続いて ANOVA（EV、AV、GRR、PV、TV、%GRR、ndc、判定）。',
        '阅读结果：先看均值极差法，再看 ANOVA（EV、AV、GRR、PV、TV、%GRR、ndc、判定）。'),
    'spc.gage-rr.guide.terms-title': (
        'Signification des grandeurs', 'What the quantities mean', 'Significado de las magnitudes',
        'دلالة المقادير', '各項目の意味', '各量的含义'),
    'spc.gage-rr.guide.ev-term': ('EV – Répétabilité', 'EV – Repeatability', 'EV – Repetibilidad', 'EV – قابلية التكرار', 'EV – 繰返し性', 'EV – 重复性'),
    'spc.gage-rr.guide.ev': (
        "Variation d'un même opérateur mesurant la même pièce plusieurs fois (équipement). EV = R̄̄ × K1.",
        'Variation of one operator measuring the same part several times (equipment). EV = R̄̄ × K1.',
        'Variación de un mismo operador que mide la misma pieza varias veces (equipo). EV = R̄̄ × K1.',
        'تباين قياسات المشغّل نفسه للقطعة نفسها عدة مرات (المعدّات). EV = R̄̄ × K1.',
        '同じ測定者が同じ部品を繰り返し測定したときの変動（設備）。EV = R̄̄ × K1。',
        '同一操作员多次测量同一零件时的变差（设备）。EV = R̄̄ × K1。'),
    'spc.gage-rr.guide.av-term': ('AV – Reproductibilité', 'AV – Reproducibility', 'AV – Reproducibilidad', 'AV – قابلية إعادة الإنتاج', 'AV – 再現性', 'AV – 再现性'),
    'spc.gage-rr.guide.av': (
        'Variation entre opérateurs (méthode, habileté). AV = √[(X̄diff × K2)² − EV²/(n × r)], ramenée à 0 si négative.',
        'Variation between operators (method, skill). AV = √[(X̄diff × K2)² − EV²/(n × r)], set to 0 if negative.',
        'Variación entre operadores (método, habilidad). AV = √[(X̄diff × K2)² − EV²/(n × r)], llevada a 0 si es negativa.',
        'التباين بين المشغّلين (الطريقة، المهارة). AV = √[(X̄diff × K2)² − EV²/(n × r)]، ويُعاد إلى 0 إذا كان سالبًا.',
        '測定者間の変動（方法、技能）。AV = √[(X̄diff × K2)² − EV²/(n × r)]、負の場合は 0 とします。',
        '操作员之间的变差（方法、技能）。AV = √[(X̄diff × K2)² − EV²/(n × r)]，若为负则取 0。'),
    'spc.gage-rr.guide.grr': (
        'Variation du système de mesure : √(EV² + AV²).', 'Variation of the measurement system: √(EV² + AV²).',
        'Variación del sistema de medición: √(EV² + AV²).', 'تباين نظام القياس: √(EV² + AV²).',
        '測定システムの変動：√(EV² + AV²)。', '测量系统的变差：√(EV² + AV²)。'),
    'spc.gage-rr.guide.pv-term': ('PV – Variation pièces', 'PV – Part variation', 'PV – Variación de las piezas', 'PV – تباين القطع', 'PV – 部品変動', 'PV – 零件变差'),
    'spc.gage-rr.guide.pv': (
        'Variation réelle entre pièces : Rp × K3.', 'Actual variation between parts: Rp × K3.', 'Variación real entre piezas: Rp × K3.',
        'التباين الفعلي بين القطع: Rp × K3.', '部品間の実際の変動：Rp × K3。', '零件之间的实际变差：Rp × K3。'),
    'spc.gage-rr.guide.tv-term': ('TV – Variation totale', 'TV – Total variation', 'TV – Variación total', 'TV – التباين الكلي', 'TV – 全変動', 'TV – 总变差'),
    'spc.gage-rr.guide.ndc': (
        "Nombre de catégories distinctes que le système sait séparer : 1,41 × PV / GRR, arrondi à l'entier inférieur.",
        'Number of distinct categories the system can separate: 1.41 × PV / GRR, rounded down.',
        'Número de categorías distintas que el sistema sabe separar: 1,41 × PV / GRR, redondeado hacia abajo.',
        'عدد الفئات المتمايزة التي يستطيع النظام الفصل بينها: 1.41 × PV / GRR، مقرَّبًا إلى العدد الصحيح الأدنى.',
        'システムが区別できる区分の数：1.41 × PV / GRR、小数点以下切り捨て。',
        '系统能够区分的类别数：1.41 × PV / GRR，向下取整。'),
    'spc.gage-rr.guide.criteria-title': (
        "Critères d'acceptation (AIAG MSA)", 'Acceptance criteria (AIAG MSA)', 'Criterios de aceptación (AIAG MSA)',
        'معايير القبول (AIAG MSA)', '合否基準（AIAG MSA）', '接受准则（AIAG MSA）'),
    'spc.gage-rr.guide.criteria-ok': (
        'Système de mesure acceptable.', 'Acceptable measurement system.', 'Sistema de medición aceptable.',
        'نظام قياس مقبول.', '測定システムは合格です。', '测量系统可接受。'),
    'spc.gage-rr.guide.criteria-warn': (
        "Acceptable sous conditions (importance de la mesure, coût de l'instrument, coût des réparations) : décision à justifier.",
        'Conditionally acceptable (importance of the measurement, cost of the instrument, cost of repairs): the decision must be justified.',
        'Aceptable bajo condiciones (importancia de la medición, coste del instrumento, coste de las reparaciones): decisión que debe justificarse.',
        'مقبول بشروط (أهمية القياس، كلفة الأداة، كلفة الإصلاحات): يجب تبرير القرار.',
        '条件付き合格（測定の重要度、測定器のコスト、修理コスト）：判断の根拠が必要です。',
        '有条件可接受（测量的重要性、量具成本、维修成本）：需说明判定理由。'),
    'spc.gage-rr.guide.criteria-bad': (
        'Non acceptable : améliorer le système de mesure.', 'Not acceptable: improve the measurement system.',
        'No aceptable: mejore el sistema de medición.', 'غير مقبول: حسّن نظام القياس.',
        '不合格：測定システムを改善してください。', '不可接受：请改进测量系统。'),
    'spc.gage-rr.guide.criteria-ndc': (
        'Discrimination suffisante. De 2 à 4 : information grossière, bonne pour un tri large mais pas pour des cartes de contrôle ni des Cp / Cpk. À 1, le système ne distingue rien.',
        'Sufficient discrimination. From 2 to 4: coarse information, fine for broad sorting but not for control charts or Cp / Cpk. At 1, the system distinguishes nothing.',
        'Discriminación suficiente. De 2 a 4: información aproximada, válida para una clasificación amplia pero no para gráficos de control ni Cp / Cpk. Con 1, el sistema no distingue nada.',
        'تمييز كافٍ. من 2 إلى 4: معلومات تقريبية تصلح لفرز واسع لا لمخططات التحكم ولا لـ Cp / Cpk. عند 1، لا يميّز النظام شيئًا.',
        '識別能力は十分。2～4：大まかな情報で、大まかな選別には使えますが、管理図や Cp / Cpk には使えません。1 では何も識別できません。',
        '分辨力充分。2 至 4：信息粗略，可用于粗分选，但不适用于控制图或 Cp / Cpk。为 1 时，系统无法区分任何差异。'),
    'spc.gage-rr.guide.methods-title': (
        'Deux méthodes, mêmes mesures', 'Two methods, same measurements', 'Dos métodos, mismas mediciones',
        'طريقتان، القياسات نفسها', '2 つの手法、同じ測定値', '两种方法，同一组测量值'),
    'spc.gage-rr.guide.methods': (
        "La méthode moyenne et étendue est rapide ; l'ANOVA à deux facteurs, avec l'interaction pièce × opérateur, est la méthode de référence, plus précise. L'interaction est regroupée avec la répétabilité quand elle n'est pas significative (p > 0,25). Les constantes K1, K2 et K3 valent pour 5,15 σ ; à 6 σ, elles sont mises à l'échelle (× 6 / 5,15).",
        'The average and range method is quick; two-way ANOVA, with the part × operator interaction, is the reference method and more accurate. The interaction is pooled with repeatability when it is not significant (p > 0.25). Constants K1, K2 and K3 apply to 5.15 σ; at 6 σ they are scaled (× 6 / 5.15).',
        'El método de promedio y rango es rápido; el ANOVA de dos factores, con la interacción pieza × operador, es el método de referencia, más preciso. La interacción se agrupa con la repetibilidad cuando no es significativa (p > 0,25). Las constantes K1, K2 y K3 valen para 5,15 σ; a 6 σ se ajustan a escala (× 6 / 5,15).',
        'طريقة المتوسط والمدى سريعة؛ أما ANOVA ثنائي العوامل مع تفاعل القطعة × المشغّل فهي الطريقة المرجعية والأدق. يُدمج التفاعل مع قابلية التكرار عندما لا يكون دالًّا (p > 0.25). تصلح الثوابت K1 وK2 وK3 لـ 5.15 σ؛ وعند 6 σ تُعدَّل بالنسبة (× 6 / 5.15).',
        '平均・範囲法は迅速です。部品 × 測定者の交互作用を含む二元配置分散分析は、より精密な基準手法です。交互作用が有意でない場合（p > 0.25）は繰返し性にプールします。定数 K1、K2、K3 は 5.15 σ 用で、6 σ ではスケーリングします（× 6 / 5.15）。',
        '均值极差法速度快；含零件 × 操作员交互作用的双因素方差分析是更精确的参考方法。交互作用不显著（p > 0.25）时并入重复性。常数 K1、K2、K3 适用于 5.15 σ；采用 6 σ 时按比例缩放（× 6 / 5.15）。'),

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
