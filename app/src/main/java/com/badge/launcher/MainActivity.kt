package com.badge.launcher

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {

    private class AppInfo(val label: String, val pkg: String, val icon: Drawable, val intent: Intent)

    private val apps = ArrayList<AppInfo>()
    private lateinit var banner: Button
    private lateinit var grid: GridView
    private val adapter = object : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(i: Int) = apps[i]
        override fun getItemId(i: Int) = i.toLong()
        override fun getView(i: Int, v: View?, p: ViewGroup?) = cell(apps[i])
    }
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        runOnUiThread { adapter.notifyDataSetChanged() }
    }

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#101820"))
            fitsSystemWindows = true
        }
        banner = Button(this).apply {
            text = "Activar acceso a notificaciones"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        grid = GridView(this).apply {
            numColumns = 4
            adapter = this@MainActivity.adapter
            setOnItemClickListener { _, _, i, _ ->
                val a = apps[i]
                BadgeStore.clear(this@MainActivity, a.pkg)
                startActivity(a.intent)
            }
            // Mantener pulsado = quitar el número sin abrir la app
            setOnItemLongClickListener { _, _, i, _ ->
                BadgeStore.clear(this@MainActivity, apps[i].pkg); true
            }
        }
        root.addView(banner)
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        loadApps()
        banner.visibility = if (hasAccess()) View.GONE else View.VISIBLE
        getSharedPreferences(BadgeStore.PREFS, MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences(BadgeStore.PREFS, MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onNewIntent(i: Intent?) { super.onNewIntent(i); grid.smoothScrollToPosition(0) }

    @Deprecated("") override fun onBackPressed() {}

    private fun hasAccess(): Boolean {
        val s = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return s.contains(packageName)
    }

    private fun loadApps() {
        val pm = packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps.clear()
        for (ri in pm.queryIntentActivities(q, PackageManager.MATCH_ALL)) {
            val pkg = ri.activityInfo.packageName
            if (pkg == packageName) continue
            val intent = pm.getLaunchIntentForPackage(pkg) ?: continue
            apps.add(AppInfo(ri.loadLabel(pm).toString(), pkg, ri.loadIcon(pm), intent))
        }
        apps.sortBy { it.label.lowercase() }
        adapter.notifyDataSetChanged()
    }

    private fun cell(a: AppInfo): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(10), 0, dp(10))
        }
        val box = FrameLayout(this)
        val icon = ImageView(this).apply { setImageDrawable(a.icon) }
        box.addView(icon, FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))

        val n = BadgeStore.count(this, a.pkg)
        if (n > 0) {
            val badge = TextView(this).apply {
                text = if (n > 99) "99+" else n.toString()
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                textSize = 12f
                gravity = Gravity.CENTER
                minWidth = dp(22)
                setPadding(dp(5), 0, dp(5), 0)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FF3B30"))
                    cornerRadius = dp(11).toFloat()
                    setStroke(dp(2), Color.WHITE)
                }
            }
            box.addView(badge, FrameLayout.LayoutParams(-2, dp(22), Gravity.TOP or Gravity.END))
        }
        col.addView(box, LinearLayout.LayoutParams(dp(72), dp(64)))

        col.addView(TextView(this).apply {
            text = a.label
            setTextColor(Color.WHITE)
            textSize = 11f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
        })
        return col
    }
}
