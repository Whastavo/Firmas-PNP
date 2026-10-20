package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.PlacementConfig
import kotlin.math.roundToInt

@Composable
fun PdfPlacementCanvas(
    pageBitmap: Bitmap,
    signatureBitmap: Bitmap?,
    placement: PlacementConfig,
    onPositionChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .background(Color(0xFFE2E8F0))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        // Background PDF page is rendered once as hardware-accelerated image
        Image(
            bitmap = pageBitmap.asImageBitmap(),
            contentDescription = "Página del PDF destino",
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(containerWidthPx, containerHeightPx) {
                    detectTapGestures { tapOffset ->
                        // Tapping anywhere on the page moves the signature center there
                        if (containerWidthPx > 0 && containerHeightPx > 0) {
                            val targetX = (tapOffset.x / containerWidthPx - placement.widthPercent / 2f)
                                .coerceIn(0f, 1f - placement.widthPercent)
                            val targetY = (tapOffset.y / containerHeightPx - placement.heightPercent / 2f)
                                .coerceIn(0f, 1f - placement.heightPercent)
                            onPositionChanged(targetX, targetY)
                        }
                    }
                },
            contentScale = ContentScale.FillBounds
        )

        // Overlay signature: Hardware accelerated translation, zero lag, natural aspect ratio
        if (signatureBitmap != null && containerWidthPx > 0 && containerHeightPx > 0) {
            val bmpRatio = if (signatureBitmap.height > 0) {
                signatureBitmap.width.toFloat() / signatureBitmap.height.toFloat()
            } else {
                3.6f
            }
            val sigWidthPx = placement.widthPercent * containerWidthPx
            val sigHeightPx = sigWidthPx / bmpRatio

            val sigWidthDp = with(density) { sigWidthPx.toDp() }
            val sigHeightDp = with(density) { sigHeightPx.toDp() }

            val leftPx = (placement.xPercent * containerWidthPx).roundToInt()
            val topPx = (placement.yPercent * containerHeightPx).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(leftPx, topPx) }
                    .size(sigWidthDp, sigHeightDp)
                    .rotate(placement.rotation)
                    .alpha(placement.opacity)
                    .border(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.85f), RoundedCornerShape(2.dp))
                    .pointerInput(containerWidthPx, containerHeightPx, placement.widthPercent, placement.heightPercent) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dxNorm = dragAmount.x / containerWidthPx
                            val dyNorm = dragAmount.y / containerHeightPx
                            val newX = (placement.xPercent + dxNorm).coerceIn(0f, 1f - placement.widthPercent)
                            val newY = (placement.yPercent + dyNorm).coerceIn(0f, 1f - placement.heightPercent)
                            onPositionChanged(newX, newY)
                        }
                    }
            ) {
                Image(
                    bitmap = signatureBitmap.asImageBitmap(),
                    contentDescription = "Sello de firma digital",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }
        }
    }
}
