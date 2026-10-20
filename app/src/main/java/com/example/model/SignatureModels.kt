package com.example.model

import android.graphics.Bitmap
import android.graphics.RectF
import java.io.File

enum class SignatureSourceType {
    OFFICIAL_PERU_STAMP,
    CROPPED_FROM_PDF1,
    IMAGE_FROM_GALLERY
}

data class PdfDocumentState(
    val file: File? = null,
    val fileName: String = "",
    val pageCount: Int = 0,
    val currentPageIndex: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SignatureData(
    val id: String = System.currentTimeMillis().toString(),
    val sourceType: SignatureSourceType = SignatureSourceType.OFFICIAL_PERU_STAMP,
    val bitmap: Bitmap,
    val signerName: String = "ANDRADE SALVADOR BRAYAN SHAMIR",
    val signerId: String = "FIR 46725678 hard",
    val signerReason: String = "Soy el autor del documento",
    val signatureDate: String = "",
    val isTransparentBackground: Boolean = true,
    val originalCroppedBitmap: Bitmap? = null
)

data class PlacementConfig(
    val targetPageIndex: Int = 0,
    val xPercent: Float = 0.65f, // Position X center or top-left as fraction of page (0..1)
    val yPercent: Float = 0.44f, // Position Y center or top-left as fraction of page (0..1)
    val widthPercent: Float = 0.28f, // Width as fraction of page
    val heightPercent: Float = 0.08f, // Height as fraction of page
    val rotation: Float = 0f, // Degrees
    val opacity: Float = 1.0f, // 0..1
    val lockAspectRatio: Boolean = true
)

enum class AppStep {
    DOCUMENTS,
    SIGNATURE_AND_DATE,
    PLACE_AND_ADJUST,
    EXPORT_AND_SHARE
}
