package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SignatureStampGenerator {

    private var cachedSealBitmap: Bitmap? = null

    /**
     * Formats names so that first letter of each word is capital and rest lowercase:
     * e.g., "brayan shamir" -> "Brayan Shamir"
     */
    fun formatCapitalizedName(name: String): String {
        return name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }

    /**
     * Formats surnames to standard ALL CAPS:
     * e.g., "Andrade Salvador" -> "ANDRADE SALVADOR"
     */
    fun formatUppercaseSurname(surname: String): String {
        return surname.trim().uppercase()
    }

    /**
     * Generates a date string formatted matching Peru digital signature standard:
     * e.g., "20/10/2026 09:30:15-0500"
     */
    fun getFormattedCurrentDate(date: Date = Date()): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ssZ", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT-5")
        return sdf.format(date)
    }

    private fun getSealBitmap(context: Context): Bitmap? {
        if (cachedSealBitmap != null && !cachedSealBitmap!!.isRecycled) {
            return cachedSealBitmap
        }
        return try {
            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.sello_peru_autentico)
            cachedSealBitmap = bmp
            bmp
        } catch (e: Exception) {
            try {
                context.assets.open("sello_peru_autentico.png").use {
                    val bmp = BitmapFactory.decodeStream(it)
                    cachedSealBitmap = bmp
                    bmp
                }
            } catch (ex: Exception) {
                null
            }
        }
    }

    /**
     * Generates an authentic Peru Digital Signature stamp bitmap using the official
     * national emblem and bold typewriter/monospace typography matching ReFirma / RENIEC:
     *
     * Format:
     * [SELLO OFICIAL]  Firmado digitalmente por:
     * [CON ESCUDO Y]   APELLIDOS PrimerNombre
     * [FIRMA DIGITAL]  SegundoNombre FIR XXXXXXXX hard
     *                  Motivo: Soy el autor del
     *                  documento
     *                  Fecha: 20/10/2026 09:18:40-0500
     */
    fun generateAuthenticPeruStamp(
        context: Context,
        apellidos: String,
        nombres: String,
        firId: String,
        signerReason: String = "Soy el autor del documento",
        dateString: String,
        transparentBackground: Boolean = true
    ): Bitmap {
        // Dimensions matching natural signature aspect ratio ~ 3.33:1
        val width = 450
        val height = 135
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (!transparentBackground) {
            canvas.drawColor(Color.WHITE)
        }

        // Draw authentic seal image on left preserving natural 1.366:1 aspect ratio
        val sealBmp = getSealBitmap(context)
        val sealHeight = 118f
        val sealWidth = sealHeight * (724f / 530f) // Exact 1.366 aspect ratio -> ~161px
        val sealLeft = 6f
        val sealTop = (height - sealHeight) / 2f

        if (sealBmp != null) {
            val destRect = RectF(sealLeft, sealTop, sealLeft + sealWidth, sealTop + sealHeight)
            val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(sealBmp, null, destRect, sealPaint)
        }

        // Text formatting using compact, dark bold Sans-Serif matching authentic ReFirma
        val textStartX = sealLeft + sealWidth + 7f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = Color.BLACK
            textSize = 14.5f
            letterSpacing = -0.015f // Compact letter spacing ("más pegada")
            isFakeBoldText = true // Ensure strong, thick print ink
        }

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = Color.BLACK
            textSize = 15.5f
            letterSpacing = -0.015f
            isFakeBoldText = true
        }

        val formattedSurnames = formatUppercaseSurname(apellidos)
        val nameTokens = nombres.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            .map { it.lowercase().replaceFirstChar { c -> c.titlecase(Locale.ROOT) } }

        val firstName = nameTokens.firstOrNull() ?: ""
        val secondName = if (nameTokens.size > 1) nameTokens.drop(1).joinToString(" ") else ""

        val cleanFir = if (firId.isBlank()) "FIR 46725678 hard" else if (!firId.contains("FIR", ignoreCase = true)) "FIR $firId hard" else firId

        val line1 = "Firmado digitalmente por:"
        val line2 = "$formattedSurnames $firstName".trim()
        val line3 = if (secondName.isNotBlank()) "$secondName $cleanFir" else cleanFir

        val reasonText = if (signerReason.isBlank()) "Soy el autor del documento" else signerReason.removePrefix("Motivo:").trim()
        val line4 = "Motivo: Soy el autor del"
        val line5 = if (reasonText.contains("documento", ignoreCase = true)) "documento" else reasonText

        val cleanDate = if (dateString.isBlank()) getFormattedCurrentDate() else dateString.removePrefix("Fecha:").trim()
        val line6 = "Fecha: $cleanDate"

        // Draw 6 lines with compact line height ("más pegada")
        var y = sealTop + 16f
        val lineSpacing = 18f

        canvas.drawText(line1, textStartX, y, textPaint)
        y += lineSpacing
        canvas.drawText(line2, textStartX, y, namePaint)
        y += lineSpacing
        canvas.drawText(line3, textStartX, y, textPaint)
        y += lineSpacing
        canvas.drawText(line4, textStartX, y, textPaint)
        y += lineSpacing
        canvas.drawText(line5, textStartX, y, textPaint)
        y += lineSpacing
        canvas.drawText(line6, textStartX, y, textPaint)

        return bitmap
    }

    /**
     * Converts near-white pixels into transparent pixels so the cropped signature
     * can overlay smoothly over documents and table lines.
     */
    fun removeWhiteBackground(source: Bitmap, threshold: Int = 232): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)

            if (r >= threshold && g >= threshold && b >= threshold) {
                pixels[i] = Color.TRANSPARENT
            } else {
                pixels[i] = color
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Replaces the date line on an existing cropped signature bitmap with a fresh date string.
     */
    fun patchDateOnCroppedSignature(
        source: Bitmap,
        newDateString: String,
        eraseWhiteBackground: Boolean = true
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val dateBoxTop = height * 0.80f
        val dateBoxBottom = height * 0.99f
        val dateBoxLeft = width * 0.30f
        val dateBoxRight = width * 0.99f

        val erasePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(dateBoxLeft, dateBoxTop, dateBoxRight, dateBoxBottom, erasePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            color = Color.BLACK
            textSize = (height * 0.11f).coerceIn(12f, 28f)
            isFakeBoldText = true
        }

        val displayDate = if (newDateString.startsWith("Fecha:")) newDateString else "Fecha: $newDateString"
        val textY = dateBoxTop + (dateBoxBottom - dateBoxTop) * 0.70f
        canvas.drawText(displayDate, dateBoxLeft + 4f, textY, textPaint)

        return if (eraseWhiteBackground) {
            removeWhiteBackground(output)
        } else {
            output
        }
    }
}
