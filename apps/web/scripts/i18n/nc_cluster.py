# -*- coding: utf-8 -*-
"""Table i18n - clustering de non-conformités (TF-IDF + DBSCAN, §4.3/§12.1). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.nc-clusters': ('Clustering NC', 'NC clustering', 'Agrupación de NC', 'تجميع حالات عدم المطابقة', 'NCクラスタリング', '不符合项聚类'),

    'nccluster.title': ('Clustering de non-conformités', 'Non-conformity clustering', 'Agrupación de no conformidades', 'تجميع حالات عدم المطابقة', '不適合のクラスタリング', '不符合项聚类'),
    'nccluster.subtitle': ("Regroupement par densité (TF-IDF + DBSCAN) pour révéler les patterns récurrents de NC — calculé par l'IA.", 'Density-based grouping (TF-IDF + DBSCAN) to reveal recurrent NC patterns — computed by AI.', 'Agrupación por densidad (TF-IDF + DBSCAN) para revelar patrones recurrentes de NC — calculada por IA.', 'تجميع قائم على الكثافة (TF-IDF + DBSCAN) للكشف عن الأنماط المتكررة لحالات عدم المطابقة — محسوب بالذكاء الاصطناعي.', '密度ベースのグループ化（TF-IDF + DBSCAN）で不適合の繰り返しパターンをAIが抽出。', '基于密度的分组（TF-IDF + DBSCAN）以揭示不符合项的重复模式 — 由AI计算。'),

    'nccluster.input-panel': ('Non-conformités', 'Non-conformities', 'No conformidades', 'حالات عدم المطابقة', '不適合', '不符合项'),
    'nccluster.texts-label': ('Une non-conformité par ligne', 'One non-conformity per line', 'Una no conformidad por línea', 'حالة عدم مطابقة واحدة لكل سطر', '1行に1件の不適合', '每行一个不符合项'),
    'nccluster.threshold-label': ('Similarité minimale', 'Minimum similarity', 'Similitud mínima', 'الحد الأدنى للتشابه', '最小類似度', '最小相似度'),
    'nccluster.threshold-hint': ('0–1 : plus haut = clusters plus stricts', '0–1: higher = stricter clusters', '0–1: más alto = clústeres más estrictos', '0–1: أعلى = مجموعات أكثر صرامة', '0–1：高いほど厳格なクラスタ', '0–1：越高聚类越严格'),
    'nccluster.min-samples-label': ('Densité min (min_samples)', 'Min density (min_samples)', 'Densidad mín. (min_samples)', 'الكثافة الدنيا (min_samples)', '最小密度 (min_samples)', '最小密度 (min_samples)'),
    'nccluster.min-samples-hint': ("Taille mini du voisinage d'un point-cœur", 'Minimum neighborhood size of a core point', 'Tamaño mínimo del vecindario de un punto central', 'الحجم الأدنى لجوار نقطة جوهرية', 'コア点の最小近傍サイズ', '核心点的最小邻域大小'),
    'nccluster.run': ('Regrouper', 'Cluster', 'Agrupar', 'تجميع', 'グループ化', '聚类'),
    'nccluster.running': ('Regroupement…', 'Clustering…', 'Agrupando…', 'جارٍ التجميع…', 'グループ化中…', '聚类中…'),
    'nccluster.example': ('Exemple', 'Example', 'Ejemplo', 'مثال', '例', '示例'),

    'nccluster.card-clusters': ('Patterns détectés', 'Patterns detected', 'Patrones detectados', 'الأنماط المكتشفة', '検出パターン', '检测到的模式'),
    'nccluster.card-ratio': ('Taux regroupé', 'Clustered ratio', 'Tasa agrupada', 'نسبة المجموعة', 'クラスタ化率', '聚类比例'),

    'nccluster.clusters-panel': ('Clusters', 'Clusters', 'Clústeres', 'المجموعات', 'クラスタ', '聚类'),
    'nccluster.no-cluster': ('Aucun pattern récurrent : chaque NC est singulière au seuil retenu.', 'No recurrent pattern: each NC is singular at the chosen threshold.', 'Ningún patrón recurrente: cada NC es singular en el umbral elegido.', 'لا نمط متكرر: كل حالة عدم مطابقة فريدة عند العتبة المختارة.', '繰り返しパターンなし：選択したしきい値では各不適合は固有です。', '无重复模式：在所选阈值下每个不符合项都是独立的。'),
    'nccluster.nc-count': ('NC', 'NCs', 'NC', 'حالات', '件', '项'),
    'nccluster.noise-panel': ('NC isolées (bruit)', 'Isolated NCs (noise)', 'NC aisladas (ruido)', 'حالات معزولة (ضوضاء)', '孤立した不適合（ノイズ）', '孤立不符合项（噪声）'),

    'nccluster.err-min-texts': ('Saisissez au moins 2 non-conformités (une par ligne).', 'Enter at least 2 non-conformities (one per line).', 'Introduzca al menos 2 no conformidades (una por línea).', 'أدخل حالتي عدم مطابقة على الأقل (واحدة لكل سطر).', '少なくとも2件の不適合を入力してください（1行に1件）。', '请至少输入 2 个不符合项（每行一个）。'),
    'nccluster.err-backend': ('Backend injoignable (engine sur 8082 ?).', 'Backend unreachable (engine on 8082?).', 'Backend inaccesible (¿engine en 8082?).', 'تعذّر الوصول إلى الخادم (engine على 8082؟).', 'バックエンドに到達できません（engine は 8082？）。', '无法连接后端（engine 在 8082？）。'),
    'nccluster.err-invalid': ('Requête invalide (2 à 2000 NC attendues).', 'Invalid request (2 to 2000 NCs expected).', 'Solicitud no válida (se esperan de 2 a 2000 NC).', 'طلب غير صالح (من 2 إلى 2000 حالة متوقعة).', '無効なリクエスト（2〜2000件の不適合）。', '请求无效（应为 2 到 2000 个不符合项）。'),
    'nccluster.err-too-large': ('Liste trop volumineuse (garde-fou IA).', 'List too large (AI guardrail).', 'Lista demasiado grande (barrera de IA).', 'القائمة كبيرة جدًا (حاجز أمان الذكاء الاصطناعي).', 'リストが大きすぎます（AIガードレール）。', '列表过大（AI 防护）。'),
    'nccluster.err-quota': ('Débit/quota IA dépassé pour ce tenant — réessayez plus tard.', 'AI rate/quota exceeded for this tenant — try again later.', 'Tasa/cuota de IA superada para este inquilino — inténtelo más tarde.', 'تم تجاوز معدّل/حصة الذكاء الاصطناعي لهذا المستأجر — أعد المحاولة لاحقًا.', 'このテナントのAIレート/クォータを超過しました — 後で再試行してください。', '该租户的 AI 速率/配额已超出 — 请稍后重试。'),
    'nccluster.err-gateway': ('Passerelle IA indisponible (ai-service injoignable).', 'AI gateway unavailable (ai-service unreachable).', 'Pasarela de IA no disponible (ai-service inaccesible).', 'بوابة الذكاء الاصطناعي غير متاحة (ai-service غير قابل للوصول).', 'AIゲートウェイが利用できません（ai-service に到達不可）。', 'AI 网关不可用（无法连接 ai-service）。'),
    'nccluster.err-unavailable': ('Service IA momentanément indisponible (disjoncteur ouvert).', 'AI service temporarily unavailable (circuit breaker open).', 'Servicio de IA temporalmente no disponible (disyuntor abierto).', 'خدمة الذكاء الاصطناعي غير متاحة مؤقتًا (قاطع الدائرة مفتوح).', 'AIサービスは一時的に利用できません（サーキットブレーカー作動中）。', 'AI 服务暂时不可用（断路器已打开）。'),
    'nccluster.err-generic': ('Échec du clustering (HTTP {$status}).', 'Clustering failed (HTTP {$status}).', 'Fallo de la agrupación (HTTP {$status}).', 'فشل التجميع (HTTP {$status}).', 'クラスタリングに失敗しました（HTTP {$status}）。', '聚类失败（HTTP {$status}）。'),
}
