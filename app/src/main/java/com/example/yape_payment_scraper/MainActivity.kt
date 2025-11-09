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
import androidx.activity.addCallback // ¡Asegúrate de que este import esté!
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

    // Vistas de la UI (las que ya tenías)
    private lateinit var powerButton: ImageButton
    private lateinit var statusTextView: TextView
    private lateinit var descriptionTextView: TextView

    // Colores
    private val colorWhite by lazy { ContextCompat.getColor(this, R.color.white) }
    private val colorBlack by lazy { ContextCompat.getColor(this, R.color.black) }
    private val colorStatusOff by lazy { ContextCompat.getColor(this, R.color.status_off) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // --- Configuración del Sidebar ---
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Marcar "Monitor" (Home) como seleccionado al inicio
        navigationView.setCheckedItem(R.id.nav_home)

        // --- Configuración del Botón de Encendido ---
        powerButton = findViewById(R.id.powerButton)
        statusTextView = findViewById(R.id.statusTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)

        powerButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // --- NUEVO MANEJO DEL BOTÓN "ATRÁS" ---
        // Esto reemplaza la función onBackPressed() obsoleta
        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                // Si el menú no está abierto, ejecuta el comportamiento
                // normal del botón "atrás" (salir de la app).
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
                // TODO: Aquí lanzarías la nueva Activity de Historial
                Toast.makeText(this, "Abriendo Historial...", Toast.LENGTH_SHORT).show()
                // Por ejemplo: startActivity(Intent(this, HistoryActivity::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // --- SE ELIMINÓ LA FUNCIÓN onBackPressed() ---
    // Ya no es necesaria, su lógica está en el onCreate.


    // --- Tus funciones de antes ---

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(this, YapeNotificationListener::class.java)
        return enabledListeners?.contains(componentName.flattenToString()) == true
    }

    private fun updateUI() {
        if (isServiceEnabled) {
            powerButton.setBackgroundResource(R.drawable.button_state_on)
            powerButton.setImageResource(R.drawable.ic_check)
            powerButton.imageTintList = ColorStateList.valueOf(colorWhite)
            powerButton.contentDescription = getString(R.string.button_turn_off)
            statusTextView.text = getString(R.string.status_on)
            statusTextView.setTextColor(colorWhite)
            descriptionTextView.setTextColor(colorWhite)
        } else {
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