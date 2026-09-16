# -*- coding: utf-8 -*-
"""Table i18n - vérification d'efficacité exigée sur un dossier CAPA.

L'EXIGENCE de vérification, à distinguer du constat qu'elle a eu lieu : sans
elle, rien ne sépare un dossier qu'on a délibérément choisi de ne pas vérifier
d'un dossier qu'on a simplement oublié de vérifier.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # « Oui » et « Non » manquaient au socle commun : cette question fermée est
    # la première à en avoir besoin, et deux autres écrans les réutiliseront.
    'common.yes': ('Oui', 'Yes', 'Sí', 'نعم', 'はい', '是'),
    'common.no': ('Non', 'No', 'No', 'لا', 'いいえ', '否'),

    'capa.verification.title': (
        "Vérification d'efficacité", 'Effectiveness verification',
        'Verificación de la eficacia', 'التحقق من الفعالية',
        '有効性の検証', '有效性验证'),
    'capa.verification.hint': (
        "Une vérification est-elle exigée sur ce dossier ? Répondre « non » est une "
        "décision, qui se lit ; ne pas répondre laisse la question ouverte.",
        'Is a verification required on this case? Answering "no" is a decision, and it '
        'shows; leaving it unanswered leaves the question open.',
        '¿Se exige una verificación en este expediente? Responder «no» es una decisión, '
        'y se lee; no responder deja la pregunta abierta.',
        'هل يُشترط التحقق في هذا الملف؟ الإجابة بـ«لا» قرار يُقرأ، أما عدم الإجابة '
        'فيُبقي السؤال مفتوحاً.',
        'この案件で検証は必要ですか。「いいえ」も記録に残る判断です。未回答のままだと'
        '問いは開いたままになります。',
        '本案卷是否要求验证？回答「否」是一项会被读到的决定；不作答则问题仍然悬而未决。'),
    'capa.verification.required': (
        'Exigée', 'Required', 'Exigida', 'مطلوب', '必要', '要求'),
    'capa.verification.not-required': (
        'Non exigée', 'Not required', 'No exigida', 'غير مطلوب', '不要', '不要求'),
    'capa.verification.assignee': (
        'Vérification confiée à', 'Verification assigned to',
        'Verificación asignada a', 'التحقق مُسند إلى',
        '検証の担当者', '验证指派给'),
    'capa.verification.assignee-required': (
        "Une vérification exigée doit être confiée à quelqu'un.",
        'A required verification must be assigned to someone.',
        'Una verificación exigida debe asignarse a alguien.',
        'التحقق المطلوب يجب إسناده إلى شخص ما.',
        '必要とされた検証は、担当者を定めなければなりません。',
        '被要求的验证必须指派给某个人。'),
    'capa.verification.directory-down': (
        "L'annuaire de l'organisation n'a pas répondu : la liste est vide, et le "
        "dossier ne pourra pas être enregistré avec une vérification exigée.",
        'The organisation directory did not answer: the list is empty, and the case '
        'cannot be saved with a required verification.',
        'El directorio de la organización no respondió: la lista está vacía y el '
        'expediente no podrá guardarse con una verificación exigida.',
        'لم يستجب دليل المؤسسة: القائمة فارغة، ولا يمكن حفظ الملف مع اشتراط التحقق.',
        '組織のディレクトリが応答しませんでした。一覧は空で、検証を必要とした状態では'
        '案件を保存できません。',
        '组织目录没有响应：列表为空，且在要求验证的状态下无法保存本案卷。'),
    'capa.verification.instructions': (
        'Instructions de vérification', 'Verification instructions',
        'Instrucciones de verificación', 'تعليمات التحقق',
        '検証手順', '验证说明'),
    'capa.verification.instructions-placeholder': (
        "Ce qu'il faut vérifier, et comment : échantillon, délai, critère d'acceptation…",
        'What to verify, and how: sample, lead time, acceptance criterion…',
        'Qué verificar y cómo: muestra, plazo, criterio de aceptación…',
        'ما الذي يجب التحقق منه وكيف: العينة، المهلة، معيار القبول…',
        '何を、どう検証するか：サンプル、期限、合否基準など',
        '验证什么、如何验证：样本、时限、接受准则……'),
}
