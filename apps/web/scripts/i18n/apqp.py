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


    'apqp.eyebrow': ('APQP', 'APQP', 'APQP', 'APQP', 'APQP', 'APQP'),
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
        'Remplacer le cycle de ce projet par celui du référentiel ? Ses phases, ses livrables et les pièces qui les prouvent seront définitivement perdus.',
        'Replace this project’s cycle with the reference one? Its phases, its deliverables and the files proving them will be lost for good.',
        '¿Sustituir el ciclo de este proyecto por el del referencial? Sus fases, sus entregables y los archivos que los prueban se perderán definitivamente.',
        'هل تستبدل دورة هذا المشروع بدورة المرجع؟ ستُفقد مراحله ومُخرَجاته والملفات التي تُثبتها نهائيًا.',
        'このプロジェクトのサイクルを標準リストで置き換えますか？フェーズ、成果物、それを証明するファイルは完全に失われます。',
        '要用参考清单替换该项目的循环吗？其阶段、交付物及其证明文件将被永久删除。'),
    'apqp.toggle-deliverable-aria': (
        'Déclarer « {$label} » acquis', 'Declare “{$label}” obtained',
        'Declarar «{$label}» obtenido', 'إعلان إنجاز «{$label}»',
        '「{$label}」を取得済みとする', '声明“{$label}”已取得'),
    'apqp.ppap-mark-aria': (
        'Livrable requis au dossier PPAP', 'Deliverable required for the PPAP file',
        'Entregable requerido para el expediente PPAP', 'مُخرَج مطلوب لملف PPAP',
        'PPAP 提出資料に必要な成果物', 'PPAP 文件所需的交付物'),
    'apqp.evidence-count-aria': (
        '{$count} pièce(s) jointe(s)', '{$count} attached file(s)',
        '{$count} archivo(s) adjunto(s)', '{$count} ملف مرفق',
        '添付ファイル {$count} 件', '{$count} 个附件'),

    'apqp.ppap.title': (
        'Dossier PPAP', 'PPAP file', 'Expediente PPAP', 'ملف PPAP',
        'PPAP 提出資料', 'PPAP 文件'),
    # Forme neutre au nombre : « 1 deliverables obtained » accordait mal, et une
    # regle ICU pour un compteur de tableau de bord serait payer cher un pluriel.
    # Forme neutre au nombre : « 1 deliverables supplied » accordait mal, et une
    # regle ICU pour un compteur de tableau de bord serait payer cher un pluriel.
    'apqp.ppap.progress': (
        '{$done} / {$total} livrables requis fournis',
        '{$done} of {$total} required deliverables supplied',
        '{$done} de {$total} entregables requeridos entregados',
        '{$done} من {$total} مُخرَج مطلوب تم تقديمه',
        '提出済みの必須成果物 {$done} / {$total} 件',
        '已提供 {$done} / {$total} 项必需交付物'),
    'apqp.ppap.progress-aria': (
        'Complétude du dossier PPAP : {$percent} %', 'PPAP file completeness: {$percent} %',
        'Integridad del expediente PPAP: {$percent} %', 'اكتمال ملف PPAP: {$percent} %',
        'PPAP 提出資料の充足度：{$percent} %', 'PPAP 文件完成度：{$percent} %'),
    'apqp.ppap.row-done': (
        '{$label} — fourni', '{$label} — supplied', '{$label} — entregado',
        '{$label} — مُقدَّم', '{$label} — 提出済み', '{$label} — 已提供'),
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
        "Un renvoi se pose entier : le module ET son identifiant, ou ni l'un ni l'autre.",
        'A reference is set whole: the module AND its identifier, or neither.',
        'Una referencia se define entera: el módulo Y su identificador, o ninguno.',
        'تُحدَّد الإشارة كاملة: الوحدة ومُعرِّفها معًا، أو لا شيء.',
        '参照は一括で指定します。モジュールと識別子の両方、またはどちらも指定しません。',
        '引用需成对填写：模块与其标识符，或两者都不填。'),
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


    'apqp.deliverable-dialog.ppap': (
        'Ce livrable est requis au dossier PPAP',
        'This deliverable is required for the PPAP file',
        'Este entregable es requerido para el expediente PPAP',
        'هذا المُخرَج مطلوب لملف PPAP',
        'この成果物は PPAP 提出資料に必要です', '该交付物为 PPAP 文件所需'),
    'apqp.ppap.page-subtitle': (
        "Les livrables que vous avez marqués « requis au dossier PPAP » : ce qui compose le dossier remis au client.",
        'The deliverables you marked “required for the PPAP file”: what makes up the file handed to the customer.',
        'Los entregables que ha marcado «requeridos para el expediente PPAP»: lo que compone el expediente entregado al cliente.',
        'المُخرَجات التي وسمتَها «مطلوبة لملف PPAP»: ما يُشكِّل الملف المُقدَّم إلى العميل.',
        '「PPAP 提出資料に必要」と印を付けた成果物 — 顧客に提出する資料を構成します。',
        '您标记为「PPAP 文件所需」的交付物：构成提交给客户的文件。'),
    'apqp.ppap.empty': (
        "Aucun livrable de ce projet n'est marqué « requis au dossier PPAP ». Ouvrez un livrable du cycle pour l'y ajouter.",
        'No deliverable in this project is marked “required for the PPAP file”. Open a deliverable of the cycle to add one.',
        'Ningún entregable de este proyecto está marcado como «requerido para el expediente PPAP». Abra un entregable del ciclo para añadirlo.',
        'لا يوجد مُخرَج في هذا المشروع موسوم «مطلوب لملف PPAP». افتح مُخرَجًا من الدورة لإضافته.',
        'このプロジェクトには「PPAP 提出資料に必要」と印の付いた成果物がありません。サイクルの成果物を開いて追加してください。',
        '该项目中没有交付物被标记为「PPAP 文件所需」。请打开循环中的某个交付物进行添加。'),
    'apqp.link.open': (
        'Ouvrir la fiche', 'Open the record', 'Abrir la ficha',
        'فتح السجل', 'レコードを開く', '打开记录'),
    'apqp.link.no-route': (
        "Un plan de surveillance s'ouvre depuis son produit : il n'a pas d'adresse propre.",
        'A control plan opens from its product: it has no address of its own.',
        'Un plan de control se abre desde su producto: no tiene direccion propia.',
        'تُفتح خطة المراقبة من منتجها: ليس لها عنوان خاص بها.',
        'コントロールプランは製品から開きます。単独の URL はありません。',
        '控制计划从其产品页面打开，没有独立地址。'),

    # --- les projets APQP : la racine du module ------------------------------
    'nav.apqp-projects': (
        'Projets', 'Projects', 'Proyectos', 'المشاريع', 'プロジェクト', '项目'),
    'apqp.projects.title': (
        'APQP — Projets', 'APQP — Projects', 'APQP — Proyectos',
        'APQP — المشاريع', 'APQP — プロジェクト', 'APQP — 项目'),
    'apqp.projects.subtitle': (
        'Chaque projet porte son propre cycle en V, ses livrables et son dossier PPAP.',
        'Each project carries its own V cycle, its deliverables and its PPAP file.',
        'Cada proyecto tiene su propio ciclo en V, sus entregables y su expediente PPAP.',
        'يحمل كل مشروع دورته على شكل V ومُخرَجاته وملف PPAP الخاص به.',
        '各プロジェクトは、独自の V 字サイクル、成果物、PPAP 提出資料を持ちます。',
        '每个项目都有各自的 V 形流程、交付物和 PPAP 文件。'),
    'apqp.projects.new': (
        'Nouveau projet', 'New project', 'Nuevo proyecto', 'مشروع جديد',
        '新規プロジェクト', '新建项目'),
    'apqp.projects.col-deliverables': (
        'Livrables', 'Deliverables', 'Entregables', 'المُخرَجات', '成果物', '交付物'),
    'apqp.projects.col-ppap': (
        'Dossier PPAP', 'PPAP file', 'Expediente PPAP', 'ملف PPAP',
        'PPAP 提出資料', 'PPAP 文件'),
    'apqp.projects.empty': (
        "Aucun projet pour l'instant. Créez-en un : il partira du cycle du référentiel, que vous adapterez ensuite.",
        'No project yet. Create one: it starts from the reference cycle, which you then adapt.',
        'Aún no hay proyectos. Cree uno: partirá del ciclo del referencial, que luego adaptará.',
        'لا يوجد مشروع بعد. أنشئ مشروعًا: سينطلق من دورة المرجع التي تُكيِّفها بعد ذلك.',
        'プロジェクトがまだありません。作成すると標準リストのサイクルから始まり、以後は自由に調整できます。',
        '尚无项目。新建一个：它将从参考清单的循环开始，之后可自行调整。'),
    'apqp.projects.delete-aria': (
        'Supprimer le projet {$name}', 'Delete project {$name}',
        'Eliminar el proyecto {$name}', 'حذف المشروع {$name}',
        'プロジェクト「{$name}」を削除', '删除项目 {$name}'),
    'apqp.projects.confirm-delete': (
        'Supprimer le projet « {$name} » ? Son cycle, ses livrables et les pièces qui les prouvent seront définitivement perdus.',
        'Delete project “{$name}”? Its cycle, its deliverables and the files proving them will be lost for good.',
        '¿Eliminar el proyecto «{$name}»? Su ciclo, sus entregables y los archivos que los prueban se perderán definitivamente.',
        'حذف المشروع «{$name}»؟ ستُفقد دورته ومُخرَجاته والملفات التي تُثبتها نهائيًا.',
        'プロジェクト「{$name}」を削除しますか？そのサイクル、成果物、証明ファイルは完全に失われます。',
        '删除项目“{$name}”？其循环、交付物及其证明文件将被永久删除。'),

    'apqp.project.type.npi': (
        'NPI — nouveau produit', 'NPI — new product', 'NPI — nuevo producto',
        'NPI — منتج جديد', 'NPI — 新製品', 'NPI — 新产品'),
    'apqp.project.type.tow': (
        "ToW — transfert d'activité", 'ToW — transfer of work',
        'ToW — transferencia de actividad', 'ToW — نقل نشاط',
        'ToW — 業務移管', 'ToW — 业务转移'),
    'apqp.project.type.new-customer': (
        'Nouveau client', 'New customer', 'Nuevo cliente', 'عميل جديد',
        '新規顧客', '新客户'),
    'apqp.project.type.other': (
        'Autre', 'Other', 'Otro', 'أخرى', 'その他', '其他'),
    'apqp.project.customer': (
        'Client', 'Customer', 'Cliente', 'العميل', '顧客', '客户'),
    'apqp.project.reference': (
        'Référence', 'Reference', 'Referencia', 'المرجع', '参照番号', '编号'),

    'apqp.project-dialog.title-create': (
        'Nouveau projet APQP', 'New APQP project', 'Nuevo proyecto APQP',
        'مشروع APQP جديد', '新規 APQP プロジェクト', '新建 APQP 项目'),
    'apqp.project-dialog.title-edit': (
        'Modifier le projet', 'Edit the project', 'Modificar el proyecto',
        'تعديل المشروع', 'プロジェクトを編集', '修改项目'),
    'apqp.project-dialog.field-name': (
        'Nom du projet', 'Project name', 'Nombre del proyecto', 'اسم المشروع',
        'プロジェクト名', '项目名称'),
    'apqp.project-dialog.name-placeholder': (
        'Ex. : Support moteur — ligne 4', 'E.g. engine mount — line 4',
        'Ej.: soporte de motor — línea 4', 'مثال: حامل المحرك — الخط 4',
        '例：エンジンマウント — ライン 4', '例如：发动机支架 — 4 号线'),
    'apqp.project-dialog.name-required': (
        'Un projet sans nom ne se retrouve pas dans la liste.',
        'A project without a name cannot be found in the list.',
        'Un proyecto sin nombre no se encuentra en la lista.',
        'المشروع بلا اسم لا يُعثر عليه في القائمة.',
        '名称のないプロジェクトは一覧で見つけられません。', '没有名称的项目无法在列表中找到。'),
    'apqp.project-dialog.blocked-name': (
        "Donnez un nom au projet : c'est ce que la liste affiche.",
        'Give the project a name: it is what the list shows.',
        'Dé un nombre al proyecto: es lo que muestra la lista.',
        'أعطِ المشروع اسمًا: هو ما تعرضه القائمة.',
        'プロジェクトに名前を付けてください。一覧に表示されるのはこれです。',
        '请为项目命名：列表显示的正是它。'),
    'apqp.project-dialog.field-type': (
        'Type de projet', 'Project type', 'Tipo de proyecto', 'نوع المشروع',
        'プロジェクト種別', '项目类型'),
    'apqp.project-dialog.type-hint': (
        'Ce qui motive le projet : un lancement, un transfert, une ouverture de compte.',
        'What drives the project: a launch, a transfer, a new account.',
        'Lo que motiva el proyecto: un lanzamiento, una transferencia, una nueva cuenta.',
        'ما يُبرِّر المشروع: إطلاق أو نقل أو فتح حساب جديد.',
        'プロジェクトの動機：立ち上げ、移管、新規取引の開始。',
        '项目的缘由：新品导入、业务转移或开拓新客户。'),
    'apqp.project-dialog.reference-hint': (
        "La référence pièce ou le numéro d'affaire — celle que le client cite.",
        'The part reference or job number — the one the customer quotes.',
        'La referencia de pieza o el número de expediente: el que cita el cliente.',
        'مرجع القطعة أو رقم الصفقة — الذي يذكره العميل.',
        '部品番号または案件番号 — 顧客が使う番号です。',
        '零件编号或项目编号 — 客户所引用的那个。'),

    'apqp.back-to-projects': (
        'Retour aux projets', 'Back to projects', 'Volver a los proyectos',
        'العودة إلى المشاريع', 'プロジェクト一覧に戻る', '返回项目列表'),
    'apqp.back-to-project': (
        'Retour au projet', 'Back to the project', 'Volver al proyecto',
        'العودة إلى المشروع', 'プロジェクトに戻る', '返回项目'),

    # --- l'etat d'un livrable -------------------------------------------------
    'apqp.status.not-started': (
        'Non commencé', 'Not started', 'No iniciado', 'لم يبدأ', '未着手', '未开始'),
    'apqp.status.in-progress': (
        'En cours', 'In progress', 'En curso', 'قيد التنفيذ', '進行中', '进行中'),
    'apqp.status.blocked': (
        'Bloqué', 'Blocked', 'Bloqueado', 'مُعطَّل', '停滞', '受阻'),
    'apqp.status.done': (
        'Acquis', 'Obtained', 'Obtenido', 'مُنجَز', '取得済み', '已取得'),

    'apqp.deliverable.owner': (
        'Responsable', 'Owner', 'Responsable', 'المسؤول', '担当者', '负责人'),
    'apqp.deliverable.due-date': (
        'Échéance', 'Due date', 'Fecha límite', 'الموعد النهائي', '期限', '截止日期'),
    'apqp.deliverable.percent': (
        'Avancement (%)', 'Progress (%)', 'Progreso (%)', 'التقدم (%)',
        '進捗（%）', '进度（%）'),
    'apqp.deliverable.percent-range': (
        "L'avancement se compte de 0 à 100.", 'Progress runs from 0 to 100.',
        'El progreso va de 0 a 100.', 'يُحسب التقدم من 0 إلى 100.',
        '進捗は 0 から 100 までです。', '进度取值为 0 到 100。'),
    'apqp.deliverable.expected-artifact': (
        'Artefact attendu', 'Expected artefact', 'Artefacto esperado',
        'المُنتَج المتوقع', '想定される成果物', '预期产出物'),
    'apqp.deliverable.expected-artifact-placeholder': (
        'Ex. : plan de surveillance signé, au format tableur',
        'E.g. signed control plan, as a spreadsheet',
        'Ej.: plan de control firmado, en formato de hoja de cálculo',
        'مثال: خطة مراقبة موقَّعة بصيغة جدول بيانات',
        '例：署名済みのコントロールプラン（表計算形式）',
        '例如：已签署的控制计划，电子表格格式'),
    'apqp.deliverable.expected-artifact-hint': (
        "Ce que ce livrable doit produire, dit en clair : c'est ce qu'on vérifiera.",
        'What this deliverable must produce, plainly stated: that is what will be checked.',
        'Lo que debe producir este entregable, dicho con claridad: es lo que se verificará.',
        'ما يجب أن يُنتجه هذا المُخرَج بوضوح: هو ما سيُتحقَّق منه.',
        'この成果物が生み出すべきものを平易に。検証されるのはこれです。',
        '用平实的话写明该交付物应产出什么：这正是将被核查的内容。'),
    'apqp.deliverable.blocked-fields': (
        'Un champ du formulaire est hors limites.',
        'A field of the form is out of bounds.',
        'Un campo del formulario está fuera de límites.',
        'أحد حقول النموذج خارج الحدود المسموحة.',
        'フォームの項目が許容範囲を超えています。', '表单中有字段超出允许范围。'),

    'apqp.link.section': (
        'Renvoi vers un enregistrement', 'Reference to a record',
        'Referencia a un registro', 'إشارة إلى سجل',
        'レコードへの参照', '指向某条记录的引用'),
    'apqp.link.optional-hint': (
        'Facultatif. Les deux champs se posent ensemble, ou aucun des deux.',
        'Optional. Both fields are set together, or neither.',
        'Opcional. Ambos campos se rellenan juntos, o ninguno.',
        'اختياري. يُملأ الحقلان معًا أو لا يُملأ أيٌّ منهما.',
        '任意です。2 つの項目は両方指定するか、どちらも指定しません。',
        '可选。两个字段要么都填，要么都不填。'),
    'apqp.link.none': (
        'Aucun', 'None', 'Ninguno', 'لا شيء', 'なし', '无'),

    'apqp.deliverable-dialog.field-artifact': (
        'Artefact attendu', 'Expected artefact', 'Artefacto esperado',
        'المُنتَج المتوقع', '想定される成果物', '预期产出物'),
    'apqp.deliverable-dialog.artifact-placeholder': (
        'Ex. : tableur signé par le responsable méthodes',
        'E.g. spreadsheet signed by the methods engineer',
        'Ej.: hoja de cálculo firmada por el responsable de métodos',
        'مثال: جدول بيانات موقَّع من مسؤول الأساليب',
        '例：生産技術責任者が署名した表計算ファイル',
        '例如：由工艺负责人签署的电子表格'),
    'apqp.deliverable-dialog.artifact-hint': (
        "Facultatif. Ce que ce livrable doit produire concrètement : c'est ce qu'on vérifiera.",
        'Optional. What this deliverable must concretely produce: that is what will be checked.',
        'Opcional. Lo que debe producir concretamente este entregable: es lo que se verificará.',
        'اختياري. ما يجب أن يُنتجه هذا المُخرَج فعليًا: هو ما سيُتحقَّق منه.',
        '任意です。この成果物が具体的に生み出すもの。検証されるのはこれです。',
        '可选。该交付物具体应产出什么：这正是将被核查的内容。'),
}
