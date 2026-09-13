package com.openlab.qualitos.quality.apqp;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Le référentiel APQP dans les six langues de la plateforme.
 *
 * <p>Les phases et les livrables sont des données du client (ADR 0066), et une
 * donnée ne se traduit pas. Mais la part qui vient du RÉFÉRENTIEL est écrite dans
 * le code : celle-là peut suivre la langue de l'interface, et doit le faire —
 * sans quoi un utilisateur anglophone lit un V français, et inversement.
 *
 * <p>La frontière est la même que partout ailleurs dans la plateforme : ce que la
 * plateforme fournit se traduit, ce que le client écrit lui appartient. Une ligne
 * retouchée n'est donc plus traduite (cf. {@code ApqpService}).
 *
 * <p>Les désignations normatives restent reconnaissables dans chaque langue :
 * « Control plan » devient « Plan de surveillance », qui est le terme du métier en
 * français, et non une traduction mot à mot. Là où l'usage garde l'anglais — PPAP,
 * FAIR, MSA, PFMEA, BOM — le sigle est conservé et explicité.
 *
 * <p>L'ordre des colonnes est celui de {@code LANGUES} : fr, en, es, ar, ja, zh.
 */
final class ApqpReferenceTranslations {

    private ApqpReferenceTranslations() {}

    static final List<String> LANGUES = List.of("fr", "en", "es", "ar", "ja", "zh");

    /** Langue rendue quand celle qui est demandée n'est pas servie. */
    static final String DEFAUT = "fr";

    private static final Map<String, List<String>> TEXTES = new HashMap<>();

    private static void t(String cle, String fr, String en, String es,
                          String ar, String ja, String zh) {
        TEXTES.put(cle, List.of(fr, en, es, ar, ja, zh));
    }

    /**
     * Le texte d'une clé dans la langue demandée.
     *
     * @return le texte traduit, ou {@code null} si la clé est inconnue — auquel cas
     *         l'appelant garde ce que porte la base, qui reste la vérité
     */
    static String texte(String cle, Locale locale) {
        List<String> textes = TEXTES.get(cle);
        if (textes == null) {
            return null;
        }
        int index = LANGUES.indexOf(langue(locale));
        return textes.get(index < 0 ? LANGUES.indexOf(DEFAUT) : index);
    }

    static boolean connait(String cle) {
        return TEXTES.containsKey(cle);
    }

    private static String langue(Locale locale) {
        return locale == null ? DEFAUT : locale.getLanguage().toLowerCase(Locale.ROOT);
    }

