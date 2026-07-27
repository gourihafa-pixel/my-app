package com.calcvault.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.calcvault.app.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvCurrentPin.text = "الرمز الحالي: ${VaultManager.getPin(this)}"

        binding.btnChangePin.setOnClickListener { showChangePinDialog() }
        binding.btnChangeIcon.setOnClickListener {
            Toast.makeText(
                this,
                "لتغيير الأيقونة: افتح إعدادات النظام > التطبيقات > حاسبة > تغيير الأيقونة (مدعوم على Android 13+)",
                Toast.LENGTH_LONG
            ).show()
        }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun showChangePinDialog() {
        val container = layoutInflater.inflate(R.layout.dialog_change_pin, null)
        val old = container.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOld)
        val nw = container.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNew)
        val cf = container.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirm)

        MaterialAlertDialogBuilder(this)
            .setTitle("تغيير رمز الدخول")
            .setView(container)
            .setPositiveButton("حفظ") { _, _ ->
                val cur = VaultManager.getPin(this)
                if (old.text.toString() != cur) {
                    Toast.makeText(this, "الرمز القديم غير صحيح", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                }
                val newPin = nw.text.toString().trim()
                if (newPin.length < 3 || !newPin.all { it.isDigit() }) {
                    Toast.makeText(this, "الرمز يجب أن يكون أرقامًا (3 أرقام على الأقل)", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                }
                if (newPin != cf.text.toString().trim()) {
                    Toast.makeText(this, "الرمز وتأكيده غير متطابقين", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                }
                VaultManager.setPin(this, newPin)
                Toast.makeText(this, "تم تغيير الرمز", Toast.LENGTH_SHORT).show()
                recreate()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
