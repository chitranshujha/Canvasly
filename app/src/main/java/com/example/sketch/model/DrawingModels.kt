package com.example.sketch.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Represents a single drawing stroke with its properties
 */
data class DrawingStroke(
    val path: Path = Path(),
    val color: Color = Color.Black,
    val strokeWidth: Float = 5f,
    val points: MutableList<Offset> = mutableListOf()
)

/**
 * Represents the complete drawing state
 */
data class DrawingState(
    val strokes: List<DrawingStroke> = emptyList(),
    val currentStroke: DrawingStroke? = null,
    val backgroundImagePath: String? = null,
    val canvasWidth: Float = 0f,
    val canvasHeight: Float = 0f
)

/**
 * Drawing actions for undo/redo functionality
 */
sealed class DrawingAction {
    object StartStroke : DrawingAction()
    data class AddPoint(val point: Offset) : DrawingAction()
    object EndStroke : DrawingAction()
    object ClearCanvas : DrawingAction()
    data class SetBackgroundImage(val imagePath: String?) : DrawingAction()
    data class UpdateBrushColor(val color: Color) : DrawingAction()
    data class UpdateBrushWidth(val width: Float) : DrawingAction()
}
