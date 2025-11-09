package com.example.yape_payment_scraper

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar // <-- Importante

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        // ⬇️ Encontrar la Toolbar y establecerla como ActionBar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Establecer la barra de título
        supportActionBar?.setTitle("Historial de Pagos")

        // Habilitar el botón de "atrás" en la barra de acción
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val historyList = YapeNotificationListener.getPaymentHistory(this)

        val listView: ListView = findViewById(R.id.history_list_view)

        // Mapear los PaymentRecords a un formato de String para el ListView
        val displayList = historyList.map { record ->
            // Formato de fecha para mostrar
            val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.timestamp))
            "S/ ${record.amount} - ${record.senderName}\n($date)"
        }

        // Usar un adaptador simple para mostrar los datos
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listView.adapter = adapter
    }

    // Manejar el clic del botón de "atrás" en la ActionBar
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}