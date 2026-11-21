# -*- coding: utf-8 -*-
"""Table i18n - le bandeau qui dit ce qui manque avant de clore un ecart.

Le bandeau enoncait les motifs qui retiennent un ecart et s'arretait la :
« 2 dossiers CAPA ne sont pas clotures » laissait chercher lesquels dans un
registre qui en compte des centaines. Les liens y sont desormais, et une
derniere ligne dit le cas ou ce n'est pas l'ecart qui resiste, mais le droit qui
manque a celui qui lit -- jusqu'ici le bouton etait simplement absent, et rien
ne distinguait « il n'y a plus rien a faire » de « ce geste n'est pas le votre ».

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- le bandeau : le droit qui manque a celui qui lit -------------------
    'nc.blocker.no-right-resolve': (
        "Vous n'avez pas le droit de résoudre un écart. Demandez à un responsable qualité de porter ce geste.",
        'You are not allowed to resolve a non-conformity. Ask a quality manager to carry out this step.',
        'No tiene permiso para resolver una no conformidad. Pida a un responsable de calidad que lo haga.',
        'ليست لديك صلاحية حل حالة عدم مطابقة. اطلب من مسؤول الجودة القيام بذلك.',
        '不適合を解決する権限がありません。品質責任者に依頼してください。',
        '您没有解决不合格项的权限，请让质量负责人执行该操作。'),
    'nc.blocker.no-right-close': (
        "Vous n'avez pas le droit de clôturer un écart. Demandez à un responsable qualité de porter ce geste.",
        'You are not allowed to close a non-conformity. Ask a quality manager to carry out this step.',
        'No tiene permiso para cerrar una no conformidad. Pida a un responsable de calidad que lo haga.',
        'ليست لديك صلاحية إغلاق حالة عدم مطابقة. اطلب من مسؤول الجودة القيام بذلك.',
        '不適合を閉じる権限がありません。品質責任者に依頼してください。',
        '您没有关闭不合格项的权限，请让质量负责人执行该操作。'),

    # --- ce qu'on attend d'une action nommee sous son dossier --------------
    'nc.blocker.action-to-do': (
        "à mener", 'to carry out', 'por ejecutar', 'قيد التنفيذ',
        '未実施', '待执行'),
    'nc.blocker.action-to-verify': (
        "faite, son efficacité reste à vérifier",
        'done, its effectiveness still has to be verified',
        'realizada, falta verificar su eficacia',
        'منفّذ، ولم يتم التحقق من فعاليته بعد',
        '実施済み、有効性の検証が未了',
        '已执行，有效性尚待验证'),
    'nc.blocker.actions-count-one': (
        "1 action le retient", '1 action is holding it', '1 acción lo retiene',
        'إجراء واحد يحجزه', '1 件の処置が保留しています', '1 项措施仍未完成'),
    'nc.blocker.actions-count-many': (
        "{$count} actions le retiennent", '{$count} actions are holding it',
        '{$count} acciones lo retienen', '{$count} إجراءات تحجزه',
        '{$count} 件の処置が保留しています', '{$count} 项措施仍未完成'),
    'nc.blocker.action-pending': (
        "en attente", 'pending', 'pendiente', 'قيد الانتظار', '保留中', '待处理'),
}
