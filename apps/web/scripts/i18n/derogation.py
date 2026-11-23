# -*- coding: utf-8 -*-
"""Table i18n — la derogation client (ADR 0138).

id: (fr, en, es, ar, ja, zh).

Trois familles de mots, et elles ne disent pas la meme chose :

- `status-*` nomme l'etat du DOSSIER dans le cycle interne. « Approuvee par la
  qualite » n'est pas « Signee » : la premiere nomme qui engage sa
  responsabilite, la seconde decrit un geste. Le libelle est fige (ADR 0052).
- `decision-*` nomme la reponse du CLIENT, qui arrive hors de la plateforme.
  « Acceptee sous conditions » vaut un accord, et ce n'est pas la meme chose
  qu'une acceptation : la ramener a l'etat du dossier perdrait la condition.
- `validation.*` nomme les gestes du cycle. Le manager qualite NE REFUSE PAS :
  il signe, ou il renvoie pour correction -- refuser usurperait la decision du
  client, seul a pouvoir accorder la derogation. Aucun mot de refus ici, donc,
  et c'est voulu.

Le vocabulaire anglais suit celui des normes : « concession » (ISO 9000, 3.12.5)
et non « derogation », « waiver » restant l'usage nord-americain.
"""

TRANSLATIONS = {

    # ---------------------------------------------------------------- menu
    'nav.derogations': (
        'Dérogations client',
        'Customer concessions',
        'Concesiones de cliente',
        'تنازلات العميل',
        '顧客特別採用',
        '客户让步许可',
    ),

    # ------------------------------------------------------------- registre
    'derogation.eyebrow': (
        'Non-conformité',
        'Nonconformity',
        'No conformidad',
        'عدم المطابقة',
        '不適合',
        '不符合',
    ),
    'derogation.title': (
        'Dérogations client',
        'Customer concessions',
        'Concesiones de cliente',
        'تنازلات العميل',
        '顧客特別採用',
        '客户让步许可',
    ),
    'derogation.subtitle': (
        "Demandez à votre client l'autorisation d'expédier un lot qui s'écarte du plan. Chaque dossier est validé en interne par un manager qualité avant de partir.",
        'Ask your customer for authorisation to ship a batch that departs from the drawing. Every case is approved internally by a quality manager before it goes out.',
        'Solicite a su cliente la autorización para enviar un lote que se desvía del plano. Cada expediente se valida internamente por un responsable de calidad antes de salir.',
        'اطلب من عميلك الإذن بشحن دفعة تخرج عن المخطط. تُعتمد كل حالة داخليًا من مدير الجودة قبل إرسالها.',
        '図面から外れたロットの出荷許可を顧客に依頼します。各案件は送付前に品質マネージャーが社内で承認します。',
        '就偏离图纸的批次向客户申请发货许可。每份案卷在对外发出前均由质量经理在内部审批。',
    ),
    'derogation.tabs': (
        'Vues du registre',
        'Register views',
        'Vistas del registro',
        'عروض السجل',
        '登録簿の表示',
        '登记簿视图',
    ),
    'derogation.tab-register': (
        'Registre',
        'Register',
        'Registro',
        'السجل',
        '登録簿',
        '登记簿',
    ),
    'derogation.tab-approvals': (
        'Mes validations',
        'My approvals',
        'Mis validaciones',
        'اعتماداتي',
        '自分の承認',
        '我的审批',
    ),
    'derogation.new': (
        'Nouvelle dérogation',
        'New concession',
        'Nueva concesión',
        'تنازل جديد',
        '新規特別採用',
        '新建让步许可',
    ),
    'derogation.load-failed': (
        "Le registre n'a pas pu être chargé.",
        'The register could not be loaded.',
        'No se pudo cargar el registro.',
        'لم يتمكن النظام من تحميل السجل.',
        '登録簿を読み込めませんでした。',
        '无法加载登记簿。',
    ),
    'derogation.empty-register': (
        'Aucune dérogation ne correspond à cette recherche.',
        'No concession matches this search.',
        'Ninguna concesión coincide con esta búsqueda.',
        'لا يوجد تنازل يطابق هذا البحث.',
        'この検索に一致する特別採用はありません。',
        '没有符合此搜索条件的让步许可。',
    ),
    'derogation.empty-approvals': (
        "Aucun dossier n'attend votre signature.",
        'No case is waiting for your signature.',
        'Ningún expediente espera su firma.',
        'لا توجد حالة تنتظر توقيعك.',
        '署名をお待ちしている案件はありません。',
        '没有案卷等待您的签署。',
    ),

    # --------------------------------------------------------------- filtres
    'derogation.filter-owner': (
        'Créateur',
        'Raised by',
        'Creador',
        'المُنشئ',
        '起票者',
        '创建人',
    ),
    'derogation.filter-part': (
        'Produit',
        'Part',
        'Producto',
        'المنتج',
        '製品',
        '产品',
    ),
    'derogation.filter-customer': (
        'Client',
        'Customer',
        'Cliente',
        'العميل',
        '顧客',
        '客户',
    ),
    'derogation.filter-status': (
        'Statut',
        'Status',
        'Estado',
        'الحالة',
        'ステータス',
        '状态',
    ),
    'derogation.filter-all': (
        'Tous',
        'All',
        'Todos',
        'الكل',
        'すべて',
        '全部',
    ),
    'derogation.filter-apply': (
        'Filtrer',
        'Filter',
        'Filtrar',
        'تصفية',
        '絞り込む',
        '筛选',
    ),
    'derogation.filter-clear': (
        'Effacer',
        'Clear',
        'Borrar',
        'مسح',
        'クリア',
        '清除',
    ),

    # -------------------------------------------------------------- colonnes
    'derogation.col-created': (
        'Date de création',
        'Raised on',
        'Fecha de creación',
        'تاريخ الإنشاء',
        '起票日',
        '创建日期',
    ),
    'derogation.col-reference': (
        'Référence',
        'Reference',
        'Referencia',
        'المرجع',
        '管理番号',
        '编号',
    ),
    'derogation.col-part': (
        'Produit',
        'Part',
        'Producto',
        'المنتج',
        '製品',
        '产品',
    ),
    'derogation.col-customer': (
        'Client',
        'Customer',
        'Cliente',
        'العميل',
        '顧客',
        '客户',
    ),
    'derogation.col-author': (
        'Créateur',
        'Raised by',
        'Creador',
        'المُنشئ',
        '起票者',
        '创建人',
    ),
    'derogation.col-deviation': (
        'Défaut',
        'Deviation',
        'Defecto',
        'الانحراف',
        '不具合',
        '缺陷',
    ),
    'derogation.col-status': (
        'Statut',
        'Status',
        'Estado',
        'الحالة',
        'ステータス',
        '状态',
    ),
    'derogation.col-attachments': (
        'Pièces jointes',
        'Attachments',
        'Adjuntos',
        'المرفقات',
        '添付資料',
        '附件',
    ),
    'derogation.prev-page': (
        'Page précédente',
        'Previous page',
        'Página anterior',
        'الصفحة السابقة',
        '前のページ',
        '上一页',
    ),
    'derogation.next-page': (
        'Page suivante',
        'Next page',
        'Página siguiente',
        'الصفحة التالية',
        '次のページ',
        '下一页',
    ),

    # ----------------------------------------------------------------- etats
    'derogation.status.draft': (
        'Brouillon',
        'Draft',
        'Borrador',
        'مسودة',
        '下書き',
        '草稿',
    ),
    'derogation.status.pending-internal': (
        'En attente du manager qualité',
        'Awaiting quality manager',
        'Pendiente del responsable de calidad',
        'في انتظار مدير الجودة',
        '品質マネージャー承認待ち',
        '等待质量经理',
    ),
    'derogation.status.to-amend': (
        'À modifier',
        'To amend',
        'Para modificar',
        'للتعديل',
        '修正要',
        '待修改',
    ),
    'derogation.status.quality-approved': (
        'Approuvée par la qualité',
        'Approved by quality',
        'Aprobada por calidad',
        'معتمدة من الجودة',
        '品質部門承認済み',
        '质量部门已批准',
    ),
    'derogation.status.pending-customer': (
        'En attente du client',
        'Awaiting customer',
        'Pendiente del cliente',
        'في انتظار العميل',
        '顧客回答待ち',
        '等待客户',
    ),
    'derogation.status.accepted': (
        'Acceptée',
        'Accepted',
        'Aceptada',
        'مقبولة',
        '受諾',
        '已接受',
    ),
    'derogation.status.refused': (
        'Refusée',
        'Refused',
        'Rechazada',
        'مرفوضة',
        '却下',
        '已拒绝',
    ),

    # ------------------------------------------------- reponse du client
    'derogation.decision.pending': (
        'En attente',
        'Pending',
        'Pendiente',
        'قيد الانتظار',
        '保留中',
        '待定',
    ),
    'derogation.decision.accepted': (
        'Acceptée',
        'Accepted',
        'Aceptada',
        'مقبولة',
        '受諾',
        '已接受',
    ),
    'derogation.decision.accepted-conditions': (
        'Acceptée sous conditions',
        'Accepted with conditions',
        'Aceptada con condiciones',
        'مقبولة بشروط',
        '条件付き受諾',
        '有条件接受',
    ),
    'derogation.decision.refused': (
        'Refusée',
        'Refused',
        'Rechazada',
        'مرفوضة',
        '却下',
        '已拒绝',
    ),

    # --------------------------------------------------------- nature
    'derogation.shipment.to-ship': (
        'Pièces à expédier',
        'Parts yet to ship',
        'Piezas por enviar',
        'قطع لم تُشحن بعد',
        '出荷前の部品',
        '待发运零件',
    ),
    'derogation.shipment.already-shipped': (
        'Pièces déjà expédiées',
        'Parts already shipped',
        'Piezas ya enviadas',
        'قطع تم شحنها بالفعل',
        '出荷済みの部品',
        '已发运零件',
    ),
    'derogation.origin.product': (
        'Lié au produit',
        'Product-related',
        'Relacionado con el producto',
        'متعلق بالمنتج',
        '製品に起因',
        '与产品相关',
    ),
    'derogation.origin.process': (
        'Lié au process',
        'Process-related',
        'Relacionado con el proceso',
        'متعلق بالعملية',
        '工程に起因',
        '与过程相关',
    ),
    'derogation.occurrence.first': (
        'Première occurrence',
        'First occurrence',
        'Primera ocurrencia',
        'الحدوث الأول',
        '初回発生',
        '首次发生',
    ),
    'derogation.occurrence.repeat': (
        'Répétition',
        'Repeat occurrence',
        'Repetición',
        'حدوث متكرر',
        '再発',
        '重复发生',
    ),
    'derogation.duration.temporary': (
        'Temporaire',
        'Temporary',
        'Temporal',
        'مؤقت',
        '一時的',
        '临时',
    ),
    'derogation.duration.permanent': (
        'Permanente',
        'Permanent',
        'Permanente',
        'دائم',
        '恒久的',
        '永久',
    ),
}
