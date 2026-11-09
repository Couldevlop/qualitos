# -*- coding: utf-8 -*-
"""Un écart devient le cycle PDCA ou le projet DMAIC qui le traitera (§3.6).

La CAPA traite l'écart lui-même : on contient, on corrige, on vérifie que ça a
tenu. Elle ne couvre pas tout — un écart qui révèle un procédé instable demande
des mesures et une phase de maîtrise, un écart qui révèle une pratique à
corriger demande un cycle. Les deux modules existaient, sans porte depuis la
fiche de l'écart.
id: (fr, en, es, ar, ja, zh).
"""

TRANSLATIONS = {

    # --- la fenêtre ---------------------------------------------------------
    'nc.convert.title': (
        'Traiter cet écart', 'Treat this finding', 'Tratar esta desviación',
        'معالجة هذا الانحراف', 'この逸脱に対処', '处理该偏差'),
    'nc.convert.submit': (
        'Ouvrir le dossier', 'Open the case', 'Abrir el expediente',
        'فتح الملف', '案件を開く', '开立档案'),
    'nc.convert.from-nc': (
        'Écart repris', 'Finding carried over', 'Desviación retomada',
        'الانحراف المنقول', '引き継がれる逸脱', '沿用的偏差'),

    # --- la méthode, proposée par la gravité --------------------------------
    'nc.convert.method': (
        'Méthode', 'Method', 'Método', 'الأسلوب', '手法', '方法'),
    'nc.convert.method-pdca': (
        'Cycle PDCA', 'PDCA cycle', 'Ciclo PDCA', 'دورة PDCA', 'PDCAサイクル', 'PDCA 循环'),
    'nc.convert.method-dmaic': (
        'Projet DMAIC', 'DMAIC project', 'Proyecto DMAIC',
        'مشروع DMAIC', 'DMAICプロジェクト', 'DMAIC 项目'),
    'nc.convert.why-dmaic': (
        "Un écart critique demande des mesures et une phase de maîtrise : c'est un projet DMAIC.",
        'A critical finding calls for measurement and a control phase: that is a DMAIC project.',
        'Una desviación crítica exige mediciones y una fase de control: es un proyecto DMAIC.',
        'يتطلّب الانحراف الحرج قياسات ومرحلة ضبط: وهذا مشروع DMAIC.',
        '重大な逸脱には測定と管理フェーズが必要です。つまりDMAICプロジェクトです。',
        '严重偏差需要测量与控制阶段，因此属于 DMAIC 项目。'),
    'nc.convert.why-pdca': (
        'Un écart mineur ou majeur se traite par un cycle : planifier, agir, vérifier, généraliser.',
        'A minor or major finding is treated by a cycle: plan, do, check, act.',
        'Una desviación menor o mayor se trata con un ciclo: planificar, hacer, verificar, actuar.',
        'يُعالج الانحراف البسيط أو الجوهري بدورة: التخطيط، التنفيذ، التحقّق، التعميم.',
        '軽微または重要な逸脱はサイクルで対処します。計画し、実行し、確認し、定着させる。',
        '轻微或重大偏差通过循环处理：计划、执行、检查、推行。'),
    'nc.convert.overridden': (
        "Ce n'est pas la méthode que la gravité appelait. C'est votre choix, il part tel quel.",
        'This is not the method the severity called for. It is your call, and it goes as is.',
        'No es el método que la gravedad pedía. Es su decisión, y se envía tal cual.',
        'ليس هذا الأسلوب الذي تستدعيه الخطورة. القرار قرارك، ويُرسل كما هو.',
        '重大度が示した手法とは異なります。あなたの判断として、そのまま送信されます。',
        '这不是严重程度所对应的方法。这是您的选择，将按原样提交。'),

    # --- la personne qui mène ----------------------------------------------
    'nc.convert.pilot': (
        'Qui mène le travail', 'Who leads the work', 'Quién lidera el trabajo',
        'من يقود العمل', '担当する人', '由谁主导'),
    'nc.convert.pilot-hint': (
        'Propriétaire du cycle, ou black belt du projet. Un dossier dont personne ne répond '
        'ne se revoit jamais.',
        'Owner of the cycle, or black belt of the project. A case nobody answers for is '
        'never reviewed again.',
        'Propietario del ciclo, o black belt del proyecto. Un expediente del que nadie '
        'responde no se revisa nunca.',
        'مالك الدورة أو الحزام الأسود للمشروع. الملف الذي لا يتولّاه أحد لا يُراجع أبدًا.',
        'サイクルのオーナー、またはプロジェクトのブラックベルト。誰も責任を持たない案件は'
        '二度と見直されません。',
        '循环的责任人，或项目的黑带。无人负责的档案永远不会被复查。'),

    # --- l'échéance, réservée au projet ------------------------------------
    'nc.convert.due-date': (
        'Échéance visée', 'Target date', 'Fecha prevista',
        'الموعد المستهدف', '目標期日', '目标日期'),
    'nc.convert.due-date-hint': (
        'Facultative : le projet peut la fixer plus tard.',
        'Optional: the project can set it later.',
        'Opcional: el proyecto puede fijarla más tarde.',
        'اختياري: يمكن للمشروع تحديده لاحقًا.',
        '任意：プロジェクト側で後から設定できます。',
        '可选：项目可稍后设定。'),
    'nc.convert.due-date-open': (
        'Ouvrir le calendrier', 'Open the calendar', 'Abrir el calendario',
        'فتح التقويم', 'カレンダーを開く', '打开日历'),

    # --- ce que l'écran dit après coup -------------------------------------
    'nc.convert.pdca-opened': (
        'Cycle PDCA ouvert.', 'PDCA cycle opened.', 'Ciclo PDCA abierto.',
        'تم فتح دورة PDCA.', 'PDCAサイクルを開きました。', 'PDCA 循环已开立。'),
    'nc.convert.dmaic-opened': (
        'Projet DMAIC ouvert.', 'DMAIC project opened.', 'Proyecto DMAIC abierto.',
        'تم فتح مشروع DMAIC.', 'DMAICプロジェクトを開きました。', 'DMAIC 项目已开立。'),
    'nc.convert.failed': (
        "Le dossier n'a pas pu être ouvert.", 'The case could not be opened.',
        'El expediente no pudo abrirse.', 'لم يتمكّن النظام من فتح الملف.',
        '案件を開けませんでした。', '档案未能开立。'),

    # --- la fiche de l'écart -----------------------------------------------
    'nc.detail.convert': (
        'Traiter par une méthode', 'Treat by a method', 'Tratar con un método',
        'المعالجة بأسلوب', '手法で対処', '按方法处理'),
    'nc.detail.convert-tooltip': (
        'Ouvrir le cycle ou le projet qui traitera ce que cet écart révèle',
        'Open the cycle or project that will treat what this finding reveals',
        'Abrir el ciclo o el proyecto que tratará lo que revela esta desviación',
        'فتح الدورة أو المشروع الذي سيعالج ما يكشفه هذا الانحراف',
        'この逸脱が明らかにしたものに対処するサイクルまたはプロジェクトを開く',
        '开立用于处理该偏差所揭示问题的循环或项目'),
    'nc.detail.open-pdca': (
        'Voir le cycle PDCA', 'View the PDCA cycle', 'Ver el ciclo PDCA',
        'عرض دورة PDCA', 'PDCAサイクルを表示', '查看 PDCA 循环'),
    'nc.detail.open-dmaic': (
        'Voir le projet DMAIC', 'View the DMAIC project', 'Ver el proyecto DMAIC',
        'عرض مشروع DMAIC', 'DMAICプロジェクトを表示', '查看 DMAIC 项目'),
    'nc.detail.pdca-label': (
        'Cycle PDCA', 'PDCA cycle', 'Ciclo PDCA', 'دورة PDCA', 'PDCAサイクル', 'PDCA 循环'),
    'nc.detail.pdca-link-open': (
        'Ouvrir le cycle', 'Open the cycle', 'Abrir el ciclo',
        'فتح الدورة', 'サイクルを開く', '打开循环'),
    'nc.detail.dmaic-label': (
        'Projet DMAIC', 'DMAIC project', 'Proyecto DMAIC',
        'مشروع DMAIC', 'DMAICプロジェクト', 'DMAIC 项目'),
    'nc.detail.dmaic-link-open': (
        'Ouvrir le projet', 'Open the project', 'Abrir el proyecto',
        'فتح المشروع', 'プロジェクトを開く', '打开项目'),
}
