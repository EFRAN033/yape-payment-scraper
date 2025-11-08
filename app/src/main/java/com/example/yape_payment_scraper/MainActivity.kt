package com.example.yape_payment_scraper

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge // <-- Esto se encarga de todo
import androidx.appcompat.app.AppCompatActivity
// Ya no necesitas 'ViewCompat' ni 'WindowInsetsCompat' aquí

class MainActivity : AppCompatActivity() {

    private var isServiceOn = false

    private val colorEncendido = Color.parseColor("#4CAF50") // Verde
    private val colorApagado = Color.parseColor("#F1F1F1")   // Gris claro

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Esta línea es la única que necesitas para los márgenes
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        val statusTextView: TextView = findViewById(R.id.statusTextView)
        val toggleButton: Button = findViewById(R.id.toggleButton)

        toggleButton.setOnClickListener {
            isServiceOn = !isServiceOn

            if (isServiceOn) {
                // Estado ENCENDIDO
                statusTextView.text = "Estado: Encendido"
                toggleButton.text = "Apagar"
                statusTextView.backgroundTintList = ColorStateList.valueOf(colorEncendido)
                statusTextView.setTextColor(Color.WHITE)

            } else {
                // Estado APAGADO
                statusTextView.text = "Estado: Apagado"
                toggleButton.text = "Encender"
                statusTextView.backgroundTintList = ColorStateList.valueOf(colorApagado)
                statusTextView.setTextColor(Color.parseColor("#333333"))
            }
        }

        // --- BORRAMOS EL BLOQUE "ViewCompat.setOnApplyWindowInsetsListener" DE AQUÍ ---
        // Ya no es necesario
    }
}