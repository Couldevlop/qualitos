# -*- coding: utf-8 -*-
"""Table i18n - domaine workflow-designer (§5.4). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    # --- Liste ---
    'workflow.list.title': ('Designer de workflow', 'Workflow designer', 'Diseñador de flujos', 'مصمّم سير العمل', 'ワークフローデザイナー', '工作流设计器'),
    'workflow.list.subtitle': ('Concevez vos processus qualité en BPMN 2.0, sans code : glisser-déposer, versioning et publication.', 'Design your quality processes in BPMN 2.0, no code: drag & drop, versioning and publishing.', 'Diseñe sus procesos de calidad en BPMN 2.0, sin código: arrastrar y soltar, versionado y publicación.', 'صمّم عمليات الجودة بـ BPMN 2.0 بدون برمجة: السحب والإفلات، الإصدارات والنشر.', 'BPMN 2.0 で品質プロセスをノーコード設計：ドラッグ＆ドロップ、バージョン管理、公開。', '以 BPMN 2.0 无代码设计质量流程：拖放、版本管理与发布。'),
    'workflow.list.new': ('Nouveau workflow', 'New workflow', 'Nuevo flujo', 'سير عمل جديد', '新規ワークフロー', '新建工作流'),
    'workflow.list.version': ('Version {$INTERPOLATION}', 'Version {$INTERPOLATION}', 'Versión {$INTERPOLATION}', 'الإصدار {$INTERPOLATION}', 'バージョン {$INTERPOLATION}', '版本 {$INTERPOLATION}'),
    'workflow.list.publish': ('Publier', 'Publish', 'Publicar', 'نشر', '公開', '发布'),
    'workflow.list.archive': ('Archiver', 'Archive', 'Archivar', 'أرشفة', 'アーカイブ', '归档'),
    'workflow.list.empty': ('Aucun workflow pour ce filtre. Cliquez sur Nouveau workflow pour démarrer.', 'No workflow for this filter. Click New workflow to get started.', 'Ningún flujo para este filtro. Haga clic en Nuevo flujo para empezar.', 'لا يوجد سير عمل لهذا المرشّح. انقر على سير عمل جديد للبدء.', 'この絞り込みに該当するワークフローはありません。新規ワークフローをクリックして開始してください。', '没有符合此筛选条件的工作流。点击新建工作流开始。'),
    'workflow.list.published': ('Workflow publié.', 'Workflow published.', 'Flujo publicado.', 'تم نشر سير العمل.', 'ワークフローを公開しました。', '工作流已发布。'),
    'workflow.list.publish-error': ('Publication impossible.', 'Cannot publish.', 'No se puede publicar.', 'تعذّر النشر.', '公開できません。', '无法发布。'),
    'workflow.list.archived': ('Workflow archivé.', 'Workflow archived.', 'Flujo archivado.', 'تمت أرشفة سير العمل.', 'ワークフローをアーカイブしました。', '工作流已归档。'),
    'workflow.list.archive-error': ('Archivage impossible.', 'Cannot archive.', 'No se puede archivar.', 'تعذّرت الأرشفة.', 'アーカイブできません。', '无法归档。'),

    # --- Éditeur ---
    'workflow.editor.untitled': ('Nouveau workflow', 'New workflow', 'Nuevo flujo', 'سير عمل جديد', '新規ワークフロー', '新建工作流'),
    'workflow.editor.eyebrow': ('Designer de workflow · BPMN 2.0', 'Workflow designer · BPMN 2.0', 'Diseñador de flujos · BPMN 2.0', 'مصمّم سير العمل · BPMN 2.0', 'ワークフローデザイナー · BPMN 2.0', '工作流设计器 · BPMN 2.0'),
    'workflow.editor.name-label': ('Nom du workflow', 'Workflow name', 'Nombre del flujo', 'اسم سير العمل', 'ワークフロー名', '工作流名称'),
    'workflow.editor.description-label': ('Description (optionnel)', 'Description (optional)', 'Descripción (opcional)', 'الوصف (اختياري)', '説明（任意）', '描述（可选）'),
    'workflow.editor.version': ('v{$INTERPOLATION}', 'v{$INTERPOLATION}', 'v{$INTERPOLATION}', 'إصدار {$INTERPOLATION}', 'v{$INTERPOLATION}', 'v{$INTERPOLATION}'),
    'workflow.editor.publish': ('Publier', 'Publish', 'Publicar', 'نشر', '公開', '发布'),
    'workflow.editor.save': ('Enregistrer', 'Save', 'Guardar', 'حفظ', '保存', '保存'),
    'workflow.editor.back-tooltip': ('Retour à la liste', 'Back to list', 'Volver a la lista', 'العودة إلى القائمة', '一覧へ戻る', '返回列表'),
    'workflow.editor.back-aria': ('Retour', 'Back', 'Volver', 'رجوع', '戻る', '返回'),
    'workflow.editor.readonly': ('Ce workflow est {$INTERPOLATION} : il est figé en lecture seule. Dupliquez-le pour le modifier.', 'This workflow is {$INTERPOLATION}: it is read-only. Duplicate it to edit.', 'Este flujo está {$INTERPOLATION}: es de solo lectura. Duplíquelo para editarlo.', 'سير العمل هذا {$INTERPOLATION}: للقراءة فقط. كرّره لتعديله.', 'このワークフローは {$INTERPOLATION} です：読み取り専用。編集するには複製してください。', '此工作流为 {$INTERPOLATION}：只读。复制后方可编辑。'),
    'workflow.editor.canvas-loading': ("Initialisation de l'éditeur BPMN…", 'Initializing the BPMN editor…', 'Inicializando el editor BPMN…', 'جارٍ تهيئة محرّر BPMN…', 'BPMN エディターを初期化中…', '正在初始化 BPMN 编辑器……'),
    'workflow.editor.saved': ('Workflow enregistré.', 'Workflow saved.', 'Flujo guardado.', 'تم حفظ سير العمل.', 'ワークフローを保存しました。', '工作流已保存。'),
    'workflow.editor.save-error': ('Enregistrement impossible.', 'Cannot save.', 'No se puede guardar.', 'تعذّر الحفظ.', '保存できません。', '无法保存。'),
    'workflow.editor.published': ('Workflow publié.', 'Workflow published.', 'Flujo publicado.', 'تم نشر سير العمل.', 'ワークフローを公開しました。', '工作流已发布。'),
    'workflow.editor.publish-error': ('Publication impossible.', 'Cannot publish.', 'No se puede publicar.', 'تعذّر النشر.', '公開できません。', '无法发布。'),
    'workflow.editor.load-error': ('Workflow introuvable.', 'Workflow not found.', 'Flujo no encontrado.', 'تعذّر العثور على سير العمل.', 'ワークフローが見つかりません。', '未找到工作流。'),
    'workflow.editor.canvas-error': ("Impossible de charger l'éditeur de diagramme.", 'Cannot load the diagram editor.', 'No se puede cargar el editor de diagramas.', 'تعذّر تحميل محرّر المخطط.', 'ダイアグラムエディターを読み込めません。', '无法加载图编辑器。'),
}
