package com.calcvault.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Boot receiver — placeholder. We don't autostart anything on boot because
 * drawing over the lock screen with our own icon would break the "calculator
 * disguise" purpose. Instead, this exists so the manifest keeps the
 * RECEIVE_BOOT_COMPLETED permission request for users who want to extend it
 * (for example, to relock the vault on reboot via secure prefs re-init).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // no-op; included for future extensions
    }
}
