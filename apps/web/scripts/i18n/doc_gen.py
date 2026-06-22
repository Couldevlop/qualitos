# -*- coding: utf-8 -*-
"""Table i18n - domaine standards-doc-gen (génération documentaire IA + revue). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.doc-gen-ia': ('Génération doc IA', 'AI doc generation', 'Generación de docs IA', 'إنشاء الوثائق بالذكاء الاصطناعي', 'AI 文書生成', 'AI 文档生成'),

    # ===== Génération (doc-gen) =====
    'docgen.title': ('Génération documentaire IA', 'AI document generation', 'Generación documental IA', 'إنشاء الوثائق بالذكاء الاصطناعي', 'AI 文書生成', 'AI 文档生成'),
    'docgen.subtitle': ("Générez en lot un dossier documentaire complet (Manuel Qualité, Politique, procédures) pré-rempli par l'IA à partir du contexte de votre organisation. Chaque pièce passe par une validation humaine avant signature et ancrage.", 'Generate a complete documentation set (Quality Manual, Policy, procedures) in batch, pre-filled by AI from your organization context. Each item goes through human validation before signing and anchoring.', 'Genere por lotes un expediente documental completo (Manual de Calidad, Política, procedimientos) precompletado por la IA a partir del contexto de su organización. Cada pieza pasa por una validación humana antes de la firma y el anclaje.', 'أنشئ دفعةً ملفّاً وثائقياً كاملاً (دليل الجودة، السياسة، الإجراءات) مملوءاً مسبقاً بالذكاء الاصطناعي من سياق مؤسستك. تمرّ كل وثيقة بتحقّق بشري قبل التوقيع والتثبيت.', '組織のコンテキストから AI が事前入力した完全な文書一式（品質マニュアル、方針、手順書）を一括生成します。各文書は署名と記録の前に人による検証を経ます。', '从您组织的上下文出发，由 AI 预填批量生成完整文档集（质量手册、方针、程序）。每份文档在签名和锚定前都需经过人工验证。'),

    # Config de génération
    'docgen.config.title': ('Nouvelle génération', 'New generation', 'Nueva generación', 'إنشاء جديد', '新規生成', '新建生成'),
    'docgen.config.standard': ('Norme visée', 'Target standard', 'Norma objetivo', 'المعيار المستهدف', '対象規格', '目标标准'),
    'docgen.config.loading-standards': ('Chargement du catalogue…', 'Loading catalog…', 'Cargando el catálogo…', 'جارٍ تحميل الكتالوج…', 'カタログを読み込み中…', '正在加载目录…'),
    'docgen.config.org': ('Organisation', 'Organization', 'Organización', 'المؤسسة', '組織', '组织'),
    'docgen.config.industry': ('Secteur', 'Industry', 'Sector', 'القطاع', '業種', '行业'),
    'docgen.config.size': ('Taille', 'Size', 'Tamaño', 'الحجم', '規模', '规模'),
    'docgen.config.language': ('Langue de rédaction', 'Drafting language', 'Idioma de redacción', 'لغة التحرير', '作成言語', '撰写语言'),
    'docgen.config.start': ('Générer le dossier', 'Generate the set', 'Generar el expediente', 'إنشاء الملف', '文書一式を生成', '生成文档集'),

    # Langues
    'docgen.lang.fr': ('Français', 'French', 'Francés', 'الفرنسية', 'フランス語', '法语'),
    'docgen.lang.en': ('Anglais', 'English', 'Inglés', 'الإنجليزية', '英語', '英语'),
    'docgen.lang.es': ('Espagnol', 'Spanish', 'Español', 'الإسبانية', 'スペイン語', '西班牙语'),
    'docgen.lang.de': ('Allemand', 'German', 'Alemán', 'الألمانية', 'ドイツ語', '德语'),

    # Pièces
    'docgen.pieces.title': ('Pièces à générer', 'Items to generate', 'Piezas a generar', 'الوثائق المطلوب إنشاؤها', '生成する文書', '待生成文档'),
    'docgen.pieces.sections': ('{$INTERPOLATION} section(s)', '{$INTERPOLATION} section(s)', '{$INTERPOLATION} sección(es)', '{$INTERPOLATION} قسم/أقسام', '{$INTERPOLATION} セクション', '{$INTERPOLATION} 个章节'),
    'docgen.selection.all': ('Plan complet (toutes les pièces)', 'Full plan (all items)', 'Plan completo (todas las piezas)', 'الخطة الكاملة (جميع الوثائق)', '全体プラン（すべての文書）', '完整方案（所有文档）'),
    'docgen.selection.count': ('{$count} pièce(s) sélectionnée(s)', '{$count} item(s) selected', '{$count} pieza(s) seleccionada(s)', 'تم تحديد {$count} وثيقة', '{$count} 件の文書を選択', '已选择 {$count} 份文档'),

    # Dossier en cours
    'docgen.active.title': ('Dossier en cours', 'Set in progress', 'Expediente en curso', 'الملف قيد الإنجاز', '進行中の文書一式', '进行中的文档集'),
    'docgen.progress': ('{$INTERPOLATION} / {$INTERPOLATION_1} générées', '{$INTERPOLATION} / {$INTERPOLATION_1} generated', '{$INTERPOLATION} / {$INTERPOLATION_1} generadas', 'تم إنشاء {$INTERPOLATION} / {$INTERPOLATION_1}', '{$INTERPOLATION} / {$INTERPOLATION_1} 件生成済み', '已生成 {$INTERPOLATION} / {$INTERPOLATION_1}'),
    'docgen.doc.reuse': ('réutilisation possible', 'reuse possible', 'reutilización posible', 'إمكانية إعادة الاستخدام', '再利用可能', '可复用'),
    'docgen.doc.review': ('Réviser', 'Review', 'Revisar', 'مراجعة', 'レビュー', '审阅'),
    'docgen.action.retry': ('Relancer les pièces en échec', 'Retry failed items', 'Reintentar las piezas fallidas', 'إعادة محاولة الوثائق الفاشلة', '失敗した文書を再試行', '重试失败的文档'),

    # Finalisation
    'docgen.sealed.title': ('Dossier finalisé, signé et ancré', 'Set finalized, signed and anchored', 'Expediente finalizado, firmado y anclado', 'الملف منجَز وموقَّع ومثبَّت', '文書一式は確定・署名・記録済み', '文档集已完成、签名并锚定'),
    'docgen.sealed.anchor': ('Ancrage', 'Anchor', 'Anclaje', 'التثبيت', '記録', '锚定'),
    'docgen.finalize.note': ('Toutes les pièces doivent être approuvées (validation humaine) avant la finalisation.', 'All items must be approved (human validation) before finalization.', 'Todas las piezas deben estar aprobadas (validación humana) antes de la finalización.', 'يجب اعتماد جميع الوثائق (تحقّق بشري) قبل الإنهاء.', '確定前にすべての文書が承認（人による検証）されている必要があります。', '所有文档在最终确定前必须经过批准（人工验证）。'),
    'docgen.finalize.signature': ('Signature du responsable', 'Responsible signature', 'Firma del responsable', 'توقيع المسؤول', '責任者の署名', '负责人签名'),
    'docgen.finalize.notes': ('Notes (facultatif)', 'Notes (optional)', 'Notas (opcional)', 'ملاحظات (اختياري)', 'メモ（任意）', '备注（可选）'),
    'docgen.finalize.note': ('Toutes les pièces doivent être approuvées (validation humaine) avant la finalisation.', 'All items must be approved (human validation) before finalization.', 'Todas las piezas deben estar aprobadas (validación humana) antes de la finalización.', 'يجب اعتماد جميع الوثائق (تحقّق بشري) قبل الإنهاء.', '確定前にすべての文書が承認（人による検証）されている必要があります。', '所有文档在最终确定前必须经过批准（人工验证）。'),
    'docgen.finalize.submit': ('Finaliser, signer et ancrer', 'Finalize, sign and anchor', 'Finalizar, firmar y anclar', 'الإنهاء والتوقيع والتثبيت', '確定・署名・記録', '完成、签名并锚定'),

    # Historique
    'docgen.history.title': ('Mes dossiers documentaires', 'My documentation sets', 'Mis expedientes documentales', 'ملفّاتي الوثائقية', '私の文書一式', '我的文档集'),
    'docgen.history.empty': ("Aucun dossier généré pour l'instant.", 'No set generated yet.', 'Ningún expediente generado por ahora.', 'لم يُنشَأ أي ملف بعد.', 'まだ生成された文書一式はありません。', '目前尚未生成任何文档集。'),
    'docgen.col.standard': ('Norme', 'Standard', 'Norma', 'المعيار', '規格', '标准'),
    'docgen.col.org': ('Organisation', 'Organization', 'Organización', 'المؤسسة', '組織', '组织'),
    'docgen.col.progress': ('Progression', 'Progress', 'Progreso', 'التقدّم', '進捗', '进度'),
    'docgen.col.status': ('Statut', 'Status', 'Estado', 'الحالة', 'ステータス', '状态'),

    # Type de pièce
    'docgen.kind.manual': ('Manuel', 'Manual', 'Manual', 'الدليل', 'マニュアル', '手册'),
    'docgen.kind.policy': ('Politique', 'Policy', 'Política', 'السياسة', '方針', '方针'),
    'docgen.kind.procedure': ('Procédure', 'Procedure', 'Procedimiento', 'الإجراء', '手順書', '程序'),

    # Statut d'une pièce
    'docgen.docstatus.pending': ('En attente', 'Pending', 'Pendiente', 'قيد الانتظار', '保留中', '待处理'),
    'docgen.docstatus.generating': ('Génération…', 'Generating…', 'Generando…', 'جارٍ الإنشاء…', '生成中…', '正在生成…'),
    'docgen.docstatus.generated': ('Brouillon généré', 'Draft generated', 'Borrador generado', 'تم إنشاء المسوّدة', '下書きを生成', '已生成草稿'),
    'docgen.docstatus.failed': ('Échec', 'Failed', 'Fallido', 'فشل', '失敗', '失败'),
    'docgen.docstatus.reused': ('Réutilisable', 'Reusable', 'Reutilizable', 'قابل لإعادة الاستخدام', '再利用可能', '可复用'),

    # Statut du dossier
    'docgen.status.inprogress': ('Génération en cours', 'Generation in progress', 'Generación en curso', 'الإنشاء قيد التنفيذ', '生成中', '生成中'),
    'docgen.status.generated': ('Généré — à valider', 'Generated — to validate', 'Generado — por validar', 'تم الإنشاء — بانتظار التحقّق', '生成済み — 要検証', '已生成 — 待验证'),
    'docgen.status.finalized': ('Finalisé', 'Finalized', 'Finalizado', 'منجَز', '確定済み', '已完成'),

    # Toasts
    'docgen.toast.generated': ('Dossier généré : {$n} pièce(s) en brouillon.', 'Set generated: {$n} draft item(s).', 'Expediente generado: {$n} pieza(s) en borrador.', 'تم إنشاء الملف: {$n} وثيقة كمسوّدة.', '文書一式を生成：下書き {$n} 件。', '已生成文档集：{$n} 份草稿。'),
    'docgen.toast.finalized': ('Dossier finalisé, signé et ancré.', 'Set finalized, signed and anchored.', 'Expediente finalizado, firmado y anclado.', 'تم إنجاز الملف وتوقيعه وتثبيته.', '文書一式を確定・署名・記録しました。', '文档集已完成、签名并锚定。'),
    'docgen.toast.close': ('Fermer', 'Close', 'Cerrar', 'إغلاق', '閉じる', '关闭'),

    # Erreurs
    'docgen.err.backend': ('Backend injoignable (engine sur 8082 ?).', 'Backend unreachable (engine on 8082?).', 'Backend inaccesible (¿engine en 8082?).', 'تعذّر الوصول إلى الخلفية (المحرّك على 8082؟).', 'バックエンドに到達できません（engine は 8082 ？）。', '无法连接后端（engine 在 8082 ？）。'),
    'docgen.err.invalid': ('Requête invalide (norme + organisation requises).', 'Invalid request (standard + organization required).', 'Solicitud no válida (se requieren norma + organización).', 'طلب غير صالح (المعيار والمؤسسة مطلوبان).', '無効なリクエスト（規格と組織が必須）。', '请求无效（需要标准和组织）。'),
    'docgen.err.forbidden': ('Droits insuffisants pour cette action.', 'Insufficient rights for this action.', 'Permisos insuficientes para esta acción.', 'صلاحيات غير كافية لهذا الإجراء.', 'この操作の権限が不足しています。', '权限不足，无法执行此操作。'),
    'docgen.err.notfound': ('Norme ou dossier introuvable.', 'Standard or set not found.', 'Norma o expediente no encontrado.', 'لم يُعثَر على المعيار أو الملف.', '規格または文書一式が見つかりません。', '未找到标准或文档集。'),
    'docgen.err.conflict': ("Action impossible dans l'état actuel du dossier.", 'Action not possible in the current set state.', 'Acción imposible en el estado actual del expediente.', 'الإجراء غير ممكن في الحالة الراهنة للملف.', '現在の文書一式の状態ではこの操作はできません。', '在文档集当前状态下无法执行此操作。'),
    'docgen.err.unprocessable': ('Génération IA impossible pour cette requête.', 'AI generation not possible for this request.', 'Generación IA imposible para esta solicitud.', 'تعذّر الإنشاء بالذكاء الاصطناعي لهذا الطلب.', 'このリクエストでは AI 生成ができません。', '此请求无法进行 AI 生成。'),
    'docgen.err.quota': ('Débit/quota IA dépassé pour ce tenant — réessayez plus tard.', 'AI rate/quota exceeded for this tenant — try again later.', 'Tasa/cuota IA superada para este tenant — inténtelo más tarde.', 'تم تجاوز معدّل/حصة الذكاء الاصطناعي لهذا المستأجر — أعد المحاولة لاحقاً.', 'このテナントの AI レート／クォータを超過しました — 後でやり直してください。', '该租户的 AI 速率/配额已超出 — 请稍后重试。'),
    'docgen.err.unavailable': ('Service IA momentanément indisponible.', 'AI service temporarily unavailable.', 'Servicio IA temporalmente no disponible.', 'خدمة الذكاء الاصطناعي غير متاحة مؤقتاً.', 'AI サービスは一時的に利用できません。', 'AI 服务暂时不可用。'),
    'docgen.err.generic': ("Échec de l'opération (HTTP {$status}).", 'Operation failed (HTTP {$status}).', 'Error en la operación (HTTP {$status}).', 'فشلت العملية (HTTP {$status}).', '操作に失敗しました（HTTP {$status}）。', '操作失败（HTTP {$status}）。'),

    # ===== Revue (doc-review) =====
    'docreview.title': ("Revue d'un document généré", 'Review of a generated document', 'Revisión de un documento generado', 'مراجعة وثيقة مُنشأة', '生成文書のレビュー', '生成文档的审阅'),
    'docreview.subtitle': ("Relisez le brouillon rédigé par l'IA, soumettez-le à validation, puis approuvez-le (signature humaine) ou rejetez-le. Aucune publication sans validation humaine.", 'Review the draft written by the AI, submit it for validation, then approve it (human signature) or reject it. No publication without human validation.', 'Relea el borrador redactado por la IA, envíelo a validación y luego apruébelo (firma humana) o recházelo. Ninguna publicación sin validación humana.', 'راجع المسوّدة التي كتبها الذكاء الاصطناعي، أرسلها للتحقّق، ثم اعتمدها (توقيع بشري) أو ارفضها. لا نشر دون تحقّق بشري.', 'AI が作成した下書きを確認し、検証に提出してから承認（人による署名）または却下します。人による検証なしに公開されることはありません。', '审阅 AI 撰写的草稿，提交验证，然后批准（人工签名）或拒绝。未经人工验证不予发布。'),
    'docreview.lastreject': ('Dernier rejet :', 'Last rejection:', 'Último rechazo:', 'آخر رفض:', '最後の却下：', '最近一次拒绝：'),
    'docreview.clauses': ('Clauses', 'Clauses', 'Cláusulas', 'البنود', '条項', '条款'),
    'docreview.action.submit': ('Soumettre à validation', 'Submit for validation', 'Enviar a validación', 'إرسال للتحقّق', '検証に提出', '提交验证'),
    'docreview.approve.note': ("L'approbation appose une signature humaine. L'approbateur doit différer du soumetteur.", 'Approval affixes a human signature. The approver must differ from the submitter.', 'La aprobación aplica una firma humana. El aprobador debe ser distinto del remitente.', 'يضع الاعتماد توقيعاً بشرياً. يجب أن يختلف المُعتمِد عن المُقدِّم.', '承認は人による署名を付与します。承認者は提出者と異なる必要があります。', '批准会附加人工签名。批准人必须与提交人不同。'),
    'docreview.approve.signature': ('Signature', 'Signature', 'Firma', 'التوقيع', '署名', '签名'),
    'docreview.approve.notes': ('Notes (facultatif)', 'Notes (optional)', 'Notas (opcional)', 'ملاحظات (اختياري)', 'メモ（任意）', '备注（可选）'),
    'docreview.action.approve': ('Approuver & signer', 'Approve & sign', 'Aprobar y firmar', 'الاعتماد والتوقيع', '承認して署名', '批准并签名'),
    'docreview.action.reject-toggle': ('Rejeter', 'Reject', 'Rechazar', 'رفض', '却下', '拒绝'),
    'docreview.reject.reason': ('Motif du rejet', 'Rejection reason', 'Motivo del rechazo', 'سبب الرفض', '却下理由', '拒绝原因'),
    'docreview.action.reject': ('Confirmer le rejet', 'Confirm rejection', 'Confirmar el rechazo', 'تأكيد الرفض', '却下を確定', '确认拒绝'),
    'docreview.approved': ('Pièce approuvée et signée — prête pour la finalisation du dossier.', 'Item approved and signed — ready for set finalization.', 'Pieza aprobada y firmada — lista para la finalización del expediente.', 'تم اعتماد الوثيقة وتوقيعها — جاهزة لإنهاء الملف.', '文書は承認・署名済み — 文書一式の確定準備完了。', '文档已批准并签名 — 可进行文档集最终确定。'),

    # Type de pièce (revue)
    'docreview.kind.manual': ('Manuel Qualité', 'Quality Manual', 'Manual de Calidad', 'دليل الجودة', '品質マニュアル', '质量手册'),
    'docreview.kind.policy': ('Politique Qualité', 'Quality Policy', 'Política de Calidad', 'سياسة الجودة', '品質方針', '质量方针'),
    'docreview.kind.procedure': ('Procédure documentée', 'Documented procedure', 'Procedimiento documentado', 'إجراء موثَّق', '文書化された手順', '文件化程序'),

    # Statut (revue)
    'docreview.status.draft': ('Brouillon IA', 'AI draft', 'Borrador IA', 'مسوّدة الذكاء الاصطناعي', 'AI 下書き', 'AI 草稿'),
    'docreview.status.review': ('En validation', 'In validation', 'En validación', 'قيد التحقّق', '検証中', '验证中'),
    'docreview.status.approved': ('Approuvé', 'Approved', 'Aprobado', 'معتمَد', '承認済み', '已批准'),
    'docreview.status.rejected': ('Rejeté', 'Rejected', 'Rechazado', 'مرفوض', '却下', '已拒绝'),

    # Toasts (revue)
    'docreview.toast.submitted': ('Pièce soumise à validation.', 'Item submitted for validation.', 'Pieza enviada a validación.', 'تم إرسال الوثيقة للتحقّق.', '文書を検証に提出しました。', '文档已提交验证。'),
    'docreview.toast.approved': ('Pièce approuvée et signée.', 'Item approved and signed.', 'Pieza aprobada y firmada.', 'تم اعتماد الوثيقة وتوقيعها.', '文書を承認・署名しました。', '文档已批准并签名。'),
    'docreview.toast.rejected': ('Pièce rejetée — revenue en brouillon.', 'Item rejected — back to draft.', 'Pieza rechazada — vuelta a borrador.', 'تم رفض الوثيقة — أُعيدت إلى المسوّدة.', '文書を却下 — 下書きに戻しました。', '文档已拒绝 — 已退回草稿。'),
    'docreview.toast.close': ('Fermer', 'Close', 'Cerrar', 'إغلاق', '閉じる', '关闭'),

    # Erreurs (revue)
    'docreview.err.backend': ('Backend injoignable (engine sur 8082 ?).', 'Backend unreachable (engine on 8082?).', 'Backend inaccesible (¿engine en 8082?).', 'تعذّر الوصول إلى الخلفية (المحرّك على 8082؟).', 'バックエンドに到達できません（engine は 8082 ？）。', '无法连接后端（engine 在 8082 ？）。'),
    'docreview.err.invalid': ('Requête invalide (signature/motif requis).', 'Invalid request (signature/reason required).', 'Solicitud no válida (se requiere firma/motivo).', 'طلب غير صالح (التوقيع/السبب مطلوب).', '無効なリクエスト（署名／理由が必須）。', '请求无效（需要签名/原因）。'),
    'docreview.err.forbidden': ('Droits insuffisants (approbation réservée au Directeur Qualité).', 'Insufficient rights (approval reserved for the Quality Director).', 'Permisos insuficientes (aprobación reservada al Director de Calidad).', 'صلاحيات غير كافية (الاعتماد محصور بمدير الجودة).', '権限が不足しています（承認は品質責任者に限定）。', '权限不足（批准仅限质量总监）。'),
    'docreview.err.notfound': ('Pièce introuvable.', 'Item not found.', 'Pieza no encontrada.', 'لم يُعثَر على الوثيقة.', '文書が見つかりません。', '未找到文档。'),
    'docreview.err.conflict': ('Transition impossible (séparation des tâches, état invalide).', 'Transition not possible (separation of duties, invalid state).', 'Transición imposible (separación de funciones, estado no válido).', 'الانتقال غير ممكن (فصل المهام، حالة غير صالحة).', '遷移できません（職務分掌、無効な状態）。', '无法转换（职责分离，状态无效）。'),
    'docreview.err.generic': ("Échec de l'opération (HTTP {$status}).", 'Operation failed (HTTP {$status}).', 'Error en la operación (HTTP {$status}).', 'فشلت العملية (HTTP {$status}).', '操作に失敗しました（HTTP {$status}）。', '操作失败（HTTP {$status}）。'),
}
