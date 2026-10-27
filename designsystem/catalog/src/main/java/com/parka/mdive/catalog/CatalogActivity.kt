package com.parka.mdive.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.MaterialTheme
import com.parka.mdive.designsystem.MDiveButton
import com.parka.mdive.designsystem.MDiveButtonVariant
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.parka.mdive.designsystem.CatalogContent
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.designsystem.generated.CatalogThemes
import com.parka.mdive.designsystem.generated.catalogEntries
import com.parka.mdive.catalog.base.BaseViewModel
import com.parka.mdive.catalog.base.UiEffect
import com.parka.mdive.catalog.base.UiIntent
import com.parka.mdive.catalog.base.UiState

data class CatalogState(val darkOverride: Boolean? = null, val showLicenses: Boolean = false, val paletteId: String = "deepsea", val reviewExpanded: Boolean = false, val selectedEntryId: String? = null) : UiState
sealed interface CatalogIntent : UiIntent {
    data class ToggleTheme(val currentDark: Boolean) : CatalogIntent
    data class ShowLicenses(val show: Boolean) : CatalogIntent
    data class SelectPalette(val id: String) : CatalogIntent
    data object ToggleReview : CatalogIntent
    data class SelectEntry(val id: String?) : CatalogIntent
}
sealed interface CatalogEffect : UiEffect
class CatalogViewModel : BaseViewModel<CatalogState, CatalogIntent, CatalogEffect>(CatalogState()) {
    override fun onIntent(intent: CatalogIntent) {
        when (intent) {
            is CatalogIntent.ToggleTheme -> reduce { copy(darkOverride = !intent.currentDark) }
            CatalogIntent.ToggleReview -> reduce { copy(reviewExpanded = !reviewExpanded) }
            is CatalogIntent.SelectEntry -> {
                require(intent.id == null || catalogEntries.any { it.id == intent.id })
                reduce { copy(selectedEntryId = intent.id) }
            }
            is CatalogIntent.ShowLicenses -> reduce { copy(showLicenses = intent.show) }
            is CatalogIntent.SelectPalette -> {
                require(intent.id in CatalogThemes.names) { "Unknown catalog palette: ${intent.id}" }
                reduce { copy(paletteId = intent.id) }
            }
        }
    }
}

/** Standalone design-system review app; independent of the product application. */
class CatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[CatalogViewModel::class.java]
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val dark = state.darkOverride ?: isSystemInDarkTheme()
            BackHandler(enabled = state.selectedEntryId != null && !state.showLicenses) {
                viewModel.onIntent(CatalogIntent.SelectEntry(null))
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            LaunchedEffect(viewModel, lifecycle) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.effects.collect { } }
            }
            CatalogContent(
                darkTheme = dark,
                onToggleTheme = { viewModel.onIntent(CatalogIntent.ToggleTheme(dark)) },
                onShowLicenses = { viewModel.onIntent(CatalogIntent.ShowLicenses(true)) },
                paletteId = state.paletteId,
                onSelectPalette = { viewModel.onIntent(CatalogIntent.SelectPalette(it)) },
                reviewExpanded = state.reviewExpanded,
                onToggleReview = { viewModel.onIntent(CatalogIntent.ToggleReview) },
                selectedEntryId = state.selectedEntryId,
                onSelectEntry = { viewModel.onIntent(CatalogIntent.SelectEntry(it)) },
            )
            if (state.showLicenses) {
                val context = LocalContext.current
                val licenses = remember {
                    listOf("pretendard_OFL.txt", "jetbrains_mono_OFL.txt").joinToString("\n\n") { name ->
                        "$name\n\n" + context.assets.open("font_licenses/$name").bufferedReader().use { it.readText() }
                    }
                }
                MDiveDesignTheme(darkTheme = dark, paletteId = state.paletteId) {
                    Dialog(onDismissRequest = { viewModel.onIntent(CatalogIntent.ShowLicenses(false)) }) {
                        Surface(shape = RoundedCornerShape(10.dp)) {
                            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("글꼴 라이선스", style = MaterialTheme.typography.titleMedium)
                                Text(licenses, Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodyMedium)
                                MDiveButton("닫기", { viewModel.onIntent(CatalogIntent.ShowLicenses(false)) }, variant = MDiveButtonVariant.Quiet)
                            }
                        }
                    }
                }
            }
        }
    }
}
