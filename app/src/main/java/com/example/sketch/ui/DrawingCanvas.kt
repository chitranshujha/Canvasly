package com.example.sketch.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.sketch.model.DrawingStroke
import com.example.sketch.viewmodel.DrawingViewModel
import kotlinx.coroutines.launch

@Composable
fun DrawingCanvas(
    viewModel: DrawingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    
    val drawingState by viewModel.drawingState
    val backgroundImageUri by viewModel.backgroundImageUri
    
    var backgroundBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Load background image when URI changes
    LaunchedEffect(backgroundImageUri) {
        backgroundImageUri?.let { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                backgroundBitmap = bitmap?.asImageBitmap()
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
                backgroundBitmap = null
            }
        } ?: run {
            backgroundBitmap = null
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.White)
            .onSizeChanged { size ->
                viewModel.setCanvasSize(size.width.toFloat(), size.height.toFloat())
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        viewModel.startStroke(offset)
                    },
                    onDragEnd = {
                        viewModel.endStroke()
                    },
                    onDragCancel = {
                        viewModel.endStroke()
                    },
                    onDrag = { change, _ ->
                        viewModel.addPointToStroke(change.position)
                    }
                )

            }
    ) {
        // Draw background image if available
        backgroundBitmap?.let { bitmap ->
            drawImage(
                image = bitmap,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.High
            )
        }
        
        // Draw all completed strokes
        drawingState.strokes.forEach { stroke ->
            drawStroke(stroke)
        }
        
        // Draw current stroke being drawn
        drawingState.currentStroke?.let { stroke ->
            drawStroke(stroke)
        }
    }
}

private fun DrawScope.drawStroke(stroke: DrawingStroke) {
    if (stroke.points.size > 1) {
        val path = Path()
        stroke.points.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }
        
        drawPath(
            path = path,
            color = stroke.color,
            style = Stroke(
                width = stroke.strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    } else if (stroke.points.size == 1) {
        // Draw a single point as a circle
        val point = stroke.points.first()
        drawCircle(
            color = stroke.color,
            radius = stroke.strokeWidth / 2,
            center = point
        )
    }
}


