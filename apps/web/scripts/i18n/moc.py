# -*- coding: utf-8 -*-
"""Libellés de la gestion des changements (MOC, ADR 0087) : l'écran unique
(liste, frise, demande, analyse d'impact, validation, historique), la saisie
d'une demande, les refus du serveur traduits, et les « Paramètres MOC » de
l'administrateur du client.

Placeholders : `${x}:nom:` côté TS s'écrit {$nom} ; `{{ x // i18n(ph="nom") }}`
côté gabarit s'écrit {$NOM}.
"""

TRANSLATIONS = {
    # ---------- navigation et droits ----------
    'nav.moc': ('MOC', 'MOC', 'MOC', 'MOC', 'MOC', 'MOC'),
    'nav.admin-moc': ('Paramètres MOC', 'MOC settings', 'Parámetros MOC', 'إعدادات MOC', 'MOC 設定', 'MOC 设置'),
    'authz.module.change': (
        'Gestion des changements', 'Management of change', 'Gestión del cambio', 'إدارة التغيير',
        '変更管理', '变更管理'),
    'authz.perm.change-request': (
        'Demander un changement et conduire ses demandes', 'Request a change and manage own requests',
        'Solicitar un cambio y gestionar sus solicitudes', 'طلب تغيير وإدارة طلباته',
        '変更を申請し自分の申請を進める', '提出变更申请并处理自己的申请'),
    'authz.perm.change-analyze': (
        'Gérer toutes les demandes de changement', 'Manage all change requests',
        'Gestionar todas las solicitudes de cambio', 'إدارة جميع طلبات التغيير',
        'すべての変更申請を管理する', '管理所有变更申请'),

    # ---------- écran ----------
    'moc.title': ('MOC', 'MOC', 'MOC', 'MOC', 'MOC', 'MOC'),
    'moc.subtitle': (
        "Demande, analyse d'impact, validation", 'Request, impact analysis, approval',
        'Solicitud, análisis de impacto, validación', 'الطلب، تحليل الأثر، الاعتماد',
        '申請、影響分析、承認', '申请、影响分析、审批'),
    'moc.settings-link': ('Paramètres MOC', 'MOC settings', 'Parámetros MOC', 'إعدادات MOC', 'MOC 設定', 'MOC 设置'),
    'moc.export': ('Extraire en Excel', 'Export to Excel', 'Exportar a Excel', 'تصدير إلى Excel', 'Excel に出力', '导出为 Excel'),
    'moc.export-failed': (
        "L'extraction Excel a échoué.", 'The Excel export failed.', 'La exportación a Excel ha fallado.',
        'فشل التصدير إلى Excel.', 'Excel への出力に失敗しました。', 'Excel 导出失败。'),
    'moc.new': ('Nouvelle demande', 'New request', 'Nueva solicitud', 'طلب جديد', '新規申請', '新建申请'),
    'moc.list-failed': (
        "Les demandes n'ont pas pu être chargées.", 'The requests could not be loaded.',
        'No se pudieron cargar las solicitudes.', 'تعذّر تحميل الطلبات.', '申請を読み込めませんでした。', '无法加载申请。'),
    'moc.list-title': (
        'Demandes de changement', 'Change requests', 'Solicitudes de cambio', 'طلبات التغيير', '変更申請', '变更申请'),
    'moc.load-failed': (
        "La demande n'a pas pu être chargée.", 'The request could not be loaded.', 'No se pudo cargar la solicitud.',
        'تعذّر تحميل الطلب.', '申請を読み込めませんでした。', '无法加载该申请。'),
    'moc.filter-aria': (
        'Filtrer par statut', 'Filter by status', 'Filtrar por estado', 'التصفية حسب الحالة',
        'ステータスで絞り込む', '按状态筛选'),
    'moc.filter.all': ('Tous', 'All', 'Todas', 'الكل', 'すべて', '全部'),
    'moc.filter.mine': (
        'À valider par moi', 'Awaiting my approval', 'Pendientes de mi validación', 'بانتظار اعتمادي',
        '自分の承認待ち', '待我审批'),
    'moc.awaiting-me': ('À valider', 'To approve', 'Por validar', 'للاعتماد', '承認待ち', '待审批'),
    'moc.empty-filter': (
        'Aucune demande dans ce statut.', 'No request with this status.', 'Ninguna solicitud con este estado.',
        'لا يوجد طلب بهذه الحالة.', 'このステータスの申請はありません。', '没有此状态的申请。'),
    'moc.empty': (
        "Aucune demande de changement pour l'instant.", 'No change request yet.',
        'Todavía no hay solicitudes de cambio.', 'لا توجد طلبات تغيير حتى الآن.',
        '変更申請はまだありません。', '暂无变更申请。'),
    'moc.no-selection': (
        "Sélectionnez une demande pour voir sa demande, son analyse d'impact, ses validations et son historique.",
        'Select a request to see its details, impact analysis, approvals and history.',
        'Seleccione una solicitud para ver su contenido, su análisis de impacto, sus validaciones y su historial.',
        'اختر طلبًا لعرض تفاصيله وتحليل أثره واعتماداته وسجله.',
        '申請を選択すると、内容、影響分析、承認、履歴を確認できます。',
        '选择一个申请以查看其内容、影响分析、审批和历史。'),

    # ---------- statuts, classes, niveaux, domaines, types ----------
    'moc.status.draft': ('Brouillon', 'Draft', 'Borrador', 'مسودة', '下書き', '草稿'),
    'moc.status.in-analysis': ('En analyse', 'In analysis', 'En análisis', 'قيد التحليل', '分析中', '分析中'),
    'moc.status.in-validation': ('En validation', 'In approval', 'En validación', 'قيد الاعتماد', '承認中', '审批中'),
    'moc.status.approved': ('Approuvé', 'Approved', 'Aprobado', 'معتمد', '承認済み', '已批准'),
    'moc.status.rejected': ('Rejeté', 'Rejected', 'Rechazado', 'مرفوض', '却下', '已拒绝'),
    'moc.class.not-assessed': ('Non évalué', 'Not assessed', 'No evaluado', 'غير مقيّم', '未評価', '未评估'),
    'moc.class.minor': ('Mineur', 'Minor', 'Menor', 'ثانوي', '軽微', '轻微'),
    'moc.class.major': ('Majeur', 'Major', 'Mayor', 'رئيسي', '重大', '重大'),
    'moc.class.critical': ('Critique', 'Critical', 'Crítico', 'حرج', '重要危機', '关键'),
    'moc.level.none': ('Aucun', 'None', 'Ninguno', 'لا يوجد', 'なし', '无'),
    'moc.level.low': ('Faible', 'Low', 'Bajo', 'منخفض', '低', '低'),
    'moc.level.medium': ('Moyen', 'Medium', 'Medio', 'متوسط', '中', '中'),
    'moc.level.high': ('Élevé', 'High', 'Alto', 'مرتفع', '高', '高'),
    'moc.domain.safety': ('Sécurité', 'Safety', 'Seguridad', 'السلامة', '安全', '安全'),
    'moc.domain.health-environment': (
        'Santé et environnement', 'Health and environment', 'Salud y medio ambiente', 'الصحة والبيئة',
        '健康・環境', '健康与环境'),
    'moc.domain.quality': ('Qualité', 'Quality', 'Calidad', 'الجودة', '品質', '质量'),
    'moc.domain.production': ('Production', 'Production', 'Producción', 'الإنتاج', '生産', '生产'),
    'moc.domain.maintenance': ('Maintenance', 'Maintenance', 'Mantenimiento', 'الصيانة', '保全', '维护'),
    'moc.domain.costs': ('Coûts', 'Costs', 'Costes', 'التكاليف', 'コスト', '成本'),
    'moc.domain.regulatory': ('Réglementaire', 'Regulatory', 'Normativo', 'التنظيمي', '法規制', '法规'),
    'moc.domain.training': ('Formation', 'Training', 'Formación', 'التدريب', '教育訓練', '培训'),
    'moc.type.permanent': ('Permanent', 'Permanent', 'Permanente', 'دائم', '恒久', '永久'),
    'moc.type.temporary': ('Temporaire', 'Temporary', 'Temporal', 'مؤقت', '一時的', '临时'),
    'moc.decision.pending': ('En attente', 'Pending', 'Pendiente', 'قيد الانتظار', '保留中', '待定'),
    'moc.decision.approved': ('Approuvé', 'Approved', 'Aprobado', 'معتمد', '承認済み', '已批准'),
    'moc.decision.rejected': ('Rejeté', 'Rejected', 'Rechazado', 'مرفوض', '却下', '已拒绝'),

    # ---------- panneau ----------
    'moc.send-to-analysis': ('Envoyer en analyse', 'Send to analysis', 'Enviar a análisis', 'إرسال للتحليل', '分析に回す', '提交分析'),
    'moc.submit': ('Soumettre à validation', 'Submit for approval', 'Enviar a validación', 'إرسال للاعتماد', '承認に提出', '提交审批'),
    'moc.reopen': ("Rouvrir l'analyse", 'Reopen the analysis', 'Reabrir el análisis', 'إعادة فتح التحليل', '分析を再開', '重新打开分析'),
    'moc.submit-blocked': (
        "Soumission bloquée : {$COUNT} domaine(s) d'impact sur 8 restent à évaluer.",
        'Submission blocked: {$COUNT} of 8 impact domain(s) remain to be assessed.',
        'Envío bloqueado: quedan {$COUNT} dominio(s) de impacto de 8 por evaluar.',
        'الإرسال محظور: لا يزال {$COUNT} من 8 مجالات أثر بحاجة إلى تقييم.',
        '提出不可：影響領域 8 件中 {$COUNT} 件が未評価です。',
        '无法提交：8 个影响领域中还有 {$COUNT} 个待评估。'),
    'moc.progress-aria': (
        'Avancement de la demande', 'Request progress', 'Avance de la solicitud', 'تقدّم الطلب', '申請の進捗', '申请进度'),
    'moc.tab.request': ('Demande', 'Request', 'Solicitud', 'الطلب', '申請', '申请'),
    'moc.tab.impact': ("Analyse d'impact", 'Impact analysis', 'Análisis de impacto', 'تحليل الأثر', '影響分析', '影响分析'),
    'moc.tab.validation': ('Validation', 'Approval', 'Validación', 'الاعتماد', '承認', '审批'),
    'moc.tab.history': ('Historique', 'History', 'Historial', 'السجل', '履歴', '历史'),
    'moc.field.type': ('Type', 'Type', 'Tipo', 'النوع', '種別', '类型'),
    'moc.field.site': ('Site / unité', 'Site / unit', 'Centro / unidad', 'الموقع / الوحدة', '拠点／部門', '场所/单位'),
    'moc.field.requester': ('Demandeur · date', 'Requester · date', 'Solicitante · fecha', 'مقدّم الطلب · التاريخ', '申請者・日付', '申请人 · 日期'),
    'moc.field.planned': ('Mise en œuvre prévue', 'Planned implementation', 'Implantación prevista', 'التنفيذ المخطط', '実施予定日', '计划实施'),
    'moc.field.description': ('Description', 'Description', 'Descripción', 'الوصف', '説明', '描述'),
    'moc.field.justification': ('Justification', 'Justification', 'Justificación', 'المبرر', '理由', '理由'),
    'moc.sent-to-analysis': (
        'Demande envoyée en analyse.', 'Request sent to analysis.', 'Solicitud enviada a análisis.',
        'تم إرسال الطلب للتحليل.', '申請を分析に回しました。', '申请已提交分析。'),
    'moc.submitted': (
        'Demande soumise à validation : les validateurs sont prévenus.',
        'Request submitted for approval: the approvers have been notified.',
        'Solicitud enviada a validación: se ha avisado a los validadores.',
        'تم إرسال الطلب للاعتماد: تم إبلاغ المعتمِدين.',
        '申請を承認に提出しました。承認者に通知されました。',
        '申请已提交审批：已通知审批人。'),
    'moc.reopened': ("L'analyse est rouverte.", 'The analysis has been reopened.', 'El análisis se ha reabierto.',
                     'أُعيد فتح التحليل.', '分析を再開しました。', '分析已重新打开。'),
    'moc.delete-title': (
        'Supprimer la demande {$code} ?', 'Delete request {$code}?', '¿Eliminar la solicitud {$code}?',
        'حذف الطلب {$code}؟', '申請 {$code} を削除しますか？', '删除申请 {$code}？'),
    'moc.delete-message': (
        'Le brouillon et son historique seront supprimés. Cette action est définitive.',
        'The draft and its history will be deleted. This cannot be undone.',
        'Se eliminarán el borrador y su historial. Esta acción es definitiva.',
        'ستُحذف المسودة وسجلها. هذا الإجراء نهائي.',
        '下書きとその履歴が削除されます。この操作は取り消せません。',
        '草稿及其历史将被删除。此操作不可撤销。'),
    'moc.deleted': ('Demande supprimée.', 'Request deleted.', 'Solicitud eliminada.', 'تم حذف الطلب.', '申請を削除しました。', '申请已删除。'),
    'moc.action-failed': (
        "L'action sur la demande a échoué.", 'The action on the request failed.',
        'La acción sobre la solicitud ha fallado.', 'فشل الإجراء على الطلب.',
        '申請への操作に失敗しました。', '对申请的操作失败。'),

    # ---------- historique ----------
    'moc.history.with-actor': (
        '{$event} par {$actor}', '{$event} by {$actor}', '{$event} por {$actor}', '{$event} بواسطة {$actor}',
        '{$event}（{$actor}）', '{$event}（{$actor}）'),
    'moc.history.created': ('Demande créée', 'Request created', 'Solicitud creada', 'تم إنشاء الطلب', '申請作成', '申请已创建'),
    'moc.history.updated': ('Demande modifiée', 'Request updated', 'Solicitud modificada', 'تم تعديل الطلب', '申請更新', '申请已修改'),
    'moc.history.sent-to-analysis': ('Envoyée en analyse', 'Sent to analysis', 'Enviada a análisis', 'أُرسل للتحليل', '分析に回付', '已提交分析'),
    'moc.history.analysis-completed': (
        "Analyse d'impact terminée", 'Impact analysis completed', 'Análisis de impacto terminado',
        'اكتمل تحليل الأثر', '影響分析完了', '影响分析已完成'),
    'moc.history.submitted': ('Soumise à validation', 'Submitted for approval', 'Enviada a validación',
                              'أُرسل للاعتماد', '承認に提出', '已提交审批'),
    'moc.history.validation-approved': ('Validation accordée', 'Approval granted', 'Validación concedida',
                                        'تم منح الاعتماد', '承認', '审批通过'),
    'moc.history.validation-rejected': ('Validation refusée', 'Approval refused', 'Validación denegada',
                                        'تم رفض الاعتماد', '承認拒否', '审批被拒'),
    'moc.history.approved': ('Demande approuvée', 'Request approved', 'Solicitud aprobada', 'تم اعتماد الطلب', '申請承認', '申请已批准'),
    'moc.history.rejected': ('Demande rejetée', 'Request rejected', 'Solicitud rechazada', 'تم رفض الطلب', '申請却下', '申请已拒绝'),
    'moc.history.reopened': ('Analyse rouverte', 'Analysis reopened', 'Análisis reabierto', 'أُعيد فتح التحليل', '分析再開', '分析已重新打开'),
    'moc.history.reminder-sent': (
        'Rappel envoyé aux validateurs', 'Reminder sent to approvers', 'Recordatorio enviado a los validadores',
        'تم إرسال تذكير إلى المعتمِدين', '承認者にリマインダーを送信', '已向审批人发送提醒'),
    'moc.history.legacy-status': ('Statut antérieur au MOC', 'Status before MOC', 'Estado anterior al MOC',
                                  'الحالة السابقة لـ MOC', 'MOC 導入前のステータス', 'MOC 之前的状态'),
    'moc.history.legacy-decision': ('Décision antérieure au MOC', 'Decision before MOC', 'Decisión anterior al MOC',
                                    'قرار سابق لـ MOC', 'MOC 導入前の決定', 'MOC 之前的决定'),
    'moc.history.legacy-impact': ('Objet lié avant le MOC', 'Object linked before MOC', 'Objeto vinculado antes del MOC',
                                  'عنصر مرتبط قبل MOC', 'MOC 導入前の関連対象', 'MOC 之前关联的对象'),
    'moc.history.empty': ('Aucun événement.', 'No event.', 'Ningún evento.', 'لا توجد أحداث.', 'イベントはありません。', '暂无事件。'),

    # ---------- analyse d'impact ----------
    'moc.impact.title': (
        'Classification du changement', 'Change classification', 'Clasificación del cambio', 'تصنيف التغيير',
        '変更の分類', '变更分类'),
    'moc.impact.score-aria': ("Score d'impact", 'Impact score', 'Puntuación de impacto', 'درجة الأثر', '影響スコア', '影响评分'),
    'moc.impact.incomplete': (
        "{$DONE} domaine(s) évalué(s) sur 8. La classification reste « Non évalué » et la soumission à validation est bloquée tant que les huit domaines ne sont pas évalués.",
        '{$DONE} of 8 domain(s) assessed. The classification stays “Not assessed” and submission for approval is blocked until all eight domains are assessed.',
        '{$DONE} de 8 dominio(s) evaluado(s). La clasificación sigue «No evaluado» y el envío a validación está bloqueado hasta evaluar los ocho dominios.',
        'تم تقييم {$DONE} من 8 مجالات. يبقى التصنيف «غير مقيّم» ويبقى الإرسال للاعتماد محظورًا حتى تقييم المجالات الثمانية.',
        '8 領域中 {$DONE} 件を評価済み。8 領域すべてを評価するまで、分類は「未評価」のままで承認への提出はできません。',
        '已评估 8 个领域中的 {$DONE} 个。在八个领域全部评估之前，分类保持“未评估”，且无法提交审批。'),
    'moc.impact.draft': (
        "L'analyse d'impact s'ouvre quand la demande est envoyée en analyse.",
        'The impact analysis opens when the request is sent to analysis.',
        'El análisis de impacto se abre cuando la solicitud se envía a análisis.',
        'يُفتح تحليل الأثر عند إرسال الطلب للتحليل.',
        '影響分析は申請が分析に回された時点で開始されます。',
        '申请提交分析后即可进行影响分析。'),
    'moc.impact.frozen': (
        'Analyse figée à la soumission à validation.', 'Analysis frozen at submission for approval.',
        'Análisis congelado al enviarse a validación.', 'تم تجميد التحليل عند الإرسال للاعتماد.',
        '承認への提出時に分析は確定しました。', '提交审批时分析已锁定。'),
    'moc.impact.weight-tooltip': (
        "Poids de ce domaine dans le score, réglé par l'administrateur",
        'Weight of this domain in the score, set by the administrator',
        'Peso de este dominio en la puntuación, definido por el administrador',
        'وزن هذا المجال في الدرجة، يحدده المسؤول',
        'スコアにおけるこの領域の重み（管理者が設定）', '该领域在评分中的权重，由管理员设置'),
    'moc.impact.notes': (
        "Nature de l'impact et mesures de maîtrise", 'Nature of the impact and control measures',
        'Naturaleza del impacto y medidas de control', 'طبيعة الأثر وتدابير التحكم',
        '影響の内容と管理策', '影响性质及控制措施'),
    'moc.impact-failed': (
        "L'évaluation n'a pas été enregistrée.", 'The assessment was not saved.', 'La evaluación no se ha guardado.',
        'لم يتم حفظ التقييم.', '評価を保存できませんでした。', '评估未能保存。'),

    # ---------- validation ----------
    'moc.validation.summary': (
        'Changement {$classe} : {$count} validation(s) requise(s). Un seul refus rejette la demande.',
        '{$classe} change: {$count} approval(s) required. A single refusal rejects the request.',
        'Cambio {$classe}: {$count} validación(es) requerida(s). Un solo rechazo rechaza la solicitud.',
        'تغيير {$classe}: {$count} اعتماد(ات) مطلوبة. رفض واحد يكفي لرفض الطلب.',
        '{$classe} の変更：必要な承認は {$count} 件。1 件でも拒否されると申請は却下されます。',
        '{$classe}变更：需要 {$count} 项审批。任一拒绝即驳回申请。'),
    'moc.validation.not-assessed': (
        "Les validateurs requis seront connus une fois les huit domaines d'impact évalués.",
        'The required approvers will be known once all eight impact domains are assessed.',
        'Los validadores requeridos se conocerán una vez evaluados los ocho dominios de impacto.',
        'سيُعرف المعتمِدون المطلوبون بعد تقييم مجالات الأثر الثمانية.',
        '必要な承認者は、8 つの影響領域をすべて評価した後に決まります。',
        '八个影响领域全部评估后，才会确定所需审批人。'),
    'moc.validation.legacy': (
        "Demande antérieure au MOC : ses décisions sont consignées dans l'historique.",
        'Request created before MOC: its decisions are recorded in the history.',
        'Solicitud anterior al MOC: sus decisiones constan en el historial.',
        'طلب سابق لـ MOC: قراراته مسجّلة في السجل.',
        'MOC 導入前の申請です。決定内容は履歴に記録されています。',
        'MOC 之前的申请：其决定记录在历史中。'),
    'moc.validation.preview': (
        'Aperçu : la liste des validateurs sera figée à la soumission à validation.',
        'Preview: the list of approvers will be frozen at submission for approval.',
        'Vista previa: la lista de validadores se congelará al enviarse a validación.',
        'معاينة: ستُثبَّت قائمة المعتمِدين عند الإرسال للاعتماد.',
        'プレビュー：承認者の一覧は承認への提出時に確定します。',
        '预览：审批人名单将在提交审批时锁定。'),
    'moc.validation.approved-by': ('Approuvé par {$NAME}', 'Approved by {$NAME}', 'Aprobado por {$NAME}',
                                   'اعتمده {$NAME}', '承認者：{$NAME}', '批准人：{$NAME}'),
    'moc.validation.rejected-by': ('Rejeté par {$NAME}', 'Rejected by {$NAME}', 'Rechazado por {$NAME}',
                                   'رفضه {$NAME}', '却下者：{$NAME}', '拒绝人：{$NAME}'),
    'moc.validation.holders': ('Titulaires : {$NAMES}', 'Holders: {$NAMES}', 'Titulares: {$NAMES}',
                               'أصحاب الدور: {$NAMES}', '担当者：{$NAMES}', '负责人：{$NAMES}'),
    'moc.validation.waiting-for': ('En attente de : {$NAMES}', 'Waiting for: {$NAMES}', 'A la espera de: {$NAMES}',
                                   'بانتظار: {$NAMES}', '承認待ち：{$NAMES}', '等待：{$NAMES}'),
    'moc.validation.no-holder': (
        "Aucun titulaire : l'administrateur doit en nommer un dans les paramètres MOC.",
        'No holder: the administrator must appoint one in the MOC settings.',
        'Ningún titular: el administrador debe designar uno en los parámetros MOC.',
        'لا يوجد صاحب دور: يجب على المسؤول تعيين واحد في إعدادات MOC.',
        '担当者がいません。管理者が MOC 設定で指名する必要があります。',
        '无负责人：管理员需在 MOC 设置中指定。'),
    'moc.validation.self': (
        'Vous êtes le demandeur : vous ne pouvez pas valider votre propre demande.',
        'You are the requester: you cannot approve your own request.',
        'Usted es el solicitante: no puede validar su propia solicitud.',
        'أنت مقدّم الطلب: لا يمكنك اعتماد طلبك.',
        'あなたは申請者のため、自分の申請を承認できません。',
        '您是申请人：不能审批自己的申请。'),
    'moc.validation.comment': (
        'Commentaire (obligatoire en cas de refus)', 'Comment (required when rejecting)',
        'Comentario (obligatorio en caso de rechazo)', 'تعليق (إلزامي في حالة الرفض)',
        'コメント（却下時は必須）', '评论（拒绝时必填）'),
    'moc.validation.approve': ('Approuver', 'Approve', 'Aprobar', 'اعتماد', '承認', '批准'),
    'moc.validation.reject': ('Rejeter', 'Reject', 'Rechazar', 'رفض', '却下', '拒绝'),
    'moc.validation.reject-reason': ('Motif du refus (obligatoire)', 'Reason for rejection (required)',
        'Motivo del rechazo (obligatorio)', 'سبب الرفض (إلزامي)', '却下の理由（必須）', '拒绝理由（必填）'),
    'moc.validation.reject-confirm': ('Confirmer le rejet', 'Confirm rejection', 'Confirmar el rechazo',
        'تأكيد الرفض', '却下を確定', '确认拒绝'),
    'moc.validation.reject-hint': (
        'Un refus doit être motivé par un commentaire', 'A rejection must be justified by a comment',
        'Un rechazo debe justificarse con un comentario', 'يجب تعليل الرفض بتعليق',
        '却下にはコメントによる理由が必要です', '拒绝须附评论说明理由'),
    'moc.validation.approved': ('Validation enregistrée.', 'Approval recorded.', 'Validación registrada.',
                                'تم تسجيل الاعتماد.', '承認を記録しました。', '审批已记录。'),
    'moc.validation.rejected': (
        'Refus enregistré : la demande est rejetée.', 'Rejection recorded: the request is rejected.',
        'Rechazo registrado: la solicitud queda rechazada.', 'تم تسجيل الرفض: رُفض الطلب.',
        '却下を記録しました。申請は却下されました。', '拒绝已记录：申请已被驳回。'),
    'moc.validation.failed': (
        "La décision n'a pas été enregistrée.", 'The decision was not saved.', 'La decisión no se ha guardado.',
        'لم يتم حفظ القرار.', '決定を保存できませんでした。', '决定未能保存。'),

    # ---------- saisie d'une demande ----------
    'moc.dialog.new-title': (
        'Nouvelle demande de changement', 'New change request', 'Nueva solicitud de cambio', 'طلب تغيير جديد',
        '新規変更申請', '新建变更申请'),
    'moc.dialog.edit-title': ('Modifier la demande {$code}', 'Edit request {$code}', 'Modificar la solicitud {$code}',
                              'تعديل الطلب {$code}', '申請 {$code} を編集', '编辑申请 {$code}'),
    'moc.dialog.title-label': ('Titre', 'Title', 'Título', 'العنوان', 'タイトル', '标题'),
    'moc.dialog.title-required': (
        'Donnez un titre à la demande.', 'Give the request a title.', 'Indique un título para la solicitud.',
        'أدخل عنوانًا للطلب.', '申請にタイトルを付けてください。', '请为申请填写标题。'),
    'moc.dialog.planned-hint': (
        "Facultatif : l'échéance apparaît au tableau de bord SMI.",
        'Optional: the due date appears on the IMS dashboard.',
        'Opcional: la fecha aparece en el panel del SIG.',
        'اختياري: يظهر الموعد في لوحة نظام الإدارة المتكامل.',
        '任意：期日は統合マネジメントシステムのダッシュボードに表示されます。',
        '可选：截止日期将显示在综合管理体系仪表板上。'),
    'moc.dialog.create': ('Créer la demande', 'Create the request', 'Crear la solicitud', 'إنشاء الطلب', '申請を作成', '创建申请'),
    'moc.dialog.created': ('Demande {$code} créée.', 'Request {$code} created.', 'Solicitud {$code} creada.',
                           'تم إنشاء الطلب {$code}.', '申請 {$code} を作成しました。', '申请 {$code} 已创建。'),
    'moc.dialog.updated': ('Demande modifiée.', 'Request updated.', 'Solicitud modificada.', 'تم تعديل الطلب.',
                           '申請を更新しました。', '申请已修改。'),
    'moc.dialog.failed': (
        "La demande n'a pas été enregistrée.", 'The request was not saved.', 'La solicitud no se ha guardado.',
        'لم يتم حفظ الطلب.', '申請を保存できませんでした。', '申请未能保存。'),

    # ---------- refus du serveur ----------
    'moc.error.analysis-incomplete': (
        "Évaluez les huit domaines d'impact avant de soumettre à validation.",
        'Assess all eight impact domains before submitting for approval.',
        'Evalúe los ocho dominios de impacto antes de enviar a validación.',
        'قيّم مجالات الأثر الثمانية قبل الإرسال للاعتماد.',
        '承認に提出する前に 8 つの影響領域をすべて評価してください。',
        '提交审批前请评估全部八个影响领域。'),
    'moc.error.role-without-holder': (
        "Le rôle « {$role} » n'a aucun titulaire : demandez à l'administrateur d'en nommer un.",
        'The role “{$role}” has no holder: ask the administrator to appoint one.',
        'El rol «{$role}» no tiene titular: pida al administrador que designe uno.',
        'الدور «{$role}» بلا صاحب: اطلب من المسؤول تعيين واحد.',
        '役割「{$role}」に担当者がいません。管理者に指名を依頼してください。',
        '角色“{$role}”没有负责人：请联系管理员指定。'),
    'moc.error.role-only-requester': (
        "Le rôle « {$role} » n'a pour titulaire que le demandeur, qui ne peut pas valider sa propre demande.",
        'The only holder of the role “{$role}” is the requester, who cannot approve their own request.',
        'El único titular del rol «{$role}» es el solicitante, que no puede validar su propia solicitud.',
        'صاحب الدور «{$role}» الوحيد هو مقدّم الطلب، ولا يمكنه اعتماد طلبه.',
        '役割「{$role}」の担当者は申請者のみで、申請者は自分の申請を承認できません。',
        '角色“{$role}”的唯一负责人是申请人，申请人不能审批自己的申请。'),
    'moc.error.not-holder': (
        'Seul un titulaire du rôle « {$role} » peut statuer sur cette carte.',
        'Only a holder of the role “{$role}” can decide on this card.',
        'Solo un titular del rol «{$role}» puede decidir sobre esta tarjeta.',
        'لا يمكن البتّ في هذه البطاقة إلا لصاحب الدور «{$role}».',
        'このカードを判断できるのは役割「{$role}」の担当者のみです。',
        '只有角色“{$role}”的负责人才能处理此卡片。'),
    'moc.error.not-owner': (
        'Seul le demandeur ou un gestionnaire des changements peut conduire cette demande.',
        'Only the requester or a change manager can handle this request.',
        'Solo el solicitante o un gestor de cambios puede gestionar esta solicitud.',
        'لا يمكن إدارة هذا الطلب إلا لمقدّمه أو لمدير التغييرات.',
        'この申請を進められるのは申請者または変更管理者のみです。',
        '只有申请人或变更管理者才能处理此申请。'),
    'moc.error.self-validation': (
        'Vous êtes le demandeur : vous ne pouvez pas valider votre propre demande.',
        'You are the requester: you cannot approve your own request.',
        'Usted es el solicitante: no puede validar su propia solicitud.',
        'أنت مقدّم الطلب: لا يمكنك اعتماد طلبك.',
        'あなたは申請者のため、自分の申請を承認できません。',
        '您是申请人：不能审批自己的申请。'),
    'moc.error.comment-required': (
        'Un refus doit être motivé par un commentaire.', 'A rejection must be justified by a comment.',
        'Un rechazo debe justificarse con un comentario.', 'يجب تعليل الرفض بتعليق.',
        '却下にはコメントによる理由が必要です。', '拒绝须附评论说明理由。'),
    'moc.error.already-decided': ('Ce rôle a déjà statué.', 'This role has already decided.', 'Este rol ya ha decidido.',
                                  'لقد بتّ هذا الدور بالفعل.', 'この役割はすでに判断済みです。', '该角色已作出决定。'),
    'moc.error.delete-not-draft': (
        'Seule une demande en brouillon peut être supprimée.', 'Only a draft request can be deleted.',
        'Solo se puede eliminar una solicitud en borrador.', 'لا يمكن حذف إلا الطلب الذي في حالة مسودة.',
        '削除できるのは下書きの申請のみです。', '只能删除草稿状态的申请。'),
    'moc.error.role-in-use': (
        'Le rôle « {$role} » est attendu par une demande en validation : il ne peut pas être retiré.',
        'The role “{$role}” is awaited by a request in approval: it cannot be removed.',
        'El rol «{$role}» lo espera una solicitud en validación: no se puede quitar.',
        'الدور «{$role}» منتظَر في طلب قيد الاعتماد: لا يمكن حذفه.',
        '役割「{$role}」は承認中の申請で必要とされているため削除できません。',
        '角色“{$role}”正被审批中的申请等待：无法删除。'),
    'moc.error.role-in-use-holders': (
        'Le rôle « {$role} » est attendu par une demande en validation : il doit garder au moins un titulaire.',
        'The role “{$role}” is awaited by a request in approval: it must keep at least one holder.',
        'El rol «{$role}» lo espera una solicitud en validación: debe conservar al menos un titular.',
        'الدور «{$role}» منتظَر في طلب قيد الاعتماد: يجب أن يحتفظ بصاحب واحد على الأقل.',
        '役割「{$role}」は承認中の申請で必要とされているため、担当者を 1 人以上残す必要があります。',
        '角色“{$role}”正被审批中的申请等待：必须至少保留一名负责人。'),
    'moc.error.role-duplicate': (
        'Deux rôles portent le même libellé « {$role} ».', 'Two roles have the same label “{$role}”.',
        'Dos roles tienen la misma denominación «{$role}».', 'دوران يحملان الاسم نفسه «{$role}».',
        '2 つの役割が同じ名称「{$role}」です。', '两个角色名称相同：“{$role}”。'),
    'moc.error.required-empty': (
        'Chaque classification doit exiger au moins un rôle de validation.',
        'Each classification must require at least one approval role.',
        'Cada clasificación debe exigir al menos un rol de validación.',
        'يجب أن يتطلب كل تصنيف دور اعتماد واحدًا على الأقل.',
        '各分類には少なくとも 1 つの承認役割が必要です。',
        '每个分类必须至少要求一个审批角色。'),
    'moc.error.thresholds': (
        'Les seuils doivent respecter 0 < seuil majeur < seuil critique ≤ 100.',
        'Thresholds must satisfy 0 < major threshold < critical threshold ≤ 100.',
        'Los umbrales deben cumplir 0 < umbral mayor < umbral crítico ≤ 100.',
        'يجب أن تحقق العتبات 0 < عتبة رئيسي < عتبة حرج ≤ 100.',
        'しきい値は 0 < 重大しきい値 < 重要危機しきい値 ≤ 100 を満たす必要があります。',
        '阈值必须满足 0 < 重大阈值 < 关键阈值 ≤ 100。'),
    'moc.error.weights-zero': (
        'Au moins un domaine doit avoir un poids supérieur à 0.', 'At least one domain must have a weight above 0.',
        'Al menos un dominio debe tener un peso superior a 0.', 'يجب أن يكون لمجال واحد على الأقل وزن أكبر من 0.',
        '少なくとも 1 つの領域の重みを 0 より大きくしてください。', '至少一个领域的权重必须大于 0。'),

    # ---------- paramètres MOC ----------
    'moc.settings.title': ('Paramètres MOC', 'MOC settings', 'Parámetros MOC', 'إعدادات MOC', 'MOC 設定', 'MOC 设置'),
    'moc.settings.subtitle': (
        'Classification, poids des domaines, rappel et validateurs de la gestion des changements.',
        'Classification, domain weights, reminders and approvers for management of change.',
        'Clasificación, pesos de los dominios, recordatorio y validadores de la gestión del cambio.',
        'التصنيف وأوزان المجالات والتذكير والمعتمِدون في إدارة التغيير.',
        '変更管理の分類、領域の重み、リマインダー、承認者。',
        '变更管理的分类、领域权重、提醒和审批人。'),
    'moc.settings.back': ('Gestion des changements', 'Management of change', 'Gestión del cambio', 'إدارة التغيير',
                          '変更管理', '变更管理'),
    'moc.settings.load-failed': (
        "Les paramètres MOC n'ont pas pu être chargés.", 'The MOC settings could not be loaded.',
        'No se pudieron cargar los parámetros MOC.', 'تعذّر تحميل إعدادات MOC.',
        'MOC 設定を読み込めませんでした。', '无法加载 MOC 设置。'),
    'moc.settings.scope': (
        'Un enregistrement reclasse aussitôt les demandes en brouillon ou en analyse. Les demandes déjà en validation, approuvées ou rejetées gardent leur score, leur classification et leurs validateurs, figés à la soumission.',
        'Saving immediately reclassifies requests in draft or in analysis. Requests already in approval, approved or rejected keep the score, classification and approvers frozen at submission.',
        'Al guardar se reclasifican de inmediato las solicitudes en borrador o en análisis. Las ya en validación, aprobadas o rechazadas conservan la puntuación, la clasificación y los validadores congelados al enviarse.',
        'يعيد الحفظ تصنيف الطلبات المسودة أو قيد التحليل فورًا. أما الطلبات قيد الاعتماد أو المعتمدة أو المرفوضة فتحتفظ بالدرجة والتصنيف والمعتمِدين المثبّتين عند الإرسال.',
        '保存すると下書きまたは分析中の申請は直ちに再分類されます。承認中・承認済み・却下済みの申請は、提出時に確定したスコア、分類、承認者を維持します。',
        '保存后，草稿或分析中的申请会立即重新分类。已在审批中、已批准或已拒绝的申请保留提交时锁定的评分、分类和审批人。'),
    'moc.settings.thresholds': ('Seuils de classification', 'Classification thresholds', 'Umbrales de clasificación',
                                'عتبات التصنيف', '分類しきい値', '分类阈值'),
    'moc.settings.thresholds-help': (
        "Le score d'impact va de 0 à 100. Il classe la demande, et la classe désigne les validateurs requis.",
        'The impact score ranges from 0 to 100. It classifies the request, and the class determines the required approvers.',
        'La puntuación de impacto va de 0 a 100. Clasifica la solicitud, y la clase determina los validadores requeridos.',
        'تتراوح درجة الأثر من 0 إلى 100. وهي تصنّف الطلب، والتصنيف يحدد المعتمِدين المطلوبين.',
        '影響スコアは 0〜100 です。スコアが申請を分類し、分類が必要な承認者を決めます。',
        '影响评分范围为 0 到 100。评分决定申请的分类，分类决定所需审批人。'),
    'moc.settings.major': ('Seuil majeur', 'Major threshold', 'Umbral mayor', 'عتبة رئيسي', '重大しきい値', '重大阈值'),
    'moc.settings.critical': ('Seuil critique', 'Critical threshold', 'Umbral crítico', 'عتبة حرج', '重要危機しきい値', '关键阈值'),
    'moc.settings.threshold-summary': (
        'Mineur < {$major} ≤ Majeur < {$critical} ≤ Critique', 'Minor < {$major} ≤ Major < {$critical} ≤ Critical',
        'Menor < {$major} ≤ Mayor < {$critical} ≤ Crítico', 'ثانوي < {$major} ≤ رئيسي < {$critical} ≤ حرج',
        '軽微 < {$major} ≤ 重大 < {$critical} ≤ 重要危機', '轻微 < {$major} ≤ 重大 < {$critical} ≤ 关键'),
    'moc.settings.reminder': ('Rappel aux validateurs', 'Approver reminders', 'Recordatorio a los validadores',
                              'تذكير المعتمِدين', '承認者へのリマインダー', '审批人提醒'),
    'moc.settings.reminder-help': (
        "Sans décision dans ce délai, les titulaires du rôle reçoivent un rappel, répété au même rythme tant que personne n'a statué.",
        'Without a decision within this period, the role holders receive a reminder, repeated at the same pace until someone decides.',
        'Sin decisión en este plazo, los titulares del rol reciben un recordatorio, repetido al mismo ritmo hasta que alguien decida.',
        'في غياب قرار خلال هذه المدة، يتلقى أصحاب الدور تذكيرًا يتكرر بالوتيرة نفسها حتى يبتّ أحدهم.',
        'この期間内に判断がない場合、役割の担当者にリマインダーが送られ、誰かが判断するまで同じ間隔で繰り返されます。',
        '在此期限内未作决定时，角色负责人会收到提醒，并按相同频率重复，直到有人作出决定。'),
    'moc.settings.reminder-days': ('Délai (jours)', 'Delay (days)', 'Plazo (días)', 'المهلة (أيام)', '期間（日）', '期限（天）'),
    'moc.settings.weights': ("Poids des domaines d'impact", 'Impact domain weights', 'Pesos de los dominios de impacto',
                             'أوزان مجالات الأثر', '影響領域の重み', '影响领域权重'),
    'moc.settings.weights-help': (
        'De 0 à 10. Un domaine de poids 0 reste à évaluer mais ne compte pas dans le score. À 1 partout, le score est la moyenne simple.',
        'From 0 to 10. A domain weighted 0 must still be assessed but does not count in the score. With 1 everywhere, the score is the simple average.',
        'De 0 a 10. Un dominio con peso 0 debe evaluarse pero no cuenta en la puntuación. Con 1 en todos, la puntuación es la media simple.',
        'من 0 إلى 10. المجال ذو الوزن 0 يبقى واجب التقييم لكنه لا يُحتسب في الدرجة. عند الوزن 1 للجميع، تكون الدرجة متوسطًا بسيطًا.',
        '0〜10。重み 0 の領域も評価は必要ですが、スコアには含まれません。すべて 1 の場合、スコアは単純平均です。',
        '0 到 10。权重为 0 的领域仍需评估，但不计入评分。全部为 1 时，评分即为简单平均。'),
    'moc.settings.roles': ('Rôles de validation', 'Approval roles', 'Roles de validación', 'أدوار الاعتماد', '承認役割', '审批角色'),
    'moc.settings.roles-help': (
        "Pour chaque rôle : les classifications qui l'exigent et ses titulaires (principal en premier, puis suppléants). N'importe lequel d'entre eux peut statuer ; jamais le demandeur.",
        'For each role: the classifications that require it and its holders (primary first, then deputies). Any of them can decide; never the requester.',
        'Para cada rol: las clasificaciones que lo exigen y sus titulares (principal primero, luego suplentes). Cualquiera de ellos puede decidir; nunca el solicitante.',
        'لكل دور: التصنيفات التي تتطلبه وأصحابه (الأساسي أولًا ثم النواب). يمكن لأي منهم البتّ؛ وليس مقدّم الطلب أبدًا.',
        '各役割について、その役割を必要とする分類と担当者（主担当、次に代理）。いずれの担当者も判断できますが、申請者は判断できません。',
        '每个角色：要求该角色的分类及其负责人（主负责人在前，其次为代理人）。任何负责人均可作出决定；申请人除外。'),
    'moc.settings.add-role': ('Ajouter un rôle', 'Add a role', 'Añadir un rol', 'إضافة دور', '役割を追加', '添加角色'),
    'moc.settings.role-label': ('Libellé du rôle', 'Role label', 'Denominación del rol', 'اسم الدور', '役割名', '角色名称'),
    'moc.settings.up': ('Monter le rôle', 'Move role up', 'Subir el rol', 'نقل الدور لأعلى', '役割を上へ', '上移角色'),
    'moc.settings.down': ('Descendre le rôle', 'Move role down', 'Bajar el rol', 'نقل الدور لأسفل', '役割を下へ', '下移角色'),
    'moc.settings.remove-role': ('Retirer le rôle', 'Remove the role', 'Quitar el rol', 'إزالة الدور', '役割を削除', '移除角色'),
    'moc.settings.required-for': ('Requis pour', 'Required for', 'Requerido para', 'مطلوب لـ', '必要な分類', '适用于'),
    'moc.settings.holders': ('Titulaires', 'Holders', 'Titulares', 'أصحاب الدور', '担当者', '负责人'),
    'moc.settings.primary': ('Principal ·', 'Primary ·', 'Principal ·', 'أساسي ·', '主担当・', '主负责人 ·'),
    'moc.settings.inactive': ('(désactivé)', '(deactivated)', '(desactivado)', '(معطّل)', '（無効）', '（已停用）'),
    'moc.settings.unknown': ("(absent de l'annuaire)", '(not in directory)', '(no está en el directorio)',
                             '(غير موجود في الدليل)', '（ディレクトリに存在しません）', '（不在目录中）'),
    'moc.settings.remove-holder': ('Retirer {$member}', 'Remove {$member}', 'Quitar a {$member}', 'إزالة {$member}',
                                   '{$member} を外す', '移除 {$member}'),
    'moc.settings.no-holder': (
        'Aucun titulaire : une demande qui exige ce rôle ne pourra pas être soumise à validation.',
        'No holder: a request requiring this role cannot be submitted for approval.',
        'Ningún titular: una solicitud que exija este rol no podrá enviarse a validación.',
        'لا يوجد صاحب دور: لا يمكن إرسال طلب يتطلب هذا الدور للاعتماد.',
        '担当者がいません。この役割を必要とする申請は承認に提出できません。',
        '无负责人：需要该角色的申请将无法提交审批。'),
    'moc.settings.add-holder': (
        'Ajouter un titulaire (annuaire)', 'Add a holder (directory)', 'Añadir un titular (directorio)',
        'إضافة صاحب دور (الدليل)', '担当者を追加（ディレクトリ）', '添加负责人（目录）'),
    'moc.settings.pending': ('Paramètres modifiés', 'Settings changed', 'Parámetros modificados', 'تم تعديل الإعدادات',
                             '設定が変更されました', '设置已修改'),
    'moc.settings.saved': (
        'Paramètres MOC enregistrés. Les demandes en brouillon ou en analyse sont reclassées.',
        'MOC settings saved. Requests in draft or in analysis have been reclassified.',
        'Parámetros MOC guardados. Las solicitudes en borrador o en análisis se han reclasificado.',
        'تم حفظ إعدادات MOC. أُعيد تصنيف الطلبات المسودة أو قيد التحليل.',
        'MOC 設定を保存しました。下書きまたは分析中の申請は再分類されました。',
        'MOC 设置已保存。草稿或分析中的申请已重新分类。'),
    'moc.settings.failed': (
        "Les paramètres MOC n'ont pas pu être enregistrés.", 'The MOC settings could not be saved.',
        'No se pudieron guardar los parámetros MOC.', 'تعذّر حفظ إعدادات MOC.',
        'MOC 設定を保存できませんでした。', '无法保存 MOC 设置。'),
    'moc.settings.problem.thresholds': (
        'Les seuils doivent respecter 0 < seuil majeur < seuil critique ≤ 100.',
        'Thresholds must satisfy 0 < major threshold < critical threshold ≤ 100.',
        'Los umbrales deben cumplir 0 < umbral mayor < umbral crítico ≤ 100.',
        'يجب أن تحقق العتبات 0 < عتبة رئيسي < عتبة حرج ≤ 100.',
        'しきい値は 0 < 重大しきい値 < 重要危機しきい値 ≤ 100 を満たす必要があります。',
        '阈值必须满足 0 < 重大阈值 < 关键阈值 ≤ 100。'),
    'moc.settings.problem.reminder': (
        'Le délai de rappel est un nombre de jours de 1 à 30.', 'The reminder delay is a number of days from 1 to 30.',
        'El plazo del recordatorio es un número de días de 1 a 30.', 'مهلة التذكير عدد أيام من 1 إلى 30.',
        'リマインダーの期間は 1〜30 日です。', '提醒期限为 1 到 30 天。'),
    'moc.settings.problem.weights': (
        'Chaque poids est un entier de 0 à 10.', 'Each weight is a whole number from 0 to 10.',
        'Cada peso es un número entero de 0 a 10.', 'كل وزن عدد صحيح من 0 إلى 10.',
        '各重みは 0〜10 の整数です。', '每个权重为 0 到 10 的整数。'),
    'moc.settings.problem.weights-zero': (
        'Au moins un domaine doit avoir un poids supérieur à 0.', 'At least one domain must have a weight above 0.',
        'Al menos un dominio debe tener un peso superior a 0.', 'يجب أن يكون لمجال واحد على الأقل وزن أكبر من 0.',
        '少なくとも 1 つの領域の重みを 0 より大きくしてください。', '至少一个领域的权重必须大于 0。'),
    'moc.settings.problem.no-role': (
        'Il faut au moins un rôle de validation.', 'At least one approval role is required.',
        'Se necesita al menos un rol de validación.', 'يلزم دور اعتماد واحد على الأقل.',
        '承認役割が少なくとも 1 つ必要です。', '至少需要一个审批角色。'),
    'moc.settings.problem.label': (
        'Chaque rôle porte un libellé de 1 à 100 caractères.', 'Each role has a label of 1 to 100 characters.',
        'Cada rol tiene una denominación de 1 a 100 caracteres.', 'لكل دور اسم من 1 إلى 100 حرف.',
        '各役割の名称は 1〜100 文字です。', '每个角色名称为 1 到 100 个字符。'),
    'moc.settings.problem.duplicate': (
        'Deux rôles portent le même libellé.', 'Two roles have the same label.', 'Dos roles tienen la misma denominación.',
        'دوران يحملان الاسم نفسه.', '2 つの役割が同じ名称です。', '两个角色名称相同。'),
    'moc.settings.problem.required': (
        "La classification « {$classe} » n'exige aucun rôle.", 'The classification “{$classe}” requires no role.',
        'La clasificación «{$classe}» no exige ningún rol.', 'التصنيف «{$classe}» لا يتطلب أي دور.',
        '分類「{$classe}」に必要な役割がありません。', '分类“{$classe}”未要求任何角色。'),
}
