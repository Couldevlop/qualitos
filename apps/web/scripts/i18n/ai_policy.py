# -*- coding: utf-8 -*-
"""Table i18n - Fournisseur d'IA du tenant et souveraineté (ADR 0102).
id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.admin-ai': ("Fournisseur d'IA", 'AI provider', 'Proveedor de IA', 'مزوّد الذكاء الاصطناعي', 'AI プロバイダー', 'AI 提供方'),

    'admin.ai.title': ("Fournisseur d'IA", 'AI provider', 'Proveedor de IA', 'مزوّد الذكاء الاصطناعي', 'AI プロバイダー', 'AI 提供方'),
    'admin.ai.subtitle': (
        "À qui partent les demandes faites à l'IA pour votre organisation.",
        "Where the requests made to the AI for your organization are sent.",
        'A quién se envían las solicitudes hechas a la IA para su organización.',
        'إلى أين تُرسَل الطلبات الموجّهة إلى الذكاء الاصطناعي لمؤسستك.',
        '組織の AI へのリクエストの送信先。',
        '贵组织向 AI 发出的请求发送到哪里。'),
    'admin.ai.load-failed': (
        "Le fournisseur d'IA n'a pas pu être chargé.", 'The AI provider could not be loaded.',
        'No se ha podido cargar el proveedor de IA.', 'تعذّر تحميل مزوّد الذكاء الاصطناعي.',
        'AI プロバイダーを読み込めませんでした。', '无法加载 AI 提供方。'),
    'admin.ai.save-failed': (
        "Le fournisseur d'IA n'a pas pu être enregistré.", 'The AI provider could not be saved.',
        'No se ha podido guardar el proveedor de IA.', 'تعذّر حفظ مزوّد الذكاء الاصطناعي.',
        'AI プロバイダーを保存できませんでした。', '无法保存 AI 提供方。'),
    'admin.ai.saved': (
        "Fournisseur d'IA enregistré. Il s'applique aux prochaines demandes.",
        'AI provider saved. It applies to the next requests.',
        'Proveedor de IA guardado. Se aplica a las próximas solicitudes.',
        'تم حفظ مزوّد الذكاء الاصطناعي. يُطبَّق على الطلبات القادمة.',
        'AI プロバイダーを保存しました。次のリクエストから適用されます。',
        'AI 提供方已保存，将用于之后的请求。'),
    'admin.ai.unknown-provider': ('inconnu', 'unknown', 'desconocido', 'غير معروف', '不明', '未知'),

    'admin.ai.provider.ollama': ('Modèle local (Ollama)', 'Local model (Ollama)', 'Modelo local (Ollama)', 'نموذج محلي (Ollama)', 'ローカルモデル（Ollama）', '本地模型（Ollama）'),
    'admin.ai.provider.ollama-desc': (
        "Le modèle tourne sur l'infrastructure de votre installation. Les demandes, et les données qu'elles portent, ne la quittent pas.",
        'The model runs on the infrastructure of your installation. Requests, and the data they carry, never leave it.',
        'El modelo se ejecuta en la infraestructura de su instalación. Las solicitudes, y los datos que contienen, no salen de ella.',
        'يعمل النموذج على البنية التحتية لتثبيتك. لا تغادرها الطلبات ولا البيانات التي تحملها.',
        'モデルはお客様の環境のインフラ上で動作します。リクエストとそれに含まれるデータは外部に出ません。',
        '模型运行在您部署环境的基础设施上。请求及其携带的数据不会离开该环境。'),
    'admin.ai.provider.mistral': ('Mistral AI', 'Mistral AI', 'Mistral AI', 'Mistral AI', 'Mistral AI', 'Mistral AI'),
    'admin.ai.provider.mistral-desc': (
        'Service externe. Les demandes lui sont transmises, une fois retirées les données personnelles détectées.',
        'External service. Requests are sent to it once the detected personal data has been removed.',
        'Servicio externo. Las solicitudes se le transmiten una vez eliminados los datos personales detectados.',
        'خدمة خارجية. تُرسَل إليها الطلبات بعد إزالة البيانات الشخصية المكتشفة.',
        '外部サービス。検出された個人データを除去したうえでリクエストを送信します。',
        '外部服务。请求在移除检测到的个人数据后发送给它。'),
    'admin.ai.provider.anthropic': ('Anthropic', 'Anthropic', 'Anthropic', 'Anthropic', 'Anthropic', 'Anthropic'),
    'admin.ai.provider.anthropic-desc': (
        'Service externe. Les demandes lui sont transmises, une fois retirées les données personnelles détectées.',
        'External service. Requests are sent to it once the detected personal data has been removed.',
        'Servicio externo. Las solicitudes se le transmiten una vez eliminados los datos personales detectados.',
        'خدمة خارجية. تُرسَل إليها الطلبات بعد إزالة البيانات الشخصية المكتشفة.',
        '外部サービス。検出された個人データを除去したうえでリクエストを送信します。',
        '外部服务。请求在移除检测到的个人数据后发送给它。'),

    'admin.ai.state.sovereign': (
        'Souverain : les demandes restent sur votre infrastructure.',
        'Sovereign: requests stay on your infrastructure.',
        'Soberano: las solicitudes permanecen en su infraestructura.',
        'سيادي: تبقى الطلبات على بنيتك التحتية.',
        'ソブリン：リクエストはお客様のインフラ内にとどまります。',
        '自主可控：请求保留在您的基础设施内。'),
    'admin.ai.state.external': (
        'Les demandes partent vers un service externe : {$INTERPOLATION}.',
        'Requests are sent to an external service: {$INTERPOLATION}.',
        'Las solicitudes se envían a un servicio externo: {$INTERPOLATION}.',
        'تُرسَل الطلبات إلى خدمة خارجية: {$INTERPOLATION}.',
        'リクエストは外部サービスに送信されます：{$INTERPOLATION}。',
        '请求将发送到外部服务：{$INTERPOLATION}。'),
    'admin.ai.state.unknown': (
        "Le fournisseur qui répond n'est pas connu pour l'instant.",
        'The provider answering is not known for now.',
        'Por ahora no se sabe qué proveedor responde.',
        'المزوّد الذي يجيب غير معروف حاليًا.',
        '現在、応答するプロバイダーは不明です。',
        '目前尚不清楚由哪个提供方响应。'),
    'admin.ai.state.pending': (
        'Ce sera le cas une fois le choix enregistré.', 'This will apply once the choice is saved.',
        'Así será una vez guardada la elección.', 'سيصبح ذلك ساريًا بعد حفظ الاختيار.',
        '選択を保存すると適用されます。', '保存选择后即生效。'),
    'admin.ai.state.changed': (
        'Choisi le {$INTERPOLATION} par', 'Chosen on {$INTERPOLATION} by', 'Elegido el {$INTERPOLATION} por',
        'اختير في {$INTERPOLATION} بواسطة', '{$INTERPOLATION} に選択、選択者：', '于 {$INTERPOLATION} 选择，选择人：'),

    'admin.ai.catalog-unavailable': (
        "Le service d'IA ne répond pas : on ne peut pas vérifier pour l'instant quels fournisseurs sont configurés.",
        'The AI service is not responding: which providers are configured cannot be checked for now.',
        'El servicio de IA no responde: por ahora no se puede comprobar qué proveedores están configurados.',
        'خدمة الذكاء الاصطناعي لا تستجيب: لا يمكن حاليًا التحقق من المزوّدين المهيّئين.',
        'AI サービスが応答しません：現在、どのプロバイダーが設定済みか確認できません。',
        'AI 服务无响应：目前无法确认哪些提供方已配置。'),
    'admin.ai.pinned-broken': (
        "Le fournisseur choisi n'est plus configuré sur la plateforme : les fonctions d'IA échouent tant qu'un autre n'est pas choisi.",
        'The chosen provider is no longer configured on the platform: AI features fail until another one is chosen.',
        'El proveedor elegido ya no está configurado en la plataforma: las funciones de IA fallan hasta que se elija otro.',
        'المزوّد المختار لم يعد مهيّأً على المنصة: تفشل وظائف الذكاء الاصطناعي حتى يتم اختيار مزوّد آخر.',
        '選択したプロバイダーはプラットフォームで設定されていません。別のプロバイダーを選ぶまで AI 機能は失敗します。',
        '所选提供方已不再在平台上配置：在选择其他提供方之前，AI 功能将无法使用。'),

    'admin.ai.choice': ('Fournisseur des demandes', 'Provider for requests', 'Proveedor de las solicitudes', 'مزوّد الطلبات', 'リクエストのプロバイダー', '请求的提供方'),
    'admin.ai.choice-help': (
        "Un seul fournisseur reçoit toutes les demandes de votre organisation. Il n'y a pas de bascule automatique : si celui-ci ne répond pas, la demande échoue plutôt que de partir ailleurs.",
        'A single provider receives all your organization\'s requests. There is no automatic failover: if it does not respond, the request fails rather than going elsewhere.',
        'Un único proveedor recibe todas las solicitudes de su organización. No hay conmutación automática: si no responde, la solicitud falla en lugar de enviarse a otro lugar.',
        'يتلقى مزوّد واحد جميع طلبات مؤسستك. لا يوجد تحويل تلقائي: إذا لم يستجب، تفشل الطلب بدلًا من إرساله إلى جهة أخرى.',
        '組織のすべてのリクエストを 1 つのプロバイダーが受け取ります。自動切り替えはありません。応答がない場合、リクエストは他へ送られずに失敗します。',
        '贵组织的所有请求均由一个提供方接收。没有自动切换：若其无响应，请求将失败，而不会转发到其他地方。'),
    'admin.ai.default': ('Défaut de la plateforme', 'Platform default', 'Predeterminado de la plataforma', 'الإعداد الافتراضي للمنصة', 'プラットフォームの既定', '平台默认'),
    'admin.ai.default-desc': (
        "Suit le réglage de la plateforme, aujourd'hui : {$INTERPOLATION}. Il peut changer sans que vous le choisissiez.",
        'Follows the platform setting, currently: {$INTERPOLATION}. It may change without you choosing it.',
        'Sigue la configuración de la plataforma, actualmente: {$INTERPOLATION}. Puede cambiar sin que usted lo elija.',
        'يتبع إعداد المنصة، حاليًا: {$INTERPOLATION}. قد يتغيّر دون أن تختاره.',
        'プラットフォームの設定に従います（現在：{$INTERPOLATION}）。お客様が選ばなくても変わることがあります。',
        '遵循平台设置，当前为：{$INTERPOLATION}。它可能在您未选择的情况下改变。'),
    'admin.ai.default-desc-unknown': (
        'Suit le réglage de la plateforme. Il peut changer sans que vous le choisissiez.',
        'Follows the platform setting. It may change without you choosing it.',
        'Sigue la configuración de la plataforma. Puede cambiar sin que usted lo elija.',
        'يتبع إعداد المنصة. قد يتغيّر دون أن تختاره.',
        'プラットフォームの設定に従います。お客様が選ばなくても変わることがあります。',
        '遵循平台设置。它可能在您未选择的情况下改变。'),
    'admin.ai.badge.on-premise': ('Sur votre infrastructure', 'On your infrastructure', 'En su infraestructura', 'على بنيتك التحتية', 'お客様のインフラ上', '在您的基础设施上'),
    'admin.ai.badge.external': ('Service externe', 'External service', 'Servicio externo', 'خدمة خارجية', '外部サービス', '外部服务'),
    'admin.ai.badge.not-configured': ('Non configuré', 'Not configured', 'No configurado', 'غير مهيّأ', '未設定', '未配置'),

    'admin.ai.scope': ('Ce que ce choix couvre', 'What this choice covers', 'Lo que abarca esta elección', 'ما يشمله هذا الاختيار', 'この選択の対象', '此选择涵盖的内容'),
    'admin.ai.scope.reports': (
        "Rapports d'audit, comptes-rendus de cercles et récits des tableaux de bord.",
        'Audit reports, circle minutes and dashboard narratives.',
        'Informes de auditoría, actas de círculos y relatos de los cuadros de mando.',
        'تقارير التدقيق ومحاضر الحلقات وسرديات لوحات المعلومات.',
        '監査報告書、サークルの議事録、ダッシュボードの解説。',
        '审核报告、圈会议纪要和仪表板叙述。'),
    'admin.ai.scope.causes': (
        "Causes suggérées des diagrammes d'Ishikawa et brouillons de documents normatifs.",
        'Suggested causes for Ishikawa diagrams and drafts of standards documents.',
        'Causas sugeridas de los diagramas de Ishikawa y borradores de documentos normativos.',
        'الأسباب المقترحة لمخططات إيشيكاوا ومسودات الوثائق المعيارية.',
        '特性要因図（石川図）の推奨原因と規格文書の下書き。',
        '石川图的建议原因及标准文件草稿。'),
    'admin.ai.scope.nlq': (
        'Questions posées en langage naturel sur vos données.', 'Natural-language questions about your data.',
        'Preguntas en lenguaje natural sobre sus datos.', 'الأسئلة المطروحة بلغة طبيعية حول بياناتك.',
        'データに関する自然言語での質問。', '以自然语言提出的关于您数据的问题。'),
    'admin.ai.scope.mock-audit': (
        'Audits blancs du Standards Hub.', 'Standards Hub mock audits.', 'Auditorías simuladas del Standards Hub.',
        'عمليات التدقيق التجريبية في Standards Hub.', 'Standards Hub の模擬監査。', 'Standards Hub 的模拟审核。'),
    'admin.ai.scope.not-covered': (
        "Les calculs statistiques (cartes SPC, anomalies, prévisions, regroupement des NC) n'utilisent aucun modèle de langage : ils restent sur la plateforme quel que soit ce choix.",
        'Statistical calculations (SPC charts, anomalies, forecasts, NC clustering) use no language model: they stay on the platform whatever this choice.',
        'Los cálculos estadísticos (gráficos SPC, anomalías, previsiones, agrupación de NC) no usan ningún modelo de lenguaje: permanecen en la plataforma sea cual sea esta elección.',
        'لا تستخدم الحسابات الإحصائية (مخططات SPC، الحالات الشاذة، التوقعات، تجميع حالات عدم المطابقة) أي نموذج لغوي: تبقى على المنصة مهما كان هذا الاختيار.',
        '統計計算（SPC 管理図、異常検知、予測、不適合のクラスタリング）は言語モデルを使用しません。この選択にかかわらずプラットフォーム内で処理されます。',
        '统计计算（SPC 控制图、异常、预测、不合格项聚类）不使用任何语言模型：无论此选择如何，均保留在平台内。'),
    'admin.ai.leaving-premises': (
        'Attention : les demandes vont quitter votre infrastructure.', 'Warning: requests will leave your infrastructure.',
        'Atención: las solicitudes saldrán de su infraestructura.', 'تنبيه: ستغادر الطلبات بنيتك التحتية.',
        '注意：リクエストがお客様のインフラの外に送信されます。', '注意：请求将离开您的基础设施。'),
    'admin.ai.pending': ('Fournisseur modifié', 'Provider changed', 'Proveedor modificado', 'تم تغيير المزوّد', 'プロバイダーが変更されました', '提供方已更改'),
}
