# -*- coding: utf-8 -*-
"""Table i18n - rapport 8D d'une non-conformité. id: (fr, en, es, ar, ja, zh).

Fidélité normative : « 8D », « D1 »… « D8 » sont des sigles de la méthode et ne se
traduisent pas. Les intitulés des huit disciplines, eux, se traduisent — sauf qu'ils
sont produits par le SERVEUR dans l'instantané figé du rapport, et n'apparaissent donc
pas ici : un document émis doit dire, dans dix ans, ce qu'il disait le jour de la
clôture, et son texte ne suit pas la langue de l'écran.
"""

TRANSLATIONS = {
    'nc.8d.title': (
        'Rapport 8D', '8D report', 'Informe 8D', 'تقرير 8D',
        '8D レポート', '8D 报告'),
    'nc.8d.eyebrow': (
        "Huit disciplines · Agrégé depuis le dossier · Signé et ancré à l'émission",
        'Eight disciplines · Aggregated from the case · Signed and anchored on issue',
        'Ocho disciplinas · Agregado del expediente · Firmado y anclado al emitir',
        'ثماني مراحل · مُجمَّع من الملف · موقَّع ومُرسَّخ عند الإصدار',
        '8 つの規律 · 案件から集約 · 発行時に署名・アンカー',
        '八个步骤 · 从案卷汇总 · 发布时签名并锚定'),
    'nc.8d.back-tooltip': (
        'Retour à la non-conformité', 'Back to the non-conformity',
        'Volver a la no conformidad', 'العودة إلى حالة عدم المطابقة',
        '不適合に戻る', '返回不合格项'),
    'nc.8d.status-draft': (
        'Brouillon', 'Draft', 'Borrador', 'مسودة', '下書き', '草稿'),
    'nc.8d.status-issued': (
        'Émis', 'Issued', 'Emitido', 'صادر', '発行済み', '已发布'),
    'nc.8d.partial-badge': (
        'Partiel', 'Partial', 'Parcial', 'جزئي', '一部未記入', '部分完成'),
    'nc.8d.partial-note': (
        'Disciplines sans contenu :', 'Disciplines with no content:',
        'Disciplinas sin contenido:', 'المراحل بلا محتوى:',
        '内容のない規律：', '无内容的步骤：'),
    'nc.8d.sealed': (
        'Contenu figé, signé (Ed25519 + ML-DSA-65) et ancré.',
        'Content frozen, signed (Ed25519 + ML-DSA-65) and anchored.',
        'Contenido congelado, firmado (Ed25519 + ML-DSA-65) y anclado.',
        'المحتوى مُثبَّت وموقَّع (Ed25519 + ML-DSA-65) ومُرسَّخ.',
        '内容を確定し、署名（Ed25519 + ML-DSA-65）とアンカーを付与しました。',
        '内容已固定、签名（Ed25519 + ML-DSA-65）并锚定。'),
    'nc.8d.saisies-title': (
        'Les trois disciplines à saisir', 'The three disciplines to fill in',
        'Las tres disciplinas que se rellenan', 'المراحل الثلاث التي تُدخَل يدوياً',
        '手入力する 3 つの規律', '需手工填写的三个步骤'),
    'nc.8d.saisies-hint': (
        "L'équipe, l'endiguement et la reconnaissance n'ont aucune source dans la plateforme : elles se saisissent. Les cinq autres disciplines sont agrégées depuis le dossier de la non-conformité.",
        'The team, the containment and the recognition have no source in the platform: they are filled in here. The other five disciplines are aggregated from the non-conformity case.',
        'El equipo, la contención y el reconocimiento no tienen ninguna fuente en la plataforma: se rellenan aquí. Las otras cinco disciplinas se agregan del expediente de la no conformidad.',
        'الفريق والاحتواء وتقدير الجهد لا مصدر لها في المنصة: تُدخَل هنا. أما المراحل الخمس الأخرى فتُجمَّع من ملف حالة عدم المطابقة.',
        'チーム、暫定対策、功績の承認はプラットフォームに情報源がないため、ここで入力します。残る 5 つの規律は不適合の案件から集約されます。',
        '团队、围堵措施和团队认可在平台中没有数据来源，需在此填写。其余五个步骤从不合格项案卷中汇总。'),
    'nc.8d.team-label': (
        'D1 — Équipe', 'D1 — Team', 'D1 — Equipo', 'D1 — الفريق',
        'D1 — チーム', 'D1 — 团队'),
    'nc.8d.team-placeholder': (
        "Une personne par ligne, avec son rôle dans le traitement de l'écart",
        'One person per line, with their role in handling the deviation',
        'Una persona por línea, con su función en el tratamiento de la desviación',
        'شخص واحد في كل سطر، مع دوره في معالجة الانحراف',
        '1 行に 1 名、逸脱対応における役割を添えて',
        '每行一人，并注明其在偏差处理中的角色'),
    'nc.8d.containment-label': (
        "D3 — Actions d'endiguement immédiates", 'D3 — Immediate containment actions',
        'D3 — Acciones de contención inmediatas', 'D3 — إجراءات الاحتواء الفورية',
        'D3 — 緊急の暫定対策', 'D3 — 立即围堵措施'),
    'nc.8d.containment-placeholder': (
        'Ce qui a protégé le client pendant la recherche de la cause : tri, blocage de lot, contrôle renforcé…',
        'What protected the customer while the cause was being found: sorting, lot hold, tightened inspection…',
        'Lo que protegió al cliente mientras se buscaba la causa: selección, bloqueo de lote, control reforzado…',
        'ما حمى العميل أثناء البحث عن السبب: فرز، حجز دفعة، تفتيش مشدّد…',
        '原因究明の間に顧客を守った措置：選別、ロット保留、検査強化など',
        '在查找原因期间保护客户的措施：分选、批次冻结、加严检验……'),
    'nc.8d.recognition-label': (
        "D8 — Reconnaissance de l'équipe", 'D8 — Team recognition',
        'D8 — Reconocimiento del equipo', 'D8 — تقدير جهد الفريق',
        'D8 — チームの功績の承認', 'D8 — 团队认可'),
    'nc.8d.recognition-placeholder': (
        "Ce que l'équipe a appris, et ce qui mérite d'être dit",
        'What the team learned, and what deserves to be said',
        'Lo que el equipo aprendió y lo que merece decirse',
        'ما تعلَّمه الفريق، وما يستحق أن يُقال',
        'チームが学んだこと、そして伝える価値のあること',
        '团队学到了什么，以及值得说明的内容'),
    'nc.8d.discipline-empty': (
        'Non renseigné.', 'Not filled in.', 'Sin rellenar.', 'غير مُدخَل.',
        '未記入です。', '未填写。'),
    'nc.8d.issue': (
        'Émettre', 'Issue', 'Emitir', 'إصدار', '発行', '发布'),
    'nc.8d.issue-tooltip': (
        "Figer le contenu, signer son empreinte et l'ancrer",
        'Freeze the content, sign its fingerprint and anchor it',
        'Congelar el contenido, firmar su huella y anclarla',
        'تثبيت المحتوى وتوقيع بصمته وترسيخها',
        '内容を確定し、ハッシュに署名してアンカーを付与します',
        '固定内容、对其指纹签名并锚定'),
    'nc.8d.issue-confirm-title': (
        'Émettre le rapport 8D ?', 'Issue the 8D report?', '¿Emitir el informe 8D?',
        'إصدار تقرير 8D؟', '8D レポートを発行しますか？', '要发布 8D 报告吗？'),
    'nc.8d.issue-confirm': (
        'Le contenu sera figé, signé et ancré. Il ne pourra plus être modifié.',
        'The content will be frozen, signed and anchored. It can no longer be changed.',
        'El contenido se congelará, se firmará y se anclará. Ya no podrá modificarse.',
        'سيُثبَّت المحتوى ويُوقَّع ويُرسَّخ، ولن يكون من الممكن تعديله بعد ذلك.',
        '内容は確定され、署名とアンカーが付きます。以後は変更できません。',
        '内容将被固定、签名并锚定，之后不可再修改。'),
    'nc.8d.issue-confirm-partial': (
        "Des disciplines n'ont aucun contenu : le rapport sera marqué « partiel ». Son contenu sera figé, signé et ancré — il ne pourra plus être modifié.",
        'Some disciplines have no content: the report will be marked "partial". Its content will be frozen, signed and anchored — it can no longer be changed.',
        'Algunas disciplinas no tienen contenido: el informe se marcará como «parcial». Su contenido se congelará, se firmará y se anclará, y ya no podrá modificarse.',
        'بعض المراحل بلا محتوى: سيُوسَم التقرير بأنه «جزئي». وسيُثبَّت محتواه ويُوقَّع ويُرسَّخ، ولن يكون من الممكن تعديله.',
        '内容のない規律があります。レポートは「一部未記入」と表示されます。内容は確定され、署名とアンカーが付き、以後は変更できません。',
        '部分步骤没有内容：报告将标记为“部分完成”。其内容将被固定、签名并锚定，之后不可再修改。'),
    'nc.8d.issued': (
        'Rapport 8D émis, signé et ancré.', '8D report issued, signed and anchored.',
        'Informe 8D emitido, firmado y anclado.', 'تم إصدار تقرير 8D وتوقيعه وترسيخه.',
        '8D レポートを発行し、署名とアンカーを付与しました。', '8D 报告已发布、签名并锚定。'),
    'nc.8d.issue-failed': (
        'Émission impossible.', 'Cannot issue the report.', 'No se puede emitir.',
        'الإصدار غير ممكن.', '発行できません。', '无法发布。'),
    'nc.8d.saved': (
        'Rapport 8D enregistré.', '8D report saved.', 'Informe 8D guardado.',
        'تم حفظ تقرير 8D.', '8D レポートを保存しました。', '8D 报告已保存。'),
    'nc.8d.save-failed': (
        'Enregistrement impossible.', 'Cannot save.', 'No se puede guardar.',
        'الحفظ غير ممكن.', '保存できません。', '无法保存。'),
    'nc.8d.load-failed': (
        'Rapport 8D indisponible.', '8D report unavailable.', 'Informe 8D no disponible.',
        'تقرير 8D غير متاح.', '8D レポートを取得できません。', '无法获取 8D 报告。'),
    'nc.8d.download': (
        'Télécharger le PDF', 'Download the PDF', 'Descargar el PDF',
        'تنزيل ملف PDF', 'PDF をダウンロード', '下载 PDF'),
    'nc.8d.download-failed': (
        'Téléchargement impossible.', 'Download failed.', 'Descarga imposible.',
        'التنزيل غير ممكن.', 'ダウンロードできません。', '无法下载。'),
    'nc.detail.eightd': (
        'Rapport 8D', '8D report', 'Informe 8D', 'تقرير 8D',
        '8D レポート', '8D 报告'),
    'nc.detail.eightd-tooltip': (
        'Les huit disciplines, agrégées depuis ce dossier',
        'The eight disciplines, aggregated from this case',
        'Las ocho disciplinas, agregadas de este expediente',
        'المراحل الثماني، مُجمَّعة من هذا الملف',
        '8 つの規律を、この案件から集約して表示',
        '从本案卷汇总的八个步骤'),
}
