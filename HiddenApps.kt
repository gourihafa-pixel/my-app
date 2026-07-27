package com.calcvault.app

import android.content.Context

object HiddenApps {
    private const val KEY = "hidden_apps_list"

    fun add(ctx: Context, pkg: String) {
        val s = get(ctx).toMutableSet()
        s.add(pkg)
        save(ctx, s)
    }

    fun remove(ctx: Context, pkg: String) {
        val s = get(ctx).toMutableSet()
        s.remove(pkg)
        save(ctx, s)
    }

    fun get(ctx: Context): List<String> {
        val sp = ctx.getSharedPreferences("vault", Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun isHidden(ctx: Context, pkg: String): Boolean = get(ctx).contains(pkg)

    private fun save(ctx: Context, list: Set<String>) {
        val sp = ctx.getSharedPreferences("vault", Context.MODE_PRIVATE)
        sp.edit().putString(KEY, list.joinToString(",")).apply()
    }
}
