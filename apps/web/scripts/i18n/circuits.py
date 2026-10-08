# -*- coding: utf-8 -*-
"""Libellés des circuits de validation (ADR 0080) : le réglage du circuit par
l'administrateur, l'avancement d'une version de document, le refus motivé.

Placeholders : `${x}:nom:` côté TS s'écrit {$nom} ; `{{ x // i18n(ph="nom") }}`
côté gabarit s'écrit {$NOM}.
"""

TRANSLATIONS = {
    'nav.admin-circuits': (
        'Circuits de validation', 'Approval workflows', 'Circuitos de validación', 'مسارات الاعتماد',
        '承認ワークフロー', '审批流程'),

    # ---------- réglage du circuit ----------
    'circuits.title': (
        'Circuits de validation', 'Approval workflows', 'Circuitos de validación', 'مسارات الاعتماد',
        '承認ワークフロー', '审批流程'),
    'circuits.subtitle': (
        'Qui approuve, et dans quel ordre. Glissez un rôle sur le parcours pour ajouter une étape ; glissez une étape pour la déplacer.',
        'Who approves, and in what order. Drag a role onto the path to add a step; drag a step to move it.',
        'Quién aprueba y en qué orden. Arrastre un rol al recorrido para añadir una etapa; arrastre una etapa para moverla.',
        'من يعتمد وبأي ترتيب. اسحب دورًا إلى المسار لإضافة مرحلة، واسحب مرحلة لنقلها.',
        '誰がどの順序で承認するか。役割をパスにドラッグしてステップを追加し、ステップをドラッグして移動します。',
        '由谁审批、按什么顺序。将角色拖到流程上即可添加步骤；拖动步骤可调整位置。'),
    'circuits.load-failed': (
        "Le circuit n'a pas pu être chargé.", 'The workflow could not be loaded.',
        'No se pudo cargar el circuito.', 'تعذّر تحميل المسار.', 'ワークフローを読み込めませんでした。', '无法加载审批流程。'),
    'circuits.palette': (
        'Rôles qui approuvent', 'Roles that approve', 'Roles que aprueban', 'الأدوار التي تعتمد',
        '承認できる役割', '可审批的角色'),
    'circuits.palette-help': (
        'Glissez un rôle sur le parcours, ou utilisez son bouton +.',
        'Drag a role onto the path, or use its + button.',
        'Arrastre un rol al recorrido o use su botón +.',
        'اسحب دورًا إلى المسار أو استخدم زر + الخاص به.',
        '役割をパスにドラッグするか、＋ボタンを使います。',
        '将角色拖到流程上，或使用其 + 按钮。'),
    'circuits.add-aria': (
        'Ajouter une étape tenue par {$role}', 'Add a step held by {$role}', 'Añadir una etapa a cargo de {$role}',
        'إضافة مرحلة يتولاها {$role}', '{$role} が担当するステップを追加', '添加由 {$role} 负责的步骤'),
    'circuits.no-approver': (
        "Aucun rôle n'a le droit d'approuver un document.", 'No role is allowed to approve a document.',
        'Ningún rol tiene derecho a aprobar un documento.', 'لا يملك أي دور صلاحية اعتماد مستند.',
        '文書を承認できる役割がありません。', '没有角色有权审批文档。'),
    'circuits.others': (
        "Sans le droit d'approuver", 'Without the right to approve', 'Sin derecho a aprobar', 'بدون صلاحية الاعتماد',
        '承認権限なし', '无审批权限'),
    'circuits.grant-link': (
        'Leur donner ce droit dans « Rôles et droits »', 'Grant them this right in “Roles and rights”',
        'Concederles este derecho en «Roles y derechos»', 'امنحهم هذه الصلاحية في «الأدوار والصلاحيات»',
        '「役割と権限」でこの権限を付与', '在“角色与权限”中授予此权限'),
    'circuits.subject.document-version': (
        'Versions de document', 'Document versions', 'Versiones de documento', 'إصدارات المستندات',
        '文書のバージョン', '文档版本'),
    'circuits.start': (
        'Soumise à revue', 'Submitted for review', 'Enviada a revisión', 'مُقدَّم للمراجعة', 'レビューに提出', '已提交审核'),
    'circuits.start-help': (
        "par son auteur, qui ne l'approuvera jamais lui-même",
        'by its author, who will never approve it themselves',
        'por su autor, que nunca la aprobará personalmente',
        'من قِبل مؤلفه، الذي لن يعتمده بنفسه أبدًا',
        '作成者による提出（作成者自身は承認できません）',
        '由作者提交，作者本人永远不能审批'),
    'circuits.steps': ('Étapes du circuit', 'Workflow steps', 'Etapas del circuito', 'مراحل المسار', 'ワークフローのステップ', '流程步骤'),
    'circuits.step-name': ("Nom de l'étape", 'Step name', 'Nombre de la etapa', 'اسم المرحلة', 'ステップ名', '步骤名称'),
    'circuits.step-name-required': (
        "Donnez un nom à l'étape.", 'Give the step a name.', 'Dé un nombre a la etapa.', 'أعطِ المرحلة اسمًا.',
        'ステップに名前を付けてください。', '请为步骤命名。'),
    'circuits.step-role': ('Qui approuve', 'Who approves', 'Quién aprueba', 'من يعتمد', '承認者', '审批人'),
    'circuits.fewer': (
        'Une approbation de moins', 'One approval fewer', 'Una aprobación menos', 'اعتماد واحد أقل',
        '承認を1件減らす', '减少一次审批'),
    'circuits.more': (
        'Une approbation de plus', 'One approval more', 'Una aprobación más', 'اعتماد واحد إضافي',
        '承認を1件増やす', '增加一次审批'),
    'circuits.approvals-needed': (
        'approbation(s) distincte(s)', 'distinct approval(s)', 'aprobación(es) distinta(s)', 'اعتماد (اعتمادات) منفصلة',
        '件の個別承認', '次独立审批'),
    'circuits.orphan': (
        "Ce rôle n'a plus le droit d'approuver : choisissez-en un autre.",
        'This role can no longer approve: choose another one.',
        'Este rol ya no puede aprobar: elija otro.',
        'لم يعد لهذا الدور صلاحية الاعتماد: اختر دورًا آخر.',
        'この役割は承認できなくなりました。別の役割を選んでください。',
        '该角色已无审批权限：请另选一个。'),
    'circuits.up': ("Monter l'étape", 'Move step up', 'Subir la etapa', 'نقل المرحلة لأعلى', 'ステップを上へ', '上移步骤'),
    'circuits.down': ("Descendre l'étape", 'Move step down', 'Bajar la etapa', 'نقل المرحلة لأسفل', 'ステップを下へ', '下移步骤'),
    'circuits.remove': ("Retirer l'étape", 'Remove step', 'Quitar la etapa', 'إزالة المرحلة', 'ステップを削除', '移除步骤'),
    'circuits.empty': (
        'Aucun circuit : une seule approbation suffit. Déposez un rôle ici pour créer la première étape.',
        'No workflow: a single approval is enough. Drop a role here to create the first step.',
        'Sin circuito: basta una sola aprobación. Suelte un rol aquí para crear la primera etapa.',
        'لا يوجد مسار: يكفي اعتماد واحد. أفلِت دورًا هنا لإنشاء المرحلة الأولى.',
        'ワークフローなし：承認は1件で足ります。ここに役割をドロップして最初のステップを作成します。',
        '暂无流程：一次审批即可。将角色拖放到此处以创建第一个步骤。'),
    'circuits.full': (
        'Un circuit compte au plus {$MAX} étapes.', 'A workflow has at most {$MAX} steps.',
        'Un circuito tiene como máximo {$MAX} etapas.', 'يضم المسار {$MAX} مراحل على الأكثر.',
        'ワークフローのステップは最大 {$MAX} 件です。', '一个流程最多 {$MAX} 个步骤。'),
    'circuits.end': ('Approuvée', 'Approved', 'Aprobada', 'معتمد', '承認済み', '已审批'),
    'circuits.end-help': (
        "prête à publier. Un refus, à n'importe quelle étape, la renvoie en brouillon avec sa raison.",
        'ready to publish. A rejection at any step sends it back to draft with its reason.',
        'lista para publicar. Un rechazo en cualquier etapa la devuelve a borrador con su motivo.',
        'جاهز للنشر. أي رفض في أي مرحلة يعيده إلى المسودة مع سببه.',
        '公開準備完了。どのステップで却下されても、理由とともに下書きに戻ります。',
        '可发布。任何步骤被驳回，都会连同理由退回草稿。'),
    'circuits.note': (
        "Une même personne ne décide qu'une fois par circuit. Les versions déjà soumises gardent le circuit de leur soumission.",
        'The same person decides only once per workflow. Versions already submitted keep the workflow of their submission.',
        'Una misma persona decide solo una vez por circuito. Las versiones ya enviadas conservan el circuito de su envío.',
        'يقرر الشخص الواحد مرة واحدة فقط في كل مسار. تحتفظ الإصدارات المقدَّمة بالمسار الساري عند تقديمها.',
        '同じ人が判断できるのはワークフローごとに1回だけです。提出済みのバージョンは提出時のワークフローを保持します。',
        '同一人在每个流程中只能决定一次。已提交的版本沿用提交时的流程。'),
    'circuits.pending': ('Circuit modifié', 'Workflow changed', 'Circuito modificado', 'تم تعديل المسار', 'ワークフローが変更されました', '流程已修改'),
    'circuits.pending-invalid': (
        'Chaque étape doit porter un nom', 'Every step needs a name', 'Cada etapa necesita un nombre',
        'يجب أن تحمل كل مرحلة اسمًا', 'すべてのステップに名前が必要です', '每个步骤都必须命名'),
    'circuits.saved': (
        'Circuit enregistré. Les prochaines soumissions le suivront.',
        'Workflow saved. Upcoming submissions will follow it.',
        'Circuito guardado. Los próximos envíos lo seguirán.',
        'تم حفظ المسار. ستتبعه عمليات التقديم القادمة.',
        'ワークフローを保存しました。今後の提出はこれに従います。',
        '流程已保存。之后的提交将按此流程进行。'),
    'circuits.cleared': (
        'Circuit retiré : une seule approbation suffit de nouveau.',
        'Workflow removed: a single approval is enough again.',
        'Circuito retirado: vuelve a bastar una sola aprobación.',
        'تمت إزالة المسار: يكفي اعتماد واحد من جديد.',
        'ワークフローを削除しました。再び承認1件で足ります。',
        '流程已移除：重新恢复为一次审批即可。'),
    'circuits.failed': (
        "L'opération sur le circuit a échoué.", 'The workflow operation failed.', 'La operación sobre el circuito ha fallado.',
        'فشلت العملية على المسار.', 'ワークフローの操作に失敗しました。', '流程操作失败。'),
    'circuits.summary.none': (
        "Aujourd'hui, une seule approbation suffit, par toute personne qui a le droit d'approuver.",
        'Today, a single approval is enough, by anyone allowed to approve.',
        'Hoy basta una sola aprobación, de cualquier persona con derecho a aprobar.',
        'حاليًا يكفي اعتماد واحد من أي شخص يملك صلاحية الاعتماد.',
        '現在は、承認権限を持つ人による承認1件で足ります。',
        '目前，任何有审批权限的人审批一次即可。'),
    'circuits.summary.lead': (
        'Une version soumise est approuvée par {$steps}.', 'A submitted version is approved by {$steps}.',
        'Una versión enviada es aprobada por {$steps}.', 'يُعتمد الإصدار المقدَّم من قِبل {$steps}.',
        '提出されたバージョンは {$steps} によって承認されます。', '提交的版本由 {$steps} 审批。'),
    'circuits.summary.then': (', puis ', ', then ', ', luego ', '، ثم ', '、次に', '，然后 '),
    'circuits.summary.one': (
        'une personne « {$role} »', 'one “{$role}”', 'una persona «{$role}»', 'شخص واحد «{$role}»',
        '「{$role}」1名', '一名“{$role}”'),
    'circuits.summary.many': (
        '{$count} personnes « {$role} »', '{$count} “{$role}”', '{$count} personas «{$role}»',
        '{$count} أشخاص «{$role}»', '「{$role}」{$count}名', '{$count} 名“{$role}”'),

    # ---------- fiche document : avancement et refus ----------
    'documents.detail.reject-tooltip': ('Refuser', 'Reject', 'Rechazar', 'رفض', '却下', '驳回'),
    'documents.detail.step-approved': (
        'Votre approbation est enregistrée : la validation continue.',
        'Your approval is recorded: the review continues.',
        'Su aprobación ha quedado registrada: la validación continúa.',
        'تم تسجيل اعتمادك: يستمر الاعتماد.',
        '承認を記録しました。レビューは続きます。',
        '您的审批已记录：审批流程继续。'),
    'documents.circuit.title': (
        'Validation de la version v{$NUMBER}', 'Approval of version v{$NUMBER}', 'Validación de la versión v{$NUMBER}',
        'اعتماد الإصدار v{$NUMBER}', 'バージョン v{$NUMBER} の承認', '版本 v{$NUMBER} 的审批'),
    'documents.circuit.turn-yours': (
        "C'est à vous : approuvez ou refusez cette étape.", 'Your turn: approve or reject this step.',
        'Es su turno: apruebe o rechace esta etapa.', 'دورك: اعتمد هذه المرحلة أو ارفضها.',
        'あなたの番です。このステップを承認または却下してください。', '轮到您了：请审批或驳回此步骤。'),
    'documents.circuit.turn-decided': (
        'Vous avez décidé : une autre personne doit valider la suite.',
        'You have decided: someone else must approve what comes next.',
        'Ya ha decidido: otra persona debe validar lo siguiente.',
        'لقد اتخذت قرارك: يجب أن يعتمد شخص آخر ما يلي.',
        '判断済みです。続きは別の人が承認します。',
        '您已作出决定：后续须由他人审批。'),
    'documents.circuit.turn-author': (
        "Vous en êtes l'auteur : d'autres la valident.", 'You are its author: others approve it.',
        'Usted es el autor: otros la validan.', 'أنت مؤلفه: يعتمده آخرون.',
        'あなたは作成者です。承認は他の人が行います。', '您是作者：由他人审批。'),
    'documents.circuit.turn-waiting': (
        'En attente de : {$ROLE}.', 'Waiting for: {$ROLE}.', 'En espera de: {$ROLE}.', 'بانتظار: {$ROLE}.',
        '待機中：{$ROLE}', '等待：{$ROLE}。'),
    'documents.circuit.simple': (
        "Approbation simple : une personne ayant le droit d'approuver suffit.",
        'Simple approval: one person allowed to approve is enough.',
        'Aprobación simple: basta una persona con derecho a aprobar.',
        'اعتماد بسيط: يكفي شخص واحد يملك صلاحية الاعتماد.',
        'シンプル承認：承認権限を持つ1名で足ります。',
        '简单审批：有审批权限的一人即可。'),
    'documents.circuit.failed': (
        "L'avancement de la validation n'a pas pu être lu.", 'The approval progress could not be read.',
        'No se pudo leer el avance de la validación.', 'تعذّرت قراءة تقدّم الاعتماد.',
        '承認の進捗を読み込めませんでした。', '无法读取审批进度。'),
    'documents.circuit.count': (
        '{$DONE} sur {$NEEDED}', '{$DONE} of {$NEEDED}', '{$DONE} de {$NEEDED}', '{$DONE} من {$NEEDED}',
        '{$NEEDED} 件中 {$DONE} 件', '{$DONE}/{$NEEDED}'),
    'documents.circuit.you': ('Vous', 'You', 'Usted', 'أنت', 'あなた', '您'),
    'documents.circuit.someone': ('Un membre', 'A member', 'Un miembro', 'عضو', 'メンバー', '一名成员'),
    'documents.circuit.rejected': (
        'Version v{$NUMBER} refusée le {$DATE}', 'Version v{$NUMBER} rejected on {$DATE}',
        'Versión v{$NUMBER} rechazada el {$DATE}', 'رُفض الإصدار v{$NUMBER} في {$DATE}',
        'バージョン v{$NUMBER} は {$DATE} に却下されました', '版本 v{$NUMBER} 于 {$DATE} 被驳回'),
    'documents.circuit.rejected-next': (
        'Corrigez-la, puis soumettez-la de nouveau : le circuit repartira de la première étape.',
        'Correct it, then submit it again: the workflow will restart from the first step.',
        'Corríjala y vuelva a enviarla: el circuito empezará de nuevo desde la primera etapa.',
        'صحّحه ثم قدّمه من جديد: سيبدأ المسار من المرحلة الأولى.',
        '修正して再提出してください。ワークフローは最初のステップからやり直しになります。',
        '请修改后重新提交：流程将从第一步重新开始。'),
    'documents.reject.title': (
        'Refuser la version v{$number}', 'Reject version v{$number}', 'Rechazar la versión v{$number}',
        'رفض الإصدار v{$number}', 'バージョン v{$number} を却下', '驳回版本 v{$number}'),
    'documents.reject.subtitle': (
        'La version revient en brouillon. Son auteur lira votre raison, la corrigera et la soumettra de nouveau.',
        'The version goes back to draft. Its author will read your reason, correct it and submit it again.',
        'La versión vuelve a borrador. Su autor leerá su motivo, la corregirá y la volverá a enviar.',
        'يعود الإصدار إلى المسودة. سيقرأ مؤلفه سببك ويصحّحه ثم يقدّمه من جديد.',
        'バージョンは下書きに戻ります。作成者が理由を読み、修正して再提出します。',
        '该版本将退回草稿。作者会阅读您的理由，修改后重新提交。'),
    'documents.reject.blocked': (
        'Écrivez la raison du refus.', 'Write the reason for the rejection.', 'Escriba el motivo del rechazo.',
        'اكتب سبب الرفض.', '却下の理由を書いてください。', '请填写驳回理由。'),
    'documents.reject.submit': ('Refuser', 'Reject', 'Rechazar', 'رفض', '却下', '驳回'),
    'documents.reject.reason': ('Raison du refus', 'Reason for rejection', 'Motivo del rechazo', 'سبب الرفض', '却下の理由', '驳回理由'),
    'documents.reject.reason-placeholder': (
        'Ce qui manque, ce qui est à corriger', 'What is missing, what needs correcting', 'Lo que falta, lo que hay que corregir',
        'ما الذي ينقص وما الذي يجب تصحيحه', '不足している点、修正が必要な点', '缺少什么、需要修改什么'),
    'documents.reject.reason-required': (
        'Un refus se motive.', 'A rejection needs a reason.', 'Un rechazo debe motivarse.', 'يجب تعليل الرفض.',
        '却下には理由が必要です。', '驳回须说明理由。'),
    'documents.reject.done': (
        'Version refusée : elle revient en brouillon chez son auteur.',
        'Version rejected: it goes back to its author as a draft.',
        'Versión rechazada: vuelve a borrador para su autor.',
        'تم رفض الإصدار: يعود مسودةً إلى مؤلفه.',
        'バージョンを却下しました。下書きとして作成者に戻ります。',
        '版本已驳回：已作为草稿退回作者。'),
    'documents.reject.failed': (
        'Refus impossible.', 'Rejection failed.', 'No se pudo rechazar.', 'تعذّر الرفض.', '却下できませんでした。', '驳回失败。'),
}
