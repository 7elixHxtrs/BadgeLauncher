package com.badge.launcher

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.service.notification.NotificationListenerService.RankingMap

class BadgeService : NotificationListenerService() {

    override fun onListenerConnected() {
        activeNotifications?.forEach { count(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = count(sbn)

    // Al borrar de la barra no hacemos nada: el número se queda.
    // Solo se limpia si tocas la notificación (se abre la app).
    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap, reason: Int) {
        if (reason == REASON_CLICK) BadgeStore.clear(this, sbn.packageName)
    }

    private fun count(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val f = sbn.notification.flags
        if (f and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (f and Notification.FLAG_ONGOING_EVENT != 0) return
        BadgeStore.add(this, sbn.packageName, sbn.key)
    }
}
