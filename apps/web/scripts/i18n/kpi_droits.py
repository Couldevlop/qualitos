# -*- coding: utf-8 -*-
"""Table i18n - les droits sur les indicateurs (ADR 0078).

Definir un indicateur, c'est fixer sa cible et ses seuils : un acte de pilotage.
Consigner un releve, non -- c'est un fait, rapporte par celui qui a mesure. D'ou
deux actions distinctes dans la matrice des droits, et les mots qui les nomment.

Au passage, « parties interessees » manquait a la liste des modules et son action
s'affichait en code brut dans l'ecran d'administration.

id: (fr, en, es, ar, ja, zh)
"""

TRANSLATIONS = {
    # --- la matrice des droits ---------------------------------------------
    'authz.perm.kpi-manage': (
        'Définir les indicateurs, leurs cibles et leurs seuils',
        'Define the indicators, their targets and their thresholds',
        'Definir los indicadores, sus objetivos y sus umbrales',
        'تحديد المؤشرات وأهدافها وحدودها',
        '指標・目標値・しきい値を定義する',
        '定义指标及其目标值和阈值'),
    'authz.perm.kpi-measure': (
        "Consigner un relevé d'indicateur", 'Record an indicator reading',
        'Registrar una medición de indicador', 'تسجيل قراءة مؤشر',
        '指標の実測値を記録する', '记录指标实测值'),
    'authz.module.kpi': (
        'Indicateurs', 'Indicators', 'Indicadores', 'المؤشرات', '指標', '指标'),
    'authz.perm.stakeholder-manage': (
        'Tenir le registre des parties intéressées',
        'Keep the register of interested parties',
        'Mantener el registro de las partes interesadas',
        'إدارة سجل الأطراف المعنية',
        '利害関係者の登録簿を管理する',
        '维护相关方登记册'),
    'authz.module.stakeholder': (
        'Parties intéressées', 'Interested parties', 'Partes interesadas',
        'الأطراف المعنية', '利害関係者', '相关方'),

    # --- la fiche d'un indicateur -------------------------------------------
    'kpis.detail.measurements-empty-readonly': (
        "Aucune mesure pour l'instant.", 'No reading yet.',
        'Ninguna medición por ahora.', 'لا توجد قراءة حتى الآن.',
        'まだ実測値がありません。', '暂无实测值。'),
}
