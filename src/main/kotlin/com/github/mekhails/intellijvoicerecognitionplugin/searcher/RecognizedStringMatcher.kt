package com.github.mekhails.intellijvoicerecognitionplugin.searcher

class RecognizedStringMatcher(
    val recognizedString: String,
    val actionPhrase: String
) {
    val matchRate: Float by lazy { computeMatchRate() }

    private fun computeMatchRate(): Float {
        val recognizedWords = recognizedString.split(" ")
        val actionWords = actionPhrase.split(" ")
        return recognizedWords.maxOfOrNull { recognizedWord ->
            actionWords.maxOfOrNull { actionWord ->
                matchWords(recognizedWord, actionPhrase)
            } ?: 0F
        } ?: 0F
    }

    private fun matchWords(recognizedWord: String, actionWord: String) : Float {
        val longestCommonSubstringLength = computeLongestCommonSubstringLength(recognizedWord, actionWord)
        return recognizedWord.length.toFloat() / longestCommonSubstringLength.toFloat()
    }

    private fun computeLongestCommonSubstringLength(strA: String, strB: String): Int {
        val longestCommonSubstringTable = Array(strA.length + 1) { IntArray(strB.length + 1) }

        var result = 0
        for (i in 0..strA.length) {
            for (j in 0..strB.length) {
                if (i == 0 || j == 0) {
                    longestCommonSubstringTable[i][j] = 0
                }
                else if (strA[i - 1] == strB[j - 1]) {
                    longestCommonSubstringTable[i][j] = longestCommonSubstringTable[i - 1][j - 1] + 1
                    result = Integer.max(result, longestCommonSubstringTable[i][j])
                }
                else {
                    longestCommonSubstringTable[i][j] = 0
                }
            }
        }
        return result
    }
}