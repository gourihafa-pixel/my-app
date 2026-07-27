package com.calcvault.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.calcvault.app.databinding.ActivityFileHiderBinding
import java.io.File

class FileHiderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileHiderBinding
    private lateinit var adapter: FileAdapter
    private val pickedUris = mutableListOf<Uri>()

    private val pickFiles = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        for (u in uris) {
            try {
                contentResolver.takePersistableUriPermission(
                    u, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { /* some providers don't allow it, that's fine */ }
        }
        val saved = VaultManager.importFiles(this, uris)
        Toast.makeText(this, "تم إخفاء $saved ملف بنجاح", Toast.LENGTH_SHORT).show()
        refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileHiderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = FileAdapter(
            onClick = { item ->
                // Decrypt and open with system viewer
                try {
                    val tmp = VaultManager.exportToCache(this, item)
                    val mime = VaultManager.guessMime(item.displayName)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.fromFile(tmp), mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "تعذر فتح الملف: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            onDelete = { item ->
                VaultManager.deleteFile(this, item)
                Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show()
                refresh()
            }
        )

        binding.rvFiles.layoutManager = LinearLayoutManager(this)
        binding.rvFiles.adapter = adapter

        binding.btnImport.setOnClickListener {
            pickFiles.launch("*/*")
        }
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val list = VaultManager.listFiles(this)
        adapter.submit(list)
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    // -------- adapter --------
    inner class FileAdapter(
        val onClick: (VaultManager.VaultFile) -> Unit,
        val onDelete: (VaultManager.VaultFile) -> Unit
    ) : RecyclerView.Adapter<FileAdapter.VH>() {

        private val items = mutableListOf<VaultManager.VaultFile>()

        fun submit(list: List<VaultManager.VaultFile>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_file, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val it = items[position]
            holder.name.text = it.displayName
            holder.size.text = VaultManager.humanSize(it.sizeBytes)
            // click
            holder.itemView.setOnClickListener { onClick(it) }
            holder.btnDel.setOnClickListener { onDelete(it) }
        }

        override fun getItemCount() = items.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val size: TextView = v.findViewById(R.id.tvSize)
            val btnDel: View = v.findViewById(R.id.btnDel)
        }
    }
}
