# -*- coding: utf-8 -*-
"""Table i18n - une CAPA = une action, créée par UNE fenêtre partout (ADR 0088).

La fenêtre de création commune (liste CAPA, NC, constat d'audit, risque, case
de la matrice SMI), son mode « définir l'action », la fiche réduite à l'action
unique, le pilote nommé, et les points d'entrée des écrans d'origine.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- la fenêtre commune ----------------------------------------------------
    'capa.open.title': (
        'Nouvelle CAPA', 'New CAPA', 'Nueva CAPA',
        'إجراء تصحيحي/وقائي جديد', '新規CAPA', '新建 CAPA'),
    'capa.open.define-title': (
        "Définir l'action", 'Define the action', 'Definir la acción',
        'تحديد الإجراء', '対策を定義', '定义措施'),
    'capa.open.submit': (
        'Créer la CAPA', 'Create the CAPA', 'Crear la CAPA',
        'إنشاء الإجراء', 'CAPAを作成', '创建 CAPA'),
    'capa.open.define-submit': (
        "Enregistrer l'action", 'Save the action', 'Guardar la acción',
        'حفظ الإجراء', '対策を保存', '保存措施'),
    'capa.open.subtitle': (
        "Une CAPA porte une action : ce qu'on va faire, qui en répond et pour quand. On la fait avancer jusqu'à la vérification d'efficacité.",
        'A CAPA carries one action: what will be done, who is accountable and by when. It is followed through to the effectiveness check.',
        'Una CAPA lleva una acción: qué se hará, quién responde y para cuándo. Se avanza hasta la verificación de eficacia.',
        'يتضمن كل إجراء تصحيحي/وقائي إجراءً واحدًا: ما الذي سيُنفَّذ، ومن المسؤول، وبحلول متى. ويُتابَع حتى التحقق من الفعالية.',
        'CAPAは1つの対策を持ちます。何を行い、誰が責任を持ち、いつまでに行うか。有効性確認まで進めます。',
        '一个 CAPA 对应一项措施：做什么、由谁负责、何时完成。持续推进直至有效性验证。'),
    'capa.open.title-label': (
        "Intitulé de l'action", 'Action title', 'Título de la acción',
        'عنوان الإجراء', '対策の件名', '措施名称'),
    'capa.open.description-placeholder': (
        'Méthode, ressources, critère de fin…', 'Method, resources, completion criterion…',
        'Método, recursos, criterio de finalización…', 'الطريقة، الموارد، معيار الإنجاز…',
        '方法、リソース、完了基準…', '方法、资源、完成标准…'),
    'capa.open.nature': (
        'Nature', 'Nature', 'Naturaleza', 'الطبيعة', '種別', '性质'),
    'capa.open.owner': (
        'Responsable', 'Owner', 'Responsable', 'المسؤول', '責任者', '负责人'),
    'capa.open.owner-loading': (
        "Lecture de l'annuaire…", 'Reading the directory…', 'Leyendo el directorio…',
        'جارٍ قراءة الدليل…', 'ディレクトリを読み込み中…', '正在读取通讯录…'),
    'capa.open.due-date': (
        'Échéance', 'Due date', 'Fecha límite', 'تاريخ الاستحقاق', '期限', '截止日期'),
    'capa.open.due-date-required': (
        "L'échéance est obligatoire.", 'The due date is required.', 'La fecha límite es obligatoria.',
        'تاريخ الاستحقاق إلزامي.', '期限は必須です。', '截止日期为必填项。'),
    'capa.open.due-date-past': (
        "L'échéance ne peut pas être passée.", 'The due date cannot be in the past.',
        'La fecha límite no puede estar en el pasado.', 'لا يمكن أن يكون تاريخ الاستحقاق في الماضي.',
        '期限に過去の日付は指定できません。', '截止日期不能早于今天。'),
    'capa.open.owner-hint-unmatched': (
        'Propriétaire indiqué : {$INTERPOLATION} — introuvable dans l\'annuaire, choisis son compte dans la liste.',
        'Stated owner: {$INTERPOLATION} — not found in the directory, choose their account from the list.',
        'Propietario indicado: {$INTERPOLATION} — no encontrado en el directorio, elige su cuenta en la lista.',
        'المالك المذكور: {$INTERPOLATION} — غير موجود في الدليل، اختر حسابه من القائمة.',
        '記載された所有者：{$INTERPOLATION} — ディレクトリに見つかりません。一覧からアカウントを選択してください。',
        '指定的负责人：{$INTERPOLATION} — 通讯录中未找到，请从列表中选择其账户。'),
    'capa.open.criticity-low': (
        'Faible', 'Low', 'Baja', 'منخفضة', '低', '低'),
    'capa.open.criticity-medium': (
        'Moyenne', 'Medium', 'Media', 'متوسطة', '中', '中'),
    'capa.open.criticity-high': (
        'Élevée', 'High', 'Alta', 'مرتفعة', '高', '高'),
    'capa.open.criticity-critical': (
        'Critique', 'Critical', 'Crítica', 'حرجة', '重大', '严重'),
    'capa.open.criticity-locked': (
        'Issue de la cotation du risque.', 'Derived from the risk rating.', 'Derivada de la valoración del riesgo.',
        'مستمدة من تقييم الخطر.', 'リスク評価に基づきます。', '取自风险评级。'),
    'capa.open.attachment-remove-aria': (
        'Retirer {$name}', 'Remove {$name}', 'Quitar {$name}',
        'إزالة {$name}', '{$name} を削除', '移除 {$name}'),
    'capa.open.success': (
        'CAPA créée.', 'CAPA created.', 'CAPA creada.',
        'تم إنشاء الإجراء.', 'CAPAを作成しました。', '已创建 CAPA。'),
    'capa.open.success-with-evidence': (
        'CAPA créée, pièces jointes déposées.', 'CAPA created, attachments uploaded.',
        'CAPA creada, adjuntos cargados.', 'تم إنشاء الإجراء ورفع المرفقات.',
        'CAPAを作成し、添付ファイルをアップロードしました。', '已创建 CAPA，附件已上传。'),
    'capa.open.success-partial-evidence': (
        "CAPA créée, mais des pièces n'ont pas pu être déposées — reprends-les depuis la fiche.",
        'CAPA created, but some attachments could not be uploaded — add them again from the record.',
        'CAPA creada, pero algunos adjuntos no se pudieron cargar: vuelve a añadirlos desde la ficha.',
        'تم إنشاء الإجراء، لكن تعذّر رفع بعض المرفقات — أعد إضافتها من البطاقة.',
        'CAPAを作成しましたが、一部の添付ファイルをアップロードできませんでした。詳細画面から再度追加してください。',
        '已创建 CAPA，但部分附件未能上传——请在详情页重新添加。'),
    'capa.open.action-defined': (
        'Action définie.', 'Action defined.', 'Acción definida.',
        'تم تحديد الإجراء.', '対策を定義しました。', '已定义措施。'),

    # --- les sources ------------------------------------------------------------
    'capa.source.spc-alert': (
        'Carte de contrôle', 'Control chart', 'Gráfico de control',
        'مخطط التحكم', '管理図', '控制图'),
    'capa.source.anomaly': (
        'Anomalie détectée', 'Detected anomaly', 'Anomalía detectada',
        'شذوذ مكتشف', '検知された異常', '检测到的异常'),
    'capa.source.risk': (
        'Risque', 'Risk', 'Riesgo', 'خطر', 'リスク', '风险'),

    # --- la fiche : l'action unique et le pilote -------------------------------
    'capa.detail.legacy-actions': (
        'Actions ({$INTERPOLATION})', 'Actions ({$INTERPOLATION})', 'Acciones ({$INTERPOLATION})',
        'الإجراءات ({$INTERPOLATION})', '対策（{$INTERPOLATION}）', '措施（{$INTERPOLATION}）'),
    'capa.detail.action-missing': (
        "L'action de ce dossier n'a pas encore été définie : sans elle, il ne pourra pas être clôturé.",
        'The action of this case has not been defined yet: without it, the case cannot be closed.',
        'La acción de este expediente aún no se ha definido: sin ella, no podrá cerrarse.',
        'لم يُحدَّد إجراء هذا الملف بعد: من دونه لا يمكن إغلاقه.',
        'この案件の対策はまだ定義されていません。対策がなければクローズできません。',
        '该案例的措施尚未定义：没有措施则无法关闭。'),
    'capa.blocker.containment-only-single': (
        "Le dossier ne porte qu'une mesure d'endiguement : elle arrête l'effet sans supprimer la cause. Ouvrez une CAPA corrective pour traiter la cause.",
        'The case only carries a containment measure: it stops the effect without removing the cause. Open a corrective CAPA to address the cause.',
        'El expediente solo lleva una medida de contención: detiene el efecto sin eliminar la causa. Abra una CAPA correctiva para tratar la causa.',
        'لا يتضمن الملف سوى إجراء احتواء: يوقف الأثر دون إزالة السبب. افتح إجراءً تصحيحيًا لمعالجة السبب.',
        'この案件には暫定対策しかありません。影響は止めますが原因は除去しません。原因に対処するには是正CAPAを作成してください。',
        '该案例仅包含围堵措施：它阻止了影响，但未消除原因。请新建纠正 CAPA 以处理原因。'),
    'capa.list.col-owner': (
        'Pilote', 'Owner', 'Responsable', 'المسؤول', '責任者', '负责人'),

    # --- les points d'entrée des écrans d'origine ------------------------------
    'nc.detail.create-capa': (
        'Créer une CAPA', 'Create a CAPA', 'Crear una CAPA',
        'إنشاء إجراء تصحيحي/وقائي', 'CAPAを作成', '创建 CAPA'),
    'nc.detail.create-capa-tooltip': (
        'Ouvrir la CAPA qui traite cette non-conformité', 'Open the CAPA that addresses this non-conformity',
        'Abrir la CAPA que trata esta no conformidad', 'فتح الإجراء الذي يعالج عدم المطابقة هذا',
        'この不適合に対処するCAPAを作成', '创建处理此不符合项的 CAPA'),
    'nc.detail.open-capa': (
        'Voir la CAPA', 'View the CAPA', 'Ver la CAPA',
        'عرض الإجراء', 'CAPAを表示', '查看 CAPA'),
    'nc.detail.capa-link-open': (
        'Ouvrir le dossier', 'Open the case', 'Abrir el expediente',
        'فتح الملف', '案件を開く', '打开案例'),
    'audits.detail.create-capa': (
        'Créer une CAPA', 'Create a CAPA', 'Crear una CAPA',
        'إنشاء إجراء تصحيحي/وقائي', 'CAPAを作成', '创建 CAPA'),
    'audits.detail.open-capa': (
        'Voir la CAPA', 'View the CAPA', 'Ver la CAPA',
        'عرض الإجراء', 'CAPAを表示', '查看 CAPA'),
    'audits.detail.finding-capa-ref': (
        '{$audit} / constat {$rank}', '{$audit} / finding {$rank}', '{$audit} / hallazgo {$rank}',
        '{$audit} / الملاحظة {$rank}', '{$audit} / 所見 {$rank}', '{$audit} / 发现项 {$rank}'),
    'capa.list.filter-criticity': ('Filtrer par criticité', 'Filter by criticality', 'Filtrar por criticidad', 'تصفية حسب الأهمية', '重要度で絞り込み', '按严重程度筛选'),
}
