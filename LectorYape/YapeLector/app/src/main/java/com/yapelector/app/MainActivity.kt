package com.yapelector.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tvPermission: TextView
    private lateinit var tvSelected: TextView
    private lateinit var swEnabled: SwitchMaterial
    private lateinit var swSayApp: SwitchMaterial
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPermission = findViewById(R.id.tvPermission)
        tvSelected = findViewById(R.id.tvSelected)
        swEnabled = findViewById(R.id.swEnabled)
        swSayApp = findViewById(R.id.swSayApp)

        tts = TextToSpeech(this, this)

        swEnabled.isChecked = Prefs.isEnabled(this)
        swEnabled.setOnCheckedChangeListener { _, checked -> Prefs.setEnabled(this, checked) }

        swSayApp.isChecked = Prefs.sayAppName(this)
        swSayApp.setOnCheckedChangeListener { _, checked -> Prefs.setSayAppName(this, checked) }

        findViewById<Button>(R.id.btnPermission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.btnChooseApps).setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
        findViewById<Button>(R.id.btnTest).setOnClickListener {
            if (ttsReady) {
                tts?.speak(
                    "Yape. Juan Pérez te envió un pago de 10.50 soles",
                    TextToSpeech.QUEUE_FLUSH, null, "test"
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val engine = tts ?: return
        val r = engine.setLanguage(Locale("es", "PE"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale("es"))
        }
        ttsReady = true
    }

    override fun onResume() {
        super.onResume()
        val granted = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        tvPermission.text = if (granted) getString(R.string.perm_ok) else getString(R.string.perm_missing)
        refreshSelected()
    }

    private fun refreshSelected() {
        val names = Prefs.selectedApps(this).map { pkg ->
            try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                if (pkg == Prefs.YAPE_PACKAGE) "Yape (no instalada)" else pkg
            }
        }.sorted()
        tvSelected.text = if (names.isEmpty()) getString(R.string.none_selected) else names.joinToString("\n• ", prefix = "• ")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
