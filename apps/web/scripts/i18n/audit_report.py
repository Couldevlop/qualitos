# -*- coding: utf-8 -*-
"""Table i18n - audits : rapport, documents joints, auditeur responsable et
raison des actions barrees (SS4.4). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    # --- auditeur responsable ---
    'audits.lead-auditor': (
        'Auditeur responsable', 'Lead auditor', 'Auditor responsable',
        'المدقّق المسؤول', '主任監査員', '审核组长'),
    'audits.lead-auditor.hint': (
        "Celui ou celle qui conduit l'audit et signe le rapport. Par défaut : vous.",
        'The person who leads the audit and signs the report. Defaults to you.',
        'La persona que dirige la auditoría y firma el informe. Por defecto: usted.',
        'الشخص الذي يقود التدقيق ويوقّع التقرير. افتراضيًا: أنت.',
        '監査を主導し報告書に署名する人。既定ではあなたです。',
        '负责实施审核并签署报告的人。默认为您。'),
    'audits.lead-auditor.hint-edit': (
        "Celui ou celle qui conduit l'audit et signe le rapport.",
        'The person who leads the audit and signs the report.',
        'La persona que dirige la auditoría y firma el informe.',
        'الشخص الذي يقود التدقيق ويوقّع التقرير.',
        '監査を主導し報告書に署名する人。',
        '负责实施审核并签署报告的人。'),
    'audits.lead-auditor.directory-down': (
        "L'annuaire de l'organisation n'est pas accessible avec vos droits : l'audit vous est confié.",
        "The organization directory is not available with your permissions: the audit is assigned to you.",
        'El directorio de la organización no es accesible con sus permisos: la auditoría se le asigna a usted.',
        'دليل المؤسسة غير متاح بصلاحياتك: يُسند التدقيق إليك.',
        '権限により組織のディレクトリを参照できないため、監査はあなたに割り当てられます。',
        '您的权限无法访问组织目录：审核将分配给您。'),
    'audits.lead-auditor.directory-down-edit': (
        "L'annuaire de l'organisation n'est pas accessible avec vos droits : seuls vous et l'auditeur actuel sont proposés.",
        'The organization directory is not available with your permissions: only you and the current auditor are offered.',
        'El directorio de la organización no es accesible con sus permisos: solo se proponen usted y el auditor actual.',
        'دليل المؤسسة غير متاح بصلاحياتك: لا يُقترح سواك والمدقّق الحالي.',
        '権限により組織のディレクトリを参照できないため、あなたと現在の監査員のみが表示されます。',
        '您的权限无法访问组织目录：仅提供您和当前审核员。'),
    'audits.lead-auditor.required': (
        "Désignez l'auditeur responsable.", 'Select the lead auditor.',
        'Designe al auditor responsable.', 'حدّد المدقّق المسؤول.',
        '主任監査員を選択してください。', '请指定审核组长。'),
    'audits.lead-auditor.me': (
        '{$name} (vous)', '{$name} (you)', '{$name} (usted)',
        '{$name} (أنت)', '{$name}（あなた）', '{$name}（您）'),
    'audits.lead-auditor.unknown': (
        "Membre de l'organisation (nom non enregistré)",
        'Organization member (name not recorded)',
        'Miembro de la organización (nombre no registrado)',
        'عضو في المؤسسة (الاسم غير مسجّل)',
        '組織のメンバー（氏名未登録）',
        '组织成员（未记录姓名）'),

    # --- constats : pourquoi le bouton est barre ---
    'audits.detail.finding-blocked-planned': (
        "Les constats se relèvent pendant l'audit : démarrez-le pour en ajouter.",
        'Findings are raised during the audit: start it to add some.',
        'Los hallazgos se registran durante la auditoría: iníciela para añadirlos.',
        'تُسجَّل الملاحظات أثناء التدقيق: ابدأه لإضافتها.',
        '所見は監査中に記録します。追加するには監査を開始してください。',
        '审核发现在审核期间记录：请先启动审核再添加。'),
    'audits.detail.finding-blocked-no-checklist': (
        "Les constats se relèvent pendant l'audit. Ajoutez d'abord une question à la checklist, puis démarrez l'audit.",
        'Findings are raised during the audit. First add a question to the checklist, then start the audit.',
        'Los hallazgos se registran durante la auditoría. Añada primero una pregunta a la lista de verificación y luego inicie la auditoría.',
        'تُسجَّل الملاحظات أثناء التدقيق. أضف أولًا سؤالًا إلى قائمة التحقّق، ثم ابدأ التدقيق.',
        '所見は監査中に記録します。まずチェックリストに質問を追加し、監査を開始してください。',
        '审核发现在审核期间记录。请先在检查清单中添加问题，然后启动审核。'),
    'audits.detail.finding-blocked-completed': (
        'Audit clôturé : plus aucun constat ne peut être ajouté.',
        'Audit closed: no more findings can be added.',
        'Auditoría cerrada: ya no se pueden añadir hallazgos.',
        'التدقيق مغلق: لا يمكن إضافة ملاحظات أخرى.',
        '監査は終了しました。所見はこれ以上追加できません。',
        '审核已关闭：不能再添加发现。'),
    'audits.detail.finding-blocked-cancelled': (
        'Audit annulé : plus aucun constat ne peut être ajouté.',
        'Audit cancelled: no more findings can be added.',
        'Auditoría cancelada: ya no se pueden añadir hallazgos.',
        'التدقيق ملغى: لا يمكن إضافة ملاحظات أخرى.',
        '監査は中止されました。所見はこれ以上追加できません。',
        '审核已取消：不能再添加发现。'),
    'audits.detail.start-audit': (
        "Démarrer l'audit", 'Start the audit', 'Iniciar la auditoría',
        'بدء التدقيق', '監査を開始', '启动审核'),
    'audits.detail.start-blocked': (
        "Ajoutez au moins une question à la checklist pour démarrer l'audit.",
        'Add at least one question to the checklist to start the audit.',
        'Añada al menos una pregunta a la lista de verificación para iniciar la auditoría.',
        'أضف سؤالًا واحدًا على الأقل إلى قائمة التحقّق لبدء التدقيق.',
        '監査を開始するには、チェックリストに質問を1つ以上追加してください。',
        '请至少在检查清单中添加一个问题以启动审核。'),
    'audits.finding.not-in-progress': (
        "Un constat se relève pendant l'audit : démarrez l'audit pour en ajouter.",
        'A finding is raised during the audit: start the audit to add one.',
        'Un hallazgo se registra durante la auditoría: inicie la auditoría para añadirlo.',
        'تُسجَّل الملاحظة أثناء التدقيق: ابدأ التدقيق لإضافتها.',
        '所見は監査中に記録します。追加するには監査を開始してください。',
        '审核发现在审核期间记录：请启动审核后再添加。'),

    # --- documents : verrou ---
    'audits.detail.documents-locked-cancelled': (
        'Audit annulé : ses documents ne se modifient plus.',
        'Audit cancelled: its documents can no longer be changed.',
        'Auditoría cancelada: sus documentos ya no se pueden modificar.',
        'التدقيق ملغى: لم يعد بالإمكان تعديل مستنداته.',
        '監査は中止されました。文書は変更できません。',
        '审核已取消：其文件不能再修改。'),
    'audits.detail.documents-locked-issued': (
        "Rapport émis : les documents de l'audit sont figés avec lui.",
        "Report issued: the audit's documents are locked with it.",
        'Informe emitido: los documentos de la auditoría quedan bloqueados con él.',
        'صدر التقرير: جُمِّدت مستندات التدقيق معه.',
        '報告書が発行されました。監査の文書も確定されています。',
        '报告已发布：审核文件随之锁定。'),

    # --- rapport ---
    'audits.report.title': ('Rapport', 'Report', 'Informe', 'التقرير', '報告書', '报告'),
    'audits.report.state-draft': ('Brouillon', 'Draft', 'Borrador', 'مسودة', '下書き', '草稿'),
    'audits.report.state-issued': ('Émis', 'Issued', 'Emitido', 'صادر', '発行済み', '已发布'),
    'audits.report.figures-aria': (
        'Chiffres clés du rapport', 'Report key figures', 'Cifras clave del informe',
        'الأرقام الرئيسية للتقرير', '報告書の主要数値', '报告关键数据'),
    'audits.report.figures-total': ('Constats', 'Findings', 'Hallazgos', 'الملاحظات', '所見', '发现'),
    'audits.report.figures-score': (
        'Taux de conformité', 'Conformity rate', 'Tasa de conformidad',
        'نسبة المطابقة', '適合率', '符合率'),
    'audits.report.issued-line': (
        'Émis le {$INTERPOLATION} par {$INTERPOLATION_1}',
        'Issued on {$INTERPOLATION} by {$INTERPOLATION_1}',
        'Emitido el {$INTERPOLATION} por {$INTERPOLATION_1}',
        'صدر في {$INTERPOLATION} بواسطة {$INTERPOLATION_1}',
        '{$INTERPOLATION} に {$INTERPOLATION_1} が発行',
        '由 {$INTERPOLATION_1} 于 {$INTERPOLATION} 发布'),
    'audits.report.fingerprint-tooltip': (
        "Empreinte SHA-256 du contenu émis, inscrite au journal d'audit",
        'SHA-256 fingerprint of the issued content, recorded in the audit trail',
        'Huella SHA-256 del contenido emitido, registrada en el registro de auditoría',
        'بصمة SHA-256 للمحتوى الصادر، مسجّلة في سجل التدقيق',
        '発行内容の SHA-256 フィンガープリント（監査証跡に記録）',
        '已发布内容的 SHA-256 指纹，记录在审计日志中'),
    'audits.report.summary': (
        'Synthèse et conclusion rédigée', 'Written summary and conclusion',
        'Síntesis y conclusión redactada', 'الملخّص والخلاصة المكتوبة',
        '総括と結論（記述）', '书面总结与结论'),
    'audits.report.summary-placeholder': (
        "Contexte, périmètre couvert, appréciation d'ensemble du système…",
        'Context, scope covered, overall assessment of the system…',
        'Contexto, alcance cubierto, valoración global del sistema…',
        'السياق والنطاق المشمول والتقييم العام للنظام…',
        '背景、対象範囲、システム全体の評価…',
        '背景、覆盖范围、体系的总体评价…'),
    'audits.report.generate': (
        'Proposer par IA', 'Draft with AI', 'Proponer con IA',
        'اقتراح بالذكاء الاصطناعي', 'AI で下書き', 'AI 起草'),
    'audits.report.generated': (
        "Synthèse générée : relisez-la avant d'émettre.",
        'Summary generated: review it before issuing.',
        'Síntesis generada: revísela antes de emitir.',
        'تم إنشاء الملخّص: راجعه قبل الإصدار.',
        '総括を生成しました。発行前に確認してください。',
        '已生成总结：发布前请审阅。'),
    'audits.report.strengths': ('Points forts', 'Strengths', 'Puntos fuertes', 'نقاط القوة', '強み', '优势'),
    'audits.report.improvements': (
        "Axes d'amélioration", 'Areas for improvement', 'Áreas de mejora',
        'مجالات التحسين', '改善の余地', '改进方向'),
    'audits.report.conclusion': (
        'Conclusion / recommandation', 'Conclusion / recommendation', 'Conclusión / recomendación',
        'الخلاصة / التوصية', '結論／推奨', '结论 / 建议'),
    'audits.report.conclusion.favorable': (
        'Favorable — système conforme', 'Favorable — system conforms',
        'Favorable — sistema conforme', 'إيجابي — النظام مطابق',
        '良好 — システムは適合', '合格 — 体系符合'),
    'audits.report.conclusion.reservations': (
        'Favorable avec réserves', 'Favorable with reservations', 'Favorable con reservas',
        'إيجابي مع تحفّظات', '条件付きで良好', '有保留地合格'),
    'audits.report.conclusion.unfavorable': (
        'Défavorable — système non conforme', 'Unfavorable — system does not conform',
        'Desfavorable — sistema no conforme', 'سلبي — النظام غير مطابق',
        '不良 — システムは不適合', '不合格 — 体系不符合'),
    'audits.report.conclusion.cert-favorable': (
        'Certification recommandée', 'Certification recommended', 'Certificación recomendada',
        'يُوصى بالاعتماد', '認証を推奨', '建议认证'),
    'audits.report.conclusion.cert-reservations': (
        'Certification recommandée avec réserves', 'Certification recommended with reservations',
        'Certificación recomendada con reservas', 'يُوصى بالاعتماد مع تحفّظات',
        '条件付きで認証を推奨', '有保留地建议认证'),
    'audits.report.conclusion.cert-unfavorable': (
        'Certification non recommandée', 'Certification not recommended',
        'Certificación no recomendada', 'لا يُوصى بالاعتماد', '認証を推奨しない', '不建议认证'),
    'audits.report.save-draft': (
        'Enregistrer le brouillon', 'Save draft', 'Guardar borrador',
        'حفظ المسودة', '下書きを保存', '保存草稿'),
    'audits.report.issue': (
        'Émettre le rapport', 'Issue the report', 'Emitir el informe',
        'إصدار التقرير', '報告書を発行', '发布报告'),
    'audits.report.issue-confirm-title': (
        'Émettre le rapport ?', 'Issue the report?', '¿Emitir el informe?',
        'إصدار التقرير؟', '報告書を発行しますか？', '发布报告？'),
    'audits.report.issue-confirm-message': (
        "Le rapport sera figé — texte, conclusion et chiffres clés — et l'émission inscrite au journal d'audit. Il ne pourra plus être modifié, ni les documents de l'audit.",
        'The report will be locked — text, conclusion and key figures — and the issuance recorded in the audit trail. Neither it nor the audit documents can be changed afterwards.',
        'El informe quedará fijado — texto, conclusión y cifras clave — y la emisión se registrará en el registro de auditoría. Ya no se podrá modificar, ni tampoco los documentos de la auditoría.',
        'سيُجمَّد التقرير — النص والخلاصة والأرقام الرئيسية — ويُسجَّل الإصدار في سجل التدقيق. لن يمكن تعديله بعد ذلك، ولا مستندات التدقيق.',
        '報告書（本文・結論・主要数値）は確定され、発行は監査証跡に記録されます。以後、報告書も監査文書も変更できません。',
        '报告（文本、结论和关键数据）将被锁定，发布记录写入审计日志。此后报告及审核文件均不可修改。'),
    'audits.report.issued-msg': ('Rapport émis.', 'Report issued.', 'Informe emitido.',
                                 'صدر التقرير.', '報告書を発行しました。', '报告已发布。'),
    'audits.report.saved': (
        'Brouillon du rapport enregistré.', 'Report draft saved.', 'Borrador del informe guardado.',
        'حُفظت مسودة التقرير.', '報告書の下書きを保存しました。', '报告草稿已保存。'),
    'audits.report.save-error': (
        "Le brouillon n'a pas pu être enregistré.", 'The draft could not be saved.',
        'No se pudo guardar el borrador.', 'تعذّر حفظ المسودة.',
        '下書きを保存できませんでした。', '无法保存草稿。'),
    'audits.report.issue-error': (
        "Le rapport n'a pas pu être émis.", 'The report could not be issued.',
        'No se pudo emitir el informe.', 'تعذّر إصدار التقرير.',
        '報告書を発行できませんでした。', '无法发布报告。'),
    'audits.report.load-error': (
        "Le rapport n'a pas pu être chargé.", 'The report could not be loaded.',
        'No se pudo cargar el informe.', 'تعذّر تحميل التقرير.',
        '報告書を読み込めませんでした。', '无法加载报告。'),
    'audits.report.conflict': (
        "Le rapport ne peut plus être modifié dans l'état actuel de l'audit — rechargez pour voir son état.",
        'The report can no longer be changed in the current state of the audit — reload to see its state.',
        'El informe ya no se puede modificar en el estado actual de la auditoría: recargue para ver su estado.',
        'لم يعد بالإمكان تعديل التقرير في الحالة الحالية للتدقيق — أعد التحميل لمعرفة حالته.',
        '現在の監査状態では報告書を変更できません。再読み込みして状態を確認してください。',
        '在审核当前状态下报告已不可修改——请刷新查看其状态。'),
    'audits.report.incomplete': (
        'Le rapport est incomplet : synthèse et conclusion sont requises.',
        'The report is incomplete: a summary and a conclusion are required.',
        'El informe está incompleto: se requieren síntesis y conclusión.',
        'التقرير غير مكتمل: الملخّص والخلاصة مطلوبان.',
        '報告書が不完全です。総括と結論が必要です。',
        '报告不完整：需要总结和结论。'),
    'audits.report.step-up': (
        "L'émission du rapport exige votre code à usage unique.",
        'Issuing the report requires your one-time code.',
        'La emisión del informe requiere su código de un solo uso.',
        'يتطلّب إصدار التقرير رمزك لمرة واحدة.',
        '報告書の発行にはワンタイムコードが必要です。',
        '发布报告需要您的一次性验证码。'),
    'audits.report.blocker-cancelled': (
        "Un audit annulé n'a pas de rapport.", 'A cancelled audit has no report.',
        'Una auditoría cancelada no tiene informe.', 'لا تقرير للتدقيق الملغى.',
        '中止された監査には報告書がありません。', '已取消的审核没有报告。'),
    'audits.report.blocker-not-completed': (
        "Le rapport s'émet une fois l'audit clôturé. Vous pouvez déjà rédiger le brouillon.",
        'The report is issued once the audit is closed. You can already write the draft.',
        'El informe se emite una vez cerrada la auditoría. Ya puede redactar el borrador.',
        'يصدر التقرير بعد إغلاق التدقيق. يمكنك كتابة المسودة منذ الآن.',
        '報告書は監査終了後に発行します。下書きは今から作成できます。',
        '报告在审核关闭后发布。您现在即可撰写草稿。'),
    'audits.report.blocker-role': (
        "L'émission du rapport revient au pilotage qualité (manager ou directeur qualité).",
        'Issuing the report is up to quality management (quality manager or director).',
        'La emisión del informe corresponde a la dirección de calidad (responsable o director de calidad).',
        'إصدار التقرير من صلاحيات إدارة الجودة (مدير الجودة أو مديرها العام).',
        '報告書の発行は品質管理部門（品質マネージャーまたは品質部長）が行います。',
        '报告的发布由质量管理层（质量经理或质量总监）负责。'),
    'audits.report.blocker-summary': (
        "Rédigez la synthèse (ou générez-la) avant d'émettre.",
        'Write (or generate) the summary before issuing.',
        'Redacte (o genere) la síntesis antes de emitir.',
        'اكتب الملخّص (أو أنشئه) قبل الإصدار.',
        '発行前に総括を記述（または生成）してください。',
        '发布前请撰写（或生成）总结。'),
    'audits.report.blocker-conclusion': (
        "Choisissez la conclusion avant d'émettre.", 'Choose the conclusion before issuing.',
        'Elija la conclusión antes de emitir.', 'اختر الخلاصة قبل الإصدار.',
        '発行前に結論を選択してください。', '发布前请选择结论。'),

    # --- documents ---
    'audits.documents.title': (
        'Documents ({$INTERPOLATION})', 'Documents ({$INTERPOLATION})', 'Documentos ({$INTERPOLATION})',
        'المستندات ({$INTERPOLATION})', '文書（{$INTERPOLATION}）', '文件（{$INTERPOLATION}）'),
    'audits.documents.add': (
        'Ajouter un document', 'Add document', 'Añadir un documento',
        'إضافة مستند', '文書を追加', '添加文件'),
    'audits.documents.added': (
        "Document ajouté à l'audit.", 'Document added to the audit.',
        'Documento añadido a la auditoría.', 'أُضيف المستند إلى التدقيق.',
        '文書を監査に追加しました。', '文件已添加到审核。'),
    'audits.documents.empty': (
        "Aucun document. Joignez la convocation, le plan d'audit, des photos ou des relevés.",
        'No documents yet. Attach the notice, the audit plan, photos or records.',
        'Ningún documento. Adjunte la convocatoria, el plan de auditoría, fotos o registros.',
        'لا مستندات. أرفق الدعوة أو خطة التدقيق أو صورًا أو سجلات.',
        '文書はありません。通知、監査計画、写真、記録を添付してください。',
        '暂无文件。请附上通知、审核计划、照片或记录。'),
    'audits.documents.limits': (
        'PDF, image, Word, Excel ou PowerPoint — 10 Mo par fichier, 20 documents par audit.',
        'PDF, image, Word, Excel or PowerPoint — 10 MB per file, 20 documents per audit.',
        'PDF, imagen, Word, Excel o PowerPoint — 10 MB por archivo, 20 documentos por auditoría.',
        'PDF أو صورة أو Word أو Excel أو PowerPoint — ‏10 ميغابايت لكل ملف، و20 مستندًا لكل تدقيق.',
        'PDF、画像、Word、Excel、PowerPoint — 1 ファイル 10 MB、監査あたり 20 文書まで。',
        'PDF、图片、Word、Excel 或 PowerPoint — 每个文件 10 MB，每次审核最多 20 个文件。'),
    'audits.documents.too-large': (
        'Fichier trop lourd : 10 Mo au plus.', 'File too large: 10 MB at most.',
        'Archivo demasiado grande: 10 MB como máximo.', 'الملف كبير جدًا: 10 ميغابايت كحد أقصى.',
        'ファイルが大きすぎます（最大 10 MB）。', '文件过大：最多 10 MB。'),
    'audits.documents.unnamed': (
        'document sans nom', 'unnamed document', 'documento sin nombre',
        'مستند بلا اسم', '名前のない文書', '未命名文件'),
    'audits.documents.delete-title': (
        'Retirer ce document ?', 'Remove this document?', '¿Retirar este documento?',
        'إزالة هذا المستند؟', 'この文書を削除しますか？', '移除此文件？'),
    'audits.documents.delete-message': (
        "« {$name} » sera retiré de l'audit. Le retrait est inscrit au journal d'audit.",
        '“{$name}” will be removed from the audit. The removal is recorded in the audit trail.',
        '«{$name}» se retirará de la auditoría. La retirada se registra en el registro de auditoría.',
        'ستُزال «{$name}» من التدقيق. تُسجَّل الإزالة في سجل التدقيق.',
        '「{$name}」を監査から削除します。削除は監査証跡に記録されます。',
        '“{$name}”将从审核中移除。移除操作会记录在审计日志中。'),
    'audits.documents.delete': ('Retirer', 'Remove', 'Retirar', 'إزالة', '削除', '移除'),
    'audits.documents.delete-tooltip': (
        'Retirer le document', 'Remove the document', 'Retirar el documento',
        'إزالة المستند', '文書を削除', '移除文件'),
    'audits.documents.remove-aria': (
        'Retirer le document {$name}', 'Remove the document {$name}', 'Retirar el documento {$name}',
        'إزالة المستند {$name}', '文書 {$name} を削除', '移除文件 {$name}'),
    'audits.documents.storage-off': (
        'Le stockage des documents est désactivé sur cet environnement.',
        'Document storage is disabled on this environment.',
        'El almacenamiento de documentos está desactivado en este entorno.',
        'تخزين المستندات معطّل في هذه البيئة.',
        'この環境では文書ストレージが無効です。',
        '此环境已禁用文件存储。'),
    'audits.documents.load-error': (
        "Les documents n'ont pas pu être chargés.", 'The documents could not be loaded.',
        'No se pudieron cargar los documentos.', 'تعذّر تحميل المستندات.',
        '文書を読み込めませんでした。', '无法加载文件。'),
    'audits.documents.invalid': (
        'Fichier refusé : type non admis ou contenu qui ne correspond pas à son extension.',
        'File rejected: type not allowed or content that does not match its extension.',
        'Archivo rechazado: tipo no admitido o contenido que no corresponde a su extensión.',
        'رُفض الملف: نوع غير مسموح أو محتوى لا يطابق امتداده.',
        'ファイルが拒否されました：許可されていない形式、または拡張子と内容が一致しません。',
        '文件被拒绝：类型不允许或内容与扩展名不符。'),
    'audits.documents.limit': (
        'Limite atteinte : 10 Mo par fichier, 20 documents et 50 Mo par audit.',
        'Limit reached: 10 MB per file, 20 documents and 50 MB per audit.',
        'Límite alcanzado: 10 MB por archivo, 20 documentos y 50 MB por auditoría.',
        'تم بلوغ الحد: 10 ميغابايت لكل ملف، و20 مستندًا و50 ميغابايت لكل تدقيق.',
        '上限に達しました：1 ファイル 10 MB、監査あたり 20 文書・50 MB まで。',
        '已达上限：每个文件 10 MB，每次审核 20 个文件、50 MB。'),
    'audits.documents.locked': (
        'Les documents de cet audit sont verrouillés (audit annulé ou rapport émis).',
        "This audit's documents are locked (audit cancelled or report issued).",
        'Los documentos de esta auditoría están bloqueados (auditoría cancelada o informe emitido).',
        'مستندات هذا التدقيق مقفلة (تدقيق ملغى أو تقرير صادر).',
        'この監査の文書はロックされています（監査中止または報告書発行済み）。',
        '此审核的文件已锁定（审核已取消或报告已发布）。'),
    'audits.documents.error': (
        "L'opération sur le document a échoué.", 'The document operation failed.',
        'La operación sobre el documento ha fallado.', 'فشلت العملية على المستند.',
        '文書の操作に失敗しました。', '文件操作失败。'),
}
