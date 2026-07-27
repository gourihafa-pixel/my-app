package com.calcvault.app

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.calcvault.app.databinding.ActivityMainBinding
import net.objecthunter.exp4j.ExpressionBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentInput: StringBuilder = StringBuilder()
    private var lastWasResult: Boolean = false
    private var justEvaluated: Boolean = false

    // The secret passcode the user must type, then press '=' to unlock the vault.
    private val secretPin = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make sure really look like a calculator in the recent apps list
        window.setBackgroundDrawableResource(R.color.calc_bg)

        binding.btn0.setOnClickListener { input("0") }
        binding.btn1.setOnClickListener { input("1") }
        binding.btn2.setOnClickListener { input("2") }
        binding.btn3.setOnClickListener { input("3") }
        binding.btn4.setOnClickListener { input("4") }
        binding.btn5.setOnClickListener { input("5") }
        binding.btn6.setOnClickListener { input("6") }
        binding.btn7.setOnClickListener { input("7") }
        binding.btn8.setOnClickListener { input("8") }
        binding.btn9.setOnClickListener { input("9") }
        binding.btnDot.setOnClickListener { input(".") }
        binding.btnPlus.setOnClickListener { input("+") }
        binding.btnMinus.setOnClickListener { input("-") }
        binding.btnMul.setOnClickListener { input("*") }
        binding.btnDiv.setOnClickListener { input("/") }
        binding.btnOpen.setOnClickListener { input("(") }
        binding.btnClose.setOnClickListener { input(")") }
        binding.btnPercent.setOnClickListener { input("/100") }

        binding.btnClear.setOnClickListener {
            currentInput.clear()
            binding.tvExpression.text = ""
            binding.tvResult.text = ""
            lastWasResult = false
            justEvaluated = false
        }

        binding.btnBack.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput.deleteCharAt(currentInput.length - 1)
                binding.tvExpression.text = currentInput.toString()
            }
            lastWasResult = false
        }

        binding.btnEq.setOnClickListener { onEquals() }
    }

    private fun input(token: String) {
        val v = this
        v.binding.btnEq.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        if (lastWasResult) {
            // If last action was a result, start fresh
            currentInput.clear()
            lastWasResult = false
        }

        // Disallow two operators in a row
        if (token.length == 1 && "+-*/".contains(token)) {
            if (currentInput.isEmpty()) return
            val last = currentInput.last()
            if ("+-*/".contains(last)) {
                currentInput.deleteCharAt(currentInput.length - 1)
            }
        }
        if (token == ".") {
            // Avoid 1..2
            if (currentInput.isEmpty() || "+-*/(".contains(currentInput.last())) {
                currentInput.append("0")
            }
            if (currentInput.contains(".")) {
                val tail = currentInput.substringAfterLastAny("+-*/(".toCharArray())
                if (tail.contains(".")) return
            }
        }

        currentInput.append(token)
        binding.tvExpression.text = currentInput.toString()
        preview()
    }

    private fun preview() {
        // show intermediate result if the expression is evaluable
        val s = currentInput.toString()
        if (s.isEmpty()) {
            binding.tvResult.text = ""
            return
        }
        // detect trailing operator -> do not preview
        if ("+-*/.".contains(s.last())) {
            binding.tvResult.text = ""
            return
        }
        try {
            val v = ExpressionBuilder(s).build().evaluate()
            if (v.isNaN() || v.isInfinite()) {
                binding.tvResult.text = ""
            } else {
                binding.tvResult.text = formatNumber(v)
            }
        } catch (_: Exception) {
            binding.tvResult.text = ""
        }
    }

    private fun onEquals() {
        binding.btnEq.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        // ---- SECRET UNLOCK ----
        // The user typed the PIN digits then '=' -> open vault
        val expr = currentInput.toString().trim()
        if (expr == secretPin) {
            openVault()
            // Reset so the math history doesn't reveal anything
            currentInput.clear()
            binding.tvExpression.text = ""
            binding.tvResult.text = ""
            lastWasResult = false
            return
        }
        // ALSO accept PIN preceded/followed by math operators, e.g. "12+34" -> strips to 1234 on =
        val digits = expr.filter { it.isDigit() }
        if (digits == secretPin) {
            openVault()
            currentInput.clear()
            binding.tvExpression.text = ""
            binding.tvResult.text = ""
            lastWasResult = false
            return
        }

        // ---- NORMAL CALCULATION ----
        if (expr.isEmpty()) return
        try {
            val v = ExpressionBuilder(expr).build().evaluate()
            val text = formatNumber(v)
            binding.tvResult.text = text
            binding.tvExpression.text = expr
            currentInput.clear()
            currentInput.append(text)
            lastWasResult = true
        } catch (e: Exception) {
            binding.tvResult.text = "Error"
        }
    }

    private fun openVault() {
        val intent = Intent(this, VaultActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun formatNumber(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString()
        else "%.6f".format(v).trimEnd('0').trimEnd('.')
    }

    // helper: append token exhaustion logic
    private val opChars = "+-*/".toCharArray()
    private fun String.substringAfterLastAny(chars: CharArray): String {
        if (isEmpty()) return ""
        var lastSep = -1
        for (i in indices) if (chars.contains(this[i])) lastSep = i
        return substring(lastSep + 1)
    }
}
