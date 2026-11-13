# -*- coding: utf-8 -*-
"""Table i18n - une CAPA est une action, et le PFMEA d'un produit s'edite.

Trois consequences de langue.

1. La fiche CAPA ne montre plus un tableau d'actions mais l'AVANCEMENT de son
   propre travail : une CAPA nait avec son action (ADR 0088), et une liste
   d'actions a l'interieur d'une action n'a pas d'objet. Le titre de la carte
   change, et trois phrases disent les trois etats possibles -- un travail,
   aucun, plusieurs.

2. L'onglet PFMEA d'une fiche produit, jusqu'ici en lecture seule, porte
   desormais les memes gestes que l'ecran du module FMEA : il lui faut le titre
   de son tableau, avec le nombre de lignes.

3. Rien d'autre : tous les autres libelles ajoutes a ces deux ecrans etaient
   deja au catalogue (les colonnes du FMEA, « Demarrer », « Terminer »,
   « Modifier », « Supprimer »). Les reutiliser plutot que d'en creer des
   jumeaux garantit que les deux ecrans disent le meme mot pour le meme geste.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- la fiche CAPA : l'avancement, et non une liste d'actions -----------
    'capa.detail.progress-title': (
        'Avancement', 'Progress', 'Avance', 'التقدم', '進捗', '进展'),
    'capa.detail.progress-hint': (
        'Le statut du dossier se déduit de ce travail : le démarrer le met en cours, '
        'le terminer le résout.',
        'The case status follows this work: starting it moves the case to in progress, '
        'completing it resolves the case.',
        'El estado del expediente se deduce de este trabajo: iniciarlo lo pone en curso, '
        'terminarlo lo resuelve.',
        'تُستنتج حالة الملف من هذا العمل: بدؤه يجعله قيد التنفيذ، وإنهاؤه يحله.',
        '案件のステータスはこの作業から決まります。開始すれば進行中に、完了すれば解決になります。',
        '案卷状态由此项工作推导：开始则进行中，完成则已解决。'),
    'capa.detail.progress-none': (
        "Ce dossier ne porte aucun travail : il a été ouvert avant que l'ouverture "
        "d'une CAPA crée le sien. Il restera ouvert tant qu'aucune action ne lui est "
        "rattachée depuis la fiche de l'écart d'origine.",
        'This case carries no work: it was opened before opening a CAPA created its own '
        'action. It will stay open until an action is attached to it from the originating '
        'finding.',
        'Este expediente no lleva ningún trabajo: se abrió antes de que la apertura de una '
        'CAPA creara el suyo. Seguirá abierto mientras no se le adjunte una acción desde '
        'la ficha de la desviación de origen.',
        'لا يحمل هذا الملف أي عمل: فقد فُتح قبل أن يُنشئ فتحُ إجراء تصحيحي عملَه الخاص. '
        'سيبقى مفتوحًا ما لم يُرفق به إجراء من بطاقة الانحراف الأصلي.',
        'この案件には作業がありません。CAPA の開設が自らの対策を作るようになる前に開かれたためです。'
        '元の不適合のページから対策が結び付けられるまで、開いたままになります。',
        '本案卷没有任何工作：它在"开立 CAPA 即创建其措施"之前开立。'
        '在从原始偏差页面为其附加措施之前，它将一直处于开立状态。'),
    'capa.detail.progress-many': (
        "Ce dossier porte {$INTERPOLATION} travaux, ajoutés depuis la fiche de l'écart. "
        "Ils s'y lisent et s'y avancent, là où ils ont été décidés.",
        'This case carries {$INTERPOLATION} pieces of work, added from the finding. '
        'They are read and advanced there, where they were decided.',
        'Este expediente lleva {$INTERPOLATION} trabajos, añadidos desde la ficha de la '
        'desviación. Allí se leen y se hacen avanzar, donde fueron decididos.',
        'يحمل هذا الملف {$INTERPOLATION} من الأعمال، أُضيفت من بطاقة الانحراف. '
        'تُقرأ وتُتابع هناك، حيث تقررت.',
        'この案件は、不適合のページから追加された {$INTERPOLATION} 件の作業を持ちます。'
        '決定された場所、すなわちそのページで読み、進めます。',
        '本案卷承载 {$INTERPOLATION} 项工作，自偏差页面添加。'
        '它们在决定之处——该页面——阅读和推进。'),
    'capa.detail.progress-open-nc': (
        "Ouvrir la fiche de l'écart", 'Open the finding',
        'Abrir la ficha de la desviación', 'فتح بطاقة الانحراف',
        '不適合のページを開く', '打开偏差页面'),

    # --- l'onglet PFMEA d'un produit ----------------------------------------
    'pfmea.items-count': (
        "Lignes d'analyse ({$INTERPOLATION})", 'Analysis rows ({$INTERPOLATION})',
        'Líneas de análisis ({$INTERPOLATION})', 'أسطر التحليل ({$INTERPOLATION})',
        '分析行（{$INTERPOLATION}）', '分析行（{$INTERPOLATION}）'),
}
