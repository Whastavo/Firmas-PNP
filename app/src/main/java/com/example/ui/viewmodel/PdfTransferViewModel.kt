package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppStep
import com.example.model.PdfDocumentState
import com.example.model.PlacementConfig
import com.example.model.SignatureSourceType
import com.example.util.PdfHelper
import com.example.util.SignatureStampGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val currentStep: AppStep = AppStep.DOCUMENTS,
    val pdf1: PdfDocumentState = PdfDocumentState(),
    val pdf2: PdfDocumentState = PdfDocumentState(),
    val signatureType: SignatureSourceType = SignatureSourceType.OFFICIAL_PERU_STAMP,
    // Fields for authentic Peru stamp
    val signerApellidos: String = "ANDRADE SALVADOR",
    val signerNombres: String = "Brayan Shamir",
    val signerId: String = "FIR 46725678 hard",
    val signerReason: String = "Soy el autor del documento",
    val signatureDate: String = SignatureStampGenerator.getFormattedCurrentDate(),
    // Active signature bitmap to be stamped
    val activeSignatureBitmap: Bitmap? = null,
    // Original cropped or loaded bitmap
    val rawSignatureBitmap: Bitmap? = null,
    val removeWhiteBackground: Boolean = true,
    // Cropping coordinates on PDF 1 (fractions 0..1)
    val cropRect: RectF = RectF(0.60f, 0.15f, 0.90f, 0.23f),
    // Placement configuration on PDF 2
    val placement: PlacementConfig = PlacementConfig(
        targetPageIndex = 0,
        xPercent = 0.585f,
        yPercent = 0.446f,
        widthPercent = 0.285f,
        heightPercent = 0.058f,
        rotation = 0f,
        opacity = 1.0f,
        lockAspectRatio = true
    ),
    // Export state
    val isExporting: Boolean = false,
    val exportedFile: File? = null,
    val exportedPdfPageBitmap: Bitmap? = null,
    val exportErrorMessage: String? = null,
    val toastMessage: String? = null
)

class PdfTransferViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Generate initial signature stamp
        regenerateStampBitmap()
    }

    fun setStep(step: AppStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun nextStep() {
        val next = when (_uiState.value.currentStep) {
            AppStep.DOCUMENTS -> AppStep.SIGNATURE_AND_DATE
            AppStep.SIGNATURE_AND_DATE -> AppStep.PLACE_AND_ADJUST
            AppStep.PLACE_AND_ADJUST -> AppStep.EXPORT_AND_SHARE
            AppStep.EXPORT_AND_SHARE -> AppStep.DOCUMENTS
        }
        _uiState.update { it.copy(currentStep = next) }
    }

    fun prevStep() {
        val prev = when (_uiState.value.currentStep) {
            AppStep.DOCUMENTS -> AppStep.DOCUMENTS
            AppStep.SIGNATURE_AND_DATE -> AppStep.DOCUMENTS
            AppStep.PLACE_AND_ADJUST -> AppStep.SIGNATURE_AND_DATE
            AppStep.EXPORT_AND_SHARE -> AppStep.PLACE_AND_ADJUST
        }
        _uiState.update { it.copy(currentStep = prev) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    /**
     * Loads the 2 demo PDF files sent with the prompt from app assets:
     * 1) ACTA_DE_VERIFICACION... (PDF 1 with Brayan's signature)
     * 2) CONSTANCIA_DE_ENTERADO... (PDF 2 where Brayan's signature is missing in row 02)
     */
    fun loadDemoSamples() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                val cacheDir = context.cacheDir
                val file1 = File(cacheDir, "ACTA_DE_VERIFICACION_DE_ARMA_DE_FUEGO.pdf")
                val file2 = File(cacheDir, "CONSTANCIA_DE_ENTERADO_006.pdf")

                PdfHelper.copyAssetToFile(
                    context,
                    "samples/ACTA_DE_VERIFICACION_DE_ARMA_DE_FUEGO_SET2026__1__95551197.pdf",
                    file1
                )
                PdfHelper.copyAssetToFile(
                    context,
                    "samples/CONSTANCIA_DE_ENTERADO_006_R__R__a1eada21.pdf",
                    file2
                )

                loadPdf1(file1, "ACTA_DE_VERIFICACION (Muestra)")
                loadPdf2(file2, "CONSTANCIA_DE_ENTERADO (Muestra)")

                _uiState.update {
                    it.copy(
                        toastMessage = "Documentos de prueba cargados con éxito."
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(toastMessage = "Error cargando muestras: ${e.localizedMessage}")
                }
            }
        }
    }

    fun loadPdf1FromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val destFile = File(context.cacheDir, "pdf1_source_${System.currentTimeMillis()}.pdf")
                PdfHelper.copyUriToFile(context, uri, destFile)
                loadPdf1(destFile, "Documento 1")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(toastMessage = "Error al abrir PDF 1: ${e.localizedMessage}") }
            }
        }
    }

    fun loadPdf2FromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val destFile = File(context.cacheDir, "pdf2_target_${System.currentTimeMillis()}.pdf")
                PdfHelper.copyUriToFile(context, uri, destFile)
                loadPdf2(destFile, "Documento 2")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(toastMessage = "Error al abrir PDF 2: ${e.localizedMessage}") }
            }
        }
    }

    private suspend fun loadPdf1(file: File, name: String) {
        val count = PdfHelper.getPageCount(file)
        // If it has multiple pages (e.g. Acta has 2 pages, signatures on page 2), start on page 1 (index 1) if available
        val initialPage = if (count > 1) 1 else 0
        val bitmap = PdfHelper.renderPage(file, initialPage, renderScale = 2.0f)
        _uiState.update {
            it.copy(
                pdf1 = PdfDocumentState(
                    file = file,
                    fileName = name,
                    pageCount = count,
                    currentPageIndex = initialPage,
                    currentPageBitmap = bitmap
                )
            )
        }
    }

    private suspend fun loadPdf2(file: File, name: String) {
        val count = PdfHelper.getPageCount(file)
        val bitmap = PdfHelper.renderPage(file, 0, renderScale = 2.0f)
        _uiState.update {
            it.copy(
                pdf2 = PdfDocumentState(
                    file = file,
                    fileName = name,
                    pageCount = count,
                    currentPageIndex = 0,
                    currentPageBitmap = bitmap
                )
            )
        }
    }

    fun setPdf1Page(pageIndex: Int) {
        viewModelScope.launch {
            val file = _uiState.value.pdf1.file ?: return@launch
            val bitmap = PdfHelper.renderPage(file, pageIndex, renderScale = 2.0f)
            _uiState.update {
                it.copy(
                    pdf1 = it.pdf1.copy(
                        currentPageIndex = pageIndex,
                        currentPageBitmap = bitmap
                    )
                )
            }
        }
    }

    fun setPdf2Page(pageIndex: Int) {
        viewModelScope.launch {
            val file = _uiState.value.pdf2.file ?: return@launch
            val bitmap = PdfHelper.renderPage(file, pageIndex, renderScale = 2.0f)
            _uiState.update {
                it.copy(
                    pdf2 = it.pdf2.copy(
                        currentPageIndex = pageIndex,
                        currentPageBitmap = bitmap
                    ),
                    placement = it.placement.copy(targetPageIndex = pageIndex)
                )
            }
        }
    }

    fun setSignatureType(type: SignatureSourceType) {
        _uiState.update { it.copy(signatureType = type) }
        when (type) {
            SignatureSourceType.OFFICIAL_PERU_STAMP -> regenerateStampBitmap()
            SignatureSourceType.CROPPED_FROM_PDF1 -> {
                val raw = _uiState.value.rawSignatureBitmap
                if (raw != null) {
                    applyProcessedCroppedBitmap(raw)
                }
            }
            SignatureSourceType.IMAGE_FROM_GALLERY -> {
                val raw = _uiState.value.rawSignatureBitmap
                if (raw != null) {
                    applyProcessedCroppedBitmap(raw)
                }
            }
        }
    }

    fun updateSignerApellidos(apellidos: String) {
        _uiState.update { it.copy(signerApellidos = apellidos) }
        if (_uiState.value.signatureType == SignatureSourceType.OFFICIAL_PERU_STAMP) {
            regenerateStampBitmap()
        }
    }

    fun updateSignerNombres(nombres: String) {
        _uiState.update { it.copy(signerNombres = nombres) }
        if (_uiState.value.signatureType == SignatureSourceType.OFFICIAL_PERU_STAMP) {
            regenerateStampBitmap()
        }
    }

    fun updateSignerId(id: String) {
        _uiState.update { it.copy(signerId = id) }
        if (_uiState.value.signatureType == SignatureSourceType.OFFICIAL_PERU_STAMP) {
            regenerateStampBitmap()
        }
    }

    fun updateSignerReason(reason: String) {
        _uiState.update { it.copy(signerReason = reason) }
        if (_uiState.value.signatureType == SignatureSourceType.OFFICIAL_PERU_STAMP) {
            regenerateStampBitmap()
        }
    }

    fun updateSignatureDate(date: String) {
        _uiState.update { it.copy(signatureDate = date) }
        if (_uiState.value.signatureType == SignatureSourceType.OFFICIAL_PERU_STAMP) {
            regenerateStampBitmap()
        } else if (_uiState.value.rawSignatureBitmap != null) {
            patchDateOnCurrentBitmap(date)
        }
    }

    fun setDateToNow() {
        val now = SignatureStampGenerator.getFormattedCurrentDate()
        updateSignatureDate(now)
    }

    fun toggleRemoveWhiteBackground(remove: Boolean) {
        _uiState.update { it.copy(removeWhiteBackground = remove) }
        if (_uiState.value.signatureType == SignatureSourceType.OFFICIAL_PERU_STAMP) {
            regenerateStampBitmap()
        } else {
            val raw = _uiState.value.rawSignatureBitmap
            if (raw != null) {
                applyProcessedCroppedBitmap(raw)
            }
        }
    }

    private fun regenerateStampBitmap() {
        val state = _uiState.value
        val context = getApplication<Application>()
        val bitmap = SignatureStampGenerator.generateAuthenticPeruStamp(
            context = context,
            apellidos = state.signerApellidos,
            nombres = state.signerNombres,
            firId = state.signerId,
            signerReason = state.signerReason,
            dateString = state.signatureDate,
            transparentBackground = state.removeWhiteBackground
        )
        _uiState.update { it.copy(activeSignatureBitmap = bitmap) }
    }

    fun updateCropRect(rect: RectF) {
        _uiState.update { it.copy(cropRect = rect) }
    }

    /**
     * Extracts signature from PDF 1 based on crop rectangle
     */
    fun cropSignatureFromPdf1() {
        val pageBitmap = _uiState.value.pdf1.currentPageBitmap ?: run {
            _uiState.update { it.copy(toastMessage = "Primero abra el PDF 1 para recortar.") }
            return
        }

        val crop = _uiState.value.cropRect
        val left = (crop.left * pageBitmap.width).toInt().coerceIn(0, pageBitmap.width - 2)
        val top = (crop.top * pageBitmap.height).toInt().coerceIn(0, pageBitmap.height - 2)
        val right = (crop.right * pageBitmap.width).toInt().coerceIn(left + 2, pageBitmap.width)
        val bottom = (crop.bottom * pageBitmap.height).toInt().coerceIn(top + 2, pageBitmap.height)

        val cropped = Bitmap.createBitmap(pageBitmap, left, top, right - left, bottom - top)
        _uiState.update {
            it.copy(
                rawSignatureBitmap = cropped,
                signatureType = SignatureSourceType.CROPPED_FROM_PDF1
            )
        }
        applyProcessedCroppedBitmap(cropped)
        _uiState.update { it.copy(toastMessage = "Firma recortada con éxito.") }
    }

    fun loadCustomImage(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                rawSignatureBitmap = bitmap,
                signatureType = SignatureSourceType.IMAGE_FROM_GALLERY
            )
        }
        applyProcessedCroppedBitmap(bitmap)
    }

    private fun applyProcessedCroppedBitmap(raw: Bitmap) {
        val isRemoveBg = _uiState.value.removeWhiteBackground
        val processed = if (isRemoveBg) {
            SignatureStampGenerator.removeWhiteBackground(raw)
        } else {
            raw
        }
        _uiState.update { it.copy(activeSignatureBitmap = processed) }
    }

    fun patchDateOnCurrentBitmap(newDate: String) {
        val raw = _uiState.value.rawSignatureBitmap ?: return
        val patched = SignatureStampGenerator.patchDateOnCroppedSignature(
            raw,
            newDate,
            eraseWhiteBackground = _uiState.value.removeWhiteBackground
        )
        _uiState.update {
            it.copy(
                activeSignatureBitmap = patched,
                signatureDate = newDate,
                toastMessage = "Fecha actualizada en el sello recortado."
            )
        }
    }

    // Placement adjustments on PDF 2
    fun setPlacementPosition(xNorm: Float, yNorm: Float) {
        _uiState.update {
            it.copy(
                placement = it.placement.copy(
                    xPercent = xNorm.coerceIn(0f, 1f - it.placement.widthPercent),
                    yPercent = yNorm.coerceIn(0f, 1f - it.placement.heightPercent)
                )
            )
        }
    }

    fun movePlacementBy(dxNorm: Float, dyNorm: Float) {
        val p = _uiState.value.placement
        val newX = (p.xPercent + dxNorm).coerceIn(0f, 1f - p.widthPercent)
        val newY = (p.yPercent + dyNorm).coerceIn(0f, 1f - p.heightPercent)
        _uiState.update { it.copy(placement = p.copy(xPercent = newX, yPercent = newY)) }
    }

    fun setPlacementSize(widthNorm: Float, heightNorm: Float) {
        val p = _uiState.value.placement
        val w = widthNorm.coerceIn(0.05f, 0.95f)
        val h = if (p.lockAspectRatio) {
            // Keep ratio
            val ratio = if (p.widthPercent > 0) p.heightPercent / p.widthPercent else 0.25f
            (w * ratio).coerceIn(0.02f, 0.95f)
        } else {
            heightNorm.coerceIn(0.02f, 0.95f)
        }
        _uiState.update {
            it.copy(
                placement = p.copy(
                    widthPercent = w,
                    heightPercent = h,
                    xPercent = p.xPercent.coerceAtMost(1f - w),
                    yPercent = p.yPercent.coerceAtMost(1f - h)
                )
            )
        }
    }

    fun toggleLockAspectRatio() {
        val p = _uiState.value.placement
        _uiState.update { it.copy(placement = p.copy(lockAspectRatio = !p.lockAspectRatio)) }
    }

    fun setPlacementRotation(degrees: Float) {
        _uiState.update { it.copy(placement = it.placement.copy(rotation = degrees)) }
    }

    fun setPlacementOpacity(alpha: Float) {
        _uiState.update { it.copy(placement = it.placement.copy(opacity = alpha.coerceIn(0.1f, 1f))) }
    }

    // Preset positions
    fun applyPresetTableRow2() {
        // Fits perfectly in Row 02 ("ANDRADE SALVADOR, Brayan") of CONSTANCIA_DE_ENTERADO
        _uiState.update {
            it.copy(
                placement = it.placement.copy(
                    xPercent = 0.585f,
                    yPercent = 0.446f,
                    widthPercent = 0.285f,
                    heightPercent = 0.058f,
                    rotation = 0f
                ),
                toastMessage = "Ajustado a la casilla Fila 02 (Constancia)"
            )
        }
    }

    fun applyPresetBottomInstructor() {
        // Fits in the bottom instructor signature area
        _uiState.update {
            it.copy(
                placement = it.placement.copy(
                    xPercent = 0.41f,
                    yPercent = 0.692f,
                    widthPercent = 0.31f,
                    heightPercent = 0.076f,
                    rotation = 0f
                ),
                toastMessage = "Ajustado a la zona de firma de Instructor"
            )
        }
    }

    fun applyPresetCenter() {
        _uiState.update {
            it.copy(
                placement = it.placement.copy(
                    xPercent = 0.35f,
                    yPercent = 0.46f,
                    widthPercent = 0.30f,
                    heightPercent = 0.08f,
                    rotation = 0f
                )
            )
        }
    }

    // Export PDF
    fun exportSignedPdf() {
        val basePdf = _uiState.value.pdf2.file
        val signatureBmp = _uiState.value.activeSignatureBitmap

        if (basePdf == null) {
            _uiState.update { it.copy(toastMessage = "Seleccione el PDF 2 (documento de destino).") }
            return
        }

        if (signatureBmp == null) {
            _uiState.update { it.copy(toastMessage = "Configure o recorte la firma antes de exportar.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportErrorMessage = null) }
            val context = getApplication<Application>()
            val outFile = File(
                context.cacheDir,
                "DOCUMENTO_FIRMADO_${System.currentTimeMillis()}.pdf"
            )
            val p = _uiState.value.placement

            val success = PdfHelper.exportSignedPdf(
                context = context,
                basePdfFile = basePdf,
                outputPdfFile = outFile,
                targetPageIndex = p.targetPageIndex,
                signatureBitmap = signatureBmp,
                normX = p.xPercent,
                normY = p.yPercent,
                normWidth = p.widthPercent,
                normHeight = p.heightPercent,
                rotationDegrees = p.rotation,
                opacity = p.opacity
            )

            if (success) {
                // Render preview of exported page
                val previewBmp = PdfHelper.renderPage(outFile, p.targetPageIndex, renderScale = 2.0f)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportedFile = outFile,
                        exportedPdfPageBitmap = previewBmp,
                        currentStep = AppStep.EXPORT_AND_SHARE,
                        toastMessage = "¡PDF firmado generado con éxito!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportErrorMessage = "Error al compilar y firmar el archivo PDF.",
                        toastMessage = "Error al exportar documento."
                    )
                }
            }
        }
    }

    /**
     * Downloads/saves the active signature bitmap to the device's public Downloads directory.
     */
    fun downloadSignatureImage() {
        val bitmap = _uiState.value.activeSignatureBitmap ?: run {
            _uiState.update { it.copy(toastMessage = "No hay firma para descargar.") }
            return
        }

        viewModelScope.launch {
            val context = getApplication<Application>()
            val name = "${_uiState.value.signerApellidos}_${_uiState.value.signerNombres}".replace("\\s+".toRegex(), "_")
            val fileName = "FIRMA_DIGITAL_${name}_${System.currentTimeMillis()}.png"
            val uri = PdfHelper.saveSignatureImageToDownloads(context, bitmap, fileName)
            if (uri != null) {
                _uiState.update { it.copy(toastMessage = "Firma descargada en Descargas: $fileName") }
            } else {
                _uiState.update { it.copy(toastMessage = "Error al descargar firma en el almacenamiento.") }
            }
        }
    }

    /**
     * Downloads/saves the generated signed PDF to the device's public Downloads directory.
     */
    fun downloadExportedPdf() {
        val file = _uiState.value.exportedFile ?: run {
            _uiState.update { it.copy(toastMessage = "No hay PDF generado para descargar.") }
            return
        }

        viewModelScope.launch {
            val context = getApplication<Application>()
            val targetName = "DOCUMENTO_FIRMADO_${System.currentTimeMillis()}.pdf"
            val uri = PdfHelper.savePdfToDownloads(context, file, targetName)
            if (uri != null) {
                _uiState.update { it.copy(toastMessage = "PDF guardado en Descargas: $targetName") }
            } else {
                _uiState.update { it.copy(toastMessage = "Error al guardar PDF en Descargas.") }
            }
        }
    }
}
