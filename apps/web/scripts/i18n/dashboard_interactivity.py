# -*- coding: utf-8 -*-
"""Table i18n — interactivité dashboards (§7.3) : cross-filter, drill-down,
annotations collaboratives, time-travel. id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    # --- Time-travel ---
    'dashboard.tt.aria': ('Voyage dans le temps', "Time travel", "Viaje en el tiempo", "السفر عبر الزمن", "タイムトラベル", "时间回溯"),
    'dashboard.tt.label': ('État au', "State as of", "Estado al", "الحالة بتاريخ", "時点の状態", "截至状态"),
    'dashboard.tt.clear': ('Présent', "Now", "Presente", "الآن", "現在", "当前"),
    'dashboard.tt.loading': ("Récupération de l'état historique…", "Loading historical state…", "Recuperando el estado histórico…", "جارٍ استرجاع الحالة التاريخية…", "過去の状態を取得中…", "正在加载历史状态…"),
    'dashboard.tt.viewing': ('État du tableau de bord au', "Dashboard state as of", "Estado del panel al", "حالة لوحة المعلومات بتاريخ", "ダッシュボードの状態（時点）", "仪表盘状态（截至）"),
    'dashboard.tt.empty': ('Aucune mesure de KPI disponible à cette date.', "No KPI measurement available at this date.", "No hay mediciones de KPI disponibles en esta fecha.", "لا توجد قياسات لمؤشرات الأداء في هذا التاريخ.", "この日付に利用可能なKPI測定値はありません。", "该日期没有可用的 KPI 测量值。"),
    'dashboard.tt.na': ('n/d', "n/a", "n/d", "غ/م", "該当なし", "无数据"),
    'dashboard.tt.error': ("Impossible de récupérer l'état à cette date.", "Could not retrieve the state at this date.", "No se pudo recuperar el estado en esta fecha.", "تعذّر استرجاع الحالة في هذا التاريخ.", "この日付の状態を取得できませんでした。", "无法获取该日期的状态。"),

    # --- Cross-filter ---
    'dashboard.xf.active': ('Filtre croisé actif :', "Active cross-filter:", "Filtro cruzado activo:", "مرشّح متقاطع نشط:", "クロスフィルター有効:", "交叉筛选已启用："),
    'dashboard.xf.clear': ('Effacer', "Clear", "Borrar", "مسح", "クリア", "清除"),
    'dashboard.xf.clear-aria': ('Annuler le filtre', "Clear filter", "Cancelar el filtro", "إلغاء المرشّح", "フィルターを解除", "取消筛选"),
    'dashboard.xf.pill': ('Cross-filter', "Cross-filter", "Filtro cruzado", "مرشّح متقاطع", "クロスフィルター", "交叉筛选"),

    # --- Pareto subtitle (interactif) ---
    'dashboard.exec.pareto-subtitle-2': ('Cliquer une catégorie pour filtrer et explorer', "Click a category to filter and explore", "Haga clic en una categoría para filtrar y explorar", "انقر على فئة للتصفية والاستكشاف", "カテゴリをクリックして絞り込み・分析", "点击类别以筛选和下钻"),

    # --- Drill-down ---
    'dashboard.drill.back': ('Toutes les catégories', "All categories", "Todas las categorías", "كل الفئات", "すべてのカテゴリ", "所有类别"),
    'dashboard.drill.empty': ('Pas de détail disponible pour cette catégorie.', "No detail available for this category.", "No hay detalle disponible para esta categoría.", "لا توجد تفاصيل متاحة لهذه الفئة.", "このカテゴリの詳細はありません。", "该类别暂无明细。"),

    # --- Annotations ---
    'dashboard.annot.section': ('Annotations', "Annotations", "Anotaciones", "التعليقات التوضيحية", "注釈", "批注"),
    'dashboard.annot.placeholder': ('Ajouter une annotation…', "Add an annotation…", "Añadir una anotación…", "إضافة تعليق…", "注釈を追加…", "添加批注…"),
    'dashboard.annot.input-aria': ('Nouvelle annotation', "New annotation", "Nueva anotación", "تعليق جديد", "新しい注釈", "新建批注"),
    'dashboard.annot.add': ('Annoter', "Annotate", "Anotar", "تعليق", "注釈する", "批注"),
    'dashboard.annot.empty': ('Aucune annotation pour ce graphique.', "No annotation on this chart.", "Sin anotaciones en este gráfico.", "لا توجد تعليقات على هذا الرسم البياني.", "このグラフに注釈はありません。", "此图表暂无批注。"),
    'dashboard.annot.delete-aria': ("Supprimer l'annotation", "Delete annotation", "Eliminar la anotación", "حذف التعليق", "注釈を削除", "删除批注"),
    'dashboard.annot.load-error': ('Impossible de charger les annotations.', "Could not load annotations.", "No se pudieron cargar las anotaciones.", "تعذّر تحميل التعليقات.", "注釈を読み込めませんでした。", "无法加载批注。"),
    'dashboard.annot.save-error': ("Échec de l'enregistrement.", "Failed to save.", "Error al guardar.", "فشل الحفظ.", "保存に失敗しました。", "保存失败。"),
    'dashboard.annot.delete-error': ('Suppression refusée.', "Deletion refused.", "Eliminación rechazada.", "تم رفض الحذف.", "削除が拒否されました。", "删除被拒绝。"),
}
