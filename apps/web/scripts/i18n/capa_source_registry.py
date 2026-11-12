# -*- coding: utf-8 -*-
"""Table i18n - la reference de la source d'une CAPA, choisie au registre.

« Non-conformite » tout court ne disait pas si l'ecart venait de l'interieur ou
d'un client : d'ou un identifiant NEUF pour le libelle leve d'ambiguite, plutot
que la reecriture du texte source d'un identifiant deja traduit en six langues.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- la source, desormais explicite -------------------------------------
    'capa.source.non-conformity-internal': (
        'Non-conformité interne', 'Internal non-conformity', 'No conformidad interna',
        'عدم مطابقة داخلية', '内部不適合', '内部不合格'),
    # Ouverte depuis une « NC externe » : le dossier affichait « interne » sur
    # l'ecran meme qui sert a repondre au client.
    'capa.source.non-conformity-external': (
        'Non-conformité externe (réclamation client)',
        'External non-conformity (customer complaint)',
        'No conformidad externa (reclamación de cliente)',
        'عدم مطابقة خارجية (شكوى عميل)',
        '外部不適合（顧客苦情）',
        '外部不合格（客户投诉）'),

    # --- la reference, choisie au registre ----------------------------------
    'capa.create.source-ref-required': (
        'Référence source', 'Source reference', 'Referencia de origen',
        'مرجع المصدر', '出所の参照番号', '来源编号'),
    'capa.create.source-ref-loading': (
        'Lecture du registre…', 'Reading the register…', 'Leyendo el registro…',
        'جارٍ قراءة السجل…', '台帳を読み込み中…', '正在读取登记册…'),
    'capa.create.source-ref-empty': (
        "Aucune fiche ouverte dans ce registre : choisissez une autre source.",
        'No open record in this register: choose another source.',
        'Ninguna ficha abierta en este registro: elija otro origen.',
        'لا توجد بطاقة مفتوحة في هذا السجل: اختر مصدراً آخر.',
        'この台帳に未完了の記録はありません。別の出所を選んでください。',
        '该登记册中没有未结记录：请选择其他来源。'),
    'capa.create.source-ref-truncated': (
        'Les {$SIZE} fiches ouvertes les plus récentes.',
        'The {$SIZE} most recent open records.',
        'Las {$SIZE} fichas abiertas más recientes.',
        'أحدث {$SIZE} بطاقة مفتوحة.',
        '直近 {$SIZE} 件の未完了の記録。', '最近的 {$SIZE} 条未结记录。'),
    'capa.create.source-ref-missing': (
        'Choisissez la fiche que ce dossier traite.',
        'Choose the record this case addresses.',
        'Elija la ficha que trata este expediente.',
        'اختر البطاقة التي يعالجها هذا الملف.',
        'この案件が扱う記録を選んでください。',
        '请选择本案件所处理的记录。'),
    'capa.create.source-ref-failed': (
        'Registre injoignable : saisissez la référence à la main.',
        'Register unreachable: type the reference by hand.',
        'Registro inaccesible: escriba la referencia a mano.',
        'السجل غير متاح: أدخل المرجع يدوياً.',
        '台帳に接続できません。参照番号を手入力してください。',
        '无法访问登记册：请手动输入编号。'),
    'capa.create.source-ref-retry': (
        'Relire le registre', 'Read the register again', 'Volver a leer el registro',
        'إعادة قراءة السجل', '台帳を読み直す', '重新读取登记册'),
}
