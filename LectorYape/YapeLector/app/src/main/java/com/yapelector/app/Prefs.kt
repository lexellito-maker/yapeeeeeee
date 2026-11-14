package com.yapelector.app

import android.content.Context

/** Ajustes guardados de la app. Por defecto solo se lee Yape. */
object Prefs {
    const val YAPE_PACKAGE = "com.bcp.innovacxion.yapeapp"

    private const val FILE = "yape_lector_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_APPS = "apps"
    private const val KEY_SAY_APP = "say_app_name"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = sp(c).getBoolean(KEY_ENABLED, true)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_ENABLED, v).apply()

    fun sayAppName(c: Context) = sp(c).getBoolean(KEY_SAY_APP, false)
    fun setSayAppName(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_SAY_APP, v).apply()

    /** Paquetes de las apps cuyas notificaciones se leen. */
    fun selectedApps(c: Context): Set<String> =
        sp(c).getStringSet(KEY_APPS, null)?.toSet() ?: setOf(YAPE_PACKAGE)

    fun setSelectedApps(c: Context, apps: Set<String>) =
        sp(c).edit().putStringSet(KEY_APPS, HashSet(apps)).apply()
}
