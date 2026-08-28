package com.varsel.expensetracker.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape

object AppShapes {

    val SmallCard =
        RoundedCornerShape(
            AppDimensions.SmallCornerRadius
        )

    val Card =
        RoundedCornerShape(
            AppDimensions.CardCornerRadius
        )

    val HeroCard =
        RoundedCornerShape(
            AppDimensions.LargeCardCornerRadius
        )
}
