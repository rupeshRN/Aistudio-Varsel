package com.varsel.expensetracker.ui.recurring.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.ui.recurring.util.SubscriptionBrandCatalog

@Composable
fun SubscriptionBrandBadge(
    title: String,
    category: String,
    categoryColorHex: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shapeRadius: Dp = 12.dp
) {
    val brandLogoRes = SubscriptionBrandCatalog.getBrandLogo(title)

    if (brandLogoRes != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(shapeRadius))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(shapeRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = brandLogoRes),
                contentDescription = "$title logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Fallback: Category Icon
        val defaultColor = MaterialTheme.colorScheme.primary
        val parsedColor = if (!categoryColorHex.isNullOrBlank()) {
            val colorInt = runCatching { android.graphics.Color.parseColor(categoryColorHex) }.getOrNull()
            if (colorInt != null) Color(colorInt) else defaultColor
        } else {
            defaultColor
        }

        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(parsedColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIconCatalog.iconFor(category),
                contentDescription = category,
                tint = parsedColor,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
