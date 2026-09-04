package com.varsel.expensetracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

sealed class PdfExtractionResult {
    data class Success(val text: String) : PdfExtractionResult()
    object PasswordRequired : PdfExtractionResult()
    object InvalidPassword : PdfExtractionResult()
    data class Error(val message: String? = null) : PdfExtractionResult()
}

class PdfTextExtractor @Inject constructor() {

    private val textRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromPdf(
        context: Context,
        uri: Uri,
        password: String? = null
    ): PdfExtractionResult {

        return withContext(Dispatchers.IO) {

            try {

                // --------------------------------------------------------
                // STEP 1 : Try extracting embedded text using PDFBox
                // --------------------------------------------------------

                PDFBoxResourceLoader.init(context)

                context.contentResolver.openInputStream(uri)?.use { input ->

                    val document = try {
                        if (!password.isNullOrEmpty()) {
                            PDDocument.load(input, password)
                        } else {
                            PDDocument.load(input)
                        }
                    } catch (e: InvalidPasswordException) {
                        return@withContext if (password.isNullOrEmpty()) {
                            PdfExtractionResult.PasswordRequired
                        } else {
                            PdfExtractionResult.InvalidPassword
                        }
                    } catch (e: IOException) {
                        val msg = e.message?.lowercase().orEmpty()
                        if (msg.contains("password") || msg.contains("encrypted") || msg.contains("protection")) {
                            return@withContext if (password.isNullOrEmpty()) {
                                PdfExtractionResult.PasswordRequired
                            } else {
                                PdfExtractionResult.InvalidPassword
                            }
                        } else {
                            throw e
                        }
                    } catch (e: Exception) {
                        val msg = e.message?.lowercase().orEmpty()
                        if (msg.contains("password") || msg.contains("encrypted") || msg.contains("protection")) {
                            return@withContext if (password.isNullOrEmpty()) {
                                PdfExtractionResult.PasswordRequired
                            } else {
                                PdfExtractionResult.InvalidPassword
                            }
                        } else {
                            throw e
                        }
                    }

                    document.use { doc ->
                        val stripper = PDFTextStripper()
                        stripper.sortByPosition = true

                        val pdfText = try {
                            stripper.getText(doc)
                        } catch (e: InvalidPasswordException) {
                            return@withContext if (password.isNullOrEmpty()) {
                                PdfExtractionResult.PasswordRequired
                            } else {
                                PdfExtractionResult.InvalidPassword
                            }
                        } catch (e: Exception) {
                            val msg = e.message?.lowercase().orEmpty()
                            if (msg.contains("password") || msg.contains("encrypted") || msg.contains("protection")) {
                                return@withContext if (password.isNullOrEmpty()) {
                                    PdfExtractionResult.PasswordRequired
                                } else {
                                    PdfExtractionResult.InvalidPassword
                                }
                            } else {
                                throw e
                            }
                        }

                        if (pdfText.isNotBlank()) {
                            return@withContext PdfExtractionResult.Success(pdfText.trim())
                        }
                    }

                }

            } catch (e: Exception) {
                val msg = e.message?.lowercase().orEmpty()
                if (msg.contains("password") || msg.contains("encrypted") || msg.contains("protection")) {
                    return@withContext if (password.isNullOrEmpty()) {
                        PdfExtractionResult.PasswordRequired
                    } else {
                        PdfExtractionResult.InvalidPassword
                    }
                }
                // Ignore and fall back to OCR
            }

            // --------------------------------------------------------
            // STEP 2 : OCR fallback (for scanned PDFs)
            // --------------------------------------------------------

            var fileDescriptor: ParcelFileDescriptor? = null
            var pdfRenderer: PdfRenderer? = null

            try {

                fileDescriptor =
                    context.contentResolver.openFileDescriptor(uri, "r")
                        ?: return@withContext PdfExtractionResult.Error("Could not open file descriptor")

                pdfRenderer = PdfRenderer(fileDescriptor)

                val fullText = StringBuilder()

                for (pageIndex in 0 until pdfRenderer.pageCount) {

                    val page = pdfRenderer.openPage(pageIndex)

                    val bitmap = Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )

                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                    )

                    page.close()

                    val image = InputImage.fromBitmap(bitmap, 0)

                    val result = Tasks.await(
                        textRecognizer.process(image)
                    )

                    if (result.text.isNotBlank()) {

                        fullText.append(result.text)

                        fullText.append("\n")

                    }

                    bitmap.recycle()

                }

                val text = fullText.toString().trim()

                if (text.isBlank()) {
                    PdfExtractionResult.Error("Could not extract text from document")
                } else {
                    PdfExtractionResult.Success(text)
                }

            } catch (e: SecurityException) {
                if (password.isNullOrEmpty()) {
                    PdfExtractionResult.PasswordRequired
                } else {
                    PdfExtractionResult.InvalidPassword
                }
            } catch (e: Exception) {

                PdfExtractionResult.Error(e.message ?: "Failed to process PDF file")

            } finally {

                try {
                    pdfRenderer?.close()
                    fileDescriptor?.close()
                } catch (_: Exception) {
                }

            }

        }

    }

}
