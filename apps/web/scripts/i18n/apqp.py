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
        'Modifier le livrable {$PH}', 'Edit deliverable {$PH}',
        'Editar el entregable {$PH}', 'تعديل المُخرج {$PH}',
        '成果物「{$PH}」を編集', '编辑交付物 {$PH}'),
    'apqp.delete-deliverable-aria': (
        'Supprimer le livrable {$PH}', 'Delete deliverable {$PH}',
        'Eliminar el entregable {$PH}', 'حذف المُخرج {$PH}',
        '成果物「{$PH}」を削除', '删除交付物 {$PH}'),
    'apqp.move-earlier': (
        "Avancer la phase d'un rang", 'Move the phase one rank earlier',
        'Adelantar la fase un puesto', 'تقديم المرحلة مرتبة واحدة',
        'フェーズを1つ前へ', '将阶段前移一位'),
    'apqp.move-later': (
        "Reculer la phase d'un rang", 'Move the phase one rank later',
        'Retrasar la fase un puesto', 'تأخير المرحلة مرتبة واحدة',
        'フェーズを1つ後へ', '将阶段后移一位'),
    'apqp.confirm-delete-phase': (
        'Supprimer « {$PH} » et ses {$PH_1} livrables ? Cette suppression est définitive.',
        'Delete “{$PH}” and its {$PH_1} deliverables? This cannot be undone.',
        '¿Eliminar «{$PH}» y sus {$PH_1} entregables? Es definitivo.',
        'حذف «{$PH}» و{$PH_1} من مُخرجاتها؟ هذا الحذف نهائي.',
        '「{$PH}」とその成果物 {$PH_1} 件を削除しますか。取り消せません。',
        '删除“{$PH}”及其 {$PH_1} 项交付物？此操作不可撤销。'),
    'apqp.confirm-delete-deliverable': (
        'Retirer « {$PH} » des livrables ?',
        'Remove “{$PH}” from the deliverables?',
        '¿Retirar «{$PH}» de los entregables?',
        'إزالة «{$PH}» من المُخرجات؟',
        '「{$PH}」を成果物から外しますか。',
        '将“{$PH}”从交付物中移除？'),
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

    # --- livrables cochables, prouvables, et leur dossier PPAP (ADR 0068) -----
    'apqp.reset': (
        'Réinitialiser depuis le référentiel', 'Reset from the reference',
        'Restablecer desde el referencial', 'إعادة التعيين من المرجع',
        '標準リストから再設定', '从参考清单重置'),
    'apqp.confirm-reset': (
        'Remplacer votre cycle par celui du référentiel ? Vos phases, vos livrables et les pièces qui les prouvent seront définitivement perdus.',
        'Replace your cycle with the reference one? Your phases, your deliverables and the files proving them will be lost for good.',
        '¿Sustituir su ciclo por el del referencial? Sus fases, sus entregables y los archivos que los prueban se perderán definitivamente.',
        'هل تستبدل دورتك بدورة المرجع؟ ستُفقد مراحلك ومُخرَجاتك والملفات التي تُثبتها نهائيًا.',
        'ご自身のサイクルを標準リストで置き換えますか？フェーズ、成果物、それを証明するファイルは完全に失われます。',
        '要用参考清单替换您的循环吗？您的阶段、交付物及其证明文件将被永久删除。'),
    'apqp.toggle-deliverable-aria': (
        'Déclarer « {$label} » acquis', 'Declare “{$label}” obtained',
        'Declarar «{$label}» obtenido', 'إعلان إنجاز «{$label}»',
        '「{$label}」を取得済みとする', '声明“{$label}”已取得'),
    'apqp.ppap-mark-aria': (
        'Élément du dossier PPAP', 'Part of the PPAP file',
        'Elemento del expediente PPAP', 'عنصر من ملف PPAP',
        'PPAP 提出資料の構成要素', 'PPAP 文件的组成部分'),
    'apqp.evidence-count-aria': (
        '{$count} pièce(s) jointe(s)', '{$count} attached file(s)',
        '{$count} archivo(s) adjunto(s)', '{$count} ملف مرفق',
        '添付ファイル {$count} 件', '{$count} 个附件'),

    'apqp.ppap.title': (
        'Dossier PPAP', 'PPAP file', 'Expediente PPAP', 'ملف PPAP',
        'PPAP 提出資料', 'PPAP 文件'),
    'apqp.ppap.legend': (
        "* Les livrables marqués d'un astérisque dans le référentiel composent le dossier remis au client.",
        '* Deliverables marked with an asterisk in the reference make up the file handed to the customer.',
        '* Los entregables marcados con asterisco en el referencial componen el expediente entregado al cliente.',
        '* المُخرَجات المُعلَّمة بنجمة في المرجع تُشكِّل الملف المُقدَّم إلى العميل.',
        '※ 標準リストでアスタリスクが付いた成果物が、顧客に提出する資料を構成します。',
        '* 参考清单中带星号的交付物构成提交给客户的文件。'),
    'apqp.ppap.progress': (
        '{$done} livrables acquis sur {$total}', '{$done} deliverables obtained out of {$total}',
        '{$done} entregables obtenidos de {$total}', '{$done} مُخرَجات مُنجَزة من {$total}',
        '取得済み成果物 {$done} 件 / {$total} 件', '已取得 {$done} 项交付物，共 {$total} 项'),
    'apqp.ppap.progress-aria': (
        'Complétude du dossier PPAP : {$percent} %', 'PPAP file completeness: {$percent} %',
        'Integridad del expediente PPAP: {$percent} %', 'اكتمال ملف PPAP: {$percent} %',
        'PPAP 提出資料の充足度：{$percent} %', 'PPAP 文件完成度：{$percent} %'),
    'apqp.ppap.row-done': (
        '{$label} — acquis', '{$label} — obtained', '{$label} — obtenido',
        '{$label} — مُنجَز', '{$label} — 取得済み', '{$label} — 已取得'),
    'apqp.ppap.row-pending': (
        '{$label} — à fournir', '{$label} — to be supplied',
        '{$label} — pendiente de entrega', '{$label} — يجب تقديمه',
        '{$label} — 未提出', '{$label} — 待提供'),

    'apqp.deliverable.is-ppap': (
        'Ce livrable compose le dossier PPAP.', 'This deliverable is part of the PPAP file.',
        'Este entregable forma parte del expediente PPAP.', 'هذا المُخرَج جزء من ملف PPAP.',
        'この成果物は PPAP 提出資料の一部です。', '该交付物属于 PPAP 文件。'),
    'apqp.deliverable.done': (
        'Livrable acquis', 'Deliverable obtained', 'Entregable obtenido',
        'مُخرَج مُنجَز', '成果物を取得済み', '交付物已取得'),
    'apqp.deliverable.done-at': (
        'Déclaré acquis le', 'Declared obtained on', 'Declarado obtenido el',
        'أُعلن إنجازه في', '取得宣言日', '声明取得于'),
    'apqp.deliverable.comment': (
        'Commentaire', 'Comment', 'Comentario', 'تعليق', 'コメント', '备注'),
    'apqp.deliverable.comment-placeholder': (
        'Ex. : reçu par courriel le 3 septembre, version 2',
        'E.g. received by email on 3 September, version 2',
        'Ej.: recibido por correo el 3 de septiembre, versión 2',
        'مثال: وردت بالبريد في 3 سبتمبر، الإصدار 2',
        '例：9月3日にメールで受領、第2版', '例如：9 月 3 日通过邮件收到，第 2 版'),
    'apqp.deliverable.read-only': (
        'Vous pouvez consulter ce livrable, pas le modifier.',
        'You can view this deliverable, not change it.',
        'Puede consultar este entregable, pero no modificarlo.',
        'يمكنك الاطلاع على هذا المُخرَج دون تعديله.',
        'この成果物は閲覧のみ可能で、変更はできません。', '您可以查看该交付物，但不能修改。'),
    'apqp.deliverable.blocked-link': (
        "Désignez l'enregistrement avant de déclarer ce livrable acquis.",
        'Point to the record before declaring this deliverable obtained.',
        'Indique el registro antes de declarar el entregable obtenido.',
        'حدِّد السجل قبل إعلان إنجاز هذا المُخرَج.',
        '成果物を取得済みとする前に、対象レコードを指定してください。',
        '在声明该交付物已取得之前，请先指定对应记录。'),
    'apqp.deliverable.blocked-rows': (
        "Chaque ligne a besoin d'un intitulé.", 'Every row needs a label.',
        'Cada línea necesita un título.', 'كل سطر يحتاج إلى عنوان.',
        '各行には名称が必要です。', '每一行都需要名称。'),
    'apqp.deliverable.failed': (
        'Opération impossible sur ce livrable.',
        'That operation is not possible on this deliverable.',
        'Operación imposible en este entregable.', 'تعذَّرت العملية على هذا المُخرَج.',
        'この成果物に対する操作は実行できません。', '无法对该交付物执行此操作。'),

    'apqp.evidence.title': (
        'Pièces jointes', 'Attachments', 'Archivos adjuntos', 'المرفقات',
        '添付ファイル', '附件'),
    'apqp.evidence.empty': (
        "Aucune pièce pour l'instant — le livrable n'est pas encore prouvé.",
        'No file yet — the deliverable is not proven.',
        'Aún no hay archivos: el entregable no está probado.',
        'لا يوجد ملف بعد — المُخرَج غير مُثبَت.',
        'ファイルがまだありません。成果物は未証明です。', '尚无文件 — 该交付物尚未得到证明。'),
    'apqp.evidence.add': (
        'Joindre un document', 'Attach a document', 'Adjuntar un documento',
        'إرفاق مستند', '文書を添付', '添加文档'),
    'apqp.evidence.limits': (
        'Word, Excel, PDF ou image — 10 Mo par fichier, 5 par livrable.',
        'Word, Excel, PDF or image — 10 MB per file, 5 per deliverable.',
        'Word, Excel, PDF o imagen: 10 MB por archivo, 5 por entregable.',
        'Word أو Excel أو PDF أو صورة — 10 ميغابايت للملف، 5 ملفات للمُخرَج.',
        'Word・Excel・PDF・画像 — 1ファイル10MB、成果物あたり5件まで。',
        'Word、Excel、PDF 或图片 — 每个文件 10 MB，每个交付物 5 个。'),
    'apqp.evidence.added': (
        'Pièce versée au livrable.', 'File attached to the deliverable.',
        'Archivo adjuntado al entregable.', 'أُضيف الملف إلى المُخرَج.',
        '成果物にファイルを追加しました。', '文件已附加到交付物。'),
    'apqp.evidence.remove-aria': (
        'Retirer la pièce {$filename}', 'Remove the file {$filename}',
        'Eliminar el archivo {$filename}', 'إزالة الملف {$filename}',
        'ファイル {$filename} を削除', '删除文件 {$filename}'),
    'apqp.evidence.confirm-delete': (
        'Retirer « {$filename} » de ce livrable ?',
        'Remove “{$filename}” from this deliverable?',
        '¿Eliminar «{$filename}» de este entregable?',
        'إزالة «{$filename}» من هذا المُخرَج؟',
        '「{$filename}」をこの成果物から削除しますか？', '要将“{$filename}”从该交付物中移除吗？'),

    'apqp.link.kind': (
        'Module concerné', 'Module concerned', 'Módulo correspondiente',
        'الوحدة المعنية', '対象モジュール', '相关模块'),
    'apqp.link.id': (
        "Identifiant de l'enregistrement", 'Record identifier',
        'Identificador del registro', 'مُعرِّف السجل', 'レコード識別子', '记录标识符'),
    'apqp.link.id-hint': (
        "Copié depuis l'adresse de la fiche : c'est lui qui fait le lien.",
        'Copied from the record’s address: this is what makes the link.',
        'Copiado de la dirección de la ficha: es lo que crea el enlace.',
        'منسوخ من عنوان السجل: وهو ما يُنشئ الرابط.',
        'レコードの URL からコピーします。これが紐付けの要です。',
        '从记录地址中复制：正是它建立了关联。'),
    'apqp.link.id-invalid': (
        "Cet identifiant n'a pas la forme attendue.",
        'This identifier is not in the expected form.',
        'Este identificador no tiene la forma esperada.',
        'هذا المُعرِّف ليس بالصيغة المتوقعة.',
        'この識別子は想定された形式ではありません。', '该标识符格式不正确。'),
    'apqp.link.fmea': (
        'AMDEC (DFMEA / PFMEA)', 'FMEA (DFMEA / PFMEA)', 'AMFE (DFMEA / PFMEA)',
        'تحليل أنماط الفشل (DFMEA / PFMEA)', 'FMEA（DFMEA / PFMEA）',
        'FMEA（DFMEA / PFMEA）'),
    'apqp.link.control-plan': (
        'Plan de surveillance', 'Control plan', 'Plan de control', 'خطة المراقبة',
        'コントロールプラン', '控制计划'),
    'apqp.link.pdca': (
        'Cycle PDCA', 'PDCA cycle', 'Ciclo PDCA', 'دورة PDCA', 'PDCA サイクル',
        'PDCA 循环'),
    'apqp.link.capa': (
        'Action corrective (CAPA)', 'Corrective action (CAPA)',
        'Acción correctiva (CAPA)', 'إجراء تصحيحي (CAPA)', '是正処置（CAPA）',
        '纠正措施（CAPA）'),

    'apqp.data.label': (
        'Indicateur', 'Indicator', 'Indicador', 'المؤشر', '指標', '指标'),
    'apqp.data.value': ('Valeur', 'Value', 'Valor', 'القيمة', '値', '数值'),
    'apqp.data.unit': ('Unité', 'Unit', 'Unidad', 'الوحدة', '単位', '单位'),
    'apqp.data.measured-at': (
        'Mesuré le', 'Measured on', 'Medido el', 'تاريخ القياس', '測定日', '测量日期'),
    'apqp.data.add-row': (
        'Ajouter un indicateur', 'Add an indicator', 'Añadir un indicador',
        'إضافة مؤشر', '指標を追加', '添加指标'),
    'apqp.data.remove-row': (
        'Retirer cette ligne', 'Remove this row', 'Eliminar esta línea',
        'إزالة هذا السطر', 'この行を削除', '删除该行'),
    'apqp.point.label': (
        'Point à acquitter', 'Point to acknowledge', 'Punto a validar',
        'بند يجب إقراره', '確認すべき項目', '待确认事项'),
    'apqp.point.add-row': (
        'Ajouter un point', 'Add a point', 'Añadir un punto', 'إضافة بند',
        '項目を追加', '添加事项'),

    'apqp.kind-help.attachment': (
        'Joignez le document qui prouve ce livrable — Word, Excel, PDF ou photo.',
        'Attach the document that proves this deliverable — Word, Excel, PDF or photo.',
        'Adjunte el documento que prueba el entregable: Word, Excel, PDF o foto.',
        'أرفِق المستند الذي يُثبت هذا المُخرَج — Word أو Excel أو PDF أو صورة.',
        'この成果物を証明する文書を添付してください（Word・Excel・PDF・写真）。',
        '请附上证明该交付物的文档 — Word、Excel、PDF 或照片。'),
    'apqp.kind-help.module-link': (
        "Ce livrable est déjà tenu dans un module de QualitOS : désignez l'enregistrement concerné.",
        'This deliverable already lives in a QualitOS module: point to the record concerned.',
        'Este entregable ya existe en un módulo de QualitOS: indique el registro correspondiente.',
        'هذا المُخرَج موجود أصلًا في إحدى وحدات QualitOS: حدِّد السجل المعني.',
        'この成果物は QualitOS の別モジュールで管理されています。該当レコードを指定してください。',
        '该交付物已存在于 QualitOS 的某个模块中：请指定对应记录。'),
    'apqp.kind-help.data-entry': (
        'Saisissez les valeurs mesurées, avec leur unité et leur date.',
        'Enter the measured values, with their unit and date.',
        'Introduzca los valores medidos, con su unidad y fecha.',
        'أدخِل القيم المقيسة مع وحدتها وتاريخها.',
        '測定値を、単位と日付とともに入力してください。', '请输入测量值及其单位和日期。'),
    'apqp.kind-help.checklist': (
        "Ce livrable n'est acquis que si tous ses points le sont.",
        'This deliverable is obtained only once all its points are.',
        'El entregable solo se obtiene cuando todos sus puntos lo están.',
        'لا يُعدّ هذا المُخرَج مُنجَزًا إلا بإنجاز جميع بنوده.',
        'この成果物は、すべての項目が満たされて初めて取得となります。',
        '仅当所有事项均完成时，该交付物才算取得。'),
    'apqp.kind.attachment': (
        'Un document à joindre', 'A document to attach', 'Un documento para adjuntar',
        'مستند يُرفَق', '添付する文書', '需附加的文档'),
    'apqp.kind.module-link': (
        'Un enregistrement déjà tenu dans QualitOS',
        'A record already held in QualitOS', 'Un registro que ya existe en QualitOS',
        'سجل موجود أصلًا في QualitOS', 'QualitOS で既に管理されているレコード',
        'QualitOS 中已有的记录'),
    'apqp.kind.data-entry': (
        'Des mesures à saisir', 'Measurements to enter', 'Mediciones para introducir',
        'قياسات تُدخَل', '入力する測定値', '需填写的测量值'),
    'apqp.kind.checklist': (
        'Une liste de points à acquitter', 'A list of points to acknowledge',
        'Una lista de puntos a validar', 'قائمة بنود يجب إقرارها',
        '確認項目のリスト', '待确认事项清单'),
    'apqp.deliverable-dialog.field-kind': (
        'Ce que ce livrable produit', 'What this deliverable produces',
        'Lo que produce este entregable', 'ما يُنتجه هذا المُخرَج',
        'この成果物が生み出すもの', '该交付物产出什么'),
    'apqp.deliverable-dialog.kind-hint': (
        'Détermine ce que le livrable demandera : une pièce jointe, un renvoi, des mesures ou des points à cocher.',
        'Sets what the deliverable will ask for: an attachment, a record reference, measurements, or points to tick.',
        'Determina lo que pedirá el entregable: un archivo adjunto, una referencia a un registro, mediciones o puntos para marcar.',
        'يحدِّد ما سيطلبه المُخرَج: مرفقًا، أو إشارة إلى سجل، أو قياسات، أو بنودًا تُؤشَّر.',
        '成果物が何を求めるかを決めます：添付ファイル、レコード参照、測定値、チェック項目。',
        '决定该交付物将要求什么：附件、记录引用、测量值或勾选事项。'),
    'apqp.deliverable-dialog.ppap': (
        'Ce livrable compose le dossier PPAP', 'This deliverable is part of the PPAP file',
        'Este entregable forma parte del expediente PPAP', 'هذا المُخرَج جزء من ملف PPAP',
        'この成果物は PPAP 提出資料の一部です', '该交付物属于 PPAP 文件'),
}
