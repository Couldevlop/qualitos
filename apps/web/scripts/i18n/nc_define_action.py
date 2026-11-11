# -*- coding: utf-8 -*-
"""Table i18n - « Definir action » ouvre la fenetre de creation de CAPA.

Le bouton ne faisait que franchir une etape du cycle de vie : l'ecart passait a
« action definie » sans qu'aucune action n'existe nulle part. Son infobulle
change donc de sens, d'ou un identifiant NEUF.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    'nc.detail.define-action-tooltip2': (
        "Écrire l'action corrective dans une CAPA",
        'Write the corrective action in a CAPA case',
        'Escribir la acción correctiva en un expediente CAPA',
        'تدوين الإجراء التصحيحي في ملف CAPA',
        '是正処置をCAPA案件に記載',
        '在 CAPA 案件中写明纠正措施'),
}
