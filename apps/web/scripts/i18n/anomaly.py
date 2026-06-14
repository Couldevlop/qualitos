# -*- coding: utf-8 -*-
"""Table i18n - détection d'anomalies non-supervisée (§3.4, §12.1). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.anomaly': ('Anomalies IA', 'AI anomalies', 'Anomalías IA', 'الحالات الشاذة بالذكاء الاصطناعي', 'AI異常検出', 'AI异常检测'),

    'anomaly.detect.title': ("Détection d'anomalies non-supervisée", 'Unsupervised anomaly detection', 'Detección de anomalías no supervisada', 'كشف الحالات الشاذة دون إشراف', '教師なし異常検出', '无监督异常检测'),
    'anomaly.detect.subtitle': ("Détection multivariée par Isolation Forest ou reconstruction (ACP), calculée par l'IA — au-delà des règles SPC univariées.", 'Multivariate detection via Isolation Forest or reconstruction (PCA), computed by AI — beyond univariate SPC rules.', 'Detección multivariante por Isolation Forest o reconstrucción (ACP), calculada por IA — más allá de las reglas SPC univariantes.', 'كشف متعدد المتغيرات عبر Isolation Forest أو إعادة البناء (ACP)، محسوب بالذكاء الاصطناعي — يتجاوز قواعد SPC أحادية المتغير.', 'Isolation Forest または再構成（PCA）による多変量検出をAIが算出 — 単変量SPCルールを超える検出。', '通过 Isolation Forest 或重构（PCA）进行多变量检测，由AI计算 — 超越单变量SPC规则。'),

    'anomaly.detect.matrix-panel': ('Matrice de données', 'Data matrix', 'Matriz de datos', 'مصفوفة البيانات', 'データ行列', '数据矩阵'),
    'anomaly.detect.matrix-label': ('Échantillons (une ligne par échantillon, features séparées par virgule ou espace)', 'Samples (one row per sample, features separated by comma or space)', 'Muestras (una fila por muestra, características separadas por coma o espacio)', 'العينات (سطر لكل عينة، الميزات مفصولة بفاصلة أو مسافة)', 'サンプル（1行 = 1サンプル、特徴量はカンマまたはスペースで区切る）', '样本（每行一个样本，特征用逗号或空格分隔）'),
    'anomaly.detect.method-label': ('Méthode', 'Method', 'Método', 'الطريقة', '手法', '方法'),
    'anomaly.detect.method-iforest': ('Isolation Forest', 'Isolation Forest', 'Isolation Forest', 'Isolation Forest', 'Isolation Forest', 'Isolation Forest'),
    'anomaly.detect.method-reconstruction': ('Reconstruction (ACP)', 'Reconstruction (PCA)', 'Reconstrucción (ACP)', 'إعادة البناء (ACP)', '再構成（PCA）', '重构（PCA）'),
    'anomaly.detect.contamination-label': ('Contamination', 'Contamination', 'Contaminación', 'نسبة التلوث', '汚染率', '污染率'),
    'anomaly.detect.contamination-hint': ("Fraction d'anomalies attendue ∈ ]0 ; 0,5]", 'Expected anomaly fraction ∈ (0, 0.5]', 'Fracción de anomalías esperada ∈ ]0 ; 0,5]', 'نسبة الشذوذ المتوقعة ∈ ]0 ؛ 0.5]', '想定異常割合 ∈ (0, 0.5]', '预期异常比例 ∈ (0, 0.5]'),
    'anomaly.detect.threshold-label': ('Seuil explicite', 'Explicit threshold', 'Umbral explícito', 'عتبة صريحة', '明示しきい値', '显式阈值'),
    'anomaly.detect.threshold-hint': ('Optionnel — sinon quantile de contamination', 'Optional — otherwise contamination quantile', 'Opcional — de lo contrario, cuantil de contaminación', 'اختياري — وإلا فكمّي نسبة التلوث', '任意 — 指定しない場合は汚染率の分位点', '可选 — 否则使用污染率分位数'),
    'anomaly.detect.detect': ('Détecter les anomalies', 'Detect anomalies', 'Detectar anomalías', 'كشف الحالات الشاذة', '異常を検出', '检测异常'),
    'anomaly.detect.detecting': ('Détection…', 'Detecting…', 'Detectando…', 'جارٍ الكشف…', '検出中…', '检测中…'),
    'anomaly.detect.example': ('Exemple', 'Example', 'Ejemplo', 'مثال', '例', '示例'),

    'anomaly.detect.help-panel': ('Quelle méthode ?', 'Which method?', '¿Qué método?', 'أيّ طريقة؟', 'どの手法？', '选择哪种方法？'),
    'anomaly.detect.help-iforest': ('— isole les points rares par partitions aléatoires. Robuste, sans hypothèse de distribution.', '— isolates rare points via random partitions. Robust, with no distribution assumption.', '— aísla los puntos raros mediante particiones aleatorias. Robusto, sin hipótesis de distribución.', '— يعزل النقاط النادرة عبر تقسيمات عشوائية. متين، دون افتراض توزيع.', '— ランダム分割で稀な点を分離。分布を仮定せず頑健。', '— 通过随机划分隔离稀有点。稳健，无分布假设。'),
    'anomaly.detect.help-reconstruction': ('— erreur de reconstruction sur les axes principaux (auto-encodeur linéaire). Indique la feature la plus en cause.', '— reconstruction error on principal axes (linear autoencoder). Highlights the most contributing feature.', '— error de reconstrucción en los ejes principales (autocodificador lineal). Señala la característica más implicada.', '— خطأ إعادة البناء على المحاور الرئيسية (مُرمِّز ذاتي خطي). يُبرز الميزة الأكثر تأثيرًا.', '— 主成分軸上の再構成誤差（線形オートエンコーダ）。最も寄与する特徴量を示す。', '— 主成分轴上的重构误差（线性自编码器）。指出贡献最大的特征。'),

    'anomaly.detect.card-anomalies': ('Anomalies détectées', 'Anomalies detected', 'Anomalías detectadas', 'الحالات الشاذة المكتشفة', '検出された異常', '检测到的异常'),
    'anomaly.detect.card-method': ('Méthode', 'Method', 'Método', 'الطريقة', '手法', '方法'),
    'anomaly.detect.card-threshold': ('Seuil appliqué', 'Applied threshold', 'Umbral aplicado', 'العتبة المطبقة', '適用しきい値', '应用阈值'),
    'anomaly.detect.card-threshold-desc': ('Score au-delà duquel un échantillon est marqué anormal', 'Score above which a sample is flagged as anomalous', 'Puntuación por encima de la cual una muestra se marca como anómala', 'الدرجة التي تُعَدّ العينة شاذة فوقها', 'これを超えるとサンプルが異常と判定されるスコア', '超过该分数的样本被标记为异常'),

    'anomaly.detect.chart-panel': ("Scores d'anomalie", 'Anomaly scores', 'Puntuaciones de anomalía', 'درجات الشذوذ', '異常スコア', '异常分数'),
    'anomaly.detect.axis-sample': ('Échantillon', 'Sample', 'Muestra', 'العينة', 'サンプル', '样本'),
    'anomaly.detect.axis-score': ('Score', 'Score', 'Puntuación', 'الدرجة', 'スコア', '分数'),
    'anomaly.detect.threshold-mark': ('Seuil', 'Threshold', 'Umbral', 'العتبة', 'しきい値', '阈值'),

    'anomaly.detect.table-panel': ('Anomalies détectées', 'Anomalies detected', 'Anomalías detectadas', 'الحالات الشاذة المكتشفة', '検出された異常', '检测到的异常'),
    'anomaly.detect.no-anomaly': ('Aucune anomalie au seuil retenu : les échantillons sont homogènes 🎉', 'No anomaly at the chosen threshold: the samples are homogeneous 🎉', 'Ninguna anomalía en el umbral elegido: las muestras son homogéneas 🎉', 'لا توجد حالات شاذة عند العتبة المختارة: العينات متجانسة 🎉', '選択したしきい値で異常なし：サンプルは均質です 🎉', '在所选阈值下无异常：样本是同质的 🎉'),
    'anomaly.detect.col-sample': ('Échantillon', 'Sample', 'Muestra', 'العينة', 'サンプル', '样本'),
    'anomaly.detect.col-score': ('Score', 'Score', 'Puntuación', 'الدرجة', 'スコア', '分数'),
    'anomaly.detect.col-top-feature': ('Feature dominante', 'Top feature', 'Característica dominante', 'الميزة الأبرز', '主要特徴量', '主要特征'),
    'anomaly.detect.feature-prefix': ('Feature', 'Feature', 'Característica', 'الميزة', '特徴量', '特征'),

    'anomaly.detect.err-no-data': ('Saisissez au moins un échantillon (une ligne de nombres).', 'Enter at least one sample (a row of numbers).', 'Introduzca al menos una muestra (una fila de números).', 'أدخل عينة واحدة على الأقل (سطر من الأرقام).', '少なくとも1つのサンプル（数値の行）を入力してください。', '请至少输入一个样本（一行数字）。'),
    'anomaly.detect.err-ragged': ('Toutes les lignes doivent avoir le même nombre de features (≥ 1).', 'All rows must have the same number of features (≥ 1).', 'Todas las filas deben tener el mismo número de características (≥ 1).', 'يجب أن تحتوي جميع الأسطر على العدد نفسه من الميزات (≥ 1).', 'すべての行は同じ数の特徴量（≥ 1）を持つ必要があります。', '所有行必须具有相同数量的特征（≥ 1）。'),
    'anomaly.detect.err-numbers': ('La matrice ne doit contenir que des nombres.', 'The matrix must contain only numbers.', 'La matriz solo debe contener números.', 'يجب أن تحتوي المصفوفة على أرقام فقط.', '行列には数値のみを含めてください。', '矩阵只能包含数字。'),
    'anomaly.detect.err-contamination': ("La contamination doit être dans l'intervalle ]0 ; 0,5].", 'Contamination must be within (0, 0.5].', 'La contaminación debe estar en el intervalo ]0 ; 0,5].', 'يجب أن تكون نسبة التلوث ضمن ]0 ؛ 0.5].', '汚染率は (0, 0.5] の範囲でなければなりません。', '污染率必须在 (0, 0.5] 范围内。'),

    'anomaly.detect.err-backend': ('Backend injoignable (engine sur 8082 ?).', 'Backend unreachable (engine on 8082?).', 'Backend inaccesible (¿engine en 8082?).', 'تعذّر الوصول إلى الخادم (engine على 8082؟).', 'バックエンドに到達できません（engine は 8082？）。', '无法连接后端（engine 在 8082？）。'),
    'anomaly.detect.err-invalid': ('Matrice invalide (1 à 50000 échantillons, contamination ∈ ]0 ; 0,5]).', 'Invalid matrix (1 to 50000 samples, contamination ∈ (0, 0.5]).', 'Matriz no válida (1 a 50000 muestras, contaminación ∈ ]0 ; 0,5]).', 'مصفوفة غير صالحة (من 1 إلى 50000 عينة، نسبة التلوث ∈ ]0 ؛ 0.5]).', '無効な行列（1〜50000サンプル、汚染率 ∈ (0, 0.5]）。', '矩阵无效（1 至 50000 个样本，污染率 ∈ (0, 0.5]）。'),
    'anomaly.detect.err-too-large': ('Matrice trop volumineuse (garde-fou IA).', 'Matrix too large (AI guardrail).', 'Matriz demasiado grande (barrera de IA).', 'المصفوفة كبيرة جدًا (حاجز أمان الذكاء الاصطناعي).', '行列が大きすぎます（AIガードレール）。', '矩阵过大（AI 防护）。'),
    'anomaly.detect.err-quota': ('Débit/quota IA dépassé pour ce tenant — réessayez plus tard.', 'AI rate/quota exceeded for this tenant — try again later.', 'Tasa/cuota de IA superada para este inquilino — inténtelo más tarde.', 'تم تجاوز معدّل/حصة الذكاء الاصطناعي لهذا المستأجر — أعد المحاولة لاحقًا.', 'このテナントのAIレート/クォータを超過しました — 後で再試行してください。', '该租户的 AI 速率/配额已超出 — 请稍后重试。'),
    'anomaly.detect.err-gateway': ('Passerelle IA indisponible (ai-service injoignable).', 'AI gateway unavailable (ai-service unreachable).', 'Pasarela de IA no disponible (ai-service inaccesible).', 'بوابة الذكاء الاصطناعي غير متاحة (ai-service غير قابل للوصول).', 'AIゲートウェイが利用できません（ai-service に到達不可）。', 'AI 网关不可用（无法连接 ai-service）。'),
    'anomaly.detect.err-unavailable': ('Service IA momentanément indisponible (disjoncteur ouvert).', 'AI service temporarily unavailable (circuit breaker open).', 'Servicio de IA temporalmente no disponible (disyuntor abierto).', 'خدمة الذكاء الاصطناعي غير متاحة مؤقتًا (قاطع الدائرة مفتوح).', 'AIサービスは一時的に利用できません（サーキットブレーカー作動中）。', 'AI 服务暂时不可用（断路器已打开）。'),
    'anomaly.detect.err-generic': ('Échec de la détection (HTTP {$status}).', 'Detection failed (HTTP {$status}).', 'Fallo de la detección (HTTP {$status}).', 'فشل الكشف (HTTP {$status}).', '検出に失敗しました（HTTP {$status}）。', '检测失败（HTTP {$status}）。'),
}
