# -*- coding: utf-8 -*-
"""Table i18n - rouvrir une action depuis le D8.

Une action jugee efficace se clot, et close plus rien n'y bouge : ni son etat,
ni ses preuves. Or un releve qui arrive la semaine suivante peut contredire le
verdict. La seule issue etait d'ouvrir une SECONDE CAPA sur le meme geste --
deux actions la ou il n'y en a qu'une.

La borne est le dossier, pas l'action : tant que l'ecart n'est pas cloture, son
traitement se discute.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    'nc.8d.reopen-hint': (
        "Une action dont le résultat ne tient pas se rouvre ici, tant que l’écart n’est pas clôturé.",
        'An action whose result does not hold can be reopened here, as long as the non-conformity is not closed.',
        'Una acción cuyo resultado no se sostiene puede reabrirse aquí, mientras la no conformidad no esté cerrada.',
        'يمكن إعادة فتح أي إجراء لم تصمد نتيجته من هنا، ما دام عدم المطابقة غير مغلق.',
        '結果が維持されない処置は、不適合が未クローズである限りここで再開できます。',
        '只要不合格项尚未关闭，结果未能维持的措施可在此重新打开。'),
    'nc.8d.reopen-action': (
        'Rouvrir', 'Reopen', 'Reabrir',
        'إعادة فتح', '再開', '重新打开'),
    'nc.8d.reopen-already-open': (
        'déjà ouverte', 'already open', 'ya abierta',
        'مفتوح بالفعل', '対応中', '已处于打开状态'),
    'nc.8d.reopen-aria': (
        "Rouvrir l'action : {$title}",
        'Reopen the action: {$title}',
        'Reabrir la acción: {$title}',
        'إعادة فتح الإجراء: {$title}',
        '処置を再開: {$title}',
        '重新打开措施：{$title}'),
    'nc.8d.reopen-done': (
        'Action rouverte.', 'Action reopened.', 'Acción reabierta.',
        'تمت إعادة فتح الإجراء.', '処置を再開しました。', '措施已重新打开。'),
    'nc.8d.reopen-error': (
        "L'action n'a pas pu être rouverte.",
        'The action could not be reopened.',
        'No se ha podido reabrir la acción.',
        'تعذرت إعادة فتح الإجراء.',
        '処置を再開できませんでした。',
        '无法重新打开该措施。'),
}
