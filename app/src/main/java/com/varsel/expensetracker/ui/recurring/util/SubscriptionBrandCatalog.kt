package com.varsel.expensetracker.ui.recurring.util

import androidx.annotation.DrawableRes
import com.varsel.expensetracker.R

data class SubscriptionPreset(
    val brandName: String,
    @DrawableRes val logoRes: Int,
    val defaultCategory: String = "Entertainment",
    val keywords: List<String>
)

object SubscriptionBrandCatalog {

    val supportedBrands: List<SubscriptionPreset> = listOf(
        SubscriptionPreset(
            brandName = "Netflix",
            logoRes = R.drawable.ic_logo_netflix,
            defaultCategory = "Entertainment",
            keywords = listOf("netflix", "nflx")
        ),
        SubscriptionPreset(
            brandName = "Disney+ Hotstar",
            logoRes = R.drawable.ic_logo_hotstar,
            defaultCategory = "Entertainment",
            keywords = listOf("hotstar", "disney", "disney+", "disney plus", "disney+ hotstar")
        ),
        SubscriptionPreset(
            brandName = "Prime Video",
            logoRes = R.drawable.ic_logo_prime_video,
            defaultCategory = "Entertainment",
            keywords = listOf("prime", "prime video", "amazon prime", "amazon video", "amzn prime")
        ),
        SubscriptionPreset(
            brandName = "Sony LIV",
            logoRes = R.drawable.ic_logo_sonyliv,
            defaultCategory = "Entertainment",
            keywords = listOf("sonyliv", "sony liv", "sony", "liv")
        ),
        SubscriptionPreset(
            brandName = "FanCode",
            logoRes = R.drawable.ic_logo_fancode,
            defaultCategory = "Entertainment",
            keywords = listOf("fancode", "fan code", "dream sports")
        ),
        SubscriptionPreset(
            brandName = "Spotify",
            logoRes = R.drawable.ic_logo_spotify,
            defaultCategory = "Entertainment",
            keywords = listOf("spotify", "spot")
        )
    )

    @DrawableRes
    fun getBrandLogo(title: String): Int? {
        val clean = title.trim().lowercase()
        if (clean.isBlank()) return null

        for (brand in supportedBrands) {
            if (brand.keywords.any { keyword ->
                    clean == keyword || clean.contains(keyword) || keyword.contains(clean)
                }) {
                return brand.logoRes
            }
        }
        return null
    }

    fun findMatchingBrand(title: String): SubscriptionPreset? {
        val clean = title.trim().lowercase()
        if (clean.isBlank()) return null

        return supportedBrands.firstOrNull { brand ->
            brand.keywords.any { keyword ->
                clean == keyword || clean.contains(keyword) || keyword.contains(clean)
            }
        }
    }
}
