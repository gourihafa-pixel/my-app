package com.calcvault.app

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.calcvault.app.databinding.ActivityAppHiderBinding

class AppHiderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppHiderBinding
    private lateinit var adapter: AppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppHiderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AppAdapter(
            onHide = { pkg ->
                HiddenApps.add(this, pkg)
                Toast.makeText(this, "تم إخفاء التطبيق", Toast.LENGTH_SHORT).show()
                refresh()
            },
            onUnhide = { pkg ->
                HiddenApps.remove(this, pkg)
                Toast.makeText(this, "تم إظهار التطبيق", Toast.LENGTH_SHORT).show()
                refresh()
            },
            onLaunch = { pkg ->
                try {
                    val launch = packageManager.getLaunchIntentForPackage(pkg)
                        ?: Intent().setComponent(
                            ComponentName(
                                pkg,
                                packageManager.getPackageInfo(pkg, 0).activities[0].name
                            )
                        )
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                } catch (e: Exception) {
                    Toast.makeText(this, "تعذر التشغيل: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )

        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val all = InstalledApps.list(this)
        val hidden = HiddenApps.get(this).toSet()
        val items = all.map { AppItem(it, hidden.contains(it.packageName)) }
        adapter.submit(items)
    }

    // ---- model ----
    data class AppItem(val info: InstalledApps.AppInfo, val hidden: Boolean)

    inner class AppAdapter(
        val onHide: (String) -> Unit,
        val onUnhide: (String) -> Unit,
        val onLaunch: (String) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.VH>() {

        private val items = mutableListOf<AppItem>()

        fun submit(list: List<AppItem>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val it = items[position]
            holder.label.text = it.info.label
            holder.pkg.text = it.info.packageName
            if (it.info.icon != null) holder.icon.setImageDrawable(it.info.icon)
            else holder.icon.setImageResource(android.R.drawable.sym_def_app_icon)

            holder.itemView.setOnClickListener { onLaunch(it.info.packageName) }
            holder.btnToggle.setOnClickListener {
                if (it.hidden) onUnhide(it.info.packageName) else onHide(it.info.packageName)
            }
            holder.btnToggle.text = if (it.hidden) "إظهار" else "إخفاء"
        }

        override fun getItemCount() = items.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val label: TextView = v.findViewById(R.id.tvLabel)
            val pkg: TextView = v.findViewById(R.id.tvPkg)
            val icon: ImageView = v.findViewById(R.id.ivIcon)
            val btnToggle: TextView = v.findViewById(R.id.btnToggle)
        }
    }
}
