# -*- coding: utf-8 -*-
"""Table i18n - nature des actions CAPA et motifs de blocage de cloture (SS4.2).

Deux sujets liees : le type d'action (endiguement / corrective / preventive) et
les motifs qui empechent la cloture — dont « le dossier n'a fait qu'endiguer ».

Les motifs arrivent du serveur sous forme de CODE + DECOMPTE, jamais de phrase :
c'est ici que la phrase se construit, dans la langue de l'utilisateur.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- nature d'une action -------------------------------------------------
    'capa.action-type.containment': (
        'Endiguement', 'Containment', 'Contención',
        'احتواء', '暫定対策', '围堵措施'),
    'capa.action-type.corrective': (
        'Corrective', 'Corrective', 'Correctiva',
        'تصحيحي', '是正対策', '纠正措施'),
    'capa.action-type.preventive': (
        'Préventive', 'Preventive', 'Preventiva',
        'وقائي', '予防対策', '预防措施'),
    'capa.action.action-type': (
        "Nature de l'action", 'Kind of action', 'Naturaleza de la acción',
        'طبيعة الإجراء', '対策の種別', '措施类别'),

    # --- motifs de blocage de la cloture -------------------------------------
    'capa.detail.blockers-title': (
        'Ce dossier ne peut pas encore être clôturé',
        'This case cannot be closed yet',
        'Este expediente aún no puede cerrarse',
        'لا يمكن إغلاق هذا الملف بعد',
        'この案件はまだ完了できません',
        '该档案尚不能关闭'),
    'capa.blocker.no-action': (
        "Aucune action n'est enregistrée : il n'y a rien dont vérifier l'efficacité.",
        'No action is recorded: there is nothing whose effectiveness could be checked.',
        'No hay ninguna acción registrada: no hay nada cuya eficacia verificar.',
        'لا يوجد أي إجراء مسجَّل: لا شيء يمكن التحقق من فعاليته.',
        '対策が登録されていません。有効性を確認する対象がありません。',
        '尚未登记任何措施：没有可供验证有效性的对象。'),
    'capa.blocker.actions-one': (
        '1 action reste à terminer.',
        '1 action is still to be completed.',
        'Queda 1 acción por terminar.',
        'يتبقّى إجراء واحد لإتمامه.',
        '未完了の対策が 1 件あります。',
        '还有 1 项措施未完成。'),
    'capa.blocker.actions-many': (
        '{$count} actions restent à terminer.',
        '{$count} actions are still to be completed.',
        'Quedan {$count} acciones por terminar.',
        'يتبقّى {$count} إجراءات لإتمامها.',
        '未完了の対策が {$count} 件あります。',
        '还有 {$count} 项措施未完成。'),
    'capa.blocker.nc-one': (
        '1 non-conformité liée est encore ouverte.',
        '1 linked non-conformity is still open.',
        '1 no conformidad vinculada sigue abierta.',
        'ما زالت حالة عدم مطابقة مرتبطة واحدة مفتوحة.',
        '関連する不適合が 1 件未処理です。',
        '仍有 1 项关联不符合项未关闭。'),
    'capa.blocker.nc-many': (
        '{$count} non-conformités liées sont encore ouvertes.',
        '{$count} linked non-conformities are still open.',
        '{$count} no conformidades vinculadas siguen abiertas.',
        'ما زالت {$count} حالات عدم مطابقة مرتبطة مفتوحة.',
        '関連する不適合が {$count} 件未処理です。',
        '仍有 {$count} 项关联不符合项未关闭。'),
    'capa.blocker.unknown': (
        "Un prérequis de clôture n'est pas satisfait.",
        'A closing prerequisite is not met.',
        'No se cumple un requisito de cierre.',
        'أحد شروط الإغلاق غير مستوفٍ.',
        '完了の前提条件が満たされていません。',
        '关闭的某项前提条件未满足。'),

    # --- reference d'audit ---------------------------------------------------
    'audits.col-reference': (
        'Référence', 'Reference', 'Referencia', 'المرجع', '管理番号', '编号'),
}
