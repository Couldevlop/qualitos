# -*- coding: utf-8 -*-
"""Table i18n - le traitement d'un ecart, dans l'ordre des huit disciplines.

La fiche presentait les memes elements en cartes sans ordre : on y lisait
l'equipe, les photos, les actions, sans voir ce qui restait a faire ni dans
quel ordre. La trame 8D dit ou l'on en est -- et c'est la meme trame que le
rapport remis au client.

RIEN N'Y EST OBLIGATOIRE : une discipline vide n'empeche pas la suivante. Les
libelles invitent, ils ne reclament pas, et les traductions gardent ce ton.

S'y ajoutent la REPRISE d'un ecart referme et le SECOND REGARD sur sa cloture :
personne ne valide son propre travail, et la personne designee peut porter ce
regard sans avoir de role qualite.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {

    # ---------- le sommaire et les huit disciplines ----------

    'nc.8d.treatment-eyebrow': (
        "Traitement 8D · Équipe · Problème · Causes · Actions · Preuves",
        '8D treatment · Team · Problem · Causes · Actions · Evidence',
        'Tratamiento 8D · Equipo · Problema · Causas · Acciones · Pruebas',
        'معالجة 8D · الفريق · المشكلة · الأسباب · الإجراءات · الأدلة',
        '8D 処理 · チーム · 問題 · 原因 · 対策 · 証拠',
        '8D 处理 · 团队 · 问题 · 原因 · 措施 · 证据'),
    'nc.8d.step-filled': (
        'renseignée',
        'filled in',
        'rellenada',
        'مُستوفاة',
        '入力済み',
        '已填写'),
    'nc.8d.nav-label': (
        "Les huit disciplines",
        'The eight disciplines',
        'Las ocho disciplinas',
        'الانضباطات الثمانية',
        '8 つのディシプリン',
        '八个步骤'),

    'nc.8d.d1-short': ("Équipe", 'Team', 'Equipo', 'الفريق', 'チーム', '团队'),
    'nc.8d.d1-title': (
        "Constituer l’équipe",
        'Build the team',
        'Constituir el equipo',
        'تشكيل الفريق',
        'チームを編成する',
        '组建团队'),
    'nc.8d.d1-sub': (
        "Les personnes qui traiteront cet écart. Le premier nommé pilote.",
        'The people who will handle this non-conformity. The first one named leads.',
        'Las personas que tratarán esta no conformidad. La primera designada la pilota.',
        'الأشخاص الذين سيعالجون عدم المطابقة هذا. أول من يُسمّى يقود العمل.',
        'この不適合を担当する人。最初に挙げた人がリーダーになります。',
        '将处理该不合格项的人员。第一位即为负责人。'),

    'nc.8d.d2-short': ("Problème", 'Problem', 'Problema', 'المشكلة', '問題', '问题'),
    'nc.8d.d2-title': (
        "Décrire le problème",
        'Describe the problem',
        'Describir el problema',
        'وصف المشكلة',
        '問題を記述する',
        '描述问题'),
    'nc.8d.d2-sub': (
        "Quoi, où, quand, combien : restez factuel.",
        'What, where, when, how many: stick to the facts.',
        'Qué, dónde, cuándo, cuántos: limítese a los hechos.',
        'ماذا، وأين، ومتى، وكم: التزم بالوقائع.',
        '何が、どこで、いつ、いくつ ― 事実に徹してください。',
        '什么、何处、何时、多少：只写事实。'),

    'nc.8d.d3-short': ("Confinement", 'Containment', 'Contención', 'الاحتواء', '封じ込め', '围堵'),
    'nc.8d.d3-title': (
        "Actions de confinement",
        'Containment actions',
        'Acciones de contención',
        'إجراءات الاحتواء',
        '封じ込め対策',
        '围堵措施'),
    'nc.8d.d3-sub': (
        "Ce qui protège le client dès maintenant, avant la cause racine.",
        'What protects the customer right now, before the root cause is known.',
        'Lo que protege al cliente ahora mismo, antes de la causa raíz.',
        'ما يحمي العميل الآن، قبل معرفة السبب الجذري.',
        '根本原因の前に、いま顧客を守るもの。',
        '在查明根本原因之前，先保护客户的措施。'),
    # La premiere action de confinement OUVRE le dossier : la discipline ne
    # renvoie plus au D5, ou la nature se reposait en trois choix.
    'nc.8d.d3-no-case': (
        "Aucun dossier CAPA n’existe encore sur cet écart. La première action de"
        " confinement l’ouvrira : les suivantes s’ajouteront dans ce tableau.",
        'No CAPA case exists on this non-conformity yet. The first containment'
        ' action will open one; the next ones will be added to this table.',
        'Esta no conformidad aún no tiene expediente CAPA. La primera acción de'
        ' contención lo abrirá: las siguientes se añadirán a esta tabla.',
        'لا يوجد بعد ملف CAPA لعدم المطابقة هذا. أول إجراء احتواء'
        ' سيفتح ملفًا؛ وتُضاف الإجراءات التالية إلى هذا الجدول.',
        'この不適合にはまだ CAPA 案件がありません。最初の封じ込め対策が案件を開き、以降の対策はこの表に追加されます。',
        '该不合格项尚无 CAPA 档案。首条围堵措施将开立档案，其后的措施将添加到本表。'),

    'nc.8d.d4-short': ("Causes", 'Causes', 'Causas', 'الأسباب', '原因', '原因'),
    'nc.8d.d4-title': (
        "Analyse des causes",
        'Root cause analysis',
        'Análisis de las causas',
        'تحليل الأسباب',
        '原因の分析',
        '原因分析'),
    'nc.8d.d4-sub': (
        "Identifiez la cause racine avec les outils de votre choix.",
        'Find the root cause with whichever tool suits you.',
        'Identifique la causa raíz con las herramientas que prefiera.',
        'حدِّد السبب الجذري بالأدوات التي تختارها.',
        '好みの手法で根本原因を特定してください。',
        '用您选择的工具找出根本原因。'),

    'nc.8d.d5-short': ("Correctives", 'Corrective', 'Correctivas', 'تصحيحية', '是正', '纠正'),
    'nc.8d.d5-title': (
        "Actions correctives",
        'Corrective actions',
        'Acciones correctivas',
        'الإجراءات التصحيحية',
        '是正処置',
        '纠正措施'),
    'nc.8d.d5-sub': (
        "Ce qui supprime la cause racine.",
        'What removes the root cause.',
        'Lo que elimina la causa raíz.',
        'ما يُزيل السبب الجذري.',
        '根本原因を取り除くもの。',
        '消除根本原因的措施。'),

    'nc.8d.d6-short': ("Preuves", 'Evidence', 'Pruebas', 'الأدلة', '証拠', '证据'),
    'nc.8d.d6-title': (
        "Preuves des actions correctives",
        'Evidence of the corrective actions',
        'Pruebas de las acciones correctivas',
        'أدلة الإجراءات التصحيحية',
        '是正処置の証拠',
        '纠正措施的证据'),
    'nc.8d.d6-sub': (
        "Photos, mesures, rapports de contrôle qui démontrent l’efficacité.",
        'Photos, measurements and inspection reports that show it worked.',
        'Fotos, mediciones e informes de control que demuestran la eficacia.',
        'صور وقياسات وتقارير فحص تُثبت الفعالية.',
        '有効性を示す写真・測定値・検査報告。',
        '证明有效性的照片、测量值和检验报告。'),
    'nc.8d.d6-evidence-hint': (
        "Chaque action porte sa propre pièce, dans la colonne « Preuve » des"
        " tableaux D3, D5 et D7 : une pièce détachée de son action ne démontre"
        " rien.",
        'Each action carries its own file, in the “Evidence” column of the D3, D5'
        ' and D7 tables: a file detached from its action proves nothing.',
        'Cada acción lleva su propio documento, en la columna «Prueba» de las'
        ' tablas D3, D5 y D7: un documento separado de su acción no demuestra'
        ' nada.',
        'كل إجراء يحمل مُستنده في عمود «الدليل» من جداول D3 و D5 و D7: مستند منفصل'
        ' عن إجراءه لا يُثبت شيئًا.',
        '証拠は対策ごとに、D3・D5・D7 の表の「証拠」欄に添付します。対策から切り離された資料は何も証明しません。',
        '每项措施在 D3、D5、D7 表格的“证据”列各自附件：脱离措施的文件什么也证明不了。'),
    'nc.8d.d6-verdict': (
        "Juger l’efficacité",
        'Judge effectiveness',
        'Juzgar la eficacia',
        'الحكم على الفعالية',
        '有効性を判定する',
        '判定有效性'),

    'nc.8d.d7-short': ("Préventives", 'Preventive', 'Preventivas', 'وقائية', '予防', '预防'),
    'nc.8d.d7-title': (
        "Actions préventives",
        'Preventive actions',
        'Acciones preventivas',
        'الإجراءات الوقائية',
        '予防処置',
        '预防措施'),
    'nc.8d.d7-sub': (
        "Ce qui évite que le problème revienne ailleurs.",
        'What keeps the problem from coming back elsewhere.',
        'Lo que evita que el problema reaparezca en otro sitio.',
        'ما يمنع عودة المشكلة في مكان آخر.',
        '同じ問題が別の場所で起きないようにするもの。',
        '防止问题在别处重现的措施。'),
    'nc.8d.d7-no-case': (
        "Aucun dossier CAPA n’existe encore sur cet écart. La première action"
        " préventive l’ouvrira : les suivantes s’ajouteront dans ce tableau.",
        'No CAPA case exists on this non-conformity yet. The first preventive'
        ' action will open one; the next ones will be added to this table.',
        'Esta no conformidad aún no tiene expediente CAPA. La primera acción'
        ' preventiva lo abrirá: las siguientes se añadirán a esta tabla.',
        'لا يوجد بعد ملف CAPA لعدم المطابقة هذا. أول إجراء وقائي'
        ' سيفتح ملفًا؛ وتُضاف الإجراءات التالية إلى هذا الجدول.',
        'この不適合にはまだ CAPA 案件がありません。最初の予防処置が案件を開き、以降の対策はこの表に追加されます。',
        '该不合格项尚无 CAPA 档案。首条预防措施将开立档案，其后的措施将添加到本表。'),

    'nc.8d.d8-short': ("Remerciements", 'Recognition', 'Agradecimientos', 'الشكر', '謝辞', '致谢'),
    'nc.8d.d8-title': (
        "Remercier l’équipe",
        'Thank the team',
        'Agradecer al equipo',
        'شكر الفريق',
        'チームに感謝する',
        '感谢团队'),
    'nc.8d.d8-sub': (
        "Reconnaissez le travail accompli. Ce mot part dans le rapport 8D.",
        'Acknowledge the work done. These words go into the 8D report.',
        'Reconozca el trabajo realizado. Estas palabras van en el informe 8D.',
        'اعترف بالعمل المُنجز. هذه الكلمات تُرسل في تقرير 8D.',
        '成し遂げた仕事をねぎらってください。この言葉は 8D 報告書に入ります。',
        '肯定已完成的工作。这段话会写入 8D 报告。'),
    'nc.8d.d8-label': (
        "Message à l’équipe",
        'Message to the team',
        'Mensaje para el equipo',
        'رسالة إلى الفريق',
        'チームへのメッセージ',
        '致团队的话'),
    'nc.8d.d8-placeholder': (
        "Merci à toute l’équipe pour…",
        'Thanks to the whole team for…',
        'Gracias a todo el equipo por…',
        'شكرًا لكل الفريق على…',
        'チームの皆さん、…ありがとう',
        '感谢全体团队……'),
    'nc.8d.d8-none': (
        "Aucun mot de clôture n’a été écrit.",
        'No closing words were written.',
        'No se ha escrito ninguna palabra de cierre.',
        'لم تُكتب أي كلمة ختامية.',
        '締めの言葉は書かれていません。',
        '没有写结语。'),
    'nc.8d.d8-saved': (
        "Remerciements enregistrés dans le rapport 8D.",
        'Recognition saved in the 8D report.',
        'Agradecimientos guardados en el informe 8D.',
        'تم حفظ الشكر في تقرير 8D.',
        '謝辞を 8D 報告書に保存しました。',
        '致谢已保存到 8D 报告。'),
    'nc.8d.d8-error': (
        "Les remerciements n'ont pas pu être enregistrés.",
        'The recognition could not be saved.',
        'No se han podido guardar los agradecimientos.',
        'لم يتم حفظ الشكر.',
        '謝辞を保存できませんでした。',
        '致谢未能保存。'),

    'nc.8d.no-case': (
        "Aucun dossier CAPA sur cet écart. Ouvrez-en un : les actions de"
        " confinement, correctives et préventives s’écriront et s’avanceront ici"
        " même, chacune sous sa discipline.",
        'No CAPA case on this non-conformity. Open one: containment, corrective'
        ' and preventive actions will be written and carried forward right here,'
        ' each under its own discipline.',
        'Ningún expediente CAPA en esta no conformidad. Abra uno: las acciones de'
        ' contención, correctivas y preventivas se escribirán y avanzarán aquí'
        ' mismo, cada una en su disciplina.',
        'لا يوجد ملف CAPA لعدم المطابقة هذا. افتح ملفًا: ستُكتب إجراءات الاحتواء'
        ' والتصحيح والوقاية وتتقدّم هنا نفسه، كل منها تحت انضباطه.',
        'この不適合に CAPA 案件はありません。案件を開けば、封じ込め・是正・予防の各対策をここで書き、進められます。',
        '该不合格项没有 CAPA 档案。开立一份：围堵、纠正和预防措施都在此处书写和推进，各归其步骤。'),

    # ---------- le client et le lot ----------

    'nc.8d.customer': ("Client", 'Customer', 'Cliente', 'العميل', '顧客', '客户'),
    'nc.8d.customer-placeholder': (
        "Nom du client qui réclame",
        'Name of the customer complaining',
        'Nombre del cliente que reclama',
        'اسم العميل المُطالِب',
        '申し立てた顧客の名前',
        '提出投诉的客户名称'),
    'nc.8d.batch': ("N° de lot", 'Batch no.', 'N.º de lote', 'رقم الدفعة', 'ロット番号', '批号'),
    'nc.8d.batch-placeholder': (
        "Ex. L2611-08",
        'e.g. L2611-08',
        'Ej. L2611-08',
        'مثال: L2611-08',
        '例: L2611-08',
        '例：L2611-08'),
    'nc.8d.context-saved': (
        "Description enregistrée.",
        'Description saved.',
        'Descripción guardada.',
        'تم حفظ الوصف.',
        '記述を保存しました。',
        '描述已保存。'),
    'nc.8d.context-error': (
        "La description n'a pas pu être enregistrée.",
        'The description could not be saved.',
        'No se ha podido guardar la descripción.',
        'لم يتم حفظ الوصف.',
        '記述を保存できませんでした。',
        '描述未能保存。'),

    # ---------- le bandeau : ce qui est arrive a l'ecart ----------

    'nc.8d.banner-reopened': (
        "Écart repris le",
        'Non-conformity reopened on',
        'No conformidad reabierta el',
        'أُعيد فتح عدم المطابقة في',
        '不適合の再開日:',
        '不合格项重启于'),
    'nc.8d.banner-closed': (
        "Clôturé le", 'Closed on', 'Cerrada el', 'أُغلق في', '完了日:', '关闭于'),
    'nc.8d.banner-closed-by': (
        "par {$name}", 'by {$name}', 'por {$name}', 'بواسطة {$name}', '担当: {$name}', '由 {$name}'),
    'nc.8d.banner-rejected': (
        "Écarté le", 'Dismissed on', 'Rechazada el', 'رُفض في', '却下日:', '驳回于'),
    'nc.8d.banner-cancelled': (
        "Écart annulé.",
        'Non-conformity cancelled.',
        'No conformidad anulada.',
        'أُلغي عدم المطابقة.',
        '不適合は取り消されました。',
        '不合格项已取消。'),

    # ---------- la reprise d'un ecart referme ----------

    'nc.8d.reopen': ("Rouvrir", 'Reopen', 'Reabrir', 'إعادة الفتح', '再開', '重启'),
    'nc.8d.reopen-tooltip': (
        "Reprendre l'instruction : récidive, relance du client, constat écarté à tort",
        'Resume the investigation: recurrence, customer follow-up, a finding'
        ' dismissed by mistake',
        'Reanudar la instrucción: reincidencia, insistencia del cliente, hallazgo'
        ' rechazado por error',
        'استئناف التحقيق: تكرار، أو متابعة من العميل، أو ملاحظة رُفضت خطأً',
        '調査を再開する: 再発、顧客からの再連絡、誤って却下した所見',
        '恢复调查：问题重现、客户再次反馈、误被驳回的发现'),
    'nc.reopen.title': (
        "Reprendre cet écart",
        'Reopen this non-conformity',
        'Reabrir esta no conformidad',
        'إعادة فتح عدم المطابقة هذا',
        'この不適合を再開する',
        '重启该不合格项'),
    'nc.reopen.hint': (
        "L'instruction reprendra à son début, et tout ce qui a déjà été fait reste attaché à",
        'The investigation starts again from the beginning, and everything already'
        ' done stays attached to',
        'La instrucción se reanudará desde el principio, y todo lo ya hecho sigue'
        ' adjunto a',
        'سيبدأ التحقيق من جديد، ويبقى كل ما تم إنجازه مرتبطًا بـ',
        '調査は最初からやり直しますが、これまでの内容はすべて残ります:',
        '调查将从头开始，而已完成的一切仍附在'),
    'nc.reopen.hint-end': (
        " : équipe, analyse de cause, actions et preuves. Ce qui l'avait refermé"
        " n'est pas effacé.",
        ': team, root cause analysis, actions and evidence. What had closed it is'
        ' not erased.',
        ': equipo, análisis de causas, acciones y pruebas. Lo que la había cerrado'
        ' no se borra.',
        ': الفريق، وتحليل الأسباب، والإجراءات، والأدلة. وما أغلقه لا يُمحى.',
        ' ― チーム、原因分析、対策、証拠。閉じた記録も消えません。',
        '：团队、原因分析、措施和证据。此前关闭的记录不会被清除。'),
    'nc.reopen.reason-label': (
        "Motif de la reprise (facultatif)",
        'Reason for reopening (optional)',
        'Motivo de la reapertura (opcional)',
        'سبب إعادة الفتح (اختياري)',
        '再開の理由（任意）',
        '重启原因（可选）'),
    'nc.reopen.reason-placeholder': (
        "Récidive constatée sur le lot suivant, relance du client, constat écarté à tort…",
        'Recurrence on the next batch, customer follow-up, a finding dismissed by'
        ' mistake…',
        'Reincidencia en el lote siguiente, insistencia del cliente, hallazgo'
        ' rechazado por error…',
        'تكرار في الدفعة التالية، أو متابعة من العميل، أو ملاحظة رُفضت خطأً…',
        '次ロットでの再発、顧客からの再連絡、誤って却下した所見…',
        '下一批次重现、客户再次反馈、误被驳回的发现……'),
    'nc.reopen.reason-hint': (
        "Sans motif, la reprise reste possible — mais rien n'expliquera ce retour en arrière.",
        'Without a reason, reopening still works — but nothing will explain the'
        ' step back.',
        'Sin motivo, la reapertura sigue siendo posible, pero nada explicará ese'
        ' paso atrás.',
        'بدون سبب، تبقى إعادة الفتح ممكنة — لكن لن يُفسّر شيء هذه العودة.',
        '理由がなくても再開はできますが、この後戻りを説明するものは残りません。',
        '不填原因仍可重启，但没有任何记录能解释这次回退。'),
    'nc.reopen.submit': ("Reprendre", 'Reopen', 'Reanudar', 'استئناف', '再開する', '重启'),
    'nc.reopen.success': (
        "Écart repris : l'instruction reprend à son début.",
        'Non-conformity reopened: the investigation starts again.',
        'No conformidad reabierta: la instrucción se reanuda desde el principio.',
        'أُعيد فتح عدم المطابقة: يبدأ التحقيق من جديد.',
        '不適合を再開しました。調査をやり直します。',
        '不合格项已重启：调查从头开始。'),
    'nc.reopen.error': (
        "Reprise impossible.",
        'Cannot reopen.',
        'Reapertura imposible.',
        'إعادة الفتح غير ممكنة.',
        '再開できません。',
        '无法重启。'),

    # ---------- le second regard sur la cloture ----------

    'nc.8d.problem': (
        "Description du problème",
        'Problem description',
        'Descripción del problema',
        'وصف المشكلة',
        '問題の説明',
        '问题描述'),
    'nc.8d.problem-placeholder': (
        "Décrivez l'écart constaté, la quantité concernée, le lieu et le moment"
        " de la détection",
        'Describe the finding, the quantity involved, where and when it was'
        ' detected',
        'Describa la desviación observada, la cantidad afectada, el lugar y el'
        ' momento de la detección',
        'صف الانحراف الملاحظ والكمية المعنية ومكان الاكتشاف ووقته',
        '検出した不適合、対象数量、検出の場所と時刻を記載してください',
        '请描述所发现的偏差、涉及数量、发现的地点和时间'),
    'nc.8d.problem-required': (
        "Le constat ne peut pas rester vide.",
        'The finding cannot be left empty.',
        'La constatación no puede quedar vacía.',
        'لا يمكن ترك الملاحظة فارغة.',
        '所見を空欄のままにはできません。',
        '发现内容不能为空。'),
    'nc.8d.validator': (
        "Valideur de la clôture (second regard)",
        'Closure validator (second pair of eyes)',
        'Validador del cierre (segunda mirada)',
        'مُصادِق الإغلاق (المراجعة الثانية)',
        '完了の承認者（second pair of eyes）',
        '关闭审核人（第二双眼睛）'),
    'nc.8d.validator-hint': (
        "Facultatif. Cette personne pourra clôturer cet écart même sans rôle"
        " qualité.",
        'Optional. This person can close this non-conformity even without a'
        ' quality role.',
        'Opcional. Esta persona podrá cerrar esta no conformidad incluso sin rol'
        ' de calidad.',
        'اختياري. يمكن لهذا الشخص إغلاق عدم المطابقة هذا حتى بدون دور في الجودة.',
        '任意。この人は品質の役割がなくてもこの不適合を完了できます。',
        '可选。此人即使没有质量角色也可关闭该不合格项；若由其本人解决，则不可。'),
    'nc.8d.validator-saved': (
        "Valideur enregistré.",
        'Validator saved.',
        'Validador guardado.',
        'تم حفظ المُصادِق.',
        '承認者を保存しました。',
        '审核人已保存。'),
    'nc.8d.validator-failed': (
        "Le valideur n'a pas pu être enregistré.",
        'The validator could not be saved.',
        'No se ha podido guardar el validador.',
        'لم يتم حفظ المُصادِق.',
        '承認者を保存できませんでした。',
        '审核人未能保存。'),
    'nc.8d.closed-by': (
        "Clôturé par", 'Closed by', 'Cerrada por', 'أغلقه', '完了者', '关闭人'),
}
