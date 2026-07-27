package com.calcvault.app

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Stores hidden files in app-private encrypted storage and persists the user's PIN.
 * Uses AES-CBC with a key derived from the install ID + PIN.
 *
 * NOTE on app-hiding: Android does not let one user-installed app force-hide a
 * different app's launcher icon (only the owning app can do that via
 * PackageManager.setComponentEnabledSetting, which requires android.intent.action.MAIN
 * aliases — so "hiding" other people's apps is restricted by the OS). What we
 * realistically offer here is:
 *   1. Hide selected apps' LAUNCH aliases inside this vault (their original icons
 *      remain unless the user manually disables them — see AppHiderActivity for
 *      the user's notes on Android limits).
 *   2. A launch shortcut through the vault itself.
 * For full confiscation of another app's icon you need the OEM launcher, an
 * admin/MDM app, or the target app's own hidden mode.
 */
object VaultManager {

    private const val PIN_KEY = "calc_vault_pin"
    private const val MASTER_KEY = "calc_vault_master"

    data class VaultFile(
        val id: String,
        val displayName: String,
        val sizeBytes: Long,
        val storedPath: String
    )

    fun getPin(ctx: Context): String {
        val sp = ctx.getSharedPreferences("vault", Context.MODE_PRIVATE)
        return sp.getString(PIN_KEY, "1234") ?: "1234"
    }

    fun setPin(ctx: Context, pin: String) {
        val sp = ctx.getSharedPreferences("vault", Context.MODE_PRIVATE)
        sp.edit().putString(PIN_KEY, pin).putString(MASTER_KEY, deriveMaster(pin, ctx)).apply()
    }

    private fun deriveMaster(pin: String, ctx: Context): String {
        val salt = ctx.packageName + "_calc_vault_salt"
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray())
        md.update(pin.toByteArray())
        return Base64.encodeToString(md.digest(), Base64.NO_WRAP)
    }

    private fun key(ctx: Context): SecretKeySpec {
        val pin = getPin(ctx)
        val md = MessageDigest.getInstance("SHA-256")
        val raw = md.digest(pin.toByteArray() + ctx.packageName.toByteArray())
        return SecretKeySpec(raw.copyOf(16), "AES")
    }

    private fun vaultDir(ctx: Context): File {
        val d = File(ctx.filesDir, "vault_store")
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun indexFile(ctx: Context): File = File(vaultDir(ctx), "index.csv")

    private fun readIndex(ctx: Context): MutableList<VaultFile> {
        val f = indexFile(ctx)
        if (!f.exists()) return mutableListOf()
        return f.readLines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 4) null
            else VaultFile(parts[0], parts[1], parts[2].toLongOrNull() ?: 0L, parts[3])
        }.toMutableList()
    }

    private fun writeIndex(ctx: Context, list: List<VaultFile>) {
        val f = indexFile(ctx)
        f.writeText(list.joinToString("\n") { "${it.id}|${it.displayName}|${it.sizeBytes}|${it.storedPath}" })
    }

    fun importFiles(ctx: Context, uris: List<Uri>): Int {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val k = key(ctx)
        var saved = 0
        for (u in uris) {
            try {
                val display = queryDisplayName(ctx, u) ?: "file_${System.currentTimeMillis()}"
                val safeName = display.replace('/', '_').replace('|', '_')
                val stored = File(vaultDir(ctx), "${System.currentTimeMillis()}_$safeName.bin")

                val raw = ctx.contentResolver.openInputStream(u) ?: continue
                val bytes = raw.readBytes()
                raw.close()

                val iv = ByteArray(16)
                java.security.SecureRandom().nextBytes(iv)
                cipher.init(Cipher.ENCRYPT_MODE, k, IvParameterSpec(iv))
                val enc = cipher.doFinal(bytes)

                FileOutputStream(stored).use { out ->
                    out.write(iv)
                    out.write(enc)
                }

                val list = readIndex(ctx)
                list.add(
                    VaultFile(
                        id = stored.name,
                        displayName = safeName,
                        sizeBytes = bytes.size.toLong(),
                        storedPath = stored.absolutePath
                    )
                )
                writeIndex(ctx, list)
                saved++
            } catch (_: Exception) { /* skip that file */ }
        }
        return saved
    }

    fun listFiles(ctx: Context): List<VaultFile> = readIndex(ctx).sortedByDescending { it.storedPath }

    fun deleteFile(ctx: Context, item: VaultFile) {
        try { File(item.storedPath).delete() } catch (_: Exception) {}
        val list = readIndex(ctx).filter { it.id != item.id }
        writeIndex(ctx, list)
    }

    fun exportToCache(ctx: Context, item: VaultFile): File {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key(ctx), IvParameterSpec(File(item.storedPath).readBytes().copyOfRange(0, 16)))
        val encBody = File(item.storedPath).readBytes().copyOfRange(16, File(item.storedPath).length().toInt())
        val out = File(ctx.cacheDir, "out_" + item.displayName)
        FileOutputStream(out).use { it.write(cipher.doFinal(encBody)) }
        return out
    }

    fun guessMime(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "zip" -> "application/zip"
            else -> "*/*"
        }
    }

    fun humanSize(b: Long): String {
        if (b < 1024) return "$b B"
        val kb = b / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }

    private fun queryDisplayName(ctx: Context, u: Uri): String? {
        val c = ctx.contentResolver.query(u, null, null, null, null) ?: return null
        c.use {
            val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && it.moveToFirst()) return it.getString(idx)
        }
        return null
    }
}
