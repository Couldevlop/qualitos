# -*- coding: utf-8 -*-
"""Table i18n - preuves jointes aux etapes d'un cycle PDCA (SS3.1, ADR 0061).

Une etape declaree faite sans document ne prouve rien : elle affirme. La colonne
« Preuve » du tableau des etapes porte ce document. id: (fr, en, es, ar, ja, zh).
"""

TRANSLATIONS = {
    'pdca.detail.col-evidence': ('Preuve', 'Evidence', 'Prueba', 'الدليل', '証拠', '证据'),
    'pdca.evidence.attach': (
        'Joindre', 'Attach', 'Adjuntar', 'إرفاق', '添付', '附加'),
    'pdca.evidence.added': (
        "Preuve jointe à l'étape.", 'Evidence attached to the step.',
        'Prueba adjuntada a la etapa.', 'تم إرفاق الدليل بالخطوة.',
        'ステップに証拠を添付しました。', '已为该步骤附加证据。'),
    'pdca.evidence.remove-tooltip': (
        'Retirer la preuve de cette étape', "Remove this step's evidence",
        'Quitar la prueba de esta etapa', 'إزالة دليل هذه الخطوة',
        'このステップの証拠を削除', '移除此步骤的证据'),
    'pdca.evidence.remove-title': (
        'Retirer la preuve de cette étape ?', "Remove this step's evidence?",
        '¿Quitar la prueba de esta etapa?', 'إزالة دليل هذه الخطوة؟',
        'このステップの証拠を削除しますか？', '移除此步骤的证据？'),
    'pdca.evidence.remove-message': (
        "La pièce sera définitivement retirée. Une étape déclarée faite sans preuve "
        "s'affirme faite sans le démontrer : ce retrait est traçable.",
        'The file will be permanently removed. A step declared done without evidence '
        'claims to be done without showing it: this removal is traceable.',
        'La pieza se retirará definitivamente. Una etapa declarada hecha sin prueba '
        'se afirma hecha sin demostrarlo: esta retirada queda trazada.',
        'ستتم إزالة الملف نهائياً. خطوة يُعلن إنجازها بلا دليل تدّعي الإنجاز دون أن '
        'تبرهن عليه: هذه الإزالة قابلة للتتبع.',
        'ファイルは完全に削除されます。証拠のないまま完了と宣言されたステップは、'
        '示さずに主張しているだけです。この削除は追跡されます。',
        '该文件将被永久移除。没有证据就宣称完成的步骤，等于说完成却不加证明：'
        '此次移除可追溯。'),
    'pdca.evidence.remove-error': (
        'Échec du retrait de la preuve.', 'Could not remove the evidence.',
        'No se pudo quitar la prueba.', 'تعذّرت إزالة الدليل.',
        '証拠を削除できませんでした。', '无法移除证据。'),
    'pdca.evidence.error': (
        "Échec de l'ajout de la preuve.", 'Could not add the evidence.',
        'No se pudo añadir la prueba.', 'تعذّرت إضافة الدليل.',
        '証拠を追加できませんでした。', '无法添加证据。'),
    'pdca.evidence.too-large': (
        'Pièce trop lourde — 10 Mo au maximum.', 'File too large — 10 MB at most.',
        'Pieza demasiado pesada: 10 MB como máximo.', 'الملف كبير جداً — 10 ميغابايت كحد أقصى.',
        'ファイルが大きすぎます — 最大10 MB。', '文件过大 — 最多 10 MB。'),
    'pdca.evidence.rejected': (
        'Format refusé — PDF, image, Word ou Excel, et le contenu doit correspondre '
        'au format annoncé.',
        'Format refused — PDF, image, Word or Excel, and the content must match the '
        'declared format.',
        'Formato rechazado: PDF, imagen, Word o Excel, y el contenido debe coincidir '
        'con el formato declarado.',
        'صيغة مرفوضة — PDF أو صورة أو Word أو Excel، ويجب أن يطابق المحتوى الصيغة المعلنة.',
        '形式が拒否されました — PDF・画像・Word・Excel、かつ内容が申告された形式と'
        '一致する必要があります。',
        '格式被拒 — 仅限 PDF、图片、Word 或 Excel，且内容须与所声明的格式一致。'),
    'pdca.evidence.limit': (
        'Cette étape porte déjà sa preuve, ou le cycle est clos.',
        'This step already carries its evidence, or the cycle is closed.',
        'Esta etapa ya lleva su prueba, o el ciclo está cerrado.',
        'هذه الخطوة تحمل دليلها بالفعل، أو أن الدورة مغلقة.',
        'このステップには既に証拠があるか、サイクルが終了しています。',
        '此步骤已有证据，或该循环已结束。'),
    'pdca.evidence.gone': (
        "Cette étape n'existe plus — recharge la fiche.",
        'This step no longer exists — reload the page.',
        'Esta etapa ya no existe: recarga la ficha.',
        'لم تعد هذه الخطوة موجودة — أعد تحميل الصفحة.',
        'このステップは存在しません。ページを再読み込みしてください。',
        '该步骤已不存在 — 请重新加载页面。'),
    'pdca.evidence.storage-disabled': (
        "Le stockage des pièces jointes est désactivé sur cet environnement : aucune "
        "preuve ne peut y être déposée ni relue.",
        'Attachment storage is disabled on this environment: no evidence can be added '
        'or read here.',
        'El almacenamiento de adjuntos está desactivado en este entorno: no se puede '
        'añadir ni leer ninguna prueba.',
        'تخزين المرفقات معطّل في هذه البيئة: لا يمكن إيداع أي دليل ولا الاطّلاع عليه.',
        'この環境では添付ファイルの保管が無効です。証拠の追加も閲覧もできません。',
        '本环境的附件存储已停用：无法在此上传或查阅任何证据。'),
    # Etiquettes d'accessibilite : une icone seule ou un mot isole ne dit pas SUR
    # QUOI il agit — dix lignes rendraient dix boutons indiscernables.
    'pdca.evidence.attach-aria': (
        "Joindre une preuve à l'étape : {$title}",
        'Attach evidence to the step: {$title}',
        'Adjuntar una prueba a la etapa: {$title}',
        'إرفاق دليل بالخطوة: {$title}',
        'ステップに証拠を添付：{$title}',
        '为步骤附加证据：{$title}'),
    'pdca.evidence.remove-aria': (
        "Retirer la preuve de l'étape : {$title}",
        "Remove the step's evidence: {$title}",
        'Quitar la prueba de la etapa: {$title}',
        'إزالة دليل الخطوة: {$title}',
        'ステップの証拠を削除：{$title}',
        '移除步骤的证据：{$title}'),
}
