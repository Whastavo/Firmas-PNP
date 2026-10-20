package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun CropOverlayCanvas(
    pageBitmap: Bitmap,
    cropRect: RectF,
    onCropRectChanged: (RectF) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = modifier
            .background(Color.DarkGray)
            .pointerInput(pageBitmap) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (canvasSize.width <= 0 || canvasSize.height <= 0) return@detectDragGestures

                    val dxNorm = dragAmount.x / canvasSize.width
                    val dyNorm = dragAmount.y / canvasSize.height

                    val width = cropRect.width()
                    val height = cropRect.height()

                    val newLeft = (cropRect.left + dxNorm).coerceIn(0f, 1f - width)
                    val newTop = (cropRect.top + dyNorm).coerceIn(0f, 1f - height)

                    onCropRectChanged(RectF(newLeft, newTop, newLeft + width, newTop + height))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size
            val imageBitmap = pageBitmap.asImageBitmap()

            // Draw scaled page
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )

            // Dim area outside crop
            val cropLeftPx = cropRect.left * size.width
            val cropTopPx = cropRect.top * size.height
            val cropWidthPx = cropRect.width() * size.width
            val cropHeightPx = cropRect.height() * size.height

            val dimColor = Color.Black.copy(alpha = 0.45f)

            // Top overlay
            drawRect(
                color = dimColor,
                topLeft = Offset.Zero,
                size = Size(size.width, cropTopPx)
            )
            // Bottom overlay
            drawRect(
                color = dimColor,
                topLeft = Offset(0f, cropTopPx + cropHeightPx),
                size = Size(size.width, size.height - (cropTopPx + cropHeightPx))
            )
            // Left overlay
            drawRect(
                color = dimColor,
                topLeft = Offset(0f, cropTopPx),
                size = Size(cropLeftPx, cropHeightPx)
            )
            // Right overlay
            drawRect(
                color = dimColor,
                topLeft = Offset(cropLeftPx + cropWidthPx, cropTopPx),
                size = Size(size.width - (cropLeftPx + cropWidthPx), cropHeightPx)
            )

            // Crop box border (cyan / accent)
            drawRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(cropLeftPx, cropTopPx),
                size = Size(cropWidthPx, cropHeightPx),
                style = Stroke(width = 3.dp.toPx())
            )

            // Corner handles
            val handleLen = 14.dp.toPx()
            val handleStroke = 4.dp.toPx()
            val handleColor = Color.White

            // Top-left corner
            drawLine(handleColor, Offset(cropLeftPx, cropTopPx), Offset(cropLeftPx + handleLen, cropTopPx), handleStroke)
            drawLine(handleColor, Offset(cropLeftPx, cropTopPx), Offset(cropLeftPx, cropTopPx + handleLen), handleStroke)

            // Top-right corner
            drawLine(handleColor, Offset(cropLeftPx + cropWidthPx, cropTopPx), Offset(cropLeftPx + cropWidthPx - handleLen, cropTopPx), handleStroke)
            drawLine(handleColor, Offset(cropLeftPx + cropWidthPx, cropTopPx), Offset(cropLeftPx + cropWidthPx, cropTopPx + handleLen), handleStroke)

            // Bottom-left corner
            drawLine(handleColor, Offset(cropLeftPx, cropTopPx + cropHeightPx), Offset(cropLeftPx + handleLen, cropTopPx + cropHeightPx), handleStroke)
            drawLine(handleColor, Offset(cropLeftPx, cropTopPx + cropHeightPx), Offset(cropLeftPx, cropTopPx + cropHeightPx - handleLen), handleStroke)

            // Bottom-right corner
            drawLine(handleColor, Offset(cropLeftPx + cropWidthPx, cropTopPx + cropHeightPx), Offset(cropLeftPx + cropWidthPx - handleLen, cropTopPx + cropHeightPx), handleStroke)
            drawLine(handleColor, Offset(cropLeftPx + cropWidthPx, cropTopPx + cropHeightPx), Offset(cropLeftPx + cropWidthPx, cropTopPx + cropHeightPx - handleLen), handleStroke)
        }
    }
}
