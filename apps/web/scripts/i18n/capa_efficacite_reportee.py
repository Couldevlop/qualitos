# -*- coding: utf-8 -*-
"""Table i18n - la verification d'efficacite est REPORTEE, pas perdue.

Ajouter une action a un dossier resolu le ramene en traitement : c'est juste,
il reste du travail. Mais la question d'efficacite, visible l'instant d'avant,
disparaissait alors de l'ecran sans un mot, et on croyait a une fonction
perdue. Ces phrases disent a quelle condition elle revient.

Le DECOMPTE des actions restantes est la condition elle-meme : il se dit, et il
decide de la phrase (une / plusieurs / aucune).

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    'capa.detail.effectiveness-pending-one': (
        "La vérification d'efficacité sera proposée quand la dernière action aura été terminée.",
        'The effectiveness check will be offered once the last action has been completed.',
        'La verificación de eficacia se propondrá cuando se haya terminado la última acción.',
        'سيُتاح التحقق من الفعالية بعد إتمام الإجراء الأخير.',
        '最後の対策が完了した時点で、有効性の確認が可能になります。',
        '待最后一项措施完成后，将提供有效性验证。'),
    'capa.detail.effectiveness-pending-many': (
        "La vérification d'efficacité sera proposée quand les {$count} actions restantes auront été terminées.",
        'The effectiveness check will be offered once the {$count} remaining actions have been completed.',
        'La verificación de eficacia se propondrá cuando se hayan terminado las {$count} acciones restantes.',
        'سيُتاح التحقق من الفعالية بعد إتمام الإجراءات المتبقية وعددها {$count}.',
        '残り {$count} 件の対策が完了した時点で、有効性の確認が可能になります。',
        '待剩余 {$count} 项措施完成后，将提供有效性验证。'),
    'capa.detail.effectiveness-pending-none': (
        "La vérification d'efficacité sera proposée une fois le dossier résolu.",
        'The effectiveness check will be offered once the case is resolved.',
        'La verificación de eficacia se propondrá una vez resuelto el expediente.',
        'سيُتاح التحقق من الفعالية بعد تسوية الملف.',
        '案件が解決された時点で、有効性の確認が可能になります。',
        '待档案解决后，将提供有效性验证。'),
}
