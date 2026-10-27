package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ParsedReceiptItem(
    val name: String,
    val quantity: Int = 1,
    val totalPrice: Double,
    val unitPrice: Double = if (quantity > 0) totalPrice / quantity else totalPrice
)

data class ParsedReceipt(
    val items: List<ParsedReceiptItem>,
    val detectedSubtotal: Double?,
    val detectedTax: Double?,
    val detectedTotal: Double?,
    val rawText: String
)

object ReceiptParser {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeTextFromUri(context: Context, uri: Uri): ParsedReceipt {
        val image = InputImage.fromFilePath(context, uri)
        return recognizeImage(image)
    }

    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): ParsedReceipt {
        val image = InputImage.fromBitmap(bitmap, 0)
        return recognizeImage(image)
    }

    private suspend fun recognizeImage(image: InputImage): ParsedReceipt =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val parsed = parseVisionText(visionText)
                    continuation.resume(parsed)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }

    fun parseVisionText(visionText: Text): ParsedReceipt {
        // Collect all lines in top-to-bottom reading order
        val allLines = visionText.textBlocks
            .flatMap { it.lines }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }

        val rawFullText = visionText.text
        return parseLines(allLines, rawFullText)
    }

    fun parseRawText(rawText: String): ParsedReceipt {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return parseLines(lines, rawText)
    }

    private fun parseLines(lines: List<String>, rawFullText: String): ParsedReceipt {
        val parsedItems = mutableListOf<ParsedReceiptItem>()
        var detectedSubtotal: Double? = null
        var detectedTax: Double? = null
        var detectedTotal: Double? = null

        val priceRegex = Pattern.compile("""\$?\s*([0-9]+\.[0-9]{2})\b""")
        val qtyRegex = Pattern.compile("""^(\d{1,2})\s*[xX]?\s+(.*)""")

        val ignoreKeywords = listOf(
            "subtotal", "sub total", "sub-total",
            "tax", "sales tax", "state tax", "gst", "hst", "vat",
            "total", "grand total", "balance due", "amount due",
            "cash", "change", "credit", "visa", "mastercard", "amex", "debit",
            "tip", "gratuity", "suggested",
            "table", "server", "guest", "check #", "order #", "ticket #",
            "thank you", "welcome", "receipt", "tel:", "phone", "date:", "time:"
        )

        for (line in lines) {
            val lower = line.lowercase(Locale.US)

            // 1. Check for Subtotal
            if ((lower.contains("subtotal") || lower.contains("sub total") || lower.contains("sub-total")) && !lower.contains("tax")) {
                val matcher = priceRegex.matcher(line)
                var lastMatch: Double? = null
                while (matcher.find()) {
                    lastMatch = matcher.group(1)?.toDoubleOrNull()
                }
                if (lastMatch != null && (detectedSubtotal == null || lastMatch > (detectedSubtotal ?: 0.0))) {
                    detectedSubtotal = lastMatch
                }
                continue
            }

            // 2. Check for Tax
            if (lower.contains("tax") || lower.contains("gst") || lower.contains("hst") || lower.contains("vat")) {
                val matcher = priceRegex.matcher(line)
                var lastMatch: Double? = null
                while (matcher.find()) {
                    lastMatch = matcher.group(1)?.toDoubleOrNull()
                }
                if (lastMatch != null) {
                    detectedTax = lastMatch
                }
                continue
            }

            // 3. Check for Grand Total
            if ((lower.contains("total") || lower.contains("amount due") || lower.contains("balance")) &&
                !lower.contains("sub") && !lower.contains("tax")
            ) {
                val matcher = priceRegex.matcher(line)
                var lastMatch: Double? = null
                while (matcher.find()) {
                    lastMatch = matcher.group(1)?.toDoubleOrNull()
                }
                if (lastMatch != null && (detectedTotal == null || lastMatch > (detectedTotal ?: 0.0))) {
                    detectedTotal = lastMatch
                }
                continue
            }

            // 4. Skip metadata lines
            if (ignoreKeywords.any { lower.contains(it) }) {
                continue
            }

            // 5. Try to extract line items: [Qty] [Item Name] [Price]
            val matcher = priceRegex.matcher(line)
            var price: Double? = null
            var priceStart = -1
            while (matcher.find()) {
                price = matcher.group(1)?.toDoubleOrNull()
                priceStart = matcher.start()
            }

            if (price != null && price > 0.0 && price < 10000.0) {
                val textBeforePrice = if (priceStart > 0) line.substring(0, priceStart).trim() else ""
                val cleanDescription = textBeforePrice
                    .replace(Regex("""^\W+"""), "")
                    .replace(Regex("""\W+$"""), "")
                    .trim()

                if (cleanDescription.length >= 2) {
                    // Check for quantity prefix (e.g. "2x Burgers" or "2 Burgers")
                    val qtyMatcher = qtyRegex.matcher(cleanDescription)
                    val (quantity, name) = if (qtyMatcher.find()) {
                        val q = qtyMatcher.group(1)?.toIntOrNull() ?: 1
                        val n = qtyMatcher.group(2)?.trim() ?: cleanDescription
                        Pair(q.coerceAtLeast(1), n)
                    } else {
                        Pair(1, cleanDescription)
                    }

                    // Avoid duplicate line items with empty names or pure digits
                    if (name.any { it.isLetter() }) {
                        parsedItems.add(
                            ParsedReceiptItem(
                                name = name,
                                quantity = quantity,
                                totalPrice = price,
                                unitPrice = price / quantity
                            )
                        )
                    }
                }
            }
        }

        // Fallback: If subtotal wasn't explicitly labeled, sum parsed items
        val finalSubtotal = detectedSubtotal ?: if (parsedItems.isNotEmpty()) {
            parsedItems.sumOf { it.totalPrice }
        } else null

        return ParsedReceipt(
            items = parsedItems,
            detectedSubtotal = finalSubtotal,
            detectedTax = detectedTax,
            detectedTotal = detectedTotal,
            rawText = rawFullText
        )
    }
}
