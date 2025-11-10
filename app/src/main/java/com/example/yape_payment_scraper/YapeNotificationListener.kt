package com.example.yape_payment_scraper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompatz

// 1. Data Class para el Registro de Pago (sin cambios)
data class PaymentRecord(
    val senderName: String,
    val amount: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun toString(): String {
        return "$senderName|$amount|$timestamp"
    }
    companion object {
        fun fromString(s: String): PaymentRecord? {
            return try {
                val parts = s.split("|")
                PaymentRecord(parts[0], parts[1], parts[2].toLong())
            } catch (e: Exception) {
                null
            }
        }
    }
}

class YapeNotificationListener : NotificationListenerService() {

    // El ID de paquete de Yape
    // --- ¡AQUÍ ESTÁ LA CORRECCIÓN! ---
    private val YAPE_PACKAGE_NAME = "com.bcp.innovacxion.yapeapp"

    // La Regex para extraer los pagos (coincide con tu imagen)
    private val paymentRegex = Regex("(?:Yape! )?(.+?) te (?:yapeó|envió un pago por) S/ (\\d+\\.?\\d*)")

    // --- CÓDIGO DEL SERVICIO EN PRIMER PLANO ---

    companion object {
        private const val CHANNEL_ID = "YapeScraperServiceChannel"
        private const val NOTIFICATION_ID = 1

        // --- ¡NUEVO! ---
        // Claves para el estado interno y el archivo de SharedPreferences
        const val PREF_SCANNING_ENABLED = "scanning_enabled"
        const val PREF_FILE_NAME = "YapePaymentHistory" // Reutilizamos el mismo nombre

        // Función estática para obtener el historial (sin cambios)
        fun getPaymentHistory(context: Context): List<PaymentRecord> {
            val sharedPrefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE) // Usamos la constante
            val historySet = sharedPrefs.getStringSet("history_list", emptySet()) ?: emptySet()
            return historySet.mapNotNull { PaymentRecord.fromString(it) }.sortedByDescending { it.timestamp }
        }

        // --- ¡NUEVO! ---
        // Función de ayuda para saber si el escaneo está activado por el usuario
        private fun isScanningEnabled(context: Context): Boolean {
            val sharedPrefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
            // Por defecto, está apagado
            return sharedPrefs.getBoolean(PREF_SCANNING_ENABLED, false)
        }
    }

    /**
     * Se llama cuando el servicio se crea.
     * (Sin cambios)
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d("YapeScraper", "Servicio iniciado en primer plano.")
    }

    /**
     * Crea el canal de notificación (necesario para Android 8.0+).
     * (Sin cambios)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Yape Scraper Service",
                NotificationManager.IMPORTANCE_LOW // Poca importancia para que no sea intrusiva
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Construye la notificación persistente.
     * (Sin cambios)
     */
    private fun createNotification(): Notification {
        // Intent para abrir MainActivity cuando se toca la notificación
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Construir la notificación
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitor de Yape Activo")
            .setContentText("Escuchando nuevas notificaciones de pago...")
            .setSmallIcon(R.drawable.ic_check) // Reutilizamos tu ícono de "check"
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Hacerla persistente
            .build()
    }

    /**
     * Se llama cuando el servicio se destruye (ej. el usuario quita el permiso).
     * (Sin cambios)
     */
    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE) // <-- Corrección
        Log.d("YapeScraper", "Servicio detenido.")
    }

    /**
     * ¡AQUÍ ESTÁ LA LÓGICA MEJORADA!
     * Se llama cuando llega una nueva notificación.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        // --- ¡LÓGICA MEJORADA! ---
        // 1. Verificamos si el usuario quiere escanear (nuestro interruptor interno)
        if (!isScanningEnabled(this)) {
            Log.d("YapeScraper", "Servicio pausado por el usuario. Ignorando notificación.")
            return // No hacer nada si está apagado
        }

        // 2. Filtramos para que solo reaccione a notificaciones de Yape
        if (sbn.packageName == YAPE_PACKAGE_NAME) {

            // 3. Extraemos el contenido de la notificación
            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getString("android.title")?.toString() ?: ""
            val text = extras.getString("android.text")?.toString() ?: ""

            Log.d("YapeScraper", "Notificación de Yape recibida (Servicio ON):")
            Log.d("YapeScraper", "Título: $title")
            Log.d("YapeScraper", "Texto: $text")

            // 4. ¡AQUÍ HACES EL "SCRAPING"!
            val matchResult = paymentRegex.find(text)

            if (matchResult != null) {
                // El grupo 1 es el nombre, el grupo 2 es el monto
                val (senderName, amount) = matchResult.destructured
                val paymentRecord = PaymentRecord(senderName.trim(), amount)

                Log.d("YapeScraper", "Pago detectado. Remitente: ${paymentRecord.senderName}, Monto: S/ ${paymentRecord.amount}")

                // 5. Guardar en el Historial
                savePaymentRecord(paymentRecord)

            } else {
                Log.d("YapeScraper", "El texto de la notificación no coincide con el patrón de pago esperado.")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    // --- Lógica de persistencia de datos (usando SharedPreferences) ---
    private fun savePaymentRecord(record: PaymentRecord) {
        val sharedPrefs = getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE) // Usamos la constante
        val editor = sharedPrefs.edit()
        val currentHistory = sharedPrefs.getStringSet("history_list", mutableSetOf()) ?: mutableSetOf()
        val newHistory = HashSet(currentHistory)
        newHistory.add(record.toString())
        editor.putStringSet("history_list", newHistory)
        editor.apply()
        Log.d("YapeScraper", "Registro guardado. Total de registros: ${newHistory.size}")
    }
}