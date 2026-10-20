package com.example.ui.screens

import android.graphics.BitmapFactory
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SignatureSourceType
import com.example.ui.components.CropOverlayCanvas
import com.example.ui.viewmodel.UiState

@Composable
fun SignatureStepScreen(
    uiState: UiState,
    onSignatureTypeChanged: (SignatureSourceType) -> Unit,
    onSignerApellidosChanged: (String) -> Unit,
    onSignerNombresChanged: (String) -> Unit,
    onSignerIdChanged: (String) -> Unit,
    onSignerReasonChanged: (String) -> Unit,
    onSignatureDateChanged: (String) -> Unit,
    onSetDateToNow: () -> Unit,
    onToggleRemoveWhiteBg: (Boolean) -> Unit,
    onCropRectChanged: (RectF) -> Unit,
    onCropSignature: () -> Unit,
    onCustomImageLoaded: (android.graphics.Bitmap) -> Unit,
    onPatchDateOnCropped: (String) -> Unit,
    onDownloadSignature: () -> Unit,
    onPdf1PageChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember(uiState.signatureType) {
        mutableIntStateOf(
            when (uiState.signatureType) {
                SignatureSourceType.OFFICIAL_PERU_STAMP -> 0
                SignatureSourceType.CROPPED_FROM_PDF1 -> 1
                SignatureSourceType.IMAGE_FROM_GALLERY -> 2
            }
        )
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        onCustomImageLoaded(bmp)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab selector
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    onSignatureTypeChanged(SignatureSourceType.OFFICIAL_PERU_STAMP)
                },
                text = { Text("Sello Oficial", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    onSignatureTypeChanged(SignatureSourceType.CROPPED_FROM_PDF1)
                },
                text = { Text("Recortar PDF 1", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = {
                    selectedTab = 2
                    onSignatureTypeChanged(SignatureSourceType.IMAGE_FROM_GALLERY)
                },
                text = { Text("Subir Foto", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // Tab 0: Official Peru Stamp (Editable)
        if (selectedTab == 0) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Sello Digital Oficial Auténtico (Perú)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Genera el sello oficial exacto con el escudo de la República del Perú. Los apellidos van en MAYÚSCULAS y los nombres con la primera letra mayúscula y el resto en minúscula (formato oficial ReFirma/ONPE/RENIEC).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.signerApellidos,
                            onValueChange = onSignerApellidosChanged,
                            label = { Text("Apellidos (MAYÚSCULAS)") },
                            modifier = Modifier.weight(1f).testTag("signer_apellidos_field"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = uiState.signerNombres,
                            onValueChange = onSignerNombresChanged,
                            label = { Text("Nombres (1ra Mayúscula)") },
                            modifier = Modifier.weight(1f).testTag("signer_nombres_field"),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = uiState.signerId,
                        onValueChange = onSignerIdChanged,
                        label = { Text("Identificador FIR / DNI") },
                        modifier = Modifier.fillMaxWidth().testTag("signer_id_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.signerReason,
                        onValueChange = onSignerReasonChanged,
                        label = { Text("Motivo de la Firma") },
                        modifier = Modifier.fillMaxWidth().testTag("signer_reason_field"),
                        singleLine = true
                    )

                    // Date row with "Set Current Time" button
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = uiState.signatureDate,
                            onValueChange = onSignatureDateChanged,
                            label = { Text("Fecha y Hora de la Firma") },
                            modifier = Modifier.fillMaxWidth().testTag("signature_date_field"),
                            singleLine = true
                        )

                        FilledTonalButton(
                            onClick = onSetDateToNow,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("set_now_button")
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Usar Fecha y Hora Actual", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Tab 1: Crop from PDF 1
        if (selectedTab == 1) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Recortar Firma desde Documento 1",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (uiState.pdf1.currentPageBitmap != null) {
                        // Quick Presets for sample document
                        Text("Atajos de recorte para muestra:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    // Row 1: Brayan Andrade
                                    onPdf1PageChanged(1)
                                    onCropRectChanged(RectF(0.61f, 0.150f, 0.90f, 0.222f))
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Brayan (Fila 1)", fontSize = 11.sp)
                            }
                            FilledTonalButton(
                                onClick = {
                                    // Row 2: Joselito Peralta
                                    onPdf1PageChanged(1)
                                    onCropRectChanged(RectF(0.61f, 0.230f, 0.90f, 0.305f))
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Joselito (Fila 2)", fontSize = 11.sp)
                            }
                            FilledTonalButton(
                                onClick = {
                                    // Bottom: Walter Reyes
                                    onPdf1PageChanged(1)
                                    onCropRectChanged(RectF(0.50f, 0.495f, 0.93f, 0.620f))
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Instructor", fontSize = 11.sp)
                            }
                        }

                        // Page switcher if multi-page
                        if (uiState.pdf1.pageCount > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Página del PDF 1: ", fontSize = 12.sp)
                                for (p in 0 until uiState.pdf1.pageCount) {
                                    OutlinedButton(
                                        onClick = { onPdf1PageChanged(p) },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp).padding(end = 4.dp),
                                        colors = if (p == uiState.pdf1.currentPageIndex) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                                    ) {
                                        Text("${p + 1}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Text("Arrastra el recuadro cyan para seleccionar la firma:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Visual crop canvas
                        CropOverlayCanvas(
                            pageBitmap = uiState.pdf1.currentPageBitmap,
                            cropRect = uiState.cropRect,
                            onCropRectChanged = onCropRectChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Button(
                            onClick = onCropSignature,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("crop_signature_button")
                        ) {
                            Icon(Icons.Default.Crop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recortar Firma Seleccionada", fontWeight = FontWeight.Bold)
                        }

                        // Patch date tool on cropped signature
                        if (uiState.rawSignatureBitmap != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Modificar fecha en la firma recortada:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = uiState.signatureDate,
                                    onValueChange = onSignatureDateChanged,
                                    label = { Text("Nueva fecha") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = { onPatchDateOnCropped(uiState.signatureDate) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.CenterVertically).height(50.dp)
                                ) {
                                    Text("Reemplazar Fecha", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No se ha cargado el PDF 1. Vuelve al paso 1 para abrirlo o cargar la muestra.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Tab 2: Custom Image Upload
        if (selectedTab == 2) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Subir Imagen de Firma o Rúbrica",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Carga una foto o escaneo de firma desde la galería. La app quitará automáticamente el fondo blanco de papel para dejar solo el trazo limpio.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { galleryPicker.launch("image/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir Imagen de la Galería")
                    }
                }
            }
        }

        // Live preview of the active signature to be placed
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vista previa de la firma a estampar:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Fondo transparente", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = uiState.removeWhiteBackground,
                            onCheckedChange = onToggleRemoveWhiteBg,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.activeSignatureBitmap != null) {
                        Image(
                            bitmap = uiState.activeSignatureBitmap.asImageBitmap(),
                            contentDescription = "Firma activa",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Sin firma configurada", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = onDownloadSignature,
                    enabled = uiState.activeSignatureBitmap != null,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("download_signature_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Descargar Firma como Imagen (PNG)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        // Navigation buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Atrás", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onContinue,
                enabled = uiState.activeSignatureBitmap != null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.5f).height(50.dp).testTag("continue_to_placement_button")
            ) {
                Text("Ubicar en PDF 2", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}
