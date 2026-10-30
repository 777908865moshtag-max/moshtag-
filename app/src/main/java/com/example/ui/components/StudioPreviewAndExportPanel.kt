package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.ProcessingMode
import com.example.viewmodel.StudioUiState
import kotlin.math.roundToInt

@Composable
fun StudioPreviewAndExportPanel(
    uiState: StudioUiState,
    onToggleCompare: (Boolean) -> Unit,
    onComparisonSliderChange: (Float) -> Unit,
    onDragDesignLayer: (dxFraction: Float, dyFraction: Float) -> Unit,
    onExportFormat: (String) -> Unit,
    onRestoreOriginal: () -> Unit,
    onRemoveBackgroundEdges: () -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showSideBySide by remember { mutableStateOf(false) }
    var touchMoveEnabled by remember { mutableStateOf(false) }

    val resultBmp = uiState.compositeResultBitmap
    val origBmp = uiState.originalAsset?.bitmap
    val rememberedResultImage = remember(resultBmp) { resultBmp?.asImageBitmap() }
    val rememberedOrigImage = remember(origBmp) { origBmp?.asImageBitmap() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Header: Preview Status & Identity Protection Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "حالة حماية الهوية",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "منصة المعاينة الحية والمقارنة",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val dimText = if (resultBmp != null) {
                            "الإخراج: ${uiState.selectedImageSize.maxDimensionPx}px • النسبة ${uiState.selectedAspectRatio.label} • الدقة ${uiState.selectedImageSize.label}"
                        } else {
                            "جاري تحضير المعاينة..."
                        }
                        Text(
                            text = dimText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (zoomLevel > 1.05f) {
                    IconButton(
                        onClick = {
                            zoomLevel = 1f
                            panOffset = Offset.Zero
                        },
                        modifier = Modifier.testTag("reset_zoom_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOutMap,
                            contentDescription = "إعادة ضبط التكبير",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Processing Progress Indicator when AI or Image Engine is active
            AnimatedVisibility(visible = uiState.isProcessingAi) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = uiState.processingStepMessageAr.ifBlank { "جارٍ المعالجة الذكية (قد تستغرق 30-60 ثانية)..." },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Fixed-height Interactive Canvas Stage (prevents LazyColumn height jumps and keeps scrolling smooth)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(275.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF283246),
                                Color(0xFF121722)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clipToBounds()
                    .testTag("interactive_preview_canvas"),
                contentAlignment = Alignment.Center
            ) {
                if (showSideBySide && origBmp != null && resultBmp != null && rememberedOrigImage != null && rememberedResultImage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = "الأصل المحمي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Image(
                                bitmap = rememberedOrigImage,
                                contentDescription = "الصورة الأصلية",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = "النتيجة النهائية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Image(
                                bitmap = rememberedResultImage,
                                contentDescription = "الصورة الناتجة",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                } else if (uiState.isComparingBeforeAfter && origBmp != null && resultBmp != null) {
                    SplitBeforeAfterCanvas(
                        originalBitmap = origBmp,
                        resultBitmap = resultBmp,
                        sliderFraction = uiState.comparisonSliderPosition,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (rememberedResultImage != null) {
                    val dragModifier = if (touchMoveEnabled) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (zoomLevel > 1.05f) {
                                    panOffset += dragAmount
                                } else {
                                    val dx = -dragAmount.x / size.width.coerceAtLeast(1)
                                    val dy = dragAmount.y / size.height.coerceAtLeast(1)
                                    onDragDesignLayer(dx, dy)
                                }
                            }
                        }
                    } else {
                        Modifier
                    }

                    Image(
                        bitmap = rememberedResultImage,
                        contentDescription = "معاينة النتيجة النهائية",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                            .graphicsLayer(
                                scaleX = zoomLevel,
                                scaleY = zoomLevel,
                                translationX = panOffset.x,
                                translationY = panOffset.y
                            )
                            .then(dragModifier)
                    )
                } else {
                    Text(
                        text = "جارٍ تجهيز المعاينة...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Top Overlay Badge showing active mode
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = if (uiState.processingMode == ProcessingMode.STRICT_PRESERVATION) {
                            "محافظة قصوى 100% على الأصل • ${uiState.selectedCategory.titleAr}"
                        } else {
                            "وضع التحسين بالذكاء الاصطناعي"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Comparison Slider when Split Before/After is enabled
            AnimatedVisibility(visible = uiState.isComparingBeforeAfter && !showSideBySide) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "بعد المعالجة (الموك أب)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "اسحب للمقارنة بين الأصل والنتيجة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "الأصل غير المعدل",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Slider(
                        value = uiState.comparisonSliderPosition,
                        onValueChange = onComparisonSliderChange,
                        valueRange = 0.05f..0.95f,
                        modifier = Modifier.testTag("comparison_slider")
                    )
                }
            }

            // View Mode & Layer Quick Actions Row (Fast horizontal scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.isComparingBeforeAfter && !showSideBySide,
                    onClick = {
                        showSideBySide = false
                        onToggleCompare(!uiState.isComparingBeforeAfter)
                    },
                    label = { Text("مقارنة قبل/بعد") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("toggle_split_compare_chip")
                )

                FilterChip(
                    selected = showSideBySide,
                    onClick = {
                        showSideBySide = !showSideBySide
                        if (showSideBySide) onToggleCompare(false)
                    },
                    label = { Text("جنبًا إلى جنب") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("toggle_side_by_side_chip")
                )

                FilterChip(
                    selected = touchMoveEnabled,
                    onClick = { touchMoveEnabled = !touchMoveEnabled },
                    label = { Text("تحريك التصميم باللمس") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.OpenWith,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("toggle_touch_move_chip")
                )

                AssistChip(
                    onClick = {
                        zoomLevel = if (zoomLevel < 1.8f) zoomLevel + 0.4f else 1f
                        if (zoomLevel == 1f) panOffset = Offset.Zero
                    },
                    label = { Text("تكبير (${(zoomLevel * 100).roundToInt()}%)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("zoom_preview_chip")
                )

                AssistChip(
                    onClick = onRemoveBackgroundEdges,
                    label = { Text("إزالة الخلفية") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("remove_background_chip")
                )

                AssistChip(
                    onClick = onRestoreOriginal,
                    label = { Text("استعادة الأصل") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        leadingIconContentColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.testTag("restore_original_chip")
                )
            }

            // Export Final Result Buttons (PNG & JPEG)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onExportFormat("PNG") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_png_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تنزيل PNG",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = { onExportFormat("JPEG") },
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .testTag("export_jpeg_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تنزيل JPEG",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitBeforeAfterCanvas(
    originalBitmap: Bitmap,
    resultBitmap: Bitmap,
    sliderFraction: Float,
    modifier: Modifier = Modifier
) {
    val origImg = remember(originalBitmap) { originalBitmap.asImageBitmap() }
    val resImg = remember(resultBitmap) { resultBitmap.asImageBitmap() }
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val canvasW = size.width.toInt().coerceAtLeast(1)
        val canvasH = size.height.toInt().coerceAtLeast(1)

        drawImage(
            image = resImg,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(canvasW, canvasH)
        )

        val splitX = size.width * sliderFraction
        clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
            drawImage(
                image = origImg,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(canvasW, canvasH)
            )
        }

        drawLine(
            color = primaryColor,
            start = Offset(splitX, 0f),
            end = Offset(splitX, size.height),
            strokeWidth = 5f
        )
        drawCircle(
            color = primaryColor,
            radius = 16f,
            center = Offset(splitX, size.height / 2f)
        )
    }
}
