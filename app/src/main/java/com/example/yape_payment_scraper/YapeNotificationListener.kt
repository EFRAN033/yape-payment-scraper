package com.example.yape_payment_scraper

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

// 1. Data Class para el Registro de Pago y métodos de serialización/deserialización
// Nota: Se usa una serialización simple con "|" para evitar añadir librerías de JSON (como Gson).
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
    private val YAPE_PACKAGE_NAME = "com.bcp.yape"

    // Regex para buscar el patrón "\[Nombre] te yapeó S/ \[Monto]"
    // Captura: (Nombre), (Monto)
    private val paymentRegex = Regex("(.+?) te yapeó S/ (\\d+\\.?\\d*)")

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
            val matchResult = paymentRegex.find(text)

            if (matchResult != null) {
                val (senderName, amount) = matchResult.destructured
                val paymentRecord = PaymentRecord(senderName.trim(), amount)

                Log.d("YapeScraper", "Pago detectado. Remitente: ${paymentRecord.senderName}, Monto: S/ ${paymentRecord.amount}")

                // 4. Guardar en el Historial
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
        val sharedPrefs = getSharedPreferences("YapePaymentHistory", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // 1. Recuperar la lista actual de registros guardados como una lista de strings (Set)
        val currentHistory = sharedPrefs.getStringSet("history_list", mutableSetOf()) ?: mutableSetOf()

        // 2. Agregar el nuevo registro.
        val newHistory = HashSet(currentHistory)
        newHistory.add(record.toString())

        // 3. Guardar el set actualizado.
        editor.putStringSet("history_list", newHistory)
        editor.apply()

        Log.d("YapeScraper", "Registro guardado. Total de registros: ${newHistory.size}")
    }

    companion object {
        // Función estática para obtener el historial desde cualquier Activity
        fun getPaymentHistory(context: Context): List<PaymentRecord> {
            val sharedPrefs = context.getSharedPreferences("YapePaymentHistory", Context.MODE_PRIVATE)
            val historySet = sharedPrefs.getStringSet("history_list", emptySet()) ?: emptySet()
            // Mapear los Strings guardados de vuelta a PaymentRecord y ordenar por timestamp
            return historySet.mapNotNull { PaymentRecord.fromString(it) }.sortedByDescending { it.timestamp }
        }
    }
}