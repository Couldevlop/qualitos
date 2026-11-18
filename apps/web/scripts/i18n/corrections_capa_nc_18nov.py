# -*- coding: utf-8 -*-
"""Table i18n - lot de corrections CAPA / ecarts du 18 novembre 2026.

Quatre phrases neuves : le refus d'une conclusion de cause racine ecrite depuis
les figures d'analyse, le filtre par pilote du registre CAPA, et le motif qui
retenait la resolution d'un ecart sans qu'aucun mot ne le dise.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- la cause racine, conclue depuis les figures (D4, fiche de l ecart) ---
    'nc.cause.root-cause-failed': (
        "La conclusion n’a pas été enregistrée. Réessayez.",
        'The conclusion was not saved. Try again.',
        'La conclusión no se ha guardado. Inténtelo de nuevo.',
        'لم يتم حفظ الخلاصة. أعد المحاولة.',
        '結論を保存できませんでした。もう一度お試しください。',
        '结论未保存，请重试。'),

    # --- le registre CAPA, filtre par pilote -------------------------------
    'capa.list.filter-owner': (
        'Filtrer par pilote', 'Filter by owner', 'Filtrar por responsable',
        'تصفية حسب المسؤول', '担当者で絞り込む', '按负责人筛选'),
    'capa.list.filter-owner-placeholder': (
        'Toutes les personnes', 'Everyone', 'Todas las personas',
        'كل الأشخاص', 'すべての担当者', '所有人员'),

    # --- un ecart qu'aucune action ne traite -------------------------------
    'nc.blocker.no-capa': (
        "Aucune action n'est encore inscrite sur cet écart. Ajoutez au moins une action pour pouvoir le résoudre.",
        'No action is recorded on this non-conformity yet. Add at least one action before resolving it.',
        'Aún no hay ninguna acción registrada en esta no conformidad. Añada al menos una acción para poder resolverla.',
        'لا يوجد أي إجراء مسجَّل على عدم المطابقة هذا بعد. أضف إجراءً واحدًا على الأقل حتى تتمكن من معالجته.',
        'この不適合にはまだ処置が登録されていません。解決するには少なくとも一つの処置を追加してください。',
        '该不合格项尚未登记任何措施。请至少添加一项措施才能解决。'),
}
