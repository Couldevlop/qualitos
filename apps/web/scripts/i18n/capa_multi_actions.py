# -*- coding: utf-8 -*-
"""Table i18n - plusieurs actions par dossier CAPA (ADR 0115).

Un ecart se traite rarement d'un seul geste : on endigue, on corrige la cause,
on previent la recidive -- et le 8D nomme ces etapes separement (D3, D5, D6).
Le tableau les montre toutes ; la fenetre d'une action sert a l'ajouter comme a
la corriger.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- le tableau ---------------------------------------------------------
    'capa.actions.add': (
        'Ajouter une action', 'Add action', 'Anadir una accion',
        'إضافة إجراء', '対策を追加', '添加措施'),
    'capa.actions.caption': (
        'Actions de ce dossier CAPA', 'Actions of this CAPA case',
        'Acciones de este expediente CAPA', 'إجراءات ملف CAPA هذا',
        'この CAPA 案件の対策', '本 CAPA 案卷的措施'),
    'capa.actions.row-hint': (
        "Cliquez une ligne pour corriger l'action, son responsable ou son echeance.",
        'Click a row to correct the action, its owner or its due date.',
        'Haga clic en una fila para corregir la accion, su responsable o su plazo.',
        'انقر على سطر لتصحيح الإجراء أو مسؤوله أو تاريخ استحقاقه.',
        '行をクリックすると、対策・担当者・期限を修正できます。',
        '点击某一行可修改措施、负责人或期限。'),
    'capa.actions.open-aria': (
        "Ouvrir l'action : {$titre}", 'Open the action: {$titre}',
        'Abrir la accion: {$titre}', 'فتح الإجراء: {$titre}',
        '対策を開く: {$titre}', '打开措施：{$titre}'),

    # --- la fenetre d'une action --------------------------------------------
    'capa.action-dialog.title-add': (
        'Ajouter une action', 'Add an action', 'Anadir una accion',
        'إضافة إجراء', '対策を追加', '添加措施'),
    'capa.action-dialog.title-edit': (
        "Modifier l'action", 'Edit the action', 'Modificar la accion',
        'تعديل الإجراء', '対策を編集', '修改措施'),
    'capa.action-dialog.subtitle': (
        "Ce qu'on va faire, de quelle nature, qui en repond et pour quand.",
        'What will be done, of what nature, who answers for it and by when.',
        'Lo que se hara, de que naturaleza, quien responde y para cuando.',
        'ما سيُنجَز، وطبيعته، ومن يتحمل مسؤوليته، وبحلول متى.',
        '何をするか、どの性質か、誰が責任を負い、いつまでか。',
        '要做什么、属于哪种性质、由谁负责、何时完成。'),
    'capa.action-dialog.read-only': (
        'Ce dossier est termine : son action se lit, elle ne se modifie plus.',
        'This case is closed: its action can be read, not changed.',
        'Este expediente esta terminado: su accion se lee, ya no se modifica.',
        'هذا الملف منتهٍ: يمكن قراءة إجرائه، لا تعديله.',
        'この案件は終了しています。対策は閲覧のみで、変更できません。',
        '本案卷已结束：其措施仅可查看，不可修改。'),
    'capa.action-dialog.added': (
        'Action ajoutee.', 'Action added.', 'Accion anadida.',
        'تمت إضافة الإجراء.', '対策を追加しました。', '措施已添加。'),
    'capa.action-dialog.saved': (
        'Action enregistree.', 'Action saved.', 'Accion guardada.',
        'تم حفظ الإجراء.', '対策を保存しました。', '措施已保存。'),
    'capa.action-dialog.failed': (
        "L'action n'a pas pu etre enregistree.", 'The action could not be saved.',
        'No se pudo guardar la accion.', 'لم يتمكن النظام من حفظ الإجراء.',
        '対策を保存できませんでした。', '无法保存该措施。'),

    # --- la fenetre de ce qui reste OUVERT, ouverte depuis la fiche d'ecart --
    # Elle portait le tableau complet, que la fiche porte desormais elle-meme :
    # son titre ne dit donc plus « Actions correctives » mais ce qu'elle montre.
    'capa.actions-dialog.open-title': (
        'Actions ouvertes', 'Open actions', 'Acciones abiertas',
        'الإجراءات المفتوحة', '未完了の対策', '未完成措施'),
    'capa.actions-dialog.hint': (
        "Ce qui reste a faire sur cet ecart. Une ligne ouvre le dossier CAPA "
        "qui la porte, ou l'action s'ecrit, s'avance et se prouve.",
        'What remains to be done on this nonconformity. A row opens the CAPA '
        'case that carries it, where the action is written, advanced and proven.',
        'Lo que queda por hacer en esta desviacion. Una fila abre el expediente '
        'CAPA que la contiene, donde la accion se escribe, avanza y se prueba.',
        'ما تبقى من عمل على هذا الانحراف. يفتح السطر ملف CAPA الذي يحمله، حيث '
        'يُكتب الإجراء ويُتابع ويُثبت.',
        'この不適合に残っている作業です。行を開くと、その対策を持つ CAPA 案件'
        'に移動し、そこで対策を記述・進行・証明できます。',
        '此不合格尚待完成的工作。点击某行可打开承载该措施的 CAPA 案卷，'
        '在那里编写、推进并证明该措施。'),
    'capa.actions-dialog.caption': (
        "Actions ouvertes de cet ecart", 'Open actions of this nonconformity',
        'Acciones abiertas de esta desviacion', 'الإجراءات المفتوحة لهذا الانحراف',
        'この不適合の未完了の対策', '此不合格的未完成措施'),
    'capa.actions-dialog.none': (
        "Aucune action ouverte sur cet ecart. Les actions terminees se lisent "
        "en bas de la fiche, dans le tableau des actions correctives.",
        'No open action on this nonconformity. Completed actions are listed at '
        'the bottom of the record, in the corrective actions table.',
        'Ninguna accion abierta en esta desviacion. Las acciones terminadas se '
        'leen al final de la ficha, en la tabla de acciones correctivas.',
        'لا توجد إجراءات مفتوحة على هذا الانحراف. تُقرأ الإجراءات المنتهية في '
        'أسفل البطاقة، في جدول الإجراءات التصحيحية.',
        'この不適合に未完了の対策はありません。完了した対策は、記録の下部に'
        'ある是正対策の表で確認できます。',
        '此不合格没有未完成的措施。已完成的措施见记录下方的纠正措施表。'),
    'capa.actions-dialog.go-to-case': (
        'Ouvrir le dossier', 'Open the case', 'Abrir el expediente',
        'فتح الملف', '案件を開く', '打开案卷'),
    'capa.actions-dialog.open-aria': (
        "Ouvrir le dossier CAPA qui porte l'action : {$titre}",
        'Open the CAPA case that carries the action: {$titre}',
        'Abrir el expediente CAPA que contiene la accion: {$titre}',
        'فتح ملف CAPA الذي يحمل الإجراء: {$titre}',
        '対策を持つ CAPA 案件を開く：{$titre}',
        '打开承载该措施的 CAPA 案卷：{$titre}'),
    'capa.actions-dialog.failed': (
        "Les actions du dossier n'ont pas pu etre lues.",
        'The actions of the case could not be read.',
        'No se pudieron leer las acciones del expediente.',
        'لم يتمكن النظام من قراءة إجراءات الملف.',
        '案件の対策を読み取れませんでした。', '无法读取案卷的措施。'),

    # --- le bouton d'actions de l'en-tete d'une fiche d'ecart ---------------
    # Ils etaient DEUX pour le meme geste (« Ajouter des actions » et « Ajouter
    # une CAPA ») ; il n'en reste qu'un, et son libelle suit l'etat : sans
    # dossier il n'y a rien a lire, et la premiere action s'ecrit avec le
    # dossier qui la portera.
    'nc.detail.new-action': (
        'Nouvelle action', 'New action', 'Nueva accion',
        'إجراء جديد', '新しい対策', '新建措施'),
    'nc.detail.view-actions': (
        'Voir les actions', 'View actions', 'Ver las acciones',
        'عرض الإجراءات', '対策を見る', '查看措施'),
    'nc.detail.add-actions-tooltip': (
        "Les actions correctives de cet ecart : les lire, en ajouter",
        'The corrective actions of this nonconformity: read them, add some',
        'Las acciones correctivas de esta desviacion: leerlas, anadir alguna',
        'الإجراءات التصحيحية لهذا الانحراف: اقرأها أو أضف إليها',
        'この不適合の是正対策：閲覧し、追加する',
        '此不合格的纠正措施：查看或添加'),

    # --- un tableau restreint a une nature (D5, D6 du rapport 8D) -----------
    'capa.actions.none-of-nature': (
        "Aucune action de cette nature n'a encore été écrite pour cet écart.",
        'No action of this nature has been written for this nonconformity yet.',
        'Todavia no se ha escrito ninguna accion de esta naturaleza para esta desviacion.',
        'لم يُكتب بعد أي إجراء من هذا النوع لهذا الانحراف.',
        'この不適合について、この種別の対策はまだ記載されていません。',
        '尚未为此不合格编写此类措施。'),

    # --- le rapport 8D montre aussi l'etat vivant du dossier ----------------
    'nc.8d.live-note': (
        "État actuel du dossier, affiché à côté du texte agrégé. Le PDF, lui, ne porte que le texte figé à l'émission.",
        'Current state of the case, shown next to the aggregated text. The PDF carries only the text frozen at issuance.',
        'Estado actual del expediente, mostrado junto al texto agregado. El PDF solo contiene el texto congelado en la emision.',
        'الحالة الحالية للملف، معروضة بجانب النص المجمع. أما ملف PDF فلا يحمل سوى النص المجمّد عند الإصدار.',
        '案件の現在の状態を、集約されたテキストの横に表示しています。PDF には発行時に固定されたテキストのみが記載されます。',
        '案卷的当前状态，显示在汇总文本旁。PDF 仅包含签发时冻结的文本。'),
}
