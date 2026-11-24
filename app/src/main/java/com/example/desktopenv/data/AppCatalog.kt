package com.example.desktopenv.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(val label: String, val packageName: String)

/** Lista os apps com ícone de lançamento e abre apps por meio de Intents públicas. */
class AppCatalog(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun loadLaunchable(): List<AppEntry> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { AppEntry(label = it.loadLabel(pm).toString(), packageName = it.activityInfo.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launchIntent(packageName: String): Intent? =
        pm.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun icon(packageName: String): ImageBitmap? = runCatching {
        pm.getApplicationIcon(packageName).toBitmap(width = 96, height = 96).asImageBitmap()
    }.getOrNull()
}
