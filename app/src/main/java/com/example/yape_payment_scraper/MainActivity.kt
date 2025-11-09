package com.example.yape_payment_scraper

import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var isServiceEnabled = false

    // Vistas del Sidebar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar

    // Vistas de la UI
    private lateinit var powerButton: ImageButton
    private lateinit var statusTextView: TextView
    private lateinit var descriptionTextView: TextView

    // Colores (Inicializados de forma perezosa para el contexto)
    private val colorWhite by lazy { ContextCompat.getColor(this, R.color.white) }
    private val colorBlack by lazy { ContextCompat.getColor(this, R.color.black) }
    private val colorStatusOff by lazy { ContextCompat.getColor(this, R.color.status_off) }
    // ⬇️ CORRECCIÓN 1: El color correcto en colors.xml es yape_accent_yellow, no yape_yellow.
    private val colorYapeYellow by lazy { ContextCompat.getColor(this, R.color.yape_accent_yellow) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicialización de Vistas
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)
        // ⬇️ CORRECCIÓN 2, 3 y 4: Los IDs en activity_main.xml usan CamelCase, no snake_case.
        powerButton = findViewById(R.id.powerButton)
        statusTextView = findViewById(R.id.statusTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)

        // Configurar la Toolbar como ActionBar
        setSupportActionBar(toolbar)
        // Asume que R.string.app_name existe
        supportActionBar?.setTitle(R.string.app_name)

        // Configurar el Navigation Drawer
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

        // Manejar el botón de encendido/apagado (Power Button)
        powerButton.setOnClickListener {
            // Abrir la configuración para que el usuario pueda activar/desactivar el servicio
            if (!isServiceEnabled) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
                // El estado de la UI se actualizará en onResume()
            } else {
                Toast.makeText(this, "Para desactivar, hazlo desde Configuración > Notificaciones > Acceso especial a Notificaciones", Toast.LENGTH_LONG).show()
            }
        }

        // Manejar el botón de retroceso (Back Button)
        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish() // Cierra la Activity si el drawer está cerrado
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar el estado del servicio cada vez que la Activity vuelve al primer plano
        isServiceEnabled = isNotificationServiceEnabled()
        updateUI()
    }

    /**
     * Maneja los clics en los ítems del sidebar
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // No hacemos nada, ya estamos aquí
            }
            R.id.nav_history -> {
                // Lógica AGREGADA para lanzar la nueva Activity de Historial
                startActivity(Intent(this, HistoryActivity::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }


    // --- Funciones de Ayuda ---

    /**
     * Verifica si nuestro NotificationListenerService está activo en la configuración del sistema.
     */
    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(this, YapeNotificationListener::class.java)
        return enabledListeners?.contains(componentName.flattenToString()) == true
    }

    /**
     * Actualiza la UI (botón de encendido y textos de estado) según el estado del servicio.
     */
    private fun updateUI() {
        if (isServiceEnabled) {
            // Estado ON (Activado)
            powerButton.setBackgroundResource(R.drawable.button_state_on)
            powerButton.setImageResource(R.drawable.ic_check)
            powerButton.imageTintList = ColorStateList.valueOf(colorWhite)
            powerButton.contentDescription = getString(R.string.button_turn_off)
            statusTextView.text = getString(R.string.status_on)
            statusTextView.setTextColor(colorWhite)
            descriptionTextView.setTextColor(colorWhite)
        } else {
            // Estado OFF (Desactivado)
            powerButton.setBackgroundResource(R.drawable.button_state_off)
            powerButton.setImageResource(R.drawable.ic_power)
            powerButton.imageTintList = ColorStateList.valueOf(colorBlack)
            powerButton.contentDescription = getString(R.string.button_turn_on)
            statusTextView.text = getString(R.string.status_off)
            statusTextView.setTextColor(colorStatusOff)
            descriptionTextView.setTextColor(colorStatusOff)
        }
    }
}