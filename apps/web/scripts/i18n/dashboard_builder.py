# -*- coding: utf-8 -*-
"""Table i18n - domaine dashboard-builder (tableaux de bord drag & drop). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    # Éditeur
    'dbb.editor.back': ('Retour à la liste', 'Back to list', 'Volver a la lista', 'العودة إلى القائمة', '一覧に戻る', '返回列表'),
    'dbb.editor.name': ('Nom du tableau de bord', 'Dashboard name', 'Nombre del tablero', 'اسم لوحة المعلومات', 'ダッシュボード名', '仪表板名称'),
    'dbb.editor.newName': ('Nouveau tableau de bord', 'New dashboard', 'Nuevo tablero', 'لوحة معلومات جديدة', '新しいダッシュボード', '新建仪表板'),
    'dbb.editor.shared': ('Partagé', 'Shared', 'Compartido', 'مُشارَك', '共有', '已共享'),
    'dbb.editor.private': ('Privé', 'Private', 'Privado', 'خاص', 'プライベート', '私有'),
    'dbb.editor.save': ('Enregistrer', 'Save', 'Guardar', 'حفظ', '保存', '保存'),
    'dbb.editor.saving': ('Enregistrement…', 'Saving…', 'Guardando…', 'جارٍ الحفظ…', '保存中…', '正在保存…'),
    'dbb.editor.notFound': ('Tableau de bord introuvable', 'Dashboard not found', 'Tablero no encontrado', 'لم يُعثَر على لوحة المعلومات', 'ダッシュボードが見つかりません', '未找到仪表板'),
    'dbb.editor.added': ('Widget ajouté', 'Widget added', 'Widget añadido', 'تمت إضافة الأداة', 'ウィジェットを追加しました', '已添加小部件'),
    'dbb.editor.saved': ('Tableau de bord enregistré', 'Dashboard saved', 'Tablero guardado', 'تم حفظ لوحة المعلومات', 'ダッシュボードを保存しました', '仪表板已保存'),
    'dbb.editor.saveFailed': ("Échec de l'enregistrement", 'Save failed', 'Error al guardar', 'فشل الحفظ', '保存に失敗しました', '保存失败'),

    # Palette / grille
    'dbb.palette.aria': ('Palette de widgets', 'Widget palette', 'Paleta de widgets', 'لوحة الأدوات', 'ウィジェットパレット', '小部件面板'),
    'dbb.palette.label': ('Glisser un widget', 'Drag a widget', 'Arrastre un widget', 'اسحب أداة', 'ウィジェットをドラッグ', '拖动小部件'),
    'dbb.grid.aria': ('Grille du tableau de bord', 'Dashboard grid', 'Cuadrícula del tablero', 'شبكة لوحة المعلومات', 'ダッシュボードグリッド', '仪表板网格'),
    'dbb.item.move': ('Déplacer', 'Move', 'Mover', 'نقل', '移動', '移动'),
    'dbb.item.config': ('Configurer', 'Configure', 'Configurar', 'تكوين', '設定', '配置'),
    'dbb.item.remove': ('Supprimer', 'Remove', 'Quitar', 'إزالة', '削除', '移除'),
    'dbb.empty.hint': ("Votre tableau de bord est vide. Glissez un widget depuis la palette, ou cliquez dessus pour l'ajouter.", 'Your dashboard is empty. Drag a widget from the palette, or click it to add it.', 'Su tablero está vacío. Arrastre un widget desde la paleta o haga clic en él para añadirlo.', 'لوحة معلوماتك فارغة. اسحب أداة من اللوحة، أو انقر عليها لإضافتها.', 'ダッシュボードは空です。パレットからウィジェットをドラッグするか、クリックして追加してください。', '您的仪表板为空。从面板拖动小部件，或点击以添加。'),
    'dbb.narrative.empty': ("Récit IA : aucun élément pour l'instant — branchez une source pour générer un storyboard.", 'AI narrative: nothing yet — connect a source to generate a storyboard.', 'Relato IA: aún no hay nada — conecte una fuente para generar un storyboard.', 'سرد الذكاء الاصطناعي: لا شيء بعد — اربط مصدراً لإنشاء قصة مصوّرة.', 'AIナラティブ：まだ何もありません — ソースを接続してストーリーボードを生成してください。', 'AI 叙事：暂无内容 — 接入数据源以生成故事板。'),

    # Panneau de configuration du widget
    'dbb.config.aria.label': ('Panneau de configuration du widget', 'Widget configuration panel', 'Panel de configuración del widget', 'لوحة تكوين الأداة', 'ウィジェット設定パネル', '小部件配置面板'),
    'dbb.config.title': ('Configurer le widget', 'Configure widget', 'Configurar widget', 'تكوين الأداة', 'ウィジェットを設定', '配置小部件'),
    'dbb.config.close': ('Fermer le panneau', 'Close panel', 'Cerrar panel', 'إغلاق اللوحة', 'パネルを閉じる', '关闭面板'),
    'dbb.config.widgetTitle': ('Titre', 'Title', 'Título', 'العنوان', 'タイトル', '标题'),
    'dbb.config.source': ('Source KPI', 'KPI source', 'Fuente de KPI', 'مصدر مؤشر الأداء', 'KPI ソース', 'KPI 数据源'),
    'dbb.config.label': ('Libellé affiché', 'Displayed label', 'Etiqueta mostrada', 'التسمية المعروضة', '表示ラベル', '显示标签'),
    'dbb.config.unit': ('Unité', 'Unit', 'Unidad', 'الوحدة', '単位', '单位'),
    'dbb.config.threshold': ('Seuil', 'Threshold', 'Umbral', 'العتبة', 'しきい値', '阈值'),
    'dbb.config.text': ('Texte du récit', 'Narrative text', 'Texto del relato', 'نص السرد', 'ナラティブテキスト', '叙事文本'),
    'dbb.config.remove': ('Supprimer', 'Remove', 'Quitar', 'إزالة', '削除', '移除'),
    'dbb.config.done': ('Terminé', 'Done', 'Listo', 'تم', '完了', '完成'),

    # Catalogue de widgets
    'dbb.widget.kpi': ('Indicateur KPI', 'KPI indicator', 'Indicador KPI', 'مؤشر الأداء KPI', 'KPI 指標', 'KPI 指标'),
    'dbb.widget.kpi.desc': ('Valeur unique avec tendance et seuil', 'Single value with trend and threshold', 'Valor único con tendencia y umbral', 'قيمة واحدة مع الاتجاه والعتبة', 'トレンドとしきい値付きの単一値', '带趋势和阈值的单一数值'),
    'dbb.widget.line': ('Courbe', 'Line chart', 'Gráfico de líneas', 'منحنى', '折れ線グラフ', '折线图'),
    'dbb.widget.line.desc': ('Tendance temporelle (ECharts)', 'Time trend (ECharts)', 'Tendencia temporal (ECharts)', 'الاتجاه الزمني (ECharts)', '時系列トレンド（ECharts）', '时间趋势（ECharts）'),
    'dbb.widget.bar': ('Histogramme', 'Bar chart', 'Gráfico de barras', 'مخطط أعمدة', '棒グラフ', '柱状图'),
    'dbb.widget.bar.desc': ('Comparaison par catégorie', 'Comparison by category', 'Comparación por categoría', 'مقارنة حسب الفئة', 'カテゴリ別の比較', '按类别比较'),
    'dbb.widget.pie': ('Camembert', 'Pie chart', 'Gráfico circular', 'مخطط دائري', '円グラフ', '饼图'),
    'dbb.widget.pie.desc': ('Répartition proportionnelle', 'Proportional breakdown', 'Distribución proporcional', 'توزيع نسبي', '比率の内訳', '比例分布'),
    'dbb.widget.gauge': ('Jauge', 'Gauge', 'Indicador de aguja', 'مقياس', 'ゲージ', '仪表盘'),
    'dbb.widget.gauge.desc': ("Atteinte d'une cible", 'Target attainment', 'Cumplimiento de un objetivo', 'مدى تحقيق الهدف', '目標達成度', '目标达成度'),
    'dbb.widget.control': ('Carte de contrôle', 'Control chart', 'Gráfico de control', 'مخطط التحكم', '管理図', '控制图'),
    'dbb.widget.control.desc': ('SPC avec limites UCL/LCL', 'SPC with UCL/LCL limits', 'SPC con límites UCL/LCL', 'التحكم الإحصائي مع حدود UCL/LCL', 'UCL/LCL 限界付き SPC', '带 UCL/LCL 控制限的 SPC'),
    'dbb.widget.table': ('Tableau', 'Table', 'Tabla', 'جدول', 'テーブル', '表格'),
    'dbb.widget.table.desc': ('Liste dense de valeurs', 'Dense list of values', 'Lista densa de valores', 'قائمة كثيفة من القيم', '密な値のリスト', '密集数值列表'),
    'dbb.widget.heatmap': ('Heatmap', 'Heatmap', 'Mapa de calor', 'خريطة حرارية', 'ヒートマップ', '热力图'),
    'dbb.widget.heatmap.desc': ('Matrice de conformité', 'Compliance matrix', 'Matriz de cumplimiento', 'مصفوفة الامتثال', 'コンプライアンスマトリクス', '合规矩阵'),
    'dbb.widget.narrative': ('Récit IA', 'AI narrative', 'Relato IA', 'سرد بالذكاء الاصطناعي', 'AI ナラティブ', 'AI 叙事'),
    'dbb.widget.narrative.desc': ('Texte / storyboard généré', 'Generated text / storyboard', 'Texto / storyboard generado', 'نص / قصة مصوّرة مُولّدة', '生成されたテキスト／ストーリーボード', '生成的文本/故事板'),

    # Catalogue de KPI
    'dbb.kpi.capa': ('Délai moyen de clôture CAPA', 'Average CAPA closure time', 'Tiempo medio de cierre de CAPA', 'متوسط زمن إغلاق الإجراء التصحيحي/الوقائي', 'CAPA 平均クローズ時間', 'CAPA 平均关闭时长'),
    'dbb.kpi.recurrence': ('Taux de récidive CAPA', 'CAPA recurrence rate', 'Tasa de recurrencia de CAPA', 'معدل تكرار الإجراء التصحيحي/الوقائي', 'CAPA 再発率', 'CAPA 复发率'),
    'dbb.kpi.ncrate': ('Taux de non-conformités', 'Nonconformity rate', 'Tasa de no conformidades', 'معدل عدم المطابقة', '不適合率', '不合格率'),
    'dbb.kpi.dpmo': ('DPMO', 'DPMO', 'DPMO', 'DPMO', 'DPMO', 'DPMO'),
    'dbb.kpi.fpy': ('First Pass Yield', 'First Pass Yield', 'First Pass Yield', 'النجاح من المحاولة الأولى (FPY)', '直行率（FPY）', '一次通过率（FPY）'),
    'dbb.kpi.coq': ("Coût d'obtention de la qualité", 'Cost of quality', 'Costo de la calidad', 'تكلفة الجودة', '品質コスト', '质量成本'),
    'dbb.kpi.pdca': ('Avancement cycles PDCA', 'PDCA cycle progress', 'Avance de los ciclos PDCA', 'تقدّم دورات PDCA', 'PDCA サイクル進捗', 'PDCA 循环进度'),
    'dbb.kpi.fives': ('Score 5S moyen', 'Average 5S score', 'Puntuación 5S media', 'متوسط نتيجة 5S', '平均5Sスコア', '平均5S得分'),
    'dbb.kpi.cpk': ('Capabilité Cpk', 'Cpk capability', 'Capacidad Cpk', 'قدرة العملية Cpk', '工程能力 Cpk', '过程能力 Cpk'),
    'dbb.kpi.alignment': ("Taux d'alignement normatif", 'Standards alignment rate', 'Tasa de alineación normativa', 'معدل المواءمة المعيارية', '規格適合率', '标准对齐率'),
    'dbb.kpi.audit': ('Audits réalisés vs planifiés', 'Audits completed vs planned', 'Auditorías realizadas vs planificadas', 'التدقيقات المُنجَزة مقابل المُخطَّطة', '実施監査数 vs 計画', '已完成审核 vs 计划'),
    'dbb.kpi.supplier': ('Score qualité fournisseurs', 'Supplier quality score', 'Puntuación de calidad de proveedores', 'نتيجة جودة المورّدين', 'サプライヤー品質スコア', '供应商质量评分'),
}
