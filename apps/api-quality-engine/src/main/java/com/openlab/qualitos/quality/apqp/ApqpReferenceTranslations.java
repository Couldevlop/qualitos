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

        // ---------- artefacts attendus : la colonne D du classeur ----------
        //
        // Le libellé dit CE QU'ON DOIT PRODUIRE ; l'artefact dit SOUS QUELLE
        // FORME, et c'est lui qu'un auditeur confronte à la pièce versée. Les
        // textes anglais sont ceux du classeur du commanditaire, mot pour mot.
        //
        // « Control plan » paraît DEUX fois dans le classeur — en phase 3 pour le
        // plan de pré-lancement, en phase 4 pour celui de production. Deux clés
        // d'artefact pour une seule clé de livrable : c'est le même livrable à
        // deux états, et confondre les deux artefacts effacerait la nuance qui
        // justifie sa double présence.

        t("deliv.product-design-requirements.artifact",
          "Synthèse de la voix du client, cahier des charges technique, liste des normes et exigences réglementaires applicables, note d'intention de conception",
          "VOC summary, engineering spec document, applicable standards/regulatory list, design intent statement",
          "Síntesis de la voz del cliente, pliego técnico, lista de normas y requisitos reglamentarios aplicables, nota de intención de diseño",
          "ملخص صوت العميل، وثيقة المواصفات الهندسية، قائمة المعايير والمتطلبات التنظيمية المنطبقة، بيان نية التصميم",
          "VOC 要約、技術仕様書、適用規格・法規一覧、設計意図書",
          "客户之声汇总、工程规范文件、适用标准与法规清单、设计意图说明");
        t("deliv.project-targets.artifact",
          "Fiche d'objectifs chiffrés (cible PPM, cible garantie, seuils de gravité DFMEA, coût cible par pièce, jalons du programme), validée par l'équipe pluridisciplinaire",
          "Quantified target sheet (PPM target, warranty target, DFMEA severity thresholds, target cost/piece, program timing milestones) - CFT signed off",
          "Ficha de objetivos cuantificados (objetivo PPM, objetivo de garantía, umbrales de gravedad del AMFE de diseño, coste objetivo por pieza, hitos del programa), aprobada por el equipo multidisciplinar",
          "ورقة أهداف مُحدَّدة كميًا (هدف PPM، هدف الضمان، عتبات الخطورة في DFMEA، التكلفة المستهدفة للقطعة، محطات جدول البرنامج) معتمدة من الفريق متعدد الوظائف",
          "定量目標シート（PPM 目標、保証目標、DFMEA 厳しさ基準、目標単価、プログラム日程マイルストーン）— CFT 承認済み",
          "量化目标表（PPM 目标、保修目标、DFMEA 严重度阈值、单件目标成本、项目节点），经跨职能小组签署");
        t("deliv.ci-kc-listing.artifact",
          "Registre provisoire des éléments critiques et matrice des caractéristiques clés, rattachés au plan ou à la spécification du client, avec leur justification",
          "Draft CI register and KC matrix linked to customer print/spec with rationale",
          "Registro provisional de elementos críticos y matriz de características clave, vinculados al plano o especificación del cliente, con su justificación",
          "سجل أولي للعناصر الحرجة ومصفوفة الخصائص الرئيسية مرتبطة برسم أو مواصفة العميل مع المبررات",
          "重要アイテム暫定登録簿および重要特性マトリクス（顧客図面・仕様に紐づけ、根拠付き）",
          "关键项目初稿登记表与关键特性矩阵，与客户图纸/规范关联并附理由");
        t("deliv.preliminary-bom.artifact",
          "Nomenclature multi-niveaux (fabriqué ou acheté signalé), plan de codification provisoire, indice de révision d'ingénierie relevé",
          "Multi-level BOM (make/buy flagged), draft part numbering, engineering revision level noted",
          "Lista de materiales multinivel (fabricar o comprar señalado), codificación provisional, índice de revisión de ingeniería anotado",
          "قائمة مواد متعددة المستويات (مع تحديد التصنيع أو الشراء)، ترقيم أولي للقطع، وتسجيل مستوى المراجعة الهندسية",
          "多階層 BOM（内製・購入の区分付き）、暫定部品番号体系、技術改訂レベルの記載",
          "多层级物料清单（标注自制/外购）、零件编号草案、记录工程版本");
        t("deliv.preliminary-process-flow.artifact",
          "Schéma en blocs de haut niveau, de la réception à l'expédition, montrant les grandes étapes du processus",
          "High-level box diagram (receiving to shipping) showing major process steps",
          "Diagrama de bloques de alto nivel, de la recepción a la expedición, con las grandes etapas del proceso",
          "مخطط صندوقي عام (من الاستلام إلى الشحن) يوضح الخطوات الرئيسية للعملية",
          "受入から出荷までの概略ブロック図（主要工程ステップを表示）",
          "从收货到发运的高层方框图，显示主要过程步骤");
        t("deliv.sow-review.artifact",
          "Analyse d'écarts du cahier des charges ou liste de conformité au contrat client, avec le journal des actions",
          "SOW gap analysis / compliance checklist vs. customer contract, action item log",
          "Análisis de brechas del pliego o lista de conformidad frente al contrato del cliente, con el registro de acciones",
          "تحليل الفجوات في بيان العمل أو قائمة المطابقة مقابل عقد العميل، مع سجل الإجراءات",
          "SOW ギャップ分析／顧客契約との適合チェックリスト、アクション記録",
          "工作说明书差距分析／对照客户合同的符合性检查表，以及行动项记录");
        t("deliv.preliminary-sourcing-plan.artifact",
          "Matrice de décision fabriquer ou acheter, liste des fournisseurs candidats, calendrier d'approvisionnement",
          "Make/buy decision matrix, candidate supplier list, sourcing timing plan",
          "Matriz de decisión fabricar o comprar, lista de proveedores candidatos, calendario de aprovisionamiento",
          "مصفوفة قرار التصنيع أو الشراء، قائمة الموردين المرشحين، الجدول الزمني للتوريد",
          "内製・購入の意思決定マトリクス、候補サプライヤー一覧、調達日程計画",
          "自制/外购决策矩阵、候选供应商清单、采购时间计划");
        t("deliv.project-plan.artifact",
          "Planning APQP (diagramme de Gantt), matrice RACI et composition de l'équipe, calendrier des jalons et des revues de passage, journal des risques du programme",
          "APQP timing chart (Gantt), RACI/team roster, milestone/gate schedule, program risk log",
          "Planificación APQP (diagrama de Gantt), matriz RACI y composición del equipo, calendario de hitos y puertas, registro de riesgos del programa",
          "جدول زمني لـ APQP (مخطط جانت)، مصفوفة RACI وقائمة الفريق، جدول المحطات والبوابات، سجل مخاطر البرنامج",
          "APQP 日程表（ガントチャート）、RACI・チーム名簿、マイルストーン／ゲート日程、プログラムリスク記録",
          "APQP 进度表（甘特图）、RACI 与团队名册、里程碑/关口计划、项目风险记录");
        t("deliv.design-risk-analysis.artifact",
          "AMDEC produit initiale, liste de revue de conception, cotation du risque (RPN ou AP)",
          "DFMEA (initial), design review checklist, risk ranking (RPN/AP)",
          "AMFE de diseño inicial, lista de revisión de diseño, valoración del riesgo (RPN o AP)",
          "تحليل أنماط الفشل للتصميم (أولي)، قائمة مراجعة التصميم، ترتيب المخاطر (RPN أو AP)",
          "初版 DFMEA、設計審査チェックリスト、リスク評価（RPN／AP）",
          "初版 DFMEA、设计评审检查表、风险评级（RPN/AP）");
        t("deliv.design-records-bom.artifact",
          "Plans diffusés ou mis à jour, modèles 3D, nomenclature d'ingénierie révisée pour solder les actions de l'AMDEC produit",
          "Released/updated drawings, 3D models, engineering BOM revised to close DFMEA actions",
          "Planos publicados o actualizados, modelos 3D, lista de materiales de ingeniería revisada para cerrar las acciones del AMFE de diseño",
          "رسومات صادرة أو محدَّثة، نماذج ثلاثية الأبعاد، قائمة مواد هندسية منقَّحة لإغلاق إجراءات DFMEA",
          "発行・更新済み図面、3D モデル、DFMEA 処置を完了させるよう改訂した技術 BOM",
          "已发布/更新的图纸、3D 模型、为关闭 DFMEA 措施而修订的工程 BOM");
        t("deliv.special-requirements-kc-ci.artifact",
          "Matrice définitive des caractéristiques spéciales (symboles du client), liste des éléments critiques et des caractéristiques clés avec leur méthode de maîtrise",
          "Finalized special characteristics matrix (customer symbols), CI/KC list with control method flagged",
          "Matriz definitiva de características especiales (símbolos del cliente), lista de elementos críticos y características clave con su método de control",
          "مصفوفة نهائية للخصائص الخاصة (رموز العميل)، قائمة العناصر الحرجة والخصائص الرئيسية مع تحديد أسلوب الضبط",
          "特別特性マトリクス確定版（顧客記号）、管理方法を明示した CI／KC リスト",
          "最终版特殊特性矩阵（客户符号）、标注控制方法的 CI/KC 清单");
        t("deliv.sourcing-risk-analysis.artifact",
          "Évaluation du risque fournisseur (savoir-faire, capacité, santé financière, situation géographique) et plan d'atténuation",
          "Supplier risk assessment (capability, capacity, financial, geographic), mitigation plan",
          "Evaluación del riesgo de proveedores (capacidad técnica, capacidad productiva, solidez financiera, situación geográfica) y plan de mitigación",
          "تقييم مخاطر المورّدين (الكفاءة، الطاقة، الوضع المالي، الموقع الجغرافي) وخطة التخفيف",
          "サプライヤーリスク評価（技術力、能力、財務、地理）と低減計画",
          "供应商风险评估（能力、产能、财务、地域）与缓解计划");
        t("deliv.packaging-specification.artifact",
          "Spécification d'emballage consigné ou perdu, plan d'emballage, projet d'exigences d'étiquetage",
          "Returnable/expendable packaging spec, packaging drawing, labeling requirements draft",
          "Especificación de embalaje retornable o de un solo uso, plano de embalaje, borrador de requisitos de etiquetado",
          "مواصفات التغليف القابل للإرجاع أو المستهلك، رسم التغليف، مسودة متطلبات وضع الملصقات",
          "リターナブル／使い捨て包装仕様、包装図面、ラベル要求事項の草案",
          "可回收/一次性包装规范、包装图纸、标签要求草案");
        t("deliv.design-review-report.artifact",
          "Compte rendu formel de la revue de conception, journal des actions avec responsables et échéances, feuille d'approbation",
          "Formal DR meeting minutes, action item log with owners/due dates, sign-off sheet",
          "Acta formal de la revisión de diseño, registro de acciones con responsables y fechas, hoja de aprobación",
          "محضر رسمي لمراجعة التصميم، سجل الإجراءات مع المسؤولين والمواعيد، ورقة الاعتماد",
          "設計審査の正式議事録、責任者・期限付きアクション記録、承認書",
          "正式设计评审会议纪要、含责任人与到期日的行动项记录、签署表");
        t("deliv.build-plan.artifact",
          "Plan de surveillance prototype, calendrier de fabrication, besoins en ressources et en outillages",
          "Prototype/proto-build control plan, build schedule, resource/tooling requirements",
          "Plan de control de prototipo, calendario de fabricación, necesidades de recursos y utillaje",
          "خطة مراقبة النموذج الأولي، جدول التصنيع، احتياجات الموارد والعُدَد",
          "試作コントロールプラン、製作日程、要員・治工具の要求",
          "样件控制计划、试制日程、资源与工装需求");
        t("deliv.verification-validation-plans.artifact",
          "Plan et rapport de vérification et validation de la conception (DVP&R), avec les résultats d'essai et leur verdict",
          "DVP&R with test results and pass/fail status",
          "Plan e informe de verificación y validación del diseño (DVP&R), con resultados de ensayo y su veredicto",
          "خطة وتقرير التحقق والتصديق على التصميم (DVP&R) مع نتائج الاختبار وحالة النجاح أو الرسوب",
          "試験結果と合否を含む設計検証・妥当性確認計画書（DVP&R）",
          "含试验结果与合格/不合格判定的设计验证与确认计划（DVP&R）");
        t("deliv.feasibility-assessment.artifact",
          "Engagement de faisabilité industrielle signé (fabrication, qualité, achats, outillage) et journal des risques de faisabilité",
          "Manufacturing feasibility commitment sign-off (mfg, quality, purchasing, tooling), feasibility risk log",
          "Compromiso de viabilidad industrial firmado (fabricación, calidad, compras, utillaje) y registro de riesgos de viabilidad",
          "تعهد الجدوى التصنيعية الموقَّع (التصنيع، الجودة، المشتريات، العُدَد) وسجل مخاطر الجدوى",
          "製造可能性コミットメントの承認（製造・品質・購買・治工具）、実現可能性リスク記録",
          "制造可行性承诺签署（制造、质量、采购、工装）与可行性风险记录");
        t("deliv.process-flow-diagram.artifact",
          "Schéma de flux détaillé poste par poste, dont la numérotation coïncide avec celle de l'AMDEC processus et du plan de surveillance",
          "Detailed station-by-station flow diagram matching PFMEA/Control Plan numbering",
          "Diagrama de flujo detallado puesto por puesto, cuya numeración coincide con la del AMFE de proceso y el plan de control",
          "مخطط تدفق تفصيلي محطة بمحطة يطابق ترقيم PFMEA وخطة المراقبة",
          "PFMEA・コントロールプランと番号が一致する工程別詳細フロー図",
          "逐工位的详细流程图，其编号与 PFMEA 和控制计划一致");
        t("deliv.floor-plan-layout.artifact",
          "Plan d'implantation du site ou de la ligne montrant les flux matière, les en-cours, les points de contrôle et l'ergonomie",
          "Facility/line layout drawing showing material flow, WIP, inspection points, ergonomics",
          "Plano de implantación de la planta o de la línea con flujos de material, en curso, puntos de control y ergonomía",
          "رسم تخطيطي للمنشأة أو الخط يبيّن تدفق المواد والعمل تحت التشغيل ونقاط الفحص وبيئة العمل",
          "物流、仕掛品、検査ポイント、人間工学を示す工場・ライン配置図",
          "显示物流、在制品、检验点与人机工程的厂房/生产线布置图");
        t("deliv.production-preparation-plan.artifact",
          "Liste d'aptitude à l'essai de cadence, planning de pré-lancement",
          "Run-at-rate readiness checklist, pre-launch timing plan",
          "Lista de preparación para la prueba de cadencia, calendario de prelanzamiento",
          "قائمة الجاهزية لاختبار المعدل الإنتاجي، الجدول الزمني لما قبل الإطلاق",
          "量産速度試行の準備チェックリスト、量産前日程計画",
          "节拍试生产准备检查表、试生产前进度计划");
        t("deliv.staffing-training-plan.artifact",
          "Matrice de compétences, plan et calendrier de formation, effectifs prévus par équipe",
          "Skills matrix, training plan/schedule, staffing headcount plan by shift",
          "Matriz de competencias, plan y calendario de formación, plantilla prevista por turno",
          "مصفوفة المهارات، خطة وجدول التدريب، خطة أعداد العاملين لكل وردية",
          "スキルマトリクス、教育訓練計画・日程、シフト別要員計画",
          "技能矩阵、培训计划与日程、按班次的人员编制计划");
        t("deliv.pfmea.artifact",
          "AMDEC processus rattachée à l'AMDEC produit et au schéma de flux, cotation RPN ou AP, plan d'actions",
          "Process FMEA linked to DFMEA/process flow, RPN/AP ranking, action plan",
          "AMFE de proceso vinculado al AMFE de diseño y al diagrama de flujo, valoración RPN o AP, plan de acciones",
          "تحليل أنماط الفشل للعملية مرتبط بـ DFMEA ومخطط التدفق، ترتيب RPN أو AP، خطة الإجراءات",
          "DFMEA・工程フローに紐づく工程 FMEA、RPN／AP 評価、処置計画",
          "与 DFMEA 和流程图关联的过程 FMEA、RPN/AP 评级、措施计划");
        t("deliv.process-kcs.artifact",
          "Liste des caractéristiques du processus mises en regard des caractéristiques clés produit, avec la méthode de maîtrise retenue",
          "Process characteristic list mapped to product KCs, control method identified",
          "Lista de características del proceso relacionadas con las características clave del producto, con el método de control definido",
          "قائمة خصائص العملية مرتبطة بالخصائص الرئيسية للمنتج مع تحديد أسلوب الضبط",
          "製品 KC に対応づけた工程特性一覧と、特定された管理方法",
          "与产品关键特性对应的过程特性清单，并明确控制方法");
        t("deliv.control-plan.artifact.prelaunch",
          "Plan de surveillance prototype ou de pré-lancement (entrées, spécification, méthode, taille et fréquence d'échantillon, plan de réaction)",
          "Prototype/Pre-launch Control Plan (inputs, spec, method, sample size/freq, reaction plan)",
          "Plan de control de prototipo o de prelanzamiento (entradas, especificación, método, tamaño y frecuencia de muestra, plan de reacción)",
          "خطة مراقبة النموذج الأولي أو ما قبل الإطلاق (المدخلات، المواصفة، الأسلوب، حجم العينة وتواترها، خطة الاستجابة)",
          "試作／量産前コントロールプラン（入力、規格、方法、サンプル数・頻度、反応計画）",
          "样件/试生产控制计划（输入、规范、方法、样本量与频次、反应计划）");
        t("deliv.preliminary-capacity.artifact",
          "Étude de capacité (théorique face à démontrée), hypothèses de TRS, analyse du goulot",
          "Capacity study (theoretical vs. demonstrated), OEE assumptions, bottleneck analysis",
          "Estudio de capacidad (teórica frente a demostrada), hipótesis de OEE, análisis del cuello de botella",
          "دراسة الطاقة الإنتاجية (النظرية مقابل المُثبتة)، افتراضات OEE، تحليل عنق الزجاجة",
          "能力調査（理論値と実証値）、OEE 前提条件、ボトルネック分析",
          "产能研究（理论值与验证值）、OEE 假设、瓶颈分析");
        t("deliv.work-station-documentation.artifact",
          "Instructions de travail standardisées, aides visuelles, leçons ponctuelles",
          "Standard Work Instructions (SWI), visual aids, one-point lessons",
          "Instrucciones de trabajo estandarizadas, ayudas visuales, lecciones puntuales",
          "تعليمات العمل القياسية، الوسائل البصرية، الدروس أحادية النقطة",
          "標準作業手順書（SWI）、目で見る補助具、ワンポイントレッスン",
          "标准作业指导书（SWI）、目视辅助、单点课程");
        t("deliv.msa-plan.artifact",
          "Liste des moyens de mesure, calendrier des études MSA, plan de R&R par caractéristique",
          "Gage list, MSA study schedule, gage R&R plan per characteristic",
          "Lista de medios de medición, calendario de estudios MSA, plan de R&R por característica",
          "قائمة أدوات القياس، جدول دراسات MSA، خطة R&R لكل خاصية",
          "ゲージ一覧、MSA 実施日程、特性ごとのゲージ R&R 計画",
          "量具清单、MSA 研究日程、按特性的量具 R&R 计划");
        t("deliv.supply-chain-risk-plan.artifact",
          "Registre des risques des fournisseurs de rang inférieur, plan de secours ou de double source",
          "Sub-tier supplier risk register, contingency/dual-sourcing plan",
          "Registro de riesgos de proveedores de nivel inferior, plan de contingencia o de doble fuente",
          "سجل مخاطر مورّدي المستويات الأدنى، خطة الطوارئ أو التوريد المزدوج",
          "二次以下サプライヤーのリスク登録簿、代替・二重調達計画",
          "次级供应商风险登记表、应急/双源采购计划");
        t("deliv.handling-packaging-labelling.artifact",
          "Rapport d'essai d'emballage approuvé, validation de l'étiquette et du code-barres, vérification du marquage des pièces",
          "Approved packaging trial report, label/barcode approval, part marking verification",
          "Informe aprobado del ensayo de embalaje, validación de la etiqueta y del código de barras, verificación del marcado de piezas",
          "تقرير تجربة التغليف المعتمد، اعتماد الملصق والباركود، التحقق من وسم القطع",
          "承認済み包装トライアル報告書、ラベル・バーコード承認、部品刻印の検証",
          "已批准的包装试验报告、标签/条码批准、零件标识验证");
        t("deliv.prr-results.artifact",
          "Grille de notation de la revue d'aptitude au lancement, liste des points ouverts, décision de passage ou de report",
          "PRR scorecard, open issues list, go/no-go decision record",
          "Cuadro de puntuación de la revisión de preparación, lista de puntos abiertos, decisión de seguir o no",
          "بطاقة تقييم مراجعة الجاهزية، قائمة النقاط المفتوحة، سجل قرار المضي أو التوقف",
          "PRR スコアカード、未解決課題一覧、GO／NO-GO 判定記録",
          "生产准备度评审记分卡、未决事项清单、放行/不放行决定记录");
        t("deliv.production-run.artifact",
          "Rapport d'essai de cadence ou de production significative (pièces produites à la cadence prévue, outillage et processus dans leur état définitif)",
          "Run-at-rate/significant production run report (parts built at rate, tooling/process at intended state)",
          "Informe de la prueba de cadencia o de la producción significativa (piezas fabricadas al ritmo previsto, utillaje y proceso en su estado definitivo)",
          "تقرير اختبار المعدل الإنتاجي أو تشغيلة الإنتاج المعتبرة (قطع مُنتجة بالمعدل، العُدَد والعملية في حالتهما النهائية)",
          "量産速度試行／本格生産試行の報告書（規定速度での製作、治工具・工程は本番状態）",
          "节拍试生产/重要生产运行报告（按节拍生产的零件，工装与过程处于最终状态）");
        t("deliv.msa.artifact",
          "Résultats de R&R (%GRR, ndc), analyse de concordance des attributs, approbation",
          "Gage R&R results (%GRR, ndc), attribute agreement analysis, sign-off",
          "Resultados de R&R (%GRR, ndc), análisis de concordancia de atributos, aprobación",
          "نتائج R&R (‏%GRR، ndc)، تحليل توافق الخصائص الوصفية، الاعتماد",
          "ゲージ R&R 結果（%GRR、ndc）、計数値一致性分析、承認",
          "量具 R&R 结果（%GRR、ndc）、计数型一致性分析、签署");
        t("deliv.initial-capability.artifact",
          "Rapport d'étude Cpk/Ppk par caractéristique clé, tableau de synthèse de la capabilité",
          "Cpk/Ppk study report per KC, capability summary sheet",
          "Informe del estudio Cpk/Ppk por característica clave, cuadro resumen de capacidad",
          "تقرير دراسة Cpk/Ppk لكل خاصية رئيسية، ورقة ملخص القدرة",
          "重要特性ごとの Cpk／Ppk 調査報告書、工程能力サマリー",
          "按关键特性的 Cpk/Ppk 研究报告、能力汇总表");
        t("deliv.control-plan.artifact.production",
          "Plan de surveillance de production, finalisé après l'essai de cadence",
          "Production Control Plan (finalized, post run-at-rate)",
          "Plan de control de producción, finalizado tras la prueba de cadencia",
          "خطة مراقبة الإنتاج، نهائية بعد اختبار المعدل الإنتاجي",
          "量産コントロールプラン（量産速度試行後に確定）",
          "生产控制计划（节拍试生产后定稿）");
        t("deliv.capacity-verification.artifact",
          "Rapport de capacité réellement démontrée, comparée à la demande du client (pièces à l'heure, TRS)",
          "Actual demonstrated capacity report vs. customer demand (parts/hr, OEE)",
          "Informe de capacidad realmente demostrada frente a la demanda del cliente (piezas por hora, OEE)",
          "تقرير الطاقة المُثبتة فعليًا مقارنة بطلب العميل (قطع/ساعة، OEE)",
          "顧客要求と対比した実証能力報告書（個／時、OEE）",
          "实际验证产能与客户需求的对比报告（件/小时、OEE）");
        t("deliv.product-validation-results.artifact",
          "Rapport d'essais fonctionnels et de performance au regard du DVP&R, sur des pièces représentatives de la production",
          "Functional/performance test report against DVP&R at production intent",
          "Informe de ensayos funcionales y de rendimiento frente al DVP&R, con piezas representativas de la producción",
          "تقرير الاختبارات الوظيفية والأدائية مقابل DVP&R على قطع ممثلة للإنتاج",
          "量産想定品での DVP&R に対する機能・性能試験報告書",
          "在量产状态下对照 DVP&R 的功能/性能试验报告");
        t("deliv.fair.artifact",
          "Rapport de contrôle du premier article selon AS9102 ou équivalent, plan coté par repères, traçabilité de chaque caractéristique",
          "AS9102 or equivalent FAIR, ballooned drawing, characteristic accountability",
          "Informe de inspección del primer artículo según AS9102 o equivalente, plano numerado por globos, trazabilidad de cada característica",
          "تقرير فحص العينة الأولى وفق AS9102 أو ما يعادله، رسم مُرقَّم بالبالونات، تتبع كل خاصية",
          "AS9102 または同等の初品検査報告書、バルーン付き図面、特性の網羅確認",
          "符合 AS9102 或等效标准的首件检验报告、气球标注图纸、特性逐项追溯");
        t("deliv.ppap-file.artifact",
          "Dossier PPAP complet (éléments 1 à 18 de l'AIAG), formulaire d'approbation signé par le client",
          "Full PPAP package (elements 1-18 per AIAG), PSW signed by customer",
          "Expediente PPAP completo (elementos 1 a 18 según AIAG), formulario de aprobación firmado por el cliente",
          "ملف PPAP كامل (العناصر 1 إلى 18 وفق AIAG)، نموذج الاعتماد موقَّع من العميل",
          "完全な PPAP 一式（AIAG の要素 1〜18）、顧客署名済み PSW",
          "完整 PPAP 资料包（AIAG 第 1–18 项要素）、客户签署的零件提交保证书");
        t("deliv.customer-specific-requirements.artifact",
          "Liste ou matrice de conformité aux exigences spécifiques du client, dossier de preuves conforme aux attentes de son portail",
          "CSR compliance checklist/matrix, evidence file per customer portal requirements",
          "Lista o matriz de conformidad con los requisitos específicos del cliente, expediente de pruebas conforme a las exigencias de su portal",
          "قائمة أو مصفوفة المطابقة للمتطلبات الخاصة بالعميل، ملف أدلة وفق متطلبات بوابته",
          "顧客固有要求への適合チェックリスト／マトリクス、顧客ポータル要件に沿った証拠ファイル",
          "顾客特殊要求符合性检查表/矩阵，以及符合客户门户要求的证据文件");
        t("deliv.quality-indices.artifact",
          "Tableau de bord de maîtrise statistique du processus, bilan qualité mensuel, courbes de tendance",
          "SPC dashboard, monthly quality scorecard, trend charts",
          "Cuadro de mando de control estadístico del proceso, balance mensual de calidad, gráficos de tendencia",
          "لوحة متابعة الضبط الإحصائي للعملية، بطاقة الجودة الشهرية، مخططات الاتجاه",
          "SPC ダッシュボード、月次品質スコアカード、トレンドチャート",
          "SPC 仪表板、月度质量记分卡、趋势图");
        t("deliv.kpis.artifact",
          "Tableau de bord des indicateurs (sécurité, qualité, livraison, coût) avec la cible face au réalisé",
          "KPI dashboard (safety, quality, delivery, cost) with targets vs. actuals",
          "Cuadro de mando de indicadores (seguridad, calidad, entrega, coste) con objetivo frente a realizado",
          "لوحة مؤشرات الأداء (السلامة، الجودة، التسليم، التكلفة) مع المستهدف مقابل المحقق",
          "KPI ダッシュボード（安全・品質・納入・コスト）— 目標と実績の対比",
          "KPI 仪表板（安全、质量、交付、成本），目标与实际对比");
        t("deliv.targets-met-evidence.artifact",
          "Rapport ou matrice de clôture des objectifs de la phase 1, traçant chaque objectif jusqu'à son résultat réel",
          "Phase 1 target closure report/matrix (traceability from targets to actual results)",
          "Informe o matriz de cierre de los objetivos de la fase 1, con trazabilidad de cada objetivo hasta su resultado real",
          "تقرير أو مصفوفة إغلاق أهداف المرحلة الأولى (تتبع من الأهداف إلى النتائج الفعلية)",
          "フェーズ 1 目標の締めくくり報告書／マトリクス（目標から実績までの追跡）",
          "第 1 阶段目标关闭报告/矩阵（从目标到实际结果的追溯）");
        t("deliv.otd-capacity-kpis.artifact",
          "Rapport de tendance de la livraison à l'heure, rapport d'utilisation de la capacité",
          "OTD trend report, capacity utilization report",
          "Informe de tendencia de la entrega a tiempo, informe de utilización de la capacidad",
          "تقرير اتجاه التسليم في الموعد، تقرير استغلال الطاقة الإنتاجية",
          "納期遵守率の推移報告、能力稼働率報告",
          "准时交付趋势报告、产能利用率报告");
        t("deliv.otd-improvement-plan.artifact",
          "Plan d'actions correctives, avec ses jalons, pour tout indicateur au rouge",
          "Corrective action plan with milestones for any red KPIs",
          "Plan de acciones correctivas, con sus hitos, para todo indicador en rojo",
          "خطة إجراءات تصحيحية بمحطاتها لكل مؤشر في المنطقة الحمراء",
          "赤信号の KPI に対するマイルストーン付き是正処置計画",
          "针对任何红色 KPI 的纠正措施计划及其节点");
        t("deliv.closure-recommendations.artifact",
          "Rapport de clôture du programme, traitement des points restés ouverts, approbation du client",
          "Program closure report, open issue disposition, customer sign-off",
          "Informe de cierre del programa, tratamiento de los puntos abiertos, aprobación del cliente",
          "تقرير إغلاق البرنامج، معالجة النقاط المفتوحة، اعتماد العميل",
          "プログラム終結報告書、未解決課題の処置、顧客承認",
          "项目收尾报告、未决事项处置、客户签署");
        t("deliv.continuous-improvement.artifact",
          "Journal des actions kaizen et d'amélioration continue, suivi des gains de coût et de qualité",
          "Kaizen/CI action log, cost/quality improvement tracker",
          "Registro de acciones kaizen y de mejora continua, seguimiento de las mejoras de coste y calidad",
          "سجل إجراءات الكايزن والتحسين المستمر، متابعة مكاسب التكلفة والجودة",
          "改善（カイゼン）・CI 活動記録、コスト／品質改善の管理表",
          "改善/持续改进行动记录、成本与质量改进跟踪表");
        t("deliv.lessons-learned.artifact",
          "Registre des leçons apprises (technique, processus, conduite de programme)",
          "Lessons learned register (technical, process, program management)",
          "Registro de lecciones aprendidas (técnica, proceso, gestión del programa)",
          "سجل الدروس المستفادة (تقني، عملياتي، إدارة البرنامج)",
          "教訓登録簿（技術、工程、プログラム運営）",
          "经验教训登记表（技术、过程、项目管理）");
        t("deliv.updated-risk-analyses.artifact",
          "Documents vivants sous gestion des révisions, mis à jour au vu des données terrain et de garantie, des avis de modification ou des actions d'amélioration",
          "Revision-controlled living documents updated per field/warranty data, ECNs, or CI actions",
          "Documentos vivos bajo control de revisiones, actualizados según los datos de campo y de garantía, los avisos de modificación o las acciones de mejora",
          "وثائق حيّة خاضعة لضبط المراجعات، محدَّثة وفق بيانات الميدان والضمان أو إشعارات التغيير أو إجراءات التحسين",
          "改訂管理された生きた文書（市場・保証データ、設計変更通知、改善活動に応じて更新）",
          "受版本控制的动态文件，依据现场/保修数据、工程变更通知或改进措施更新");
    }
}
