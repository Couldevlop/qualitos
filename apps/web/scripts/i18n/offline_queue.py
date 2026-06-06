# -*- coding: utf-8 -*-
"""Table i18n - domaine offline_queue. id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'offline.queue.title': ("File d'attente offline", 'Offline queue', 'Cola sin conexión', 'قائمة الانتظار دون اتصال', 'オフラインキュー', '离线队列'),
    'offline.queue.subtitle': ("Actions terrain saisies sans réseau, rejouées dans l'ordre au retour de la connexion.", 'Field actions captured without network, replayed in order once the connection returns.', 'Acciones de campo registradas sin red, reproducidas en orden al recuperar la conexión.', 'إجراءات ميدانية سُجِّلت دون شبكة، تُعاد بالترتيب عند عودة الاتصال.', 'ネットワークなしで記録された現場アクション。接続回復時に順番に再実行されます。', '无网络时录入的现场操作，恢复连接后按顺序重放。'),
    'offline.queue.sync-now': ('Synchroniser maintenant', 'Sync now', 'Sincronizar ahora', 'مزامنة الآن', '今すぐ同期', '立即同步'),
    'offline.queue.state-offline': ('Hors-ligne', 'Offline', 'Sin conexión', 'غير متصل', 'オフライン', '离线'),
    'offline.queue.state-online': ('En ligne', 'Online', 'En línea', 'متصل', 'オンライン', '在线'),
    'offline.queue.pending-suffix': ('action(s) en attente de synchronisation', 'action(s) awaiting synchronization', 'acción(es) pendiente(s) de sincronización', 'إجراء(ات) في انتظار المزامنة', '件のアクションが同期待ち', '项操作等待同步'),
    'offline.queue.nothing-pending': ('Aucune action en attente — tout est synchronisé.', 'No pending action — everything is synchronized.', 'Ninguna acción pendiente — todo está sincronizado.', 'لا توجد إجراءات معلّقة — كل شيء متزامن.', '保留中のアクションはありません — すべて同期済みです。', '没有待处理的操作——全部已同步。'),
    'offline.queue.col-action': ('Action', 'Action', 'Acción', 'الإجراء', 'アクション', '操作'),
    'offline.queue.col-queued-at': ('Mise en attente le', 'Queued on', 'En cola desde', 'أُدرج في الانتظار في', 'キュー追加日時', '加入队列时间'),
    'offline.queue.discard-tooltip': ('Abandonner cette action', 'Discard this action', 'Descartar esta acción', 'التخلّي عن هذا الإجراء', 'このアクションを破棄', '放弃此操作'),
    'offline.queue.discard-title': ('Abandonner cette action ?', 'Discard this action?', '¿Descartar esta acción?', 'التخلّي عن هذا الإجراء؟', 'このアクションを破棄しますか？', '放弃此操作？'),
    'offline.queue.discard-message': ('« {$label} » ne sera jamais synchronisée vers le serveur. Cette décision est définitive.', '“{$label}” will never be synchronized to the server. This decision is final.', '«{$label}» nunca se sincronizará con el servidor. Esta decisión es definitiva.', '«{$label}» لن يُزامَن مع الخادم أبداً. هذا القرار نهائي.', '「{$label}」はサーバーに同期されません。この決定は取り消せません。', '「{$label}」将永远不会同步到服务器。此决定不可撤销。'),
    'offline.queue.discard-confirm': ('Abandonner', 'Discard', 'Descartar', 'تخلٍّ', '破棄', '放弃'),
    'offline.queue.discard-keep': ('Conserver', 'Keep', 'Conservar', 'الاحتفاظ', '保持', '保留'),
    'offline.queue.discarded': ('Action abandonnée.', 'Action discarded.', 'Acción descartada.', 'تم التخلّي عن الإجراء.', 'アクションを破棄しました。', '已放弃操作。'),
    'offline.queue.still-offline': ('Toujours hors-ligne — la synchronisation reprendra au retour du réseau.', 'Still offline — synchronization will resume once the network returns.', 'Aún sin conexión — la sincronización se reanudará al volver la red.', 'لا يزال الاتصال مقطوعاً — ستُستأنف المزامنة عند عودة الشبكة.', 'まだオフラインです — ネットワーク回復時に同期が再開されます。', '仍处于离线状态——网络恢复后将继续同步。'),
    'offline.queue.sync-done': ('Synchronisation terminée.', 'Synchronization complete.', 'Sincronización finalizada.', 'اكتملت المزامنة.', '同期が完了しました。', '同步完成。'),
    'offline.queue.empty-title': ('File vide', 'Empty queue', 'Cola vacía', 'قائمة الانتظار فارغة', 'キューは空です', '队列为空'),
    'offline.queue.empty-hint': ("Les audits 5S saisis hors connexion apparaîtront ici jusqu'à leur synchronisation.", '5S audits captured offline will appear here until they are synchronized.', 'Las auditorías 5S registradas sin conexión aparecerán aquí hasta su sincronización.', 'ستظهر تدقيقات 5S المسجّلة دون اتصال هنا حتى تتم مزامنتها.', 'オフラインで記録された5S監査は、同期されるまでここに表示されます。', '离线录入的5S审核将显示在此处，直至完成同步。'),
}
