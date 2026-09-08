# -*- coding: utf-8 -*-
"""Libellés APQP — l'interface seulement.

Les phases et leurs livrables ne sont PAS ici : ils appartiennent désormais au
client et vivent en base, amorcés par `ApqpReference.java` côté serveur. Ce qui
motivait déjà leur mise à l'écart vaut toujours — un livrable normatif traduit
librement n'est plus le même livrable, « Control Plan » désignant un document
précis et non un plan de contrôle quelconque. Seule la coquille est traduite.
"""

TRANSLATIONS = {
    'nav.apqp': ('APQP', 'APQP', 'APQP', 'APQP', 'APQP', 'APQP'),
    'nav.apqp-cycle': (
        'Le cycle', 'The cycle', 'El ciclo', 'الدورة', 'サイクル', '流程总览'),

    'apqp.eyebrow': ('APQP', 'APQP', 'APQP', 'APQP', 'APQP', 'APQP'),
    'apqp.title': (
        'Planification qualité produit', 'Advanced product quality planning',
        'Planificación avanzada de la calidad', 'التخطيط المتقدم لجودة المنتج',
        '先行製品品質計画', '产品质量先期策划'),
    'apqp.subtitle': (
        "Les phases se lisent en V : on descend jusqu'au point bas du projet, on remonte vers la production série.",
        'The phases read as a V: down to the low point of the project, then back up to serial production.',
        'Las fases se leen en V: se baja hasta el punto bajo del proyecto y se remonta a la producción en serie.',
        'تُقرأ المراحل على شكل V: نزولاً إلى أدنى نقطة في المشروع ثم صعوداً إلى الإنتاج المتسلسل.',
        'フェーズはV字に読みます。プロジェクトの底まで下り、量産へと上がります。',
        '各阶段呈 V 形：下行至项目低点，再上行至量产。'),
    'apqp.cycle-aria': (
        'Cycle APQP', 'APQP cycle', 'Ciclo APQP',
        'دورة APQP', 'APQP サイクル', 'APQP 流程'),
    'apqp.pick-a-phase': (
        "Choisissez une phase du schéma pour voir ce qu'elle établit et ce qu'elle doit produire.",
        'Pick a phase on the diagram to see what it establishes and must produce.',
        'Elija una fase del esquema para ver qué establece y qué debe producir.',
        'اختر مرحلة من المخطط لمعرفة ما تُرسيه وما يجب أن تُنتجه.',
        '図からフェーズを選ぶと、その目的と成果物が表示されます。',
        '在示意图中选择一个阶段，查看其确立的内容与应产出的交付物。'),
    'apqp.empty-cycle': (
        "Le cycle est vide. Ajoutez une première phase pour le redessiner.",
        'The cycle is empty. Add a first phase to redraw it.',
        'El ciclo está vacío. Añada una primera fase para volver a dibujarlo.',
        'الدورة فارغة. أضف مرحلة أولى لإعادة رسمها.',
        'サイクルが空です。最初のフェーズを追加して描き直してください。',
        '流程为空。添加第一个阶段以重新绘制。'),
    'apqp.deliverables-title': (
        'Livrables attendus', 'Expected deliverables', 'Entregables esperados',
        'المُخرجات المتوقعة', '想定される成果物', '预期交付物'),
    'apqp.deliverables-subtitle': (
        "Ce que la phase doit avoir produit avant de passer à la suivante.",
        'What the phase must have produced before moving on.',
        'Lo que la fase debe haber producido antes de pasar a la siguiente.',
        'ما يجب أن تنتجه المرحلة قبل الانتقال إلى التالية.',
        '次のフェーズに進む前に、この段階が生み出しておくべきもの。',
        '进入下一阶段前，本阶段必须完成的内容。'),
    'apqp.no-deliverable': (
        "Aucun livrable pour l'instant — la phase ne dit pas encore ce qu'elle doit produire.",
        "No deliverable yet — the phase does not say what it must produce.",
        'Ningún entregable por ahora: la fase aún no dice qué debe producir.',
        'لا مُخرجات بعد — لم تحدد المرحلة ما يجب أن تنتجه.',
        'まだ成果物がありません。この段階が何を生み出すかは未定です。',
        '暂无交付物 — 该阶段尚未说明应产出什么。'),

    # ---- Édition du cycle ----
    'apqp.add-phase': (
        'Ajouter une phase', 'Add a phase', 'Añadir una fase',
        'إضافة مرحلة', 'フェーズを追加', '添加阶段'),
    'apqp.add-deliverable': (
        'Ajouter un livrable', 'Add a deliverable', 'Añadir un entregable',
        'إضافة مُخرج', '成果物を追加', '添加交付物'),
    'apqp.edit-deliverable-aria': (
        'Modifier le livrable {$INTERPOLATION}', 'Edit deliverable {$INTERPOLATION}',
        'Editar el entregable {$INTERPOLATION}', 'تعديل المُخرج {$INTERPOLATION}',
        '成果物「{$INTERPOLATION}」を編集', '编辑交付物 {$INTERPOLATION}'),
    'apqp.delete-deliverable-aria': (
        'Supprimer le livrable {$INTERPOLATION}', 'Delete deliverable {$INTERPOLATION}',
        'Eliminar el entregable {$INTERPOLATION}', 'حذف المُخرج {$INTERPOLATION}',
        '成果物「{$INTERPOLATION}」を削除', '删除交付物 {$INTERPOLATION}'),
    'apqp.move-earlier': (
        "Avancer la phase d'un rang", 'Move the phase one rank earlier',
        'Adelantar la fase un puesto', 'تقديم المرحلة مرتبة واحدة',
        'フェーズを1つ前へ', '将阶段前移一位'),
    'apqp.move-later': (
        "Reculer la phase d'un rang", 'Move the phase one rank later',
        'Retrasar la fase un puesto', 'تأخير المرحلة مرتبة واحدة',
        'フェーズを1つ後へ', '将阶段后移一位'),
    'apqp.confirm-delete-phase': (
        'Supprimer « {$INTERPOLATION} » et ses {$INTERPOLATION_1} livrables ? Cette suppression est définitive.',
        'Delete “{$INTERPOLATION}” and its {$INTERPOLATION_1} deliverables? This cannot be undone.',
        '¿Eliminar «{$INTERPOLATION}» y sus {$INTERPOLATION_1} entregables? Es definitivo.',
        'حذف «{$INTERPOLATION}» و{$INTERPOLATION_1} من مُخرجاتها؟ هذا الحذف نهائي.',
        '「{$INTERPOLATION}」とその成果物 {$INTERPOLATION_1} 件を削除しますか。取り消せません。',
        '删除“{$INTERPOLATION}”及其 {$INTERPOLATION_1} 项交付物？此操作不可撤销。'),
    'apqp.confirm-delete-deliverable': (
        'Retirer « {$INTERPOLATION} » des livrables ?',
        'Remove “{$INTERPOLATION}” from the deliverables?',
        '¿Retirar «{$INTERPOLATION}» de los entregables?',
        'إزالة «{$INTERPOLATION}» من المُخرجات؟',
        '「{$INTERPOLATION}」を成果物から外しますか。',
        '将“{$INTERPOLATION}”从交付物中移除？'),
    'apqp.failed': (
        'Opération impossible sur le cycle APQP.', 'That APQP cycle operation failed.',
        'No se pudo realizar la operación sobre el ciclo APQP.',
        'تعذّر تنفيذ العملية على دورة APQP.',
        'APQP サイクルの操作を実行できませんでした。', '无法对 APQP 流程执行该操作。'),

    # ---- Dialogue « phase » ----
    'apqp.phase-dialog.title-create': (
        'Ajouter une phase', 'Add a phase', 'Añadir una fase',
        'إضافة مرحلة', 'フェーズを追加', '添加阶段'),
    'apqp.phase-dialog.title-edit': (
        'Modifier la phase', 'Edit the phase', 'Modificar la fase',
        'تعديل المرحلة', 'フェーズを編集', '编辑阶段'),
    'apqp.phase-dialog.field-title': (
        'Intitulé de la phase', 'Phase name', 'Nombre de la fase',
        'اسم المرحلة', 'フェーズ名', '阶段名称'),
    'apqp.phase-dialog.title-placeholder': (
        'Ex. : Conception du processus', 'E.g. Process design', 'Ej.: Diseño del proceso',
        'مثال: تصميم العملية', '例：工程設計', '例：过程设计'),
    'apqp.phase-dialog.title-required': (
        "L'intitulé est requis : c'est le seul texte que porte le schéma.",
        'The name is required: it is the only text the diagram carries.',
        'El nombre es obligatorio: es el único texto que muestra el esquema.',
        'الاسم مطلوب: فهو النص الوحيد الذي يحمله المخطط.',
        '名称は必須です。図に載る唯一の文字だからです。',
        '名称为必填项：它是示意图上唯一的文字。'),
    'apqp.phase-dialog.blocked-title': (
        "Donnez un intitulé : c'est ce que le schéma affiche.",
        'Give it a name: that is what the diagram shows.',
        'Póngale un nombre: es lo que muestra el esquema.',
        'أعطِ المرحلة اسماً: هذا ما يعرضه المخطط.',
        '名称を入力してください。図に表示されるのはこれです。',
        '请填写名称：示意图显示的正是它。'),
    'apqp.phase-dialog.field-purpose': (
        'Ce que la phase établit', 'What the phase establishes', 'Lo que establece la fase',
        'ما تُرسيه المرحلة', 'この段階が定めること', '该阶段确立的内容'),
    'apqp.phase-dialog.purpose-hint': (
        "Une phrase, pas un paragraphe — elle s'affiche sous l'intitulé.",
        'One sentence, not a paragraph — it shows under the name.',
        'Una frase, no un párrafo: se muestra bajo el nombre.',
        'جملة واحدة لا فقرة — تظهر أسفل الاسم.',
        '段落ではなく一文で。名称の下に表示されます。',
        '一句话，而非一段：它显示在名称下方。'),
    'apqp.phase-dialog.field-question': (
        'La question à laquelle elle répond', 'The question it answers',
        'La pregunta que responde', 'السؤال الذي تجيب عنه',
        'この段階が答える問い', '该阶段回答的问题'),
    'apqp.phase-dialog.question-hint': (
        "Ce qu'on se demande en entrant dans la phase, et ce que ses livrables tranchent.",
        'What you ask entering the phase, and what its deliverables settle.',
        'Lo que uno se pregunta al entrar en la fase y lo que zanjan sus entregables.',
        'ما نتساءل عنه عند بدء المرحلة، وما تحسمه مُخرجاتها.',
        'この段階に入るときの問いであり、成果物が決着させるもの。',
        '进入该阶段时的疑问，也是其交付物所要解答的。'),

    # ---- Dialogue « livrable » ----
    'apqp.deliverable-dialog.title-create': (
        'Ajouter un livrable', 'Add a deliverable', 'Añadir un entregable',
        'إضافة مُخرج', '成果物を追加', '添加交付物'),
    'apqp.deliverable-dialog.title-edit': (
        'Reformuler un livrable', 'Reword a deliverable', 'Reformular un entregable',
        'إعادة صياغة مُخرج', '成果物を書き直す', '重述交付物'),
    'apqp.deliverable-dialog.field-label': (
        'Livrable attendu', 'Expected deliverable', 'Entregable esperado',
        'المُخرج المتوقع', '想定される成果物', '预期交付物'),
    'apqp.deliverable-dialog.label-placeholder': (
        'Ex. : AMDEC processus (PFMEA)', 'E.g. Process FMEA (PFMEA)',
        'Ej.: AMFE de proceso (PFMEA)', 'مثال: تحليل أنماط الفشل للعملية (PFMEA)',
        '例：工程FMEA（PFMEA）', '例：过程 FMEA（PFMEA）'),
    'apqp.deliverable-dialog.label-hint': (
        "Ce que la phase doit avoir produit avant de passer à la suivante.",
        'What the phase must have produced before moving on.',
        'Lo que la fase debe haber producido antes de pasar a la siguiente.',
        'ما يجب أن تنتجه المرحلة قبل الانتقال إلى التالية.',
        '次のフェーズに進む前に、この段階が生み出しておくべきもの。',
        '进入下一阶段前，本阶段必须完成的内容。'),
    'apqp.deliverable-dialog.label-required': (
        'Un livrable sans intitulé ne se vérifie pas.',
        'A deliverable with no name cannot be checked.',
        'Un entregable sin nombre no se puede verificar.',
        'المُخرج بلا اسم لا يمكن التحقق منه.',
        '名称のない成果物は確認できません。',
        '没有名称的交付物无法核查。'),
    'apqp.deliverable-dialog.blocked-label': (
        'Nommez le livrable attendu.', 'Name the expected deliverable.',
        'Nombre el entregable esperado.', 'سمِّ المُخرج المتوقع.',
        '想定される成果物を入力してください。', '请填写预期交付物的名称。'),
}
