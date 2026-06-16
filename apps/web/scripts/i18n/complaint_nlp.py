# -*- coding: utf-8 -*-
"""Table i18n - analyse NLP des réclamations (§4.9/§12.1). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.complaints-nlp': ('Réclamations IA', 'AI complaints', 'Reclamaciones IA', 'الشكاوى بالذكاء الاصطناعي', 'AIクレーム', 'AI投诉'),

    'complaintnlp.title': ('Analyse des réclamations (NLP)', 'Complaint analysis (NLP)', 'Análisis de reclamaciones (NLP)', 'تحليل الشكاوى (NLP)', 'クレーム分析（NLP）', '投诉分析（NLP）'),
    'complaintnlp.subtitle': ("Sentiment, catégorie et criticité de chaque réclamation — détection des cas critiques par l'IA.", 'Sentiment, category and criticality of each complaint — critical cases flagged by AI.', 'Sentimiento, categoría y criticidad de cada reclamación — casos críticos detectados por IA.', 'المشاعر والفئة والأهمية لكل شكوى — يكشف الذكاء الاصطناعي الحالات الحرجة.', '各クレームの感情・カテゴリ・重大度 — 重要案件をAIが検出。', '每条投诉的情感、类别和严重性 — 由AI标记关键案例。'),

    'complaintnlp.input-panel': ('Réclamations', 'Complaints', 'Reclamaciones', 'الشكاوى', 'クレーム', '投诉'),
    'complaintnlp.texts-label': ('Une réclamation par ligne', 'One complaint per line', 'Una reclamación por línea', 'شكوى واحدة لكل سطر', '1行に1件のクレーム', '每行一条投诉'),
    'complaintnlp.run': ('Analyser', 'Analyze', 'Analizar', 'تحليل', '分析', '分析'),
    'complaintnlp.running': ('Analyse…', 'Analyzing…', 'Analizando…', 'جارٍ التحليل…', '分析中…', '分析中…'),
    'complaintnlp.example': ('Exemple', 'Example', 'Ejemplo', 'مثال', '例', '示例'),

    'complaintnlp.card-critical': ('Réclamations critiques', 'Critical complaints', 'Reclamaciones críticas', 'الشكاوى الحرجة', '重大クレーム', '关键投诉'),
    'complaintnlp.card-critical-desc': ('Sécurité, juridique ou sentiment très négatif', 'Safety, legal or very negative sentiment', 'Seguridad, legal o sentimiento muy negativo', 'السلامة أو القانون أو مشاعر سلبية جدًا', '安全・法務または非常に否定的な感情', '安全、法律或非常负面的情感'),

    'complaintnlp.results-panel': ('Analyse par réclamation', 'Per-complaint analysis', 'Análisis por reclamación', 'تحليل لكل شكوى', 'クレームごとの分析', '逐条投诉分析'),
    'complaintnlp.col-complaint': ('Réclamation', 'Complaint', 'Reclamación', 'الشكوى', 'クレーム', '投诉'),
    'complaintnlp.col-sentiment': ('Sentiment', 'Sentiment', 'Sentimiento', 'المشاعر', '感情', '情感'),
    'complaintnlp.col-category': ('Catégorie', 'Category', 'Categoría', 'الفئة', 'カテゴリ', '类别'),
    'complaintnlp.col-critical': ('Critique', 'Critical', 'Crítica', 'حرجة', '重大', '关键'),
    'complaintnlp.critical-aria': ('Critique', 'Critical', 'Crítica', 'حرجة', '重大', '关键'),

    'complaintnlp.sent-negative': ('Négatif', 'Negative', 'Negativo', 'سلبي', '否定的', '负面'),
    'complaintnlp.sent-positive': ('Positif', 'Positive', 'Positivo', 'إيجابي', '肯定的', '正面'),
    'complaintnlp.sent-neutral': ('Neutre', 'Neutral', 'Neutral', 'محايد', '中立', '中性'),

    'complaintnlp.err-min': ('Saisissez au moins une réclamation (une par ligne).', 'Enter at least one complaint (one per line).', 'Introduzca al menos una reclamación (una por línea).', 'أدخل شكوى واحدة على الأقل (واحدة لكل سطر).', '少なくとも1件のクレームを入力してください（1行に1件）。', '请至少输入一条投诉（每行一条）。'),
    'complaintnlp.err-backend': ('Backend injoignable (engine sur 8082 ?).', 'Backend unreachable (engine on 8082?).', 'Backend inaccesible (¿engine en 8082?).', 'تعذّر الوصول إلى الخادم (engine على 8082؟).', 'バックエンドに到達できません（engine は 8082？）。', '无法连接后端（engine 在 8082？）。'),
    'complaintnlp.err-invalid': ('Requête invalide (1 à 2000 réclamations).', 'Invalid request (1 to 2000 complaints).', 'Solicitud no válida (1 a 2000 reclamaciones).', 'طلب غير صالح (من 1 إلى 2000 شكوى).', '無効なリクエスト（1〜2000件）。', '请求无效（1 到 2000 条投诉）。'),
    'complaintnlp.err-too-large': ('Lot trop volumineux (garde-fou IA).', 'Batch too large (AI guardrail).', 'Lote demasiado grande (barrera de IA).', 'الدفعة كبيرة جدًا (حاجز أمان الذكاء الاصطناعي).', 'バッチが大きすぎます（AIガードレール）。', '批量过大（AI 防护）。'),
    'complaintnlp.err-quota': ('Débit/quota IA dépassé pour ce tenant — réessayez plus tard.', 'AI rate/quota exceeded for this tenant — try again later.', 'Tasa/cuota de IA superada para este inquilino — inténtelo más tarde.', 'تم تجاوز معدّل/حصة الذكاء الاصطناعي لهذا المستأجر — أعد المحاولة لاحقًا.', 'このテナントのAIレート/クォータを超過しました — 後で再試行してください。', '该租户的 AI 速率/配额已超出 — 请稍后重试。'),
    'complaintnlp.err-gateway': ('Passerelle IA indisponible (ai-service injoignable).', 'AI gateway unavailable (ai-service unreachable).', 'Pasarela de IA no disponible (ai-service inaccesible).', 'بوابة الذكاء الاصطناعي غير متاحة (ai-service غير قابل للوصول).', 'AIゲートウェイが利用できません（ai-service に到達不可）。', 'AI 网关不可用（无法连接 ai-service）。'),
    'complaintnlp.err-unavailable': ('Service IA momentanément indisponible (disjoncteur ouvert).', 'AI service temporarily unavailable (circuit breaker open).', 'Servicio de IA temporalmente no disponible (disyuntor abierto).', 'خدمة الذكاء الاصطناعي غير متاحة مؤقتًا (قاطع الدائرة مفتوح).', 'AIサービスは一時的に利用できません（サーキットブレーカー作動中）。', 'AI 服务暂时不可用（断路器已打开）。'),
    'complaintnlp.err-generic': ("Échec de l'analyse (HTTP {$status}).", 'Analysis failed (HTTP {$status}).', 'Fallo del análisis (HTTP {$status}).', 'فشل التحليل (HTTP {$status}).', '分析に失敗しました（HTTP {$status}）。', '分析失败（HTTP {$status}）。'),
}
