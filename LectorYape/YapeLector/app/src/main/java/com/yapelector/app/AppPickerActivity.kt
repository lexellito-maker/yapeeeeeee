package com.yapelector.app

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.concurrent.thread

/** Pantalla para elegir de qué apps se leen las notificaciones. */
class AppPickerActivity : AppCompatActivity() {

    private data class AppItem(val label: String, val pkg: String, val icon: Drawable?)

    private val selected = mutableSetOf<String>()
    private var all: List<AppItem> = emptyList()
    private var shown: List<AppItem> = emptyList()
    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)

        selected.addAll(Prefs.selectedApps(this))

        val list = findViewById<ListView>(R.id.list)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val search = findViewById<EditText>(R.id.search)

        adapter = Adapter()
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            val item = shown[position]
            if (!selected.add(item.pkg)) selected.remove(item.pkg)
            Prefs.setSelectedApps(this, selected)
            adapter.notifyDataSetChanged()
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = applyFilter(s?.toString().orEmpty())
        })

        // Cargar apps en segundo plano
        thread {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val items = pm.queryIntentActivities(intent, 0)
                .map { ri ->
                    AppItem(
                        label = ri.loadLabel(pm).toString(),
                        pkg = ri.activityInfo.packageName,
                        icon = ri.loadIcon(pm)
                    )
                }
                .filter { it.pkg != packageName }
                .distinctBy { it.pkg }
                .toMutableList()

            // Si Yape está elegida pero no instalada, igual la mostramos arriba
            if (items.none { it.pkg == Prefs.YAPE_PACKAGE }) {
                items += AppItem("Yape (no instalada)", Prefs.YAPE_PACKAGE, null)
            }

            runOnUiThread {
                all = items
                progress.visibility = View.GONE
                applyFilter(search.text.toString())
            }
        }
    }

    private fun applyFilter(query: String) {
        val q = query.trim().lowercase(Locale.getDefault())
        shown = all
            .filter { q.isEmpty() || it.label.lowercase(Locale.getDefault()).contains(q) }
            .sortedWith(
                compareByDescending<AppItem> { it.pkg == Prefs.YAPE_PACKAGE }
                    .thenByDescending { it.pkg in selected }
                    .thenBy { it.label.lowercase(Locale.getDefault()) }
            )
        adapter.notifyDataSetChanged()
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            val item = shown[position]
            v.findViewById<ImageView>(R.id.icon).setImageDrawable(item.icon)
            v.findViewById<TextView>(R.id.name).text = item.label
            v.findViewById<CheckBox>(R.id.check).isChecked = item.pkg in selected
            return v
        }
    }
}
