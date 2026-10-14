# -*- coding: utf-8 -*-
"""Table i18n - tableau des actions d'une fiche CAPA (SS4.2, ADR 0052).

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- colonnes -----------------------------------------------------------
    'capa.detail.col-evidence': (
        'Preuve', 'Evidence', 'Prueba', 'الدليل', '証拠', '证据'),

    # --- statuts d'action, en toutes lettres --------------------------------
    'capa.action-status.pending': (
        'À faire', 'To do', 'Por hacer', 'قيد الانتظار', '未着手', '待办'),
    'capa.action-status.in-progress': (
        'En cours', 'In progress', 'En curso', 'قيد التنفيذ', '進行中', '进行中'),
    'capa.action-status.done': (
        'Faite', 'Done', 'Hecha', 'منجزة', '完了', '已完成'),

    # --- édition en ligne ----------------------------------------------------

    # --- preuve rattachée à une action --------------------------------------
    'capa.action-evidence.attach': (
        'Joindre', 'Attach', 'Adjuntar', 'إرفاق', '添付', '附加'),
    'capa.action-evidence.added': (
        "Preuve jointe à l'action.", 'Evidence attached to the action.',
        'Prueba adjuntada a la acción.', 'تم إرفاق الدليل بالإجراء.',
        '対策に証拠を添付しました。', '已为该措施附加证据。'),
    # Étiquettes d'accessibilité : une icône seule ou un mot isolé ne dit pas
    # SUR QUOI il agit — huit lignes rendraient huit boutons indiscernables.
    'capa.action-evidence.attach-aria': (
        "Joindre une preuve à l'action : {$title}",
        'Attach evidence to the action: {$title}',
        'Adjuntar una prueba a la acción: {$title}',
        'إرفاق دليل بالإجراء: {$title}',
        '対策に証拠を添付：{$title}',
        '为措施附加证据：{$title}'),
    'capa.action-evidence.remove-aria': (
        "Retirer la preuve de l'action : {$title}",
        "Remove the action's evidence: {$title}",
        'Quitar la prueba de la acción: {$title}',
        'إزالة دليل الإجراء: {$title}',
        '対策の証拠を削除：{$title}',
        '移除措施的证据：{$title}'),
    'capa.action-evidence.remove-tooltip': (
        'Retirer la preuve de cette action', "Remove this action's evidence",
        'Quitar la prueba de esta acción', 'إزالة دليل هذا الإجراء',
        'この対策の証拠を削除', '移除该措施的证据'),
    'capa.action-evidence.remove-title': (
        'Retirer la preuve de cette action ?', "Remove this action's evidence?",
        '¿Quitar la prueba de esta acción?', 'إزالة دليل هذا الإجراء؟',
        'この対策の証拠を削除しますか？', '移除该措施的证据？'),
    'capa.action-evidence.remove-message': (
        "La pièce sera définitivement retirée. Une action déclarée faite sans preuve "
        "s'affirme faite sans le démontrer : ce retrait est traçable.",
        'The file will be permanently removed. An action declared done without evidence '
        'claims completion without showing it: this removal is traceable.',
        'La pieza se retirará definitivamente. Una acción declarada hecha sin prueba '
        'afirma su cumplimiento sin demostrarlo: esta retirada queda trazada.',
        'ستتم إزالة الملف نهائياً. إجراء يُعلن إنجازه بلا دليل يدّعي الإنجاز دون أن يبرهن عليه: '
        'هذه الإزالة قابلة للتتبع.',
        '添付ファイルは完全に削除されます。証拠なしに完了とされた対策は、示さずに主張しているだけです。'
        'この削除は追跡されます。',
        '该文件将被永久移除。没有证据就宣称完成的措施，是主张而非证明：此次移除可追溯。'),
    'capa.action-evidence.limit': (
        "Cette action porte déjà sa preuve, ou le dossier est clôturé.",
        'This action already carries its evidence, or the case is closed.',
        'Esta acción ya tiene su prueba, o el expediente está cerrado.',
        'هذا الإجراء يحمل دليله بالفعل، أو أن الملف مُغلق.',
        'この対策には既に証拠があるか、ケースが閉じられています。',
        '该措施已有证据，或档案已结案。'),
    'capa.action-evidence.gone': (
        "Cette action n'existe plus — recharge la fiche.",
        'This action no longer exists — reload the case.',
        'Esta acción ya no existe: recarga la ficha.',
        'لم يعد هذا الإجراء موجوداً — أعد تحميل الملف.',
        'この対策はもう存在しません。ページを再読み込みしてください。',
        '该措施已不存在 — 请重新加载档案。'),

    # --- boîte de dialogue d'ajout d'action ---------------------------------
}
