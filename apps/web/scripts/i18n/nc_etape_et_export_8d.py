# -*- coding: utf-8 -*-
"""Table i18n - l'etape qui manque avant de resoudre, et le PDF du 8D avant
la cloture. id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    # --- « Resoudre » aux etapes ou le geste n'est pas encore possible ---
    'nc.detail.resolve-needs-analysis-title': (
        "Analysez l'écart d'abord",
        'Analyse the deviation first',
        'Analice primero la desviación',
        'حلّل الانحراف أولاً',
        'まず不適合を分析してください',
        '请先分析该不合格',
    ),
    'nc.detail.resolve-needs-analysis': (
        "Cet écart n'est pas encore instruit. Lancez « Analyser », puis inscrivez au moins"
        " une action : c'est l'ouverture du dossier d'actions qui porte l'écart à l'étape"
        " où il se résout.",
        'This deviation has not been investigated yet. Start "Analyse", then record at'
        ' least one action: opening the action case is what brings the deviation to the'
        ' stage where it can be resolved.',
        'Esta desviación aún no se ha instruido. Inicie «Analizar» y registre al menos'
        ' una acción: la apertura del expediente de acciones es lo que lleva la desviación'
        ' a la etapa en la que puede resolverse.',
        'لم يُدرس هذا الانحراف بعد. ابدأ «تحليل» ثم سجّل إجراءً واحداً على الأقل: فتح ملف'
        ' الإجراءات هو ما ينقل الانحراف إلى مرحلة إمكانية حلّه.',
        'この不適合はまだ調査されていません。「分析」を実行し、少なくとも一つの対策を'
        '登録してください。対策台帳を開くことで、解決できる段階に進みます。',
        '该不合格尚未立案。请先执行「分析」，再登记至少一项措施：打开措施台账才能使其进入可解决的阶段。',
    ),
    'nc.detail.resolve-needs-action-title': (
        "Inscrivez une action d'abord",
        'Record an action first',
        'Registre primero una acción',
        'سجّل إجراءً أولاً',
        'まず対策を登録してください',
        '请先登记一项措施',
    ),
    'nc.detail.resolve-needs-action': (
        "Aucune action n'est encore inscrite sur cet écart. « Nouvelle action » ouvre son"
        " dossier et le porte à l'étape où il se résout.",
        'No action has been recorded on this deviation yet. "New action" opens its case'
        ' and brings it to the stage where it can be resolved.',
        'Todavía no se ha registrado ninguna acción en esta desviación. «Nueva acción»'
        ' abre su expediente y la lleva a la etapa en la que puede resolverse.',
        'لم يُسجَّل أي إجراء على هذا الانحراف بعد. يفتح «إجراء جديد» ملفه وينقله إلى'
        ' مرحلة إمكانية حلّه.',
        'この不適合にはまだ対策が登録されていません。「新しい対策」が台帳を開き、'
        '解決できる段階に進めます。',
        '该不合格尚未登记任何措施。「新建措施」将打开其台账并使其进入可解决的阶段。',
    ),
    'nc.detail.resolve-hint-analysis': (
        "Cet écart n'est pas encore instruit : commencez par « Analyser ».",
        'This deviation has not been investigated yet: start with "Analyse".',
        'Esta desviación aún no se ha instruido: comience por «Analizar».',
        'لم يُدرس هذا الانحراف بعد: ابدأ بـ «تحليل».',
        'この不適合はまだ調査されていません。まず「分析」から始めてください。',
        '该不合格尚未立案：请从「分析」开始。',
    ),
    'nc.detail.resolve-hint-action': (
        "Aucune action n'est encore inscrite : commencez par « Nouvelle action ».",
        'No action has been recorded yet: start with "New action".',
        'Todavía no se ha registrado ninguna acción: comience por «Nueva acción».',
        'لم يُسجَّل أي إجراء بعد: ابدأ بـ «إجراء جديد».',
        'まだ対策が登録されていません。まず「新しい対策」から始めてください。',
        '尚未登记任何措施：请从「新建措施」开始。',
    ),
    # --- le PDF du rapport 8D n'existe pas avant la cloture de l'ecart ---
    'nc.8d.export-needs-closure-title': (
        "Clôturez l'écart d'abord",
        'Close the deviation first',
        'Cierre primero la desviación',
        'أغلق الانحراف أولاً',
        'まず不適合を完了してください',
        '请先关闭该不合格',
    ),
    'nc.8d.export-needs-closure': (
        "Le PDF n'existe qu'une fois le rapport émis, et le rapport ne s'émet qu'une fois"
        " l'écart clos : c'est la clôture qui arrête le contenu. Clôturez l'écart sur sa"
        " fiche, puis revenez émettre ce rapport.",
        'The PDF exists only once the report is issued, and the report can only be issued'
        ' once the deviation is closed: closure is what freezes the content. Close the'
        ' deviation on its record, then come back to issue this report.',
        'El PDF solo existe una vez emitido el informe, y el informe solo puede emitirse'
        ' una vez cerrada la desviación: el cierre es lo que fija el contenido. Cierre la'
        ' desviación en su ficha y vuelva a emitir este informe.',
        'لا يوجد ملف PDF إلا بعد إصدار التقرير، ولا يُصدر التقرير إلا بعد إغلاق الانحراف:'
        ' فالإغلاق هو ما يثبّت المحتوى. أغلق الانحراف في بطاقته ثم عد لإصدار هذا التقرير.',
        'PDF は報告書を発行した後にのみ存在し、報告書は不適合を完了した後にのみ発行'
        'できます。完了が内容を確定させます。不適合を票上で完了させ、その後この報告書を'
        '発行してください。',
        'PDF 仅在报告签发后存在，而报告只能在不合格关闭后签发：关闭才会固定内容。'
        '请在不合格单上关闭该不合格，然后回来签发本报告。',
    ),
}
