package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PdfDocumentState
import com.example.ui.viewmodel.UiState

@Composable
fun DocumentsStepScreen(
    uiState: UiState,
    onLoadDemoSamples: () -> Unit,
    onPdf1UriSelected: (Uri) -> Unit,
    onPdf2UriSelected: (Uri) -> Unit,
    onPdf1PageChanged: (Int) -> Unit,
    onPdf2PageChanged: (Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pdf1Picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onPdf1UriSelected(it) }
    }
    val pdf2Picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onPdf2UriSelected(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Demo sample quick load button
        Button(
            onClick = onLoadDemoSamples,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("load_demo_button")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cargar Ejemplos Adjuntos (Acta y Constancia)",
                fontWeight = FontWeight.SemiBold
            )
        }

        // PDF 1 Card (Source Document)
        PdfDocumentCard(
            title = "1. PDF con Firma Original (Origen)",
            subtitle = "De aquí extraeremos la firma o sello",
            pdfState = uiState.pdf1,
            onSelectPdf = { pdf1Picker.launch(arrayOf("application/pdf")) },
            onPageChange = onPdf1PageChanged,
            testTag = "pdf1_card"
        )

        // PDF 2 Card (Target Document)
        PdfDocumentCard(
            title = "2. PDF a Firmar (Destino)",
            subtitle = "Donde colocaremos la firma con fecha actualizada",
            pdfState = uiState.pdf2,
            onSelectPdf = { pdf2Picker.launch(arrayOf("application/pdf")) },
            onPageChange = onPdf2PageChanged,
            testTag = "pdf2_card"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Continue Button
        val canContinue = uiState.pdf2.file != null || uiState.pdf1.file != null
        Button(
            onClick = onContinue,
            enabled = canContinue,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("continue_to_signature_button")
        ) {
            Text(
                text = "Paso Siguiente: Editar Firma y Fecha",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun PdfDocumentCard(
    title: String,
    subtitle: String,
    pdfState: PdfDocumentState,
    onSelectPdf: () -> Unit,
    onPageChange: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth().testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (pdfState.file != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (pdfState.file != null) Icons.Default.CheckCircle else Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = if (pdfState.file != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (pdfState.file != null) {
                // PDF is loaded
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page preview thumbnail
                    if (pdfState.currentPageBitmap != null) {
                        Image(
                            bitmap = pdfState.currentPageBitmap.asImageBitmap(),
                            contentDescription = "Vista previa de página",
                            modifier = Modifier
                                .width(60.dp)
                                .height(85.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pdfState.fileName,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Páginas: ${pdfState.pageCount} (Viendo pág ${pdfState.currentPageIndex + 1})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // If multiple pages, show page switcher
                        if (pdfState.pageCount > 1) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pág:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                for (p in 0 until pdfState.pageCount) {
                                    FilledTonalButton(
                                        onClick = { onPageChange(p) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).padding(end = 4.dp),
                                        colors = if (p == pdfState.currentPageIndex) {
                                            ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            ButtonDefaults.filledTonalButtonColors()
                                        }
                                    ) {
                                        Text("${p + 1}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onSelectPdf,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Cambiar", fontSize = 12.sp)
                    }
                }
            } else {
                // Empty state: Choose PDF button
                OutlinedButton(
                    onClick = onSelectPdf,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seleccionar archivo PDF")
                }
            }
        }
    }
}
