package com.example.sketch.viewmodel

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.sketch.model.DrawingState
import com.example.sketch.model.DrawingStroke
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class DrawingViewModel : ViewModel() {

    private val _drawingState = mutableStateOf(DrawingState())
    val drawingState: State<DrawingState> = _drawingState


    // Brush properties
    private val _brushColor = mutableStateOf(Color(0xFF000000))
    val brushColor: MutableState<Color> = _brushColor

    private val _brushWidth = mutableStateOf(5f)
    val brushWidth: State<Float> = _brushWidth

    // Undo/Redo stacks
    private val undoStack = mutableListOf<List<DrawingStroke>>()
    private val redoStack = mutableListOf<List<DrawingStroke>>()

    // Background image
    private val _backgroundImageUri = mutableStateOf<Uri?>(null)
    val backgroundImageUri: State<Uri?> = _backgroundImageUri

    // Canvas dimensions
    private val _canvasSize = mutableStateOf(Pair(0f, 0f))
    val canvasSize: State<Pair<Float, Float>> = _canvasSize
    fun setCanvasSize(width: Float, height: Float) {

        _canvasSize.value = Pair(width, height)
        _drawingState.value = _drawingState.value.copy(
            canvasWidth = width,
            canvasHeight = height
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setBrushColor(color: androidx.compose.ui.graphics.Color) {
        _brushColor.value = color
    }

    fun setBrushWidth(width: Float) {
        _brushWidth.value = width
    }

    fun setBackgroundImage(uri: Uri?) {
        _backgroundImageUri.value = uri
    }

    fun startStroke(point: Offset) {
        saveStateForUndo()
        val newStroke = DrawingStroke(
            path = Path().apply { moveTo(point.x, point.y) },
            color = _brushColor.value,
            strokeWidth = _brushWidth.value,
            points = mutableListOf(point)
        )
        _drawingState.value = _drawingState.value.copy(currentStroke = newStroke)
    }

    fun addPointToStroke(point: Offset) {
        val currentStroke = _drawingState.value.currentStroke ?: return
        currentStroke.path.lineTo(point.x, point.y)
        currentStroke.points.add(point)
        _drawingState.value = _drawingState.value.copy(currentStroke = currentStroke)
    }

    fun endStroke() {
        val currentStroke = _drawingState.value.currentStroke ?: return
        val newStrokes = _drawingState.value.strokes + currentStroke
        _drawingState.value = _drawingState.value.copy(
            strokes = newStrokes,
            currentStroke = null
        )
        // Clear redo stack when new action is performed
        redoStack.clear()
    }


    private fun saveStateForUndo() {
        undoStack.add(_drawingState.value.strokes.toList())
        // Limit undo stack size to prevent memory issues
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            // Save current state to redo stack
            redoStack.add(_drawingState.value.strokes.toList())

            // Restore previous state
            val previousState = undoStack.removeAt(undoStack.size - 1)
            _drawingState.value = _drawingState.value.copy(
                strokes = previousState,
                currentStroke = null
            )
        }
    }


    fun redo() {
        if (redoStack.isNotEmpty()) {
            // Save current state to undo stack
            undoStack.add(_drawingState.value.strokes.toList())

            // Restore next state
            val nextState = redoStack.removeAt(redoStack.size - 1)
            _drawingState.value = _drawingState.value.copy(
                strokes = nextState,
                currentStroke = null
            )
        }
    }

    fun clearCanvas() {
        saveStateForUndo()
        _drawingState.value = _drawingState.value.copy(
            strokes = emptyList(),
            currentStroke = null
        )
        _backgroundImageUri.value = null
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()




    fun exportDrawing(context: Context, backgroundBitmap: Bitmap? = null): Boolean {
        return try {
            val canvasWidth = _canvasSize.value.first.toInt()
            val canvasHeight = _canvasSize.value.second.toInt()

            if (canvasWidth <= 0 || canvasHeight <= 0) return false

            // Create bitmap for the final image
            val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Draw background if exists
            backgroundBitmap?.let { bg ->
                val scaledBg = Bitmap.createScaledBitmap(bg, canvasWidth, canvasHeight, true)
                canvas.drawBitmap(scaledBg, 0f, 0f, null)
            }

            // Draw all strokes
            _drawingState.value.strokes.forEach { stroke ->
                val paint = Paint().apply {
                    color = stroke.color.toArgb()
                    strokeWidth = stroke.strokeWidth
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }

                // Convert Path to Android Path and draw
                val androidPath = android.graphics.Path()
                stroke.points.forEachIndexed { index, point ->
                    if (index == 0) {
                        androidPath.moveTo(point.x, point.y)
                    } else {
                        androidPath.lineTo(point.x, point.y)
                    }
                }
                canvas.drawPath(androidPath, paint)
            }

            // Save to gallery
            saveImageToGallery(context, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    private fun saveImageToGallery(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val filename = "sketch_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Sketch")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let { imageUri ->
                context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
                true
            } ?: false
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun shareDrawing(context: Context): Uri? {
        return try {
            // Use fixed canvas size to avoid any issues
            val canvasWidth = 1080
            val canvasHeight = 1920

            // Create bitmap
            val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // White background
            canvas.drawColor(android.graphics.Color.WHITE)

            // Draw strokes
            _drawingState.value.strokes.forEach { stroke ->
                if (stroke.points.size >= 2) {
                    val paint = Paint().apply {
                        color = stroke.color.toArgb()
                        strokeWidth = stroke.strokeWidth
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        isAntiAlias = true
                    }

                    val path = android.graphics.Path()
                    stroke.points.forEachIndexed { index, point ->
                        if (index == 0) {
                            path.moveTo(point.x, point.y)
                        } else {
                            path.lineTo(point.x, point.y)
                        }
                    }
                    canvas.drawPath(path, paint)
                }
            }

            // Simple file creation
            val filename = "sketch_${System.currentTimeMillis()}.jpg"
            val file = File(context.cacheDir, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            // Return FileProvider URI
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}
