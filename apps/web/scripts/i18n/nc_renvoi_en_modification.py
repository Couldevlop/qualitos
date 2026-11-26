# -*- coding: utf-8 -*-
"""Table i18n - le second regard renvoie le traitement en modification.

La personne qui valide un ecart resolu n'avait qu'une reponse : clore. Si
l'action corrective ne repondait pas a la cause, elle n'avait aucun geste pour
le dire -- on telephonait au pilote, et le registre gardait un ecart « resolu »
que personne ne cloturait.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    'nc.detail.send-back': (
        'Renvoyer', 'Send back', 'Devolver',
        'إعادة', '差し戻す', '退回'),
    'nc.detail.send-back-tooltip': (
        "Rendre le traitement à son pilote : l'écart redevient modifiable",
        'Hand the treatment back to its owner: the finding becomes editable again',
        'Devolver el tratamiento a su responsable: la desviación vuelve a ser editable',
        'إعادة المعالجة إلى مسؤولها: تصبح المخالفة قابلة للتعديل مرة أخرى',
        '処理を担当者に差し戻します。不適合は再び編集可能になります',
        '将处理退回给负责人：该不符合项重新可编辑'),
    'nc.detail.banner-sent-back': (
        'Traitement renvoyé au pilote le', 'Treatment sent back to its owner on',
        'Tratamiento devuelto a su responsable el', 'أُعيدت المعالجة إلى مسؤولها في',
        '処理を担当者に差し戻した日', '处理退回负责人日期'),
    'nc.send-back.title': (
        'Renvoyer en modification', 'Send back for modification',
        'Devolver para modificación', 'إعادة للتعديل', '修正のために差し戻す', '退回修改'),
    'nc.send-back.submit': (
        'Renvoyer', 'Send back', 'Devolver', 'إعادة', '差し戻す', '退回'),
    'nc.send-back.hint': (
        'Le traitement de', 'The treatment of', 'El tratamiento de',
        'معالجة', '次の処理', '以下的处理'),
    'nc.send-back.hint-end': (
        " revient à son pilote, qui pourra tout reprendre — analyse de cause,"
        " actions, preuves, rapport 8D — puis déclarer l'écart résolu à nouveau."
        " Rien n'est effacé : la résolution précédente reste datée et signée.",
        ' goes back to its owner, who can revisit everything — root-cause analysis,'
        ' actions, evidence, 8D report — then declare the finding resolved again.'
        ' Nothing is erased: the previous resolution stays dated and signed.',
        ' vuelve a su responsable, que podrá revisar todo — análisis de causa,'
        ' acciones, pruebas, informe 8D — y volver a declarar la desviación resuelta.'
        ' Nada se borra: la resolución anterior queda fechada y firmada.',
        ' تعود إلى مسؤولها، الذي يمكنه مراجعة كل شيء — تحليل السبب والإجراءات'
        ' والأدلة وتقرير 8D — ثم إعلان حل المخالفة من جديد.'
        ' لا يُمحى شيء: يبقى الحل السابق مؤرخًا وموقعًا.',
        'は担当者に戻ります。原因分析、処置、証跡、8D報告書のすべてを見直し、'
        '改めて是正済みと宣言できます。消えるものはなく、前回の是正は日付と署名が残ります。',
        '退回给负责人，他可以重新审视全部内容——原因分析、措施、证据、8D 报告——'
        '然后再次宣布不符合项已解决。不会删除任何内容：上一次的解决仍保留日期与签署。'),
    'nc.send-back.message-label': (
        'Ce qui reste à revoir (facultatif)', 'What still needs work (optional)',
        'Lo que queda por revisar (opcional)', 'ما يجب مراجعته (اختياري)',
        '見直しが必要な点（任意）', '仍需复核的内容（可选）'),
    'nc.send-back.message-placeholder': (
        "L'action corrective ne répond pas à la cause racine, la preuve"
        " d'efficacité manque…",
        'The corrective action does not address the root cause, the effectiveness'
        ' evidence is missing…',
        'La acción correctiva no responde a la causa raíz, falta la prueba de'
        ' eficacia…',
        'الإجراء التصحيحي لا يعالج السبب الجذري، دليل الفعالية مفقود…',
        '是正処置が根本原因に対応していない、有効性の証跡がない…',
        '纠正措施未针对根本原因，缺少有效性证据……'),
    'nc.send-back.message-hint': (
        'Sans message, le renvoi reste possible — mais le pilote devra deviner ce'
        ' qui ne va pas.',
        'Without a message the send-back still works — but the owner will have to'
        ' guess what is wrong.',
        'Sin mensaje la devolución sigue siendo posible — pero el responsable'
        ' tendrá que adivinar qué falla.',
        'بدون رسالة تبقى الإعادة ممكنة — لكن على المسؤول أن يخمّن ما الخطأ.',
        'メッセージがなくても差し戻せます。ただし担当者は何が問題かを推測することになります。',
        '不填写说明也可退回，但负责人只能自己猜问题在哪。'),
    'nc.send-back.success': (
        "Traitement renvoyé au pilote : l'écart redevient modifiable.",
        'Treatment sent back to its owner: the finding is editable again.',
        'Tratamiento devuelto a su responsable: la desviación vuelve a ser editable.',
        'أُعيدت المعالجة إلى مسؤولها: أصبحت المخالفة قابلة للتعديل.',
        '処理を担当者に差し戻しました。不適合は再び編集可能です。',
        '处理已退回负责人：该不符合项重新可编辑。'),
    'nc.send-back.error': (
        'Renvoi impossible.', 'Send-back failed.', 'No se puede devolver.',
        'الإعادة غير ممكنة.', '差し戻せません。', '无法退回。'),
}
