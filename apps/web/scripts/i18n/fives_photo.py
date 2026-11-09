# -*- coding: utf-8 -*-
"""La photo du constat d'un audit 5S (§3.2, §15.3).

Une note de 3/10 ne dit pas ce qui a été vu ; la photo le montre, et c'est elle
qu'on ressort trois mois plus tard pour savoir si la zone a changé. On importe un
fichier déjà pris, sans ouvrir l'appareil : l'auditeur photographie avec son
téléphone comme il en a l'habitude, puis verse ce qu'il a retenu. L'import
fonctionne hors ligne : en zone blanche, les octets attendent localement et
partent au retour du réseau — c'est là qu'on relève, et refuser le versement y
viderait l'audit mobile de son sens.
id: (fr, en, es, ar, ja, zh).
"""

TRANSLATIONS = {

    # --- la prise de vue ----------------------------------------------------
    'fives.photo.take': (
        'Importer une photo', 'Import a photo', 'Importar una foto',
        'إدراج صورة', '写真を取り込む', '导入照片'),
    'fives.photo.tooltip': (
        'Importer la photo du constat fait sur ce pilier',
        'Import the photo of what was observed on this pillar',
        'Importar la foto de lo observado en este pilar',
        'إدراج صورة ما لوحظ على هذا الركن',
        'この柱で確認した状態の写真を取り込みます',
        '导入该支柱上观察情况的照片'),
    'fives.photo.done': (
        'Photo ajoutée au pilier.', 'Photo added to the pillar.',
        'Foto añadida al pilar.', 'أُضيفت الصورة إلى الركن.',
        '柱に写真を追加しました。', '照片已添加到该支柱。'),

    # --- hors ligne ---------------------------------------------------------
    'fives.photo.queued': (
        'Photo conservée localement. Elle partira au retour du réseau.',
        'Photo kept locally. It will be sent when the network is back.',
        'Foto guardada localmente. Se enviará al volver la red.',
        'حُفظت الصورة محلياً، وسترسل عند عودة الشبكة.',
        '写真はローカルに保存しました。通信が戻り次第送信します。',
        '照片已保存在本机，网络恢复后会自动上传。'),
    'fives.photo.pending': (
        'En attente de réseau', 'Waiting for network', 'Esperando la red',
        'في انتظار الشبكة', '通信待ち', '等待网络'),

    # --- le retrait ---------------------------------------------------------
    'fives.photo.remove-title': (
        'Retirer cette photo ?', 'Remove this photo?', '¿Retirar esta foto?',
        'إزالة هذه الصورة؟', 'この写真を取り消しますか？', '移除这张照片？'),
    'fives.photo.remove-message': (
        "La photo disparaîtra de l'audit. Le retrait est consigné au journal.",
        'The photo will leave the audit. The removal is written to the log.',
        'La foto desaparecerá de la auditoría. La retirada queda registrada.',
        'ستختفي الصورة من التدقيق، ويُسجَّل الحذف في السجل.',
        '写真は監査から外れます。取り消しは台帳に記録されます。',
        '照片将从审核中移除，该操作会记入日志。'),
    'fives.photo.remove-aria': (
        'Retirer cette photo', 'Remove this photo', 'Retirar esta foto',
        'إزالة هذه الصورة', 'この写真を取り消す', '移除这张照片'),
    'fives.photo.remove-error': (
        "La photo n'a pas pu être retirée.", 'The photo could not be removed.',
        'No se ha podido retirar la foto.', 'لم يتعذّر حذف الصورة.',
        '写真を取り消せませんでした。', '无法移除照片。'),

    # --- ce qui est refusé, et pourquoi -------------------------------------
    'fives.photo.err-type': (
        'Seules les photos (JPEG, PNG, WebP, HEIC) sont acceptées.',
        'Only photos (JPEG, PNG, WebP, HEIC) are accepted.',
        'Solo se aceptan fotos (JPEG, PNG, WebP, HEIC).',
        'لا تُقبل سوى الصور (JPEG وPNG وWebP وHEIC).',
        '写真（JPEG・PNG・WebP・HEIC）のみ受け付けます。',
        '仅接受照片（JPEG、PNG、WebP、HEIC）。'),
    'fives.photo.err-too-large': (
        'Photo trop volumineuse (10 Mo maximum).',
        'Photo too large (10 MB maximum).',
        'Foto demasiado grande (10 MB como máximo).',
        'الصورة أكبر من الحد المسموح (10 ميغابايت).',
        '写真が大きすぎます（最大10MB）。',
        '照片过大（最多 10 MB）。'),
    'fives.photo.err-quota': (
        'Plafond atteint : 10 Mo par photo, 5 photos par pilier, 40 Mo par audit.',
        'Limit reached: 10 MB per photo, 5 photos per pillar, 40 MB per audit.',
        'Límite alcanzado: 10 MB por foto, 5 fotos por pilar, 40 MB por auditoría.',
        'بلغت الحد: 10 ميغابايت للصورة، و5 صور للركن، و40 ميغابايت للتدقيق.',
        '上限に達しました（1枚10MB、柱ごとに5枚、監査ごとに40MB）。',
        '已达上限：每张 10 MB，每个支柱 5 张，每次审核 40 MB。'),
    'fives.photo.err-state': (
        "Ce pilier doit d'abord être noté, et l'audit ne doit pas être annulé.",
        'This pillar must be scored first, and the audit must not be cancelled.',
        'Este pilar debe puntuarse primero y la auditoría no debe estar anulada.',
        'يجب تقييم هذا الركن أولاً، وألا يكون التدقيق ملغى.',
        'まずこの柱を採点してください。監査が取消済みの場合は追加できません。',
        '请先为该支柱评分，且审核不能处于已取消状态。'),
    'fives.photo.err-storage': (
        'Le stockage des photos est momentanément indisponible.',
        'Photo storage is temporarily unavailable.',
        'El almacenamiento de fotos no está disponible por el momento.',
        'تخزين الصور غير متاح مؤقتاً.',
        '写真の保管先が一時的に利用できません。',
        '照片存储暂时不可用。'),
    'fives.photo.err-generic': (
        "La photo n'a pas pu être envoyée.", 'The photo could not be sent.',
        'No se ha podido enviar la foto.', 'لم يتم إرسال الصورة.',
        '写真を送信できませんでした。', '照片发送失败。'),
}
