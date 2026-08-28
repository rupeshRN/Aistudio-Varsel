package com.varsel.expensetracker.parser

import javax.inject.Inject

class TokenNormalizer @Inject constructor() {

fun normalize(
    tokens: List<String>
): List<String> {

    return tokens.map {

        var value = repairHyphenArtifacts(it)

        value = repairBrokenWords(value)

        value
    }
}

    //----------------------------------------------------
    // Layout repair only
    //----------------------------------------------------

private fun repairHyphenArtifacts(
    token: String
): String {

    var text = token.trim()

    //------------------------------------------------
    // Rule 1
    // Remove trailing layout hyphen
    //
    // DECATHLON -
    // ->
    // DECATHLON
    //------------------------------------------------

    text = text.replace(
        Regex("\\s*-\\s*$"),
        ""
    )

    //------------------------------------------------
    // Rule 2
    // Remove layout separator
    //
    // AJAY - SINGH
    // Indian - Railways
    // UPI - RVSL
    //------------------------------------------------

    text = text.replace(
        Regex("\\s+-\\s+"),
        " "
    )

    //------------------------------------------------
    // Rule 3
    // Join broken merchant words
    //
    // WAHEGUR U PETROLEUM
    // ->
    // WAHEGURU PETROLEUM
    //
    // HARIPRAS ATH K
    // ->
    // HARIPRASATH K
    //
    // Only joins if:
    // - left word is reasonably long
    // - middle fragment is short
    //------------------------------------------------

    text = text.replace(

        Regex("\\b([A-Za-z]{5,})\\s+([A-Za-z]{1,3})\\b(?=\\s+[A-Za-z])")

    ) {

        val left = it.groupValues[1]
        val right = it.groupValues[2]

        left + right
    }

    //------------------------------------------------
    // Cleanup
    //------------------------------------------------

    return text
        .replace(
            Regex("\\s+"),
            " "
        )
        .trim()
}

//------------------------------------------------
// Broken word repair
//
// samos a
// ->
// samosa
//------------------------------------------------

private fun repairBrokenWords(
    token: String
): String {

    val words =
        token.split(Regex("\\s+"))
            .toMutableList()

    if (words.size < 2)
        return token

    val repaired =
        mutableListOf<String>()

    var i = 0

    while (i < words.size) {

        //------------------------------------------------
        // Last word
        //------------------------------------------------

        if (i == words.lastIndex) {

            repaired.add(words[i])
            break
        }

        val current =
            words[i]

        val next =
            words[i + 1]

        //------------------------------------------------
        // Join if:
        //
        // current >= 4 letters
        // next <= 2 letters
        //
        // samos a
        // restaur ant
        //------------------------------------------------

        val shouldJoin =

            current.length >= 4 &&
            next.length <= 2 &&
            current.all(Char::isLetter) &&
            next.all(Char::isLetter)

        if (shouldJoin) {

            repaired.add(current + next)

            i += 2

        } else {

            repaired.add(current)

            i++
        }
    }

    return repaired.joinToString(" ")
}
}
