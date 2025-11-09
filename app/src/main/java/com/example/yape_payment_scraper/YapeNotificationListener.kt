package com.example.yape_payment_scraper

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class YapeNotificationListener : NotificationListenerService() {

    // El ID de paquete de Yape
    private val YAPE_PACKAGE_NAME = "com.bcp.yape"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        // 1. Filtramos para que solo reaccione a notificaciones de Yape
        if (sbn.packageName == YAPE_PACKAGE_NAME) {

            // 2. Extraemos el contenido de la notificación
            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getString("android.title")?.toString() ?: ""
            val text = extras.getString("android.text")?.toString() ?: ""

            Log.d("YapeScraper", "Notificación de Yape recibida:")
            Log.d("YapeScraper", "Título: $title")
            Log.d("YapeScraper", "Texto: $text")

            // 3. ¡AQUÍ HACES EL "SCRAPING"!
            // El 'text' probablemente sea algo como "Juan Pérez te yapeó S/ 20.00"
            // Necesitarás usar Regex o .split() para extraer el nombre y el monto.

            // (Opcional: Enviar esta info a tu MainActivity con un Broadcast)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Opcional: puedes reaccionar cuando el usuario descarta la notificación
        super.onNotificationRemoved(sbn)
    }
}