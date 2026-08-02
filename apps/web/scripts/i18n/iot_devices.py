# -*- coding: utf-8 -*-
"""Table i18n - IoT & Edge Connectivity (SS9). id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {

    # --- action ------------------------------------------------------
    'iot.action.activate': ('Mettre en service', 'Put in service', 'Poner en servicio', 'تشغيل الجهاز', '稼働させる', '投入运行'),
    'iot.action.decommission': ('Décommissionner', 'Decommission', 'Dar de baja', 'إيقاف نهائي', '廃止する', '退役'),
    'iot.action.suspend': ('Suspendre', 'Suspend', 'Suspender', 'تعليق', '一時停止', '暂停'),

    # --- age ---------------------------------------------------------
    'iot.age.never': ('Jamais', 'Never', 'Nunca', 'أبدًا', 'なし', '从未'),
    'iot.age.now': ("À l'instant", 'Just now', 'Ahora mismo', 'الآن', 'たった今', '刚刚'),

    # --- iot ---------------------------------------------------------
    'iot.col-device': ('Équipement', 'Device', 'Equipo', 'الجهاز', '機器', '设备'),
    'iot.col-health': ('Dernier signal', 'Last signal', 'Última señal', 'آخر إشارة', '最終信号', '最后信号'),
    'iot.col-location': ('Emplacement', 'Location', 'Ubicación', 'الموقع', '設置場所', '位置'),
    'iot.col-protocol': ('Protocole', 'Protocol', 'Protocolo', 'البروتوكول', 'プロトコル', '协议'),
    'iot.col-telemetry': ('Mesures', 'Measurements', 'Mediciones', 'القياسات', '測定値', '测量数'),

    # --- criticity ---------------------------------------------------
    'iot.criticity.critical': ('Critique', 'Critical', 'Crítica', 'حرجة', '重大', '严重'),
    'iot.criticity.high': ('Élevée', 'High', 'Alta', 'عالية', '高', '高'),
    'iot.criticity.low': ('Faible', 'Low', 'Baja', 'منخفضة', '低', '低'),
    'iot.criticity.medium': ('Moyenne', 'Medium', 'Media', 'متوسطة', '中', '中'),

    # --- detail ------------------------------------------------------
    'iot.detail.add-telemetry': ('Relever une mesure', 'Record a measurement', 'Registrar una medición', 'تسجيل قياس', '測定値を記録', '记录一次测量'),
    'iot.detail.back': ('Retour au parc IoT', 'Back to the IoT fleet', 'Volver al parque IoT', 'العودة إلى أسطول إنترنت الأشياء', 'IoT機器一覧に戻る', '返回物联网设备列表'),
    'iot.detail.chart-error': ('Impossible de charger la fenêtre de mesures.', 'Unable to load the measurement window.', 'No se ha podido cargar la ventana de mediciones.', 'تعذّر تحميل نافذة القياسات.', '測定ウィンドウを読み込めませんでした。', '无法加载该测量时间窗。'),
    'iot.detail.decommission-message': ("L'état « décommissionné » est définitif : aucune remise en service, aucune nouvelle mesure. L'historique reste consultable.", 'The "decommissioned" state is final: no return to service, no new measurement. History remains available.', 'El estado «dado de baja» es definitivo: sin vuelta al servicio ni nuevas mediciones. El historial sigue disponible.', 'حالة «مُوقَف نهائيًا» نهائية: لا إعادة تشغيل ولا قياسات جديدة. يبقى السجل متاحًا للاطلاع.', '「廃止済み」は最終状態です。再稼働も新しい測定もできません。履歴は引き続き参照できます。', '“已退役”为终态：无法重新投入运行，也不再接收新测量。历史记录仍可查阅。'),
    'iot.detail.decommission-title': ("Décommissionner l'équipement ?", 'Decommission the device?', '¿Dar de baja el equipo?', 'هل تريد إيقاف الجهاز نهائيًا؟', '機器を廃止しますか？', '要退役该设备吗？'),
    'iot.detail.decommissioned-note': ("Équipement décommissionné : l'historique reste consultable, plus aucune mesure ni modification n'est acceptée.", 'Decommissioned device: history remains available, no further measurement or change is accepted.', 'Equipo dado de baja: el historial sigue disponible, no se acepta ninguna medición ni modificación.', 'جهاز مُوقَف نهائيًا: يبقى السجل متاحًا، ولا يُقبل أي قياس أو تعديل.', '廃止済みの機器です。履歴は参照できますが、測定値の追加も変更も受け付けません。', '已退役设备：历史记录仍可查阅，但不再接受任何测量或修改。'),
    'iot.detail.delete-failed': ('Suppression impossible.', 'Deletion failed.', 'No se ha podido eliminar.', 'تعذّر الحذف.', '削除できませんでした。', '无法删除。'),
    'iot.detail.delete-message': ("Suppression définitive de l'équipement du registre. Les mesures déjà ingérées ne sont plus rattachées à aucun capteur.", 'Permanent removal of the device from the registry. Measurements already ingested are no longer attached to any sensor.', 'Eliminación definitiva del equipo del registro. Las mediciones ya ingeridas dejan de estar asociadas a ningún sensor.', 'حذف نهائي للجهاز من السجل. لن تبقى القياسات المستلمة مرتبطة بأي مستشعر.', '登録簿から機器を完全に削除します。取り込み済みの測定値はどのセンサーにも紐づかなくなります。', '将设备从注册表中永久删除。已接入的测量数据将不再关联任何传感器。'),
    'iot.detail.delete-title': ("Supprimer l'équipement ?", 'Delete the device?', '¿Eliminar el equipo?', 'هل تريد حذف الجهاز؟', '機器を削除しますか？', '要删除该设备吗？'),
    'iot.detail.deleted': ('Équipement supprimé.', 'Device deleted.', 'Equipo eliminado.', 'تم حذف الجهاز.', '機器を削除しました。', '设备已删除。'),
    'iot.detail.last-signal': ('Dernier signal', 'Last signal', 'Última señal', 'آخر إشارة', '最終信号', '最后信号'),
    'iot.detail.load-error': ('Équipement introuvable.', 'Device not found.', 'Equipo no encontrado.', 'الجهاز غير موجود.', '機器が見つかりません。', '未找到该设备。'),
    'iot.detail.metrics-count': ('Métriques suivies', 'Tracked metrics', 'Métricas seguidas', 'المقاييس المتابَعة', '追跡中のメトリック', '跟踪的指标'),
    'iot.detail.never-emitted': ('Aucune mesure reçue à ce jour', 'No measurement received so far', 'Ninguna medición recibida hasta ahora', 'لم يُستلم أي قياس حتى الآن', 'これまでに測定値の受信はありません', '至今未收到任何测量数据'),
    'iot.detail.recent-note': ("Cent dernières mesures de l'équipement, toutes métriques confondues. Total ingéré :", 'Last hundred measurements of the device, all metrics combined. Total ingested:', 'Últimas cien mediciones del equipo, todas las métricas juntas. Total ingerido:', 'آخر مئة قياس للجهاز، لجميع المقاييس مجتمعة. الإجمالي المستلم:', '全メトリックを含む直近100件の測定値です。取り込み総数：', '该设备最近一百条测量数据（含全部指标）。已接入总数：'),
    'iot.detail.silence-rule': ('Un équipement en service est déclaré muet après une heure sans signal ; la mention « signal vieillissant » apparaît dès trente minutes.', 'An in-service device is declared silent after one hour without a signal; the "ageing signal" mention appears from thirty minutes.', 'Un equipo en servicio se declara mudo tras una hora sin señal; la indicación «señal envejecida» aparece a partir de treinta minutos.', 'يُعتبر الجهاز قيد الخدمة صامتًا بعد ساعة دون إشارة؛ وتظهر عبارة «إشارة قديمة» ابتداءً من ثلاثين دقيقة.', '稼働中の機器は1時間信号がないと無応答と判定されます。「信号が古い」の表示は30分から出ます。', '运行中的设备若一小时没有信号即判定为静默；满三十分钟起显示“信号老化”。'),
    'iot.detail.status-changed': ('Statut mis à jour.', 'Status updated.', 'Estado actualizado.', 'تم تحديث الحالة.', 'ステータスを更新しました。', '状态已更新。'),
    'iot.detail.status-failed': ('Changement de statut refusé.', 'Status change rejected.', 'Cambio de estado rechazado.', 'تم رفض تغيير الحالة.', 'ステータスの変更が拒否されました。', '状态变更被拒绝。'),
    'iot.detail.suspend-message': ("Le serveur cessera d'accepter sa télémétrie : aucune dérive ne sera plus détectée tant qu'il n'est pas remis en service.", 'The server will stop accepting its telemetry: no drift will be detected until it is put back in service.', 'El servidor dejará de aceptar su telemetría: no se detectará ninguna desviación hasta que vuelva a ponerse en servicio.', 'سيتوقف الخادم عن قبول بياناته: لن يُكتشف أي انحراف إلى أن يُعاد تشغيله.', 'サーバーはテレメトリの受け付けを停止します。再稼働させるまで逸脱は検知されません。', '服务器将停止接收其遥测数据：在重新投入运行前不会检测到任何偏差。'),
    'iot.detail.suspend-title': ("Suspendre l'équipement ?", 'Suspend the device?', '¿Suspender el equipo?', 'هل تريد تعليق الجهاز؟', '機器を一時停止しますか？', '要暂停该设备吗？'),
    'iot.detail.telemetry-empty': ("Aucune mesure reçue. Relevez une valeur manuellement ou branchez le capteur sur son protocole d'ingestion.", 'No measurement received. Record a value manually or connect the sensor to its ingestion protocol.', 'No se ha recibido ninguna medición. Registre un valor manualmente o conecte el sensor a su protocolo de ingesta.', 'لم يُستلم أي قياس. سجّل قيمة يدويًا أو اربط المستشعر ببروتوكول الاستقبال الخاص به.', '測定値がありません。手動で値を記録するか、センサーを取り込みプロトコルに接続してください。', '尚未收到任何测量数据。请手动记录一个数值，或将传感器接入其数据采集协议。'),
    'iot.detail.telemetry-title': ('Télémétrie', 'Telemetry', 'Telemetría', 'القياس عن بُعد', 'テレメトリ', '遥测'),
    'iot.detail.threshold-add': ('Nouveau seuil', 'New threshold', 'Nuevo umbral', 'عتبة جديدة', '閾値を追加', '新增阈值'),
    'iot.detail.threshold-delete-message': ("Les dépassements de cette métrique n'ouvriront plus de CAPA automatiquement.", 'Breaches of this metric will no longer open a CAPA automatically.', 'Las superaciones de esta métrica ya no abrirán una CAPA automáticamente.', 'لن تؤدي تجاوزات هذا المقياس إلى فتح إجراء CAPA تلقائيًا بعد الآن.', 'このメトリックの逸脱で自動的にCAPAが起票されなくなります。', '该指标的超限将不再自动开立 CAPA。'),
    'iot.detail.threshold-delete-title': ('Supprimer ce seuil ?', 'Delete this threshold?', '¿Eliminar este umbral?', 'هل تريد حذف هذه العتبة؟', 'この閾値を削除しますか？', '要删除该阈值吗？'),
    'iot.detail.threshold-deleted': ('Seuil supprimé.', 'Threshold deleted.', 'Umbral eliminado.', 'تم حذف العتبة.', '閾値を削除しました。', '阈值已删除。'),
    'iot.detail.threshold-explain': ("Le franchissement d'un seuil actif ouvre une CAPA à l'ingestion de la mesure, et un cycle PDCA si le seuil le prévoit.", 'Crossing an active threshold opens a CAPA when the measurement is ingested, and a PDCA cycle if the threshold says so.', 'Superar un umbral activo abre una CAPA en el momento de la ingesta y un ciclo PDCA si el umbral lo prevé.', 'تجاوز عتبة مفعّلة يفتح إجراء CAPA عند استقبال القياس، ودورة PDCA إذا نصّت العتبة على ذلك.', '有効な閾値を超えると、測定値の取り込み時にCAPAが起票され、設定に応じてPDCAサイクルも開始します。', '越过启用中的阈值会在测量数据接入时开立 CAPA；若阈值设定如此，还会启动 PDCA 循环。'),
    'iot.detail.threshold-global': ('Géré globalement', 'Managed globally', 'Gestionado globalmente', 'يُدار على المستوى العام', '全体で管理', '全局管理'),
    'iot.detail.thresholds-count': ('Seuils applicables', 'Applicable thresholds', 'Umbrales aplicables', 'العتبات المطبَّقة', '適用される閾値', '适用的阈值'),
    'iot.detail.thresholds-empty': ("Aucun seuil : les mesures de cet équipement sont enregistrées mais aucune dérive n'ouvrira de CAPA automatiquement.", "No threshold: this device's measurements are recorded but no drift will open a CAPA automatically.", 'Sin umbrales: las mediciones de este equipo se registran, pero ninguna desviación abrirá una CAPA automáticamente.', 'لا توجد عتبات: تُسجَّل قياسات هذا الجهاز لكن لن يفتح أي انحراف إجراء CAPA تلقائيًا.', '閾値がありません。この機器の測定値は記録されますが、逸脱が自動的にCAPAを起票することはありません。', '没有阈值：该设备的测量数据仍会记录，但任何偏差都不会自动开立 CAPA。'),
    'iot.detail.thresholds-title': ('Seuils de surveillance', 'Monitoring thresholds', 'Umbrales de vigilancia', 'عتبات المراقبة', '監視閾値', '监控阈值'),
    'iot.detail.total-measures': ('Mesures ingérées', 'Ingested measurements', 'Mediciones ingeridas', 'القياسات المستلمة', '取り込み済み測定値', '已接入测量数'),
    'iot.detail.window': ('Fenêtre', 'Window', 'Ventana', 'النافذة الزمنية', '期間', '时间窗'),
    'iot.detail.window-complete': ('Mesures de la fenêtre :', 'Measurements in the window:', 'Mediciones de la ventana:', 'قياسات النافذة:', 'ウィンドウ内の測定数：', '该时间窗内的测量数：'),
    'iot.detail.window-truncated': ('Début de fenêtre seulement — points affichés puis mesures de la fenêtre :', 'Start of the window only — points displayed then measurements in the window:', 'Solo el inicio de la ventana: puntos mostrados y mediciones de la ventana:', 'بداية النافذة فقط — النقاط المعروضة ثم قياسات النافذة:', 'ウィンドウの先頭部分のみ — 表示中の点数とウィンドウ内の測定数：', '仅为时间窗起始部分 —— 显示点数与该时间窗内的测量数：'),

    # --- device-dialog -----------------------------------------------
    'iot.device-dialog.code': ('Code', 'Code', 'Código', 'الرمز', 'コード', '编码'),
    'iot.device-dialog.code-hint': ('Lettres, chiffres, point, tiret ou souligné — 120 caractères maximum.', 'Letters, digits, dot, hyphen or underscore — 120 characters maximum.', 'Letras, cifras, punto, guion o guion bajo: 120 caracteres como máximo.', 'أحرف وأرقام ونقطة وشرطة أو شرطة سفلية — 120 حرفًا كحد أقصى.', '英数字、ピリオド、ハイフン、アンダースコアのみ。最大120文字です。', '字母、数字、点、连字符或下划线 —— 最多 120 个字符。'),
    'iot.device-dialog.code-locked': ("Le code est figé après l'enregistrement.", 'The code is frozen once the device is registered.', 'El código queda fijado tras el registro.', 'يصبح الرمز ثابتًا بعد التسجيل.', '登録後にコードは変更できません。', '登记后编码不可更改。'),
    'iot.device-dialog.create-title': ('Nouvel équipement IoT', 'New IoT device', 'Nuevo equipo IoT', 'جهاز إنترنت أشياء جديد', '新しいIoT機器', '新建物联网设备'),
    'iot.device-dialog.edit-title': ("Modifier l'équipement", 'Edit device', 'Modificar el equipo', 'تعديل الجهاز', '機器を編集', '编辑设备'),
    'iot.device-dialog.name': ('Désignation', 'Name', 'Denominación', 'التسمية', '名称', '名称'),
    'iot.device-dialog.provisioned-hint': ("L'équipement est créé à l'état « provisionné » : il faut le mettre en service pour que le serveur accepte sa télémétrie.", 'The device is created in the "provisioned" state: put it in service for the server to accept its telemetry.', 'El equipo se crea en estado «aprovisionado»: hay que ponerlo en servicio para que el servidor acepte su telemetría.', 'يُنشأ الجهاز بحالة «مُهيّأ»: يجب تشغيله كي يقبل الخادم بياناته.', '機器は「プロビジョニング済み」の状態で作成されます。サーバーがテレメトリを受け付けるには稼働させてください。', '设备以“已配置”状态创建：需将其投入运行，服务器才会接收其遥测数据。'),

    # --- devices -----------------------------------------------------
    'iot.devices.action-error': ("La transition d'état a été refusée.", 'The state transition was rejected.', 'La transición de estado ha sido rechazada.', 'تم رفض تغيير الحالة.', '状態の変更が拒否されました。', '状态变更被拒绝。'),
    'iot.devices.created': ('Équipement enregistré.', 'Device registered.', 'Equipo registrado.', 'تم تسجيل الجهاز.', '機器を登録しました。', '设备已登记。'),
    'iot.devices.empty': ('Aucun équipement pour ce filtre. Enregistrez un capteur, une passerelle ou un automate pour commencer à recevoir sa télémétrie.', 'No device matches this filter. Register a sensor, a gateway or a controller to start receiving its telemetry.', 'Ningún equipo coincide con este filtro. Registre un sensor, una pasarela o un autómata para empezar a recibir su telemetría.', 'لا يوجد جهاز مطابق لهذا الفلتر. سجّل مستشعرًا أو بوابة أو متحكمًا لبدء استقبال بياناته.', 'この条件に一致する機器はありません。センサー、ゲートウェイ、または制御装置を登録するとテレメトリの受信が始まります。', '没有符合该筛选条件的设备。请登记传感器、网关或控制器以开始接收其遥测数据。'),
    'iot.devices.filter-note': ("Le serveur n'applique qu'un seul critère de filtrage : choisir un statut remet le type à « tous », et inversement.", 'The server applies only one filter criterion: choosing a status resets the type to "all", and vice versa.', 'El servidor solo aplica un criterio de filtrado: elegir un estado restablece el tipo a «todos», y viceversa.', 'يطبّق الخادم معيار تصفية واحدًا فقط: اختيار حالة يعيد النوع إلى «الكل»، والعكس صحيح.', 'サーバーは絞り込み条件を1つしか適用しません。ステータスを選ぶと種類は「すべて」に戻り、その逆も同様です。', '服务器只应用一个筛选条件：选择状态会将类型重置为“全部”，反之亦然。'),
    'iot.devices.load-error': ("Impossible de charger le parc d'équipements.", 'Unable to load the device fleet.', 'No se ha podido cargar el parque de equipos.', 'تعذّر تحميل أسطول الأجهزة.', '機器一覧を読み込めませんでした。', '无法加载设备清单。'),
    'iot.devices.new': ('Nouvel équipement', 'New device', 'Nuevo equipo', 'جهاز جديد', '機器を追加', '新增设备'),
    'iot.devices.open': ('Ouvrir la fiche', 'Open record', 'Abrir la ficha', 'فتح البطاقة', '詳細を開く', '打开档案'),
    'iot.devices.paginator-aria': ('Pagination des équipements', 'Device pagination', 'Paginación de equipos', 'ترقيم صفحات الأجهزة', '機器のページ送り', '设备分页'),
    'iot.devices.scope-note': ('Compteurs de santé calculés sur les équipements en service du tenant.', "Health counters computed over the tenant's in-service devices.", 'Contadores de estado calculados sobre los equipos en servicio del inquilino.', 'تُحتسب مؤشرات الحالة على الأجهزة قيد الخدمة لدى المستأجر.', '稼働中の機器を対象に健全性の件数を集計しています。', '健康计数基于该租户处于运行中的设备统计。'),
    'iot.devices.scope-truncated': ("Compteurs de santé calculés sur un échantillon des équipements en service : le tenant en compte davantage que ce qu'une page serveur peut renvoyer.", 'Health counters computed over a sample of in-service devices: the tenant has more of them than a single server page can return.', 'Contadores de estado calculados sobre una muestra de los equipos en servicio: el inquilino tiene más de los que puede devolver una página del servidor.', 'تُحتسب مؤشرات الحالة على عيّنة من الأجهزة قيد الخدمة: لدى المستأجر عدد أكبر مما يمكن أن تعيده صفحة واحدة من الخادم.', '稼働中機器の一部を対象に健全性を集計しています。テナントの機器数がサーバーの1ページで返せる件数を超えています。', '健康计数基于运行中设备的样本统计：该租户的设备数量超过服务器单页可返回的上限。'),
    'iot.devices.silence': ('Seuil de silence', 'Silence threshold', 'Umbral de silencio', 'عتبة الصمت', '無応答とみなす時間', '静默阈值'),
    'iot.devices.subtitle': ('Équipements connectés du tenant et fraîcheur de leur dernier signal. Un capteur muet ne déclenche plus aucune détection de dérive : il remonte en tête de liste.', 'Connected devices of the tenant and how recent their last signal is. A silent sensor no longer triggers any drift detection: it is moved to the top of the list.', 'Equipos conectados del inquilino y antigüedad de su última señal. Un sensor mudo ya no activa ninguna detección de desviación: se muestra al principio de la lista.', 'الأجهزة المتصلة الخاصة بالمستأجر ومدى حداثة آخر إشارة لها. المستشعر الصامت لم يعد يطلق أي كشف عن الانحراف، لذلك يظهر في أعلى القائمة.', 'テナントの接続機器と最終信号の新しさ。無応答のセンサーは逸脱検知を一切行わないため、リストの先頭に表示されます。', '租户的联网设备及其最后信号的新鲜度。静默的传感器不再触发任何偏差检测，因此排在列表最前面。'),
    'iot.devices.tile-aging': ('Signal vieillissant', 'Ageing signal', 'Señal envejecida', 'إشارة قديمة', '信号が古い', '信号老化'),
    'iot.devices.tile-listed': ('Équipements listés', 'Listed devices', 'Equipos listados', 'الأجهزة المعروضة', '表示中の機器', '已列出设备'),
    'iot.devices.tile-live': ('En ligne', 'Online', 'En línea', 'متصل', 'オンライン', '在线'),
    'iot.devices.tile-never': ('Jamais vus', 'Never seen', 'Nunca vistos', 'لم تُرَ قط', '未受信', '从未上报'),
    'iot.devices.tile-silent': ('Capteurs muets', 'Silent sensors', 'Sensores mudos', 'مستشعرات صامتة', '無応答センサー', '静默传感器'),
    'iot.devices.title': ('Parc IoT et télémétrie', 'IoT fleet and telemetry', 'Parque IoT y telemetría', 'أسطول إنترنت الأشياء والقياس عن بُعد', 'IoT機器とテレメトリ', '物联网设备与遥测'),

    # --- health ------------------------------------------------------
    'iot.health.aging': ('Signal vieillissant', 'Ageing signal', 'Señal envejecida', 'إشارة قديمة', '信号が古い', '信号老化'),
    'iot.health.inactive': ('Hors surveillance', 'Not monitored', 'Fuera de vigilancia', 'خارج المراقبة', '監視対象外', '未纳入监控'),
    'iot.health.live': ('En ligne', 'Online', 'En línea', 'متصل', 'オンライン', '在线'),
    'iot.health.never': ('Jamais vu', 'Never seen', 'Nunca visto', 'لم يُرَ قط', '未受信', '从未上报'),
    'iot.health.silent': ('Muet', 'Silent', 'Mudo', 'صامت', '無応答', '静默'),

    # --- protocol ----------------------------------------------------
    'iot.protocol.manual': ('Relevé manuel', 'Manual reading', 'Lectura manual', 'قراءة يدوية', '手動記録', '手动读数'),

    # --- iot ---------------------------------------------------------
    'iot.save-error': ("Erreur lors de l'enregistrement.", 'Error while saving.', 'Error al guardar.', 'حدث خطأ أثناء الحفظ.', '保存中にエラーが発生しました。', '保存时出错。'),

    # --- silence -----------------------------------------------------
    'iot.silence.15min': ('15 minutes', '15 minutes', '15 minutos', '15 دقيقة', '15分', '15 分钟'),
    'iot.silence.1h': ('1 heure', '1 hour', '1 hora', 'ساعة واحدة', '1時間', '1 小时'),
    'iot.silence.24h': ('24 heures', '24 hours', '24 horas', '24 ساعة', '24時間', '24 小时'),
    'iot.silence.6h': ('6 heures', '6 hours', '6 horas', '6 ساعات', '6時間', '6 小时'),

    # --- status ------------------------------------------------------
    'iot.status.active': ('En service', 'In service', 'En servicio', 'قيد الخدمة', '稼働中', '运行中'),
    'iot.status.decommissioned': ('Décommissionné', 'Decommissioned', 'Dado de baja', 'مُوقَف نهائيًا', '廃止済み', '已退役'),
    'iot.status.provisioned': ('Provisionné', 'Provisioned', 'Aprovisionado', 'مُهيّأ', 'プロビジョニング済み', '已配置'),
    'iot.status.suspended': ('Suspendu', 'Suspended', 'Suspendido', 'معلّق', '一時停止', '已暂停'),

    # --- telemetry-dialog --------------------------------------------
    'iot.telemetry-dialog.error': ('La mesure a été refusée par le serveur.', 'The measurement was rejected by the server.', 'El servidor ha rechazado la medición.', 'رفض الخادم القياس.', '測定値がサーバーに拒否されました。', '服务器拒绝了该测量值。'),
    'iot.telemetry-dialog.known': ('Déjà relevées', 'Already recorded', 'Ya registradas', 'مسجّلة سابقًا', '記録済み', '已记录'),
    'iot.telemetry-dialog.recorded-hint': ('Laissez vide pour horodater à la réception.', 'Leave empty to timestamp on reception.', 'Déjelo vacío para marcar la hora en la recepción.', 'اتركه فارغًا ليُسجَّل الوقت عند الاستلام.', '空欄のままにすると受信時刻が使われます。', '留空则以接收时间为准。'),
    'iot.telemetry-dialog.threshold-hint': ('Une valeur numérique hors des seuils configurés ouvre automatiquement une CAPA, et un cycle PDCA si le seuil le prévoit.', 'A numeric value outside the configured thresholds automatically opens a CAPA, and a PDCA cycle if the threshold says so.', 'Un valor numérico fuera de los umbrales configurados abre automáticamente una CAPA y un ciclo PDCA si el umbral lo prevé.', 'أي قيمة رقمية خارج العتبات المحددة تفتح تلقائيًا إجراءً تصحيحيًا (CAPA)، ودورة PDCA إذا نصّت العتبة على ذلك.', '設定した閾値を外れた数値は自動的にCAPAを起票し、閾値の設定に応じてPDCAサイクルも開始します。', '超出所配置阈值的数值会自动开立 CAPA；若阈值设定如此，还会启动一个 PDCA 循环。'),
    'iot.telemetry-dialog.title': ('Relever une mesure', 'Record a measurement', 'Registrar una medición', 'تسجيل قياس', '測定値を記録', '记录一次测量'),
    'iot.telemetry-dialog.value-required': ('Renseignez au moins une valeur, numérique ou textuelle.', 'Enter at least one value, numeric or text.', 'Introduzca al menos un valor, numérico o de texto.', 'أدخل قيمة واحدة على الأقل، رقمية أو نصية.', '数値またはテキストの値を少なくとも1つ入力してください。', '请至少填写一个数值或文本值。'),

    # --- telemetry ---------------------------------------------------
    'iot.telemetry.metric': ('Métrique', 'Metric', 'Métrica', 'المقياس', 'メトリック', '指标'),
    'iot.telemetry.recorded-at': ('Horodatage de la mesure', 'Measurement timestamp', 'Marca de tiempo de la medición', 'توقيت القياس', '測定日時', '测量时间戳'),
    'iot.telemetry.source': ('Source', 'Source', 'Origen', 'المصدر', '取得元', '来源'),
    'iot.telemetry.unit': ('Unité', 'Unit', 'Unidad', 'الوحدة', '単位', '单位'),
    'iot.telemetry.value': ('Valeur', 'Value', 'Valor', 'القيمة', '値', '数值'),
    'iot.telemetry.value-numeric': ('Valeur numérique', 'Numeric value', 'Valor numérico', 'قيمة رقمية', '数値', '数值'),
    'iot.telemetry.value-text': ('Valeur textuelle', 'Text value', 'Valor de texto', 'قيمة نصية', 'テキスト値', '文本值'),

    # --- threshold-dialog --------------------------------------------
    'iot.threshold-dialog.bounds-inverted': ('La borne basse doit être inférieure ou égale à la borne haute.', 'The lower bound must be less than or equal to the upper bound.', 'El límite inferior debe ser menor o igual que el superior.', 'يجب أن يكون الحد الأدنى أصغر من الحد الأقصى أو مساويًا له.', '下限は上限以下でなければなりません。', '下限必须小于或等于上限。'),
    'iot.threshold-dialog.bounds-required': ('Renseignez au moins une borne, basse ou haute.', 'Enter at least one bound, lower or upper.', 'Introduzca al menos un límite, inferior o superior.', 'أدخل حدًّا واحدًا على الأقل، أدنى أو أقصى.', '下限または上限を少なくとも1つ入力してください。', '请至少填写一个下限或上限。'),
    'iot.threshold-dialog.create-title': ('Nouveau seuil de surveillance', 'New monitoring threshold', 'Nuevo umbral de vigilancia', 'عتبة مراقبة جديدة', '新しい監視閾値', '新建监控阈值'),
    'iot.threshold-dialog.edit-title': ('Modifier le seuil', 'Edit threshold', 'Modificar el umbral', 'تعديل العتبة', '閾値を編集', '编辑阈值'),
    'iot.threshold-dialog.enabled-hint': ("Un seuil inactif reste enregistré mais n'est plus évalué à l'ingestion.", 'An inactive threshold stays recorded but is no longer evaluated on ingestion.', 'Un umbral inactivo se conserva pero ya no se evalúa durante la ingesta.', 'العتبة غير المفعّلة تبقى محفوظة لكنها لا تُقيَّم عند استقبال القياسات.', '無効な閾値は保存されますが、データ受信時には評価されません。', '停用的阈值仍会保存，但在数据接入时不再评估。'),
    'iot.threshold-dialog.enabled-label': ('Seuil actif', 'Threshold active', 'Umbral activo', 'العتبة مفعّلة', '閾値を有効にする', '阈值启用'),
    'iot.threshold-dialog.error': ('Le seuil a été refusé par le serveur.', 'The threshold was rejected by the server.', 'El servidor ha rechazado el umbral.', 'رفض الخادم العتبة.', '閾値がサーバーに拒否されました。', '服务器拒绝了该阈值。'),
    'iot.threshold-dialog.fmea-hint': ("Identifiant d'une fiche FMEA du tenant, rappelé dans la CAPA générée. Facultatif.", 'Identifier of a tenant FMEA item, quoted in the generated CAPA. Optional.', 'Identificador de una ficha FMEA del inquilino, citado en la CAPA generada. Opcional.', 'معرّف بطاقة FMEA لدى المستأجر، يُذكر في إجراء CAPA المُولَّد. اختياري.', 'テナントのFMEA項目の識別子です。生成されるCAPAに記載されます。任意項目です。', '租户 FMEA 条目的标识符，会写入所生成的 CAPA。可选填。'),
    'iot.threshold-dialog.fmea-invalid': ('Identifiant invalide : un UUID est attendu.', 'Invalid identifier: a UUID is expected.', 'Identificador no válido: se espera un UUID.', 'معرّف غير صالح: المتوقع هو UUID.', '識別子が不正です。UUIDを入力してください。', '标识符无效：应为 UUID。'),
    'iot.threshold-dialog.pdca-hint': ("En plus de la CAPA, un cycle d'amélioration est créé et référencé dans la CAPA.", 'In addition to the CAPA, an improvement cycle is created and referenced in the CAPA.', 'Además de la CAPA, se crea un ciclo de mejora al que la CAPA hace referencia.', 'بالإضافة إلى إجراء CAPA، تُنشأ دورة تحسين يُشار إليها داخل CAPA.', 'CAPAに加えて改善サイクルが作成され、CAPAから参照されます。', '除 CAPA 外，还会创建一个改进循环并在 CAPA 中引用。'),
    'iot.threshold-dialog.pdca-label': ('Ouvrir aussi un cycle PDCA', 'Also open a PDCA cycle', 'Abrir también un ciclo PDCA', 'فتح دورة PDCA أيضًا', 'PDCAサイクルも開始する', '同时启动 PDCA 循环'),

    # --- threshold ---------------------------------------------------
    'iot.threshold.bounds': ('Bornes', 'Bounds', 'Límites', 'الحدود', '上下限', '边界值'),
    'iot.threshold.criticity': ('Criticité de la CAPA générée', 'Criticality of the generated CAPA', 'Criticidad de la CAPA generada', 'درجة خطورة إجراء CAPA المُولَّد', '生成されるCAPAの重大度', '所生成 CAPA 的严重度'),
    'iot.threshold.disabled': ('Inactif', 'Inactive', 'Inactivo', 'غير مفعّل', '無効', '停用'),
    'iot.threshold.enabled': ('Actif', 'Active', 'Activo', 'مفعّل', '有効', '启用'),
    'iot.threshold.fmea': ('Fiche FMEA liée', 'Linked FMEA item', 'Ficha FMEA vinculada', 'بطاقة FMEA المرتبطة', '紐づくFMEA項目', '关联的 FMEA 条目'),
    'iot.threshold.max': ('Borne haute', 'Upper bound', 'Límite superior', 'الحد الأقصى', '上限', '上限'),
    'iot.threshold.min': ('Borne basse', 'Lower bound', 'Límite inferior', 'الحد الأدنى', '下限', '下限'),
    'iot.threshold.pdca': ('Cycle PDCA', 'PDCA cycle', 'Ciclo PDCA', 'دورة PDCA', 'PDCAサイクル', 'PDCA 循环'),
    'iot.threshold.scope': ('Portée', 'Scope', 'Alcance', 'النطاق', '適用範囲', '适用范围'),
    'iot.threshold.scope-device': ('Cet équipement', 'This device', 'Este equipo', 'هذا الجهاز', 'この機器', '本设备'),
    'iot.threshold.scope-tenant': ('Tous les équipements du tenant', 'All devices of the tenant', 'Todos los equipos del inquilino', 'جميع أجهزة المستأجر', 'テナントの全機器', '该租户的所有设备'),

    # --- type --------------------------------------------------------
    'iot.type.agro': ('Station agro', 'Agricultural station', 'Estación agrícola', 'محطة زراعية', '農業観測ステーション', '农业站点'),
    'iot.type.biomed': ('Dispositif biomédical', 'Biomedical device', 'Dispositivo biomédico', 'جهاز طبي حيوي', '生体医用機器', '生物医学设备'),
    'iot.type.bms': ('GTB / bâtiment', 'Building management (BMS)', 'Gestión técnica de edificios', 'نظام إدارة المباني', 'ビル管理システム', '楼宇管理系统'),
    'iot.type.camera': ('Caméra', 'Camera', 'Cámara', 'كاميرا', 'カメラ', '摄像头'),
    'iot.type.gateway': ('Passerelle Edge', 'Edge gateway', 'Pasarela Edge', 'بوابة طرفية', 'エッジゲートウェイ', '边缘网关'),
    'iot.type.generic': ('Capteur générique', 'Generic sensor', 'Sensor genérico', 'مستشعر عام', '汎用センサー', '通用传感器'),
    'iot.type.humidity': ("Capteur d'humidité", 'Humidity sensor', 'Sensor de humedad', 'مستشعر رطوبة', '湿度センサー', '湿度传感器'),
    'iot.type.plc': ('Automate (PLC)', 'PLC controller', 'Autómata (PLC)', 'متحكم منطقي (PLC)', 'PLC（制御装置）', '可编程控制器（PLC）'),
    'iot.type.pressure': ('Capteur de pression', 'Pressure sensor', 'Sensor de presión', 'مستشعر ضغط', '圧力センサー', '压力传感器'),
    'iot.type.temperature': ('Sonde de température', 'Temperature probe', 'Sonda de temperatura', 'مجس حرارة', '温度センサー', '温度探头'),
    'iot.type.unknown': ('Non renseigné', 'Unspecified', 'Sin especificar', 'غير محدد', '未設定', '未指定'),
    'iot.type.vibration': ('Capteur de vibration', 'Vibration sensor', 'Sensor de vibración', 'مستشعر اهتزاز', '振動センサー', '振动传感器'),

    # --- unit --------------------------------------------------------
    'iot.unit.days': ('j', 'd', 'd', 'ي', '日', '天'),
    'iot.unit.hours': ('h', 'h', 'h', 'س', '時間', '小时'),
    'iot.unit.minutes': ('min', 'min', 'min', 'د', '分', '分钟'),

    # --- window ------------------------------------------------------
    'iot.window.d30': ('30 jours', '30 days', '30 días', '30 يومًا', '30日間', '30 天'),
    'iot.window.d7': ('7 jours', '7 days', '7 días', '7 أيام', '7日間', '7 天'),
    'iot.window.h24': ('24 heures', '24 hours', '24 horas', '24 ساعة', '24時間', '24 小时'),
    'iot.window.recent': ('Dernières mesures', 'Latest measurements', 'Últimas mediciones', 'أحدث القياسات', '直近の測定値', '最近的测量'),
}
