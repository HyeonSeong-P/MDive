package com.parka.mdive.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.MDiveSkeleton

/** Screen-owned static layout; no prepared title or document content is loaded into the product. */
@Composable
internal fun ReaderLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).semantics(mergeDescendants = true) {
            contentDescription = "문서를 준비하고 있어요"
            stateDescription = "불러오는 중"
            liveRegion = LiveRegionMode.Polite
        },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MDiveSkeleton(Modifier.fillMaxWidth(0.65f).height(28.dp))
        Spacer(Modifier.height(8.dp))
        repeat(2) {
            MDiveSkeleton(Modifier.fillMaxWidth().height(14.dp))
            MDiveSkeleton(Modifier.fillMaxWidth().height(14.dp))
            MDiveSkeleton(Modifier.fillMaxWidth(0.75f).height(14.dp))
            Spacer(Modifier.height(8.dp))
        }
        MDiveSkeleton(Modifier.fillMaxWidth(0.45f).height(22.dp))
        MDiveSkeleton(Modifier.fillMaxWidth().height(14.dp))
        MDiveSkeleton(Modifier.fillMaxWidth(0.8f).height(14.dp))
    }
}
