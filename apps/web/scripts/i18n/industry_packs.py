# -*- coding: utf-8 -*-
"""Table i18n - feature Industry Packs (packs sectoriels). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    # --- liste ---------------------------------------------------------------
    'industry-packs.list.title': ('Packs sectoriels', 'Industry packs', 'Packs sectoriales', 'حزم القطاعات', '業種パック', '行业包'),
    'industry-packs.list.subtitle': (
        'Activez un pack métier pour pré-charger KPIs, normes, templates Ishikawa et bibliothèque Poka-Yoke adaptés à votre secteur.',
        'Activate an industry pack to preload KPIs, standards, Ishikawa templates and a Poka-Yoke library tailored to your sector.',
        'Active un pack sectorial para precargar KPIs, normas, plantillas Ishikawa y una biblioteca Poka-Yoke adaptados a su sector.',
        'فعّل حزمة قطاعية لتحميل مؤشرات الأداء والمعايير وقوالب إيشيكاوا ومكتبة بوكا-يوكي المناسبة لقطاعك مسبقًا.',
        '業界パックを有効化して、貴社の業種に合わせたKPI・規格・石川テンプレート・ポカヨケライブラリを事前読み込みします。',
        '激活行业包，以预加载适合您所在行业的 KPI、标准、石川图模板和防错（Poka-Yoke）库。'),
    'industry-packs.list.search': ('Rechercher un pack', 'Search a pack', 'Buscar un pack', 'البحث عن حزمة', 'パックを検索', '搜索行业包'),
    'industry-packs.list.search-placeholder': ('Secteur, code, mot-clé…', 'Sector, code, keyword…', 'Sector, código, palabra clave…', 'القطاع، الرمز، كلمة مفتاحية…', '業種、コード、キーワード…', '行业、代码、关键词……'),
    'industry-packs.list.empty': ('Aucun pack ne correspond à votre recherche.', 'No pack matches your search.', 'Ningún pack coincide con su búsqueda.', 'لا توجد حزمة تطابق بحثك.', '検索条件に一致するパックはありません。', '没有符合搜索条件的行业包。'),

    # --- état / badges -------------------------------------------------------
    'industry-packs.badge-active': ('Actif', 'Active', 'Activo', 'مُفعّل', '有効', '已激活'),
    'industry-packs.badge-inactive': ('Inactif', 'Inactive', 'Inactivo', 'غير مُفعّل', '無効', '未激活'),
    'industry-packs.version': ('Version', 'Version', 'Versión', 'الإصدار', 'バージョン', '版本'),
    'industry-packs.retry': ('Réessayer', 'Retry', 'Reintentar', 'إعادة المحاولة', '再試行', '重试'),
    'industry-packs.flat-note': (
        'Pack de base — contenu structuré (KPIs détaillés, Ishikawa, Poka-Yoke) à venir.',
        'Basic pack — structured content (detailed KPIs, Ishikawa, Poka-Yoke) coming soon.',
        'Pack básico — contenido estructurado (KPIs detallados, Ishikawa, Poka-Yoke) próximamente.',
        'حزمة أساسية — المحتوى المنظَّم (مؤشرات أداء مفصّلة، إيشيكاوا، بوكا-يوكي) قريبًا.',
        '基本パック — 構造化コンテンツ（詳細KPI、石川図、ポカヨケ）は近日提供予定。',
        '基础行业包——结构化内容（详细 KPI、石川图、防错）即将推出。'),

    # --- activation ----------------------------------------------------------
    'industry-packs.activate': ('Activer', 'Activate', 'Activar', 'تفعيل', '有効化', '激活'),
    'industry-packs.deactivate': ('Désactiver', 'Deactivate', 'Desactivar', 'إلغاء التفعيل', '無効化', '停用'),
    'industry-packs.activate-success': ('Pack activé', 'Pack activated', 'Pack activado', 'تم تفعيل الحزمة', 'パックを有効化しました', '行业包已激活'),
    'industry-packs.activate-success-counts': (
        'Pack activé — {$created} KPIs provisionnés, {$skipped} ignorés',
        'Pack activated — {$created} KPIs provisioned, {$skipped} skipped',
        'Pack activado — {$created} KPIs aprovisionados, {$skipped} omitidos',
        'تم تفعيل الحزمة — تم تهيئة {$created} مؤشرات أداء، وتم تجاهل {$skipped}',
        'パックを有効化しました — {$created} 件のKPIをプロビジョニング、{$skipped} 件をスキップ',
        '行业包已激活 — 已配置 {$created} 个 KPI，跳过 {$skipped} 个'),
    'industry-packs.activate-error': ("Échec de l'activation du pack", 'Failed to activate the pack', 'Error al activar el pack', 'فشل تفعيل الحزمة', 'パックの有効化に失敗しました', '激活行业包失败'),
    'industry-packs.deactivate-success': ('Pack désactivé', 'Pack deactivated', 'Pack desactivado', 'تم إلغاء تفعيل الحزمة', 'パックを無効化しました', '行业包已停用'),
    'industry-packs.deactivate-error': ('Échec de la désactivation', 'Failed to deactivate', 'Error al desactivar', 'فشل إلغاء التفعيل', '無効化に失敗しました', '停用失败'),
    'industry-packs.deactivate-title': ('Désactiver le pack ?', 'Deactivate the pack?', '¿Desactivar el pack?', 'إلغاء تفعيل الحزمة؟', 'パックを無効化しますか？', '停用此行业包？'),
    'industry-packs.deactivate-message': (
        'Le pack sera désactivé pour ce tenant. Ses contenus restent disponibles dans le catalogue et peuvent être réactivés.',
        'The pack will be deactivated for this tenant. Its content stays available in the catalog and can be reactivated.',
        'El pack se desactivará para este tenant. Su contenido permanece disponible en el catálogo y puede reactivarse.',
        'سيتم إلغاء تفعيل الحزمة لهذا المستأجر. يبقى محتواها متاحًا في الكتالوج ويمكن إعادة تفعيله.',
        'このテナントでパックが無効化されます。コンテンツはカタログに残り、再度有効化できます。',
        '此行业包将对该租户停用。其内容仍保留在目录中，可重新激活。'),

    # --- onglets -------------------------------------------------------------
    'industry-packs.tab-norms': ('Normes', 'Standards', 'Normas', 'المعايير', '規格', '标准'),
    'industry-packs.tab-kpis': ('KPIs', 'KPIs', 'KPIs', 'مؤشرات الأداء', 'KPI', 'KPI'),
    'industry-packs.tab-ishikawa': ('Templates Ishikawa', 'Ishikawa templates', 'Plantillas Ishikawa', 'قوالب إيشيكاوا', '石川図テンプレート', '石川图模板'),
    'industry-packs.tab-pokayoke': ('Poka-Yoke', 'Poka-Yoke', 'Poka-Yoke', 'بوكا-يوكي', 'ポカヨケ', '防错（Poka-Yoke）'),
    'industry-packs.tab-glossary': ('Glossaire', 'Glossary', 'Glosario', 'المسرد', '用語集', '术语表'),

    # --- onglet Normes -------------------------------------------------------
    'industry-packs.norms-empty': ('Aucune norme référencée par ce pack.', 'No standard referenced by this pack.', 'Ninguna norma referenciada por este pack.', 'لا توجد معايير مرجعية لهذه الحزمة.', 'このパックが参照する規格はありません。', '此行业包未引用任何标准。'),

    # --- onglet KPIs ---------------------------------------------------------
    'industry-packs.kpi-formula': ('Formule', 'Formula', 'Fórmula', 'الصيغة', '計算式', '公式'),
    'industry-packs.kpi-unit': ('Unité', 'Unit', 'Unidad', 'الوحدة', '単位', '单位'),
    'industry-packs.kpi-target': ('Cible', 'Target', 'Objetivo', 'الهدف', '目標', '目标'),
    'industry-packs.kpi-thresholds': ('Seuils', 'Thresholds', 'Umbrales', 'العتبات', 'しきい値', '阈值'),
    'industry-packs.kpi-owner': ('Propriétaire', 'Owner', 'Propietario', 'المالك', '責任者', '负责人'),
    'industry-packs.kpis-flat-note': (
        'Ce pack référence des KPIs par identifiant ; les définitions détaillées (formule, seuils) ne sont pas encore structurées.',
        'This pack references KPIs by identifier; detailed definitions (formula, thresholds) are not yet structured.',
        'Este pack referencia KPIs por identificador; las definiciones detalladas (fórmula, umbrales) aún no están estructuradas.',
        'تشير هذه الحزمة إلى مؤشرات الأداء بالمعرّف؛ التعريفات التفصيلية (الصيغة، العتبات) غير منظَّمة بعد.',
        'このパックはKPIを識別子で参照しています。詳細な定義（計算式、しきい値）はまだ構造化されていません。',
        '此行业包按标识符引用 KPI；详细定义（公式、阈值）尚未结构化。'),
    'industry-packs.kpis-empty': ('Aucun KPI défini par ce pack.', 'No KPI defined by this pack.', 'Ningún KPI definido por este pack.', 'لا توجد مؤشرات أداء معرّفة في هذه الحزمة.', 'このパックで定義されたKPIはありません。', '此行业包未定义任何 KPI。'),

    # --- onglet Ishikawa -----------------------------------------------------
    'industry-packs.ishikawa-empty': ('Aucun template Ishikawa dans ce pack.', 'No Ishikawa template in this pack.', 'Ninguna plantilla Ishikawa en este pack.', 'لا توجد قوالب إيشيكاوا في هذه الحزمة.', 'このパックに石川図テンプレートはありません。', '此行业包没有石川图模板。'),

    # --- onglet Poka-Yoke ----------------------------------------------------
    'industry-packs.pokayoke-empty': ('Aucun dispositif Poka-Yoke dans ce pack.', 'No Poka-Yoke device in this pack.', 'Ningún dispositivo Poka-Yoke en este pack.', 'لا توجد أدوات بوكا-يوكي في هذه الحزمة.', 'このパックにポカヨケはありません。', '此行业包没有防错装置。'),

    # --- onglet Glossaire ----------------------------------------------------
    'industry-packs.glossary-empty': ('Aucun glossaire dans ce pack.', 'No glossary in this pack.', 'Ningún glosario en este pack.', 'لا يوجد مسرد في هذه الحزمة.', 'このパックに用語集はありません。', '此行业包没有术语表。'),
}
