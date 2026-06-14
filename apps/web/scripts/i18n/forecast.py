# -*- coding: utf-8 -*-
"""Table i18n - prévision KPI (Holt-Winters, §6.5/§12.1). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.forecast': ('Prévision KPI', 'KPI forecast', 'Previsión de KPI', 'التنبؤ بمؤشرات الأداء', 'KPI予測', 'KPI预测'),

    'forecast.kpi.title': ('Prévision KPI', 'KPI forecast', 'Previsión de KPI', 'التنبؤ بمؤشرات الأداء', 'KPI予測', 'KPI预测'),
    'forecast.kpi.subtitle': ("Projection par lissage exponentiel Holt-Winters (niveau, tendance, saisonnalité) et probabilité d'atteindre la cible — calculée par l'IA.", 'Projection via Holt-Winters exponential smoothing (level, trend, seasonality) and probability of reaching the target — computed by AI.', 'Proyección por suavizado exponencial Holt-Winters (nivel, tendencia, estacionalidad) y probabilidad de alcanzar el objetivo — calculada por IA.', 'إسقاط عبر التنعيم الأسي Holt-Winters (المستوى، الاتجاه، الموسمية) واحتمال بلوغ الهدف — محسوب بالذكاء الاصطناعي.', 'Holt-Winters指数平滑（水準・トレンド・季節性）による予測と目標到達確率をAIが算出。', '通过 Holt-Winters 指数平滑（水平、趋势、季节性）进行预测，并计算达到目标的概率 — 由AI计算。'),

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
}
