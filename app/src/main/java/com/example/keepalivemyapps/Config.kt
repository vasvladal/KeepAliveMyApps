package com.example.keepalivemyapps

import android.content.Context
import android.content.SharedPreferences

object Config {
    private const val PREFS_NAME = "AppConfig"
    private const val KEY_TARGET_PACKAGE = "target_package"
    private const val DEFAULT_PACKAGE = "com.example.unwantedcallblocker"

    fun getTargetPackage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TARGET_PACKAGE, DEFAULT_PACKAGE) ?: DEFAULT_PACKAGE
    }

    fun setTargetPackage(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TARGET_PACKAGE, packageName).apply()
    }
}