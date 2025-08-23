package com.example.sketch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sketch.viewmodel.DrawingViewModel

@Composable
fun ControlPanel(
    viewModel: DrawingViewModel,
    onImportImage: () -> Unit,
    onExportDrawing: () -> Unit,
    onShareDrawing: () -> Unit,
    onClearCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brushColor by viewModel.brushColor
    val brushWidth by viewModel.brushWidth
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Brush Controls Section
            Text(
                text = "Brush Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Color Picker
            ColorPicker(
                selectedColor = brushColor,
                onColorSelected = { viewModel.setBrushColor(it) }
            )
            
            // Brush Width Slider
            BrushWidthSlider(
                brushWidth = brushWidth,
                onWidthChanged = { viewModel.setBrushWidth(it) }
            )
            
            Divider()
            
            // Action Buttons
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            ActionButtons(
                viewModel = viewModel,
                onImportImage = onImportImage,
                onExportDrawing = onExportDrawing,
                onShareDrawing = onShareDrawing,
                onClearCanvas = onClearCanvas
            )
        }
    }
}

@Composable
private fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.Black,
        Color.Red,
        Color.Blue,
        Color.Green,
        Color.Yellow,
        Color.Magenta,
        Color.Cyan,
        Color.Gray,
        Color(0xFF8B4513), // Brown
        Color(0xFFFFA500), // Orange
        Color(0xFF800080), // Purple
        Color(0xFFFF69B4)  // Hot Pink
    )
    
    Column {
        Text(
            text = "Color",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(colors) { color ->
                ColorButton(
                    color = color,
                    isSelected = color == selectedColor,
                    onClick = { onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
private fun BrushWidthSlider(
    brushWidth: Float,
    onWidthChanged: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Brush Width",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${brushWidth.toInt()}px",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Slider(
            value = brushWidth,
            onValueChange = onWidthChanged,
            valueRange = 1f..50f,
            steps = 49,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActionButtons(
    viewModel: DrawingViewModel,
    onImportImage: () -> Unit,
    onExportDrawing: () -> Unit,
    onShareDrawing: () -> Unit,
    onClearCanvas: () -> Unit
) {
    // First row of buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton(
            icon = Icons.Default.Image,
            text = "Import",
            onClick = onImportImage,
            modifier = Modifier.weight(1f)
        )
        
        ActionButton(
            icon = Icons.Default.Save,
            text = "Export",
            onClick = onExportDrawing,
            modifier = Modifier.weight(1f)
        )
        
        ActionButton(
            icon = Icons.Default.Share,
            text = "Share",
            onClick = onShareDrawing,
            modifier = Modifier.weight(1f)
        )
    }
    
    // Second row of buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton(
            icon = Icons.Default.Undo,
            text = "Undo",
            onClick = { viewModel.undo() },
            enabled = viewModel.canUndo(),
            modifier = Modifier.weight(1f)
        )
        
        ActionButton(
            icon = Icons.Default.Redo,
            text = "Redo",
            onClick = { viewModel.redo() },
            enabled = viewModel.canRedo(),
            modifier = Modifier.weight(1f)
        )
        
        ActionButton(
            icon = Icons.Default.Clear,
            text = "Clear",
            onClick = onClearCanvas,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
