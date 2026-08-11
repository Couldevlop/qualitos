# -*- coding: utf-8 -*-
"""Table i18n - planning des audits et rappel d'echeance (SS4.4). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    # --- entree de navigation ---
    'nav.audits-planning': (
        'Planning audits', 'Audit schedule', 'Planificación de auditorías',
        'جدول عمليات التدقيق', '監査スケジュール', '审核计划'),

    # --- en-tete de la page ---
    'audits.planning.title': (
        'Planning des audits', 'Audit schedule', 'Planificación de auditorías',
        'جدول عمليات التدقيق', '監査スケジュール', '审核计划'),
    'audits.planning.subtitle': (
        "Les audits à venir, du plus proche au plus lointain. Un rappel part "
        "automatiquement 30 jours avant l'échéance.",
        'Upcoming audits, nearest first. A reminder goes out automatically 30 days '
        'before the due date.',
        'Las auditorías previstas, de la más próxima a la más lejana. Un recordatorio '
        'se envía automáticamente 30 días antes del vencimiento.',
        'عمليات التدقيق القادمة، من الأقرب إلى الأبعد. يُرسل تذكير تلقائيًا قبل 30 يومًا من الموعد.',
        '今後の監査を期日が近い順に表示します。期日の30日前に自動でリマインダーが送信されます。',
        '即将到来的审核，按期限由近及远排列。系统会在到期前 30 天自动发送提醒。'),
    'audits.planning.back-to-list': (
        'Tous les plans', 'All plans', 'Todos los planes',
        'جميع الخطط', 'すべての計画', '全部计划'),

    # --- filtres ---
    'audits.planning.filter-type': (
        "Type d'audit", 'Audit type', 'Tipo de auditoría',
        'نوع التدقيق', '監査の種類', '审核类型'),
    'audits.planning.filter-horizon': (
        'Horizon', 'Horizon', 'Horizonte', 'الأفق الزمني', '対象期間', '时间范围'),
    'audits.planning.horizon-days': (
        '{$INTERPOLATION} jours', '{$INTERPOLATION} days', '{$INTERPOLATION} días',
        '{$INTERPOLATION} يومًا', '{$INTERPOLATION} 日間', '{$INTERPOLATION} 天'),

    # --- compteurs d'en-tete ---
    'audits.planning.count-overdue': (
        '{$INTERPOLATION} en retard', '{$INTERPOLATION} overdue', '{$INTERPOLATION} atrasadas',
        '{$INTERPOLATION} متأخرة', '{$INTERPOLATION} 件が期限超過', '{$INTERPOLATION} 项已逾期'),
    'audits.planning.count-approaching': (
        '{$INTERPOLATION} à moins de 30 jours', '{$INTERPOLATION} within 30 days',
        '{$INTERPOLATION} a menos de 30 días', '{$INTERPOLATION} خلال أقل من 30 يومًا',
        '{$INTERPOLATION} 件が30日以内', '{$INTERPOLATION} 项在 30 天内'),

    # --- colonnes ---
    'audits.planning.col-due': (
        'Échéance', 'Due date', 'Vencimiento', 'الموعد', '期日', '到期日'),
    'audits.planning.col-countdown': (
        'Décompte', 'Countdown', 'Cuenta atrás', 'العد التنازلي', 'カウントダウン', '倒计时'),
    'audits.planning.col-reminder': (
        'Rappel', 'Reminder', 'Recordatorio', 'التذكير', 'リマインダー', '提醒'),

    # --- decompte (le retard s'ecrit en positif : un signe moins se confond avec
    #     le tiret d'une valeur absente au milieu d'un tableau) ---
    'audits.planning.today': (
        "Aujourd'hui", 'Today', 'Hoy', 'اليوم', '本日', '今天'),
    'audits.planning.tomorrow': (
        'Demain', 'Tomorrow', 'Mañana', 'غدًا', '明日', '明天'),
    'audits.planning.in-days': (
        'Dans {$days} jours', 'In {$days} days', 'En {$days} días',
        'خلال {$days} يومًا', '{$days} 日後', '{$days} 天后'),
    'audits.planning.late-one': (
        '1 jour de retard', '1 day overdue', '1 día de retraso',
        'متأخر بيوم واحد', '1 日超過', '逾期 1 天'),
    'audits.planning.late-many': (
        '{$days} jours de retard', '{$days} days overdue', '{$days} días de retraso',
        'متأخر بـ {$days} يومًا', '{$days} 日超過', '逾期 {$days} 天'),

    # --- etat du rappel ---
    'audits.planning.reminder-sent': (
        'Envoyé', 'Sent', 'Enviado', 'أُرسل', '送信済み', '已发送'),
    'audits.planning.reminder-pending': (
        'En attente', 'Pending', 'Pendiente', 'قيد الانتظار', '未送信', '待发送'),

    'audits.planning.empty': (
        'Aucun audit planifié sur cet horizon.',
        'No audit scheduled within this horizon.',
        'Ninguna auditoría planificada en este horizonte.',
        'لا توجد عمليات تدقيق مجدولة ضمن هذا الأفق.',
        'この期間に予定されている監査はありません。',
        '此时间范围内没有已安排的审核。'),

    # --- destinataire du rappel (dialogue de creation) ---
    'audits.create.reminder-email': (
        'Courriel de rappel (optionnel)', 'Reminder email (optional)',
        'Correo de recordatorio (opcional)', 'بريد التذكير (اختياري)',
        'リマインダーのメール（任意）', '提醒邮箱（可选）'),
    'audits.create.reminder-email-placeholder': (
        'Ex. : qualite@monentreprise.fr', 'e.g. quality@mycompany.com',
        'Ej.: calidad@miempresa.es', 'مثال: quality@mycompany.com',
        '例：quality@example.co.jp', '例：quality@mycompany.cn'),
    'audits.create.reminder-email-hint': (
        "Un rappel part 30 jours avant l'échéance. Sans adresse, il reste dans l'application.",
        'A reminder goes out 30 days before the due date. Without an address it stays in the app.',
        'Se envía un recordatorio 30 días antes del vencimiento. Sin dirección, permanece en la aplicación.',
        'يُرسل تذكير قبل 30 يومًا من الموعد. بدون عنوان، يبقى داخل التطبيق فقط.',
        '期日の30日前にリマインダーを送信します。アドレスがない場合はアプリ内のみに通知されます。',
        '系统会在到期前 30 天发送提醒。未填写邮箱时，提醒仅保留在应用内。'),
    'audits.create.reminder-email-invalid': (
        'Adresse de courriel invalide.', 'Invalid email address.',
        'Dirección de correo no válida.', 'عنوان بريد إلكتروني غير صالح.',
        'メールアドレスが正しくありません。', '邮箱地址无效。'),
    # --- edition du destinataire du rappel -----------------------------------
    'audits.edit.reminder-email-hint': (
        'Videz le champ pour ne plus envoyer de rappel par courriel.',
        'Clear the field to stop sending email reminders.',
        'Vacíe el campo para dejar de enviar recordatorios por correo.',
        'أفرغ الحقل لإيقاف إرسال التذكير بالبريد.',
        'メール通知を止めるには、この欄を空にしてください。',
        '清空该字段即可停止发送邮件提醒。'),
}
