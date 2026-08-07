# -*- coding: utf-8 -*-
"""Table i18n - plan d'actions d'un diagramme Ishikawa (SS3.5). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'ishikawa.actions.title': ("Plan d'actions", 'Action plan', 'Plan de acciones', 'خطة الإجراءات', 'アクションプラン', '行动计划'),
    'ishikawa.actions.subtitle': (
        "Ce qui a été décidé devant ce diagramme : qui fait quoi, et depuis quand.",
        'What was decided in front of this diagram: who does what, and since when.',
        'Lo que se decidió ante este diagrama: quién hace qué y desde cuándo.',
        'ما تقرّر أمام هذا المخطط: من يفعل ماذا، ومنذ متى.',
        'この図の前で決まったこと：誰が何を、いつから。',
        '在此图前作出的决定：谁做什么，从何时起。'),
    'ishikawa.actions.col-action': ('Action', 'Action', 'Acción', 'الإجراء', 'アクション', '行动'),
    'ishikawa.actions.col-responsible': ('Responsable', 'Owner', 'Responsable', 'المسؤول', '担当者', '负责人'),
    'ishikawa.actions.col-decided': ('Date de décision', 'Decision date', 'Fecha de decisión', 'تاريخ القرار', '決定日', '决定日期'),
    'ishikawa.actions.add': ('Ajouter', 'Add', 'Añadir', 'إضافة', '追加', '添加'),
    'ishikawa.actions.empty': (
        "Aucune action décidée pour l'instant.", 'No action decided yet.',
        'Aún no se ha decidido ninguna acción.', 'لم يُتخذ أي إجراء بعد.',
        'まだ決定されたアクションはありません。', '尚未决定任何行动。'),
    'ishikawa.actions.todo': ('À faire', 'To do', 'Por hacer', 'للتنفيذ', '未着手', '待办'),
    'ishikawa.actions.in-progress': ('En cours', 'In progress', 'En curso', 'قيد التنفيذ', '進行中', '进行中'),
    'ishikawa.actions.done': ('Fait', 'Done', 'Hecho', 'منجز', '完了', '已完成'),
    'ishikawa.actions.edit-tooltip': ("Modifier l'intitulé", 'Edit the wording', 'Editar el enunciado', 'تعديل الصياغة', '文言を編集', '编辑措辞'),
    'ishikawa.actions.delete-tooltip': ("Supprimer l'action", 'Delete the action', 'Eliminar la acción', 'حذف الإجراء', 'アクションを削除', '删除行动'),
    'ishikawa.actions.label-aria': ("Intitulé de l'action", 'Action wording', 'Enunciado de la acción', 'صياغة الإجراء', 'アクションの文言', '行动措辞'),
    'ishikawa.actions.responsible-aria': ("Responsable de l'action", 'Action owner', 'Responsable de la acción', 'مسؤول الإجراء', 'アクション担当者', '行动负责人'),
    'ishikawa.actions.decided-aria': ('Date de décision', 'Decision date', 'Fecha de decisión', 'تاريخ القرار', '決定日', '决定日期'),
    'ishikawa.actions.status-aria': ("Statut de l'action", 'Action status', 'Estado de la acción', 'حالة الإجراء', 'アクションの状態', '行动状态'),
    'ishikawa.actions.label-required': (
        "L'intitulé de l'action est obligatoire.", 'The action wording is required.',
        'El enunciado de la acción es obligatorio.', 'صياغة الإجراء إلزامية.',
        'アクションの文言は必須です。', '行动措辞为必填项。'),
    'ishikawa.actions.load-failed': (
        "Impossible de charger le plan d'actions.", 'Could not load the action plan.',
        'No se pudo cargar el plan de acciones.', 'تعذّر تحميل خطة الإجراءات.',
        'アクションプランを読み込めませんでした。', '无法加载行动计划。'),
    'ishikawa.actions.add-failed': (
        "Ajout de l'action refusé.", 'Adding the action was refused.',
        'Se rechazó añadir la acción.', 'تم رفض إضافة الإجراء.',
        'アクションの追加は拒否されました。', '添加行动被拒绝。'),
    'ishikawa.actions.update-failed': ('Modification refusée.', 'Change refused.', 'Modificación rechazada.', 'تم رفض التعديل.', '変更は拒否されました。', '修改被拒绝。'),
    'ishikawa.actions.delete-failed': ('Suppression refusée.', 'Deletion refused.', 'Eliminación rechazada.', 'تم رفض الحذف.', '削除は拒否されました。', '删除被拒绝。'),
}
