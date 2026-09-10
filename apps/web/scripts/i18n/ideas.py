# -*- coding: utf-8 -*-
"""Libellés Boîte à idées.

Ouverte à tous pour déposer et soutenir ; l'arbitrage (retenir, écarter,
marquer réalisée, consigner l'impact) demande un rôle. Volontairement sans
dépendance au module Cercle : c'est justement l'écran où déposer une idée
sans qu'un cercle existe.
"""

TRANSLATIONS = {
    'nav.idees': (
        'Boîte à idées', 'Idea box', 'Buzón de ideas',
        'صندوق الأفكار', 'アイデアボックス', '创意箱'),

    'ideas.eyebrow': ('Idées', 'Ideas', 'Ideas', 'أفكار', 'アイデア', '创意'),
    'ideas.title': (
        'Boîte à idées', 'Idea box', 'Buzón de ideas',
        'صندوق الأفكار', 'アイデアボックス', '创意箱'),
    'ideas.subtitle': (
        "Déposez une idée, soutenez-la d'une voix : l'arbitrage retient, écarte ou marque réalisée.",
        'Submit an idea, back it with a vote: the review team accepts, rejects, or marks it implemented.',
        'Presente una idea y apóyela con un voto: la revisión la acepta, la rechaza o la marca como implementada.',
        'أرسل فكرة وادعمها بصوت: تتم مراجعتها فتُقبل أو تُرفض أو تُوضع علامة عليها كمُنفَّذة.',
        'アイデアを投稿し、投票で支持しましょう。審査が採用・却下・実施済みの判定を行います。',
        '提交一个创意并为其投票支持：评审会将其采纳、驳回或标记为已实施。'),

    'ideas.submit-button': (
        'Déposer une idée', 'Submit an idea', 'Presentar una idea',
        'إرسال فكرة', 'アイデアを投稿', '提交创意'),

    'ideas.col-submitted': (
        'Soumise', 'Submitted', 'Presentada', 'مُرسَلة', '提出済み', '已提交'),
    'ideas.col-review': (
        "À l'étude", 'Under review', 'En estudio',
        'قيد المراجعة', '検討中', '审议中'),
    'ideas.col-approved': (
        'En cours', 'In progress', 'En curso', 'قيد التنفيذ', '進行中', '进行中'),
    'ideas.col-done': (
        'Réalisée', 'Implemented', 'Implementada',
        'مُنفَّذة', '実施済み', '已实施'),

    'ideas.column-empty': (
        'Aucune idée ici pour l’instant.', 'No ideas here yet.',
        'Todavía no hay ideas aquí.', 'لا توجد أفكار هنا حتى الآن.',
        'ここにはまだアイデアがありません。', '这里暂时还没有创意。'),

    'ideas.review-button': (
        "À l'étude", 'Under review', 'En estudio', 'قيد المراجعة', '検討中', '审议中'),
    'ideas.approve-button': (
        'Retenir', 'Approve', 'Aceptar', 'قبول', '採用', '采纳'),
    'ideas.reject-button': (
        'Écarter', 'Reject', 'Rechazar', 'رفض', '却下', '驳回'),
    'ideas.implement-button': (
        'Réalisée', 'Implemented', 'Implementada', 'مُنفَّذة', '実施済み', '已实施'),
    'ideas.measure-button': (
        "Consigner l'impact", 'Record the impact', 'Registrar el impacto',
        'تسجيل الأثر', '効果を記録', '记录成效'),

    'ideas.show-rejected': (
        'Voir les idées écartées ({$INTERPOLATION})',
        'See rejected ideas ({$INTERPOLATION})',
        'Ver las ideas rechazadas ({$INTERPOLATION})',
        'عرض الأفكار المرفوضة ({$INTERPOLATION})',
        '却下されたアイデアを見る（{$INTERPOLATION}）',
        '查看被驳回的创意（{$INTERPOLATION}）'),
    'ideas.hide-rejected': (
        'Masquer les idées écartées', 'Hide rejected ideas', 'Ocultar las ideas rechazadas',
        'إخفاء الأفكار المرفوضة', '却下されたアイデアを隠す', '隐藏被驳回的创意'),

    'ideas.vote-aria': (
        'Soutenir {$title} ({$votes} voix)', 'Back {$title} ({$votes} votes)',
        'Apoyar {$title} ({$votes} votos)', 'دعم {$title} ({$votes} صوتًا)',
        '{$title} を支持（{$votes} 票）', '支持 {$title}（{$votes} 票）'),
    'ideas.unvote-aria': (
        'Retirer ma voix de {$title} ({$votes} voix)', 'Withdraw my vote from {$title} ({$votes} votes)',
        'Retirar mi voto de {$title} ({$votes} votos)', 'سحب صوتي من {$title} ({$votes} صوتًا)',
        '{$title} への支持を取り消す（{$votes} 票）', '撤回我对 {$title} 的支持（{$votes} 票）'),

    'ideas.reject-prompt': (
        'Pourquoi cette idée est-elle écartée ?', 'Why is this idea being rejected?',
        '¿Por qué se rechaza esta idea?', 'لماذا تُرفض هذه الفكرة؟',
        'このアイデアはなぜ却下されますか？', '为什么驳回这个创意？'),
    'ideas.reject-submit': (
        'Écarter', 'Reject', 'Rechazar', 'رفض', '却下', '驳回'),
    'ideas.impact-prompt': (
        "Qu'a produit cette idée, une fois en place ?", 'What did this idea produce, once in place?',
        '¿Qué produjo esta idea, una vez implementada?', 'ما الذي أنتجته هذه الفكرة بعد تطبيقها؟',
        '実施後、このアイデアは何をもたらしましたか？', '这个创意实施后带来了什么成效？'),
    'ideas.impact-submit': (
        'Consigner', 'Record', 'Registrar', 'تسجيل', '記録', '记录'),

    'ideas.failed': (
        'Opération impossible sur cette idée.', 'Could not perform this operation on the idea.',
        'No se pudo realizar esta operación sobre la idea.',
        'تعذّر تنفيذ هذه العملية على الفكرة.', 'このアイデアに対する操作を実行できませんでした。',
        '无法对该创意执行此操作。'),

    'ideas.dialog.title': (
        'Déposer une idée', 'Submit an idea', 'Presentar una idea',
        'إرسال فكرة', 'アイデアを投稿', '提交创意'),
    'ideas.dialog.submit': (
        'Déposer', 'Submit', 'Presentar', 'إرسال', '投稿', '提交'),
    'ideas.dialog.field-title': (
        "Titre de l'idée", 'Idea title', 'Título de la idea',
        'عنوان الفكرة', 'アイデアのタイトル', '创意标题'),
    'ideas.dialog.title-placeholder': (
        'Ex. : Un bac de tri près du poste 3', 'E.g.: A sorting bin near station 3',
        'Ej.: Un contenedor de clasificación cerca del puesto 3',
        'مثال: صندوق فرز قرب المحطة 3', '例：3番工程近くの分別ボックス',
        '例如：3号工位附近的分类箱'),
    'ideas.dialog.title-required': (
        "Le titre est requis : c'est ce que chaque colonne affiche.",
        'The title is required: it is what every column displays.',
        'El título es obligatorio: es lo que muestra cada columna.',
        'العنوان مطلوب: فهو ما يُعرض في كل عمود.',
        'タイトルは必須です。各列に表示される内容です。',
        '标题为必填项：这是每一列所显示的内容。'),
    'ideas.dialog.field-description': (
        'Description (facultative)', 'Description (optional)', 'Descripción (opcional)',
        'الوصف (اختياري)', '説明（任意）', '描述（可选）'),
    'ideas.dialog.description-hint': (
        "Ce qui aide l'arbitrage à comprendre l'idée, et son intérêt.",
        'What helps the review team understand the idea, and its value.',
        'Lo que ayuda a la revisión a entender la idea y su valor.',
        'ما يساعد المراجعين على فهم الفكرة وقيمتها.',
        '審査がアイデアとその意義を理解するのに役立つ情報。',
        '帮助评审理解创意及其价值的信息。'),
    'ideas.dialog.blocked-title': (
        "Donnez un titre : c'est ce que chaque colonne affiche.",
        'Give it a title: it is what every column displays.',
        'Indique un título: es lo que muestra cada columna.',
        'أدخل عنوانًا: فهو ما يُعرض في كل عمود.',
        'タイトルを入力してください。各列に表示される内容です。',
        '请填写标题：这是每一列所显示的内容。'),

    'ideas.reject-dialog.field-text': (
        'Votre réponse', 'Your answer', 'Su respuesta', 'إجابتك', '回答', '您的回答'),
    'ideas.reject-dialog.required': (
        'Ce champ est requis pour valider.', 'This field is required to submit.',
        'Este campo es obligatorio para confirmar.', 'هذا الحقل مطلوب للتأكيد.',
        'この項目は送信に必須です。', '此字段为提交所必填。'),
    'ideas.reject-dialog.blocked': (
        'Ce champ est requis pour valider.', 'This field is required to submit.',
        'Este campo es obligatorio para confirmar.', 'هذا الحقل مطلوب للتأكيد.',
        'この項目は送信に必須です。', '此字段为提交所必填。'),
}
