# -*- coding: utf-8 -*-
"""Table i18n - Mode TV / Salle qualité (§7.3). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.tv-mode': ('Mode TV / Salle qualité', 'TV mode / Quality room', 'Modo TV / Sala de calidad', 'وضع التلفزيون / قاعة الجودة', 'TVモード／品質ルーム', '电视模式 / 质量室'),

    'tv.brand': ('QualitOS · Salle qualité', 'QualitOS · Quality room', 'QualitOS · Sala de calidad', 'QualitOS · قاعة الجودة', 'QualitOS · 品質ルーム', 'QualitOS · 质量室'),

    'tv.toolbar.aria': ('Commandes Mode TV', 'TV mode controls', 'Controles del modo TV', 'عناصر تحكم وضع التلفزيون', 'TVモードのコントロール', '电视模式控制'),
    'tv.interval.label': ('Intervalle', 'Interval', 'Intervalo', 'الفاصل', '間隔', '间隔'),
    'tv.interval.aria': ('Intervalle de rotation', 'Rotation interval', 'Intervalo de rotación', 'فاصل التدوير', 'ローテーション間隔', '轮播间隔'),
    'tv.progress.aria': ('Avancement de la slide', 'Slide progress', 'Avance de la diapositiva', 'تقدّم الشريحة', 'スライドの進行状況', '幻灯片进度'),

    'tv.a11y.play': ('Reprendre la rotation', 'Resume rotation', 'Reanudar la rotación', 'استئناف التدوير', 'ローテーションを再開', '恢复轮播'),
    'tv.a11y.pause': ('Mettre en pause', 'Pause', 'Pausar', 'إيقاف مؤقت', '一時停止', '暂停'),
    'tv.a11y.prev': ('Slide précédente', 'Previous slide', 'Diapositiva anterior', 'الشريحة السابقة', '前のスライド', '上一张幻灯片'),
    'tv.a11y.next': ('Slide suivante', 'Next slide', 'Diapositiva siguiente', 'الشريحة التالية', '次のスライド', '下一张幻灯片'),
    'tv.a11y.fullscreen': ('Basculer en plein écran', 'Toggle fullscreen', 'Alternar pantalla completa', 'تبديل ملء الشاشة', '全画面を切り替え', '切换全屏'),

    'tv.loading': ('Chargement des indicateurs…', 'Loading indicators…', 'Cargando indicadores…', 'جارٍ تحميل المؤشرات…', '指標を読み込み中…', '正在加载指标…'),
    'tv.error': ('Impossible de charger les indicateurs. Réessayez.', 'Unable to load indicators. Try again.', 'No se pudieron cargar los indicadores. Inténtelo de nuevo.', 'تعذّر تحميل المؤشرات. أعد المحاولة.', '指標を読み込めません。再試行してください。', '无法加载指标。请重试。'),
    'tv.retry': ('Réessayer', 'Retry', 'Reintentar', 'إعادة المحاولة', '再試行', '重试'),

    'tv.slide.kpis.title': ('Indicateurs stratégiques', 'Strategic indicators', 'Indicadores estratégicos', 'المؤشرات الاستراتيجية', '戦略指標', '战略指标'),
    'tv.slide.kpis.subtitle': ('Performance qualité — cible vs réalisé', 'Quality performance — target vs actual', 'Rendimiento de calidad — objetivo vs real', 'أداء الجودة — الهدف مقابل المحقّق', '品質パフォーマンス — 目標と実績', '质量绩效 — 目标与实际'),
    'tv.slide.risks.title': ('Risques critiques', 'Critical risks', 'Riesgos críticos', 'المخاطر الحرجة', '重大リスク', '关键风险'),
    'tv.slide.risks.subtitle': ('Actions prioritaires à traiter', 'Priority actions to address', 'Acciones prioritarias a tratar', 'إجراءات ذات أولوية للمعالجة', '優先的に対応すべきアクション', '需优先处理的行动'),
    'tv.slide.predictions.title': ('Prévisions IA', 'AI forecasts', 'Previsiones de IA', 'تنبؤات الذكاء الاصطناعي', 'AI予測', 'AI 预测'),
    'tv.slide.predictions.subtitle': ('Anticipations explicables sur les prochaines semaines', 'Explainable anticipations for the coming weeks', 'Anticipaciones explicables para las próximas semanas', 'توقعات قابلة للتفسير للأسابيع المقبلة', '今後数週間の説明可能な予測', '对未来数周的可解释预测'),
    'tv.slide.trend.title': ('Tendance qualité — 12 mois', 'Quality trend — 12 months', 'Tendencia de calidad — 12 meses', 'اتجاه الجودة — 12 شهرًا', '品質トレンド — 12か月', '质量趋势 — 12 个月'),
    'tv.slide.trend.subtitle': ('Évolution de la performance globale', 'Evolution of overall performance', 'Evolución del rendimiento global', 'تطوّر الأداء الإجمالي', '全体パフォーマンスの推移', '整体绩效的演变'),
    'tv.slide.empty.title': ('Aucune donnée à afficher', 'No data to display', 'No hay datos para mostrar', 'لا توجد بيانات للعرض', '表示するデータがありません', '没有可显示的数据'),
    'tv.slide.empty.subtitle': ("Les indicateurs apparaîtront ici dès qu'ils seront disponibles.", 'Indicators will appear here as soon as they are available.', 'Los indicadores aparecerán aquí en cuanto estén disponibles.', 'ستظهر المؤشرات هنا بمجرد توفّرها.', '指標は利用可能になり次第ここに表示されます。', '指标可用后将显示在此处。'),

    'tv.trend.series': ('Qualité globale', 'Overall quality', 'Calidad global', 'الجودة الإجمالية', '全体品質', '整体质量'),
    'tv.trend.target': ('Cible', 'Target', 'Objetivo', 'الهدف', '目標', '目标'),

    'tv.card.confidence': ('Confiance {$INTERPOLATION} %', 'Confidence {$INTERPOLATION} %', 'Confianza {$INTERPOLATION} %', 'الثقة {$INTERPOLATION} %', '信頼度 {$INTERPOLATION} %', '置信度 {$INTERPOLATION} %'),
}
