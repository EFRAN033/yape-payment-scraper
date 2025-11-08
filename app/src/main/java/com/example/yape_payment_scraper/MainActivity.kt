package com.example.yape_payment_scraper

import android.os.Bundle
import android.widget.Button   // <-- IMPORTANTE: Añadir esto
import android.widget.TextView  // <-- IMPORTANTE: Añadir esto
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // 1. Variable para guardar el estado actual
    private var isServiceOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // --- INICIO DE LA LÓGICA DEL BOTÓN ---

        // 2. Encontrar las vistas (el botón y el texto) por su ID
        val statusTextView: TextView = findViewById(R.id.statusTextView)
        val toggleButton: Button = findViewById(R.id.toggleButton)

        // 3. Ponerle un "escuchador" de clics al botón
        toggleButton.setOnClickListener {

            // 4. Invertir el estado
            isServiceOn = !isServiceOn // Si es true, lo vuelve false (y viceversa)

            // 5. Actualizar los textos según el nuevo estado
            if (isServiceOn) {
                // Si el servicio está ENCENDIDO
                statusTextView.text = "Estado: Encendido"
                toggleButton.text = "Apagar"
            } else {
                // Si el servicio está APAGADO
                statusTextView.text = "Estado: Apagado"
                toggleButton.text = "Encender"
            }
        }

        // --- FIN DE LA LÓGICA DEL BOTÓN ---

        // Este es el código que ya tenías para ajustar los bordes
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}