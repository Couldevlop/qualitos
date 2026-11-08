# -*- coding: utf-8 -*-
"""Table i18n — Piste d'audit de la plateforme (ADR 0109).

Les actes de l'éditeur sur les dossiers des clients : écran de consultation,
filtres, vérification d'intégrité, export du dossier d'audit, détail d'un acte.
id: (fr, en, es, ar, ja, zh).
"""

TRANSLATIONS = {

    # --- en-tête -----------------------------------------------------------
    'platform.audit.title': (
        "Piste d'audit de la plateforme", 'Platform audit trail',
        'Pista de auditoría de la plataforma', 'مسار تدقيق المنصة',
        'プラットフォーム監査証跡', '平台审计追踪'),
    'platform.audit.subtitle': (
        "Les actes de l'éditeur sur les dossiers des clients : créations, modules, "
        "tarifs, accès accordés aux cabinets. Chaque acte est chaîné par empreinte et "
        "la base refuse de le modifier. C'est la piste qu'une autorité demande.",
        'The vendor’s acts on client accounts: creations, modules, prices, access '
        'granted to firms. Each act is hash-chained and the database refuses to alter '
        'it. This is the trail an authority asks for.',
        'Los actos del proveedor sobre las cuentas de los clientes: creaciones, módulos, '
        'tarifas, accesos concedidos a despachos. Cada acto está encadenado por huella y '
        'la base se niega a modificarlo. Es la pista que exige una autoridad.',
        'أعمال المحرر على ملفات العملاء: الإنشاءات والوحدات والأسعار والصلاحيات الممنوحة '
        'للمكاتب. كل عمل مرتبط ببصمة وقاعدة البيانات ترفض تعديله. هذا هو المسار الذي تطلبه الجهة الرقابية.',
        'ベンダーが顧客アカウントに対して行った行為（作成、モジュール、料金、事務所への'
        'アクセス付与）。各行為はハッシュで連鎖され、データベースは変更を拒否します。'
        '当局が求める証跡です。',
        '供应商对客户账户执行的操作：创建、模块、价格、授予事务所的访问权限。'
        '每项操作均以哈希链接，数据库拒绝对其修改。这正是监管机构所要求的追踪记录。'),
    'platform.audit.append-only': (
        'Registre inaltérable', 'Append-only register', 'Registro inalterable',
        'سجل غير قابل للتعديل', '追記専用台帳', '仅可追加的登记簿'),

    # --- filtres -----------------------------------------------------------
    'platform.audit.filter-action': (
        "Type d'acte", 'Act type', 'Tipo de acto', 'نوع العمل', '行為の種別', '操作类型'),
    'platform.audit.filter-any-action': (
        'Tous les actes', 'All acts', 'Todos los actos', 'كل الأعمال', 'すべての行為', '所有操作'),
    'platform.audit.filter-tenant': (
        'Client visé', 'Client concerned', 'Cliente afectado', 'العميل المعني',
        '対象の顧客', '涉及的客户'),
    'platform.audit.filter-tenant-hint': (
        'Identifiant du client', 'Client identifier', 'Identificador del cliente',
        'معرّف العميل', '顧客の識別子', '客户标识'),
    'platform.audit.filter-actor': (
        'Auteur', 'Author', 'Autor', 'الفاعل', '実行者', '操作人'),
    'platform.audit.filter-from': (
        'Depuis', 'From', 'Desde', 'من', '開始', '起始'),
    'platform.audit.filter-to': (
        "Jusqu'à", 'To', 'Hasta', 'إلى', '終了', '截止'),
    'platform.audit.uuid-error': (
        'Identifiant attendu au format UUID.', 'Identifier must be a UUID.',
        'Se espera un identificador en formato UUID.', 'المعرّف مطلوب بصيغة UUID.',
        '識別子は UUID 形式で入力してください。', '标识需为 UUID 格式。'),
    'platform.audit.apply': (
        'Filtrer', 'Filter', 'Filtrar', 'تصفية', '絞り込む', '筛选'),
    'platform.audit.reset': (
        'Réinitialiser', 'Reset', 'Restablecer', 'إعادة التعيين', 'リセット', '重置'),

    # --- critère effectivement retenu par le serveur -----------------------
    'platform.audit.criterion-tenant': (
        "Filtre appliqué : le client visé. Les autres critères sont ignorés par le serveur.",
        'Filter applied: the client concerned. The server ignores the other criteria.',
        'Filtro aplicado: el cliente afectado. El servidor ignora los demás criterios.',
        'الفلتر المطبّق: العميل المعني. يتجاهل الخادم المعايير الأخرى.',
        '適用中の絞り込み: 対象の顧客。他の条件はサーバーで無視されます。',
        '已应用的筛选：涉及的客户。服务器会忽略其他条件。'),
    'platform.audit.criterion-action': (
        "Filtre appliqué : le type d'acte. Les autres critères sont ignorés par le serveur.",
        'Filter applied: the act type. The server ignores the other criteria.',
        'Filtro aplicado: el tipo de acto. El servidor ignora los demás criterios.',
        'الفلتر المطبّق: نوع العمل. يتجاهل الخادم المعايير الأخرى.',
        '適用中の絞り込み: 行為の種別。他の条件はサーバーで無視されます。',
        '已应用的筛选：操作类型。服务器会忽略其他条件。'),
    'platform.audit.criterion-actor': (
        "Filtre appliqué : l'auteur. Les autres critères sont ignorés par le serveur.",
        'Filter applied: the author. The server ignores the other criteria.',
        'Filtro aplicado: el autor. El servidor ignora los demás criterios.',
        'الفلتر المطبّق: الفاعل. يتجاهل الخادم المعايير الأخرى.',
        '適用中の絞り込み: 実行者。他の条件はサーバーで無視されます。',
        '已应用的筛选：操作人。服务器会忽略其他条件。'),
    'platform.audit.criterion-period': (
        "Filtre appliqué : la période. Renseignez les deux bornes pour qu'elle soit prise en compte.",
        'Filter applied: the period. Fill in both bounds for it to be taken into account.',
        'Filtro aplicado: el período. Indique ambos límites para que se tenga en cuenta.',
        'الفلتر المطبّق: الفترة. أدخل الحدّين كي تُؤخذ في الحساب.',
        '適用中の絞り込み: 期間。両端を入力すると有効になります。',
        '已应用的筛选：时间段。需填写起止两端才会生效。'),
    'platform.audit.criterion-none': (
        "Aucun filtre actif : toute la piste d'audit est affichée, du plus récent au plus ancien.",
        'No filter active: the whole audit trail is shown, newest first.',
        'Sin filtro activo: se muestra toda la pista de auditoría, de lo más reciente a lo más antiguo.',
        'لا فلتر مُطبّق: يُعرض مسار التدقيق بالكامل، من الأحدث إلى الأقدم.',
        '絞り込みなし: 監査証跡の全体を新しい順に表示しています。',
        '未启用筛选：显示完整审计追踪，按时间从新到旧。'),

    # --- tableau -----------------------------------------------------------
    'platform.audit.col-seq': (
        'N°', 'No.', 'N.º', 'الرقم', '番号', '序号'),
    'platform.audit.col-when': (
        'Survenu le', 'Occurred on', 'Ocurrido el', 'تاريخ الحدوث', '発生日時', '发生时间'),
    'platform.audit.col-actor': (
        'Auteur', 'Author', 'Autor', 'الفاعل', '実行者', '操作人'),
    'platform.audit.col-action': (
        'Acte', 'Act', 'Acto', 'العمل', '行為', '操作'),
    'platform.audit.col-target': (
        'Cible', 'Target', 'Objetivo', 'الهدف', '対象', '目标'),
    'platform.audit.col-hash': (
        'Empreinte', 'Hash', 'Huella', 'البصمة', 'ハッシュ', '哈希'),
    'platform.audit.no-actor': (
        'La plateforme', 'The platform', 'La plataforma', 'المنصة',
        'プラットフォーム', '平台'),
    'platform.audit.detail-aria': (
        "Afficher le détail de l'acte", 'Show act details', 'Ver el detalle del acto',
        'عرض تفاصيل العمل', '行為の詳細を表示', '查看操作详情'),
    'platform.audit.empty': (
        'Aucun acte ne correspond à ces critères.', 'No act matches these criteria.',
        'Ningún acto coincide con estos criterios.', 'لا يوجد عمل يطابق هذه المعايير.',
        'これらの条件に一致する行為はありません。', '没有符合这些条件的操作。'),
    'platform.audit.paginator-aria': (
        "Pagination de la piste d'audit", 'Audit trail pagination',
        'Paginación de la pista de auditoría', 'ترقيم صفحات مسار التدقيق',
        '監査証跡のページ送り', '审计追踪分页'),
    'platform.audit.load-failed': (
        "La piste d'audit n'a pas pu être chargée.", 'The audit trail could not be loaded.',
        'No se ha podido cargar la pista de auditoría.', 'لم يتمكّن النظام من تحميل مسار التدقيق.',
        '監査証跡を読み込めませんでした。', '无法加载审计追踪。'),

    # --- vérification d'intégrité ------------------------------------------
    'platform.audit.verify-title': (
        'Intégrité de la chaîne', 'Chain integrity', 'Integridad de la cadena',
        'سلامة السلسلة', 'チェーンの完全性', '链的完整性'),
    'platform.audit.verify-hint': (
        "Recalcule les empreintes, contrôle leur chaînage et compte les actes. Une "
        "rupture révèle une retouche ; un acte manquant se voit au comptage, qu'aucune "
        "empreinte ne montrerait.",
        'Recomputes the hashes, checks their chaining and counts the acts. A break '
        'reveals tampering; a missing act shows up in the count, which no hash would reveal.',
        'Recalcula las huellas, comprueba su encadenamiento y cuenta los actos. Una '
        'ruptura revela una manipulación; un acto ausente se detecta en el recuento, que '
        'ninguna huella mostraría.',
        'يعيد حساب البصمات ويتحقّق من تسلسلها ويعدّ الأعمال. الانقطاع يكشف تعديلاً؛ والعمل '
        'الناقص يظهر في العدّ، وهو ما لا تكشفه أي بصمة.',
        'ハッシュを再計算し、連鎖を検査して行為の件数を数えます。連鎖の断絶は改変を示し、'
        '欠落した行為は件数で判明します（ハッシュだけでは分かりません）。',
        '重新计算哈希、检查其链接并统计操作数量。链条断裂说明存在改动；'
        '缺失的操作可通过计数发现，而哈希本身无法显示。'),
    'platform.audit.verify-all': (
        'Vérifier tout le journal', 'Verify the whole log', 'Verificar todo el registro',
        'التحقّق من كامل السجل', 'ログ全体を検証', '验证整个日志'),
    'platform.audit.verify-page': (
        'Sur la page affichée', 'On the displayed page', 'En la página mostrada',
        'على الصفحة المعروضة', '表示中のページのみ', '仅当前显示页'),
    'platform.audit.verify-running': (
        'Vérification en cours…', 'Verification in progress…', 'Verificación en curso…',
        'جارٍ التحقّق…', '検証中…', '正在验证…'),
    'platform.audit.verify-failed': (
        "La vérification d'intégrité a échoué.", 'The integrity check failed.',
        'La verificación de integridad ha fallado.', 'فشل التحقّق من السلامة.',
        '完全性の検証に失敗しました。', '完整性校验失败。'),
    'platform.audit.verdict-ok': (
        'Chaîne intacte', 'Chain intact', 'Cadena intacta', 'السلسلة سليمة',
        'チェーンは無傷', '链条完好'),
    'platform.audit.verdict-ko': (
        'Chaîne rompue', 'Chain broken', 'Cadena rota', 'السلسلة مقطوعة',
        'チェーンが断絶', '链条断裂'),
    'platform.audit.verdict-count': (
        'Actes examinés', 'Acts examined', 'Actos examinados', 'الأعمال المدروسة',
        '検証した行為', '已检查的操作'),
    'platform.audit.verdict-scope-page': (
        ' — page affichée seulement', ' — displayed page only',
        ' — solo la página mostrada', ' — الصفحة المعروضة فقط',
        ' — 表示中のページのみ', ' — 仅当前显示页'),
    'platform.audit.verdict-caveat': (
        "Aucune retouche détectée. La chaîne ne dit rien d'un acte qui n'aurait "
        "jamais été consigné.",
        'No tampering detected. The chain says nothing about an act that was never recorded.',
        'No se ha detectado manipulación. La cadena no dice nada de un acto que nunca se '
        'haya registrado.',
        'لم يُكتشف أي تعديل. السلسلة لا تقول شيئاً عن عمل لم يُسجّل أبداً.',
        '改変は検出されませんでした。記録されなかった行為についてチェーンは何も示しません。',
        '未检测到改动。对于从未被记录的操作，链条无法提供任何信息。'),

    # --- export du dossier -------------------------------------------------
    'platform.audit.export-title': (
        "Dossier d'audit", 'Audit file', 'Expediente de auditoría', 'ملف التدقيق',
        '監査ファイル', '审计档案'),
    'platform.audit.export-hint': (
        "Tout le journal, son verdict d'intégrité et un sceau, dans un fichier "
        "vérifiable sans la plateforme. L'export est lui-même consigné.",
        'The whole log, its integrity verdict and a seal, in a file verifiable without '
        'the platform. The export is itself recorded.',
        'Todo el registro, su veredicto de integridad y un sello, en un archivo '
        'verificable sin la plataforma. La exportación queda a su vez registrada.',
        'كامل السجل وحكم سلامته وخاتم، في ملف قابل للتحقّق دون المنصة. والتصدير نفسه يُسجّل.',
        'ログ全体、完全性の判定、そして封印を、プラットフォームなしで検証できる'
        'ファイルにまとめます。エクスポート自体も記録されます。',
        '整个日志、其完整性判定以及一枚印章，汇成一个无需平台即可验证的文件。'
        '导出本身也会被记录。'),
    'platform.audit.export': (
        'Exporter le dossier', 'Export the file', 'Exportar el expediente',
        'تصدير الملف', 'ファイルをエクスポート', '导出档案'),
    'platform.audit.export-running': (
        'Export en cours…', 'Export in progress…', 'Exportación en curso…',
        'جارٍ التصدير…', 'エクスポート中…', '正在导出…'),
    'platform.audit.export-failed': (
        "Le dossier d'audit n'a pas pu être exporté.", 'The audit file could not be exported.',
        'No se ha podido exportar el expediente de auditoría.', 'لم يتمكّن النظام من تصدير ملف التدقيق.',
        '監査ファイルをエクスポートできませんでした。', '无法导出审计档案。'),
    'platform.audit.export-seal': (
        'Sceau du dernier dossier', 'Seal of the latest file', 'Sello del último expediente',
        'خاتم الملف الأخير', '最新ファイルの封印', '最近一次档案的印章'),

    # --- détail d'un acte --------------------------------------------------
    'platform.audit.detail-title': (
        "Détail de l'acte", 'Act details', 'Detalle del acto', 'تفاصيل العمل',
        '行為の詳細', '操作详情'),
    'platform.audit.close-detail-aria': (
        'Fermer le détail', 'Close the details', 'Cerrar el detalle', 'إغلاق التفاصيل',
        '詳細を閉じる', '关闭详情'),
    'platform.audit.field-summary': (
        'Résumé', 'Summary', 'Resumen', 'الملخّص', '概要', '摘要'),
    'platform.audit.recorded-at': (
        'Enregistré', 'Recorded', 'Registrado', 'تاريخ التسجيل', '記録日時', '记录时间'),
    'platform.audit.field-tenant': (
        'Client concerné', 'Client concerned', 'Cliente afectado', 'العميل المعني',
        '対象の顧客', '涉及的客户'),
    'platform.audit.field-ip': (
        "Adresse d'origine", 'Source address', 'Dirección de origen', 'عنوان المصدر',
        '発信元アドレス', '来源地址'),
    'platform.audit.field-agent': (
        'Logiciel employé', 'Software used', 'Software utilizado', 'البرنامج المستخدم',
        '使用したソフトウェア', '所用软件'),
    'platform.audit.field-hash': (
        'Empreinte', 'Hash', 'Huella', 'البصمة', 'ハッシュ', '哈希'),
    'platform.audit.field-previous': (
        'Empreinte précédente', 'Previous hash', 'Huella anterior', 'البصمة السابقة',
        '直前のハッシュ', '上一条哈希'),
    'platform.audit.chain-origin': (
        'Premier acte du registre', 'First act of the register',
        'Primer acto del registro', 'أول عمل في السجل',
        '台帳の最初の行為', '登记簿的首项操作'),
    'platform.audit.field-payload': (
        'Détail enregistré', 'Recorded details', 'Detalle registrado',
        'التفاصيل المسجّلة', '記録された詳細', '已记录的详情'),

    # --- libellés des actes ------------------------------------------------
    'platform.audit.act.tenant-created': (
        'Client créé', 'Client created', 'Cliente creado', 'تم إنشاء العميل',
        '顧客を作成', '已创建客户'),
    'platform.audit.act.tenant-updated': (
        'Client modifié', 'Client updated', 'Cliente modificado', 'تم تعديل العميل',
        '顧客を変更', '已修改客户'),
    'platform.audit.act.tenant-deactivated': (
        'Client désactivé', 'Client deactivated', 'Cliente desactivado',
        'تم تعطيل العميل', '顧客を無効化', '已停用客户'),
    'platform.audit.act.module-subscribed': (
        'Module ouvert', 'Module opened', 'Módulo abierto', 'تم تفعيل الوحدة',
        'モジュールを開放', '已开通模块'),
    'platform.audit.act.module-cancelled': (
        'Module fermé', 'Module closed', 'Módulo cerrado', 'تم إغلاق الوحدة',
        'モジュールを停止', '已关闭模块'),
    'platform.audit.act.price-set': (
        'Tarif fixé', 'Price set', 'Tarifa fijada', 'تم تحديد السعر',
        '料金を設定', '已设定价格'),
    'platform.audit.act.firm-created': (
        'Cabinet créé', 'Firm created', 'Despacho creado', 'تم إنشاء المكتب',
        '事務所を作成', '已创建事务所'),
    'platform.audit.act.firm-updated': (
        'Cabinet modifié', 'Firm updated', 'Despacho modificado', 'تم تعديل المكتب',
        '事務所を変更', '已修改事务所'),
    'platform.audit.act.firm-active': (
        'Cabinet suspendu ou réactivé', 'Firm suspended or reactivated',
        'Despacho suspendido o reactivado', 'تم تعليق المكتب أو إعادة تنشيطه',
        '事務所を停止または再開', '已暂停或恢复事务所'),
    'platform.audit.act.consultant-added': (
        'Consultant rattaché', 'Consultant attached', 'Consultor vinculado',
        'تم ربط المستشار', 'コンサルタントを登録', '已关联顾问'),
    'platform.audit.act.consultant-active': (
        'Consultant désactivé ou réactivé', 'Consultant deactivated or reactivated',
        'Consultor desactivado o reactivado', 'تم تعطيل المستشار أو إعادة تنشيطه',
        'コンサルタントを無効化または再開', '已停用或恢复顾问'),
    'platform.audit.act.engagement-proposed': (
        'Mission proposée', 'Engagement proposed', 'Misión propuesta',
        'تم اقتراح المهمة', '業務を提案', '已提议委托'),
    'platform.audit.act.engagement-granted': (
        'Accès accordé à un cabinet', 'Access granted to a firm',
        'Acceso concedido a un despacho', 'تم منح المكتب صلاحية الوصول',
        '事務所にアクセスを付与', '已向事务所授予访问权限'),
    'platform.audit.act.engagement-declined': (
        'Mission refusée', 'Engagement declined', 'Misión rechazada',
        'تم رفض المهمة', '業務を辞退', '已拒绝委托'),
    'platform.audit.act.engagement-revoked': (
        'Accès retiré à un cabinet', 'Access revoked from a firm',
        'Acceso retirado a un despacho', 'تم سحب صلاحية الوصول من المكتب',
        '事務所のアクセスを取消', '已撤销事务所的访问权限'),
    'platform.audit.act.exported': (
        "Dossier d'audit exporté", 'Audit file exported', 'Expediente de auditoría exportado',
        'تم تصدير ملف التدقيق', '監査ファイルをエクスポート', '已导出审计档案'),
}
