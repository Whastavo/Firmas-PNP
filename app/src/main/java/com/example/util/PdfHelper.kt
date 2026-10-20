package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PdfHelper {

    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun renderPage(
        file: File,
        pageIndex: Int,
        renderScale: Float = 2.0f
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (pageIndex !in 0 until renderer.pageCount) {
                renderer.close()
                pfd.close()
                return@withContext null
            }
            val page = renderer.openPage(pageIndex)
            val width = (page.width * renderScale).toInt().coerceAtLeast(1)
            val height = (page.height * renderScale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            // Fill background white
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun copyAssetToFile(context: Context, assetPath: String, destFile: File): File = withContext(Dispatchers.IO) {
        context.assets.open(assetPath).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile
    }

    suspend fun copyUriToFile(context: Context, uri: Uri, destFile: File): File = withContext(Dispatchers.IO) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        requireNotNull(inputStream) { "Unable to open input stream from uri: $uri" }
        inputStream.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile
    }

    /**
     * Stamped signature export:
     * Reads all pages from basePdfFile, renders and builds a new PDF document,
     * stamping the signature bitmap onto targetPageIndex at normalized coordinates.
     */
    suspend fun exportSignedPdf(
        context: Context,
        basePdfFile: File,
        outputPdfFile: File,
        targetPageIndex: Int,
        signatureBitmap: Bitmap,
        normX: Float, // 0..1 (fraction of page width for left edge)
        normY: Float, // 0..1 (fraction of page height for top edge)
        normWidth: Float, // 0..1
        normHeight: Float, // 0..1
        rotationDegrees: Float = 0f,
        opacity: Float = 1.0f
    ): Boolean = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()

        try {
            pfd = ParcelFileDescriptor.open(basePdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val nativeWidth = page.width
                val nativeHeight = page.height

                // Render page to high-res bitmap (1.5x - 2.0x for sharpness)
                val scale = 2.0f
                val renderWidth = (nativeWidth * scale).toInt()
                val renderHeight = (nativeHeight * scale).toInt()
                val pageBitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                // Create PDF Document page
                val pageInfo = PdfDocument.PageInfo.Builder(nativeWidth, nativeHeight, i + 1).create()
                val docPage = pdfDocument.startPage(pageInfo)
                val canvas = docPage.canvas

                // Draw base page background
                val destRect = Rect(0, 0, nativeWidth, nativeHeight)
                val srcRect = Rect(0, 0, renderWidth, renderHeight)
                val basePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(pageBitmap, srcRect, destRect, basePaint)
                pageBitmap.recycle()

                // If this is the target page, overlay the signature
                if (i == targetPageIndex) {
                    val bmpRatio = if (signatureBitmap.height > 0) {
                        signatureBitmap.width.toFloat() / signatureBitmap.height.toFloat()
                    } else {
                        3.6f
                    }
                    val sigWidth = normWidth * nativeWidth
                    val sigHeight = sigWidth / bmpRatio
                    val sigLeft = normX * nativeWidth
                    val sigTop = normY * nativeHeight

                    val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        alpha = (opacity.coerceIn(0f, 1f) * 255).toInt()
                    }

                    canvas.save()
                    if (rotationDegrees != 0f) {
                        val centerX = sigLeft + sigWidth / 2f
                        val centerY = sigTop + sigHeight / 2f
                        canvas.rotate(rotationDegrees, centerX, centerY)
                    }

                    val sigDestRect = RectF(sigLeft, sigTop, sigLeft + sigWidth, sigTop + sigHeight)
                    canvas.drawBitmap(signatureBitmap, null, sigDestRect, sigPaint)
                    canvas.restore()
                }

                pdfDocument.finishPage(docPage)
            }

            FileOutputStream(outputPdfFile).use { out ->
                pdfDocument.writeTo(out)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                renderer?.close()
                pfd?.close()
                pdfDocument.close()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Creates an Android Share Intent to send the generated PDF to WhatsApp, Gmail, etc.
     */
    fun createShareIntent(context: Context, pdfFile: File): Intent {
        val authority = "${context.packageName}.provider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Documento PDF firmado: ${pdfFile.name}")
            putExtra(Intent.EXTRA_TEXT, "Adjunto documento PDF con firma digital actualizada.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Creates an Intent to view the PDF in an external PDF reader app.
     */
    fun createViewIntent(context: Context, pdfFile: File): Intent {
        val authority = "${context.packageName}.provider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Saves the signed PDF to the device's public Downloads directory.
     */
    suspend fun savePdfToDownloads(context: Context, pdfFile: File, targetName: String = pdfFile.name): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, targetName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
            }
            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                android.provider.MediaStore.Files.getContentUri("external")
            }
            val uri = contentResolver.insert(collection, contentValues) ?: return@withContext null
            contentResolver.openOutputStream(uri)?.use { outStream ->
                pdfFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves the transparent signature image to the device's Downloads directory as a PNG.
     */
    suspend fun saveSignatureImageToDownloads(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "firma_digital_${System.currentTimeMillis()}.png"
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
            }
            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val uri = contentResolver.insert(collection, contentValues) ?: return@withContext null
            contentResolver.openOutputStream(uri)?.use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