    static {
        // ---------- phases : titre, objet, question ----------

        t("phase.planning.title",
          "Planification", "Planning", "Planificación",
          "التخطيط", "計画", "策划");
        t("phase.planning.purpose",
          "Traduire la voix du client en objectifs de conception mesurables.",
          "Turn the voice of the customer into measurable design targets.",
          "Convertir la voz del cliente en objetivos de diseño medibles.",
          "تحويل صوت العميل إلى أهداف تصميم قابلة للقياس.",
          "顧客の声を、測定可能な設計目標に変換します。",
          "把客户之声转化为可量化的设计目标。");
        t("phase.planning.question",
          "Que demande le client, et qu'est-ce que cela impose au produit ?",
          "What does the customer ask for, and what does that impose on the product?",
          "¿Qué pide el cliente y qué impone eso al producto?",
          "ماذا يطلب العميل، وما الذي يفرضه ذلك على المنتج؟",
          "顧客は何を求め、それは製品に何を課すのか。",
          "客户要求什么？这对产品提出了什么约束？");

        t("phase.product-design.title",
          "Conception du produit et développement", "Product Design & Development",
          "Diseño y desarrollo del producto", "تصميم المنتج وتطويره",
          "製品設計・開発", "产品设计与开发");
        t("phase.product-design.purpose",
          "Figer une conception fabricable, vérifiée et documentée.",
          "Settle a design that can be made, verified and documented.",
          "Fijar un diseño fabricable, verificado y documentado.",
          "تثبيت تصميم قابل للتصنيع، مُتحقَّق منه وموثَّق.",
          "製造可能で、検証され、文書化された設計を固めます。",
          "确定可制造、经验证且形成文件的设计。");
        t("phase.product-design.question",
          "Le produit tel que dessiné tient-il ses objectifs, et sait-on le fabriquer ?",
          "Does the product as drawn meet its targets, and can we make it?",
          "¿El producto tal como está diseñado cumple sus objetivos y sabemos fabricarlo?",
          "هل يحقق المنتج كما صُمِّم أهدافه، وهل نعرف كيف نصنعه؟",
          "図面どおりの製品は目標を満たし、それを造れるのか。",
          "按图设计的产品能否达成目标，我们能否制造它？");

        t("phase.process-design.title",
          "Conception du processus et développement", "Process Design & Development",
          "Diseño y desarrollo del proceso", "تصميم العملية وتطويرها",
          "工程設計・開発", "过程设计与开发");
        t("phase.process-design.purpose",
          "Définir le processus de fabrication et ce qui le surveillera.",
          "Define the manufacturing process and what will watch over it.",
          "Definir el proceso de fabricación y lo que lo vigilará.",
          "تحديد عملية التصنيع وما سيراقبها.",
          "製造工程と、それを監視する仕組みを定めます。",
          "定义制造过程，以及监控它的手段。");
        t("phase.process-design.question",
          "Comment fabrique-t-on, et comment saura-t-on que c'est conforme ?",
          "How do we make it, and how will we know it conforms?",
          "¿Cómo se fabrica y cómo sabremos que es conforme?",
          "كيف نصنعه، وكيف سنعرف أنه مطابق؟",
          "どう造り、適合をどう知るのか。",
          "如何制造？又如何确认其合格？");

        t("phase.validation.title",
          "Validation du produit et du processus", "Product and Process Validation",
          "Validación del producto y del proceso", "التحقق من المنتج والعملية",
          "製品・工程の妥当性確認", "产品与过程确认");
        t("phase.validation.purpose",
          "Prouver sur une production réelle que le processus tient ses capabilités.",
          "Prove on a real production run that the process holds its capability.",
          "Probar en una producción real que el proceso mantiene su capacidad.",
          "إثبات على إنتاج حقيقي أن العملية تحافظ على قدرتها.",
          "実生産で、工程が工程能力を保つことを実証します。",
          "通过实际生产证明过程能力得以保持。");
        t("phase.validation.question",
          "Le processus réel, aux cadences réelles, produit-il conforme ?",
          "Does the real process, at real rates, produce conforming parts?",
          "¿El proceso real, a ritmos reales, produce conforme?",
          "هل تُنتج العملية الفعلية، بالمعدلات الفعلية، منتجًا مطابقًا؟",
          "実際の工程は、実際の生産速度で適合品を産み出すのか。",
          "真实过程在真实节拍下能否产出合格品？");

        t("phase.serial-production.title",
          "Production série et retour d'expérience", "Serial Production and Feedback",
          "Producción en serie y retorno de experiencia",
          "الإنتاج المتسلسل والتغذية الراجعة", "量産と振り返り", "量产与反馈");
        t("phase.serial-production.purpose",
          "Produire en série, mesurer ce que le client constate, et réduire la variation restante.",
          "Run serial production, measure what the customer sees, and cut the remaining variation.",
          "Producir en serie, medir lo que constata el cliente y reducir la variación restante.",
          "الإنتاج المتسلسل، وقياس ما يلاحظه العميل، وخفض التباين المتبقي.",
          "量産を行い、顧客が目にするものを測り、残る変動を減らします。",
          "开展量产，衡量客户所见，并削减残余变异。");
        t("phase.serial-production.question",
          "Ce qui sort de la ligne satisfait-il le client, et que corrige-t-on ?",
          "Does what comes off the line satisfy the customer, and what do we fix?",
          "¿Lo que sale de la línea satisface al cliente y qué se corrige?",
          "هل يُرضي ما يخرج من الخط العميل، وما الذي نصححه؟",
          "ラインから出るものは顧客を満足させるか、そして何を直すのか。",
          "下线产品能否让客户满意？我们该纠正什么？");

        // ---------- phase 1 : livrables ----------

        t("deliv.product-design-requirements",
          "Exigences de conception du produit", "Product design requirements",
          "Requisitos de diseño del producto", "متطلبات تصميم المنتج",
          "製品設計要求事項", "产品设计要求");
        t("deliv.project-targets",
          "Objectifs du projet – sécurité, qualité et fabricabilité, durée de vie,"
          + " fiabilité, durabilité, maintenabilité, planning et coût",
          "Project targets – safety, quality/manufacturability, service life,"
          + " reliability, durability, maintainability, schedule, and cost",
          "Objetivos del proyecto: seguridad, calidad y fabricabilidad, vida útil,"
          + " fiabilidad, durabilidad, mantenibilidad, plazos y coste",
          "أهداف المشروع – السلامة، الجودة وقابلية التصنيع، عمر الخدمة، الموثوقية،"
          + " المتانة، قابلية الصيانة، الجدول الزمني، والتكلفة",
          "プロジェクト目標 — 安全性、品質・製造性、耐用寿命、信頼性、耐久性、保全性、日程、コスト",
          "项目目标 — 安全、质量与可制造性、使用寿命、可靠性、耐久性、可维护性、进度与成本");
        t("deliv.ci-kc-listing",
          "Liste préliminaire des éléments critiques (CI) et des caractéristiques clés (KC)",
          "Preliminary listing of Critical Items (CIs) and Key Characteristics (KCs)",
          "Lista preliminar de elementos críticos (CI) y características clave (KC)",
          "قائمة أولية بالعناصر الحرجة (CIs) والخصائص الرئيسية (KCs)",
          "重要アイテム（CI）および重要特性（KC）の暫定リスト",
          "关键项目（CI）与关键特性（KC）初步清单");
        t("deliv.preliminary-bom",
          "Nomenclature préliminaire (BOM)", "Preliminary BOM",
          "Lista de materiales preliminar (BOM)", "قائمة المواد الأولية (BOM)",
          "暫定部品表（BOM）", "初步物料清单（BOM）");
        t("deliv.preliminary-process-flow",
          "Schéma de flux du processus préliminaire", "Preliminary process flow diagram",
          "Diagrama de flujo del proceso preliminar", "مخطط تدفق العملية الأولي",
          "暫定工程フロー図", "初步过程流程图");
        t("deliv.sow-review",
          "Revue du cahier des charges (SOW)", "SOW review",
          "Revisión del pliego de condiciones (SOW)", "مراجعة بيان العمل (SOW)",
          "作業範囲記述書（SOW）のレビュー", "工作说明书（SOW）评审");
        t("deliv.preliminary-sourcing-plan",
          "Plan d'approvisionnement préliminaire", "Preliminary sourcing plan",
          "Plan de aprovisionamiento preliminar", "خطة التوريد الأولية",
          "暫定調達計画", "初步采购计划");
        t("deliv.project-plan",
          "Plan de projet", "Project plan", "Plan de proyecto", "خطة المشروع",
          "プロジェクト計画", "项目计划");

        // ---------- phase 2 : livrables ----------

        t("deliv.design-risk-analysis",
          "Analyse de risque de conception (AMDEC produit)", "Design risk analysis",
          "Análisis de riesgo de diseño (AMFE de diseño)", "تحليل مخاطر التصميم",
          "設計リスク分析（DFMEA）", "设计风险分析（DFMEA）");
        t("deliv.design-records-bom",
          "Dossier de conception et nomenclature traitant les conclusions de l'analyse de risque",
          "Design records and BOM addressing the findings of the design risk analysis",
          "Registros de diseño y BOM que atienden las conclusiones del análisis de riesgo",
          "سجلات التصميم وقائمة المواد التي تعالج نتائج تحليل مخاطر التصميم",
          "設計リスク分析の指摘に対応した設計記録および BOM",
          "针对设计风险分析结论的设计记录与 BOM");
        t("deliv.special-requirements-kc-ci",
          "Exigences particulières, listes des KC et CI produit",
          "Special requirements, product KCs and CIs listings",
          "Requisitos especiales, listas de KC y CI del producto",
          "المتطلبات الخاصة، وقوائم الخصائص الرئيسية والعناصر الحرجة للمنتج",
          "特別要求事項、製品の KC・CI リスト",
          "特殊要求、产品 KC 与 CI 清单");
        t("deliv.sourcing-risk-analysis",
          "Analyse de risque préliminaire du plan d'approvisionnement",
          "Preliminary risk analysis of sourcing plan",
          "Análisis de riesgo preliminar del plan de aprovisionamiento",
          "تحليل المخاطر الأولي لخطة التوريد",
          "調達計画の暫定リスク分析", "采购计划初步风险分析");
        t("deliv.packaging-specification",
          "Spécification d'emballage", "Packaging specification",
          "Especificación de embalaje", "مواصفات التغليف",
          "包装仕様", "包装规范");
        t("deliv.design-review-report",
          "Rapport de revue de conception", "Design review report",
          "Informe de revisión de diseño", "تقرير مراجعة التصميم",
          "設計審査報告書", "设计评审报告");
        t("deliv.build-plan",
          "Plan de fabrication des produits de développement", "Development product build plan",
          "Plan de fabricación de productos de desarrollo", "خطة تصنيع منتجات التطوير",
          "開発品の製作計画", "开发产品试制计划");
        t("deliv.verification-validation-plans",
          "Plans de vérification et de validation de la conception, et résultats associés",
          "Design verification and validation plans, and associated results",
          "Planes de verificación y validación del diseño y resultados asociados",
          "خطط التحقق والتصديق على التصميم والنتائج المرتبطة بها",
          "設計検証・妥当性確認計画および関連結果",
          "设计验证与确认计划及相关结果");
        t("deliv.feasibility-assessment",
          "Évaluation de faisabilité", "Feasibility assessment",
          "Evaluación de viabilidad", "تقييم الجدوى",
          "実現可能性評価", "可行性评估");

        // ---------- phase 3 : livrables ----------

        t("deliv.process-flow-diagram",
          "Schéma de flux du processus", "Process flow diagram",
          "Diagrama de flujo del proceso", "مخطط تدفق العملية",
          "工程フロー図", "过程流程图");
        t("deliv.floor-plan-layout",
          "Plan d'implantation des postes", "Floor plan layout",
          "Plano de implantación", "مخطط توزيع أرضية المصنع",
          "レイアウト図", "车间平面布置图");
        t("deliv.production-preparation-plan",
          "Plan de préparation de la production", "Production preparation plan",
          "Plan de preparación de la producción", "خطة الإعداد للإنتاج",
          "生産準備計画", "生产准备计划");
        t("deliv.staffing-training-plan",
          "Plan d'effectifs et de formation des opérateurs (ressources humaines)",
          "Operator staffing and training plan (Human Resources)",
          "Plan de dotación y formación de operarios (recursos humanos)",
          "خطة التوظيف وتدريب المشغّلين (الموارد البشرية)",
          "オペレーターの要員・教育訓練計画（人事）",
          "操作人员配置与培训计划（人力资源）");
        t("deliv.pfmea",
          "AMDEC processus (PFMEA)", "PFMEA", "AMFE de proceso (PFMEA)",
          "تحليل أنماط الفشل للعملية (PFMEA)", "工程 FMEA（PFMEA）", "过程 FMEA（PFMEA）");
        t("deliv.process-kcs",
          "Caractéristiques clés du processus (KC)", "Process KCs",
          "Características clave del proceso (KC)", "الخصائص الرئيسية للعملية (KCs)",
          "工程の重要特性（KC）", "过程关键特性（KC）");
        t("deliv.control-plan",
          "Plan de surveillance", "Control plan", "Plan de control", "خطة المراقبة",
          "コントロールプラン", "控制计划");
        t("deliv.preliminary-capacity",
          "Évaluation préliminaire de capacité", "Preliminary capacity assessment",
          "Evaluación preliminar de capacidad", "التقييم الأولي للطاقة الإنتاجية",
          "暫定能力評価", "初步产能评估");
        t("deliv.work-station-documentation",
          "Documentation des postes de travail", "Work station documentation",
          "Documentación de los puestos de trabajo", "توثيق محطات العمل",
          "作業ステーション文書", "工位文件");
        t("deliv.msa-plan",
          "Plan d'analyse des systèmes de mesure (MSA)",
          "Measurement Systems Analysis (MSA) Plan",
          "Plan de análisis de los sistemas de medición (MSA)",
          "خطة تحليل أنظمة القياس (MSA)",
          "測定システム解析（MSA）計画", "测量系统分析（MSA）计划");
        t("deliv.supply-chain-risk-plan",
          "Plan de maîtrise des risques de la chaîne d'approvisionnement",
          "Supply Chain Risk Management Plan",
          "Plan de gestión de riesgos de la cadena de suministro",
          "خطة إدارة مخاطر سلسلة التوريد",
          "サプライチェーンリスク管理計画", "供应链风险管理计划");
        t("deliv.handling-packaging-labelling",
          "Approbations de manutention, d'emballage, d'étiquetage et de marquage des pièces",
          "Material handling, packaging, labelling, and part marking approvals",
          "Aprobaciones de manipulación, embalaje, etiquetado y marcado de piezas",
          "الموافقات على المناولة والتغليف ووضع الملصقات ووسم القطع",
          "運搬・包装・ラベル表示・部品刻印の承認",
          "物料搬运、包装、标签与零件标识的批准");
        t("deliv.prr-results",
          "Résultats de la revue d'aptitude au lancement (PRR)",
          "Production Readiness Review (PRR) results",
          "Resultados de la revisión de preparación para la producción (PRR)",
          "نتائج مراجعة الجاهزية للإنتاج (PRR)",
          "量産準備審査（PRR）の結果", "生产准备度评审（PRR）结果");

        // ---------- phase 4 : livrables ----------

        t("deliv.production-run",
          "Produit issu de l'essai de production", "Product from production process run(s)",
          "Producto procedente de la corrida de producción",
          "منتج من تشغيلات عملية الإنتاج",
          "量産試作からの製品", "生产过程试运行所得产品");
        t("deliv.msa",
          "Analyse des systèmes de mesure (MSA)", "MSA",
          "Análisis de los sistemas de medición (MSA)", "تحليل أنظمة القياس (MSA)",
          "測定システム解析（MSA）", "测量系统分析（MSA）");
        t("deliv.initial-capability",
          "Études de capabilité initiale du processus", "Initial process capability studies",
          "Estudios de capacidad inicial del proceso", "دراسات القدرة الأولية للعملية",
          "初期工程能力調査", "初始过程能力研究");
        t("deliv.capacity-verification",
          "Vérification de capacité", "Capacity verification",
          "Verificación de capacidad", "التحقق من الطاقة الإنتاجية",
          "能力検証", "产能验证");
        t("deliv.product-validation-results",
          "Résultats de validation du produit", "Product validation results",
          "Resultados de validación del producto", "نتائج التحقق من المنتج",
          "製品妥当性確認の結果", "产品确认结果");
        t("deliv.fair",
          "Rapport de contrôle du premier article (FAIR)",
          "First Article Inspection Report (FAIR)",
          "Informe de inspección del primer artículo (FAIR)",
          "تقرير فحص العينة الأولى (FAIR)",
          "初品検査報告書（FAIR）", "首件检验报告（FAIR）");
        t("deliv.ppap-file",
          "Dossier PPAP et formulaire d'approbation", "PPAP file and approval form",
          "Expediente PPAP y formulario de aprobación", "ملف PPAP ونموذج الاعتماد",
          "PPAP 提出資料および承認書", "PPAP 文件与批准表");
        t("deliv.customer-specific-requirements",
          "Exigences spécifiques du client", "Customer specific requirements",
          "Requisitos específicos del cliente", "المتطلبات الخاصة بالعميل",
          "顧客固有要求事項", "顾客特殊要求");

        // ---------- phase 5 : livrables ----------

        t("deliv.quality-indices",
          "Indices qualité (Cpk, pièces par million, taux de rebut)",
          "Quality indices [e.g., CpK, Parts Per Million (PPM), rejection rates]",
          "Índices de calidad (Cpk, partes por millón, tasa de rechazo)",
          "مؤشرات الجودة (Cpk، الأجزاء لكل مليون، معدلات الرفض)",
          "品質指標（Cpk、百万分率（PPM）、不良率）",
          "质量指标（Cpk、百万分率 PPM、拒收率）");
        t("deliv.kpis",
          "Indicateurs de performance (KPI)", "Key Performance Indicators (KPIs)",
          "Indicadores clave de rendimiento (KPI)", "مؤشرات الأداء الرئيسية (KPIs)",
          "重要業績評価指標（KPI）", "关键绩效指标（KPI）");
        t("deliv.targets-met-evidence",
          "Preuve que les objectifs du projet sont atteints",
          "Evidence that project targets have been met",
          "Prueba de que se han alcanzado los objetivos del proyecto",
          "دليل على تحقيق أهداف المشروع",
          "プロジェクト目標の達成を示す証拠", "项目目标达成的证据");
        t("deliv.otd-capacity-kpis",
          "Indicateurs de livraison à l'heure (OTD) et de capacité",
          "On-time Delivery (OTD) and capacity KPIs",
          "Indicadores de entrega a tiempo (OTD) y de capacidad",
          "مؤشرات التسليم في الموعد (OTD) والطاقة الإنتاجية",
          "納期遵守率（OTD）と能力の指標", "准时交付（OTD）与产能指标");
        t("deliv.otd-improvement-plan",
          "Plan d'amélioration de la livraison à l'heure et de la capacité",
          "OTD and capacity improvement plan",
          "Plan de mejora de la entrega a tiempo y de la capacidad",
          "خطة تحسين التسليم في الموعد والطاقة الإنتاجية",
          "納期遵守と能力の改善計画", "准时交付与产能改进计划");
        t("deliv.closure-recommendations",
          "Recommandations de clôture du projet", "Project closure recommendations",
          "Recomendaciones de cierre del proyecto", "توصيات إغلاق المشروع",
          "プロジェクト終結の提言", "项目收尾建议");
        t("deliv.continuous-improvement",
          "Actions d'amélioration continue", "Continuous improvement actions",
          "Acciones de mejora continua", "إجراءات التحسين المستمر",
          "継続的改善活動", "持续改进措施");
        t("deliv.lessons-learned",
          "Leçons apprises", "Lessons learned", "Lecciones aprendidas",
          "الدروس المستفادة", "得られた教訓", "经验教训");
        t("deliv.updated-risk-analyses",
          "Mise à jour de l'analyse de risque de conception, de l'AMDEC processus"
          + " et des plans de surveillance",
          "Updated design risk analysis, PFMEA, and control plans",
          "Actualización del análisis de riesgo de diseño, del AMFE de proceso"
          + " y de los planes de control",
          "تحديث تحليل مخاطر التصميم وتحليل أنماط الفشل للعملية وخطط المراقبة",
          "設計リスク分析・PFMEA・コントロールプランの更新",
          "更新设计风险分析、PFMEA 与控制计划");

        // ---------- sous-points et intitulés de mesures amorcés ----------

        t("row.safety", "sécurité", "safety", "seguridad", "السلامة", "安全性", "安全");
        t("row.quality-manufacturability",
          "qualité et fabricabilité", "quality/manufacturability",
          "calidad y fabricabilidad", "الجودة وقابلية التصنيع",
          "品質・製造性", "质量与可制造性");
        t("row.service-life",
          "durée de vie", "service life", "vida útil", "عمر الخدمة", "耐用寿命", "使用寿命");
        t("row.reliability",
          "fiabilité", "reliability", "fiabilidad", "الموثوقية", "信頼性", "可靠性");
        t("row.durability",
          "durabilité", "durability", "durabilidad", "المتانة", "耐久性", "耐久性");
        t("row.maintainability",
          "maintenabilité", "maintainability", "mantenibilidad", "قابلية الصيانة",
          "保全性", "可维护性");
        t("row.schedule", "planning", "schedule", "plazos", "الجدول الزمني", "日程", "进度");
        t("row.cost", "coût", "cost", "coste", "التكلفة", "コスト", "成本");

        t("row.material-handling",
          "manutention", "material handling", "manipulación", "المناولة",
          "運搬", "物料搬运");
        t("row.packaging",
          "emballage", "packaging", "embalaje", "التغليف", "包装", "包装");
        t("row.labelling",
          "étiquetage", "labelling", "etiquetado", "وضع الملصقات", "ラベル表示", "标签");
        t("row.part-marking",
          "marquage des pièces", "part marking", "marcado de piezas", "وسم القطع",
          "部品刻印", "零件标识");

        // Les indices statistiques gardent leur sigle : c'est ainsi qu'ils se lisent
        // dans tous les ateliers, et les traduire les rendrait méconnaissables.
        t("row.cp", "Cp", "Cp", "Cp", "Cp", "Cp", "Cp");
        t("row.cpk", "Cpk", "Cpk", "Cpk", "Cpk", "Cpk", "Cpk");
        t("row.pp", "Pp", "Pp", "Pp", "Pp", "Pp", "Pp");
        t("row.ppk", "Ppk", "Ppk", "Ppk", "Ppk", "Ppk", "Ppk");
        t("row.ppm", "PPM", "PPM", "PPM", "PPM", "PPM", "PPM");
        t("row.rejection-rate",
          "taux de rebut", "rejection rate", "tasa de rechazo", "معدل الرفض",
          "不良率", "拒收率");
        t("row.otd",
          "livraison à l'heure (OTD)", "OTD", "entrega a tiempo (OTD)",
          "التسليم في الموعد (OTD)", "納期遵守（OTD）", "准时交付（OTD）");
        t("row.capacity",
          "capacité", "capacity", "capacidad", "الطاقة الإنتاجية", "能力", "产能");
    }
}
