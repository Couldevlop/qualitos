# -*- coding: utf-8 -*-
"""Table i18n - prévision KPI (Holt-Winters, §6.5/§12.1). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.forecast': ('Prévision KPI', 'KPI forecast', 'Previsión de KPI', 'التنبؤ بمؤشرات الأداء', 'KPI予測', 'KPI预测'),

    'forecast.kpi.title': ('Prévision KPI', 'KPI forecast', 'Previsión de KPI', 'التنبؤ بمؤشرات الأداء', 'KPI予測', 'KPI预测'),
    'forecast.kpi.subtitle': ("Projection par lissage exponentiel Holt-Winters (niveau, tendance, saisonnalité) et probabilité d'atteindre la cible.", 'Projection via Holt-Winters exponential smoothing (level, trend, seasonality) and probability of reaching the target.', 'Proyección por suavizado exponencial Holt-Winters (nivel, tendencia, estacionalidad) y probabilidad de alcanzar el objetivo.', 'إسقاط عبر التنعيم الأسي Holt-Winters (المستوى، الاتجاه، الموسمية) واحتمال بلوغ الهدف.', 'Holt-Winters指数平滑（水準・トレンド・季節性）による予測と目標到達確率。', '通过 Holt-Winters 指数平滑（水平、趋势、季节性）进行预测，并计算达到目标的概率。'),

    'forecast.kpi.series-panel': ('Série & cible', 'Series & target', 'Serie y objetivo', 'السلسلة والهدف', '系列と目標', '序列与目标'),
    'forecast.kpi.measures-label': ('Mesures (séparées par virgule, espace ou retour ligne)', 'Measurements (separated by comma, space or line break)', 'Mediciones (separadas por coma, espacio o salto de línea)', 'القياسات (مفصولة بفاصلة أو مسافة أو سطر جديد)', '測定値（カンマ・スペース・改行で区切る）', '测量值（用逗号、空格或换行分隔）'),
    'forecast.kpi.target-label': ('Cible', 'Target', 'Objetivo', 'الهدف', '目標', '目标'),
    'forecast.kpi.horizon-label': ('Horizon (périodes)', 'Horizon (periods)', 'Horizonte (periodos)', 'الأفق (الفترات)', '予測期間（期）', '预测期（期数）'),
    'forecast.kpi.direction-label': ('Sens', 'Direction', 'Sentido', 'الاتجاه', '方向', '方向'),
    'forecast.kpi.dir-at-least': ('atteindre au moins', 'reach at least', 'alcanzar al menos', 'بلوغ ما لا يقل عن', '少なくとも到達', '至少达到'),
    'forecast.kpi.dir-at-most': ('atteindre au plus', 'reach at most', 'alcanzar como máximo', 'بلوغ ما لا يزيد عن', '多くても到達', '至多达到'),
    'forecast.kpi.seasonal-label': ('Période saisonnière', 'Seasonal period', 'Periodo estacional', 'الفترة الموسمية', '季節周期', '季节周期'),
    'forecast.kpi.seasonal-hint': ('Optionnel (ex. 7, 12) — sinon Holt linéaire', 'Optional (e.g. 7, 12) — otherwise linear Holt', 'Opcional (p. ej. 7, 12) — de lo contrario, Holt lineal', 'اختياري (مثل 7، 12) — وإلا فهولت الخطي', '任意（例: 7, 12）— 指定しない場合は線形Holt', '可选（如 7、12）— 否则使用线性 Holt'),
    'forecast.kpi.run': ('Prévoir', 'Forecast', 'Prever', 'تنبّأ', '予測', '预测'),
    'forecast.kpi.running': ('Calcul…', 'Computing…', 'Calculando…', 'جارٍ الحساب…', '計算中…', '计算中…'),
    'forecast.kpi.example': ('Exemple', 'Example', 'Ejemplo', 'مثال', '例', '示例'),

    'forecast.kpi.help-panel': ('Comment ça marche', 'How it works', 'Cómo funciona', 'كيف يعمل', '仕組み', '工作原理'),
    'forecast.kpi.help-body': ("Le modèle Holt-Winters lisse le niveau et la tendance de la série, et, si une période saisonnière est fournie, sa composante saisonnière. La probabilité estime la chance d'atteindre la cible à l'horizon, compte tenu de l'incertitude (intervalle à 95 %).", 'The Holt-Winters model smooths the series level and trend, plus its seasonal component when a seasonal period is provided. The probability estimates the chance of reaching the target at the horizon, given uncertainty (95 % interval).', 'El modelo Holt-Winters suaviza el nivel y la tendencia de la serie y, si se indica un periodo estacional, su componente estacional. La probabilidad estima la posibilidad de alcanzar el objetivo en el horizonte, considerando la incertidumbre (intervalo del 95 %).', 'يقوم نموذج Holt-Winters بتنعيم مستوى السلسلة واتجاهها، وكذلك مكوّنها الموسمي عند توفير فترة موسمية. يقدّر الاحتمال فرصة بلوغ الهدف عند الأفق مع مراعاة عدم اليقين (فاصل 95٪).', 'Holt-Winters モデルは系列の水準とトレンドを平滑化し、季節周期が指定されればその季節成分も平滑化します。確率は不確実性（95%区間）を考慮して、期末に目標へ到達する可能性を推定します。', 'Holt-Winters 模型平滑序列的水平和趋势，若提供季节周期则平滑其季节成分。在考虑不确定性（95% 区间）的情况下，概率估计在预测期末达到目标的可能性。'),

    'forecast.kpi.card-probability': ("Probabilité d'atteinte", 'Probability of reaching', 'Probabilidad de alcanzar', 'احتمال البلوغ', '到達確率', '达到概率'),
    'forecast.kpi.card-confidence': ('Confiance', 'Confidence', 'Confianza', 'الثقة', '信頼度', '置信度'),
    'forecast.kpi.card-model': ('Modèle', 'Model', 'Modelo', 'النموذج', 'モデル', '模型'),
    'forecast.kpi.model-holt': ('Holt (niveau + tendance)', 'Holt (level + trend)', 'Holt (nivel + tendencia)', 'هولت (المستوى + الاتجاه)', 'Holt（水準+トレンド）', 'Holt（水平+趋势）'),
    'forecast.kpi.model-hw': ('Holt-Winters (saisonnier)', 'Holt-Winters (seasonal)', 'Holt-Winters (estacional)', 'هولت-وينترز (موسمي)', 'Holt-Winters（季節性）', 'Holt-Winters（季节性）'),

    'forecast.kpi.chart-panel': ('Historique & prévision', 'History & forecast', 'Histórico y previsión', 'السجل والتنبؤ', '履歴と予測', '历史与预测'),
    'forecast.kpi.table-panel': ('Points projetés', 'Projected points', 'Puntos proyectados', 'النقاط المتوقعة', '予測点', '预测点'),
    'forecast.kpi.col-step': ('Horizon', 'Horizon', 'Horizonte', 'الأفق', '期', '期'),
    'forecast.kpi.col-value': ('Prévision', 'Forecast', 'Previsión', 'التنبؤ', '予測', '预测'),
    'forecast.kpi.col-interval': ('Intervalle 95 %', '95 % interval', 'Intervalo 95 %', 'فاصل 95٪', '95%区間', '95% 区间'),

    'forecast.kpi.err-min-points': ('Saisissez au moins 4 mesures numériques.', 'Enter at least 4 numeric measurements.', 'Introduzca al menos 4 mediciones numéricas.', 'أدخل 4 قياسات رقمية على الأقل.', '少なくとも4つの数値測定を入力してください。', '请至少输入 4 个数值测量。'),
    'forecast.kpi.err-target': ('Indiquez une valeur cible.', 'Provide a target value.', 'Indique un valor objetivo.', 'حدّد قيمة هدف.', '目標値を指定してください。', '请提供目标值。'),
    'forecast.kpi.err-seasonal': ('La période saisonnière doit être comprise entre 2 et 365.', 'The seasonal period must be between 2 and 365.', 'El periodo estacional debe estar entre 2 y 365.', 'يجب أن تكون الفترة الموسمية بين 2 و365.', '季節周期は2〜365の範囲でなければなりません。', '季节周期必须在 2 到 365 之间。'),
    'forecast.kpi.err-backend': ('Backend injoignable (engine sur 8082 ?).', 'Backend unreachable (engine on 8082?).', 'Backend inaccesible (¿engine en 8082?).', 'تعذّر الوصول إلى الخادم (engine على 8082؟).', 'バックエンドに到達できません（engine は 8082？）。', '无法连接后端（engine 在 8082？）。'),
    'forecast.kpi.err-invalid': ('Requête invalide (≥ 4 mesures, horizon 1–60, cible requise).', 'Invalid request (≥ 4 measurements, horizon 1–60, target required).', 'Solicitud no válida (≥ 4 mediciones, horizonte 1–60, objetivo requerido).', 'طلب غير صالح (≥ 4 قياسات، الأفق 1–60، الهدف مطلوب).', '無効なリクエスト（測定 ≥ 4、予測期間 1–60、目標必須）。', '请求无效（≥ 4 个测量，预测期 1–60，目标必填）。'),
    'forecast.kpi.err-too-large': ('Série trop volumineuse (garde-fou IA).', 'Series too large (AI guardrail).', 'Serie demasiado grande (barrera de IA).', 'السلسلة كبيرة جدًا (حاجز أمان الذكاء الاصطناعي).', '系列が大きすぎます（AIガードレール）。', '序列过大（AI 防护）。'),
    'forecast.kpi.err-unprocessable': ('Série insuffisante pour une prévision fiable.', 'Series insufficient for a reliable forecast.', 'Serie insuficiente para una previsión fiable.', 'السلسلة غير كافية لتنبؤ موثوق.', '信頼できる予測には系列が不十分です。', '序列不足以进行可靠预测。'),
    'forecast.kpi.err-quota': ('Débit/quota IA dépassé pour ce tenant — réessayez plus tard.', 'AI rate/quota exceeded for this tenant — try again later.', 'Tasa/cuota de IA superada para este inquilino — inténtelo más tarde.', 'تم تجاوز معدّل/حصة الذكاء الاصطناعي لهذا المستأجر — أعد المحاولة لاحقًا.', 'このテナントのAIレート/クォータを超過しました — 後で再試行してください。', '该租户的 AI 速率/配额已超出 — 请稍后重试。'),
    'forecast.kpi.err-gateway': ('Passerelle IA indisponible (ai-service injoignable).', 'AI gateway unavailable (ai-service unreachable).', 'Pasarela de IA no disponible (ai-service inaccesible).', 'بوابة الذكاء الاصطناعي غير متاحة (ai-service غير قابل للوصول).', 'AIゲートウェイが利用できません（ai-service に到達不可）。', 'AI 网关不可用（无法连接 ai-service）。'),
    'forecast.kpi.err-unavailable': ('Service IA momentanément indisponible (disjoncteur ouvert).', 'AI service temporarily unavailable (circuit breaker open).', 'Servicio de IA temporalmente no disponible (disyuntor abierto).', 'خدمة الذكاء الاصطناعي غير متاحة مؤقتًا (قاطع الدائرة مفتوح).', 'AIサービスは一時的に利用できません（サーキットブレーカー作動中）。', 'AI 服务暂时不可用（断路器已打开）。'),
    'forecast.kpi.err-generic': ('Échec de la prévision (HTTP {$status}).', 'Forecast failed (HTTP {$status}).', 'Fallo de la previsión (HTTP {$status}).', 'فشل التنبؤ (HTTP {$status}).', '予測に失敗しました（HTTP {$status}）。', '预测失败（HTTP {$status}）。'),

    # --- a quoi sert cet ecran, et ce a quoi il est relie -------------------
    'forecast.kpi.role-panel': (
        'À quoi sert cette prévision', 'What this forecast is for',
        'Para qué sirve esta previsión', 'ما الغرض من هذا التوقّع',
        'この予測の目的', '此预测的用途'),
    'forecast.kpi.role-what': (
        "Elle répond à une seule question : {$START_TAG_STRONG}au rythme actuel, l'indicateur atteindra-t-il sa cible, et quand ?{$CLOSE_TAG_STRONG} Le lissage exponentiel Holt-Winters prolonge le niveau, la tendance et, s'il y en a une, la saisonnalité de la série. L'intervalle à 95 % dit l'incertitude : une prévision sans intervalle se lit comme une certitude qu'elle n'est pas.",
        'It answers a single question: {$START_TAG_STRONG}at the current pace, will the indicator reach its target, and when?{$CLOSE_TAG_STRONG} Holt-Winters exponential smoothing extends the level, the trend and, where there is one, the seasonality of the series. The 95 % interval states the uncertainty: a forecast without an interval reads as a certainty it is not.',
        'Responde a una sola pregunta: {$START_TAG_STRONG}al ritmo actual, ¿alcanzará el indicador su objetivo, y cuándo?{$CLOSE_TAG_STRONG} El suavizado exponencial Holt-Winters prolonga el nivel, la tendencia y, si la hay, la estacionalidad de la serie. El intervalo del 95 % expresa la incertidumbre: una previsión sin intervalo se lee como una certeza que no es.',
        'يجيب على سؤال واحد: {$START_TAG_STRONG}هل سيبلغ المؤشر هدفه بالوتيرة الحالية، ومتى؟{$CLOSE_TAG_STRONG} يمدّ تمهيد هولت-وينترز الأسّي مستوى السلسلة واتجاهها وموسميتها إن وُجدت. ويعبّر المجال بنسبة 95 % عن عدم اليقين: فالتوقّع بلا مجال يُقرأ كأنه يقين، وهو ليس كذلك.',
        '答えるのは一つの問いだけです。{$START_TAG_STRONG}現在のペースで、この指標は目標に届くのか、そしていつか。{$CLOSE_TAG_STRONG}ホルト・ウィンタース指数平滑法が、系列の水準・トレンドおよび季節性（あれば）を延長します。95 % 区間は不確かさを示します。区間のない予測は、そうではない確信として読まれてしまいます。',
        '它只回答一个问题：{$START_TAG_STRONG}按当前节奏，该指标能否达到目标，何时达到？{$CLOSE_TAG_STRONG} Holt-Winters 指数平滑法延伸序列的水平、趋势以及季节性（若存在）。95 % 区间表达不确定性：没有区间的预测会被读成一种它并不具备的确定性。'),
    'forecast.kpi.role-when': (
        "Elle sert à décider {$START_TAG_EM}avant{$CLOSE_TAG_EM} l'échéance : relancer un cycle d'amélioration qui n'atteindra pas son objectif, renforcer une action dont l'effet s'essouffle, ou au contraire ne rien changer à ce qui converge. Sur une série courte ou erratique, la probabilité d'atteinte reste basse et l'intervalle large — c'est le signal qu'il n'y a pas encore de tendance à prolonger, pas une invitation à y lire une baisse.",
        'It is there to decide {$START_TAG_EM}before{$CLOSE_TAG_EM} the deadline: restart an improvement cycle that will not meet its objective, strengthen an action whose effect is fading, or leave alone what is converging. On a short or erratic series the probability stays low and the interval wide — the signal that there is no trend to extend yet, not an invitation to read a decline into it.',
        'Sirve para decidir {$START_TAG_EM}antes{$CLOSE_TAG_EM} del vencimiento: relanzar un ciclo de mejora que no alcanzará su objetivo, reforzar una acción cuyo efecto se agota, o no tocar lo que converge. En una serie corta o errática, la probabilidad se mantiene baja y el intervalo ancho: es la señal de que aún no hay tendencia que prolongar, no una invitación a leer una caída.',
        'وهي أداة للقرار {$START_TAG_EM}قبل{$CLOSE_TAG_EM} حلول الموعد: إعادة إطلاق دورة تحسين لن تبلغ هدفها، أو تعزيز إجراء خبا أثره، أو ترك ما يتقارب على حاله. وفي سلسلة قصيرة أو متقلّبة يبقى الاحتمال منخفضًا والمجال واسعًا، وهذا دليل على أنه لا يوجد اتجاه يمكن مدّه بعد، لا دعوة لقراءة انخفاض فيه.',
        '期限を迎える{$START_TAG_EM}前{$CLOSE_TAG_EM}に判断するためのものです。目標に届かない改善サイクルを立て直す、効果が弱まった対策を強める、収束しているものには手を入れない。系列が短い、あるいは乱れている場合、達成確率は低く区間は広いままになります。これは延長すべき傾向がまだないという合図であって、低下と読み取るべき根拠ではありません。',
        '它用于在到期{$START_TAG_EM}之前{$CLOSE_TAG_EM}作出决定：重启达不到目标的改进循环、加强效果正在减弱的措施，或者对正在收敛的指标不作改动。序列过短或波动剧烈时，达成概率偏低、区间偏宽——这是尚无趋势可供延伸的信号，而不是可以据此判定下降。'),
    'forecast.kpi.role-links-title': (
        'Ce à quoi elle est reliée', 'What it is connected to', 'Con qué está conectada',
        'بماذا يرتبط', 'どことつながっているか', '它与什么相连'),
    'forecast.kpi.role-link-kpi': (
        'Catalogue des indicateurs', 'Indicator catalogue', 'Catálogo de indicadores',
        'كتالوج المؤشرات', '指標カタログ', '指标目录'),
    'forecast.kpi.role-link-kpi-text': (
        "La source des mesures. Choisir un indicateur actif remplit la série, sa cible et le sens d'amélioration : aucune ressaisie, et le chiffre projeté porte sur la même définition que celui des tableaux de bord.",
        'The source of the readings. Picking an active indicator fills in the series, its target and the direction of improvement: no retyping, and the projected figure rests on the same definition as the dashboards.',
        'La fuente de las mediciones. Elegir un indicador activo rellena la serie, su objetivo y el sentido de mejora: sin volver a teclear, y la cifra proyectada se apoya en la misma definición que los paneles.',
        'مصدر القياسات. اختيار مؤشّر نشط يملأ السلسلة وهدفها واتجاه التحسين: بلا إدخال مكرّر، ويستند الرقم المتوقّع إلى التعريف نفسه الذي تستخدمه لوحات المعلومات.',
        '測定値の出どころです。有効な指標を選ぶと、系列・目標・改善の向きが入ります。再入力は不要で、予測値はダッシュボードと同じ定義に基づきます。',
        '测量值的来源。选择一个启用的指标即可填入序列、目标值和改进方向：无需重新录入，预测数字与仪表盘采用同一定义。'),
    'forecast.kpi.role-link-spc': (
        'Cartes de contrôle (SPC)', 'Control charts (SPC)', 'Gráficos de control (SPC)',
        'خرائط التحكّم (SPC)', '管理図（SPC）', '控制图（SPC）'),
    'forecast.kpi.role-link-spc-text': (
        "Lecture complémentaire, et non concurrente. Les règles de Nelson disent que le procédé a {$START_TAG_EM}déjà{$CLOSE_TAG_EM} dérivé ; la prévision dit où la série va si rien ne change. L'une constate, l'autre projette.",
        'A complementary reading, not a competing one. The Nelson rules say the process has {$START_TAG_EM}already{$CLOSE_TAG_EM} drifted; the forecast says where the series is heading if nothing changes. One observes, the other projects.',
        'Una lectura complementaria, no competidora. Las reglas de Nelson dicen que el proceso {$START_TAG_EM}ya{$CLOSE_TAG_EM} se ha desviado; la previsión dice hacia dónde va la serie si nada cambia. Una constata, la otra proyecta.',
        'قراءة مكمّلة لا منافسة. قواعد نيلسون تقول إنّ العملية {$START_TAG_EM}قد{$CLOSE_TAG_EM} انحرفت فعلاً؛ أمّا التوقّع فيقول إلى أين تتّجه السلسلة إن لم يتغيّر شيء. الأولى تُثبت، والثانية تمدّ.',
        '競合ではなく補完の読み方です。ネルソンのルールは工程が{$START_TAG_EM}すでに{$CLOSE_TAG_EM}外れたことを示し、予測は何も変えなければ系列がどこへ向かうかを示します。一方は確認し、他方は延長します。',
        '这是互补的读法，而非相互替代。Nelson 规则说明过程{$START_TAG_EM}已经{$CLOSE_TAG_EM}发生偏移；预测说明若不作改变序列将走向何处。前者确认，后者外推。'),
    'forecast.kpi.role-link-manual': (
        'Ce qui reste à votre main', 'What stays in your hands', 'Lo que queda en sus manos',
        'ما يبقى بيدك', '人の手に残ること', '仍由您掌握的部分'),
    'forecast.kpi.role-link-manual-text': (
        "La prévision n'ouvre rien d'elle-même : ni CAPA, ni cycle PDCA, ni projet DMAIC. Elle éclaire la décision, et c'est depuis le module concerné qu'on l'engage — une projection n'est pas un constat, et elle ne doit pas déclencher d'action au registre.",
        'The forecast opens nothing by itself: no CAPA, no PDCA cycle, no DMAIC project. It informs the decision, and the decision is taken from the module concerned — a projection is not an observed fact, and it must not enter an action in the register.',
        'La previsión no abre nada por sí misma: ni CAPA, ni ciclo PDCA, ni proyecto DMAIC. Informa la decisión, y esta se toma desde el módulo correspondiente: una proyección no es un hecho constatado y no debe inscribir una acción en el registro.',
        'لا يفتح التوقّع شيئًا من تلقاء نفسه: لا إجراء CAPA ولا دورة PDCA ولا مشروع DMAIC. إنه يُنير القرار، والقرار يُتّخذ من الوحدة المعنيّة؛ فالتوقّع ليس واقعة مُثبتة ولا ينبغي أن يسجّل إجراءً في السجل.',
        '予測はそれ自体では何も起票しません。CAPA も PDCA サイクルも DMAIC プロジェクトも開きません。判断を助けるだけで、着手はそれぞれのモジュールから行います。予測は確認された事実ではなく、台帳に対策を登録する根拠にはなりません。',
        '预测本身不会开启任何事项：既不开 CAPA，也不开 PDCA 循环或 DMAIC 项目。它只为决策提供依据，而决策要在相应模块中发起——预测不是既成事实，不应据此在登记簿中登记措施。'),
}
