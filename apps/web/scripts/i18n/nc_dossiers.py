# -*- coding: utf-8 -*-
"""Table i18n - un ecart peut porter PLUSIEURS dossiers CAPA.

La cloture d'un ecart attend TOUS les dossiers qui citent sa reference, mais
sa fiche n'en montrait qu'un : celui que porte son lien. On lisait donc
« 2 dossiers non regles » au-dessus d'un tableau qui n'en montrait qu'un, et
les actions du second n'existaient nulle part a l'ecran.

Le compte ne s'ecrit QUE s'il y a de quoi compter : « 1 dossier » sur le cas
ordinaire serait du bruit, c'est la pluralite qui est inattendue.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    'nc.detail.capa-cases-count': (
        "{$count} dossiers CAPA traitent cet écart",
        '{$count} CAPA cases address this non-conformity',
        '{$count} expedientes CAPA tratan esta no conformidad',
        '{$count} من ملفات CAPA تعالج عدم المطابقة هذا',
        'この不適合は {$count} 件の CAPA 案件で処理されています',
        '有 {$count} 份 CAPA 档案在处理该不合格项'),
}
