package com.parka.mdive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.parka.mdive.domain.document.DocumentReference
import com.parka.mdive.ui.home.HomeRoute
import com.parka.mdive.ui.reader.ReaderRoute
import com.parka.mdive.ui.reader.ReaderViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable data object Home : Destination
    @Serializable data class Reader(val reference: String) : Destination
}

/** The UI owns both platform selection and the serializable document locator in the back stack. */
@Composable
fun MDiveApp(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey> = rememberNavBackStack(Destination.Home),
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Destination.Home> {
                HomeRoute(onDocumentSelected = { reference ->
                    if (backStack.lastOrNull() == Destination.Home) backStack.add(Destination.Reader(reference.value))
                })
            }
            entry<Destination.Reader> { destination ->
                val viewModel = hiltViewModel<ReaderViewModel>()
                ReaderRoute(
                    onNavigateHome = { if (backStack.size > 1) backStack.removeLastOrNull() },
                    reference = DocumentReference(destination.reference),
                    viewModel = viewModel,
                )
            }
        },
    )
}
