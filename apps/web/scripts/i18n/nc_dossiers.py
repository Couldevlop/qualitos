# -*- coding: utf-8 -*-
"""Table i18n - le D5 compte ses ACTIONS.

Le dossier EST la non-conformite ; elle porte plusieurs actions, et chaque
action est une CAPA. La phrase du D5 annoncait « 6 dossiers CAPA traitent cet
ecart » au-dessus d'un tableau qui en montrait QUATRE : elle comptait tout ce
qui cite la reference de l'ecart -- endiguement et preventif compris -- la ou
le tableau ne retient que les correctives. Deux nombres pour une meme ligne,
dont un faux ; et c'est le chiffre affiche que l'on croit.

Elle nommait aussi un « dossier CAPA », niveau intermediaire que le modele ne
reconnait pas : il n'y a que le dossier (la NC) et ses actions.

Le compte ne s'ecrit QU'AU-DELA d'une action : « 1 action » serait du bruit,
c'est la pluralite qui est inattendue.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    'nc.detail.d5-actions-count': (
        "{$count} actions correctives traitent cet écart",
        '{$count} corrective actions address this non-conformity',
        '{$count} acciones correctivas tratan esta no conformidad',
        '{$count} من الإجراءات التصحيحية تعالج عدم المطابقة هذا',
        'この不適合は {$count} 件の是正処置で対応されています',
        '有 {$count} 项纠正措施在处理该不合格项'),
}
