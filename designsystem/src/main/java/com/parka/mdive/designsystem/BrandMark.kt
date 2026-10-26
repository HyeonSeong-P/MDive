package com.parka.mdive.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Brand identity, separate from action icons and the launcher's background/mask. */
@Composable
fun MDiveBrandMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = "MDive",
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Image(
        painter = painterResource(R.drawable.mdive_brand_mark),
        contentDescription = contentDescription,
        modifier = modifier.size(width = 48.dp, height = 36.dp),
        colorFilter = if (tint == Color.Unspecified) null else ColorFilter.tint(tint),
    )
}
