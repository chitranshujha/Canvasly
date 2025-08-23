package com.example.sketch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sketch.ui.ControlPanel
import com.example.sketch.ui.DrawingCanvas
import com.example.sketch.ui.theme.SketchTheme
import com.example.sketch.viewmodel.DrawingViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val drawingViewModel: DrawingViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SketchTheme {
                SketchApp(drawingViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SketchApp(viewModel: DrawingViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var showControlPanel by remember { mutableStateOf(false) }
    
    // Permission launcher for storage access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "Storage permission is required for image import/export", Toast.LENGTH_LONG).show()
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setBackgroundImage(it)
            Toast.makeText(context, "Image imported successfully", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Check and request permissions on first launch
    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Sketch",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showControlPanel = !showControlPanel }
                    ) {
                        Text(if (showControlPanel) "Hide Controls" else "Show Controls")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main drawing canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                DrawingCanvas(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Control panel (collapsible)
            if (showControlPanel) {
                ControlPanel(
                    viewModel = viewModel,
                    onImportImage = {
                        imagePickerLauncher.launch("image/*")
                    },
                    onExportDrawing = {
                        coroutineScope.launch {
                            val success = viewModel.exportDrawing(context)
                            val message = if (success) {
                                "Drawing exported to gallery successfully!"
                            } else {
                                "Failed to export drawing. Please try again."
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    onShareDrawing = {
                        try {
                            Toast.makeText(context, "Preparing image for sharing...", Toast.LENGTH_SHORT).show()
                            val imageUri = viewModel.shareDrawing(context)
                            if (imageUri != null) {
                                shareImage(context, imageUri)
                                Toast.makeText(context, "Opening share dialog...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to create image. Please draw something and try again.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Sharing error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    onClearCanvas = {
                        showClearDialog = true
                    }
                )
            }
        }
    }
    
    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Canvas") },
            text = { Text("Are you sure you want to clear the canvas? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCanvas()
                        showClearDialog = false
                        Toast.makeText(context, "Canvas cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun shareImage(context: android.content.Context, imageUri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share your sketch"))
}