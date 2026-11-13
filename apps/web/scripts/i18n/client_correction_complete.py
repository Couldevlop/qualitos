# -*- coding: utf-8 -*-
"""Table i18n - corriger un client, et son logo.

La fenetre de correction d'un client ne portait que le nom et l'offre ; elle
porte desormais tout ce qui a servi a le creer. Presque tous les libelles
etaient deja au catalogue -- ceux de l'assistant de creation, reutilises tels
quels pour que les deux ecrans disent le meme mot pour la meme chose.

Trois phrases manquaient, et elles concernent le LOGO : un logo pose une fois
ne pouvait plus que se remplacer, jamais se retirer. Il faut donc dire qu'il
y en a un, offrir de l'enlever, et annoncer que l'enlevement attend
l'enregistrement -- un retrait qui partirait au clic surprendrait, puisque
tout le reste de la fenetre attend le bouton « Enregistrer ».

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    'platform.clients.logo-present': (
        'Un logo est déjà en place.', 'A logo is already in place.',
        'Ya hay un logotipo.', 'يوجد شعار بالفعل.',
        'ロゴはすでに設定されています。', '已设置标识。'),
    'platform.clients.logo-drop': (
        'Le retirer', 'Remove it', 'Quitarlo', 'إزالته', '削除する', '移除'),
    'platform.clients.logo-dropped': (
        "Le logo sera retiré à l'enregistrement.",
        'The logo will be removed when you save.',
        'El logotipo se quitará al guardar.',
        'سيُزال الشعار عند الحفظ.',
        '保存時にロゴが削除されます。',
        '保存时将移除该标识。'),
}
