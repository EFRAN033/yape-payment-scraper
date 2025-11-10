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
import androidx.core.app.NotificationCompat
// --- IMPORTS AÑADIDOS ---
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
// --- FIN DE IMPORTS AÑADIDOS ---


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

    private val YAPE_PACKAGE_NAME = "com.bcp.innovacxion.yapeapp"

    private val paymentRegex = Regex("(?:Yape! )?(.+?) te (?:yapeó|envió un pago por) S/ (\\d+\\.?\\d*)")

    companion object {
        private const val CHANNEL_ID = "YapeScraperServiceChannel"
        private const val NOTIFICATION_ID = 1

        const val PREF_SCANNING_ENABLED = "scanning_enabled"
        const val PREF_FILE_NAME = "YapePaymentHistory"

        fun getPaymentHistory(context: Context): List<PaymentRecord> {
            val sharedPrefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
            val historySet = sharedPrefs.getStringSet("history_list", emptySet()) ?: emptySet()
            return historySet.mapNotNull { PaymentRecord.fromString(it) }.sortedByDescending { it.timestamp }
        }

        private fun isScanningEnabled(context: Context): Boolean {
            val sharedPrefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
            // Por defecto, está apagado
            return sharedPrefs.getBoolean(PREF_SCANNING_ENABLED, false)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d("YapeScraper", "Servicio iniciado en primer plano.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Yape Scraper Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitor de Yape Activo")
            .setContentText("Escuchando nuevas notificaciones de pago...")
            .setSmallIcon(R.drawable.ic_check)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d("YapeScraper", "Servicio detenido.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        if (!isScanningEnabled(this)) {
            Log.d("YapeScraper", "Servicio pausado por el usuario. Ignorando notificación.")
            return
        }


        if (sbn.packageName == YAPE_PACKAGE_NAME) {


            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getString("android.title")?.toString() ?: ""
            val text = extras.getString("android.text")?.toString() ?: ""

            Log.d("YapeScraper", "Notificación de Yape recibida (Servicio ON):")
            Log.d("YapeScraper", "Título: $title")
            Log.d("YapeScraper", "Texto: $text")


            val matchResult = paymentRegex.find(text)

            if (matchResult != null) {

                val (senderName, amount) = matchResult.destructured
                val paymentRecord = PaymentRecord(senderName.trim(), amount)

                Log.d("YapeScraper", "Pago detectado. Remitente: ${paymentRecord.senderName}, Monto: S/ ${paymentRecord.amount}")

                savePaymentRecord(paymentRecord)

            } else {
                Log.d("YapeScraper", "El texto de la notificación no coincide con el patrón de pago esperado.")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    private fun savePaymentRecord(record: PaymentRecord) {
        val sharedPrefs = getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE) // Usamos la constante
        val editor = sharedPrefs.edit()
        val currentHistory = sharedPrefs.getStringSet("history_list", mutableSetOf()) ?: mutableSetOf()
        val newHistory = HashSet(currentHistory)
        newHistory.add(record.toString())
        editor.putStringSet("history_list", newHistory)
        editor.apply()
        Log.d("YapeScraper", "Registro guardado. Total de registros: ${newHistory.size}")

        // --- ¡MODIFICACIÓN! LLAMAR AL BACKEND ---
        sendPaymentToBackend(record)
    }

    // --- ¡NUEVA FUNCIÓN AÑADIDA! ---
    private fun sendPaymentToBackend(record: PaymentRecord) {
        val payload = YapePaymentPayload(
            sender_name = record.senderName,
            amount = record.amount
        )

        // !!! IMPORTANTE: CAMBIA ESTA CLAVE POR LA QUE INVENTASTE !!!
        // (Esta debe ser la misma que pusiste en el .env de tu backend)
        val apiKey = "clave-secreta-de-yape-para-kambia-pe-8373jd9"

        Log.d("YapeScraper", "Enviando pago al backend...")

        RetrofitClient.instance.confirmPayment(apiKey, payload).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    Log.d("YapeScraper", "¡Pago enviado al backend exitosamente! Código: ${response.code()}")
                } else {
                    // El backend respondió con un error (ej. 401, 404, 500)
                    Log.e("YapeScraper", "Error al enviar pago. Código: ${response.code()}")
                    Log.e("YapeScraper", "Mensaje: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                // Fallo de red (ej. no hay internet, no se encuentra el dominio)
                Log.e("YapeScraper", "Fallo de red al enviar pago: ${t.message}")
            }
        })
    }
}


// --- INICIO: CÓDIGO DE RED AÑADIDO (al final del archivo) ---

// 1. Modelo de datos (debe coincidir con el Pydantic de Python)
data class YapePaymentPayload(
    val sender_name: String,
    val amount: String
)

// 2. Interfaz de Retrofit
interface ApiService {
    @POST("/api/v1/confirm-yape-payment") // Esta es la ruta de tu API en KambiaPe
    fun confirmPayment(
        @Header("x-api-key") apiKey: String,
        @Body payload: YapePaymentPayload
    ): Call<Unit> // No necesitamos una respuesta, solo saber si se envió (Call<Unit>)
}

// 3. Objeto Singleton para crear el cliente de Retrofit
object RetrofitClient {
    // !!! IMPORTANTE: CAMBIA ESTO POR LA URL DE TU BACKEND !!!
    // (Ejemplo: "https://kambia-pe-backend.onrender.com/")
    private const val BASE_URL = "https://tu-backend-kambia-pe.com/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}
// --- FIN: CÓDIGO DE RED AÑADIDO ---