# -*- coding: utf-8 -*-
"""Table i18n - panneau de prévision dans la fiche de l'indicateur (§6.5). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'forecast.panel.title': ('Prévision', 'Forecast', 'Previsión', 'التوقّع', '予測', '预测'),

    'forecast.panel.intro': (
        "Au rythme des mesures enregistrées, l'indicateur atteindra-t-il sa cible ? Le lissage exponentiel prolonge le niveau et la tendance de la série ; la bande grise dit l'incertitude à 95 %. Une projection n'est pas un constat : elle n'ouvre ni action corrective ni cycle d'amélioration d'elle-même.",
        'At the pace of the recorded measurements, will the indicator reach its target? Exponential smoothing extends the level and trend of the series; the grey band shows the 95 % uncertainty. A projection is not a finding: it opens neither a corrective action nor an improvement cycle by itself.',
        '¿Al ritmo de las mediciones registradas, el indicador alcanzará su objetivo? El suavizado exponencial prolonga el nivel y la tendencia de la serie; la banda gris indica la incertidumbre al 95 %. Una proyección no es un hecho constatado: no abre por sí misma ninguna acción correctiva ni ciclo de mejora.',
        'بالوتيرة الحالية للقياسات المسجّلة، هل سيبلغ المؤشر هدفه؟ يمدّ التنعيم الأسي مستوى السلسلة واتجاهها، ويبيّن النطاق الرمادي حدود عدم اليقين عند 95 %. التوقّع ليس واقعة مُثبتة: فهو لا يفتح من تلقاء نفسه إجراءً تصحيحيًا ولا دورة تحسين.',
        '記録された測定値のペースでは、この指標は目標に到達するでしょうか。指数平滑により系列の水準とトレンドを延長し、灰色の帯で 95 % の不確実性を示します。予測は確認された事実ではなく、それ自体で是正処置や改善サイクルを起票することはありません。',
        '按已登记测量值的节奏，该指标能否达到目标？指数平滑会延伸序列的水平与趋势，灰色区间表示 95 % 的不确定性。预测不是既成事实：它本身既不开启纠正措施，也不开启改进循环。'),

    'forecast.panel.no-target': (
        "Cet indicateur n'a pas de valeur cible : renseignez-la pour obtenir une prévision.",
        'This indicator has no target value: set one to obtain a forecast.',
        'Este indicador no tiene valor objetivo: indíquelo para obtener una previsión.',
        'لا توجد قيمة مستهدفة لهذا المؤشر: حدّدها للحصول على توقّع.',
        'この指標には目標値がありません。予測を得るには目標値を設定してください。',
        '该指标没有目标值：请先设置目标值以获得预测。'),

    'forecast.panel.too-few': (
        '{$count} mesure(s) enregistrée(s) : il en faut au moins {$min} pour projeter une tendance.',
        '{$count} measurement(s) recorded: at least {$min} are needed to project a trend.',
        '{$count} medición(es) registrada(s): se necesitan al menos {$min} para proyectar una tendencia.',
        'عدد القياسات المسجّلة {$count}: يلزم {$min} على الأقل لإسقاط اتجاه.',
        '登録済みの測定値は {$count} 件です。トレンドを延長するには {$min} 件以上必要です。',
        '已登记 {$count} 个测量值：至少需要 {$min} 个才能延伸趋势。'),

    'forecast.panel.measures-error': (
        'Impossible de lire les mesures de cet indicateur.',
        'Unable to read the measurements of this indicator.',
        'No se pudieron leer las mediciones de este indicador.',
        'تعذّر قراءة قياسات هذا المؤشر.',
        'この指標の測定値を読み取れませんでした。',
        '无法读取该指标的测量值。'),

    'forecast.panel.card-probability': (
        "Probabilité d'atteinte", 'Probability of reaching the target', 'Probabilidad de alcanzar el objetivo',
        'احتمال بلوغ الهدف', '目標到達確率', '达到目标的概率'),
    'forecast.panel.card-confidence': ('Confiance', 'Confidence', 'Confianza', 'الثقة', '信頼度', '置信度'),
    'forecast.panel.card-model': ('Modèle', 'Model', 'Modelo', 'النموذج', 'モデル', '模型'),

    'forecast.panel.points': ('mesures', 'measurements', 'mediciones', 'قياسات', '件の測定値', '个测量值'),
    'forecast.panel.slope': ('tendance', 'trend', 'tendencia', 'الاتجاه', 'トレンド', '趋势'),
    'forecast.panel.period': ('période', 'period', 'periodo', 'فترة', '期', '期'),

    'forecast.panel.dir-at-least': (
        'atteindre au moins', 'reach at least', 'alcanzar al menos',
        'بلوغ ما لا يقل عن', '少なくとも到達', '至少达到'),
    'forecast.panel.dir-at-most': (
        'ne pas dépasser', 'not exceed', 'no superar',
        'عدم تجاوز', '超えない', '不超过'),

    'forecast.panel.model-hw': (
        'Holt-Winters (saisonnier)', 'Holt-Winters (seasonal)', 'Holt-Winters (estacional)',
        'Holt-Winters (موسمي)', 'Holt-Winters（季節性）', 'Holt-Winters（季节性）'),
    'forecast.panel.model-holt': (
        'Holt (niveau + tendance)', 'Holt (level + trend)', 'Holt (nivel + tendencia)',
        'Holt (المستوى + الاتجاه)', 'Holt（水準＋トレンド）', 'Holt（水平＋趋势）'),

    'forecast.panel.table-caption': ('Points projetés', 'Projected points', 'Puntos proyectados',
                                     'النقاط المُسقطة', '予測点', '预测点'),
    'forecast.panel.col-step': ('Horizon', 'Horizon', 'Horizonte', 'الأفق', '予測期間', '预测期'),
    'forecast.panel.col-value': ('Prévision', 'Forecast', 'Previsión', 'التوقّع', '予測値', '预测值'),
    'forecast.panel.col-interval': ('Intervalle 95 %', '95 % interval', 'Intervalo del 95 %',
                                    'نطاق 95 %', '95 % 区間', '95 % 区间'),

    'forecast.panel.err-backend': (
        'Service de prévision injoignable.', 'Forecast service unreachable.',
        'Servicio de previsión inaccesible.', 'خدمة التوقّع غير متاحة.',
        '予測サービスに接続できません。', '无法连接预测服务。'),
    'forecast.panel.err-unprocessable': (
        'Série insuffisante pour une prévision fiable.', 'Series insufficient for a reliable forecast.',
        'Serie insuficiente para una previsión fiable.', 'السلسلة غير كافية لتوقّع موثوق.',
        '信頼できる予測に足るデータがありません。', '数据不足，无法给出可靠预测。'),
    'forecast.panel.err-quota': (
        'Débit IA dépassé pour ce client — réessayez plus tard.',
        'AI rate limit exceeded for this client — try again later.',
        'Límite de uso de IA superado para este cliente: inténtelo más tarde.',
        'تم تجاوز حدّ استخدام الذكاء الاصطناعي لهذا العميل — أعد المحاولة لاحقًا.',
        'このお客様の AI 利用上限を超えました。しばらくしてから再試行してください。',
        '该客户的 AI 调用速率已超限 — 请稍后重试。'),
    'forecast.panel.err-unavailable': (
        'Service de prévision momentanément indisponible.',
        'Forecast service temporarily unavailable.',
        'Servicio de previsión temporalmente no disponible.',
        'خدمة التوقّع غير متوفرة مؤقتًا.',
        '予測サービスは一時的に利用できません。',
        '预测服务暂时不可用。'),
    'forecast.panel.err-generic': (
        'Échec de la prévision (HTTP {$status}).', 'Forecast failed (HTTP {$status}).',
        'Falló la previsión (HTTP {$status}).', 'فشل التوقّع (HTTP {$status}).',
        '予測に失敗しました（HTTP {$status}）。', '预测失败（HTTP {$status}）。'),
}
