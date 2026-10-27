package com.parka.mdive.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/** Catalog compositions illustrate the primitive; real screen layout remains in the product app. */
@Composable
internal fun CatalogLoadingSkeleton(home: Boolean) {
    Column(Modifier.fillMaxWidth().padding(16.dp).semantics {
        contentDescription = if (home) "홈 목록 로딩 예시" else "읽기 본문 로딩 예시"
        stateDescription = "불러오는 중"
    }, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MDiveSkeleton(Modifier.fillMaxWidth(0.6f).height(if (home) 24.dp else 28.dp))
        if (home) MDiveSkeleton(Modifier.fillMaxWidth().height(48.dp))
        repeat(2) {
            Spacer(Modifier.height(8.dp))
            if (home) MDiveSkeleton(Modifier.fillMaxWidth(0.3f).height(18.dp))
            repeat(3) { index -> MDiveSkeleton(Modifier.fillMaxWidth(if (index == 2) 0.65f else 1f).height(14.dp)) }
        }
    }
}
