# -*- coding: utf-8 -*-
"""Table i18n - ce qui s'oppose a l'avancement d'un ecart, dit AVANT le clic.

Le refus existait et il etait juste : on ne resout pas un ecart dont les
actions courent encore, on ne le clot pas avant le verdict d'efficacite de son
dossier. Mais il n'arrivait qu'apres le clic, sous la forme d'un 409 portant
une phrase anglaise construite par le service -- et le bouton eteint ne disait
rien de ce qu'il fallait faire. Chaque phrase nomme donc LE GESTE qui debloque.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- le titre du bloc, selon l'etape ------------------------------------
    'nc.detail.blockers-resolve': (
        "Cet écart ne peut pas encore être résolu",
        'This non-conformity cannot be resolved yet',
        'Esta no conformidad aún no puede resolverse',
        'لا يمكن حل عدم المطابقة هذا بعد',
        'この不適合はまだ解決できません',
        '该不合格项尚不能解决'),
    'nc.detail.blockers-close': (
        "Cet écart ne peut pas encore être clôturé",
        'This non-conformity cannot be closed yet',
        'Esta no conformidad aún no puede cerrarse',
        'لا يمكن إغلاق عدم المطابقة هذا بعد',
        'この不適合はまだ閉じられません',
        '该不合格项尚不能关闭'),

    # --- resoudre : des actions restent a mener -----------------------------
    'nc.blocker.actions-one': (
        "1 dossier CAPA porte encore des actions à mener. Terminez ses actions pour pouvoir résoudre cet écart.",
        '1 CAPA case still has actions to carry out. Complete its actions to resolve this non-conformity.',
        '1 expediente CAPA todavía tiene acciones por realizar. Complete sus acciones para resolver esta no conformidad.',
        'لا يزال ملف CAPA واحد يحتوي على إجراءات لم تُنفَّذ. أكمل إجراءاته لتتمكن من حل عدم المطابقة هذا.',
        'CAPA 案件が 1 件、未実施の処置を残しています。その処置を完了すると、この不適合を解決できます。',
        '仍有 1 份 CAPA 档案存在未完成的措施。完成其措施后方可解决该不合格项。'),
    'nc.blocker.actions-many': (
        "{$count} dossiers CAPA portent encore des actions à mener. Terminez leurs actions pour pouvoir résoudre cet écart.",
        '{$count} CAPA cases still have actions to carry out. Complete their actions to resolve this non-conformity.',
        '{$count} expedientes CAPA todavía tienen acciones por realizar. Complete sus acciones para resolver esta no conformidad.',
        'لا تزال {$count} من ملفات CAPA تحتوي على إجراءات لم تُنفَّذ. أكمل إجراءاتها لتتمكن من حل عدم المطابقة هذا.',
        'CAPA 案件が {$count} 件、未実施の処置を残しています。それらの処置を完了すると、この不適合を解決できます。',
        '仍有 {$count} 份 CAPA 档案存在未完成的措施。完成其措施后方可解决该不合格项。'),

    # --- clore : le verdict d'efficacite n'est pas rendu --------------------
    'nc.blocker.cases-one': (
        "1 dossier CAPA n'est pas clôturé. Clôturez-le — après vérification de son efficacité — pour pouvoir clôturer cet écart.",
        '1 CAPA case is not closed. Close it — once its effectiveness is verified — to close this non-conformity.',
        '1 expediente CAPA no está cerrado. Ciérrelo —tras verificar su eficacia— para cerrar esta no conformidad.',
        'هناك ملف CAPA واحد غير مغلق. أغلقه — بعد التحقق من فعاليته — لتتمكن من إغلاق عدم المطابقة هذا.',
        'CAPA 案件が 1 件、未完了です。有効性を確認のうえ完了させると、この不適合を閉じられます。',
        '有 1 份 CAPA 档案尚未关闭。在核实其有效性后关闭它，方可关闭该不合格项。'),
    'nc.blocker.cases-many': (
        "{$count} dossiers CAPA ne sont pas clôturés. Clôturez-les — après vérification de leur efficacité — pour pouvoir clôturer cet écart.",
        '{$count} CAPA cases are not closed. Close them — once their effectiveness is verified — to close this non-conformity.',
        '{$count} expedientes CAPA no están cerrados. Ciérrelos —tras verificar su eficacia— para cerrar esta no conformidad.',
        'هناك {$count} من ملفات CAPA غير مغلقة. أغلقها — بعد التحقق من فعاليتها — لتتمكن من إغلاق عدم المطابقة هذا.',
        'CAPA 案件が {$count} 件、未完了です。有効性を確認のうえ完了させると、この不適合を閉じられます。',
        '有 {$count} 份 CAPA 档案尚未关闭。在核实其有效性后关闭它们，方可关闭该不合格项。'),

    # --- code inconnu : serveur plus recent que l'ecran ---------------------
    'nc.blocker.unknown': (
        "Un prérequis n'est pas satisfait sur cet écart.",
        'A prerequisite is not met on this non-conformity.',
        'No se cumple un requisito previo en esta no conformidad.',
        'هناك شرط مسبق غير مستوفى على عدم المطابقة هذا.',
        'この不適合では前提条件が満たされていません。',
        '该不合格项的某项前提条件未满足。'),
}
