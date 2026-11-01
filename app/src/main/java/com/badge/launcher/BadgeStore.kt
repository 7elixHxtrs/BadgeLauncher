package com.badge.launcher

import android.content.Context

/** Guarda las claves de notificaciones vistas por app. Borrarlas de la barra NO las quita de aquí. */
object BadgeStore {
    const val PREFS = "badges"
    private fun p(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun add(c: Context, pkg: String, key: String) {
        val s = HashSet(p(c).getStringSet(pkg, emptySet())!!)
        if (s.add(key)) p(c).edit().putStringSet(pkg, s).apply()
    }

    fun count(c: Context, pkg: String): Int = p(c).getStringSet(pkg, null)?.size ?: 0

    fun clear(c: Context, pkg: String) = p(c).edit().remove(pkg).apply()
}
