package com.varsel.expensetracker.util

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OcrManager @Inject constructor() {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val task = textRecognizer.process(image)
                val result = Tasks.await(task)
                val recognizedText = result.text
                if (recognizedText.isBlank()) null else recognizedText
            } catch (e: Exception) {
                null
            }
        }
    }
}
