package com.calcvault.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.calcvault.app.databinding.ActivityVaultBinding

class VaultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVaultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardFiles.setOnClickListener {
            startActivity(Intent(this, FileHiderActivity::class.java))
        }
        binding.cardApps.setOnClickListener {
            startActivity(Intent(this, AppHiderActivity::class.java))
        }
        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnLock.setOnClickListener { finish() }
    }
}
