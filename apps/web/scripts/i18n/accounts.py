# -*- coding: utf-8 -*-
"""Libellés des comptes (ADR 0079) : création d'un client par l'éditeur,
invitation d'un membre, remise des identifiants.

Placeholders : `${x}:nom:` côté TS s'écrit {$nom} ; `{{ x // i18n(ph="nom") }}`
côté gabarit s'écrit {$NOM}.
"""

TRANSLATIONS = {
    'nav.admin-clients': ('Clients', 'Customers', 'Clientes', 'العملاء', '顧客', '客户'),

    # ---------- remise des identifiants ----------
    'admin.credential.title': (
        'Transmettez ces identifiants', 'Pass on these credentials', 'Transmita estas credenciales',
        'أرسل بيانات الدخول هذه', 'このログイン情報を伝えてください', '请转交以下登录信息'),
    'admin.credential.help': (
        "Le mot de passe provisoire n'apparaîtra plus. Il devra être changé à la première connexion.",
        'The temporary password will not be shown again. It must be changed at first sign-in.',
        'La contraseña provisional no volverá a mostrarse. Deberá cambiarse en el primer inicio de sesión.',
        'لن تظهر كلمة المرور المؤقتة مرة أخرى. ويجب تغييرها عند أول تسجيل دخول.',
        '仮パスワードは再表示されません。初回ログイン時に変更が必要です。',
        '临时密码不会再次显示，首次登录时必须更改。'),
    'admin.credential.login': ('Identifiant', 'Login', 'Usuario', 'اسم الدخول', 'ログインID', '登录名'),
    'admin.credential.password': ('Mot de passe provisoire', 'Temporary password', 'Contraseña provisional', 'كلمة المرور المؤقتة', '仮パスワード', '临时密码'),
    'admin.credential.copy': ('Copier', 'Copy', 'Copiar', 'نسخ', 'コピー', '复制'),
    'admin.credential.copied': ('Copié', 'Copied', 'Copiado', 'تم النسخ', 'コピーしました', '已复制'),
    'admin.credential.done': ("C'est transmis", 'Done, passed on', 'Ya está transmitido', 'تم الإرسال', '伝えました', '已转交'),
    'admin.credential.sent-title': ('Invitation envoyée', 'Invitation sent', 'Invitación enviada', 'تم إرسال الدعوة', '招待を送信しました', '邀请已发送'),
    'admin.credential.sent-help': (
        '{$EMAIL} reçoit un lien pour choisir son mot de passe.', '{$EMAIL} receives a link to choose a password.',
        '{$EMAIL} recibe un enlace para elegir su contraseña.', 'يتلقى {$EMAIL} رابطًا لاختيار كلمة المرور.',
        '{$EMAIL} にパスワード設定用のリンクが届きます。', '{$EMAIL} 将收到设置密码的链接。'),

    # ---------- invitation ----------
    'admin.team.invite': ('Inviter un membre', 'Invite a member', 'Invitar a un miembro', 'دعوة عضو', 'メンバーを招待', '邀请成员'),
    'admin.team.invite-title': ('Nouveau membre', 'New member', 'Nuevo miembro', 'عضو جديد', '新しいメンバー', '新成员'),
    'admin.team.invite-email': ('Adresse e-mail', 'Email address', 'Correo electrónico', 'البريد الإلكتروني', 'メールアドレス', '电子邮箱'),
    'admin.team.invite-email-invalid': (
        'Une adresse e-mail valide est attendue.', 'A valid email address is expected.',
        'Se espera un correo electrónico válido.', 'يلزم بريد إلكتروني صالح.', '有効なメールアドレスを入力してください。', '请输入有效的电子邮箱。'),
    'admin.team.invite-first-name': ('Prénom (facultatif)', 'First name (optional)', 'Nombre (opcional)', 'الاسم الأول (اختياري)', '名（任意）', '名（可选）'),
    'admin.team.invite-last-name': ('Nom (facultatif)', 'Last name (optional)', 'Apellido (opcional)', 'اسم العائلة (اختياري)', '姓（任意）', '姓（可选）'),
    'admin.team.invite-roles': ('Rôles de son compte', 'Account roles', 'Roles de su cuenta', 'أدوار حسابه', 'アカウントのロール', '账户角色'),
    'admin.team.invite-send': ('Créer son compte', 'Create their account', 'Crear su cuenta', 'إنشاء حسابه', 'アカウントを作成', '创建其账户'),
    'admin.team.invite-failed': ("L'invitation a échoué.", 'The invitation failed.', 'La invitación ha fallado.', 'فشلت الدعوة.', '招待に失敗しました。', '邀请失败。'),

    # ---------- clients ----------
    'admin.clients.title': ('Clients', 'Customers', 'Clientes', 'العملاء', '顧客', '客户'),
    'admin.clients.subtitle': (
        "Chaque organisation qui utilise QualitOS : son offre, ses modules, son administrateur.",
        'Every organisation using QualitOS: its plan, its modules, its administrator.',
        'Cada organización que usa QualitOS: su oferta, sus módulos, su administrador.',
        'كل مؤسسة تستخدم QualitOS: عرضها ووحداتها ومسؤولها.',
        'QualitOS を利用する各組織：プラン、モジュール、管理者。',
        '使用 QualitOS 的每个组织：其套餐、模块和管理员。'),
    'admin.clients.new': ('Nouveau client', 'New customer', 'Nuevo cliente', 'عميل جديد', '新しい顧客', '新建客户'),
    'admin.clients.wizard-title': ('Nouveau client', 'New customer', 'Nuevo cliente', 'عميل جديد', '新しい顧客', '新建客户'),
    'admin.clients.step-company': ('Entreprise', 'Company', 'Empresa', 'المؤسسة', '組織', '企业'),
    'admin.clients.step-modules': ('Modules', 'Modules', 'Módulos', 'الوحدات', 'モジュール', '模块'),
    'admin.clients.step-admin': ('Administrateur', 'Administrator', 'Administrador', 'المسؤول', '管理者', '管理员'),
    'admin.clients.name': ("Nom de l'organisation", 'Organisation name', 'Nombre de la organización', 'اسم المؤسسة', '組織名', '组织名称'),
    'admin.clients.name-placeholder': ('Hôpital Saint-Jean', 'Saint John Hospital', 'Hospital San Juan', 'مستشفى سانت جان', '聖ヨハネ病院', '圣约翰医院'),
    'admin.clients.slug': ('Identifiant', 'Identifier', 'Identificador', 'المعرّف', '識別子', '标识符'),
    'admin.clients.slug-hint': (
        'Minuscules, chiffres et tirets ; il ne change plus.', 'Lowercase, digits and hyphens; it cannot change later.',
        'Minúsculas, cifras y guiones; no cambia después.', 'أحرف صغيرة وأرقام وشرطات؛ لا يتغيّر بعد ذلك.',
        '小文字、数字、ハイフン。後から変更できません。', '小写字母、数字和连字符；之后不可更改。'),
    'admin.clients.slug-invalid': (
        '3 à 63 caractères : minuscules, chiffres et tirets.', '3 to 63 characters: lowercase, digits and hyphens.',
        'De 3 a 63 caracteres: minúsculas, cifras y guiones.', 'من 3 إلى 63 حرفًا: أحرف صغيرة وأرقام وشرطات.',
        '3〜63文字：小文字、数字、ハイフン。', '3 至 63 个字符：小写字母、数字和连字符。'),
    'admin.clients.plan': ('Offre', 'Plan', 'Oferta', 'العرض', 'プラン', '套餐'),
    'admin.clients.next': ('Continuer', 'Continue', 'Continuar', 'متابعة', '次へ', '继续'),
    'admin.clients.back': ('Retour', 'Back', 'Volver', 'رجوع', '戻る', '返回'),
    'admin.clients.modules-help': (
        "Cochez ce que l'organisation utilisera. Ce dont un module a besoin se coche avec lui ; le socle est inclus d'office.",
        'Tick what the organisation will use. What a module needs is ticked with it; the core is always included.',
        'Marque lo que usará la organización. Lo que necesita un módulo se marca con él; el núcleo se incluye siempre.',
        'حدّد ما ستستخدمه المؤسسة. ما تحتاجه الوحدة يُحدَّد معها؛ والأساس مضمّن دائمًا.',
        '組織が使うものをチェックします。モジュールに必要なものは一緒にチェックされ、基盤は常に含まれます。',
        '勾选组织将使用的模块。模块所需的依赖会一并勾选；基础模块始终包含。'),
    'admin.clients.catalog-failed': (
        'Le catalogue des modules ne répond pas : le client peut être créé sans module, à compléter ensuite.',
        'The module catalogue is not responding: the customer can be created without modules and completed later.',
        'El catálogo de módulos no responde: el cliente puede crearse sin módulos y completarse después.',
        'كتالوج الوحدات لا يستجيب: يمكن إنشاء العميل بلا وحدات واستكماله لاحقًا.',
        'モジュールカタログが応答しません。モジュールなしで顧客を作成し、後で追加できます。',
        '模块目录无响应：可先创建不含模块的客户，稍后补充。'),
    'admin.clients.core': ('Socle', 'Core', 'Núcleo', 'الأساس', '基盤', '基础'),
    'admin.clients.required-by': ('Requis par {$modules}', 'Required by {$modules}', 'Requerido por {$modules}', 'مطلوب من {$modules}', '{$modules} が必要とします', '{$modules} 需要此模块'),
    'admin.clients.admin-help': (
        'Son compte est créé maintenant ; il invitera ensuite son équipe et réglera ses droits.',
        'Their account is created now; they will then invite their team and set its permissions.',
        'Su cuenta se crea ahora; después invitará a su equipo y ajustará sus permisos.',
        'يُنشأ حسابه الآن؛ ثم يدعو فريقه ويضبط صلاحياته.',
        'アカウントは今作成されます。その後、チームを招待し権限を設定します。',
        '其账户现在创建；之后由其邀请团队并设置权限。'),
    'admin.clients.modules-count': ('{$COUNT} module(s) choisi(s)', '{$COUNT} module(s) selected', '{$COUNT} módulo(s) elegido(s)', '{$COUNT} وحدة مختارة', '{$COUNT} 件のモジュールを選択', '已选 {$COUNT} 个模块'),
    'admin.clients.create': ('Créer le client', 'Create the customer', 'Crear el cliente', 'إنشاء العميل', '顧客を作成', '创建客户'),
    'admin.clients.create-failed': (
        'La création du client a échoué.', 'Creating the customer failed.', 'La creación del cliente ha fallado.',
        'فشل إنشاء العميل.', '顧客の作成に失敗しました。', '客户创建失败。'),
    'admin.clients.created': ('Client créé :', 'Customer created:', 'Cliente creado:', 'تم إنشاء العميل:', '顧客を作成しました：', '客户已创建：'),
    'admin.clients.modules-failed': (
        "Certains modules n'ont pas été ouverts : reprenez-les depuis la console des modules du client.",
        'Some modules were not opened: retry them from the customer modules console.',
        'Algunos módulos no se abrieron: reinténtelos desde la consola de módulos del cliente.',
        'لم تُفتح بعض الوحدات: أعد المحاولة من وحدة تحكم وحدات العميل.',
        '一部のモジュールが開けませんでした。顧客のモジュール画面から再試行してください。',
        '部分模块未开通：请在客户的模块控制台中重试。'),
    'admin.clients.load-failed': (
        "La liste des clients n'a pas pu être chargée.", 'The customer list could not be loaded.',
        'No se pudo cargar la lista de clientes.', 'تعذّر تحميل قائمة العملاء.', '顧客一覧を読み込めませんでした。', '无法加载客户列表。'),
    'admin.clients.inactive': ('Désactivé', 'Deactivated', 'Desactivado', 'معطّل', '無効', '已停用'),
    'admin.clients.empty': (
        "Aucun client pour l'instant : créez le premier.", 'No customer yet: create the first one.',
        'Ningún cliente por ahora: cree el primero.', 'لا يوجد عميل بعد: أنشئ الأول.',
        'まだ顧客はいません。最初の顧客を作成しましょう。', '暂无客户：创建第一个吧。'),
}
