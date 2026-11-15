# -*- coding: utf-8 -*-
"""Table i18n — la gravite d un ecart se lit dans la norme (ADR 0120).

id: (fr, en, es, ar, ja, zh).

Deux familles de mots, et elles ne disent pas la meme chose :

- `obligation-*` nomme en clair le VERBE de la norme (Directives ISO/IEC,
  Partie 2) la ou l ecran affichait les codes bruts MUST / SHOULD / MAY. Seule
  une exigence peut etre manquee ; une recommandation donne une observation.
- `audit-priority-*` / `priority-*` nomme pour ce qu il est le champ
  `risk_if_missing` : l ordre de passage dans le PROGRAMME d audit (ISO 19011,
  6.3). Il ne gradue plus aucun constat, d ou les indices qui le disent.

`degree-*-def` donne, au-dessus du tableau des ecarts, le critere et la source
des trois degres : un client qui prepare un audit a besoin du critere, pas du
mot seul.
"""

TRANSLATIONS = {
    'standards.detail.obligation-must': (
        'Exigée',
        'Required',
        'Exigida',
        'مطلوبة',
        '要求',
        '要求',
    ),
    'standards.detail.obligation-should': (
        'Recommandée',
        'Recommended',
        'Recomendada',
        'موصى بها',
        '推奨',
        '建议',
    ),
    'standards.detail.obligation-may': (
        'Facultative',
        'Optional',
        'Facultativa',
        'اختيارية',
        '任意',
        '可选',
    ),
    'standards.detail.obligation-must-hint': (
        'La norme dit « doit » : son manquement est une non-conformité.',
        'The standard says “shall”: failing it is a nonconformity.',
        'La norma dice «debe»: su incumplimiento es una no conformidad.',
        'تقول المواصفة «يجب»: مخالفتها عدم مطابقة.',
        '規格が「しなければならない」と定めています。欠如は不適合です。',
        '标准使用“应”：未满足即不合格。',
    ),
    'standards.detail.obligation-should-hint': (
        "La norme dit « devrait » : son manquement n'est pas une non-conformité, mais une observation.",
        'The standard says “should”: failing it is not a nonconformity, but an observation.',
        'La norma dice «debería»: su incumplimiento no es una no conformidad, sino una observación.',
        'تقول المواصفة «ينبغي»: مخالفتها ليست عدم مطابقة بل ملاحظة.',
        '規格が「することが望ましい」と定めています。欠如は不適合ではなく、所見です。',
        '标准使用“宜”：未满足不是不合格，而是观察项。',
    ),
    'standards.detail.obligation-may-hint': (
        'La norme dit « peut » : la disposition est permise, jamais exigible.',
        'The standard says “may”: the arrangement is permitted, never required.',
        'La norma dice «puede»: la disposición está permitida, nunca es exigible.',
        'تقول المواصفة «يمكن»: الترتيب مسموح به ولا يُطلب أبدًا.',
        '規格が「してもよい」と定めています。認められているだけで、要求されることはありません。',
        '标准使用“可”：该安排是允许的，从不是必须的。',
    ),
    'standards.detail.audit-priority-high': (
        'À vérifier en premier',
        'Check first',
        'Verificar primero',
        'يُتحقق منه أولًا',
        '最初に確認',
        '优先核查',
    ),
    'standards.detail.audit-priority-medium': (
        'À vérifier',
        'Check',
        'Verificar',
        'يُتحقق منه',
        '確認',
        '核查',
    ),
    'standards.detail.audit-priority-low': (
        'À vérifier en dernier',
        'Check last',
        'Verificar al final',
        'يُتحقق منه أخيرًا',
        '最後に確認',
        '最后核查',
    ),
    'standards.detail.audit-priority-hint': (
        "Ordre de passage dans le programme d'audit (ISO 19011, 6.3). Ce n'est pas la gravité d'un constat : celle-ci se décide sur la preuve trouvée.",
        'Order of coverage in the audit programme (ISO 19011, 6.3). This is not the grade of a finding: that is decided on the evidence found.',
        'Orden de paso en el programa de auditoría (ISO 19011, 6.3). No es la gravedad de un hallazgo: esa se decide sobre la evidencia encontrada.',
        'ترتيب التغطية في برنامج التدقيق (ISO 19011، 6.3). ليس درجة خطورة الملاحظة: تلك تُحدد بناءً على الدليل الموجود.',
        '監査プログラムでの確認順序（ISO 19011, 6.3）。所見の重大性ではありません—重大性は見つかった証拠に基づいて決まります。',
        '审核方案中的核查顺序（ISO 19011，6.3）。这不是发现的严重程度：严重程度由所找到的证据决定。',
    ),
    'standards.detail.degree-major-def': (
        "La disposition demandée est absente : aucune preuve dans toute la clause. L'aptitude du système à atteindre ses résultats est en cause, et le certificat reste bloqué jusqu'à sa levée (ISO/IEC 17021-1, 3.12).",
        'The required arrangement is absent: no evidence anywhere in the clause. The ability of the system to achieve its intended results is affected, and the certificate stays blocked until it is closed (ISO/IEC 17021-1, 3.12).',
        'La disposición exigida está ausente: ninguna evidencia en toda la cláusula. La capacidad del sistema para alcanzar sus resultados queda en cuestión y el certificado sigue bloqueado hasta su cierre (ISO/IEC 17021-1, 3.12).',
        'الترتيب المطلوب غائب: لا دليل في البند بأكمله. قدرة النظام على تحقيق نتائجه موضع شك، ويبقى الشهادة معلّقًا حتى رفعه (ISO/IEC 17021-1، 3.12).',
        '要求された取決めが存在しません：当該箇条全体に証拠がありません。システムが意図した結果を達成する能力が問われ、除去まで認証は保留されます（ISO/IEC 17021-1, 3.12）。',
        '所要求的安排缺失：整个条款没有任何证据。体系达成预期结果的能力受到质疑，证书在关闭前保持冻结（ISO/IEC 17021-1，3.12）。',
    ),
    'standards.detail.degree-minor-def': (
        "La disposition existe mais n'est pas tenue partout : la clause est démontrée en partie. Manquement isolé, qui n'empêche pas le certificat et demande un plan d'actions (3.13).",
        'The arrangement exists but is not upheld everywhere: the clause is partly evidenced. An isolated lapse, which does not prevent the certificate and calls for an action plan (3.13).',
        'La disposición existe pero no se mantiene en todas partes: la cláusula está evidenciada en parte. Incumplimiento aislado, que no impide el certificado y exige un plan de acciones (3.13).',
        'الترتيب قائم لكنه غير متبع في كل موضع: البند مدعوم جزئيًا. مخالفة معزولة لا تمنع الشهادة وتستلزم خطة إجرائية (3.13).',
        '取決めはあるがすべてで守られていません：箇条は一部のみ証拠付けされています。孤立した欠陥であり、認証を妨げず、改善計画を要します（3.13）。',
        '安排存在但未处处落实：条款仅部分有证据。属孤立的失误，不阻碍证书，但需行动计划（3.13）。',
    ),
    'standards.detail.degree-observation-def': (
        "Ce que la norme recommande, et non ce qu'elle exige. Ce n'est pas une non-conformité : une piste d'amélioration, qui ne se répond pas à l'auditeur (ISO 19011).",
        'What the standard recommends, not what it requires. This is not a nonconformity: an opportunity for improvement, with no answer owed to the auditor (ISO 19011).',
        'Lo que la norma recomienda, no lo que exige. No es una no conformidad: una oportunidad de mejora, que no se responde al auditor (ISO 19011).',
        'ما توصي به المواصفة، لا ما تشترطه. ليست عدم مطابقة: فرصة للتحسين لا يُسأل عنها المدقق (ISO 19011).',
        '規格が推奨することであり、要求することではありません。不適合ではなく、監査員への回答義務のない改善の機会です（ISO 19011）。',
        '标准所建议的，而非所要求的。这不是不合格：是改进机会，无需向审核员答复（ISO 19011）。',
    ),
    'standards.tree.obligation-must': (
        'Exigée — la norme dit « doit »',
        'Required — the standard says “shall”',
        'Exigida — la norma dice «debe»',
        'مطلوبة — تقول المواصفة «يجب»',
        '要求 — 規格は「しなければならない」',
        '要求 — 标准使用“应”',
    ),
    'standards.tree.obligation-should': (
        'Recommandée — la norme dit « devrait »',
        'Recommended — the standard says “should”',
        'Recomendada — la norma dice «debería»',
        'موصى بها — تقول المواصفة «ينبغي»',
        '推奨 — 規格は「望ましい」',
        '建议 — 标准使用“宜”',
    ),
    'standards.tree.obligation-may': (
        'Facultative — la norme dit « peut »',
        'Optional — the standard says “may”',
        'Facultativa — la norma dice «puede»',
        'اختيارية — تقول المواصفة «يمكن»',
        '任意 — 規格は「してもよい」',
        '可选 — 标准使用“可”',
    ),
    'standards.tree.priority-high': (
        'À vérifier en premier',
        'Check first',
        'Verificar primero',
        'يُتحقق منه أولًا',
        '最初に確認',
        '优先核查',
    ),
    'standards.tree.priority-medium': (
        'À vérifier',
        'Check',
        'Verificar',
        'يُتحقق منه',
        '確認',
        '核查',
    ),
    'standards.tree.priority-low': (
        'À vérifier en dernier',
        'Check last',
        'Verificar al final',
        'يُتحقق منه أخيرًا',
        '最後に確認',
        '最后核查',
    ),
    'standards.tree.audit-priority': (
        'Ordre de vérification en audit',
        'Order of coverage in audit',
        'Orden de verificación en auditoría',
        'ترتيب التحقق في التدقيق',
        '監査での確認順序',
        '审核核查顺序',
    ),
    'standards.tree.audit-priority-hint': (
        "Par quoi commencer le programme d'audit (ISO 19011, 6.3). Ne gradue pas les constats : la gravité se décide sur la preuve trouvée.",
        'Where to start the audit programme (ISO 19011, 6.3). Does not grade findings: the grade is decided on the evidence found.',
        'Por dónde empezar el programa de auditoría (ISO 19011, 6.3). No gradúa los hallazgos: la gravedad se decide sobre la evidencia encontrada.',
        'من أين يبدأ برنامج التدقيق (ISO 19011، 6.3). لا يدرّج الملاحظات: الدرجة تُحدد بناءً على الدليل الموجود.',
        '監査プログラムをどこから始めるか（ISO 19011, 6.3）。所見の重大性を定めるものではありません—重大性は証拠に基づいて決まります。',
        '审核方案从何处开始（ISO 19011，6.3）。不对发现分级：严重程度由所找到的证据决定。',
    ),
}
