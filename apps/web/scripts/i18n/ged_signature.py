# -*- coding: utf-8 -*-
"""Table i18n - domaine GED, circuit de signature (ADR 0099). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    # ---------- signataires ----------
    'documents.signatory.label': (
        'Signataire {$position}', 'Signatory {$position}', 'Firmante {$position}', 'الموقّع {$position}',
        '署名者 {$position}', '签署人 {$position}'),
    'documents.signatory.pending': ('En attente', 'Pending', 'Pendiente', 'قيد الانتظار', '保留中', '待签署'),
    'documents.signatory.approved': ('Approuvé', 'Approved', 'Aprobado', 'معتمد', '承認済み', '已批准'),
    'documents.signatory.rejected': ('Rejeté', 'Rejected', 'Rechazado', 'مرفوض', '却下', '已驳回'),

    # ---------- soumission à signature ----------
    'documents.submit.title': (
        'Soumettre la version v{$number} à signature', 'Submit version v{$number} for signature',
        'Enviar la versión v{$number} a firma', 'إرسال الإصدار v{$number} للتوقيع',
        'バージョン v{$number} を署名に提出', '提交版本 v{$number} 签署'),
    'documents.submit.subtitle': (
        "Choisissez de une à trois personnes. Elles signeront dans l'ordre : le signataire 2 après le 1, le 3 après le 2. Un seul refus renvoie la version en brouillon.",
        'Choose one to three people. They will sign in order: signatory 2 after 1, 3 after 2. A single rejection sends the version back to draft.',
        'Elija de una a tres personas. Firmarán en orden: el firmante 2 después del 1, el 3 después del 2. Un solo rechazo devuelve la versión a borrador.',
        'اختر من شخص إلى ثلاثة أشخاص. سيوقّعون بالترتيب: الموقّع 2 بعد 1، و3 بعد 2. رفض واحد يعيد الإصدار إلى المسودة.',
        '1～3 人を選択してください。順番に署名します：署名者 2 は 1 の後、3 は 2 の後です。1 件でも却下されると、バージョンは下書きに戻ります。',
        '请选择一到三人。他们将按顺序签署：签署人 2 在 1 之后，3 在 2 之后。只要有一人驳回，版本即退回草稿。'),
    'documents.submit.blocked': (
        'Choisissez au moins le signataire 1, une personne différente à chaque place.',
        'Choose at least signatory 1, a different person in each place.',
        'Elija al menos el firmante 1, una persona distinta en cada puesto.',
        'اختر الموقّع 1 على الأقل، وشخصًا مختلفًا في كل موضع.',
        '少なくとも署名者 1 を選択し、各枠に別の人を指定してください。',
        '请至少选择签署人 1，且每个位置为不同的人。'),
    'documents.submit.submit': ('Soumettre à signature', 'Submit for signature', 'Enviar a firma', 'إرسال للتوقيع', '署名に提出', '提交签署'),
    'documents.submit.duplicate': (
        "Une même personne ne peut signer qu'une fois.", 'The same person can only sign once.',
        'Una misma persona solo puede firmar una vez.', 'لا يمكن للشخص نفسه التوقيع إلا مرة واحدة.',
        '同じ人が署名できるのは 1 回だけです。', '同一人只能签署一次。'),
    'documents.submit.done': (
        'Version soumise : le signataire 1 est invité à signer.', 'Version submitted: signatory 1 is invited to sign.',
        'Versión enviada: se invita al firmante 1 a firmar.', 'تم إرسال الإصدار: دُعي الموقّع 1 إلى التوقيع.',
        'バージョンを提出しました：署名者 1 に署名を依頼しました。', '版本已提交：已邀请签署人 1 签署。'),
    'documents.detail.submit-needs-file': (
        'Joignez le fichier du document avant de soumettre la version.',
        'Attach the document file before submitting the version.',
        'Adjunte el archivo del documento antes de enviar la versión.',
        'أرفق ملف المستند قبل إرسال الإصدار.',
        'バージョンを提出する前に文書ファイルを添付してください。',
        '提交版本前请先附加文档文件。'),

    # ---------- cartes de signature ----------
    'documents.signature.title': (
        'Circuit de signature de la version v{$NUMBER}', 'Signature workflow for version v{$NUMBER}',
        'Circuito de firma de la versión v{$NUMBER}', 'مسار توقيع الإصدار v{$NUMBER}',
        'バージョン v{$NUMBER} の署名フロー', '版本 v{$NUMBER} 的签署流程'),
    'documents.signature.turn-yours': (
        "C'est à vous de signer : approuvez ou rejetez la version.", 'It is your turn to sign: approve or reject the version.',
        'Le toca firmar: apruebe o rechace la versión.', 'حان دورك للتوقيع: اعتمد الإصدار أو ارفضه.',
        'あなたが署名する番です：バージョンを承認または却下してください。', '轮到您签署：请批准或驳回该版本。'),
    'documents.signature.turn-author': (
        "Vous en êtes l'auteur : vos signataires la valident dans l'ordre.",
        'You are its author: your signatories approve it in order.',
        'Usted es su autor: sus firmantes la validan en orden.',
        'أنت مؤلفه: يعتمده موقّعوك بالترتيب.',
        'あなたは作成者です：署名者が順番に承認します。', '您是作者：您的签署人将按顺序审批。'),
    'documents.signature.turn-waiting': (
        'En attente de la signature de {$NAME}.', 'Waiting for the signature of {$NAME}.',
        'A la espera de la firma de {$NAME}.', 'في انتظار توقيع {$NAME}.',
        '{$NAME} の署名待ちです。', '等待 {$NAME} 签署。'),
    'documents.signature.comment': (
        'Commentaire (facultatif)', 'Comment (optional)', 'Comentario (opcional)', 'تعليق (اختياري)',
        'コメント（任意）', '备注（可选）'),
    'documents.signature.signed': (
        'Signature enregistrée : le signataire suivant est invité à signer.',
        'Signature recorded: the next signatory is invited to sign.',
        'Firma registrada: se invita al siguiente firmante a firmar.',
        'تم تسجيل التوقيع: دُعي الموقّع التالي إلى التوقيع.',
        '署名を記録しました：次の署名者に署名を依頼しました。', '签署已记录：已邀请下一位签署人签署。'),
    'documents.signature.rejected': (
        'Rejet enregistré : la version revient en brouillon chez son auteur.',
        'Rejection recorded: the version goes back to draft with its author.',
        'Rechazo registrado: la versión vuelve a borrador con su autor.',
        'تم تسجيل الرفض: يعود الإصدار إلى المسودة لدى مؤلفه.',
        '却下を記録しました：バージョンは作成者の下書きに戻ります。', '驳回已记录：版本退回作者的草稿。'),
    'documents.signature.failed': (
        "La signature n'a pas été enregistrée.", 'The signature was not recorded.', 'La firma no se ha registrado.',
        'لم يُسجَّل التوقيع.', '署名は記録されませんでした。', '签署未被记录。'),

    # ---------- parcours de la fiche ----------
    'documents.validation.signature': ('Signature', 'Signature', 'Firma', 'التوقيع', '署名', '签署'),
    'documents.validation.signature-note': (
        'De un à trois signataires, choisis à la soumission', 'One to three signatories, chosen at submission',
        'De uno a tres firmantes, elegidos al enviar', 'من موقّع إلى ثلاثة، يُختارون عند الإرسال',
        '提出時に選ぶ 1～3 名の署名者', '一至三名签署人，在提交时选择'),

    # ---------- création : fichier obligatoire ----------
    'documents.create.blocked': (
        'Renseignez les champs obligatoires et joignez le fichier du document.',
        'Fill in the required fields and attach the document file.',
        'Complete los campos obligatorios y adjunte el archivo del documento.',
        'املأ الحقول الإلزامية وأرفق ملف المستند.',
        '必須項目を入力し、文書ファイルを添付してください。', '请填写必填字段并附加文档文件。'),
    'documents.create.file-required': (
        'Un document se crée avec son fichier : joignez-le.', 'A document is created with its file: attach it.',
        'Un documento se crea con su archivo: adjúntelo.', 'يُنشأ المستند مع ملفه: أرفقه.',
        '文書はファイルとともに作成します。添付してください。', '创建文档时须附带其文件：请附加。'),
}
