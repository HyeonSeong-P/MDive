package com.parka.mdive.ui.reader

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Restrict URL handling to installed browsers, including URLs with verified native app links. */
internal fun openBrowser(
    context: Context,
    destination: String,
    candidates: (Uri) -> List<Intent> = { browserIntents(context, it) },
    launch: (Intent) -> Unit = { context.startActivity(it) },
): Boolean = try {
    val uri = Uri.parse(destination)
    if (uri.scheme?.lowercase(java.util.Locale.ROOT) !in setOf("http", "https") || uri.host.isNullOrBlank() || uri.userInfo != null) false
    else {
        val intents = candidates(uri)
        if (intents.isEmpty()) false else {
            launch(if (intents.size == 1) intents.first() else Intent.createChooser(intents.first(), "브라우저로 열기")
                .putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.drop(1).toTypedArray()))
            true
        }
    }
} catch (_: android.content.ActivityNotFoundException) { false }
catch (_: SecurityException) { false }

private fun browserIntents(context: Context, uri: Uri): List<Intent> {
    val browserQuery = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
    @Suppress("DEPRECATION")
    val browsers = context.packageManager.queryIntentActivities(browserQuery, 0).map { it.activityInfo.packageName }.distinct()
    return browsers.mapNotNull { name ->
        val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(name)
        @Suppress("DEPRECATION")
        if (context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()) intent else null
    }
}
