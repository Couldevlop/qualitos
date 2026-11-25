# -*- coding: utf-8 -*-
"""Table i18n - le registre CAPA filtre par echeance limite.

Trois phrases : l'intitule du filtre, l'ouverture du calendrier et le retrait
du filtre. « Qu'est-ce qui tombe cette semaine » est la question qu'on pose a un
registre d'actions, et elle se tranchait a l'oeil, colonne par colonne, sur une
liste paginee.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    'capa.list.filter-due-before': (
        'Échéance avant le', 'Due on or before', 'Vencimiento hasta el',
        'تاريخ الاستحقاق قبل', '期限が次の日以前', '截止日期不晚于'),
    'capa.list.filter-due-open': (
        'Ouvrir le calendrier', 'Open the calendar', 'Abrir el calendario',
        'افتح التقويم', 'カレンダーを開く', '打开日历'),
    'capa.list.filter-due-clear': (
        "Retirer le filtre d'échéance", 'Remove the due-date filter',
        'Quitar el filtro de vencimiento', 'إزالة تصفية تاريخ الاستحقاق',
        '期限の絞り込みを解除', '清除截止日期筛选'),
}
