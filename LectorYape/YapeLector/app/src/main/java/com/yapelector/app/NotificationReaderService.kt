package com.yapelector.app

import android.app.Notification
import android.content.pm.PackageManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Escucha las notificaciones del sistema y lee en voz alta solo las de las apps elegidas
 * (por defecto, Yape).
 */
class NotificationReaderService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var lastSpoken = ""
    private var lastSpokenAt = 0L

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) return
        var result = engine.setLanguage(Locale("es", "PE"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            result = engine.setLanguage(Locale("es", "US"))
        }
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale("es"))
        }
        ttsReady = true
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (!Prefs.isEnabled(this)) return
        if (sbn.packageName !in Prefs.selectedApps(this)) return

        val n = sbn.notification ?: return
        // Ignora el "resumen" de grupo para no leer dos veces lo mismo
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()

        if (title.isBlank() && body.isBlank()) return

        val parts = mutableListOf<String>()
        if (Prefs.sayAppName(this)) parts += appLabel(sbn.packageName)
        if (title.isNotBlank()) parts += title
        if (body.isNotBlank()) parts += body
        val text = cleanForSpeech(parts.joinToString(". "))

        // Evita repetir la misma notificación (algunas apps la actualizan varias veces)
        val now = SystemClock.elapsedRealtime()
        if (text == lastSpoken && now - lastSpokenAt < 5_000) return
        lastSpoken = text
        lastSpokenAt = now

        speak(text)
    }

    private fun speak(text: String) {
        if (!ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "n-${System.nanoTime()}")
    }

    /** "S/ 10.50" -> "10.50 soles" para que la voz lo pronuncie bien. */
    private fun cleanForSpeech(raw: String): String =
        raw.replace(Regex("""S/\.?\s?([0-9][0-9.,]*)"""), "$1 soles")

    private fun appLabel(pkg: String): String = try {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
