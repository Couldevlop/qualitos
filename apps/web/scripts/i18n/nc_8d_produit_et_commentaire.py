# -*- coding: utf-8 -*-
"""Table i18n - le produit au D2, la note d'analyse au D4.

Deux champs du modele de traitement 8D que l'ecran ne portait pas.

Le produit se CHOISIT dans le referentiel : une reference retapee a la main ne
rattacherait l'ecart a rien, ni au PFMEA du produit, ni a son plan de
surveillance. La fiche gardait son identifiant depuis toujours et ne l'affichait
jamais -- on ne savait donc pas, en lisant un ecart, sur quel produit il portait.

La note du D4 porte ce qui ne tient dans aucun outil -- l'hypothese ecartee, et
pourquoi on l'a ecartee. C'est ce qu'un auditeur demande quand il conteste la
cause retenue, et cela s'ecrivait dans la description de l'ecart, ou plus
personne ne le rapprochait de l'analyse.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- le produit, au D2 --------------------------------------------------
    'nc.8d.product': (
        'Produit / référence', 'Product / reference', 'Producto / referencia',
        'المنتج / المرجع', '製品／品番', '产品／编号'),

    # --- la note libre, au D4 -----------------------------------------------
    'nc.8d.d4-comment': (
        'Commentaires', 'Comments', 'Comentarios', 'تعليقات', 'コメント', '说明'),
    'nc.8d.d4-comment-placeholder': (
        'Observations, hypothèses écartées, cause racine retenue…',
        'Observations, discarded hypotheses, root cause retained…',
        'Observaciones, hipótesis descartadas, causa raíz retenida…',
        'ملاحظات، فرضيات مستبعدة، السبب الجذري المعتمد…',
        '観察、除外した仮説、採用した根本原因など…',
        '观察、已排除的假设、采纳的根本原因……'),
    'nc.8d.d4-comment-hint': (
        'Facultatif. Enregistré en quittant le champ.',
        'Optional. Saved when you leave the field.',
        'Opcional. Se guarda al salir del campo.',
        'اختياري. يُحفظ عند مغادرة الحقل.',
        '任意です。フィールドを離れると保存されます。',
        '可选。离开输入框时保存。'),
    'nc.8d.d4-comment-failed': (
        "Le commentaire n'a pas été enregistré. Réessayez en quittant le champ.",
        'The comment was not saved. Try again when you leave the field.',
        'El comentario no se ha guardado. Inténtelo de nuevo al salir del campo.',
        'لم يتم حفظ التعليق. أعد المحاولة عند مغادرة الحقل.',
        'コメントを保存できませんでした。フィールドを離れるときに再試行してください。',
        '说明未保存，请在离开输入框时重试。'),
}
