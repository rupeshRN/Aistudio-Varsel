package com.varsel.expensetracker.category

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Canonical merchant/description normalization.
 *
 * Every learning component should normalize descriptions
 * through this class before matching.
 */
@Singleton
class DescriptionNormalizer @Inject constructor() {

    fun normalize(

        description: String

    ): String {

        return description

            //--------------------------------------------------
            // Ignore case
            //--------------------------------------------------
            .lowercase()

            //--------------------------------------------------
            // Replace punctuation
            //--------------------------------------------------
            .replace(
                Regex("[^a-z0-9 ]"),
                " "
            )

            //--------------------------------------------------
            // Remove long numeric IDs
            //--------------------------------------------------
            .replace(
                Regex("\\b\\d{5,}\\b"),
                ""
            )

            //--------------------------------------------------
            // Collapse spaces
            //--------------------------------------------------
            .replace(
                Regex("\\s+"),
                " "
            )

            //--------------------------------------------------
            // Trim
            //--------------------------------------------------
            .trim()

    }

}
