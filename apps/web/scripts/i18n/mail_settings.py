# -*- coding: utf-8 -*-
"""Table i18n - messagerie d'une organisation (ADR 0127). id: (fr, en, es, ar, ja, zh).

Deux surfaces partagent ce vocabulaire : l'écran de l'administrateur d'un client
(`mail.settings.*`) et la même fenêtre ouverte sur un cabinet partenaire depuis
la console de l'éditeur (`platform.firms.mail*`). Les libellés sont communs aux
deux, parce qu'un réglage SMTP se dit des mêmes mots quelle que soit
l'organisation qui le saisit.

Le vocabulaire technique — STARTTLS, TLS, SMTP, SPF, DKIM — n'est PAS traduit :
ce sont les mots qui figurent dans la documentation du fournisseur de messagerie
que l'administrateur a sous les yeux, et les traduire l'obligerait à retraduire
pour retrouver le réglage.
"""

TRANSLATIONS = {

    # --- l'écran -----------------------------------------------------
    'mail.settings.title': (
        'Messagerie', 'Email delivery', 'Mensajería', 'البريد الصادر',
        'メール送信設定', '邮件发送'),
    'mail.settings.subtitle': (
        "L'adresse d'où partent les courriels de votre organisation.",
        'The address your organisation sends email from.',
        'La dirección desde la que su organización envía los correos.',
        'العنوان الذي ترسل منه مؤسستك رسائل البريد.',
        '組織のメールの送信元アドレス。',
        '您的组织发送邮件所用的地址。'),
    'mail.settings.load-failed': (
        "La messagerie n'a pas pu être chargée.",
        'Email settings could not be loaded.',
        'No se han podido cargar los ajustes de mensajería.',
        'تعذّر تحميل إعدادات البريد.',
        'メール設定を読み込めませんでした。',
        '无法加载邮件设置。'),

    # --- l'état courant ----------------------------------------------
    'mail.settings.using-platform': (
        "Vos courriels partent aujourd'hui de l'adresse de la plateforme. Elle "
        "n'est pas la vôtre : vos destinataires ne la reconnaissent pas, et elle "
        'ne passe pas vos règles SPF/DKIM.',
        "Your email currently goes out from the platform's address. It is not "
        'yours: your recipients will not recognise it, and it does not satisfy '
        'your SPF/DKIM rules.',
        'Sus correos salen hoy desde la dirección de la plataforma. No es la '
        'suya: sus destinatarios no la reconocen y no cumple sus reglas '
        'SPF/DKIM.',
        'تُرسل رسائلك حاليًا من عنوان المنصّة. وهو ليس عنوانك: لن يتعرّف عليه '
        'المستلمون، ولا يستوفي قواعد SPF/DKIM الخاصة بك.',
        '現在、メールはプラットフォームのアドレスから送信されています。これは自組織の'
        'アドレスではないため、受信者には認識されず、SPF/DKIM の設定も通りません。',
        '目前您的邮件由平台地址发出。该地址并非您的地址：收件人无法识别，也不符合您的 '
        'SPF/DKIM 规则。'),
    'mail.settings.using-own': (
        'Vos courriels partent de {$INTERPOLATION}.',
        'Your email goes out from {$INTERPOLATION}.',
        'Sus correos salen desde {$INTERPOLATION}.',
        'تُرسل رسائلك من {$INTERPOLATION}.',
        'メールは {$INTERPOLATION} から送信されます。',
        '您的邮件由 {$INTERPOLATION} 发出。'),
    'mail.settings.verified': (
        'Vérifiée le {$INTERPOLATION}', 'Verified on {$INTERPOLATION}',
        'Verificada el {$INTERPOLATION}', 'تم التحقّق في {$INTERPOLATION}',
        '{$INTERPOLATION} に確認済み', '已于 {$INTERPOLATION} 验证'),
    'mail.settings.not-verified': (
        "Jamais éprouvée par un envoi d'essai",
        'Never proven by a test message',
        'Nunca comprobada con un envío de prueba',
        'لم يتم إثباتها برسالة اختبار',
        'テスト送信による確認は未実施',
        '尚未通过测试邮件验证'),

    # --- expéditeur ---------------------------------------------------
    'mail.settings.sender': (
        'Expéditeur', 'Sender', 'Remitente', 'المُرسل', '送信者', '发件人'),
    'mail.settings.sender-help': (
        "Ce que vos destinataires liront. L'adresse de réponse peut différer de "
        "l'expéditeur : on émet souvent depuis une adresse de service et l'on "
        'veut les réponses dans une boîte partagée.',
        'What your recipients will read. The reply-to address may differ from '
        'the sender: mail often goes out from a service address while replies '
        'belong in a shared mailbox.',
        'Lo que leerán sus destinatarios. La dirección de respuesta puede '
        'diferir del remitente: se envía desde una dirección de servicio y se '
        'quieren las respuestas en un buzón compartido.',
        'ما سيقرأه المستلمون. قد يختلف عنوان الرد عن المُرسل: يُرسل البريد عادةً '
        'من عنوان خدمة، بينما تُستقبل الردود في صندوق مشترك.',
        '受信者に表示される情報です。返信先アドレスは送信者と異なってかまいません。'
        'サービス用アドレスから送信し、返信は共有メールボックスで受け取る運用が一般的です。',
        '收件人将看到的信息。回复地址可以与发件人不同：通常由服务地址发出，而回复进入'
        '共享邮箱。'),
    'mail.settings.from-address': (
        "Adresse d'expédition", 'From address', 'Dirección de envío',
        'عنوان الإرسال', '送信元アドレス', '发件地址'),
    'mail.settings.from-name': (
        'Nom affiché', 'Display name', 'Nombre mostrado', 'الاسم المعروض',
        '表示名', '显示名称'),
    'mail.settings.reply-to': (
        'Adresse de réponse', 'Reply-to address', 'Dirección de respuesta',
        'عنوان الرد', '返信先アドレス', '回复地址'),

    # --- serveur sortant ----------------------------------------------
    'mail.settings.server': (
        'Serveur sortant', 'Outgoing server', 'Servidor de salida',
        'الخادم الصادر', '送信サーバー', '发送服务器'),
    'mail.settings.server-help': (
        'STARTTLS ouvre en clair puis chiffre (port 587) ; TLS chiffre dès la '
        'connexion (port 465). Ce ne sont pas deux façons de dire la même '
        'chose, et les confondre donne une connexion qui expire sans message '
        'utile.',
        'STARTTLS opens in the clear then encrypts (port 587); TLS encrypts '
        'from the first byte (port 465). These are not two ways of saying the '
        'same thing, and mixing them up gives a connection that times out with '
        'no useful message.',
        'STARTTLS abre en claro y luego cifra (puerto 587); TLS cifra desde la '
        'conexión (puerto 465). No son dos formas de decir lo mismo, y '
        'confundirlos produce una conexión que caduca sin mensaje útil.',
        'يبدأ STARTTLS بالاتصال غير المشفّر ثم يشفّره (المنفذ 587)، أما TLS فيشفّر '
        'من بداية الاتصال (المنفذ 465). ليسا طريقتين للتعبير عن الشيء نفسه، '
        'والخلط بينهما ينتج اتصالًا ينتهي دون رسالة مفيدة.',
        'STARTTLS は平文で接続した後に暗号化します（ポート 587）。TLS は接続時点から'
        '暗号化します（ポート 465）。両者は同じ意味ではなく、取り違えると有用な'
        'メッセージなしに接続がタイムアウトします。',
        'STARTTLS 先以明文连接再加密（端口 587）；TLS 自连接起即加密（端口 465）。'
        '两者并非同一回事，混淆会导致连接超时且没有有用的提示。'),
    'mail.settings.host': (
        'Serveur', 'Server', 'Servidor', 'الخادم', 'サーバー', '服务器'),
    'mail.settings.port': (
        'Port', 'Port', 'Puerto', 'المنفذ', 'ポート', '端口'),
    'mail.settings.security': (
        'Protection', 'Encryption', 'Protección', 'الحماية', '暗号化', '加密方式'),
    'mail.settings.security-none': (
        'Aucune', 'None', 'Ninguna', 'بدون', 'なし', '无'),
    'mail.settings.no-tls-warning': (
        'Sans protection, votre mot de passe circule en clair sur le réseau. À '
        'réserver à un relais interne.',
        'With no encryption your password travels in the clear over the '
        'network. Reserve this for an internal relay.',
        'Sin protección, su contraseña circula en claro por la red. Resérvelo '
        'para un relé interno.',
        'بدون حماية، تنتقل كلمة المرور بنص واضح عبر الشبكة. اقصر ذلك على مُرحّل '
        'داخلي.',
        '暗号化なしではパスワードが平文でネットワークを流れます。内部リレーに限って'
        '使用してください。',
        '未加密时，您的密码将以明文在网络中传输。请仅用于内部中继。'),

    # --- compte --------------------------------------------------------
    'mail.settings.account': (
        'Compte', 'Account', 'Cuenta', 'الحساب', 'アカウント', '账户'),
    'mail.settings.account-help': (
        'Facultatif : un relais interne reconnaît parfois ses clients à leur '
        'adresse IP. Le mot de passe est chiffré et ne vous sera jamais '
        "réaffiché — laisser le champ vide ne l'efface pas.",
        'Optional: an internal relay sometimes recognises its clients by IP '
        'address. The password is encrypted and will never be shown back to you '
        '— leaving the field empty does not erase it.',
        'Opcional: un relé interno reconoce a veces a sus clientes por su '
        'dirección IP. La contraseña se cifra y nunca se le volverá a mostrar; '
        'dejar el campo vacío no la borra.',
        'اختياري: قد يتعرّف المُرحّل الداخلي على عملائه بعنوان IP. تُشفَّر كلمة '
        'المرور ولن تُعرض عليك مرة أخرى — وترك الحقل فارغًا لا يمحوها.',
        '任意です。内部リレーでは IP アドレスでクライアントを識別する場合があります。'
        'パスワードは暗号化され、再表示されることはありません。空欄のままにしても'
        '削除はされません。',
        '可选：内部中继有时按 IP 地址识别客户端。密码经过加密且绝不会再次显示——'
        '留空并不会将其删除。'),
    'mail.settings.username': (
        'Identifiant', 'Username', 'Usuario', 'المُعرّف', 'ユーザー名', '用户名'),
    'mail.settings.password': (
        'Mot de passe', 'Password', 'Contraseña', 'كلمة المرور',
        'パスワード', '密码'),
    'mail.settings.password-replace': (
        'Nouveau mot de passe', 'New password', 'Nueva contraseña',
        'كلمة مرور جديدة', '新しいパスワード', '新密码'),
    'mail.settings.password-kept': (
        'Un mot de passe est enregistré. Laissez vide pour le conserver.',
        'A password is stored. Leave empty to keep it.',
        'Hay una contraseña guardada. Déjelo vacío para conservarla.',
        'توجد كلمة مرور محفوظة. اتركه فارغًا للإبقاء عليها.',
        'パスワードは保存済みです。そのまま残す場合は空欄にしてください。',
        '已保存密码。留空即保留原密码。'),
    'mail.settings.clear-password': (
        'Effacer le mot de passe', 'Clear the password',
        'Borrar la contraseña', 'محو كلمة المرور',
        'パスワードを削除', '清除密码'),
    'mail.settings.password-cleared': (
        'Mot de passe effacé.', 'Password cleared.', 'Contraseña borrada.',
        'تم محو كلمة المرور.', 'パスワードを削除しました。', '密码已清除。'),

    # --- mise en service -----------------------------------------------
    'mail.settings.activation': (
        'Mise en service', 'Going live', 'Puesta en servicio', 'التشغيل',
        '運用開始', '启用'),
    'mail.settings.activation-help': (
        "Tant que la messagerie n'est pas activée, vos courriels continuent de "
        "partir de l'adresse de la plateforme. Éprouvez-la d'abord : "
        "enregistrer un réglage n'est pas l'avoir vérifié.",
        'Until this mailbox is switched on, your email keeps going out from the '
        "platform's address. Prove it first: saving a setting is not having "
        'verified it.',
        'Mientras no se active, sus correos seguirán saliendo desde la '
        'dirección de la plataforma. Compruébela primero: guardar un ajuste no '
        'es haberlo verificado.',
        'ما لم يتم تشغيل هذا البريد، ستظل رسائلك تُرسل من عنوان المنصّة. أثبِت '
        'صحّته أولًا: فحفظ الإعداد ليس تحقّقًا منه.',
        '有効にするまで、メールはプラットフォームのアドレスから送信され続けます。'
        'まずテスト送信で確認してください。保存したことは確認したことではありません。',
        '在启用之前，您的邮件仍由平台地址发出。请先行验证：保存设置并不等于已验证。'),
    'mail.settings.enable': (
        'Envoyer les courriels depuis cette messagerie',
        'Send email from this mailbox',
        'Enviar los correos desde esta mensajería',
        'إرسال الرسائل من هذا البريد',
        'このメールボックスから送信する',
        '使用此邮箱发送邮件'),

    # --- actions --------------------------------------------------------
    'mail.settings.saved': (
        'Messagerie enregistrée.', 'Email settings saved.',
        'Mensajería guardada.', 'تم حفظ إعدادات البريد.',
        'メール設定を保存しました。', '邮件设置已保存。'),
    'mail.settings.save-failed': (
        "La messagerie n'a pas pu être enregistrée.",
        'Email settings could not be saved.',
        'No se han podido guardar los ajustes de mensajería.',
        'تعذّر حفظ إعدادات البريد.',
        'メール設定を保存できませんでした。',
        '无法保存邮件设置。'),
    'mail.settings.remove': (
        'Retirer la messagerie', 'Remove this mailbox',
        'Retirar la mensajería', 'إزالة هذا البريد',
        'このメールボックスを削除', '移除此邮箱'),
    'mail.settings.removed': (
        "Messagerie retirée. Les courriels repartent de l'adresse de la "
        'plateforme.',
        "Mailbox removed. Email goes out from the platform's address again.",
        'Mensajería retirada. Los correos vuelven a salir desde la dirección de '
        'la plataforma.',
        'تمت إزالة البريد. ستُرسل الرسائل من عنوان المنصّة مرة أخرى.',
        'メールボックスを削除しました。メールは再びプラットフォームのアドレスから'
        '送信されます。',
        '邮箱已移除。邮件将重新由平台地址发出。'),
    'mail.settings.remove-failed': (
        "La messagerie n'a pas pu être retirée.",
        'The mailbox could not be removed.',
        'No se ha podido retirar la mensajería.',
        'تعذّرت إزالة البريد.',
        'メールボックスを削除できませんでした。',
        '无法移除此邮箱。'),

    # --- envoi d'essai ---------------------------------------------------
    'mail.settings.test': (
        "Envoi d'essai", 'Test message', 'Envío de prueba', 'رسالة اختبار',
        'テスト送信', '测试邮件'),
    'mail.settings.test-help': (
        "Un message réel part vers l'adresse indiquée. C'est le seul moyen de "
        'savoir que le serveur accepte votre compte et votre adresse '
        "d'expédition : le refus de l'expéditeur est le cas le plus fréquent.",
        'A real message goes to the address given. It is the only way to know '
        'that the server accepts both your account and your from address: a '
        'rejected sender is the most common case.',
        'Se envía un mensaje real a la dirección indicada. Es la única forma de '
        'saber que el servidor acepta su cuenta y su dirección de envío: el '
        'rechazo del remitente es el caso más frecuente.',
        'تُرسل رسالة حقيقية إلى العنوان المحدّد. وهي الطريقة الوحيدة لمعرفة أنّ '
        'الخادم يقبل حسابك وعنوان الإرسال معًا: ورفض المُرسل هو الحالة الأكثر '
        'شيوعًا.',
        '指定したアドレスへ実際のメッセージを送信します。サーバーがアカウントと'
        '送信元アドレスの両方を受け付けるかを知る唯一の方法で、送信者の拒否が'
        '最も多い原因です。',
        '将向指定地址发送一封真实邮件。这是确认服务器同时接受您的账户和发件地址的'
        '唯一方式：发件人被拒是最常见的情况。'),
    'mail.settings.test-recipient': (
        "Destinataire de l'essai", 'Test recipient',
        'Destinatario de la prueba', 'مستلم الاختبار',
        'テスト送信先', '测试收件人'),
    'mail.settings.send-test': (
        "Envoyer l'essai", 'Send the test', 'Enviar la prueba',
        'إرسال الاختبار', 'テスト送信', '发送测试'),
    'mail.settings.test-ok': (
        "Courriel d'essai envoyé. La messagerie est vérifiée.",
        'Test message sent. The mailbox is verified.',
        'Correo de prueba enviado. La mensajería está verificada.',
        'تم إرسال رسالة الاختبار. تم التحقّق من البريد.',
        'テストメールを送信しました。メールボックスは確認済みです。',
        '测试邮件已发送。该邮箱已验证。'),
    'mail.settings.test-failed': (
        "L'essai n'a pas pu être lancé.", 'The test could not be started.',
        'No se ha podido iniciar la prueba.', 'تعذّر بدء الاختبار.',
        'テストを開始できませんでした。', '无法启动测试。'),

    # --- la navigation ----------------------------------------------------
    'nav.admin-mail': (
        'Messagerie', 'Email delivery', 'Mensajería', 'البريد الصادر',
        'メール送信設定', '邮件发送'),

    # --- la console de l'éditeur, sur un cabinet ---------------------------
    'platform.firms.mail': (
        'Messagerie', 'Email', 'Mensajería', 'البريد', 'メール', '邮件'),
    'platform.firms.mail-title': (
        'Messagerie de {$INTERPOLATION}', 'Email delivery for {$INTERPOLATION}',
        'Mensajería de {$INTERPOLATION}', 'بريد {$INTERPOLATION}',
        '{$INTERPOLATION} のメール送信設定', '{$INTERPOLATION} 的邮件发送'),
}
