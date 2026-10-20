package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppStep
import com.example.ui.components.StepNavigationBar
import com.example.ui.screens.DocumentsStepScreen
import com.example.ui.screens.ExportStepScreen
import com.example.ui.screens.PlacementStepScreen
import com.example.ui.screens.SignatureStepScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PdfTransferViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PdfTransferViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }
                var showHelpDialog by remember { mutableStateOf(false) }

                // Handle back press
                BackHandler(enabled = uiState.currentStep != AppStep.DOCUMENTS) {
                    viewModel.prevStep()
                }

                // Show toasts/snackbars
                LaunchedEffect(uiState.toastMessage) {
                    uiState.toastMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearToast()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    Text(
                                        text = "FirmaTransfer",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Transferencia y Edición de Firmas en PDFs",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            navigationIcon = {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 12.dp).size(26.dp)
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { showHelpDialog = true },
                                    modifier = Modifier.testTag("help_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Ayuda y Caso de Uso",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Step Navigation Bar at the top of content
                        StepNavigationBar(
                            currentStep = uiState.currentStep,
                            onStepSelected = { step -> viewModel.setStep(step) }
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (uiState.currentStep) {
                                AppStep.DOCUMENTS -> {
                                    DocumentsStepScreen(
                                        uiState = uiState,
                                        onLoadDemoSamples = { viewModel.loadDemoSamples() },
                                        onPdf1UriSelected = { uri -> viewModel.loadPdf1FromUri(uri) },
                                        onPdf2UriSelected = { uri -> viewModel.loadPdf2FromUri(uri) },
                                        onPdf1PageChanged = { page -> viewModel.setPdf1Page(page) },
                                        onPdf2PageChanged = { page -> viewModel.setPdf2Page(page) },
                                        onContinue = { viewModel.nextStep() }
                                    )
                                }

                                AppStep.SIGNATURE_AND_DATE -> {
                                    SignatureStepScreen(
                                        uiState = uiState,
                                        onSignatureTypeChanged = { type -> viewModel.setSignatureType(type) },
                                        onSignerApellidosChanged = { apellidos -> viewModel.updateSignerApellidos(apellidos) },
                                        onSignerNombresChanged = { nombres -> viewModel.updateSignerNombres(nombres) },
                                        onSignerIdChanged = { id -> viewModel.updateSignerId(id) },
                                        onSignerReasonChanged = { reason -> viewModel.updateSignerReason(reason) },
                                        onSignatureDateChanged = { date -> viewModel.updateSignatureDate(date) },
                                        onSetDateToNow = { viewModel.setDateToNow() },
                                        onToggleRemoveWhiteBg = { remove -> viewModel.toggleRemoveWhiteBackground(remove) },
                                        onCropRectChanged = { rect -> viewModel.updateCropRect(rect) },
                                        onCropSignature = { viewModel.cropSignatureFromPdf1() },
                                        onCustomImageLoaded = { bmp -> viewModel.loadCustomImage(bmp) },
                                        onPatchDateOnCropped = { newDate -> viewModel.patchDateOnCurrentBitmap(newDate) },
                                        onDownloadSignature = { viewModel.downloadSignatureImage() },
                                        onPdf1PageChanged = { page -> viewModel.setPdf1Page(page) },
                                        onBack = { viewModel.prevStep() },
                                        onContinue = { viewModel.nextStep() }
                                    )
                                }

                                AppStep.PLACE_AND_ADJUST -> {
                                    PlacementStepScreen(
                                        uiState = uiState,
                                        onPositionChanged = { x, y -> viewModel.setPlacementPosition(x, y) },
                                        onMoveBy = { dx, dy -> viewModel.movePlacementBy(dx, dy) },
                                        onSizeChanged = { w, h -> viewModel.setPlacementSize(w, h) },
                                        onToggleLockAspect = { viewModel.toggleLockAspectRatio() },
                                        onRotationChanged = { r -> viewModel.setPlacementRotation(r) },
                                        onOpacityChanged = { o -> viewModel.setPlacementOpacity(o) },
                                        onApplyPresetRow2 = { viewModel.applyPresetTableRow2() },
                                        onApplyPresetInstructor = { viewModel.applyPresetBottomInstructor() },
                                        onApplyPresetCenter = { viewModel.applyPresetCenter() },
                                        onPdf2PageChanged = { page -> viewModel.setPdf2Page(page) },
                                        onExportPdf = { viewModel.exportSignedPdf() },
                                        onBack = { viewModel.prevStep() }
                                    )
                                }

                                AppStep.EXPORT_AND_SHARE -> {
                                    ExportStepScreen(
                                        uiState = uiState,
                                        onAdjustAgain = { viewModel.setStep(AppStep.PLACE_AND_ADJUST) },
                                        onRestart = { viewModel.setStep(AppStep.DOCUMENTS) },
                                        onDownloadPdf = { viewModel.downloadExportedPdf() }
                                    )
                                }
                            }
                        }
                    }
                }

                // Help dialog
                if (showHelpDialog) {
                    AlertDialog(
                        onDismissRequest = { showHelpDialog = false },
                        icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        title = { Text("Guía Rápida de Uso", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Pasos para firmar tu documento:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("1. Selecciona o carga los 2 documentos PDF (Origen y Destino).", fontSize = 13.sp)
                                Text("2. Configura los datos del autor y pulsa 'Usar Fecha y Hora Actual'.", fontSize = 13.sp)
                                Text("3. Arrastra el sello a la casilla de la tabla y ajusta con los controles o el atajo Fila 02.", fontSize = 13.sp)
                                Text("4. Pulsa 'Firmar y Guardar PDF' para compartirlo de inmediato.", fontSize = 13.sp)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showHelpDialog = false }) {
                                Text("Aceptar")
                            }
                        }
                    )
                }
            }
        }
    }
}
