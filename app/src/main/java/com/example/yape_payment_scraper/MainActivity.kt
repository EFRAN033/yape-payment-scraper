package com.example.yape_payment_scraper

import android.Manifest
import android.content.ComponentName
import android.content.Context // <-- IMPORT AÑADIDO
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
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
import androidx.core.app.ActivityCompat
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

    // Colores
    private val colorWhite by lazy { ContextCompat.getColor(this, R.color.white) }
    private val colorBlack by lazy { ContextCompat.getColor(this, R.color.black) }
    private val colorStatusOff by lazy { ContextCompat.getColor(this, R.color.status_off) }
    private val colorYapeYellow by lazy { ContextCompat.getColor(this, R.color.yape_accent_yellow) }

    private val NOTIFICATION_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicialización de Vistas
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)
        powerButton = findViewById(R.id.powerButton)
        statusTextView = findViewById(R.id.statusTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)

        // Configurar la Toolbar como ActionBar
        setSupportActionBar(toolbar)
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
            if (!isServiceEnabled) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Para desactivar, hazlo desde Configuración > Notificaciones > Acceso especial a Notificaciones", Toast.LENGTH_LONG).show()
            }
        }

        // Manejar el botón de retroceso (Back Button)
        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }

        // Solicitar permiso de notificación
        checkAndRequestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        isServiceEnabled = isNotificationServiceEnabled()
        updateUI()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {}
            R.id.nav_history -> {
                startActivity(Intent(this, HistoryActivity::class.java)) // Esta línea ahora funcionará
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
        // --- INICIO DE LA MODIFICACIÓN ---
        // Accede a las SharedPreferences que usa el servicio
        val sharedPrefs = getSharedPreferences(YapeNotificationListener.PREF_FILE_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        // --- FIN DE LA MODIFICACIÓN ---

        if (isServiceEnabled) {
            // Estado ON (Activado)
            powerButton.setBackgroundResource(R.drawable.button_state_on)
            powerButton.setImageResource(R.drawable.ic_check)
            powerButton.imageTintList = ColorStateList.valueOf(colorWhite)
            powerButton.contentDescription = getString(R.string.button_turn_off)
            statusTextView.text = getString(R.string.status_on)
            statusTextView.setTextColor(colorWhite)
            descriptionTextView.setTextColor(colorWhite)

            // --- INICIO DE LA MODIFICACIÓN ---
            // Sincroniza el interruptor interno del servicio: PONER EN ON
            editor.putBoolean(YapeNotificationListener.PREF_SCANNING_ENABLED, true)
            // --- FIN DE LA MODIFICACIÓN ---

        } else {
            // Estado OFF (Desactivado)
            powerButton.setBackgroundResource(R.drawable.button_state_off)
            powerButton.setImageResource(R.drawable.ic_power)
            powerButton.imageTintList = ColorStateList.valueOf(colorBlack)
            powerButton.contentDescription = getString(R.string.button_turn_on)
            statusTextView.text = getString(R.string.status_off)
            statusTextView.setTextColor(colorStatusOff)
            descriptionTextView.setTextColor(colorStatusOff)

            // --- INICIO DE LA MODIFICACIÓN ---
            // Sincroniza el interruptor interno del servicio: PONER EN OFF
            editor.putBoolean(YapeNotificationListener.PREF_SCANNING_ENABLED, false)
            // --- FIN DE LA MODIFICACIÓN ---
        }

        // --- INICIO DE LA MODIFICACIÓN ---
        // Aplica los cambios a SharedPreferences
        editor.apply()
        // --- FIN DE LA MODIFICACIÓN ---
    }

    // --- CÓDIGO NUEVO PARA PERMISOS ---

    /**
     * Verifica y solicita el permiso POST_NOTIFICATIONS si es necesario (Android 13+).
     */
    private fun checkAndRequestNotificationPermission() {
        // Solo necesario para Android 13 (TIRAMISU) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // El permiso no está concedido, solicitarlo.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_CODE)
            }
        }
    }

    /**
     * Maneja el resultado de la solicitud de permisos.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de notificación concedido.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso de notificación denegado. La notificación de servicio no se mostrará.", Toast.LENGTH_LONG).show()
            }
        }
    }
}