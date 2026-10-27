package com.parka.mdive.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** Static placeholder primitive. The screen owns layout and one accessible loading description. */
@Composable
fun MDiveSkeleton(modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).clearAndSetSemantics { })
}

/** Approved decorative, theme-tinted welcome illustration for product home and catalog. */
@Composable
fun MDiveWelcomeMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.mdive_document_plunge),
        contentDescription = null,
        modifier = modifier.width(160.dp).aspectRatio(1138f / 530f),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
    )
}
