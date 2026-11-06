# -*- coding: utf-8 -*-
"""Table i18n - identité, marque et thème d'un client, posés à sa création.

Ces libellés sont ceux de la console de l'éditeur, mais ils seront repris tels
quels par la console du client quand il reprendra la main sur son habillage :
une même notion, un même mot.
"""

TRANSLATIONS = {

    # --- étape ---------------------------------------------------------
    'admin.clients.step-identity': (
        'Identité', 'Identity', 'Identidad', 'الهوية', 'アイデンティティ', '标识'),
    'admin.clients.identity-help': (
        "Tout est facultatif : le client complétera depuis sa propre console. Ce qui est "
        "posé ici s'affiche dès sa première connexion.",
        'Everything is optional: the client will complete it from their own console. What is '
        'set here appears at their very first sign-in.',
        'Todo es opcional: el cliente lo completará desde su propia consola. Lo que se '
        'establece aquí aparece desde su primera conexión.',
        'كل شيء اختياري: سيكملها العميل من وحدة التحكم الخاصة به. ما يُحدَّد هنا يظهر منذ أول تسجيل دخول.',
        'すべて任意です。顧客は自身のコンソールで補完できます。ここで設定した内容は初回ログインから表示されます。',
        '一切均为可选：客户可在自己的控制台中补全。此处设置的内容在其首次登录时即可见。'),

    # --- informations légales ------------------------------------------
    'admin.clients.slogan': ('Signature', 'Tagline', 'Lema', 'الشعار', 'キャッチフレーズ', '标语'),
    'admin.clients.slogan-hint': (
        'Une ligne, sous le nom.', 'One line, under the name.', 'Una línea, bajo el nombre.',
        'سطر واحد تحت الاسم.', '名称の下に一行。', '名称下方的一行文字。'),
    'admin.clients.legal-name': (
        'Raison sociale', 'Legal name', 'Razón social', 'الاسم القانوني', '商号', '法定名称'),
    'admin.clients.legal-form': (
        'Forme juridique', 'Legal form', 'Forma jurídica', 'الشكل القانوني', '法人形態', '法律形式'),
    'admin.clients.registration': (
        "Numéro d'immatriculation", 'Registration number', 'Número de registro',
        'رقم التسجيل', '登記番号', '注册号'),
    'admin.clients.vat': (
        'Numéro de TVA', 'VAT number', 'Número de IVA', 'رقم ضريبة القيمة المضافة',
        '付加価値税番号', '增值税号'),
    'admin.clients.address': ('Adresse', 'Address', 'Dirección', 'العنوان', '住所', '地址'),
    'admin.clients.postal-code': (
        'Code postal', 'Postal code', 'Código postal', 'الرمز البريدي', '郵便番号', '邮政编码'),
    'admin.clients.city': ('Ville', 'City', 'Ciudad', 'المدينة', '市区町村', '城市'),
    'admin.clients.country': (
        'Pays (ISO, 2 lettres)', 'Country (ISO, 2 letters)', 'País (ISO, 2 letras)',
        'البلد (ISO، حرفان)', '国（ISO 2文字）', '国家（ISO 两位字母）'),
    'admin.clients.country-invalid': (
        'Deux lettres, par exemple FR.', 'Two letters, for example FR.',
        'Dos letras, por ejemplo FR.', 'حرفان، مثل FR.', '2文字、例：FR。', '两位字母，例如 FR。'),
    'admin.clients.contact-email': (
        'Contact — e-mail', 'Contact — email', 'Contacto — correo electrónico',
        'جهة الاتصال — البريد الإلكتروني', '連絡先 — メール', '联系人 — 邮箱'),
    'admin.clients.contact-phone': (
        'Contact — téléphone', 'Contact — phone', 'Contacto — teléfono',
        'جهة الاتصال — الهاتف', '連絡先 — 電話', '联系人 — 电话'),

    # --- marque ---------------------------------------------------------
    'admin.clients.brand': ('Marque', 'Brand', 'Marca', 'العلامة', 'ブランド', '品牌'),
    'admin.clients.primary-color': (
        'Couleur principale', 'Primary colour', 'Color principal', 'اللون الأساسي',
        'メインカラー', '主色'),
    'admin.clients.secondary-color': (
        'Couleur secondaire', 'Secondary colour', 'Color secundario', 'اللون الثانوي',
        'サブカラー', '辅助色'),
    'admin.clients.color-default': (
        'Celle du thème', "The theme's own", 'La del tema', 'لون السمة',
        'テーマの色', '主题色'),
    'admin.clients.color-note': (
        'Une couleur illisible sur le fond du thème est refusée par le serveur : le contraste '
        'prime sur la charte.',
        'A colour unreadable against the theme background is refused by the server: contrast '
        'outranks the brand guide.',
        'Un color ilegible sobre el fondo del tema es rechazado por el servidor: el contraste '
        'prevalece sobre la identidad gráfica.',
        'يرفض الخادم أي لون غير مقروء على خلفية السمة: التباين يسبق دليل العلامة.',
        'テーマの背景で判読できない色はサーバーが拒否します。コントラストはブランド規定に優先します。',
        '在主题背景上不可读的颜色会被服务器拒绝：对比度优先于品牌规范。'),
    'admin.clients.logo': ('Logo', 'Logo', 'Logotipo', 'الشعار المصوَّر', 'ロゴ', '标志'),
    'admin.clients.logo-remove': (
        'Retirer le logo', 'Remove the logo', 'Quitar el logotipo', 'إزالة الشعار',
        'ロゴを削除', '移除标志'),
    'admin.clients.logo-hint': (
        'PNG, JPEG ou WebP, 512 Ko au plus.', 'PNG, JPEG or WebP, 512 KB at most.',
        'PNG, JPEG o WebP, 512 KB como máximo.', 'PNG أو JPEG أو WebP، 512 كيلوبايت كحد أقصى.',
        'PNG、JPEG、WebP、最大512 KB。', 'PNG、JPEG 或 WebP，最大 512 KB。'),
    'admin.clients.logo-type': (
        'Formats acceptés : PNG, JPEG, WebP.', 'Accepted formats: PNG, JPEG, WebP.',
        'Formatos aceptados: PNG, JPEG, WebP.', 'الصيغ المقبولة: PNG وJPEG وWebP.',
        '対応形式：PNG、JPEG、WebP。', '支持的格式：PNG、JPEG、WebP。'),
    'admin.clients.logo-size': (
        'Le logo ne doit pas dépasser 512 Ko.', 'The logo must not exceed 512 KB.',
        'El logotipo no debe superar 512 KB.', 'يجب ألا يتجاوز الشعار 512 كيلوبايت.',
        'ロゴは512 KBを超えてはなりません。', '标志不得超过 512 KB。'),

    # --- thème ----------------------------------------------------------
    'admin.clients.theme': ('Thème', 'Theme', 'Tema', 'السمة', 'テーマ', '主题'),
    'admin.clients.theme-airy': (
        'Clair aéré — par défaut', 'Light and airy — default', 'Claro y amplio — por defecto',
        'فاتح وفسيح — الافتراضي', '明るく余白のある表示 — 既定', '浅色宽松 — 默认'),
    'admin.clients.theme-dense': (
        'Clair dense — poste de travail', 'Light and dense — workstation',
        'Claro y denso — puesto de trabajo', 'فاتح وكثيف — محطة العمل',
        '明るく高密度 — 作業端末', '浅色紧凑 — 工作台'),
    'admin.clients.theme-dark': ('Sombre', 'Dark', 'Oscuro', 'داكن', 'ダーク', '深色'),
    'admin.clients.theme-contrast': (
        'Contraste renforcé — atelier, extérieur', 'High contrast — workshop, outdoors',
        'Contraste reforzado — taller, exterior', 'تباين عالٍ — الورشة والخارج',
        '高コントラスト — 作業場、屋外', '高对比度 — 车间、户外'),

    # --- écarts après la création ---------------------------------------
    'admin.clients.identity-failed': (
        "L'identité n'a pas été enregistrée : reprenez-la depuis la fiche du client.",
        'The identity was not saved: set it again from the client record.',
        'La identidad no se guardó: retómela desde la ficha del cliente.',
        'لم تُحفظ الهوية: أعِد ضبطها من بطاقة العميل.',
        'アイデンティティは保存されませんでした。顧客カードから設定し直してください。',
        '标识未保存：请从客户档案中重新设置。'),
    'admin.clients.logo-failed': (
        "Le logo n'a pas été déposé : reprenez-le depuis la fiche du client.",
        'The logo was not uploaded: upload it again from the client record.',
        'El logotipo no se cargó: vuelva a subirlo desde la ficha del cliente.',
        'لم يُرفع الشعار: أعِد رفعه من بطاقة العميل.',
        'ロゴはアップロードされませんでした。顧客カードから再度アップロードしてください。',
        '标志未上传：请从客户档案中重新上传。'),
}
