# -*- coding: utf-8 -*-
"""Table i18n - preuves jointes a un dossier CAPA (SS4.2). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'capa.evidence.title': ('Preuves', 'Evidence', 'Pruebas', 'الأدلة', '証拠', '证据'),
    'capa.evidence.hint': (
        "Ce qui atteste que l'action a produit son effet : relevé, procédure signée, photo. "
        "PDF, image, Word ou Excel, 10 Mo par pièce, 10 pièces au maximum.",
        'What shows the action worked: a record, a signed procedure, a photo. '
        'PDF, image, Word or Excel, 10 MB per file, 10 files at most.',
        'Lo que acredita que la acción surtió efecto: un registro, un procedimiento firmado, '
        'una foto. PDF, imagen, Word o Excel, 10 MB por pieza, 10 piezas como máximo.',
        'ما يثبت أن الإجراء أحدث أثره: سجل، إجراء موقّع، صورة. '
        'PDF أو صورة أو Word أو Excel، 10 ميغابايت للملف، 10 ملفات كحد أقصى.',
        '対策が効果を上げたことを示すもの（記録、署名済み手順書、写真）。'
        'PDF・画像・Word・Excel、1件10 MBまで、最大10件。',
        '证明措施确有成效的材料：记录、已签署的程序、照片。'
        'PDF、图片、Word 或 Excel，每件 10 MB，最多 10 件。'),
    'capa.evidence.add': (
        'Joindre une preuve', 'Attach evidence', 'Adjuntar una prueba',
        'إرفاق دليل', '証拠を添付', '附加证据'),
    'capa.evidence.added': (
        'Preuve ajoutée au dossier.', 'Evidence added to the case.',
        'Prueba añadida al expediente.', 'تمت إضافة الدليل إلى الملف.',
        '証拠をケースに追加しました。', '证据已加入档案。'),
    'capa.evidence.empty': (
        "Aucune preuve jointe. Un dossier clos sans preuve s'affirme efficace sans le démontrer.",
        'No evidence attached. A case closed without evidence claims effectiveness without showing it.',
        'Sin pruebas adjuntas. Un expediente cerrado sin pruebas afirma su eficacia sin demostrarla.',
        'لا توجد أدلة مرفقة. ملف يُغلق بلا دليل يدّعي الفعالية دون أن يبرهن عليها.',
        '証拠が添付されていません。証拠のないまま閉じたケースは、有効性を示さずに主張しているだけです。',
        '尚未附加证据。没有证据就结案，等于宣称有效却不加证明。'),
    'capa.evidence.remove-tooltip': (
        'Retirer cette preuve', 'Remove this evidence', 'Quitar esta prueba',
        'إزالة هذا الدليل', 'この証拠を削除', '移除此证据'),
    'capa.evidence.remove-title': (
        'Retirer cette preuve ?', 'Remove this evidence?', '¿Quitar esta prueba?',
        'إزالة هذا الدليل؟', 'この証拠を削除しますか？', '移除此证据？'),
    'capa.evidence.remove-message': (
        "La pièce sera définitivement retirée du dossier. Un dossier d'audit se justifie par "
        "ses preuves : ce retrait est traçable.",
        'The file will be permanently removed from the case. An audit case stands on its '
        'evidence: this removal is traceable.',
        'La pieza se retirará definitivamente del expediente. Un expediente de auditoría se '
        'sostiene por sus pruebas: esta retirada queda trazada.',
        'ستتم إزالة الملف نهائياً من الحالة. ملف التدقيق يقوم على أدلته: هذه الإزالة قابلة للتتبع.',
        'ファイルはケースから完全に削除されます。監査ケースは証拠で成り立ちます。この削除は追跡されます。',
        '该文件将从档案中永久移除。审核档案立于其证据之上：此次移除可追溯。'),
    'capa.evidence.remove-error': (
        'Échec du retrait de la preuve.', 'Could not remove the evidence.',
        'No se pudo quitar la prueba.', 'تعذّرت إزالة الدليل.',
        '証拠を削除できませんでした。', '无法移除证据。'),
    'capa.evidence.error': (
        "Échec de l'ajout de la preuve.", 'Could not add the evidence.',
        'No se pudo añadir la prueba.', 'تعذّرت إضافة الدليل.',
        '証拠を追加できませんでした。', '无法添加证据。'),
    'capa.evidence.too-large': (
        'Pièce trop lourde — 10 Mo au maximum.', 'File too large — 10 MB at most.',
        'Pieza demasiado pesada: 10 MB como máximo.', 'الملف كبير جداً — 10 ميغابايت كحد أقصى.',
        'ファイルが大きすぎます — 最大10 MB。', '文件过大 — 最多 10 MB。'),
    'capa.evidence.rejected': (
        'Format refusé — PDF, image, Word ou Excel, et le contenu doit correspondre au format annoncé.',
        'Format refused — PDF, image, Word or Excel, and the content must match the declared format.',
        'Formato rechazado: PDF, imagen, Word o Excel, y el contenido debe coincidir con el formato declarado.',
        'صيغة مرفوضة — PDF أو صورة أو Word أو Excel، ويجب أن يطابق المحتوى الصيغة المعلنة.',
        '形式が拒否されました — PDF・画像・Word・Excel、かつ内容が申告された形式と一致する必要があります。',
        '格式被拒 — 仅限 PDF、图片、Word 或 Excel，且内容须与所声明的格式一致。'),
    'capa.evidence.limit': (
        'Le dossier a atteint sa limite de preuves, ou il est clôturé.',
        'The case has reached its evidence limit, or it is closed.',
        'El expediente alcanzó su límite de pruebas, o está cerrado.',
        'بلغ الملف حده الأقصى من الأدلة، أو أنه مُغلق.',
        'ケースは証拠の上限に達したか、既に閉じられています。',
        '档案已达证据数量上限，或已结案。'),
    'capa.evidence.locked-closed': (
        'Le dossier est clos : ses preuves restent consultables, mais ne changent plus.',
        'The case is closed: its evidence stays readable, but no longer changes.',
        'El expediente está cerrado: sus pruebas siguen consultables, pero ya no cambian.',
        'الملف مُغلق: تبقى أدلته قابلة للاطّلاع، لكنها لم تعد تتغيّر.',
        'ケースは閉じられています。証拠は閲覧できますが、もう変更されません。',
        '档案已结案：其证据仍可查阅，但不再变动。'),
    'capa.evidence.locked-full': (
        "Dix preuves, c'est la limite : au-delà, un dossier ne se lit plus. Retirez-en une pour "
        "en joindre une autre.",
        'Ten pieces of evidence is the limit: beyond that, a case cannot be read. Remove one to '
        'attach another.',
        'Diez pruebas es el límite: más allá, un expediente ya no se lee. Quite una para adjuntar otra.',
        'عشرة أدلة هي الحد: بعدها لا يعود الملف قابلاً للقراءة. أزل واحداً لإرفاق آخر.',
        '証拠は10件が限界です。それを超えるとケースは読めなくなります。1件外してから追加してください。',
        '十件证据是上限：再多，档案就读不下去了。请先移除一件再添加。'),
    'capa.evidence.storage-disabled': (
        "Le stockage des pièces jointes est désactivé sur cet environnement : aucune preuve ne "
        "peut y être déposée ni relue.",
        'Attachment storage is disabled on this environment: no evidence can be added or read here.',
        'El almacenamiento de adjuntos está desactivado en este entorno: no se puede añadir ni leer '
        'ninguna prueba.',
        'تخزين المرفقات معطّل في هذه البيئة: لا يمكن إيداع أي دليل ولا الاطّلاع عليه.',
        'この環境では添付ファイルの保管が無効です。証拠の追加も閲覧もできません。',
        '本环境的附件存储已停用：无法在此上传或查阅任何证据。'),
}
