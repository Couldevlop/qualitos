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
    # À l'ouverture d'un dossier (ADR 0088), la question se tranche : la même
    # décision de clôture, d'où qu'on ouvre la CAPA.
    'capa.verification.hint-mandatory': (
        "Une vérification est-elle exigée sur ce dossier ? Il faut trancher pour "
        "l'ouvrir : répondre « non » est une décision, qui se lit.",
        'Is a verification required on this case? You must decide to open it: '
        'answering "no" is a decision, and it shows.',
        '¿Se exige una verificación en este expediente? Hay que decidir para abrirlo: '
        'responder «no» es una decisión, y se lee.',
        'هل يُشترط التحقق في هذا الملف؟ يجب الحسم لفتحه: الإجابة بـ«لا» قرار يُقرأ.',
        'この案件で検証は必要ですか。開くには判断が必要です。「いいえ」も記録に残る判断です。',
        '本案卷是否要求验证？必须作出决定才能开立：回答「否」是一项会被读到的决定。'),
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
