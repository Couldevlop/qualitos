# -*- coding: utf-8 -*-
"""Table i18n - prevision de sortie des limites SPC (§6.5). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'spc.forecast.panel': ('Prévision de dérive', 'Drift forecast', 'Previsión de deriva', 'توقّع الانحراف', 'ドリフト予測', '漂移预测'),
    'spc.forecast.horizon': ('Horizon (périodes)', 'Horizon (periods)', 'Horizonte (periodos)', 'الأفق (الفترات)', '予測期間（期）', '预测范围（期）'),
    'spc.forecast.open-capa': ('Ouvrir une CAPA préventive si une sortie est annoncée', 'Open a preventive CAPA if a breach is forecast', 'Abrir una CAPA preventiva si se prevé una salida', 'فتح CAPA وقائي إذا كان الخروج متوقعاً', '逸脱が予測された場合に予防CAPAを起票する', '若预测将越限则创建预防性CAPA'),
    'spc.forecast.run': ('Prévoir la dérive', 'Forecast drift', 'Prever la deriva', 'توقّع الانحراف', 'ドリフトを予測', '预测漂移'),
    'spc.forecast.running': ('Prévision…', 'Forecasting…', 'Previendo…', 'جارٍ التوقّع…', '予測中…', '预测中…'),
    'spc.forecast.no-breach': ('Aucune sortie des limites annoncée sur les {$horizon} prochaines périodes.', 'No breach of the control limits forecast over the next {$horizon} periods.', 'No se prevé ninguna salida de los límites en los próximos {$horizon} periodos.', 'لا يُتوقَّع خروج عن حدود التحكم خلال الفترات {$horizon} القادمة.', '今後 {$horizon} 期間において管理限界の逸脱は予測されていません。', '未来 {$horizon} 期内预计不会超出控制限。'),
    'spc.forecast.breach-upper': ('Le procédé franchira sa limite supérieure dans {$steps} période(s).', 'The process will cross its upper limit in {$steps} period(s).', 'El proceso superará su límite superior en {$steps} periodo(s).', 'ستتجاوز العملية حدّها الأعلى خلال {$steps} فترة.', '工程は {$steps} 期間後に上限を超えます。', '过程将在 {$steps} 期后超出上限。'),
    'spc.forecast.breach-lower': ('Le procédé franchira sa limite inférieure dans {$steps} période(s).', 'The process will cross its lower limit in {$steps} period(s).', 'El proceso superará su límite inferior en {$steps} periodo(s).', 'ستتجاوز العملية حدّها الأدنى خلال {$steps} فترة.', '工程は {$steps} 期間後に下限を下回ります。', '过程将在 {$steps} 期后低于下限。'),
    'spc.forecast.conf-high': ('Confiance élevée', 'High confidence', 'Confianza alta', 'ثقة عالية', '信頼度：高', '高置信度'),
    'spc.forecast.conf-medium': ('Confiance moyenne', 'Medium confidence', 'Confianza media', 'ثقة متوسطة', '信頼度：中', '中等置信度'),
    'spc.forecast.conf-low': ('Confiance faible', 'Low confidence', 'Confianza baja', 'ثقة منخفضة', '信頼度：低', '低置信度'),
    'spc.forecast.trend': ('{$trend} par période', '{$trend} per period', '{$trend} por periodo', '{$trend} لكل فترة', '{$trend} / 期', '{$trend} 每期'),
    'spc.forecast.points-used': ('mesures utilisées', 'measurements used', 'mediciones utilizadas', 'قياسات مستخدمة', '件の測定値を使用', '个测量值已使用'),
    'spc.forecast.capa-link': ('CAPA préventive ouverte — voir le dossier', 'Preventive CAPA opened — view record', 'CAPA preventiva abierta — ver expediente', 'تم فتح CAPA وقائي — عرض الملف', '予防CAPA起票済み — 記録を表示', '已创建预防性CAPA — 查看档案'),
    'spc.forecast.col-step': ('Période', 'Period', 'Periodo', 'الفترة', '期', '期'),
    'spc.forecast.col-value': ('Valeur projetée', 'Projected value', 'Valor proyectado', 'القيمة المتوقعة', '予測値', '预测值'),
    'spc.forecast.col-interval': ('Intervalle', 'Interval', 'Intervalo', 'المجال', '区間', '区间'),
    'spc.forecast.col-risk': ('Risque de sortie', 'Breach risk', 'Riesgo de salida', 'خطر الخروج', '逸脱リスク', '越限风险'),
    'spc.forecast.chart-projection': ('Projection', 'Projection', 'Proyección', 'الإسقاط', '予測', '预测'),
    'spc.forecast.err-min-four': ('Au moins 4 mesures sont nécessaires pour prévoir une dérive.', 'At least 4 measurements are needed to forecast a drift.', 'Se necesitan al menos 4 mediciones para prever una deriva.', 'يلزم 4 قياسات على الأقل لتوقّع الانحراف.', 'ドリフトの予測には測定値が4件以上必要です。', '预测漂移至少需要4个测量值。'),
}
