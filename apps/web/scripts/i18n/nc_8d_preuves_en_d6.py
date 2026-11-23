# -*- coding: utf-8 -*-
"""Table i18n - les preuves DU DOSSIER au D6, et la note qui les accompagne.

Le D6 du modele de traitement 8D porte un depot libre : ce qui demontre que le
PROBLEME a cesse -- releve, photo, rapport de controle. Cela ne remplace pas la
piece d'une ACTION, qui atteste que tel geste a bien ete fait et reste au D3,
sur sa ligne : le D5 et le D7 n'ont plus de colonne de pieces, parce que ce
qu'ils ont a prouver se prouve ici, sur le dossier.

S'y ajoute une note libre -- ni le critere, qui dit ce qu'on mesurera, ni le
verdict, qui est rendu et signe.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- le D6 sans dossier : rien a prouver tant que rien n'est decide ------
    'nc.8d.d6-no-case': (
        "Aucun dossier n'est encore ouvert sur cet écart : les preuves s'attachent au dossier, il faut donc commencer par en ouvrir un.",
        'No case is open on this non-conformity yet. Evidence is attached to the case, so start by opening one.',
        'Aún no hay ningún expediente abierto en esta no conformidad. Las pruebas se adjuntan al expediente, así que empiece por abrir uno.',
        'لا يوجد أي ملف مفتوح على عدم المطابقة هذا بعد. تُرفَق الأدلة بالملف، فابدأ بفتح ملف.',
        'この不適合にはまだ案件がありません。証拠は案件に添付するため、まず案件を一つ開いてください。',
        '该不合格项尚无任何案件。证据附在案件上，请先开立一个案件。'),

    # --- le D6 avec des dossiers d'ENDIGUEMENT SEUL -------------------------
    # La preuve d'un geste d'endiguement se depose sur sa ligne du D3, qui
    # porte une colonne pour cela. Le D6 demontre que le probleme a CESSE :
    # un endiguement n'a jamais pretendu traiter la cause.
    'nc.8d.d6-containment-only': (
        "Cet écart n'a que des actions d'endiguement. Leur preuve se joint sur leur ligne, au D3 : elle atteste que le geste a été fait. Le D6 démontre que le problème a cessé, ce qu'une action corrective ou préventive peut seule établir.",
        'This non-conformity has containment actions only. Their evidence is attached on their row, under D3: it shows the action was carried out. D6 shows that the problem has stopped, which only a corrective or preventive action can establish.',
        'Esta no conformidad solo tiene acciones de contención. Sus pruebas se adjuntan en su fila, en D3: acreditan que la acción se realizó. D6 demuestra que el problema ha cesado, algo que solo puede establecer una acción correctiva o preventiva.',
        'لا يتضمّن عدم المطابقة هذا سوى إجراءات احتواء. تُرفَق أدلّتها في سطرها ضمن D3: فهي تثبت أن الإجراء قد نُفّذ. أمّا D6 فيثبت أن المشكلة قد توقّفت، وهو ما لا يمكن أن يثبته إلا إجراء تصحيحي أو وقائي.',
        'この不適合には応急処置のみがあります。その証拠は D3 のその行に添付します――処置が実施されたことを示すためです。D6 は問題が止んだことを示すものであり、それを立証できるのは是正処置または予防処置だけです。',
        '该不合格项仅有围堵措施。其证据附在 D3 对应行上：用以证明该措施已执行。D6 证明问题已停止，而这只有纠正或预防措施才能确立。'),

    # --- la note libre, a cote des pieces -----------------------------------
    'nc.8d.d6-comment': (
        "Commentaire sur l'efficacité", 'Comment on effectiveness',
        'Comentario sobre la eficacia', 'تعليق على الفعالية',
        '有効性に関するコメント', '有效性说明'),
    'nc.8d.d6-comment-placeholder': (
        'Résultats de vérification, période de suivi…',
        'Verification results, follow-up period…',
        'Resultados de la verificación, periodo de seguimiento…',
        'نتائج التحقق، فترة المتابعة…',
        '検証結果、追跡期間など…',
        '验证结果、跟踪周期……'),
    'nc.8d.d6-comment-hint': (
        'Facultatif. Enregistré en quittant le champ.',
        'Optional. Saved when you leave the field.',
        'Opcional. Se guarda al salir del campo.',
        'اختياري. يُحفظ عند مغادرة الحقل.',
        '任意です。フィールドを離れると保存されます。',
        '可选。离开输入框时保存。'),
    'nc.8d.d6-comment-failed': (
        "Le commentaire n'a pas été enregistré. Réessayez en quittant le champ.",
        'The comment was not saved. Try again when you leave the field.',
        'El comentario no se ha guardado. Inténtelo de nuevo al salir del campo.',
        'لم يتم حفظ التعليق. أعد المحاولة عند مغادرة الحقل.',
        'コメントを保存できませんでした。フィールドを離れるときに再試行してください。',
        '说明未保存，请在离开输入框时重试。'),
}

# --- les preuves DU DOSSIER, deposees au D6 -------------------------------
# Elles ne remplacent pas les pieces des actions : une piece d'action atteste
# que CE geste-la a ete fait et reste au D3 ; ici on demontre que le probleme
# a cesse (ISO 9001 §10.2).
TRANSLATIONS.update({
    'capa.evidence.none': (
        "Aucune preuve jointe. Un dossier clos sans preuve s'affirme efficace sans le démontrer.",
        'No evidence attached. A case closed without evidence claims to be effective without showing it.',
        'No hay pruebas adjuntas. Un expediente cerrado sin pruebas se declara eficaz sin demostrarlo.',
        'لا توجد أدلة مرفقة. ملف يُغلق دون دليل يدّعي الفعالية دون إثباتها.',
        '証拠が添付されていません。証拠のないまま閉じた案件は、有効性を示さずに主張することになります。',
        '未附任何证据。无证据即结案，等于宣称有效却不予证明。'),
    'capa.evidence.load-error': (
        "Les preuves n'ont pas pu être lues.", 'The evidence could not be loaded.',
        'No se han podido leer las pruebas.', 'تعذّرت قراءة الأدلة.',
        '証拠を読み込めませんでした。', '无法读取证据。'),
    'capa.evidence.remove-failed': (
        "La preuve n'a pas été retirée.", 'The evidence was not removed.',
        'La prueba no se ha retirado.', 'لم تتم إزالة الدليل.',
        '証拠を削除できませんでした。', '证据未被移除。'),
    'capa.evidence.full': (
        "Dix preuves, c'est la limite : retirez-en une pour en joindre une autre.",
        'Ten pieces of evidence is the limit: remove one to attach another.',
        'Diez pruebas es el límite: retire una para adjuntar otra.',
        'عشرة أدلة هي الحد الأقصى: أزل واحدًا لإرفاق آخر.',
        '証拠は10件までです。別の証拠を添付するには、一つ削除してください。',
        '最多十项证据：请先移除一项再添加。'),
    'capa.evidence.remove-aria': (
        'Retirer la preuve {$nom}', 'Remove evidence {$nom}',
        'Retirar la prueba {$nom}', 'إزالة الدليل {$nom}',
        '証拠 {$nom} を削除', '移除证据 {$nom}'),
})
