# -*- coding: utf-8 -*-
"""Table i18n - cabinets partenaires (ADR 0106). id: (fr, en, es, ar, ja, zh).

Trois surfaces partagent ce vocabulaire : la console d'un cabinet (`partner.*`),
le paramétrage d'un client (`partner.access.*`) et la console de l'éditeur
(`platform.firms.*`). Les libellés de portée et d'état sont volontairement
communs aux trois : un accès « Ouvert » doit se dire du même mot partout, sans
quoi les trois parties d'une même décision ne se reconnaissent plus.
"""

TRANSLATIONS = {

    # --- vocabulaire commun aux trois surfaces -----------------------
    'partner.scope.read': (
        'Consultation', 'Read-only', 'Consulta', 'قراءة فقط', '閲覧', '查阅'),
    'partner.scope.intervene': (
        'Intervention', 'Intervention', 'Intervención', 'تدخّل', '実務介入', '介入'),
    'partner.status.proposed': (
        'En attente de votre décision', 'Awaiting your decision',
        'Pendiente de su decisión', 'في انتظار قرارك',
        'あなたの判断待ち', '等待您的决定'),
    'partner.status.open': (
        'Ouvert', 'Open', 'Abierto', 'مفتوح', '有効', '已开放'),
    'partner.status.scheduled': (
        'Accordé, hors période', 'Granted, outside its dates',
        'Concedido, fuera del periodo', 'ممنوح، خارج المدة',
        '許可済み（期間外）', '已授权，不在期间内'),
    'partner.status.declined': (
        'Refusé', 'Declined', 'Rechazado', 'مرفوض', '却下', '已拒绝'),
    'partner.status.revoked': (
        'Repris', 'Revoked', 'Retirado', 'مسحوب', '取り消し', '已收回'),

    # --- console d'un cabinet ----------------------------------------
    'partner.brand.console': (
        'Console cabinet', 'Firm console', 'Consola del despacho',
        'لوحة المكتب', '事務所コンソール', '事务所控制台'),
    'partner.user.logout': (
        'Se déconnecter', 'Sign out', 'Cerrar sesión', 'تسجيل الخروج',
        'ログアウト', '退出登录'),
    'partner.portfolio.title': (
        'Mon portefeuille', 'My portfolio', 'Mi cartera', 'محفظتي',
        'マイポートフォリオ', '我的客户组合'),
    'partner.portfolio.subtitle': (
        'Les clients qui vous ont ouvert un accès, et jusqu’à quand.',
        'The clients who have granted you access, and until when.',
        'Los clientes que le han abierto un acceso, y hasta cuándo.',
        'العملاء الذين منحوك وصولًا، وإلى متى.',
        'アクセスを許可しているクライアントと、その期限。',
        '已向您开放访问的客户，以及开放到何时。'),
    'partner.portfolio.reload': (
        'Recharger', 'Reload', 'Recargar', 'إعادة التحميل', '再読み込み', '重新加载'),
    'partner.portfolio.loading': (
        'Chargement de votre portefeuille…', 'Loading your portfolio…',
        'Cargando su cartera…', 'جارٍ تحميل محفظتك…',
        'ポートフォリオを読み込み中…', '正在加载您的客户组合…'),
    'partner.portfolio.until': (
        'Jusqu’au', 'Until', 'Hasta el', 'حتى', '期限', '截至'),
    'partner.portfolio.unlinked': (
        'Votre compte n’est rattaché à aucun cabinet. Demandez à votre cabinet de vous inscrire '
        'avec l’adresse de courriel de ce compte.',
        'Your account is not linked to any firm. Ask your firm to register you with this '
        'account’s email address.',
        'Su cuenta no está vinculada a ningún despacho. Pida a su despacho que le registre con '
        'la dirección de correo de esta cuenta.',
        'حسابك غير مرتبط بأي مكتب. اطلب من مكتبك تسجيلك بعنوان البريد الإلكتروني لهذا الحساب.',
        'お使いのアカウントはどの事務所にも紐づいていません。このアカウントのメールアドレスで登録するよう、所属事務所に依頼してください。',
        '您的账号尚未关联任何事务所。请让事务所用本账号的邮箱为您登记。'),
    'partner.portfolio.empty': (
        'Aucun client ne vous a ouvert d’accès aujourd’hui. Un accès prend fin à sa date de '
        'terme, sans préavis : si une mission vient de se terminer, elle a quitté cette liste.',
        'No client has an open access for you today. An access ends on its end date, with no '
        'notice: if an engagement has just ended, it has left this list.',
        'Ningún cliente le tiene hoy un acceso abierto. Un acceso termina en su fecha de fin, '
        'sin preaviso: si una misión acaba de terminar, ha salido de esta lista.',
        'لا يوجد اليوم أي عميل بوصول مفتوح لك. ينتهي الوصول في تاريخ انتهائه دون إشعار: '
        'فإذا انتهت مهمة للتو فقد خرجت من هذه القائمة.',
        '本日アクセスが開いているクライアントはありません。アクセスは予告なく終了日に閉じます。終了した案件はこの一覧から消えます。',
        '今天没有客户为您开放访问。访问会在终止日期自动结束，不另行通知：刚结束的任务已从此列表中消失。'),
    'partner.portfolio.link-failed': (
        'Votre compte n’a pas pu être rattaché à votre cabinet.',
        'Your account could not be linked to your firm.',
        'No se ha podido vincular su cuenta a su despacho.',
        'تعذّر ربط حسابك بمكتبك.',
        'お使いのアカウントを事務所に紐づけられませんでした。',
        '无法将您的账号关联到事务所。'),
    'partner.portfolio.load-failed': (
        'Votre portefeuille n’a pas pu être chargé.', 'Your portfolio could not be loaded.',
        'No se ha podido cargar su cartera.', 'تعذّر تحميل محفظتك.',
        'ポートフォリオを読み込めませんでした。', '无法加载您的客户组合。'),

    # --- paramétrage d'un client : accorder, refuser, reprendre ------
    'partner.access.title': (
        'Cabinets', 'Partner firms', 'Despachos', 'المكاتب', '事務所', '合作事务所'),
    'partner.access.subtitle': (
        'Qui, hors de votre organisation, peut entrer dans vos données — et jusqu’à quand.',
        'Who, outside your organisation, may enter your data — and until when.',
        'Quién, fuera de su organización, puede entrar en sus datos — y hasta cuándo.',
        'من يمكنه، من خارج مؤسستك، الدخول إلى بياناتك — وإلى متى.',
        '組織外の誰が自社データに入れるのか、そしていつまでか。',
        '在您的组织之外，谁可以进入您的数据——以及开放到何时。'),
    'partner.access.reload': (
        'Recharger', 'Reload', 'Recargar', 'إعادة التحميل', '再読み込み', '重新加载'),
    'partner.access.scope': (
        'Un cabinet n’entre chez vous que si vous l’accordez, et son accès prend fin de '
        'lui-même à la date que vous fixez. Vous pouvez le reprendre à tout moment ; chaque '
        'décision reste dans l’historique.',
        'A firm only enters your space if you grant it, and its access ends by itself on the '
        'date you set. You can revoke it at any time; every decision stays in the history.',
        'Un despacho solo entra si usted lo concede, y su acceso termina por sí mismo en la '
        'fecha que fije. Puede retirarlo en cualquier momento; cada decisión queda en el '
        'historial.',
        'لا يدخل أي مكتب إلى بياناتك إلا إذا منحته ذلك، وينتهي وصوله من تلقاء نفسه في التاريخ '
        'الذي تحدده. ويمكنك سحبه في أي وقت؛ ويبقى كل قرار في السجل.',
        '事務所が入れるのは、あなたが許可した場合だけです。アクセスはあなたが定めた日に自動で終わります。いつでも取り消せ、すべての判断は履歴に残ります。',
        '只有在您授权后，事务所才能进入；访问会在您设定的日期自动结束。您可随时收回，每项决定都会留在历史记录中。'),
    'partner.access.pending': (
        'En attente de votre décision', 'Awaiting your decision',
        'Pendiente de su decisión', 'في انتظار قرارك',
        'あなたの判断待ち', '等待您的决定'),
    'partner.access.grant-action': (
        'Accorder un accès', 'Grant access', 'Conceder un acceso', 'منح وصول',
        'アクセスを許可', '授予访问'),
    'partner.access.decline-action': (
        'Refuser', 'Decline', 'Rechazar', 'رفض', '却下', '拒绝'),
    'partner.access.scope-label': (
        'Ce que le cabinet pourra faire', 'What the firm will be able to do',
        'Lo que podrá hacer el despacho', 'ما سيتمكن المكتب من فعله',
        '事務所にできること', '事务所可以做什么'),
    'partner.access.ends-on': (
        'Terme de l’accès', 'Access end date', 'Fin del acceso', 'تاريخ انتهاء الوصول',
        'アクセス終了日', '访问终止日期'),
    'partner.access.purpose': (
        'Objet de la mission', 'Purpose of the engagement', 'Objeto de la misión',
        'موضوع المهمة', '案件の目的', '任务目的'),
    'partner.access.purpose-placeholder': (
        'Ex. : préparation de l’audit ISO 9001', 'E.g. preparing the ISO 9001 audit',
        'Ej.: preparación de la auditoría ISO 9001', 'مثال: التحضير لتدقيق ISO 9001',
        '例：ISO 9001 監査の準備', '例：为 ISO 9001 审核做准备'),
    'partner.access.term-help': (
        'Un accès sans terme est un accès oublié : la date est obligatoire, et celle proposée '
        'est à trois mois.',
        'An access with no end date is a forgotten access: the date is mandatory, and the one '
        'suggested is three months out.',
        'Un acceso sin fin es un acceso olvidado: la fecha es obligatoria, y la propuesta está '
        'a tres meses.',
        'الوصول بلا أجل هو وصول منسيّ: التاريخ إلزامي، والمقترح بعد ثلاثة أشهر.',
        '期限のないアクセスは忘れられたアクセスです。日付は必須で、既定値は3か月後です。',
        '没有期限的访问就是被遗忘的访问：日期为必填，默认建议为三个月后。'),
    'partner.access.grant-confirm': (
        'Accorder', 'Grant', 'Conceder', 'منح', '許可する', '授予'),
    'partner.access.granted': (
        'Accès accordé, et il prendra fin de lui-même à son terme.',
        'Access granted, and it will end by itself on its end date.',
        'Acceso concedido; terminará por sí mismo en su fecha de fin.',
        'تم منح الوصول، وسينتهي من تلقاء نفسه في أجله.',
        'アクセスを許可しました。期限日に自動で終了します。',
        '已授予访问，将在终止日期自动结束。'),
    'partner.access.grant-failed': (
        'L’accès n’a pas pu être accordé.', 'Access could not be granted.',
        'No se ha podido conceder el acceso.', 'تعذّر منح الوصول.',
        'アクセスを許可できませんでした。', '无法授予访问。'),
    'partner.access.declined': (
        'Proposition refusée. Aucun accès n’a été ouvert.',
        'Proposal declined. No access was opened.',
        'Propuesta rechazada. No se ha abierto ningún acceso.',
        'تم رفض الاقتراح. لم يُفتح أي وصول.',
        '提案を却下しました。アクセスは開かれていません。',
        '已拒绝该提议。没有开放任何访问。'),
    'partner.access.decline-failed': (
        'La proposition n’a pas pu être refusée.', 'The proposal could not be declined.',
        'No se ha podido rechazar la propuesta.', 'تعذّر رفض الاقتراح.',
        '提案を却下できませんでした。', '无法拒绝该提议。'),
    'partner.access.granted-section': (
        'Accès accordés', 'Granted accesses', 'Accesos concedidos', 'الأوصال الممنوحة',
        '許可済みのアクセス', '已授予的访问'),
    'partner.access.from': ('Du', 'From', 'Del', 'من', '開始', '自'),
    'partner.access.to': ('au', 'to', 'al', 'إلى', '終了', '至'),
    'partner.access.revoke-action': (
        'Reprendre', 'Revoke', 'Retirar', 'سحب', '取り消す', '收回'),
    'partner.access.revoke-title': (
        'Reprendre cet accès ?', 'Revoke this access?', '¿Retirar este acceso?',
        'سحب هذا الوصول؟', 'このアクセスを取り消しますか？', '收回此访问？'),
    'partner.access.revoke-message': (
        'Le cabinet perdra l’accès immédiatement. La décision reste dans l’historique.',
        'The firm will lose access immediately. The decision stays in the history.',
        'El despacho perderá el acceso de inmediato. La decisión queda en el historial.',
        'سيفقد المكتب الوصول فورًا. ويبقى القرار في السجل.',
        '事務所は直ちにアクセスを失います。判断は履歴に残ります。',
        '事务所将立即失去访问权限。该决定会留在历史记录中。'),
    'partner.access.revoked': (
        'Accès repris.', 'Access revoked.', 'Acceso retirado.', 'تم سحب الوصول.',
        'アクセスを取り消しました。', '已收回访问。'),
    'partner.access.revoke-failed': (
        'L’accès n’a pas pu être repris.', 'Access could not be revoked.',
        'No se ha podido retirar el acceso.', 'تعذّر سحب الوصول.',
        'アクセスを取り消せませんでした。', '无法收回访问。'),
    'partner.access.history': (
        'Décisions passées', 'Past decisions', 'Decisiones pasadas', 'القرارات السابقة',
        '過去の判断', '过往决定'),
    'partner.access.load-failed': (
        'La liste des cabinets n’a pas pu être chargée.',
        'The list of firms could not be loaded.',
        'No se ha podido cargar la lista de despachos.',
        'تعذّر تحميل قائمة المكاتب.',
        '事務所の一覧を読み込めませんでした。', '无法加载事务所列表。'),
    'partner.access.empty': (
        'Aucun cabinet n’a été proposé à votre organisation. Un cabinet partenaire est déclaré '
        'par l’éditeur de la plateforme ; vous décidez ensuite s’il entre chez vous.',
        'No firm has been proposed to your organisation. A partner firm is declared by the '
        'platform publisher; you then decide whether it enters your space.',
        'No se ha propuesto ningún despacho a su organización. El editor de la plataforma '
        'declara un despacho asociado; después usted decide si entra.',
        'لم يُقترح أي مكتب على مؤسستك. يُسجّل المكتب الشريك من قبل ناشر المنصّة؛ ثم تقرّر أنت '
        'ما إذا كان يدخل إليك.',
        'あなたの組織にはまだ事務所が提案されていません。提携事務所はプラットフォーム提供者が登録し、受け入れるかはあなたが決めます。',
        '尚无事务所被提议给您的组织。合作事务所由平台方登记，之后由您决定是否让其进入。'),

    # --- console de l'éditeur : constituer le portefeuille -----------
    'platform.nav.firms': (
        'Cabinets', 'Partner firms', 'Despachos', 'المكاتب', '事務所', '合作事务所'),
    'platform.nav.firms.hint': (
        'Cabinets partenaires, consultants, clients proposés',
        'Partner firms, consultants, proposed clients',
        'Despachos asociados, consultores, clientes propuestos',
        'المكاتب الشريكة والمستشارون والعملاء المقترحون',
        '提携事務所、コンサルタント、提案したクライアント',
        '合作事务所、顾问、已提议的客户'),
    'nav.admin-firms': (
        'Cabinets', 'Partner firms', 'Despachos', 'المكاتب', '事務所', '合作事务所'),
    'platform.firms.title': (
        'Cabinets partenaires', 'Partner firms', 'Despachos asociados', 'المكاتب الشريكة',
        '提携事務所', '合作事务所'),
    'platform.firms.subtitle': (
        'Les cabinets qui interviennent chez vos clients, leurs consultants, et les clients '
        'qui leur ont été proposés.',
        'The firms that work at your clients’ sites, their consultants, and the clients they '
        'have been proposed.',
        'Los despachos que intervienen en sus clientes, sus consultores y los clientes que se '
        'les han propuesto.',
        'المكاتب التي تتدخل لدى عملائك، ومستشاروها، والعملاء الذين اقتُرحوا لها.',
        'クライアント先で稼働する事務所、その コンサルタント、提案したクライアント。',
        '在您客户处开展工作的事务所、其顾问，以及已提议给他们的客户。'),
    'platform.firms.reload': (
        'Recharger', 'Reload', 'Recargar', 'إعادة التحميل', '再読み込み', '重新加载'),
    'platform.firms.new': (
        'Nouveau cabinet', 'New firm', 'Nuevo despacho', 'مكتب جديد',
        '新しい事務所', '新建事务所'),
    'platform.firms.scope': (
        'Vous déclarez un cabinet et lui proposez des clients. Vous n’ouvrez aucun accès : '
        'chaque client décide lui-même d’accorder, et fixe le terme.',
        'You declare a firm and propose clients to it. You open no access: each client decides '
        'to grant it, and sets the end date.',
        'Usted declara un despacho y le propone clientes. No abre ningún acceso: cada cliente '
        'decide concederlo y fija el plazo.',
        'أنت تسجّل المكتب وتقترح له عملاء. ولا تفتح أي وصول: كل عميل يقرّر المنح بنفسه ويحدّد الأجل.',
        '事務所を登録し、クライアントを提案します。アクセスは開きません。許可するかと期限は各クライアントが決めます。',
        '您登记事务所并向其提议客户。您不开放任何访问：是否授予以及期限由每个客户自行决定。'),
    'platform.firms.name': (
        'Raison sociale', 'Legal name', 'Razón social', 'الاسم القانوني',
        '法人名', '法定名称'),
    'platform.firms.slug': ('Repère', 'Handle', 'Identificador', 'المعرّف', '識別名', '标识'),
    'platform.firms.slug-placeholder': (
        'Ex. : qualiconseil', 'E.g. qualiconseil', 'Ej.: qualiconseil',
        'مثال: qualiconseil', '例：qualiconseil', '例：qualiconseil'),
    'platform.firms.slug-hint': (
        'Minuscules, chiffres et tirets. Définitif.',
        'Lower case, digits and hyphens. Permanent.',
        'Minúsculas, cifras y guiones. Definitivo.',
        'حروف صغيرة وأرقام وشرطات. نهائي.',
        '小文字・数字・ハイフン。変更できません。',
        '小写字母、数字和连字符。不可更改。'),
    'platform.firms.registration': (
        'Numéro d’immatriculation', 'Registration number', 'Número de registro',
        'رقم التسجيل', '登録番号', '注册号'),
    'platform.firms.contact-email': (
        'Courriel de contact', 'Contact email', 'Correo de contacto',
        'بريد التواصل', '連絡先メール', '联系邮箱'),
    'platform.firms.contact-phone': (
        'Téléphone', 'Phone', 'Teléfono', 'الهاتف', '電話', '电话'),
    'platform.firms.create': (
        'Créer le cabinet', 'Create the firm', 'Crear el despacho', 'إنشاء المكتب',
        '事務所を作成', '创建事务所'),
    'platform.firms.create-failed': (
        'Le cabinet n’a pas pu être créé.', 'The firm could not be created.',
        'No se ha podido crear el despacho.', 'تعذّر إنشاء المكتب.',
        '事務所を作成できませんでした。', '无法创建事务所。'),
    'platform.firms.deactivate-title': (
        'Désactiver ce cabinet ?', 'Deactivate this firm?', '¿Desactivar este despacho?',
        'تعطيل هذا المكتب؟', 'この事務所を無効にしますか？', '停用此事务所？'),
    'platform.firms.deactivate-message': (
        'Ses consultants perdront immédiatement tous leurs accès. Les accords des clients sont '
        'conservés et reprendront effet si vous le réactivez.',
        'Its consultants will immediately lose all their accesses. The clients’ agreements are '
        'kept and will take effect again if you reactivate it.',
        'Sus consultores perderán de inmediato todos sus accesos. Los acuerdos de los clientes '
        'se conservan y volverán a surtir efecto si lo reactiva.',
        'سيفقد مستشاروه جميع أوصالهم فورًا. وتُحفظ اتفاقات العملاء وتعود إلى السريان إذا أعدت تنشيطه.',
        '所属コンサルタントはすべてのアクセスを直ちに失います。クライアントの合意は保持され、再有効化すれば再び効力を持ちます。',
        '其顾问将立即失去全部访问权限。客户的协议会被保留，若您重新启用则重新生效。'),
    'platform.firms.deactivate-action': (
        'Désactiver', 'Deactivate', 'Desactivar', 'تعطيل', '無効にする', '停用'),
    'platform.firms.activate-action': (
        'Réactiver', 'Reactivate', 'Reactivar', 'إعادة التنشيط', '再有効化', '重新启用'),
    'platform.firms.inactive': (
        'Inactif', 'Inactive', 'Inactivo', 'غير نشط', '無効', '已停用'),
    'platform.firms.toggle-failed': (
        'L’état du cabinet n’a pas pu être changé.', 'The firm’s state could not be changed.',
        'No se ha podido cambiar el estado del despacho.', 'تعذّر تغيير حالة المكتب.',
        '事務所の状態を変更できませんでした。', '无法更改事务所状态。'),
    'platform.firms.consultant-count': (
        '{$INTERPOLATION} consultants', '{$INTERPOLATION} consultants',
        '{$INTERPOLATION} consultores', '{$INTERPOLATION} مستشارين',
        'コンサルタント {$INTERPOLATION} 名', '{$INTERPOLATION} 名顾问'),
    'platform.firms.granted-count': (
        '{$INTERPOLATION} missions', '{$INTERPOLATION} engagements',
        '{$INTERPOLATION} misiones', '{$INTERPOLATION} مهام',
        '案件 {$INTERPOLATION} 件', '{$INTERPOLATION} 项任务'),
    'platform.firms.consultants': (
        'Consultants', 'Consultants', 'Consultores', 'المستشارون',
        'コンサルタント', '顾问'),
    'platform.firms.linked': (
        'Rattaché', 'Linked', 'Vinculado', 'مرتبط', '紐づけ済み', '已关联'),
    'platform.firms.pending-link': (
        'Jamais connecté', 'Never signed in', 'Nunca conectado', 'لم يسجّل الدخول قطّ',
        '未ログイン', '从未登录'),
    'platform.firms.suspend': (
        'Suspendre', 'Suspend', 'Suspender', 'تعليق', '停止', '暂停'),
    'platform.firms.restore': (
        'Rétablir', 'Restore', 'Restablecer', 'إعادة', '復帰', '恢复'),
    'platform.firms.no-consultant': (
        'Aucun consultant inscrit dans ce cabinet.', 'No consultant registered in this firm.',
        'Ningún consultor registrado en este despacho.', 'لا يوجد مستشار مسجّل في هذا المكتب.',
        'この事務所にはコンサルタントが登録されていません。', '此事务所尚未登记顾问。'),
    'platform.firms.consultant-name': (
        'Nom du consultant', 'Consultant’s name', 'Nombre del consultor',
        'اسم المستشار', 'コンサルタント名', '顾问姓名'),
    'platform.firms.consultant-email': (
        'Courriel', 'Email', 'Correo electrónico', 'البريد الإلكتروني',
        'メール', '邮箱'),
    'platform.firms.consultant-email-hint': (
        'Celui de son compte : c’est par là que le rattachement se fait.',
        'The one on their account: that is how the link is made.',
        'El de su cuenta: es así como se realiza la vinculación.',
        'بريد حسابه: فهو ما يتم به الربط.',
        '本人のアカウントのメール。これで紐づけが行われます。',
        '其账号所用邮箱：关联正是通过它完成的。'),
    'platform.firms.add-consultant': (
        'Inscrire', 'Register', 'Registrar', 'تسجيل', '登録', '登记'),
    'platform.firms.consultant-added': (
        'Consultant inscrit. Son accès s’ouvrira à sa première connexion, par ce courriel.',
        'Consultant registered. Their access will open at their first sign-in, through this '
        'email address.',
        'Consultor registrado. Su acceso se abrirá en su primera conexión, mediante este correo.',
        'تم تسجيل المستشار. سيُفتح وصوله عند أول تسجيل دخول له، عبر هذا البريد.',
        'コンサルタントを登録しました。このメールアドレスで初回ログインした時点でアクセスが開きます。',
        '已登记顾问。其访问将在首次登录时通过该邮箱开通。'),
    'platform.firms.consultant-failed': (
        'Le consultant n’a pas pu être inscrit.', 'The consultant could not be registered.',
        'No se ha podido registrar al consultor.', 'تعذّر تسجيل المستشار.',
        'コンサルタントを登録できませんでした。', '无法登记该顾问。'),
    'platform.firms.consultant-toggle-failed': (
        'L’état du consultant n’a pas pu être changé.',
        'The consultant’s state could not be changed.',
        'No se ha podido cambiar el estado del consultor.',
        'تعذّر تغيير حالة المستشار.',
        'コンサルタントの状態を変更できませんでした。', '无法更改该顾问的状态。'),
    'platform.firms.consultants-failed': (
        'Les consultants n’ont pas pu être chargés.', 'The consultants could not be loaded.',
        'No se han podido cargar los consultores.', 'تعذّر تحميل المستشارين.',
        'コンサルタントを読み込めませんでした。', '无法加载顾问列表。'),
    'platform.firms.portfolio': (
        'Portefeuille', 'Portfolio', 'Cartera', 'المحفظة', 'ポートフォリオ', '客户组合'),
    'platform.firms.portfolio-failed': (
        'Le portefeuille n’a pas pu être chargé.', 'The portfolio could not be loaded.',
        'No se ha podido cargar la cartera.', 'تعذّر تحميل المحفظة.',
        'ポートフォリオを読み込めませんでした。', '无法加载客户组合。'),
    'platform.firms.no-mission': (
        'Aucun client proposé à ce cabinet.', 'No client proposed to this firm.',
        'Ningún cliente propuesto a este despacho.', 'لم يُقترح أي عميل لهذا المكتب.',
        'この事務所に提案したクライアントはありません。', '尚未向此事务所提议任何客户。'),
    'platform.firms.client': ('Client', 'Client', 'Cliente', 'العميل', 'クライアント', '客户'),
    'platform.firms.proposed-scope': (
        'Portée demandée', 'Requested scope', 'Alcance solicitado', 'النطاق المطلوب',
        '要求する範囲', '申请的范围'),
    'platform.firms.proposed-purpose': (
        'Objet de la mission', 'Purpose of the engagement', 'Objeto de la misión',
        'موضوع المهمة', '案件の目的', '任务目的'),
    'platform.firms.propose': (
        'Proposer', 'Propose', 'Proponer', 'اقتراح', '提案する', '提议'),
    'platform.firms.proposed': (
        'Client proposé. Aucun accès n’est ouvert : son administrateur décide.',
        'Client proposed. No access is open: their administrator decides.',
        'Cliente propuesto. No se abre ningún acceso: su administrador decide.',
        'تم اقتراح العميل. لا يُفتح أي وصول: مسؤوله هو من يقرّر.',
        'クライアントを提案しました。アクセスは開きません。判断はその管理者が行います。',
        '已提议该客户。未开放任何访问：由其管理员决定。'),
    'platform.firms.propose-failed': (
        'Le client n’a pas pu être proposé.', 'The client could not be proposed.',
        'No se ha podido proponer el cliente.', 'تعذّر اقتراح العميل.',
        'クライアントを提案できませんでした。', '无法提议该客户。'),
    'platform.firms.awaiting': (
        'En attente du client', 'Awaiting the client', 'Pendiente del cliente',
        'في انتظار العميل', 'クライアントの判断待ち', '等待客户决定'),
    'platform.firms.load-failed': (
        'La liste des cabinets n’a pas pu être chargée.',
        'The list of firms could not be loaded.',
        'No se ha podido cargar la lista de despachos.',
        'تعذّر تحميل قائمة المكاتب.',
        '事務所の一覧を読み込めませんでした。', '无法加载事务所列表。'),
    'platform.firms.empty': (
        'Aucun cabinet partenaire n’est déclaré. Un cabinet regroupe des consultants externes '
        'à qui vos clients peuvent ouvrir un accès daté.',
        'No partner firm is declared. A firm groups external consultants to whom your clients '
        'can open a dated access.',
        'No hay ningún despacho asociado declarado. Un despacho agrupa consultores externos a '
        'los que sus clientes pueden abrir un acceso con fecha.',
        'لا يوجد أي مكتب شريك مسجّل. يجمع المكتب مستشارين خارجيين يمكن لعملائك فتح وصول مؤقّت لهم.',
        '提携事務所は未登録です。事務所は外部コンサルタントをまとめる単位で、クライアントは期限付きのアクセスを開けます。',
        '尚未登记合作事务所。事务所汇集外部顾问，您的客户可为其开放带期限的访问。'),
}
