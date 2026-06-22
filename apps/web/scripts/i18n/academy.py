# -*- coding: utf-8 -*-
"""Table i18n - domaine academy (formation/gamification). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'nav.academy': ('Academy', 'Academy', 'Academy', 'الأكاديمية', 'アカデミー', '学院'),

    # Accueil
    'academy.home.title': ('QualitOS Academy', 'QualitOS Academy', 'QualitOS Academy', 'أكاديمية QualitOS', 'QualitOS アカデミー', 'QualitOS 学院'),
    'academy.home.subtitle': ("Montez en compétence : parcours par rôle et secteur, quiz notés, badges et ceintures qualité, certificats signés et vérifiables.", 'Build your skills: role- and industry-based paths, graded quizzes, quality badges and belts, signed and verifiable certificates.', 'Mejore sus competencias: itinerarios por rol y sector, cuestionarios calificados, insignias y cinturones de calidad, certificados firmados y verificables.', 'طوّر مهاراتك: مسارات حسب الدور والقطاع، اختبارات مُقيَّمة، شارات وأحزمة الجودة، شهادات موقّعة وقابلة للتحقق.', 'スキルを高めましょう：役割と業種別の学習パス、採点クイズ、品質バッジとベルト、署名済みで検証可能な証明書。', '提升技能：按角色和行业划分的学习路径、评分测验、质量徽章与等级带、已签名且可验证的证书。'),

    # Onglets
    'academy.tab.catalog': ('Catalogue', 'Catalog', 'Catálogo', 'الكتالوج', 'カタログ', '课程目录'),
    'academy.tab.myCourses': ('Mes formations', 'My courses', 'Mis cursos', 'دوراتي', 'マイコース', '我的课程'),
    'academy.tab.leaderboard': ('Classement', 'Leaderboard', 'Clasificación', 'لوحة الصدارة', 'ランキング', '排行榜'),

    # Catalogue
    'academy.catalog.empty': ('Aucun cours publié pour le moment.', 'No course published yet.', 'Aún no hay cursos publicados.', 'لا توجد دورات منشورة حتى الآن.', '公開されているコースはまだありません。', '目前尚无已发布的课程。'),
    'academy.points.short': ('pts', 'pts', 'pts', 'نقطة', 'ポイント', '分'),
    'academy.passing': ('Réussite', 'Pass', 'Aprobación', 'النجاح', '合格', '通过'),
    'academy.action.enroll': ("S'inscrire", 'Enroll', 'Inscribirse', 'التسجيل', '受講登録', '报名'),
    'academy.action.enrolled': ('Inscrit', 'Enrolled', 'Inscrito', 'مُسجَّل', '登録済み', '已报名'),

    # Mes formations
    'academy.myCourses.empty': ("Vous n'êtes inscrit à aucun cours. Choisissez-en un dans le catalogue.", 'You are not enrolled in any course. Pick one from the catalog.', 'No está inscrito en ningún curso. Elija uno del catálogo.', 'لست مُسجَّلاً في أي دورة. اختر واحدة من الكتالوج.', 'どのコースにも登録されていません。カタログから選んでください。', '您尚未报名任何课程。请从目录中选择一门。'),
    'academy.score': ('Score : {$INTERPOLATION}%', 'Score: {$INTERPOLATION}%', 'Puntuación: {$INTERPOLATION}%', 'النتيجة: {$INTERPOLATION}%', 'スコア：{$INTERPOLATION}%', '得分：{$INTERPOLATION}%'),

    # Classement
    'academy.leaderboard.empty': ('Le classement est encore vide. Complétez un cours pour y figurer.', 'The leaderboard is still empty. Complete a course to appear on it.', 'La clasificación aún está vacía. Complete un curso para aparecer en ella.', 'لوحة الصدارة لا تزال فارغة. أكمل دورة لتظهر فيها.', 'ランキングはまだ空です。コースを修了すると掲載されます。', '排行榜目前为空。完成一门课程即可上榜。'),
    'academy.lb.rank': ('#', '#', '#', '#', '#', '#'),
    'academy.lb.learner': ('Apprenant', 'Learner', 'Estudiante', 'المتعلِّم', '受講者', '学员'),
    'academy.lb.belt': ('Ceinture', 'Belt', 'Cinturón', 'الحزام', 'ベルト', '等级带'),
    'academy.lb.points': ('Points', 'Points', 'Puntos', 'النقاط', 'ポイント', '积分'),
    'academy.lb.completed': ('Complétions', 'Completions', 'Finalizaciones', 'الإكمالات', '修了数', '完成数'),
    'academy.lb.badges': ('Badges', 'Badges', 'Insignias', 'الشارات', 'バッジ', '徽章'),

    # Lecteur de cours
    'academy.player.back': ("Retour à l'Academy", 'Back to Academy', 'Volver a Academy', 'العودة إلى الأكاديمية', 'アカデミーに戻る', '返回学院'),
    'academy.player.completed': ('Cours terminé — score {$INTERPOLATION}% !', 'Course completed — score {$INTERPOLATION}%!', 'Curso finalizado — puntuación {$INTERPOLATION}%.', 'اكتملت الدورة — النتيجة {$INTERPOLATION}%!', 'コース修了 — スコア {$INTERPOLATION}%！', '课程已完成 — 得分 {$INTERPOLATION}%！'),
    'academy.player.viewCert': ('Voir mon certificat', 'View my certificate', 'Ver mi certificado', 'عرض شهادتي', '証明書を表示', '查看我的证书'),
    'academy.lesson.markDone': ('Marquer comme vue', 'Mark as viewed', 'Marcar como visto', 'تعليم كمُشاهَد', '視聴済みにする', '标记为已学'),
    'academy.lesson.openMedia': ('Ouvrir le média', 'Open media', 'Abrir el medio', 'فتح الوسائط', 'メディアを開く', '打开媒体'),
    'academy.quiz.threshold': ('seuil {$INTERPOLATION}%', 'threshold {$INTERPOLATION}%', 'umbral {$INTERPOLATION}%', 'الحد {$INTERPOLATION}%', '合格基準 {$INTERPOLATION}%', '阈值 {$INTERPOLATION}%'),
    'academy.quiz.submit': ('Soumettre le quiz', 'Submit quiz', 'Enviar cuestionario', 'إرسال الاختبار', 'クイズを提出', '提交测验'),

    # Certificat
    'academy.cert.back': ('Academy', 'Academy', 'Academy', 'الأكاديمية', 'アカデミー', '学院'),
    'academy.cert.copyLink': ('Copier le lien de vérification', 'Copy verification link', 'Copiar el enlace de verificación', 'نسخ رابط التحقق', '検証リンクをコピー', '复制验证链接'),
    'academy.cert.print': ('Imprimer / PDF', 'Print / PDF', 'Imprimir / PDF', 'طباعة / PDF', '印刷 / PDF', '打印 / PDF'),
    'academy.cert.proofTitle': ("Preuve d'intégrité", 'Integrity proof', 'Prueba de integridad', 'إثبات السلامة', '完全性の証明', '完整性证明'),
    'academy.cert.code': ('Code de vérification', 'Verification code', 'Código de verificación', 'رمز التحقق', '検証コード', '验证码'),
    'academy.cert.sha': ('Empreinte SHA-256', 'SHA-256 fingerprint', 'Huella SHA-256', 'بصمة SHA-256', 'SHA-256 フィンガープリント', 'SHA-256 指纹'),
    'academy.cert.anchor': ('Ancrage blockchain', 'Blockchain anchoring', 'Anclaje en blockchain', 'التثبيت على البلوكشين', 'ブロックチェーンへの記録', '区块链锚定'),
    'academy.cert.verifyUrl': ('Lien public', 'Public link', 'Enlace público', 'الرابط العام', '公開リンク', '公开链接'),
    'academy.cert.note': ("Ce certificat est signé (Ed25519 + ML-DSA-65) et son empreinte ancrée en blockchain. Toute personne peut le vérifier via le lien public ci-dessus.", 'This certificate is signed (Ed25519 + ML-DSA-65) and its fingerprint is anchored on the blockchain. Anyone can verify it via the public link above.', 'Este certificado está firmado (Ed25519 + ML-DSA-65) y su huella está anclada en la blockchain. Cualquiera puede verificarlo mediante el enlace público anterior.', 'هذه الشهادة موقّعة (Ed25519 + ML-DSA-65) وبصمتها مثبَّتة على البلوكشين. يمكن لأي شخص التحقق منها عبر الرابط العام أعلاه.', 'この証明書は署名され（Ed25519 + ML-DSA-65）、その指紋はブロックチェーンに記録されています。上記の公開リンクから誰でも検証できます。', '此证书已签名（Ed25519 + ML-DSA-65），其指纹已锚定到区块链。任何人都可以通过上方的公开链接进行验证。'),

    # Toasts / erreurs
    'academy.cert.copied': ('Lien de vérification copié.', 'Verification link copied.', 'Enlace de verificación copiado.', 'تم نسخ رابط التحقق.', '検証リンクをコピーしました。', '验证链接已复制。'),
    'academy.quiz.passed': ('Quiz réussi ({$score}%) !', 'Quiz passed ({$score}%)!', '¡Cuestionario aprobado ({$score}%)!', 'تم اجتياز الاختبار ({$score}%)!', 'クイズ合格（{$score}%）！', '测验通过（{$score}%）！'),
    'academy.quiz.failed': ('Quiz échoué ({$score}%). Réessayez.', 'Quiz failed ({$score}%). Try again.', 'Cuestionario reprobado ({$score}%). Inténtelo de nuevo.', 'فشل الاختبار ({$score}%). حاول مرة أخرى.', 'クイズ不合格（{$score}%）。もう一度お試しください。', '测验未通过（{$score}%）。请重试。'),
    'academy.error.outline': ('Impossible de charger le cours.', 'Unable to load the course.', 'No se puede cargar el curso.', 'تعذّر تحميل الدورة.', 'コースを読み込めません。', '无法加载课程。'),
    'academy.error.enrollment': ('Inscription introuvable.', 'Enrollment not found.', 'Inscripción no encontrada.', 'لم يُعثَر على التسجيل.', '受講登録が見つかりません。', '未找到报名记录。'),
    'academy.error.lesson': ('Échec de la validation de la leçon.', 'Failed to mark the lesson as completed.', 'Error al validar la lección.', 'فشل التحقق من إكمال الدرس.', 'レッスンの完了登録に失敗しました。', '课时确认失败。'),
    'academy.error.quiz': ('Échec de la soumission du quiz.', 'Failed to submit the quiz.', 'Error al enviar el cuestionario.', 'فشل إرسال الاختبار.', 'クイズの提出に失敗しました。', '测验提交失败。'),
    'academy.error.cert': ('Certificat indisponible.', 'Certificate unavailable.', 'Certificado no disponible.', 'الشهادة غير متاحة.', '証明書を利用できません。', '证书不可用。'),
    'academy.error.courses': ('Impossible de charger le catalogue.', 'Unable to load the catalog.', 'No se puede cargar el catálogo.', 'تعذّر تحميل الكتالوج.', 'カタログを読み込めません。', '无法加载课程目录。'),
    'academy.error.alreadyEnrolled': ('Vous êtes déjà inscrit à ce cours.', 'You are already enrolled in this course.', 'Ya está inscrito en este curso.', 'أنت مُسجَّل بالفعل في هذه الدورة.', 'このコースには既に登録済みです。', '您已报名此课程。'),
    'academy.error.enroll': ("L'inscription a échoué.", 'Enrollment failed.', 'La inscripción ha fallado.', 'فشل التسجيل.', '受講登録に失敗しました。', '报名失败。'),
}
