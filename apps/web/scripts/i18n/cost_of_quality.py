# -*- coding: utf-8 -*-
"""Libellés Coût de la qualité (modèle PAF, ADR 0075).

Vue mois ou année ; une ligne est une imputation, et une ligne de contrôle de
pièces exige la référence, le nombre de pièces, le lot et la date de
réception ou de fabrication. Les libellés du catalogue livré sont traduits
ici par leur code ; ceux que le client tape restent dans sa langue.
"""

TRANSLATIONS = {
    'nav.cout-qualite': (
        'Coûts de non-qualité', 'Cost of quality', 'Costes de la no calidad',
        'تكاليف عدم الجودة', '品質コスト', '质量成本'),

    # ---------- page ----------
    'coq.eyebrow': ('Pilotage', 'Steering', 'Pilotaje', 'القيادة', '管理', '管控'),
    'coq.title': (
        'Coûts de non-qualité', 'Cost of quality', 'Costes de la no calidad',
        'تكاليف عدم الجودة', '品質コスト', '质量成本'),
    'coq.subtitle': (
        'Recensez et pilotez les coûts liés à la qualité selon le modèle PAF (Prévention – Appréciation – Défaillances). Chaque ligne représente une dépense réelle, imputée à sa date.',
        'Record and manage quality-related costs using the PAF model (Prevention – Appraisal – Failure). Each line is an actual expense, booked on its date.',
        'Registre y gestione los costes relacionados con la calidad según el modelo PAF (Prevención – Evaluación – Fallos). Cada línea es un gasto real, imputado en su fecha.',
        'سجّل وأدِر التكاليف المرتبطة بالجودة وفق نموذج PAF (الوقاية – التقييم – الإخفاقات). يمثل كل سطر نفقة فعلية مُقيَّدة بتاريخها.',
        'PAFモデル（予防・評価・失敗）に基づき品質関連コストを記録・管理します。各行は計上日付きの実際の支出です。',
        '按 PAF 模型（预防—鉴定—失败）记录和管理与质量相关的成本。每一行都是按其日期入账的实际支出。'),

    'coq.period-mode': (
        'Granularité de la période', 'Period granularity', 'Granularidad del periodo',
        'دقة الفترة', '期間の単位', '期间粒度'),
    'coq.mode-month': ('Mois', 'Month', 'Mes', 'شهر', '月', '月'),
    'coq.mode-year': ('Année', 'Year', 'Año', 'سنة', '年', '年'),
    'coq.period-nav': (
        'Changer de période', 'Change period', 'Cambiar de periodo',
        'تغيير الفترة', '期間を変更', '切换期间'),
    'coq.previous': (
        'Période précédente', 'Previous period', 'Periodo anterior',
        'الفترة السابقة', '前の期間', '上一期间'),
    'coq.next': (
        'Période suivante', 'Next period', 'Periodo siguiente',
        'الفترة التالية', '次の期間', '下一期间'),
    'coq.currency': ('Devise', 'Currency', 'Moneda', 'العملة', '通貨', '货币'),
    'coq.load-failed': (
        "Le rapport n'a pas pu être chargé.", 'The report could not be loaded.',
        'No se pudo cargar el informe.', 'تعذّر تحميل التقرير.',
        'レポートを読み込めませんでした。', '无法加载报告。'),
    'coq.error': (
        'Le coût de la qualité est indisponible.', 'Cost of quality is unavailable.',
        'El coste de la calidad no está disponible.', 'تكلفة الجودة غير متاحة.',
        '品質コストを利用できません。', '质量成本暂不可用。'),

    # ---------- synthèse ----------
    'coq.summary': ('Synthèse', 'Summary', 'Resumen', 'ملخص', '概要', '概览'),
    'coq.total-label': (
        'Coût total de non-qualité (CNQ)', 'Total cost of poor quality (COPQ)',
        'Coste total de la no calidad (CNC)', 'إجمالي تكلفة عدم الجودة',
        '不良品質コスト合計（COPQ）', '劣质成本总额（COPQ）'),
    'coq.conformance-costs': (
        'Coûts de conformité (investissement)', 'Cost of conformance (investment)',
        'Costes de conformidad (inversión)', 'تكاليف المطابقة (استثمار)',
        '適合コスト（投資）', '符合性成本（投入）'),
    'coq.non-conformance-costs': (
        'Coûts de non-conformité (pertes)', 'Cost of non-conformance (losses)',
        'Costes de no conformidad (pérdidas)', 'تكاليف عدم المطابقة (خسائر)',
        '不適合コスト（損失）', '不符合成本（损失）'),
    'coq.ratio-sentence': (
        "Ratio conformité / non-conformité : {$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG}. Un CNQ élevé face à un faible investissement en prévention indique un potentiel d'amélioration : chaque euro investi en prévention évite généralement plusieurs euros de défaillance.",
        'Conformance / non-conformance ratio: {$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG}. A high cost of poor quality against a low prevention spend points to room for improvement: every euro invested in prevention usually avoids several euros of failure.',
        'Ratio conformidad / no conformidad: {$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG}. Un coste de no calidad elevado frente a una baja inversión en prevención indica un potencial de mejora: cada euro invertido en prevención suele evitar varios euros de fallos.',
        'نسبة المطابقة / عدم المطابقة: {$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG}. إن ارتفاع تكلفة عدم الجودة مقابل استثمار ضعيف في الوقاية يدل على إمكانية للتحسين: فكل يورو يُستثمر في الوقاية يجنّب عادةً عدة يوروهات من الإخفاقات.',
        '適合／不適合比率：{$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG}。予防への投資が少ないまま不良品質コストが高い場合、改善の余地があります。予防に投じた1ユーロは、通常その数倍の失敗コストを防ぎます。',
        '符合 / 不符合比率：{$START_TAG_STRONG}{$INTERPOLATION}{$CLOSE_TAG_STRONG}。劣质成本高而预防投入低，说明存在改进空间：在预防上每投入一欧元，通常可避免数欧元的失败损失。'),
    'coq.ratio-none': (
        'Aucune perte de non-conformité sur la période : le ratio ne se calcule pas.',
        'No non-conformance losses in this period: the ratio cannot be calculated.',
        'Ninguna pérdida por no conformidad en el periodo: el ratio no se calcula.',
        'لا توجد خسائر عدم مطابقة في هذه الفترة: لا يمكن حساب النسبة.',
        'この期間に不適合損失はありません。比率は算出されません。',
        '本期间无不符合损失：无法计算比率。'),

    # ---------- histogramme ----------
    'coq.monthly-chart': (
        'Répartition mensuelle', 'Monthly breakdown', 'Desglose mensual',
        'التوزيع الشهري', '月別内訳', '按月分布'),
    'coq.monthly-title': (
        'Répartition par mois', 'Breakdown by month', 'Desglose por mes',
        'التوزيع حسب الشهر', '月ごとの内訳', '逐月分布'),
    'coq.year-read-only': (
        "Vue annuelle en lecture seule : chaque libellé y est cumulé sur l'année. La saisie se fait dans le mois concerné.",
        'Yearly view is read-only: each label is totalled over the year. Entries are made in the relevant month.',
        'Vista anual de solo lectura: cada concepto se acumula en el año. La introducción se hace en el mes correspondiente.',
        'العرض السنوي للقراءة فقط: يُجمَّع كل بند على مدار السنة. يتم الإدخال في الشهر المعني.',
        '年間ビューは閲覧専用です。各項目は年間で合算されます。入力は該当する月で行います。',
        '年度视图为只读：每个科目按全年累计。请在对应月份中录入。'),

    # ---------- colonnes et blocs ----------
    'coq.conformance-heading': (
        'Coûts de conformité', 'Cost of conformance', 'Costes de conformidad',
        'تكاليف المطابقة', '適合コスト', '符合性成本'),
    'coq.conformance-kind': (
        'dépenses volontaires', 'voluntary spending', 'gastos voluntarios',
        'نفقات طوعية', '自発的な支出', '主动支出'),
    'coq.non-conformance-heading': (
        'Coûts de non-conformité', 'Cost of non-conformance', 'Costes de no conformidad',
        'تكاليف عدم المطابقة', '不適合コスト', '不符合成本'),
    'coq.non-conformance-kind': (
        'pertes subies', 'losses incurred', 'pérdidas sufridas',
        'خسائر متكبَّدة', '被った損失', '已发生损失'),

    'coq.cat.prevention-badge': ('P', 'P', 'P', 'و', '予', '防'),
    'coq.cat.prevention-title': (
        'Coûts de prévention', 'Prevention costs', 'Costes de prevención',
        'تكاليف الوقاية', '予防コスト', '预防成本'),
    'coq.cat.prevention-desc': (
        "Dépenses engagées en amont pour empêcher l'apparition de défauts : formation, planification qualité, conception, fiabilisation des processus.",
        'Upfront spending to stop defects from occurring: training, quality planning, design, process reliability.',
        'Gastos realizados de antemano para impedir la aparición de defectos: formación, planificación de la calidad, diseño, fiabilización de los procesos.',
        'نفقات مسبقة لمنع ظهور العيوب: التدريب، وتخطيط الجودة، والتصميم، وتعزيز موثوقية العمليات.',
        '欠陥の発生を防ぐための事前の支出：教育、品質計画、設計、プロセスの信頼性向上。',
        '为防止缺陷出现而预先投入的支出：培训、质量策划、设计、过程可靠性提升。'),
    'coq.cat.appraisal-badge': ('A', 'A', 'E', 'ت', '評', '鉴'),
    'coq.cat.appraisal-title': (
        "Coûts d'appréciation (détection)", 'Appraisal costs (detection)',
        'Costes de evaluación (detección)', 'تكاليف التقييم (الكشف)',
        '評価コスト（検出）', '鉴定成本（检测）'),
    'coq.cat.appraisal-desc': (
        'Dépenses liées à la vérification de la conformité : contrôles, inspections et essais qui détectent les défauts avant livraison.',
        'Spending to verify conformance: checks, inspections and tests that catch defects before delivery.',
        'Gastos ligados a la verificación de la conformidad: controles, inspecciones y ensayos que detectan los defectos antes de la entrega.',
        'نفقات التحقق من المطابقة: الفحوص والتفتيشات والاختبارات التي تكشف العيوب قبل التسليم.',
        '適合性の検証に関する支出：出荷前に欠陥を検出する検査・点検・試験。',
        '与符合性验证相关的支出：在交付前发现缺陷的检查、检验和试验。'),
    'coq.cat.internal-badge': ('IF', 'IF', 'FI', 'إد', '内', '内'),
    'coq.cat.internal-title': (
        'Coûts des anomalies internes', 'Internal failure costs', 'Costes de fallos internos',
        'تكاليف الإخفاقات الداخلية', '内部失敗コスト', '内部失败成本'),
    'coq.cat.internal-desc': (
        'Coûts des défauts détectés avant livraison au client : rebuts, retouches, arrêts de production liés à la non-qualité.',
        'Cost of defects found before delivery to the customer: scrap, rework, production stoppages caused by poor quality.',
        'Costes de los defectos detectados antes de la entrega al cliente: desechos, retoques, paradas de producción debidas a la no calidad.',
        'تكاليف العيوب المكتشفة قبل التسليم إلى العميل: الهالك، وإعادة العمل، وتوقفات الإنتاج الناتجة عن عدم الجودة.',
        '顧客への出荷前に検出された欠陥のコスト：スクラップ、手直し、品質不良による生産停止。',
        '在交付客户前发现的缺陷成本：报废、返工、因质量问题导致的停产。'),
    'coq.cat.external-badge': ('EF', 'EF', 'FE', 'إخ', '外', '外'),
    'coq.cat.external-title': (
        'Coûts des anomalies externes', 'External failure costs', 'Costes de fallos externos',
        'تكاليف الإخفاقات الخارجية', '外部失敗コスト', '外部失败成本'),
    'coq.cat.external-desc': (
        'Coûts des défauts découverts après livraison : réclamations, retours, garanties et impact sur la relation client.',
        'Cost of defects found after delivery: complaints, returns, warranties and the impact on the customer relationship.',
        'Costes de los defectos descubiertos tras la entrega: reclamaciones, devoluciones, garantías e impacto en la relación con el cliente.',
        'تكاليف العيوب المكتشفة بعد التسليم: الشكاوى، والمرتجعات، والضمانات، والأثر على العلاقة مع العميل.',
        '出荷後に発見された欠陥のコスト：クレーム、返品、保証、顧客関係への影響。',
        '交付后发现的缺陷成本：投诉、退货、保修以及对客户关系的影响。'),

    # ---------- libellés livrés ----------
    'coq.label.prevention-training': (
        'Formation qualité du personnel', 'Staff quality training', 'Formación en calidad del personal',
        'تدريب العاملين على الجودة', '従業員の品質教育', '员工质量培训'),
    'coq.label.prevention-planning': (
        'Planification et système qualité', 'Quality planning and system',
        'Planificación y sistema de calidad', 'تخطيط الجودة ونظامها', '品質計画と品質システム', '质量策划与质量体系'),
    'coq.label.prevention-audits': (
        'Audits qualité internes', 'Internal quality audits', 'Auditorías de calidad internas',
        'تدقيقات الجودة الداخلية', '内部品質監査', '内部质量审核'),
    'coq.label.prevention-design-review': (
        'Revue de conception / AMDEC', 'Design review / FMEA', 'Revisión de diseño / AMFE',
        'مراجعة التصميم / FMEA', '設計審査／FMEA', '设计评审 / FMEA'),
    'coq.label.appraisal-incoming': (
        'Contrôle réception matières', 'Incoming material inspection', 'Control de recepción de materiales',
        'فحص المواد عند الاستلام', '受入材料検査', '来料检验'),
    'coq.label.appraisal-in-process': (
        'Inspections en cours de production', 'In-process inspections', 'Inspecciones durante la producción',
        'عمليات التفتيش أثناء الإنتاج', '工程内検査', '过程检验'),
    'coq.label.appraisal-testing': (
        'Essais et étalonnage', 'Testing and calibration', 'Ensayos y calibración',
        'الاختبارات والمعايرة', '試験と校正', '试验与校准'),
    'coq.label.appraisal-product-audit': (
        'Audit produit / process', 'Product / process audit', 'Auditoría de producto / proceso',
        'تدقيق المنتج / العملية', '製品／工程監査', '产品 / 过程审核'),
    'coq.label.internal-scrap': (
        'Rebuts et mise au rebut', 'Scrap and disposal', 'Desechos y eliminación',
        'الهالك والتخلص منه', 'スクラップと廃棄', '报废与处置'),
    'coq.label.internal-rework': (
        'Retouches et réparations', 'Rework and repairs', 'Retoques y reparaciones',
        'إعادة العمل والإصلاحات', '手直しと修理', '返工与返修'),
    'coq.label.internal-reinspection': (
        'Re-contrôles après retouche', 'Re-inspection after rework', 'Recontroles tras retoque',
        'إعادة الفحص بعد إعادة العمل', '手直し後の再検査', '返工后复检'),
    'coq.label.internal-downtime': (
        'Arrêts de production liés aux défauts', 'Defect-related production stoppages',
        'Paradas de producción por defectos', 'توقفات الإنتاج المرتبطة بالعيوب',
        '欠陥に起因する生産停止', '缺陷导致的停产'),
    'coq.label.external-complaints': (
        'Réclamations et traitement litiges', 'Complaints and dispute handling',
        'Reclamaciones y gestión de litigios', 'الشكاوى ومعالجة النزاعات',
        'クレームと紛争対応', '投诉与纠纷处理'),
    'coq.label.external-returns': (
        'Retours produits et remplacements', 'Product returns and replacements',
        'Devoluciones y sustituciones de productos', 'مرتجعات المنتجات واستبدالها',
        '製品の返品と交換', '产品退货与更换'),
    'coq.label.external-warranty': (
        'Garanties et interventions terrain', 'Warranty and field service',
        'Garantías e intervenciones en campo', 'الضمانات والتدخلات الميدانية',
        '保証と現地対応', '保修与现场服务'),
    'coq.label.external-penalties': (
        'Pénalités contractuelles', 'Contractual penalties', 'Penalizaciones contractuales',
        'الغرامات التعاقدية', '契約上の違約金', '合同罚金'),

    # ---------- lignes ----------
    'coq.line-lot': ('lot {$INTERPOLATION}', 'lot {$INTERPOLATION}', 'lote {$INTERPOLATION}',
                     'الدفعة {$INTERPOLATION}', 'ロット {$INTERPOLATION}', '批次 {$INTERPOLATION}'),
    'coq.line-pieces': ('pcs', 'pcs', 'uds', 'قطعة', '個', '件'),
    'coq.line-count': (
        '{$INTERPOLATION} imputation(s)', '{$INTERPOLATION} entry(ies)', '{$INTERPOLATION} imputación(es)',
        '{$INTERPOLATION} قيد/قيود', '{$INTERPOLATION} 件の計上', '{$INTERPOLATION} 笔入账'),
    'coq.add-line': (
        'Ajouter une ligne', 'Add a line', 'Añadir una línea', 'إضافة سطر', '行を追加', '添加一行'),
    'coq.delete-line': (
        'Supprimer la ligne', 'Delete the line', 'Eliminar la línea', 'حذف السطر', '行を削除', '删除该行'),
    'coq.delete-title': (
        'Supprimer cette ligne ?', 'Delete this line?', '¿Eliminar esta línea?',
        'حذف هذا السطر؟', 'この行を削除しますか？', '删除此行？'),
    'coq.delete-message': (
        "Le montant sort des totaux de la période. La suppression est tracée dans le journal d'audit.",
        'The amount is removed from the period totals. The deletion is recorded in the audit log.',
        'El importe sale de los totales del periodo. La eliminación queda registrada en el registro de auditoría.',
        'يُستبعد المبلغ من إجماليات الفترة. ويُسجَّل الحذف في سجل التدقيق.',
        '金額は期間の合計から除外されます。削除は監査ログに記録されます。',
        '该金额将从本期合计中移除。删除操作会记录在审计日志中。'),
    'coq.amount-invalid': (
        'Montant invalide : un nombre positif ou nul.', 'Invalid amount: a positive number or zero.',
        'Importe no válido: un número positivo o cero.', 'مبلغ غير صالح: رقم موجب أو صفر.',
        '金額が無効です。0以上の数値を入力してください。', '金额无效：须为正数或零。'),

    # ---------- fenêtre d'une ligne ----------
    'coq.dialog.new-title': (
        'Nouvelle ligne', 'New line', 'Nueva línea', 'سطر جديد', '新しい行', '新建一行'),
    'coq.dialog.submit': ('Enregistrer', 'Save', 'Guardar', 'حفظ', '保存', '保存'),
    'coq.dialog.read-only': (
        'Consultation seule : la saisie des coûts est réservée aux profils de pilotage qualité.',
        'View only: entering costs is restricted to quality management profiles.',
        'Solo consulta: la introducción de costes está reservada a los perfiles de gestión de la calidad.',
        'عرض فقط: إدخال التكاليف مقصور على ملفات قيادة الجودة.',
        '閲覧のみ：コストの入力は品質管理担当のプロファイルに限られます。',
        '仅供查看：成本录入仅限质量管理角色。'),
    'coq.dialog.field-label': (
        'Libellé de la ligne', 'Line label', 'Concepto de la línea', 'بند السطر', '行の項目', '行科目'),
    'coq.dialog.new-label-hint': (
        'Nouveau libellé : il rejoindra la liste de cette famille.',
        'New label: it will be added to this category’s list.',
        'Concepto nuevo: se añadirá a la lista de esta familia.',
        'بند جديد: سيُضاف إلى قائمة هذه الفئة.',
        '新しい項目：この区分のリストに追加されます。',
        '新科目：将加入此类别的列表。'),
    'coq.dialog.label-required': (
        'Le libellé est requis.', 'The label is required.', 'El concepto es obligatorio.',
        'البند مطلوب.', '項目は必須です。', '科目为必填项。'),
    'coq.dialog.new-label-parts': (
        'Ce libellé porte sur un contrôle qualité de pièces',
        'This label covers a quality inspection of parts',
        'Este concepto corresponde a un control de calidad de piezas',
        'يتعلق هذا البند بفحص جودة القطع',
        'この項目は部品の品質検査に関するものです',
        '该科目涉及零件质量检验'),
    'coq.dialog.field-amount': (
        'Montant ({$INTERPOLATION})', 'Amount ({$INTERPOLATION})', 'Importe ({$INTERPOLATION})',
        'المبلغ ({$INTERPOLATION})', '金額（{$INTERPOLATION}）', '金额（{$INTERPOLATION}）'),
    'coq.dialog.amount-invalid': (
        'Montant requis, positif ou nul.', 'Amount required, positive or zero.',
        'Importe obligatorio, positivo o cero.', 'المبلغ مطلوب، موجب أو صفر.',
        '金額は必須です（0以上）。', '金额为必填项，须为正数或零。'),
    'coq.dialog.field-date': (
        "Date d'imputation", 'Booking date', 'Fecha de imputación', 'تاريخ القيد', '計上日', '入账日期'),
    'coq.dialog.date-open': (
        'Ouvrir le calendrier', 'Open the calendar', 'Abrir el calendario',
        'فتح التقويم', 'カレンダーを開く', '打开日历'),
    'coq.dialog.date-hint': (
        'Elle range la ligne dans son mois.', 'It places the line in its month.',
        'Sitúa la línea en su mes.', 'يضع السطر في شهره.', '行はこの日付の月に計上されます。', '该日期决定此行归属的月份。'),
    'coq.dialog.date-required': (
        "La date d'imputation est requise.", 'The booking date is required.',
        'La fecha de imputación es obligatoria.', 'تاريخ القيد مطلوب.', '計上日は必須です。', '入账日期为必填项。'),
    'coq.dialog.field-responsible': (
        'Responsable', 'Owner', 'Responsable', 'المسؤول', '責任者', '负责人'),
    'coq.dialog.responsible-required': (
        'Le responsable est requis.', 'The owner is required.', 'El responsable es obligatorio.',
        'المسؤول مطلوب.', '責任者は必須です。', '负责人为必填项。'),
    'coq.dialog.parts-legend': (
        'Contrôle qualité de pièces', 'Quality inspection of parts', 'Control de calidad de piezas',
        'فحص جودة القطع', '部品の品質検査', '零件质量检验'),
    'coq.dialog.field-part-reference': (
        'Référence pièce ou produit', 'Part or product reference', 'Referencia de pieza o producto',
        'مرجع القطعة أو المنتج', '部品または製品の品番', '零件或产品编号'),
    'coq.dialog.part-reference-required': (
        'La référence est requise.', 'The reference is required.', 'La referencia es obligatoria.',
        'المرجع مطلوب.', '品番は必須です。', '编号为必填项。'),
    'coq.dialog.field-part-quantity': (
        'Nombre de pièces', 'Number of parts', 'Número de piezas', 'عدد القطع', '部品数', '零件数量'),
    'coq.dialog.part-quantity-invalid': (
        "Un nombre entier d'au moins 1.", 'A whole number of at least 1.',
        'Un número entero de al menos 1.', 'عدد صحيح لا يقل عن 1.', '1以上の整数を入力してください。', '须为不小于 1 的整数。'),
    'coq.dialog.field-lot': ('Lot', 'Lot', 'Lote', 'الدفعة', 'ロット', '批次'),
    'coq.dialog.lot-required': (
        'Le lot est requis.', 'The lot is required.', 'El lote es obligatorio.',
        'الدفعة مطلوبة.', 'ロットは必須です。', '批次为必填项。'),
    'coq.dialog.field-received': (
        'Date de réception ou fabrication', 'Receipt or manufacturing date',
        'Fecha de recepción o fabricación', 'تاريخ الاستلام أو التصنيع', '受入日または製造日', '收货或生产日期'),
    'coq.dialog.received-required': (
        'Cette date est requise.', 'This date is required.', 'Esta fecha es obligatoria.',
        'هذا التاريخ مطلوب.', 'この日付は必須です。', '该日期为必填项。'),
    'coq.dialog.field-comment': (
        'Commentaire (facultatif)', 'Comment (optional)', 'Comentario (opcional)',
        'تعليق (اختياري)', 'コメント（任意）', '备注（可选）'),
    'coq.dialog.required-legend': (
        '* champ obligatoire', '* required field', '* campo obligatorio',
        '* حقل إلزامي', '* 必須項目', '* 必填字段'),

    'coq.dialog.blocked-label': (
        'Choisissez ou saisissez un libellé.', 'Choose or type a label.', 'Elija o escriba un concepto.',
        'اختر بندًا أو اكتبه.', '項目を選択または入力してください。', '请选择或输入科目。'),
    'coq.dialog.blocked-amount': (
        'Indiquez un montant positif ou nul.', 'Enter a positive amount or zero.',
        'Indique un importe positivo o cero.', 'أدخل مبلغًا موجبًا أو صفرًا.',
        '0以上の金額を入力してください。', '请输入正数或零的金额。'),
    'coq.dialog.blocked-responsible': (
        'Indiquez le responsable.', 'Enter the owner.', 'Indique el responsable.',
        'أدخل المسؤول.', '責任者を入力してください。', '请填写负责人。'),
    'coq.dialog.blocked-date': (
        "Indiquez la date d'imputation.", 'Enter the booking date.', 'Indique la fecha de imputación.',
        'أدخل تاريخ القيد.', '計上日を入力してください。', '请填写入账日期。'),
    'coq.dialog.blocked-parts': (
        'Un contrôle de pièces exige la référence, le nombre de pièces, le lot et la date de réception ou de fabrication.',
        'A parts inspection requires the reference, the number of parts, the lot and the receipt or manufacturing date.',
        'Un control de piezas exige la referencia, el número de piezas, el lote y la fecha de recepción o fabricación.',
        'يتطلب فحص القطع المرجع وعدد القطع والدفعة وتاريخ الاستلام أو التصنيع.',
        '部品検査には、品番、部品数、ロット、受入日または製造日が必要です。',
        '零件检验须填写编号、零件数量、批次以及收货或生产日期。'),
}
