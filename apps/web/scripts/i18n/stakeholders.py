# -*- coding: utf-8 -*-
"""Table i18n — Registre des parties intéressées (ISO 9001 §4.2, ADR 0104).

Le registre, la fiche, la matrice d'évaluation, les trois assistants et leurs
fenêtres de saisie. id: (fr, en, es, ar, ja, zh).
"""

TRANSLATIONS = {

    # --- navigation et en-tête ---------------------------------------------
    'nav.stakeholders': (
        'Parties prenantes', 'Stakeholders', 'Partes interesadas',
        'الأطراف المعنية', '利害関係者', '相关方'),
    'stakeholders.export': (
        'Exporter', 'Export', 'Exportar', 'تصدير', 'エクスポート', '导出'),
    'stakeholders.title': (
        'Parties prenantes', 'Stakeholders', 'Partes interesadas',
        'الأطراف المعنية', '利害関係者', '相关方'),
    'stakeholders.subtitle': (
        "Contexte de l'organisme : qui attend quoi de vous, et comment vous vous en assurez.",
        'Context of the organization: who expects what from you, and how you make sure of it.',
        'Contexto de la organización: quién espera qué de usted y cómo se asegura de ello.',
        'سياق المنظمة: من يتوقع منك ماذا، وكيف تتحقق من ذلك.',
        '組織の状況：誰が何を期待しているのか、そしてそれをどう確かめるのか。',
        '组织环境：谁对您有何期望，以及您如何加以确认。'),
    'stakeholders.new': (
        'Nouvelle partie prenante', 'New stakeholder', 'Nueva parte interesada',
        'طرف معني جديد', '新しい利害関係者', '新增相关方'),
    'stakeholders.load-failed': (
        "Le registre des parties prenantes n'a pas pu être chargé.",
        'The stakeholder register could not be loaded.',
        'No se pudo cargar el registro de partes interesadas.',
        'لم يتمكّن سجل الأطراف المعنية من التحميل.',
        '利害関係者の登録簿を読み込めませんでした。',
        '无法加载相关方登记册。'),
    'stakeholders.sheet-failed': (
        "La fiche n'a pas pu être ouverte.", 'The sheet could not be opened.',
        'No se pudo abrir la ficha.', 'لم يتمكّن السجل من الفتح.',
        'シートを開けませんでした。', '无法打开该档案。'),
    'stakeholders.sheet.aria': (
        'Fiche de la partie prenante', 'Stakeholder sheet', 'Ficha de la parte interesada',
        'سجل الطرف المعني', '利害関係者シート', '相关方档案'),

    # --- tuiles -------------------------------------------------------------
    'stakeholders.tile.total': (
        'Parties prenantes', 'Stakeholders', 'Partes interesadas',
        'الأطراف المعنية', '利害関係者', '相关方'),
    'stakeholders.tile.sides': (
        'Internes / externes', 'Internal / external', 'Internas / externas',
        'داخلية / خارجية', '内部／外部', '内部／外部'),
    'stakeholders.tile.critical': (
        'Criticité élevée', 'High criticality', 'Criticidad alta',
        'أهمية عالية', '重要度：高', '高关键性'),
    'stakeholders.tile.overdue': (
        'Revues en retard', 'Overdue reviews', 'Revisiones atrasadas',
        'مراجعات متأخرة', '期限超過のレビュー', '逾期复评'),
    'stakeholders.tile.to-determine': (
        'Pertinence à trancher', 'Relevance to decide', 'Pertinencia por decidir',
        'الأهمية قيد التحديد', '関連性は未決定', '相关性待定'),
    'stakeholders.tile.to-reassess': (
        'Exigences à réévaluer', 'Requirements to reassess', 'Requisitos por reevaluar',
        'متطلبات تحتاج إعادة تقييم', '再評価が必要な要求事項', '待重新评估的要求'),

    # --- onglets ------------------------------------------------------------
    'stakeholders.tabs': (
        'Vues du registre', 'Register views', 'Vistas del registro',
        'عروض السجل', '登録簿のビュー', '登记册视图'),
    'stakeholders.tab.register': (
        'Registre', 'Register', 'Registro', 'السجل', '登録簿', '登记册'),
    'stakeholders.tab.matrix': (
        "Matrice d'évaluation", 'Assessment matrix', 'Matriz de evaluación',
        'مصفوفة التقييم', '評価マトリクス', '评估矩阵'),
    'stakeholders.tab.assistant': (
        'Assistant IA', 'AI assistant', 'Asistente de IA',
        'المساعد الذكي', 'AI アシスタント', 'AI 助手'),

    # --- filtres et colonnes du registre ------------------------------------
    'stakeholders.filters': (
        'Filtrer le registre des parties prenantes', 'Filter the stakeholder register',
        'Filtrar el registro de partes interesadas', 'تصفية سجل الأطراف المعنية',
        '利害関係者の登録簿を絞り込む', '筛选相关方登记册'),
    'stakeholders.filter.search': (
        'Rechercher une partie prenante', 'Search for a stakeholder',
        'Buscar una parte interesada', 'البحث عن طرف معني',
        '利害関係者を検索', '搜索相关方'),
    'stakeholders.filter.side': (
        'Interne / externe', 'Internal / external', 'Interna / externa',
        'داخلي / خارجي', '内部／外部', '内部／外部'),
    'stakeholders.filter.criticality': (
        'Niveau de risque', 'Risk level', 'Nivel de riesgo',
        'مستوى الخطر', 'リスクレベル', '风险等级'),
    'stakeholders.filter.clear': (
        'Effacer les filtres', 'Clear filters', 'Borrar los filtros',
        'مسح عوامل التصفية', '絞り込みを解除', '清除筛选'),
    'stakeholders.col.name': (
        'Partie prenante', 'Stakeholder', 'Parte interesada',
        'الطرف المعني', '利害関係者', '相关方'),
    'stakeholders.col.category': (
        'Catégorie', 'Category', 'Categoría', 'الفئة', 'カテゴリー', '类别'),
    'stakeholders.col.side': (
        'Interne / externe', 'Internal / external', 'Interna / externa',
        'داخلي / خارجي', '内部／外部', '内部／外部'),
    'stakeholders.col.criticality': (
        'Criticité', 'Criticality', 'Criticidad', 'الأهمية', '重要度', '关键性'),
    'stakeholders.col.relevance': (
        'Pertinence', 'Relevance', 'Pertinencia', 'الأهمية النسبية', '関連性', '相关性'),
    'stakeholders.col.requirements': (
        'Exigences', 'Requirements', 'Requisitos', 'المتطلبات', '要求事項', '要求'),
    'stakeholders.col.review': (
        'Dernière revue', 'Last review', 'Última revisión',
        'آخر مراجعة', '最終レビュー', '最近一次复评'),
    'stakeholders.review-overdue': (
        'Réévaluation en retard', 'Review overdue', 'Reevaluación atrasada',
        'إعادة التقييم متأخرة', '再評価が期限超過', '重新评估已逾期'),
    'stakeholders.empty': (
        "Aucune partie prenante inscrite. Ajoutez-en une, ou demandez une liste à l'assistant IA.",
        'No stakeholder registered yet. Add one, or ask the AI assistant for a list.',
        'Ninguna parte interesada registrada. Añada una o pida una lista al asistente de IA.',
        'لا يوجد أي طرف معني مسجّل. أضف طرفًا، أو اطلب قائمة من المساعد الذكي.',
        '登録された利害関係者はまだありません。追加するか、AI アシスタントに一覧を求めてください。',
        '尚未登记任何相关方。请添加一个，或向 AI 助手索取建议清单。'),
    'stakeholders.empty-filtered': (
        'Aucune partie prenante ne correspond à ces filtres.',
        'No stakeholder matches these filters.',
        'Ninguna parte interesada coincide con estos filtros.',
        'لا يوجد طرف معني مطابق لعوامل التصفية هذه.',
        'この絞り込みに該当する利害関係者はありません。',
        '没有符合这些筛选条件的相关方。'),

    # --- catégories ---------------------------------------------------------
    'stakeholders.category.customers': (
        'Clients et utilisateurs finaux', 'Customers and end users',
        'Clientes y usuarios finales', 'العملاء والمستخدمون النهائيون',
        '顧客およびエンドユーザー', '客户与最终用户'),
    'stakeholders.category.suppliers': (
        'Fournisseurs et sous-traitants', 'Suppliers and subcontractors',
        'Proveedores y subcontratistas', 'المورّدون والمتعاقدون من الباطن',
        '供給者および外注先', '供应商与分包方'),
    'stakeholders.category.employees': (
        'Collaborateurs et représentants du personnel',
        'Employees and staff representatives',
        'Colaboradores y representantes del personal',
        'العاملون وممثلو الموظفين', '従業員および従業員代表', '员工与职工代表'),
    'stakeholders.category.governance': (
        'Direction et actionnaires', 'Management and shareholders',
        'Dirección y accionistas', 'الإدارة والمساهمون',
        '経営層および株主', '管理层与股东'),
    'stakeholders.category.regulators': (
        'Autorités réglementaires', 'Regulatory authorities',
        'Autoridades reglamentarias', 'الجهات التنظيمية',
        '規制当局', '监管机构'),
    'stakeholders.category.certification': (
        "Organismes de certification et d'accréditation",
        'Certification and accreditation bodies',
        'Organismos de certificación y acreditación',
        'جهات إصدار الشهادات والاعتماد', '認証機関および認定機関', '认证与认可机构'),
    'stakeholders.category.partners': (
        'Partenaires commerciaux', 'Business partners', 'Socios comerciales',
        'الشركاء التجاريون', '事業パートナー', '商业伙伴'),
    'stakeholders.category.providers': (
        'Prestataires de services', 'Service providers', 'Prestadores de servicios',
        'مقدّمو الخدمات', 'サービス提供者', '服务提供方'),
    'stakeholders.category.financial': (
        'Assureurs et partenaires financiers', 'Insurers and financial partners',
        'Aseguradoras y socios financieros', 'شركات التأمين والشركاء الماليون',
        '保険会社および金融パートナー', '保险公司与金融伙伴'),
    'stakeholders.category.communities': (
        'Communautés locales et acteurs externes',
        'Local communities and external actors',
        'Comunidades locales y actores externos',
        'المجتمعات المحلية والجهات الخارجية', '地域社会および外部関係者', '当地社区与外部方'),
    'stakeholders.category.other': (
        'Autres parties intéressées', 'Other interested parties',
        'Otras partes interesadas', 'أطراف معنية أخرى',
        'その他の利害関係者', '其他相关方'),

    # --- interne / externe, impacts, pertinence, natures, statuts ----------
    'stakeholders.side.internal': (
        'Interne', 'Internal', 'Interna', 'داخلي', '内部', '内部'),
    'stakeholders.side.external': (
        'Externe', 'External', 'Externa', 'خارجي', '外部', '外部'),
    'stakeholders.impact.low': (
        'Faible', 'Low', 'Bajo', 'منخفض', '低', '低'),
    'stakeholders.impact.medium': (
        'Moyen', 'Medium', 'Medio', 'متوسط', '中', '中'),
    'stakeholders.impact.high': (
        'Élevé', 'High', 'Alto', 'عالٍ', '高', '高'),
    'stakeholders.relevance.relevant': (
        'Pertinente', 'Relevant', 'Pertinente', 'ذات أهمية', '関連あり', '相关'),
    'stakeholders.relevance.not-relevant': (
        'Non pertinente', 'Not relevant', 'No pertinente',
        'غير ذات أهمية', '関連なし', '不相关'),
    'stakeholders.relevance.to-determine': (
        'À déterminer', 'To be determined', 'Por determinar',
        'قيد التحديد', '未決定', '待确定'),
    'stakeholders.nature.contractual': (
        'Contractuel', 'Contractual', 'Contractual', 'تعاقدي', '契約上', '合同性'),
    'stakeholders.nature.regulatory': (
        'Réglementaire', 'Regulatory', 'Reglamentario', 'تنظيمي', '法規上', '法规性'),
    'stakeholders.nature.normative': (
        'Normatif', 'Normative', 'Normativo', 'معياري', '規格上', '标准性'),
    'stakeholders.nature.voluntary': (
        'Volontaire', 'Voluntary', 'Voluntario', 'طوعي', '自主的', '自愿性'),
    'stakeholders.nature.internal': (
        'Interne', 'Internal', 'Interno', 'داخلي', '社内', '内部'),
    'stakeholders.status.to-analyse': (
        'À analyser', 'To analyse', 'Por analizar', 'للتحليل', '分析待ち', '待分析'),
    'stakeholders.status.applicable': (
        'Applicable', 'Applicable', 'Aplicable', 'قابل للتطبيق', '適用', '适用'),
    'stakeholders.status.controlled': (
        'Maîtrisée', 'Under control', 'Controlado', 'مُحكَم', '管理下', '已受控'),
    'stakeholders.status.to-reassess': (
        'À réévaluer', 'To reassess', 'Por reevaluar',
        'لإعادة التقييم', '再評価が必要', '待重新评估'),

    # --- fiche : sections et champs ----------------------------------------
    'stakeholders.section.identification': (
        'Identification', 'Identification', 'Identificación',
        'التعريف', '識別', '识别'),
    'stakeholders.section.analysis': (
        'Analyse', 'Analysis', 'Análisis', 'التحليل', '分析', '分析'),
    'stakeholders.section.steering': (
        'Pilotage', 'Steering', 'Gestión', 'القيادة', '運営管理', '管控'),
    'stakeholders.section.requirements': (
        'Exigences', 'Requirements', 'Requisitos', 'المتطلبات', '要求事項', '要求'),
    'stakeholders.field.name': (
        'Nom ou raison sociale', 'Name or legal name', 'Nombre o razón social',
        'الاسم أو الاسم التجاري', '名称または商号', '名称或法定名称'),
    'stakeholders.field.category': (
        'Catégorie', 'Category', 'Categoría', 'الفئة', 'カテゴリー', '类别'),
    'stakeholders.field.contact': (
        'Coordonnées', 'Contact details', 'Datos de contacto',
        'بيانات الاتصال', '連絡先', '联系方式'),
    'stakeholders.field.sites': (
        'Sites concernés', 'Sites concerned', 'Centros afectados',
        'المواقع المعنية', '対象サイト', '涉及场所'),
    'stakeholders.field.needs': (
        'Besoins et attentes', 'Needs and expectations', 'Necesidades y expectativas',
        'الاحتياجات والتوقعات', 'ニーズおよび期待', '需求与期望'),
    'stakeholders.field.requirements-summary': (
        'Exigences applicables', 'Applicable requirements', 'Requisitos aplicables',
        'المتطلبات المنطبقة', '適用される要求事項', '适用要求'),
    'stakeholders.field.relevance': (
        'Pertinence pour le système qualité', 'Relevance to the quality system',
        'Pertinencia para el sistema de calidad', 'الأهمية بالنسبة لنظام الجودة',
        '品質システムにとっての関連性', '对质量体系的相关性'),
    'stakeholders.field.owner': (
        'Responsable interne de la relation', 'Internal owner of the relationship',
        'Responsable interno de la relación', 'المسؤول الداخلي عن العلاقة',
        '関係の社内責任者', '关系的内部负责人'),
    'stakeholders.field.justification': (
        'Justification de la pertinence ou de la non-pertinence',
        'Justification of relevance or non-relevance',
        'Justificación de la pertinencia o de la no pertinencia',
        'تبرير الأهمية أو عدم الأهمية',
        '関連性あり／なしの根拠', '相关或不相关的理由'),
    'stakeholders.field.risks': (
        'Risques et opportunités associés', 'Associated risks and opportunities',
        'Riesgos y oportunidades asociados', 'المخاطر والفرص المرتبطة',
        '関連するリスクおよび機会', '相关风险与机遇'),
    'stakeholders.field.quality-impact': (
        'Impact qualité', 'Quality impact', 'Impacto en la calidad',
        'الأثر على الجودة', '品質への影響', '质量影响'),
    'stakeholders.field.regulatory-impact': (
        'Impact réglementaire', 'Regulatory impact', 'Impacto reglamentario',
        'الأثر التنظيمي', '法規への影響', '法规影响'),
    'stakeholders.field.controls': (
        'Mesures de maîtrise', 'Control measures', 'Medidas de control',
        'تدابير التحكّم', '管理手段', '控制措施'),
    'stakeholders.field.indicators': (
        'Indicateurs de suivi', 'Monitoring indicators', 'Indicadores de seguimiento',
        'مؤشرات المتابعة', 'モニタリング指標', '监测指标'),
    'stakeholders.field.documents': (
        'Documents de référence', 'Reference documents', 'Documentos de referencia',
        'الوثائق المرجعية', '参照文書', '参考文件'),
    'stakeholders.field.actions': (
        'Actions associées', 'Associated actions', 'Acciones asociadas',
        'الإجراءات المرتبطة', '関連する処置', '相关措施'),
    'stakeholders.field.next-review': (
        'Prochaine réévaluation', 'Next review', 'Próxima reevaluación',
        'إعادة التقييم القادمة', '次回の再評価', '下次重新评估'),

    # --- fiche : revue et enregistrement -----------------------------------
    'stakeholders.reviewed-on': (
        'Dernière revue le', 'Last reviewed on', 'Última revisión el',
        'آخر مراجعة في', '最終レビュー日', '最近复评于'),
    'stakeholders.never-reviewed': (
        'Jamais revue.', 'Never reviewed.', 'Nunca revisada.',
        'لم تُراجع قطّ.', 'レビュー履歴はありません。', '尚未复评。'),
    'stakeholders.mark-reviewed': (
        'Marquer comme revue', 'Mark as reviewed', 'Marcar como revisada',
        'وسمها كمُراجَعة', 'レビュー済みにする', '标记为已复评'),
    'stakeholders.reviewed': (
        "Revue enregistrée à la date du jour.", "Review recorded with today's date.",
        'Revisión registrada con la fecha de hoy.',
        'تم تسجيل المراجعة بتاريخ اليوم.',
        '本日の日付でレビューを記録しました。', '已按今日日期记录复评。'),
    'stakeholders.review-failed': (
        "La revue n'a pas pu être enregistrée.", 'The review could not be recorded.',
        'No se pudo registrar la revisión.', 'لم يتمكّن تسجيل المراجعة.',
        'レビューを記録できませんでした。', '无法记录复评。'),
    'stakeholders.pending': (
        'Fiche modifiée', 'Sheet modified', 'Ficha modificada',
        'تم تعديل السجل', 'シートが変更されています', '档案已修改'),
    'stakeholders.saved': (
        'Fiche enregistrée.', 'Sheet saved.', 'Ficha guardada.',
        'تم حفظ السجل.', 'シートを保存しました。', '档案已保存。'),
    'stakeholders.save-failed': (
        "La fiche n'a pas pu être enregistrée.", 'The sheet could not be saved.',
        'No se pudo guardar la ficha.', 'لم يتمكّن حفظ السجل.',
        'シートを保存できませんでした。', '无法保存档案。'),
    'stakeholders.problem.name': (
        'Une partie prenante se désigne par un nom.',
        'A stakeholder is designated by a name.',
        'Una parte interesada se designa con un nombre.',
        'يُعرَّف الطرف المعني باسمه.',
        '利害関係者は名称で特定します。', '相关方须以名称标识。'),
    'stakeholders.problem.justification': (
        "Dites pourquoi elle est pertinente, ou pourquoi elle ne l'est pas : un auditeur vient lire "
        'cette décision.',
        'State why it is relevant, or why it is not: an auditor will come and read that decision.',
        'Indique por qué es pertinente, o por qué no lo es: un auditor vendrá a leer esa decisión.',
        'بيّن سبب أهميته أو سبب عدم أهميته: سيأتي مُدقِّق ليقرأ هذا القرار.',
        '関連ありとした理由、あるいは関連なしとした理由を書いてください。監査員がこの判断を読みに来ます。',
        '说明其为何相关或为何不相关：审核员会来查阅这一判断。'),

    # --- fiche : exigences --------------------------------------------------
    'stakeholders.requirements-help': (
        "Ce que cette partie prenante attend, d'où cela vient, et comment vous vous en assurez. "
        'Chaque ligne se rattache à un risque, un écart, un audit ou un document.',
        'What this stakeholder expects, where it comes from, and how you make sure of it. '
        'Each line links to a risk, a deviation, an audit or a document.',
        'Lo que espera esta parte interesada, de dónde viene y cómo se asegura de ello. '
        'Cada línea se vincula a un riesgo, una desviación, una auditoría o un documento.',
        'ما يتوقعه هذا الطرف المعني، ومن أين يأتي، وكيف تتحقق منه. يرتبط كل سطر بخطر أو انحراف '
        'أو تدقيق أو وثيقة.',
        'この利害関係者が期待すること、その出どころ、そしてそれをどう確かめるのか。各行はリスク、'
        '不適合、監査、文書のいずれかに結び付きます。',
        '该相关方期望什么、来源何处，以及您如何加以确认。每一行都可关联到风险、偏差、审核或文件。'),
    'stakeholders.requirements-empty': (
        "Aucune exigence pour le moment. Ajoutez-en une, ou importez un contrat depuis "
        "l'assistant IA.",
        'No requirement yet. Add one, or import a contract from the AI assistant.',
        'Ningún requisito por ahora. Añada uno o importe un contrato desde el asistente de IA.',
        'لا يوجد أي متطلب حتى الآن. أضف متطلبًا، أو استورد عقدًا من المساعد الذكي.',
        '要求事項はまだありません。追加するか、AI アシスタントから契約書を取り込んでください。',
        '暂无要求。请添加一项，或从 AI 助手导入一份合同。'),
    'stakeholders.add-requirement': (
        'Ajouter une exigence', 'Add a requirement', 'Añadir un requisito',
        'إضافة متطلب', '要求事項を追加', '添加要求'),
    'stakeholders.requirement-added': (
        'Exigence ajoutée.', 'Requirement added.', 'Requisito añadido.',
        'تمت إضافة المتطلب.', '要求事項を追加しました。', '要求已添加。'),
    'stakeholders.requirement-failed': (
        "L'exigence n'a pas pu être ajoutée.", 'The requirement could not be added.',
        'No se pudo añadir el requisito.', 'لم تتمكّن إضافة المتطلب.',
        '要求事項を追加できませんでした。', '无法添加该要求。'),
    'stakeholders.status-failed': (
        "Le statut n'a pas pu être changé.", 'The status could not be changed.',
        'No se pudo cambiar el estado.', 'لم يتمكّن تغيير الحالة.',
        'ステータスを変更できませんでした。', '无法更改状态。'),
    'stakeholders.requirement-remove': (
        'Retirer', 'Remove', 'Retirar', 'إزالة', '削除', '移除'),
    'stakeholders.requirement-remove-title': (
        'Retirer cette exigence ?', 'Remove this requirement?', '¿Retirar este requisito?',
        'إزالة هذا المتطلب؟', 'この要求事項を削除しますか？', '要移除此要求吗？'),
    'stakeholders.requirement-remove-message': (
        'Elle disparaîtra de la fiche, ainsi que ses rattachements.',
        'It will disappear from the sheet, along with its links.',
        'Desaparecerá de la ficha, junto con sus vínculos.',
        'سيختفي من السجل، ومعه ارتباطاته.',
        'シートから消え、その関連付けも失われます。',
        '它及其关联将从档案中消失。'),
    'stakeholders.requirement-remove-failed': (
        "L'exigence n'a pas pu être retirée.", 'The requirement could not be removed.',
        'No se pudo retirar el requisito.', 'لم تتمكّن إزالة المتطلب.',
        '要求事項を削除できませんでした。', '无法移除该要求。'),
    'stakeholders.create-action': (
        'Créer une action', 'Create an action', 'Crear una acción',
        'إنشاء إجراء', '処置を作成', '创建措施'),
    'stakeholders.see-capa': (
        'Voir la CAPA', 'View the CAPA', 'Ver la CAPA',
        'عرض الإجراء التصحيحي', 'CAPA を表示', '查看 CAPA'),
    'stakeholders.capa-opened': (
        "CAPA ouverte ; l'exigence repasse à réévaluer.",
        'CAPA opened; the requirement goes back to “to reassess”.',
        'CAPA abierta; el requisito vuelve a «por reevaluar».',
        'تم فتح إجراء تصحيحي؛ ويعود المتطلب إلى «لإعادة التقييم».',
        'CAPA を開きました。要求事項は「再評価が必要」に戻ります。',
        '已开启 CAPA；该要求回到“待重新评估”。'),
    'stakeholders.capa-failed': (
        "La CAPA n'a pas pu être ouverte.", 'The CAPA could not be opened.',
        'No se pudo abrir la CAPA.', 'لم يتمكّن فتح الإجراء التصحيحي.',
        'CAPA を開けませんでした。', '无法开启 CAPA。'),
    'stakeholders.capa-no-account': (
        "Impossible d'identifier votre compte.", 'Your account could not be identified.',
        'No se pudo identificar su cuenta.', 'لم يتمكّن تحديد حسابك.',
        'お使いのアカウントを特定できません。', '无法识别您的账户。'),

    # --- rattachements ------------------------------------------------------
    'stakeholders.link.risk': (
        'Risque', 'Risk', 'Riesgo', 'خطر', 'リスク', '风险'),
    'stakeholders.link.nc': (
        'Écart', 'Deviation', 'Desviación', 'انحراف', '不適合', '偏差'),
    'stakeholders.link.audit': (
        'Audit', 'Audit', 'Auditoría', 'تدقيق', '監査', '审核'),
    'stakeholders.link.capa': (
        'Action', 'Action', 'Acción', 'إجراء', 'アクション', '措施'),
    'stakeholders.link.document': (
        'Document', 'Document', 'Documento', 'وثيقة', '文書', '文件'),
    'stakeholders.linked': (
        'Exigence rattachée.', 'Requirement linked.', 'Requisito vinculado.',
        'تم ربط المتطلب.', '要求事項を関連付けました。', '要求已关联。'),
    'stakeholders.link-failed': (
        'Le rattachement a échoué.', 'The link failed.', 'El vínculo falló.',
        'فشل الربط.', '関連付けに失敗しました。', '关联失败。'),
    'stakeholders.link.title-risk': (
        'Rattacher à un risque', 'Link to a risk', 'Vincular a un riesgo',
        'الربط بخطر', 'リスクに関連付ける', '关联到风险'),
    'stakeholders.link.title-nc': (
        'Rattacher à un écart', 'Link to a deviation', 'Vincular a una desviación',
        'الربط بانحراف', '不適合に関連付ける', '关联到偏差'),
    'stakeholders.link.title-audit': (
        'Rattacher à un audit', 'Link to an audit', 'Vincular a una auditoría',
        'الربط بتدقيق', '監査に関連付ける', '关联到审核'),
    'stakeholders.link.title-document': (
        'Rattacher à un document', 'Link to a document', 'Vincular a un documento',
        'الربط بوثيقة', '文書に関連付ける', '关联到文件'),
    'stakeholders.link.subtitle': (
        'Exigence : {$exigence}', 'Requirement: {$exigence}', 'Requisito: {$exigence}',
        'المتطلب: {$exigence}', '要求事項：{$exigence}', '要求：{$exigence}'),
    'stakeholders.link.filter': (
        'Filtrer la liste', 'Filter the list', 'Filtrar la lista',
        'تصفية القائمة', '一覧を絞り込む', '筛选列表'),
    'stakeholders.link.submit': (
        'Rattacher', 'Link', 'Vincular', 'ربط', '関連付ける', '关联'),
    'stakeholders.link.choose': (
        'Choisissez la cible dans la liste.', 'Choose the target from the list.',
        'Elija el destino en la lista.', 'اختر الهدف من القائمة.',
        '一覧から対象を選んでください。', '请从列表中选择目标。'),
    'stakeholders.link.loading': (
        'Chargement de la liste…', 'Loading the list…', 'Cargando la lista…',
        'جارٍ تحميل القائمة…', '一覧を読み込んでいます…', '正在加载列表…'),
    'stakeholders.link.load-failed': (
        "La liste n'a pas pu être chargée ; fermez la fenêtre et réessayez.",
        'The list could not be loaded; close the window and try again.',
        'No se pudo cargar la lista; cierre la ventana e inténtelo de nuevo.',
        'لم تتمكّن القائمة من التحميل؛ أغلق النافذة وأعد المحاولة.',
        '一覧を読み込めませんでした。ウィンドウを閉じて再度お試しください。',
        '无法加载列表；请关闭窗口后重试。'),
    'stakeholders.link.none': (
        "Il n'y a rien à rattacher : ce module ne contient encore aucun élément.",
        'There is nothing to link to: this module holds no item yet.',
        'No hay nada que vincular: este módulo aún no contiene ningún elemento.',
        'لا يوجد ما يمكن الربط به: لا تحتوي هذه الوحدة على أي عنصر بعد.',
        '関連付ける対象がありません。このモジュールにはまだ項目がありません。',
        '没有可关联的对象：该模块尚无任何条目。'),
    'stakeholders.link.no-match': (
        'Aucun élément ne correspond à ce filtre.', 'No item matches this filter.',
        'Ningún elemento coincide con este filtro.', 'لا يوجد عنصر مطابق لهذا الفلتر.',
        'この絞り込みに該当する項目はありません。', '没有符合此筛选条件的条目。'),

    # --- fenêtre d'inscription ---------------------------------------------
    'stakeholders.create.title': (
        'Inscrire une partie prenante', 'Register a stakeholder',
        'Registrar una parte interesada', 'تسجيل طرف معني',
        '利害関係者を登録する', '登记相关方'),
    'stakeholders.create.subtitle': (
        "Trois champs suffisent : sa fiche s'ouvre ensuite pour l'analyse.",
        'Three fields are enough: its sheet then opens for the analysis.',
        'Bastan tres campos: su ficha se abre después para el análisis.',
        'ثلاثة حقول تكفي: يُفتح سجله بعد ذلك للتحليل.',
        '入力は三つの項目だけです。その後、分析のためにシートが開きます。',
        '只需三个字段：随后会打开其档案以便分析。'),
    'stakeholders.create.submit': (
        'Inscrire', 'Register', 'Registrar', 'تسجيل', '登録', '登记'),
    'stakeholders.create.need-name': (
        'Donnez le nom ou la raison sociale de la partie prenante.',
        "Give the stakeholder's name or legal name.",
        'Indique el nombre o la razón social de la parte interesada.',
        'أدخل اسم الطرف المعني أو اسمه التجاري.',
        '利害関係者の名称または商号を入力してください。',
        '请填写相关方的名称或法定名称。'),
    'stakeholders.create.name-too-long': (
        'Le nom dépasse la longueur acceptée.', 'The name exceeds the accepted length.',
        'El nombre supera la longitud admitida.', 'يتجاوز الاسم الطول المسموح به.',
        '名称が許容される長さを超えています。', '名称超出允许长度。'),
    'stakeholders.create.done': (
        "Partie prenante inscrite ; complétez son analyse.",
        'Stakeholder registered; complete its analysis.',
        'Parte interesada registrada; complete su análisis.',
        'تم تسجيل الطرف المعني؛ أكمل تحليله.',
        '利害関係者を登録しました。分析を記入してください。',
        '相关方已登记；请完成其分析。'),
    'stakeholders.create.failed': (
        "La partie prenante n'a pas pu être inscrite.",
        'The stakeholder could not be registered.',
        'No se pudo registrar la parte interesada.',
        'لم يتمكّن تسجيل الطرف المعني.',
        '利害関係者を登録できませんでした。', '无法登记该相关方。'),

    # --- fenêtre d'exigence -------------------------------------------------
    'stakeholders.requirement.title': (
        'Ajouter une exigence', 'Add a requirement', 'Añadir un requisito',
        'إضافة متطلب', '要求事項を追加する', '添加要求'),
    'stakeholders.requirement.subtitle': (
        'Ce que {$nom} attend, et comment vous vous en assurez.',
        'What {$nom} expects, and how you make sure of it.',
        'Lo que {$nom} espera y cómo se asegura de ello.',
        'ما يتوقعه {$nom}، وكيف تتحقق منه.',
        '{$nom} が期待すること、そしてそれをどう確かめるのか。',
        '{$nom} 期望什么，以及您如何加以确认。'),
    'stakeholders.requirement.text': (
        'Exigence', 'Requirement', 'Requisito', 'المتطلب', '要求事項', '要求'),
    'stakeholders.requirement.optional': (
        "Les champs qui suivent se complètent au fil de l'analyse : rien n'empêche d'ajouter "
        "l'exigence maintenant et de dire plus tard qui la suit.",
        'The fields below are filled in as the analysis goes: nothing prevents you from adding the '
        'requirement now and saying later who follows it.',
        'Los campos siguientes se completan a lo largo del análisis: nada impide añadir el '
        'requisito ahora e indicar más tarde quién lo sigue.',
        'تُستكمل الحقول التالية مع تقدّم التحليل: لا شيء يمنع إضافة المتطلب الآن وتحديد مَن يتابعه لاحقًا.',
        '以下の項目は分析の進み方に合わせて埋めていきます。いま要求事項を追加し、担当は後から'
        '決めても構いません。',
        '以下字段可随分析推进逐步填写：现在添加要求、稍后再指定跟踪人完全可以。'),
    'stakeholders.requirement.need': (
        "Besoin ou attente d'origine", 'Originating need or expectation',
        'Necesidad o expectativa de origen', 'الحاجة أو التوقع الأصلي',
        '元となるニーズまたは期待', '原始需求或期望'),
    'stakeholders.requirement.origin': (
        'Origine (contrat, texte, clause)', 'Source (contract, text, clause)',
        'Origen (contrato, texto, cláusula)', 'المصدر (عقد، نص، بند)',
        '出どころ（契約、法令、条項）', '来源（合同、条文、条款）'),
    'stakeholders.requirement.nature': (
        "Nature de l'obligation", 'Nature of the obligation',
        'Naturaleza de la obligación', 'نوع الالتزام',
        '義務の性質', '义务性质'),
    'stakeholders.requirement.no-nature': (
        'Non précisée', 'Not specified', 'Sin especificar',
        'غير محدَّد', '指定なし', '未指明'),
    'stakeholders.requirement.risk': (
        "Risque si elle n'est pas tenue", 'Risk if it is not met',
        'Riesgo si no se cumple', 'الخطر في حال عدم الوفاء به',
        '満たされない場合のリスク', '未满足时的风险'),
    'stakeholders.requirement.risk-level': (
        'Niveau de risque', 'Risk level', 'Nivel de riesgo',
        'مستوى الخطر', 'リスクレベル', '风险等级'),
    'stakeholders.requirement.no-level': (
        'Non coté', 'Not rated', 'Sin calificar', 'غير مُقيَّم', '未評価', '未评级'),
    'stakeholders.requirement.control': (
        'Mesure de maîtrise', 'Control measure', 'Medida de control',
        'تدبير التحكّم', '管理手段', '控制措施'),
    'stakeholders.requirement.indicator': (
        'Indicateur de suivi', 'Monitoring indicator', 'Indicador de seguimiento',
        'مؤشر المتابعة', 'モニタリング指標', '监测指标'),
    'stakeholders.requirement.responsible': (
        'Responsable du suivi', 'Person responsible for follow-up',
        'Responsable del seguimiento', 'المسؤول عن المتابعة',
        'フォローアップの責任者', '跟踪负责人'),
    'stakeholders.requirement.evidence': (
        'Preuve attendue', 'Expected evidence', 'Evidencia esperada',
        'الدليل المتوقع', '期待される証拠', '预期证据'),
    'stakeholders.requirement.need-text': (
        "Énoncez l'exigence : c'est la seule chose indispensable ici.",
        'State the requirement: it is the only thing indispensable here.',
        'Enuncie el requisito: es lo único indispensable aquí.',
        'اذكر المتطلب: هو الشيء الوحيد الضروري هنا.',
        '要求事項を書いてください。ここで欠かせないのはそれだけです。',
        '请写出要求：这里唯一不可或缺的就是它。'),
    'stakeholders.requirement.too-long': (
        "L'exigence dépasse la longueur acceptée ; résumez-la et détaillez dans la preuve.",
        'The requirement exceeds the accepted length; summarize it and give details in the evidence.',
        'El requisito supera la longitud admitida; resúmalo y detalle en la evidencia.',
        'يتجاوز المتطلب الطول المسموح به؛ لخّصه وفصّل في الدليل.',
        '要求事項が許容される長さを超えています。要約し、詳細は証拠欄に書いてください。',
        '要求超出允许长度；请精简，并把细节写入证据栏。'),

    # --- matrice d'évaluation ----------------------------------------------
    'stakeholders.matrix.help': (
        "Deux cotations par partie prenante : ce qu'elle pèse sur la qualité, ce qu'elle pèse au "
        'titre de la réglementation. Elles ne se compensent pas — la criticité retient la plus '
        "élevée des deux, et c'est le serveur qui la calcule.",
        'Two ratings per stakeholder: what it weighs on quality, and what it weighs in regulatory '
        'terms. They do not offset each other — criticality keeps the higher of the two, and the '
        'server computes it.',
        'Dos calificaciones por parte interesada: lo que pesa en la calidad y lo que pesa en '
        'términos reglamentarios. No se compensan: la criticidad retiene la mayor de las dos, y la '
        'calcula el servidor.',
        'تقييمان لكل طرف معني: وزنه على الجودة، ووزنه من الناحية التنظيمية. ولا يعوّض أحدهما الآخر '
        '— تأخذ الأهمية الأعلى منهما، والخادم هو الذي يحسبها.',
        '利害関係者ごとに二つの評価：品質への重みと、法規上の重み。両者は打ち消し合いません。'
        '重要度は高いほうを採り、その算出はサーバーが行います。',
        '每个相关方有两项评级：对质量的权重，以及法规层面的权重。两者不相互抵消——关键性取其'
        '较高者，并由服务器计算。'),
    'stakeholders.matrix.read-only': (
        'Vous consultez la cotation ; sa modification relève du pilotage qualité.',
        'You are viewing the rating; changing it is up to quality management.',
        'Está consultando la calificación; su modificación corresponde a la gestión de la calidad.',
        'أنت تطالع التقييم؛ وتعديله من مسؤولية إدارة الجودة.',
        '評価は閲覧のみです。変更は品質管理の担当です。',
        '您正在查看评级；修改权属于质量管理。'),
    'stakeholders.matrix.empty': (
        'Aucune partie prenante à coter pour le moment.',
        'No stakeholder to rate for the moment.',
        'Ninguna parte interesada por calificar por ahora.',
        'لا يوجد طرف معني للتقييم حاليًا.',
        '現時点で評価する利害関係者はありません。', '目前没有需要评级的相关方。'),
    'stakeholders.matrix.grid-title': (
        'Répartition des parties prenantes', 'Distribution of stakeholders',
        'Distribución de las partes interesadas', 'توزيع الأطراف المعنية',
        '利害関係者の分布', '相关方分布'),
    'stakeholders.matrix.grid-caption': (
        'Lignes : impact réglementaire. Colonnes : impact qualité.',
        'Rows: regulatory impact. Columns: quality impact.',
        'Filas: impacto reglamentario. Columnas: impacto en la calidad.',
        'الصفوف: الأثر التنظيمي. الأعمدة: الأثر على الجودة.',
        '行：法規への影響。列：品質への影響。', '行：法规影响。列：质量影响。'),
    'stakeholders.matrix.pending': (
        "Lignes recotées, en attente d'enregistrement : {$nombre}",
        'Re-rated lines awaiting saving: {$nombre}',
        'Líneas recalificadas pendientes de guardar: {$nombre}',
        'أسطر أُعيد تقييمها وتنتظر الحفظ: {$nombre}',
        '再評価して未保存の行：{$nombre}', '已重新评级、待保存的行：{$nombre}'),
    'stakeholders.matrix.recalculated': (
        "recalculée à l'enregistrement", 'recalculated on saving',
        'recalculada al guardar', 'يُعاد حسابها عند الحفظ',
        '保存時に再計算されます', '保存时重新计算'),
    'stakeholders.matrix.save': (
        'Enregistrer la cotation', 'Save the rating', 'Guardar la calificación',
        'حفظ التقييم', '評価を保存', '保存评级'),
    'stakeholders.matrix.saved': (
        'Cotation enregistrée ; la criticité a été recalculée.',
        'Rating saved; criticality has been recalculated.',
        'Calificación guardada; la criticidad se ha recalculado.',
        'تم حفظ التقييم؛ وأُعيد حساب الأهمية.',
        '評価を保存しました。重要度を再計算しました。', '评级已保存；关键性已重新计算。'),
    'stakeholders.matrix.save-failed': (
        "La cotation n'a pas pu être enregistrée.", 'The rating could not be saved.',
        'No se pudo guardar la calificación.', 'لم يتمكّن حفظ التقييم.',
        '評価を保存できませんでした。', '无法保存评级。'),

    # --- assistant : le contrat --------------------------------------------
    'stakeholders.assistant.contract': (
        "L'assistant propose ; il n'inscrit rien. Chaque proposition reste une proposition jusqu'à "
        "ce que vous l'acceptiez, une par une.",
        'The assistant proposes; it records nothing. Each proposal stays a proposal until you '
        'accept it, one by one.',
        'El asistente propone; no registra nada. Cada propuesta sigue siendo una propuesta hasta '
        'que la acepte, una por una.',
        'المساعد يقترح؛ ولا يسجّل شيئًا. يبقى كل اقتراح اقتراحًا حتى تقبله، واحدًا واحدًا.',
        'アシスタントは提案するだけで、何も登録しません。各提案は、あなたが一件ずつ受け入れる'
        'まで提案のままです。',
        '助手只提出建议，不写入任何内容。每条建议在您逐条接受之前，始终只是建议。'),

    # --- assistant : suggestions -------------------------------------------
    'stakeholders.assistant.suggest-title': (
        "Qui oublie-t-on ?", 'Who are we forgetting?', '¿A quién olvidamos?',
        'مَن ننساه؟', '誰を見落としているか', '我们遗漏了谁？'),
    'stakeholders.assistant.suggest-help': (
        "Décrivez votre activité : l'assistant propose les familles de parties intéressées qu'un "
        'organisme comme le vôtre doit prendre en compte.',
        'Describe your activity: the assistant proposes the families of interested parties that an '
        'organization like yours must take into account.',
        'Describa su actividad: el asistente propone las familias de partes interesadas que una '
        'organización como la suya debe tener en cuenta.',
        'اوصف نشاطك: يقترح المساعد عائلات الأطراف المعنية التي يجب على منظمة مثل منظمتك أن تأخذها '
        'في الحسبان.',
        '事業内容を説明してください。あなたのような組織が考慮すべき利害関係者の類型を'
        'アシスタントが提案します。',
        '描述您的业务：助手会提出像您这样的组织应当考虑的相关方类别。'),
    'stakeholders.sector.aerospace': (
        'Aéronautique', 'Aerospace', 'Aeronáutica',
        'الطيران', '航空宇宙', '航空航天'),
    'stakeholders.sector.automotive': (
        'Automobile', 'Automotive', 'Automoción',
        'السيارات', '自動車', '汽车'),
    'stakeholders.sector.food': (
        'Agroalimentaire', 'Food industry', 'Agroalimentario',
        'الصناعات الغذائية', '食品', '食品'),
    'stakeholders.sector.medical-devices': (
        'Dispositifs médicaux', 'Medical devices', 'Productos sanitarios',
        'الأجهزة الطبية', '医療機器', '医疗器械'),
    'stakeholders.sector.services': (
        'Services', 'Services', 'Servicios',
        'الخدمات', 'サービス', '服务'),
    'stakeholders.assistant.sector': (
        "Secteur d'activité", 'Sector of activity', 'Sector de actividad',
        'قطاع النشاط', '業種', '行业'),
    'stakeholders.assistant.characteristics': (
        'Caractéristiques (sites, clients, contraintes)',
        'Characteristics (sites, customers, constraints)',
        'Características (centros, clientes, restricciones)',
        'الخصائص (المواقع، العملاء، القيود)',
        '特徴（拠点、顧客、制約）', '特征（场所、客户、约束）'),
    'stakeholders.assistant.need-sector': (
        "Dites dans quel secteur vous êtes : sans cela, il n'y a rien à proposer qui vous concerne.",
        'Say what sector you are in: without that, there is nothing relevant to propose.',
        'Indique en qué sector está: sin eso, no hay nada que proponerle que le concierna.',
        'حدِّد القطاع الذي تعمل فيه: بدون ذلك لا يوجد ما يمكن اقتراحه ويخصّك.',
        'どの業種かを書いてください。それがないと、あなたに関係する提案はできません。',
        '请说明您所处的行业：否则无法提出与您相关的建议。'),
    'stakeholders.assistant.suggest': (
        'Proposer des parties prenantes', 'Propose stakeholders',
        'Proponer partes interesadas', 'اقتراح أطراف معنية',
        '利害関係者を提案させる', '提出相关方建议'),
    'stakeholders.assistant.suggest-failed': (
        "Les suggestions n'ont pas pu être obtenues.", 'The suggestions could not be obtained.',
        'No se pudieron obtener las sugerencias.', 'لم تتمكّن الاقتراحات من الوصول.',
        '提案を取得できませんでした。', '无法获取建议。'),
    'stakeholders.assistant.nothing-to-suggest': (
        "L'assistant n'a rien proposé : soit votre registre couvre déjà ce que cette "
        "description laisse voir, soit il n'a pas pu répondre. Réessayez, ou précisez "
        'votre activité.',
        'The assistant proposed nothing: either your register already covers what this '
        'description shows, or it could not answer. Try again, or describe your activity '
        'more precisely.',
        'El asistente no ha propuesto nada: o su registro ya cubre lo que esta descripción '
        'permite ver, o no ha podido responder. Vuelva a intentarlo o precise su actividad.',
        'لم يقترح المساعد شيئًا: إمّا أن سجلك يغطي بالفعل ما يُظهره هذا الوصف، أو أنه لم يتمكن '
        'من الإجابة. أعد المحاولة أو حدّد نشاطك بدقة أكبر.',
        'アシスタントからの提案はありません。この説明から見える範囲をすでに登録簿が網羅しているか、'
        'アシスタントが応答できなかったかのいずれかです。再試行するか、活動内容をより具体的に記述してください。',
        '助手没有给出建议：可能是您的登记册已覆盖此描述所反映的范围，也可能是助手无法作答。'
        '请重试，或更具体地描述您的活动。'),
    'stakeholders.assistant.edit': (
        'Corriger', 'Edit', 'Corregir', 'تصحيح', '修正', '修改'),
    'stakeholders.assistant.reject': (
        'Rejeter', 'Reject', 'Rechazar', 'رفض', '却下', '拒绝'),
    'stakeholders.assistant.already': (
        'Déjà au registre', 'Already in the register', 'Ya está en el registro',
        'موجود بالسجل سلفًا', 'すでに登録済み', '已在登记册中'),
    'stakeholders.assistant.do-register': (
        'Inscrire', 'Register', 'Registrar', 'تسجيل', '登録', '登记'),
    'stakeholders.assistant.registered': (
        'Partie prenante inscrite.', 'Stakeholder registered.',
        'Parte interesada registrada.', 'تم تسجيل الطرف المعني.',
        '利害関係者を登録しました。', '相关方已登记。'),

    # --- assistant : extraction d'exigences --------------------------------
    'stakeholders.assistant.extract-title': (
        "Les exigences d'un document", 'The requirements of a document',
        'Los requisitos de un documento', 'متطلبات وثيقة',
        '文書に含まれる要求事項', '文件中的要求'),
    'stakeholders.assistant.extract-help': (
        "Collez un contrat, un cahier des charges ou un texte réglementaire : l'assistant en relève "
        'les exigences, chacune avec le passage qui la prouve.',
        'Paste a contract, a specification or a regulatory text: the assistant picks out the '
        'requirements, each with the passage that proves it.',
        'Pegue un contrato, un pliego de condiciones o un texto reglamentario: el asistente extrae '
        'los requisitos, cada uno con el pasaje que lo prueba.',
        'الصق عقدًا أو مواصفات أو نصًا تنظيميًا: يستخرج المساعد المتطلبات، كل واحد مع المقطع '
        'الذي يُثبته.',
        '契約書、仕様書、法令の文面を貼り付けてください。アシスタントが要求事項を抜き出し、'
        'それぞれに根拠となる引用を添えます。',
        '粘贴合同、规格书或法规条文：助手会摘出各项要求，并为每项附上作为依据的原文。'),
    'stakeholders.assistant.document-name': (
        'Nom du document', 'Document name', 'Nombre del documento',
        'اسم الوثيقة', '文書の名称', '文件名称'),
    'stakeholders.assistant.document-text': (
        'Texte du document', 'Document text', 'Texto del documento',
        'نص الوثيقة', '文書の本文', '文件正文'),
    'stakeholders.assistant.need-document': (
        "Collez au moins {$minimum} caractères du document : en dessous, il n'y a rien à lire.",
        'Paste at least {$minimum} characters of the document: below that, there is nothing to read.',
        'Pegue al menos {$minimum} caracteres del documento: por debajo, no hay nada que leer.',
        'الصق {$minimum} حرفًا على الأقل من الوثيقة: أقل من ذلك لا يوجد ما يُقرأ.',
        '文書を {$minimum} 文字以上貼り付けてください。それ未満では読み取るものがありません。',
        '请至少粘贴 {$minimum} 个字符的文件内容：少于此数便无内容可读。'),
    'stakeholders.assistant.extract': (
        'Relever les exigences', 'Pick out the requirements', 'Extraer los requisitos',
        'استخراج المتطلبات', '要求事項を抜き出す', '摘出要求'),
    'stakeholders.assistant.extract-failed': (
        "Les exigences n'ont pas pu être extraites.", 'The requirements could not be extracted.',
        'No se pudieron extraer los requisitos.', 'لم تتمكّن المتطلبات من الاستخراج.',
        '要求事項を抽出できませんでした。', '无法提取要求。'),
    'stakeholders.assistant.nothing-extracted': (
        "Rien à proposer : ce texte ne porte aucune exigence identifiable.",
        'Nothing to propose: this text carries no identifiable requirement.',
        'Nada que proponer: este texto no contiene ningún requisito identificable.',
        'لا يوجد ما يُقترح: لا يحمل هذا النص أي متطلب يمكن تحديده.',
        '提案はありません。この文面に特定できる要求事項は含まれていません。',
        '无建议：该文本未包含可识别的要求。'),
    'stakeholders.assistant.target': (
        'Partie prenante qui porte ces exigences', 'Stakeholder that carries these requirements',
        'Parte interesada que sostiene estos requisitos',
        'الطرف المعني الذي يحمل هذه المتطلبات',
        'これらの要求事項を負う利害関係者', '承载这些要求的相关方'),
    'stakeholders.assistant.need-target': (
        'Choisissez la partie prenante qui porte ces exigences.',
        'Choose the stakeholder that carries these requirements.',
        'Elija la parte interesada que sostiene estos requisitos.',
        'اختر الطرف المعني الذي يحمل هذه المتطلبات.',
        'これらの要求事項を負う利害関係者を選んでください。',
        '请选择承载这些要求的相关方。'),
    'stakeholders.assistant.open-sheet': (
        'Ouvrir sa fiche', 'Open its sheet', 'Abrir su ficha',
        'فتح سجله', 'そのシートを開く', '打开其档案'),

    # --- assistant : changements -------------------------------------------
    'stakeholders.assistant.changes-title': (
        'Ce qui a changé', 'What has changed', 'Lo que ha cambiado',
        'ما الذي تغيّر', '何が変わったか', '有哪些变化'),
    'stakeholders.assistant.changes-help': (
        "Deux versions d'un même texte : l'assistant dit ce qui a bougé et ce que cela pèse. À vous "
        'de désigner les exigences que ce changement remet en cause.',
        'Two versions of the same text: the assistant says what moved and what it weighs. It is up '
        'to you to point out the requirements this change calls into question.',
        'Dos versiones de un mismo texto: el asistente indica qué se movió y cuánto pesa. A usted '
        'le corresponde señalar los requisitos que este cambio cuestiona.',
        'نسختان من النص نفسه: يبيّن المساعد ما الذي تغيّر وما وزنه. ويبقى عليك تحديد المتطلبات '
        'التي يضعها هذا التغيير على المحك.',
        '同じ文面の二つの版：何が動き、それがどれほど重いかをアシスタントが示します。この変更'
        'によって見直しが必要な要求事項を指定するのは、あなたです。',
        '同一文本的两个版本：助手说明改动之处及其分量。至于此变化会牵动哪些要求，由您指定。'),
    'stakeholders.assistant.previous-text': (
        'Version précédente', 'Previous version', 'Versión anterior',
        'النسخة السابقة', '前の版', '上一版本'),
    'stakeholders.assistant.current-text': (
        'Version en vigueur', 'Version in force', 'Versión vigente',
        'النسخة السارية', '現行の版', '现行版本'),
    'stakeholders.assistant.need-two-versions': (
        "Collez les deux versions, au moins {$minimum} caractères chacune : il n'y a pas de "
        'comparaison possible autrement.',
        'Paste both versions, at least {$minimum} characters each: no comparison is possible '
        'otherwise.',
        'Pegue las dos versiones, al menos {$minimum} caracteres cada una: de lo contrario no hay '
        'comparación posible.',
        'الصق النسختين، بما لا يقل عن {$minimum} حرفًا لكل منهما: وإلا فلا مقارنة ممكنة.',
        '二つの版を、それぞれ {$minimum} 文字以上貼り付けてください。そうでなければ比較は'
        'できません。',
        '请粘贴两个版本，每个至少 {$minimum} 个字符：否则无法比较。'),
    'stakeholders.assistant.detect': (
        'Comparer les deux versions', 'Compare the two versions',
        'Comparar las dos versiones', 'مقارنة النسختين',
        '二つの版を比較する', '比较两个版本'),
    'stakeholders.assistant.detect-failed': (
        "Les changements n'ont pas pu être analysés.", 'The changes could not be analysed.',
        'No se pudieron analizar los cambios.', 'لم تتمكّن التغييرات من التحليل.',
        '変更点を分析できませんでした。', '无法分析变化。'),
    'stakeholders.assistant.nothing-changed': (
        'Rien à signaler : ces deux versions ne diffèrent sur aucune exigence.',
        'Nothing to report: these two versions do not differ on any requirement.',
        'Nada que señalar: estas dos versiones no difieren en ningún requisito.',
        'لا شيء يُذكر: لا تختلف هاتان النسختان في أي متطلب.',
        '報告すべき点はありません。この二つの版は、どの要求事項でも違いがありません。',
        '无可报告：这两个版本在任何要求上都没有差异。'),
    'stakeholders.assistant.reassess-target': (
        'Partie prenante concernée', 'Stakeholder concerned', 'Parte interesada afectada',
        'الطرف المعني المتأثر', '対象となる利害関係者', '涉及的相关方'),
    'stakeholders.assistant.no-requirement': (
        'Cette partie prenante ne porte encore aucune exigence à réévaluer.',
        'This stakeholder carries no requirement to reassess yet.',
        'Esta parte interesada aún no sostiene ningún requisito por reevaluar.',
        'لا يحمل هذا الطرف المعني بعد أي متطلب لإعادة التقييم.',
        'この利害関係者には、再評価できる要求事項がまだありません。',
        '该相关方尚无可重新评估的要求。'),
    'stakeholders.assistant.need-selection': (
        'Cochez les exigences que ce changement remet en cause.',
        'Tick the requirements this change calls into question.',
        'Marque los requisitos que este cambio cuestiona.',
        'حدِّد المتطلبات التي يضعها هذا التغيير على المحك.',
        'この変更によって見直しが必要な要求事項にチェックを入れてください。',
        '请勾选受此变化影响的要求。'),
    'stakeholders.assistant.mark': (
        'Marquer comme à réévaluer', 'Mark as to reassess', 'Marcar como por reevaluar',
        'الوسم كـ«لإعادة التقييم»', '「再評価が必要」にする', '标记为待重新评估'),
    'stakeholders.assistant.marked': (
        'Exigences marquées à réévaluer.', 'Requirements marked as to reassess.',
        'Requisitos marcados como por reevaluar.',
        'تم وسم المتطلبات لإعادة التقييم.',
        '要求事項を「再評価が必要」にしました。', '要求已标记为待重新评估。'),
    'stakeholders.assistant.mark-failed': (
        "Les exigences n'ont pas pu être marquées.", 'The requirements could not be marked.',
        'No se pudieron marcar los requisitos.', 'لم تتمكّن المتطلبات من الوسم.',
        '要求事項を記録できませんでした。', '无法标记这些要求。'),
}
