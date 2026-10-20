package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PdfPlacementCanvas
import com.example.ui.viewmodel.UiState

@Composable
fun PlacementStepScreen(
    uiState: UiState,
    onPositionChanged: (Float, Float) -> Unit,
    onMoveBy: (Float, Float) -> Unit,
    onSizeChanged: (Float, Float) -> Unit,
    onToggleLockAspect: () -> Unit,
    onRotationChanged: (Float) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onApplyPresetRow2: () -> Unit,
    onApplyPresetInstructor: () -> Unit,
    onApplyPresetCenter: () -> Unit,
    onPdf2PageChanged: (Int) -> Unit,
    onExportPdf: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFineTuneControls by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick instruction & preset bar
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ajustar Firma en Documento 2",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Arrastra con el dedo",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = onApplyPresetRow2,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f).testTag("preset_row2_button")
                    ) {
                        Text("⭐ Fila 02", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = onApplyPresetInstructor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Instructor", fontSize = 12.sp, maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = onApplyPresetCenter,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text("Centrar", fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }

        // The interactive document canvas: EXACT match with PDF page aspect ratio
        val pageBitmap = uiState.pdf2.currentPageBitmap
        if (pageBitmap != null) {
            val pageRatio = if (pageBitmap.height > 0) pageBitmap.width.toFloat() / pageBitmap.height.toFloat() else 0.707f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(pageRatio)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag("placement_canvas_box")
            ) {
                PdfPlacementCanvas(
                    pageBitmap = pageBitmap,
                    signatureBitmap = uiState.activeSignatureBitmap,
                    placement = uiState.placement,
                    onPositionChanged = onPositionChanged,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay página cargada para el Documento 2.", color = Color.DarkGray)
            }
        }

        // Page selector for PDF 2 if multi-page
        if (uiState.pdf2.pageCount > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Página de destino: ", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                for (p in 0 until uiState.pdf2.pageCount) {
                    OutlinedButton(
                        onClick = { onPdf2PageChanged(p) },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp).padding(horizontal = 4.dp),
                        colors = if (p == uiState.pdf2.currentPageIndex) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("${p + 1}", fontSize = 12.sp)
                    }
                }
            }
        }

        // Precision Controls & D-Pad
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Calibración Milimétrica y D-Pad", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    IconButton(onClick = { showFineTuneControls = !showFineTuneControls }) {
                        Icon(
                            imageVector = if (showFineTuneControls) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Alternar controles"
                        )
                    }
                }

                if (showFineTuneControls) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Directional D-Pad
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Mover (1 pt)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            // Up
                            FilledTonalIconButton(
                                onClick = { onMoveBy(0f, -0.005f) },
                                modifier = Modifier.size(36.dp).testTag("dpad_up_button")
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Arriba")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Left
                                FilledTonalIconButton(
                                    onClick = { onMoveBy(-0.005f, 0f) },
                                    modifier = Modifier.size(36.dp).testTag("dpad_left_button")
                                ) {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Izquierda")
                                }
                                Spacer(modifier = Modifier.size(36.dp))
                                // Right
                                FilledTonalIconButton(
                                    onClick = { onMoveBy(0.005f, 0f) },
                                    modifier = Modifier.size(36.dp).testTag("dpad_right_button")
                                ) {
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Derecha")
                                }
                            }
                            // Down
                            FilledTonalIconButton(
                                onClick = { onMoveBy(0f, 0.005f) },
                                modifier = Modifier.size(36.dp).testTag("dpad_down_button")
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Abajo")
                            }
                        }

                        // Sizing quick buttons
                        Column(
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Tamaño del sello:", fontSize = 12.sp, fontWeight = FontWeight.Medium)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val p = uiState.placement
                                        onSizeChanged(p.widthPercent * 1.05f, p.heightPercent * 1.05f)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+5%", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        val p = uiState.placement
                                        onSizeChanged(p.widthPercent * 0.95f, p.heightPercent * 0.95f)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.ZoomOut, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("-5%", fontSize = 11.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bloquear proporción:", fontSize = 11.sp)
                                Switch(
                                    checked = uiState.placement.lockAspectRatio,
                                    onCheckedChange = { onToggleLockAspect() },
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }

                    // Sizing sliders
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ancho:", fontSize = 12.sp)
                            Text("${(uiState.placement.widthPercent * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = uiState.placement.widthPercent,
                            onValueChange = { newW ->
                                onSizeChanged(newW, uiState.placement.heightPercent)
                            },
                            valueRange = 0.10f..0.80f
                        )
                    }

                    // Opacity slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Opacidad del sello:", fontSize = 12.sp)
                            Text("${(uiState.placement.opacity * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = uiState.placement.opacity,
                            onValueChange = onOpacityChanged,
                            valueRange = 0.30f..1.0f
                        )
                    }
                }
            }
        }

        // Export button
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Atrás", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onExportPdf,
                enabled = !uiState.isExporting && uiState.pdf2.file != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1.8f).height(52.dp).testTag("export_signed_pdf_button")
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Estampando PDF...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Firmar y Guardar PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
