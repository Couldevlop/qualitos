# -*- coding: utf-8 -*-
"""Table i18n - Exploration NLQ -> graphique (CLAUDE.md §7.3). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.nlq-explore': ('Exploration NLQ', 'NLQ explorer', 'Explorador NLQ', 'استكشاف NLQ', 'NLQ探索', 'NLQ 探索'),

    'nlq-explore.title': ('Exploration en langage naturel', 'Natural-language exploration', 'Exploración en lenguaje natural', 'الاستكشاف باللغة الطبيعية', '自然言語による探索', '自然语言探索'),
    'nlq-explore.subtitle': ("Posez une question sur vos données qualité — l'IA la traduit en requête sûre (text-to-SQL) et génère un graphique.", 'Ask a question about your quality data — the AI translates it into a safe query (text-to-SQL) and generates a chart.', 'Haz una pregunta sobre tus datos de calidad: la IA la traduce en una consulta segura (text-to-SQL) y genera un gráfico.', 'اطرح سؤالاً عن بيانات الجودة لديك — يحوّله الذكاء الاصطناعي إلى استعلام آمن (text-to-SQL) ويُنشئ رسمًا بيانيًا.', '品質データについて質問してください。AIが安全なクエリ（text-to-SQL）に変換し、グラフを生成します。', '就您的质量数据提问 — AI 会将其转换为安全查询（text-to-SQL）并生成图表。'),

    'nlq-explore.question-panel': ('Question', 'Question', 'Pregunta', 'سؤال', '質問', '问题'),
    'nlq-explore.question-label': ('Question en langage naturel', 'Natural-language question', 'Pregunta en lenguaje natural', 'سؤال بلغة طبيعية', '自然言語による質問', '自然语言提问'),
    'nlq-explore.question-placeholder': ('Combien de CAPA par statut ?', 'How many CAPAs by status?', '¿Cuántas CAPA por estado?', 'كم عدد إجراءات CAPA حسب الحالة؟', 'ステータス別のCAPA件数は？', '按状态统计有多少 CAPA？'),
    'nlq-explore.run': ('Interroger', 'Query', 'Consultar', 'استعلام', '問い合わせ', '查询'),
    'nlq-explore.running': ('Analyse…', 'Analyzing…', 'Analizando…', 'جارٍ التحليل…', '分析中…', '分析中…'),
    'nlq-explore.examples-label': ('Exemples :', 'Examples:', 'Ejemplos:', 'أمثلة:', '例:', '示例：'),

    'nlq-explore.example-1': ('Combien de CAPA par statut ?', 'How many CAPAs by status?', '¿Cuántas CAPA por estado?', 'كم عدد إجراءات CAPA حسب الحالة؟', 'ステータス別のCAPA件数は？', '按状态统计有多少 CAPA？'),
    'nlq-explore.example-2': ('Nombre de diagrammes Ishikawa par statut', 'Number of Ishikawa diagrams by status', 'Número de diagramas de Ishikawa por estado', 'عدد مخططات إيشيكاوا حسب الحالة', 'ステータス別の石川ダイアグラム数', '按状态统计石川图数量'),
    'nlq-explore.example-3': ('Combien de CAPA par criticité ?', 'How many CAPAs by criticality?', '¿Cuántas CAPA por criticidad?', 'كم عدد إجراءات CAPA حسب الأهمية؟', '重大度別のCAPA件数は？', '按严重程度统计有多少 CAPA？'),
    'nlq-explore.example-4': ('Nombre de fournisseurs par statut', 'Number of suppliers by status', 'Número de proveedores por estado', 'عدد الموردين حسب الحالة', 'ステータス別のサプライヤー数', '按状态统计供应商数量'),

    'nlq-explore.card-rows': ('Lignes', 'Rows', 'Filas', 'الصفوف', '行数', '行数'),
    'nlq-explore.card-confidence': ('Confiance', 'Confidence', 'Confianza', 'الثقة', '信頼度', '置信度'),
    'nlq-explore.tenant-filtered': ('Filtré par tenant', 'Tenant-filtered', 'Filtrado por inquilino', 'مُرشَّح حسب المستأجر', 'テナントでフィルタ済み', '已按租户过滤'),
    'nlq-explore.tenant-unfiltered': ('Tenant non filtré', 'Tenant not filtered', 'Inquilino no filtrado', 'غير مُرشَّح حسب المستأجر', 'テナント未フィルタ', '未按租户过滤'),

    'nlq-explore.chart-panel': ('Graphique', 'Chart', 'Gráfico', 'الرسم البياني', 'グラフ', '图表'),
    'nlq-explore.chart-type-label': ('Type :', 'Type:', 'Tipo:', 'النوع:', '種類:', '类型：'),
    'nlq-explore.chart-bar': ('Barres', 'Bars', 'Barras', 'أعمدة', '棒', '柱状'),
    'nlq-explore.chart-line': ('Lignes', 'Lines', 'Líneas', 'خطوط', '折れ線', '折线'),
    'nlq-explore.not-graphable': ("Ce résultat n'est pas représentable en graphique (aucune colonne numérique exploitable) — voir le tableau ci-dessous.", 'This result cannot be charted (no usable numeric column) — see the table below.', 'Este resultado no se puede graficar (ninguna columna numérica utilizable): vea la tabla a continuación.', 'لا يمكن تمثيل هذه النتيجة برسم بياني (لا يوجد عمود رقمي قابل للاستخدام) — انظر الجدول أدناه.', 'この結果はグラフ化できません（利用可能な数値列がありません）。下の表を参照してください。', '此结果无法生成图表（没有可用的数值列）——请参见下表。'),

    'nlq-explore.table-panel': ('Résultats', 'Results', 'Resultados', 'النتائج', '結果', '结果'),
    'nlq-explore.no-row': ('Aucune ligne ne correspond à cette question.', 'No rows match this question.', 'Ninguna fila coincide con esta pregunta.', 'لا توجد صفوف مطابقة لهذا السؤال.', 'この質問に一致する行はありません。', '没有与此问题匹配的行。'),

    'nlq-explore.sql-panel': ('Explicabilité', 'Explainability', 'Explicabilidad', 'قابلية التفسير', '説明可能性', '可解释性'),
    'nlq-explore.show-sql': ('Voir le SQL généré', 'Show generated SQL', 'Ver el SQL generado', 'عرض SQL المُولَّد', '生成されたSQLを表示', '查看生成的 SQL'),
    'nlq-explore.hide-sql': ('Masquer le SQL', 'Hide SQL', 'Ocultar el SQL', 'إخفاء SQL', 'SQLを非表示', '隐藏 SQL'),

    'nlq-explore.err-backend': ('Backend injoignable (engine sur 8082 ?).', 'Backend unreachable (engine on 8082?).', 'Backend inaccesible (¿motor en 8082?).', 'تعذّر الوصول إلى الخادم (المحرّك على 8082؟).', 'バックエンドに到達できません（エンジンは8082？）。', '后端不可达（引擎在 8082？）。'),
    'nlq-explore.err-untranslatable': ("La question n'a pas pu être traduite en requête sûre. Reformule-la plus simplement.", 'The question could not be translated into a safe query. Rephrase it more simply.', 'La pregunta no se pudo traducir en una consulta segura. Reformúlela de forma más sencilla.', 'تعذّر تحويل السؤال إلى استعلام آمن. أعد صياغته بشكل أبسط.', '質問を安全なクエリに変換できませんでした。より簡単に言い換えてください。', '无法将该问题转换为安全查询。请用更简单的方式重新表述。'),
    'nlq-explore.err-too-large': ('Question trop volumineuse (garde-fou IA).', 'Question too large (AI safeguard).', 'Pregunta demasiado extensa (protección de IA).', 'السؤال كبير جدًا (حماية الذكاء الاصطناعي).', '質問が大きすぎます（AIガード）。', '问题过大（AI 防护）。'),
    'nlq-explore.err-quota': ('Débit/quota IA dépassé pour ce tenant — réessayez plus tard.', 'AI rate/quota exceeded for this tenant — try again later.', 'Tasa/cuota de IA superada para este inquilino: inténtelo más tarde.', 'تم تجاوز معدّل/حصة الذكاء الاصطناعي لهذا المستأجر — أعد المحاولة لاحقًا.', 'このテナントのAIレート/クォータを超過しました。後で再試行してください。', '该租户的 AI 速率/配额已超出 — 请稍后重试。'),
    'nlq-explore.err-unavailable': ("L'assistant IA est momentanément indisponible (modèle en cours de chargement ?). Réessaie dans un instant.", 'The AI assistant is momentarily unavailable (model loading?). Try again shortly.', 'El asistente de IA no está disponible por el momento (¿cargando el modelo?). Inténtelo de nuevo en breve.', 'مساعد الذكاء الاصطناعي غير متاح مؤقتًا (هل النموذج قيد التحميل؟). أعد المحاولة بعد قليل.', 'AIアシスタントは一時的に利用できません（モデル読み込み中？）。しばらくして再試行してください。', 'AI 助手暂时不可用（模型加载中？）。请稍后重试。'),
    'nlq-explore.err-generic': ('Une erreur est survenue lors du traitement de la question (HTTP {$status}).', 'An error occurred while processing the question (HTTP {$status}).', 'Se produjo un error al procesar la pregunta (HTTP {$status}).', 'حدث خطأ أثناء معالجة السؤال (HTTP {$status}).', '質問の処理中にエラーが発生しました（HTTP {$status}）。', '处理问题时发生错误（HTTP {$status}）。'),
}
