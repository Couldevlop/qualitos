# -*- coding: utf-8 -*-
"""Libellés de l'évaluation d'efficacité des CAPA (ADR 0108).

Le verdict à trois états — efficace, partiellement, inefficace — son critère posé
à l'avance, et le plan de travail qui dit ce qui attend un jugement.

Les XLF sont GÉNÉRÉS depuis ces tables : ne jamais éditer `src/locale/*.xlf` à la
main, la CI rejoue la génération et refuse toute divergence.

Aucun pluriel ICU ici, à dessein : les messages qui comptent quelque chose sont
scindés en un message par cas (`-one`, `-many`, `lock-no-action`), parce que
l'accord diffère dans les six langues et qu'une forme unique « dossier(s) » se
lit mal dans celle où l'on travaille. Les interpolations, elles, s'écrivent
`{$INTERPOLATION}` : laissées en `{{ }}`, elles seraient lues comme des
expressions ICU et la vue traduite lèverait à l'exécution.

Ordre des langues : fr, en, es, ar, ja, zh.
"""

TRANSLATIONS = {
    # ------------------------------------------------------- les deux onglets
    'capa.effectiveness.tab-evaluation': (
        'Évaluations', 'Evaluations', 'Evaluaciones',
        'التقييمات', '評価', '评价'),
    'capa.effectiveness.tab-recurrence': (
        'Mesure par récidive', 'Recurrence measurement', 'Medición por recurrencia',
        'القياس بالتكرار', '再発による測定', '按复发测量'),
    'capa.effectiveness.recurrence-intro': (
        "Ce que le terrain dit des dossiers clos, et non ce qu'on avait déclaré à la clôture.",
        'What the shop floor says about closed cases — not what was declared at closure.',
        'Lo que dice el terreno sobre los casos cerrados, no lo declarado al cierre.',
        'ما يقوله الواقع عن الملفات المغلقة، لا ما أُعلن عند الإغلاق.',
        'クローズ済み案件について現場が示す事実であり、クローズ時の申告ではありません。',
        '关于已关闭案件，现场给出的事实，而非结案时的声明。'),

    # ------------------------------------------------------- les quatre chiffres
    'capa.evaluation.kpi-rate': (
        "Taux d'efficacité des actions", 'Action effectiveness rate',
        'Tasa de eficacia de las acciones', 'معدل فعالية الإجراءات',
        '是正処置の有効率', '措施有效率'),
    'capa.evaluation.kpi-rate-hint': (
        'Sur les seuls dossiers déjà jugés', 'Across already judged cases only',
        'Solo sobre los casos ya evaluados', 'على الملفات التي صدر فيها حكم فقط',
        '判定済みの案件のみ', '仅统计已判定的案件'),
    'capa.evaluation.kpi-awaiting': (
        'Évaluations à faire', 'Evaluations due', 'Evaluaciones pendientes',
        'تقييمات مستحقة', '実施待ちの評価', '待完成的评价'),
    'capa.evaluation.kpi-open': (
        'Dossiers encore ouverts', 'Cases still open', 'Casos aún abiertos',
        'ملفات لا تزال مفتوحة', '未完了の案件', '仍未关闭的案件'),
    'capa.evaluation.kpi-reopened': (
        'Dossiers rouverts', 'Reopened cases', 'Casos reabiertos',
        'ملفات أُعيد فتحها', '再開された案件', '已重开的案件'),
    'capa.evaluation.kpi-reopened-hint': (
        'Jugés inefficaces', 'Judged ineffective', 'Evaluados como ineficaces',
        'حُكم بعدم فعاليتها', '無効と判定', '判定为无效'),

    # ------------------------------------------------------------ les bandeaux
    'capa.evaluation.missing-criterion-one': (
        "Un dossier ouvert n'a pas de critère d'efficacité — il se pose sur la fiche du "
        "dossier, et il doit l'être avant le jugement.",
        'One open case has no effectiveness criterion — it is set on the case itself, and '
        'must be set before the verdict.',
        'Un caso abierto no tiene criterio de eficacia: se define en la ficha del caso, y '
        'debe hacerse antes del juicio.',
        'ملف مفتوح واحد بلا معيار فعالية — يُحدَّد في بطاقة الملف، ويجب تحديده قبل إصدار الحكم.',
        '未完了の案件が1件、有効性判定基準を欠いています。基準は案件票で設定し、判定より前に定める必要があります。',
        '有 1 个未关闭案件没有有效性判定标准——标准在案件卡中设定，且必须在判定之前确定。'),
    'capa.evaluation.missing-criterion-many': (
        "{$INTERPOLATION} dossiers ouverts n'ont pas de critère d'efficacité — il se pose "
        "sur la fiche du dossier, et il doit l'être avant le jugement.",
        '{$INTERPOLATION} open cases have no effectiveness criterion — it is set on the case '
        'itself, and must be set before the verdict.',
        '{$INTERPOLATION} casos abiertos no tienen criterio de eficacia: se define en la '
        'ficha del caso, y debe hacerse antes del juicio.',
        '{$INTERPOLATION} ملفات مفتوحة بلا معيار فعالية — يُحدَّد في بطاقة الملف، ويجب تحديده قبل إصدار الحكم.',
        '未完了の案件が {$INTERPOLATION} 件、有効性判定基準を欠いています。基準は案件票で設定し、判定より前に定める必要があります。',
        '有 {$INTERPOLATION} 个未关闭案件没有有效性判定标准——标准在案件卡中设定，且必须在判定之前确定。'),
    'capa.evaluation.truncated': (
        "Le périmètre dépasse la limite de lecture : seuls les dossiers les plus urgents sont "
        "listés. Les quatre chiffres ci-dessus, eux, portent sur la totalité.",
        'The scope exceeds the read limit: only the most urgent cases are listed. The four '
        'figures above, however, cover the whole scope.',
        'El alcance supera el límite de lectura: solo se listan los casos más urgentes. Las '
        'cuatro cifras anteriores, en cambio, cubren la totalidad.',
        'يتجاوز النطاق حدّ القراءة: تُسرد الملفات الأكثر إلحاحًا فقط. أما الأرقام الأربعة أعلاه فتشمل الكل.',
        '対象範囲が読み取り上限を超えています。一覧には最も急を要する案件のみを表示します。上の4つの数値は全体を対象としています。',
        '范围超出读取上限：列表仅显示最紧急的案件。但上方四个数字覆盖全部范围。'),
    'capa.evaluation.none': (
        "Aucun dossier à évaluer. Un dossier entre dans cette boucle dès qu'il est ouvert, et "
        "se présente au jugement quand son action est menée à terme.",
        'No case to evaluate. A case enters this loop as soon as it is opened, and comes up '
        'for judgement once its action is complete.',
        'Ningún caso que evaluar. Un caso entra en este ciclo desde su apertura y se presenta '
        'a juicio cuando su acción está terminada.',
        'لا يوجد ملف للتقييم. يدخل الملف هذه الحلقة بمجرد فتحه، ويُعرض للحكم عند إتمام إجرائه.',
        '評価すべき案件はありません。案件は起票された時点でこのループに入り、処置が完了した時点で判定の対象になります。',
        '没有需要评价的案件。案件自开立起即进入此循环，待其措施完成后提交判定。'),
    'capa.evaluation.load-failed': (
        "Impossible de charger les évaluations d'efficacité.",
        'Could not load the effectiveness evaluations.',
        'No se han podido cargar las evaluaciones de eficacia.',
        'تعذّر تحميل تقييمات الفعالية.',
        '有効性評価を読み込めませんでした。',
        '无法加载有效性评价。'),
    'capa.evaluation.save-failed': (
        "L'évaluation n'a pas pu être enregistrée.",
        'The evaluation could not be saved.',
        'No se ha podido guardar la evaluación.',
        'لم يتمكّن النظام من حفظ التقييم.',
        '評価を保存できませんでした。',
        '无法保存评价。'),

    # ----------------------------------------------------------- le tableau
    'capa.evaluation.table-title': (
        'Dossiers et actions correctives', 'Cases and corrective actions',
        'Casos y acciones correctivas', 'الملفات والإجراءات التصحيحية',
        '案件と是正処置', '案件与纠正措施'),
    'capa.evaluation.table-note': (
        "L'étape et le décompte des actions sont calculés : un dossier ne se clôt qu'après "
        "une évaluation jugée efficace.",
        'The stage and the action counts are computed: a case closes only after an evaluation '
        'judged effective.',
        'La etapa y el recuento de acciones se calculan: un caso solo se cierra tras una '
        'evaluación juzgada eficaz.',
        'تُحسَب المرحلة وعدد الإجراءات تلقائيًا: لا يُغلق الملف إلا بعد تقييم يُحكم بفعاليته.',
        '段階と処置件数は自動算出されます。案件は有効と判定された評価を経てはじめてクローズします。',
        '阶段与措施计数均为自动计算：案件须经判定为有效的评价后方可关闭。'),
    'capa.evaluation.col-case': ('Dossier', 'Case', 'Caso', 'الملف', '案件', '案件'),
    'capa.evaluation.col-stage': ('Étape', 'Stage', 'Etapa', 'المرحلة', '段階', '阶段'),
    'capa.evaluation.col-actions': (
        'Actions', 'Actions', 'Acciones', 'الإجراءات', '処置', '措施'),
    'capa.evaluation.col-verdict': (
        'Dernier verdict', 'Last verdict', 'Último veredicto',
        'آخر حكم', '直近の判定', '最近判定'),
    'capa.evaluation.actions-done': (
        '{$INTERPOLATION}/{$INTERPOLATION_1} faites',
        '{$INTERPOLATION}/{$INTERPOLATION_1} done',
        '{$INTERPOLATION}/{$INTERPOLATION_1} hechas',
        '{$INTERPOLATION}/{$INTERPOLATION_1} منجزة',
        '{$INTERPOLATION}/{$INTERPOLATION_1} 完了',
        '{$INTERPOLATION}/{$INTERPOLATION_1} 已完成'),
    'capa.evaluation.no-criterion-cell': (
        'Critère à poser', 'Criterion to be set', 'Criterio por definir',
        'معيار يجب تحديده', '基準を設定', '待设定标准'),
    'capa.evaluation.never-judged': (
        'Jamais évalué', 'Never evaluated', 'Nunca evaluado',
        'لم يُقيَّم قط', '未評価', '尚未评价'),
    'capa.evaluation.review-count': (
        '{$INTERPOLATION} évaluations', '{$INTERPOLATION} evaluations',
        '{$INTERPOLATION} evaluaciones', '{$INTERPOLATION} تقييمات',
        '{$INTERPOLATION} 件の評価', '{$INTERPOLATION} 次评价'),
    'capa.evaluation.pick-a-case': (
        "Choisissez un dossier pour voir ses actions et évaluer leur efficacité.",
        'Pick a case to see its actions and evaluate their effectiveness.',
        'Elija un caso para ver sus acciones y evaluar su eficacia.',
        'اختر ملفًا لعرض إجراءاته وتقييم فعاليتها.',
        '案件を選ぶと、その処置と有効性評価を表示します。',
        '选择一个案件以查看其措施并评价其有效性。'),

    # ------------------------------------------------------------ les étapes
    'capa.evaluation.stage-awaiting': (
        "En attente d'évaluation", 'Awaiting evaluation', 'Pendiente de evaluación',
        'في انتظار التقييم', '評価待ち', '待评价'),
    'capa.evaluation.stage-closed': (
        'Clôturé', 'Closed', 'Cerrado', 'مُغلق', 'クローズ', '已关闭'),
    'capa.evaluation.stage-partial': (
        'Action complémentaire', 'Further action', 'Acción complementaria',
        'إجراء تكميلي', '追加処置', '补充措施'),
    'capa.evaluation.stage-reopened': (
        'Rouvert', 'Reopened', 'Reabierto', 'أُعيد فتحه', '再開', '已重开'),
    'capa.evaluation.stage-in-progress': (
        'Actions en cours', 'Actions in progress', 'Acciones en curso',
        'إجراءات جارية', '処置進行中', '措施进行中'),

    # ----------------------------------------------------------- les verdicts
    'capa.evaluation.verdict-effective': (
        'Efficace', 'Effective', 'Eficaz', 'فعّال', '有効', '有效'),
    'capa.evaluation.verdict-partial': (
        'Partiellement', 'Partially', 'Parcialmente', 'جزئيًا', '部分的に有効', '部分有效'),
    'capa.evaluation.verdict-ineffective': (
        'Inefficace', 'Ineffective', 'Ineficaz', 'غير فعّال', '無効', '无效'),
    'capa.evaluation.verdict-aria': (
        "Verdict d'efficacité", 'Effectiveness verdict', 'Veredicto de eficacia',
        'حكم الفعالية', '有効性の判定', '有效性判定'),

    # ------------------------------------------------- le dossier et ses freins
    'capa.evaluation.containment-only': (
        "Ce dossier ne porte que des mesures d'endiguement : elles arrêtent l'effet sans "
        "supprimer la cause. Un verdict « efficace » sera refusé.",
        'This case carries containment measures only: they stop the effect without removing '
        'the cause. An "effective" verdict will be refused.',
        'Este caso solo tiene medidas de contención: detienen el efecto sin eliminar la '
        'causa. Un veredicto «eficaz» será rechazado.',
        'لا يحمل هذا الملف سوى تدابير احتواء: توقف الأثر دون إزالة السبب. سيُرفض الحكم بـ«فعّال».',
        'この案件には封じ込め措置しかありません。影響は止まりますが原因は残ります。「有効」の判定は受け付けられません。',
        '此案件仅包含围堵措施：它们止住了影响，却未消除原因。"有效"判定将被拒绝。'),
    'capa.evaluation.open-nc-one': (
        "Un écart lié est encore ouvert : la clôture sera refusée tant qu'il n'est pas traité.",
        'One linked non-conformity is still open: closure will be refused until it is settled.',
        'Una no conformidad vinculada sigue abierta: el cierre será rechazado hasta resolverla.',
        'لا تزال حالة عدم مطابقة مرتبطة مفتوحة: سيُرفض الإغلاق حتى معالجتها.',
        '関連する不適合が1件まだ未処理です。処理されるまでクローズは受け付けられません。',
        '有 1 项关联不符合项仍未关闭：在其处理完毕前，关闭将被拒绝。'),
    'capa.evaluation.open-nc-many': (
        "{$INTERPOLATION} écarts liés sont encore ouverts : la clôture sera refusée tant "
        "qu'il en reste.",
        '{$INTERPOLATION} linked non-conformities are still open: closure will be refused '
        'while any remain.',
        '{$INTERPOLATION} no conformidades vinculadas siguen abiertas: el cierre será '
        'rechazado mientras quede alguna.',
        'لا تزال {$INTERPOLATION} حالات عدم مطابقة مرتبطة مفتوحة: سيُرفض الإغلاق ما بقيت واحدة منها.',
        '関連する不適合が {$INTERPOLATION} 件まだ未処理です。1件でも残る間はクローズできません。',
        '有 {$INTERPOLATION} 项关联不符合项仍未关闭：只要还剩一项，关闭即被拒绝。'),

    # ------------------------------------------------------ la dernière évaluation
    'capa.evaluation.last-review-title': (
        'Dernière évaluation', 'Last evaluation', 'Última evaluación',
        'آخر تقييم', '直近の評価', '最近评价'),
    'capa.evaluation.never-judged-long': (
        "Ce dossier n'a jamais été évalué.", 'This case has never been evaluated.',
        'Este caso nunca ha sido evaluado.', 'لم يُقيَّم هذا الملف قط.',
        'この案件はまだ評価されていません。', '此案件尚未经过评价。'),
    'capa.evaluation.judged-against': (
        'Jugé contre :', 'Judged against:', 'Evaluado frente a:',
        'حُكم عليه مقابل:', '判定の基準:', '判定依据：'),
    'capa.evaluation.baseline': ('Avant', 'Before', 'Antes', 'قبل', '実施前', '实施前'),
    'capa.evaluation.target': ('Cible', 'Target', 'Objetivo', 'الهدف', '目標', '目标'),
    'capa.evaluation.observed': (
        'Constaté', 'Observed', 'Constatado', 'المُلاحظ', '実測', '实测'),

    # --------------------------------------------------------- le formulaire
    'capa.evaluation.form-title': (
        "Évaluer l'efficacité", 'Evaluate effectiveness', 'Evaluar la eficacia',
        'تقييم الفعالية', '有効性を評価する', '评价有效性'),
    'capa.evaluation.criterion': (
        "Critère posé à l'ouverture", 'Criterion set at opening',
        'Criterio definido en la apertura', 'المعيار المُحدَّد عند الفتح',
        '起票時に定めた基準', '开立时设定的标准'),
    'capa.evaluation.observed-hint': (
        'Obligatoire : le verdict repose sur une mesure',
        'Required: the verdict rests on a measurement',
        'Obligatorio: el veredicto se basa en una medición',
        'إلزامي: يستند الحكم إلى قياس',
        '必須: 判定は実測値に基づきます',
        '必填：判定以实测值为依据'),
    'capa.evaluation.comment': (
        'Commentaire', 'Comment', 'Comentario', 'تعليق', 'コメント', '备注'),
    'capa.evaluation.submit': (
        "Enregistrer l'évaluation", 'Save the evaluation', 'Guardar la evaluación',
        'حفظ التقييم', '評価を保存', '保存评价'),
    'capa.evaluation.submit-hint': (
        'Choisissez un verdict et saisissez le relevé constaté.',
        'Pick a verdict and enter the observed reading.',
        'Elija un veredicto e introduzca el valor constatado.',
        'اختر حكمًا وأدخل القيمة المُلاحظة.',
        '判定を選び、実測値を入力してください。',
        '请选择判定并填写实测值。'),

    # ------------------------------------------------ ce que le verdict déclenche
    'capa.evaluation.outcome-effective': (
        'Le dossier sera clôturé.', 'The case will be closed.',
        'El caso se cerrará.', 'سيُغلق الملف.',
        '案件はクローズされます。', '案件将被关闭。'),
    'capa.evaluation.outcome-partial': (
        'Le dossier repartira au travail : une action complémentaire est attendue.',
        'The case goes back to work: a further action is expected.',
        'El caso vuelve al trabajo: se espera una acción complementaria.',
        'يعود الملف إلى العمل: يُنتظر إجراء تكميلي.',
        '案件は作業に戻ります。追加処置が必要です。',
        '案件将重回处理：需补充一项措施。'),
    'capa.evaluation.outcome-ineffective': (
        "Le dossier sera rouvert : l'analyse des causes est à reprendre (5 Pourquoi, Ishikawa).",
        'The case will be reopened: the root-cause analysis must be redone (5 Whys, Ishikawa).',
        'El caso se reabrirá: hay que retomar el análisis de causas (5 Por qué, Ishikawa).',
        'سيُعاد فتح الملف: يجب إعادة تحليل الأسباب (الأسباب الخمسة، إيشيكاوا).',
        '案件は再開されます。原因分析をやり直してください（なぜなぜ5回、特性要因図）。',
        '案件将被重开：须重做原因分析（5 why、鱼骨图）。'),
    'capa.evaluation.done-effective': (
        "Dossier clôturé : l'action est jugée efficace.",
        'Case closed: the action is judged effective.',
        'Caso cerrado: la acción se juzga eficaz.',
        'أُغلق الملف: حُكم بفعالية الإجراء.',
        '案件をクローズしました。処置は有効と判定されました。',
        '案件已关闭：措施判定为有效。'),
    'capa.evaluation.done-partial': (
        'Efficacité partielle : une action complémentaire est attendue sur le dossier.',
        'Partial effectiveness: a further action is expected on the case.',
        'Eficacia parcial: se espera una acción complementaria en el caso.',
        'فعالية جزئية: يُنتظر إجراء تكميلي على الملف.',
        '部分的に有効です。案件に追加処置が必要です。',
        '部分有效：该案件需补充一项措施。'),
    'capa.evaluation.done-ineffective': (
        "Dossier rouvert : l'analyse des causes est à reprendre.",
        'Case reopened: the root-cause analysis must be redone.',
        'Caso reabierto: hay que retomar el análisis de causas.',
        'أُعيد فتح الملف: يجب إعادة تحليل الأسباب.',
        '案件を再開しました。原因分析をやり直してください。',
        '案件已重开：须重做原因分析。'),

    # ------------------------------------------------------------- les verrous
    'capa.evaluation.lock-no-action': (
        "Aucune action n'est encore portée par ce dossier : il n'y a rien dont juger l'effet.",
        'This case carries no action yet: there is nothing whose effect could be judged.',
        'Este caso aún no tiene ninguna acción: no hay nada cuyo efecto juzgar.',
        'لا يحمل هذا الملف أي إجراء بعد: لا شيء يمكن الحكم على أثره.',
        'この案件にはまだ処置がありません。効果を判定する対象が存在しません。',
        '此案件尚无任何措施：没有可判定成效的对象。'),
    'capa.evaluation.lock-one-action': (
        "Une action reste à mener : l'évaluation s'ouvrira quand elle sera faite.",
        'One action remains: the evaluation opens once it is done.',
        'Queda una acción por realizar: la evaluación se abrirá cuando esté hecha.',
        'بقي إجراء واحد: يُفتح التقييم عند إتمامه.',
        '処置が1件残っています。完了すると評価が開きます。',
        '还剩 1 项措施：完成后即可开始评价。'),
    'capa.evaluation.lock-many-actions': (
        "{$INTERPOLATION} actions restent à mener : l'évaluation s'ouvrira quand elles seront "
        "faites.",
        '{$INTERPOLATION} actions remain: the evaluation opens once they are done.',
        'Quedan {$INTERPOLATION} acciones por realizar: la evaluación se abrirá cuando estén '
        'hechas.',
        'بقي {$INTERPOLATION} إجراءات: يُفتح التقييم عند إتمامها.',
        '処置が {$INTERPOLATION} 件残っています。完了すると評価が開きます。',
        '还剩 {$INTERPOLATION} 项措施：全部完成后即可开始评价。'),
    'capa.evaluation.lock-criterion': (
        "Aucun critère d'efficacité n'est posé sur ce dossier. Il se pose sur sa fiche, et il "
        "doit l'être avant le jugement : choisir le critère en regardant le résultat ne juge "
        "plus rien.",
        'No effectiveness criterion is set on this case. It is set on the case itself, and must '
        'be set before the verdict: picking the criterion after seeing the result judges '
        'nothing.',
        'Este caso no tiene criterio de eficacia. Se define en su ficha, y debe hacerse antes '
        'del juicio: elegir el criterio tras ver el resultado no juzga nada.',
        'لا يوجد معيار فعالية على هذا الملف. يُحدَّد في بطاقته، ويجب تحديده قبل الحكم: اختيار المعيار بعد رؤية '
        'النتيجة لا يحكم على شيء.',
        'この案件には有効性判定基準がありません。基準は案件票で設定し、判定より前に定める必要があります。結果を見てから'
        '基準を選んでは、何も判定していません。',
        '此案件未设定有效性判定标准。标准在案件卡中设定，且必须在判定之前确定：看到结果再挑标准，等于什么都没判定。'),
    'capa.evaluation.lock-done': (
        "Ce dossier est clôturé sur un verdict d'efficacité. Son historique reste lisible ; il "
        "ne se rejuge pas.",
        'This case is closed on an effectiveness verdict. Its history stays readable; it is not '
        'judged again.',
        'Este caso está cerrado con un veredicto de eficacia. Su historial sigue siendo '
        'legible; no se vuelve a juzgar.',
        'أُغلق هذا الملف بحكم فعالية. يبقى سجله قابلًا للقراءة؛ ولا يُعاد الحكم عليه.',
        'この案件は有効性の判定によりクローズされています。履歴は閲覧できますが、再判定はできません。',
        '此案件已基于有效性判定关闭。其历史仍可查阅，但不再重新判定。'),
    'capa.evaluation.lock-judged': (
        "Ce dossier attend son action complémentaire. Il se présentera de nouveau au jugement "
        "quand elle sera menée à terme.",
        'This case is waiting for its further action. It comes up for judgement again once that '
        'action is complete.',
        'Este caso espera su acción complementaria. Se presentará de nuevo a juicio cuando esté '
        'terminada.',
        'ينتظر هذا الملف إجراءه التكميلي. وسيُعرض للحكم مرة أخرى عند إتمامه.',
        'この案件は追加処置を待っています。処置が完了すると、再び判定の対象になります。',
        '此案件正在等待其补充措施。该措施完成后，案件将再次提交判定。'),
    # ------------------------------------------- le critère, là où il se pose
    # Un seul libellé de champ pour les trois écrans qui le portent — fenêtre
    # d'ouverture, fenêtre de modification, fiche du dossier : trois noms pour
    # le même champ feraient croire à trois champs.
    'capa.criterion.label': (
        "Critère d'efficacité", 'Effectiveness criterion', 'Criterio de eficacia',
        'معيار الفعالية', '有効性の判定基準', '有效性判定标准'),
    'capa.criterion.hint': (
        "À quoi on reconnaîtra l'efficacité : « zéro erreur de référence sur 90 jours ».",
        'How effectiveness will be recognised: "zero reference errors over 90 days".',
        'Cómo se reconocerá la eficacia: «cero errores de referencia en 90 días».',
        'كيف ستُعرف الفعالية: «صفر خطأ في المرجع خلال 90 يومًا».',
        '有効性をどう判断するか: 「90 日間、品番の誤りがゼロ」。',
        '如何判断有效：「90 天内零物料号差错」。'),
    'capa.criterion.locked': (
        "Ce dossier a déjà été évalué : son critère ne change plus, les verdicts rendus l'ont "
        'été contre lui.',
        'This case has already been evaluated: its criterion no longer changes, as the verdicts '
        'were rendered against it.',
        'Este caso ya ha sido evaluado: su criterio ya no cambia, los veredictos se emitieron '
        'frente a él.',
        'سبق تقييم هذا الملف: لم يعد معياره يتغير، فالأحكام صدرت في مواجهته.',
        'この案件はすでに評価済みです。判定はこの基準に対して下されたため、基準は変更できません。',
        '此案件已完成评价：其判定标准不再变更，已下的判定均以该标准为依据。'),
    'capa.detail.evaluate-link': (
        'Évaluer contre le critère, avec un verdict nuancé',
        'Evaluate against the criterion, with a three-way verdict',
        'Evaluar frente al criterio, con un veredicto matizado',
        'التقييم في مواجهة المعيار، بحكم متدرّج',
        '基準に対して評価する（三段階の判定）',
        '按标准评价（三档判定）'),
}
