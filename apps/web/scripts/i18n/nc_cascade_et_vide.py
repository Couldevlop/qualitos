# -*- coding: utf-8 -*-
"""Table i18n - la cascade categorie/nature, et l'absence de dossier CAPA.

Deux champs et non un : la liste unique groupee par familles a ete essayee puis
ecartee en recette. Soixante lignes groupees se parcourent toujours sur
plusieurs ecrans de defilement, et les titres de famille se lisent mal au milieu
des options -- le filtre prealable ramene la liste a ce qu'on embrasse d'un
regard, ce qu'aucun groupement ne fait.

Et quand il n'y a pas encore de dossier CAPA, les ecrans le disent au lieu de
masquer le tableau : on cherchait une fonction absente de l'ecran alors qu'un
seul geste la fait apparaitre.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- la cascade a la declaration ---------------------------------------
    # `nc.create.category` et `nc.create.category-required` existent deja :
    # le champ avait ete retire, ses libelles sont restes en reserve.
    'nc.create.nature-pick-category': (
        "Choisissez d'abord la catégorie.", 'Choose the category first.',
        'Elija primero la categoría.', 'اختر الفئة أولاً.',
        'まずカテゴリーを選択してください。', '请先选择类别。'),

    # --- l'absence de dossier CAPA -----------------------------------------
    'nc.detail.capa-action-empty': (
        "Aucun dossier CAPA sur cet écart. Ouvrez-en un : les actions "
        "correctives et préventives se liront et s'avanceront ici même, et "
        "rempliront les D5 et D6 du rapport 8D.",
        "No CAPA case on this deviation. Open one: corrective and preventive "
        "actions will be read and advanced right here, and will fill in D5 and "
        "D6 of the 8D report.",
        "Ningún expediente CAPA en esta desviación. Abra uno: las acciones "
        "correctivas y preventivas se leerán y avanzarán aquí mismo, y "
        "completarán D5 y D6 del informe 8D.",
        "لا يوجد ملف إجراءات تصحيحية ووقائية لهذا الانحراف. افتح ملفاً: "
        "ستُقرأ الإجراءات التصحيحية والوقائية وتُتابع هنا، وستُكمل D5 و D6 "
        "من تقرير 8D.",
        "この逸脱に是正・予防処置の案件はありません。案件を開くと、是正処置と"
        "予防処置をここで確認・進行でき、8Dレポートの D5・D6 も埋まります。",
        "此偏差尚无 CAPA 档案。开立一个：纠正和预防措施将在此处查阅与推进，"
        "并填入 8D 报告的 D5 与 D6。"),
    'nc.8d.actions-empty': (
        "Les actions de cette discipline se liront et s'avanceront ici dès "
        "qu'un dossier CAPA sera ouvert sur l'écart.",
        "The actions of this discipline will be read and advanced here as soon "
        "as a CAPA case is opened on the deviation.",
        "Las acciones de esta disciplina se leerán y avanzarán aquí en cuanto "
        "se abra un expediente CAPA en la desviación.",
        "ستُقرأ إجراءات هذا البند وتُتابع هنا بمجرد فتح ملف إجراءات تصحيحية "
        "ووقائية للانحراف.",
        "この項目の処置は、逸脱に是正・予防処置の案件が開かれ次第、ここで"
        "確認・進行できます。",
        "一旦针对该偏差开立 CAPA 档案，本项措施即可在此查阅与推进。"),
    'nc.8d.go-to-nc': (
        "Ouvrir la fiche de l'écart", 'Open the deviation record',
        'Abrir la ficha de la desviación', 'فتح بطاقة الانحراف',
        '逸脱の記録を開く', '打开偏差记录'),
}
