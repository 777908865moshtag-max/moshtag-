package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AiImageModelChoice
import com.example.model.IdentityProtectionConfig
import com.example.model.LayerTransformState
import com.example.model.LightingPreset
import com.example.model.MaskShapeType
import com.example.model.MockupCategory
import com.example.model.PerspectivePreset
import com.example.model.ProcessingMode
import com.example.model.StudioAspectRatio
import com.example.model.StudioImageSize
import com.example.viewmodel.StudioUiState
import kotlin.math.roundToInt

@Composable
fun MockupStudioWorkspaceContent(
    uiState: StudioUiState,
    onPickImageFromDevice: () -> Unit,
    onCaptureFromCamera: () -> Unit,
    onLoadSampleDesign: () -> Unit,
    onAnalyzeOriginalWithGemini: () -> Unit,
    onSelectProcessingMode: (ProcessingMode) -> Unit,
    onSelectCategory: (MockupCategory) -> Unit,
    onUpdateCustomCategoryPrompt: (String) -> Unit,
    onSelectAspectRatio: (StudioAspectRatio) -> Unit,
    onSelectImageSize: (StudioImageSize) -> Unit,
    onSelectImageModel: (AiImageModelChoice) -> Unit,
    onSelectLighting: (LightingPreset) -> Unit,
    onSelectPerspective: (PerspectivePreset) -> Unit,
    onUpdateLayerTransform: ((LayerTransformState) -> LayerTransformState) -> Unit,
    onUpdateIdentityProtection: ((IdentityProtectionConfig) -> IdentityProtectionConfig) -> Unit,
    onGenerateMockupWithAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvancedGeometrySliders by remember { mutableStateOf(false) }
    var showIdentityProtectionPanel by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Upload & Inspect Original Design Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("upload_design_section"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1. رفع صورة التصميم الأصلي وفحص الهوية",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Upload & Camera Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPickImageFromDevice,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("upload_from_device_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفع من الجهاز")
                    }

                    OutlinedButton(
                        onClick = onCaptureFromCamera,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("capture_camera_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الكاميرا")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAnalyzeOriginalWithGemini,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("analyze_image_gemini_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحليل الصورة بـ Gemini 4")
                    }

                    OutlinedButton(
                        onClick = onLoadSampleDesign,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("load_sample_design_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نموذج تجريبي")
                    }
                }

                // Original Asset Metadata Inspection Box
                uiState.originalAsset?.let { asset ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الملف الأصلي: ${asset.fileName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${asset.widthPx}×${asset.heightPx}px • ${asset.fileSizeKb}KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "تصنيف الهوية: ${asset.classification.titleAr}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = asset.analysisReportAr ?: asset.classification.adviceAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Processing Mode Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("processing_mode_section"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "2. نظام المحافظة على التصميم الأصلي واختيار الوضع",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                ProcessingMode.entries.forEach { mode ->
                    val isSelected = uiState.processingMode == mode
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProcessingMode(mode) }
                            .testTag("mode_card_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.titleAr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = mode.subtitleAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.processingMode == ProcessingMode.AI_ENHANCEMENT) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "تنبيه",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "تنبيه هام: التعديل المباشر ببكسلات التصميم عبر الذكاء الاصطناعي لا يضمن تطابق النصوص العربية والشعارات بنسبة 100%. لحماية النصوص بدقة مطلقة، استخدم «الوضع الأول: المحافظة القصوى على الأصل».",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 3. Mockup Studio Categories (All 11 Categories in fast horizontal scroll rows)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mockup_categories_section"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "3. استوديو إنشاء الموك أب (اسحب لاختيار الفئة)",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MockupCategory.entries.forEach { category ->
                        val selected = uiState.selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(category) },
                            label = { Text(category.titleAr) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("category_chip_${category.id}")
                        )
                    }
                }

                Text(
                    text = uiState.selectedCategory.descriptionAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = uiState.customCategoryPromptAr,
                    onValueChange = onUpdateCustomCategoryPrompt,
                    label = { Text("تخصيص إضافي للمشهد أو نوع النشاط (اختياري)") },
                    placeholder = { Text("مثال: منصة رخام بيج مع إضاءة دافئة لمحل عطور...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_mockup_prompt_input"),
                    singleLine = true
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // AI Image Model Choice
                Text(
                    text = "محرك توليد المشهد والدقة",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiImageModelChoice.entries.forEach { modelChoice ->
                        val isSel = uiState.selectedImageModel == modelChoice
                        FilterChip(
                            selected = isSel,
                            onClick = { onSelectImageModel(modelChoice) },
                            label = { Text(modelChoice.titleAr) },
                            modifier = Modifier.testTag("model_chip_${modelChoice.name}")
                        )
                    }
                }

                // Image Size Selector (1K, 2K, 4K)
                Text(
                    text = "حجم ودقة الصورة (1K / 2K / 4K)",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioImageSize.entries.forEach { size ->
                        FilterChip(
                            selected = uiState.selectedImageSize == size,
                            onClick = { onSelectImageSize(size) },
                            label = { Text("${size.label} (${size.maxDimensionPx}px)") },
                            modifier = Modifier.testTag("size_chip_${size.label}")
                        )
                    }
                }

                // Aspect Ratio Selector (8 Required Aspect Ratios)
                Text(
                    text = "نسبة أبعاد الصورة (Aspect Ratio)",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioAspectRatio.entries.forEach { ratio ->
                        FilterChip(
                            selected = uiState.selectedAspectRatio == ratio,
                            onClick = { onSelectAspectRatio(ratio) },
                            label = { Text(ratio.label) },
                            modifier = Modifier.testTag("aspect_ratio_chip_${ratio.label}")
                        )
                    }
                }

                // Lighting & Perspective Presets
                Text(
                    text = "الإضاءة المحيطة وزاوية المنظور",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LightingPreset.entries.forEach { light ->
                        FilterChip(
                            selected = uiState.selectedLighting == light,
                            onClick = { onSelectLighting(light) },
                            label = { Text(light.titleAr) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PerspectivePreset.entries.forEach { persp ->
                        FilterChip(
                            selected = uiState.selectedPerspective == persp,
                            onClick = { onSelectPerspective(persp) },
                            label = { Text(persp.titleAr) }
                        )
                    }
                }

                // Primary AI Scene Generation Button
                Button(
                    onClick = onGenerateMockupWithAi,
                    enabled = !uiState.isProcessingAi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("generate_mockup_ai_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.processingMode == ProcessingMode.STRICT_PRESERVATION) {
                            "توليد مشهد الموك أب ودمج التصميم الأصلي المحمي"
                        } else {
                            "توليد وتحسين الموك أب بالذكاء الاصطناعي"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Geometric Layer & Perspective Fine Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("geometric_layer_controls_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedGeometrySliders = !showAdvancedGeometrySliders },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "التحكم الهندسي بطبقة التصميم والمنظور والأقنعة",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = if (showAdvancedGeometrySliders) "إخفاء" else "فتح الأدوات",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = showAdvancedGeometrySliders) {
                    val tf = uiState.layerTransform
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "شكل القناع (Mask Shape)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MaskShapeType.entries.forEach { mask ->
                                FilterChip(
                                    selected = tf.maskShape == mask,
                                    onClick = {
                                        onUpdateLayerTransform { it.copy(maskShape = mask) }
                                    },
                                    label = { Text(mask.titleAr) }
                                )
                            }
                        }

                        LabeledSliderRow(
                            label = "حجم التصميم داخل المشهد (${(tf.scale * 100).roundToInt()}%)",
                            value = tf.scale,
                            valueRange = 0.15f..0.95f,
                            onValueChange = { v -> onUpdateLayerTransform { it.copy(scale = v) } }
                        )

                        LabeledSliderRow(
                            label = "الدوران الهندسي (${tf.rotationDegrees.roundToInt()}°)",
                            value = tf.rotationDegrees,
                            valueRange = -45f..45f,
                            onValueChange = { v -> onUpdateLayerTransform { it.copy(rotationDegrees = v) } }
                        )

                        LabeledSliderRow(
                            label = "ميلان المنظور الأفقي 3D (${(tf.perspectiveX * 100).roundToInt()}%)",
                            value = tf.perspectiveX,
                            valueRange = -0.35f..0.35f,
                            onValueChange = { v -> onUpdateLayerTransform { it.copy(perspectiveX = v) } }
                        )

                        LabeledSliderRow(
                            label = "ميلان المنظور العمودي 3D (${(tf.perspectiveY * 100).roundToInt()}%)",
                            value = tf.perspectiveY,
                            valueRange = -0.35f..0.35f,
                            onValueChange = { v -> onUpdateLayerTransform { it.copy(perspectiveY = v) } }
                        )

                        LabeledSliderRow(
                            label = "عمق الظل الواقعي (${(tf.shadowIntensity * 100).roundToInt()}%)",
                            value = tf.shadowIntensity,
                            valueRange = 0f..1f,
                            onValueChange = { v -> onUpdateLayerTransform { it.copy(shadowIntensity = v) } }
                        )

                        LabeledSliderRow(
                            label = "تفاعل الإضاءة السطحية (${(tf.surfaceLightingBlend * 100).roundToInt()}%)",
                            value = tf.surfaceLightingBlend,
                            valueRange = 0f..0.6f,
                            onValueChange = { v -> onUpdateLayerTransform { it.copy(surfaceLightingBlend = v) } }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تأثير البروز ثلاثي الأبعاد للشعارات (3D Emboss)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = tf.show3DEmbossEffect,
                                onCheckedChange = { checked ->
                                    onUpdateLayerTransform { it.copy(show3DEmbossEffect = checked) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 5. Identity Protection Tools (8 Independent Switches - Section 7)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("identity_protection_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showIdentityProtectionPanel = !showIdentityProtectionPanel },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "7. أدوات حماية الهوية الأصلية (8 ضوابط مفعّلة)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = if (showIdentityProtectionPanel) "إخفاء" else "تعديل الضوابط",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = showIdentityProtectionPanel) {
                    val p = uiState.identityProtection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProtectionSwitchRow(
                            title = "حماية النصوص العربية والإنجليزية",
                            checked = p.protectTexts,
                            onCheckedChange = { v -> onUpdateIdentityProtection { it.copy(protectTexts = v) } }
                        )
                        ProtectionSwitchRow(
                            title = "حماية الشعار والعلامة التجارية",
                            checked = p.protectLogo,
                            onCheckedChange = { v -> onUpdateIdentityProtection { it.copy(protectLogo = v) } }
                        )
                        ProtectionSwitchRow(
                            title = "حماية الألوان الأصلية والتدرجات",
                            checked = p.protectColors,
                            onCheckedChange = { v -> onUpdateIdentityProtection { it.copy(protectColors = v) } }
                        )
                        ProtectionSwitchRow(
                            title = "حماية أبعاد ونسب المنتج",
                            checked = p.protectDimensions,
                            onCheckedChange = { v -> onUpdateIdentityProtection { it.copy(protectDimensions = v) } }
                        )
                        ProtectionSwitchRow(
                            title = "حماية الوجوه وملامح الأشخاص",
                            checked = p.protectFaces,
                            onCheckedChange = { v -> onUpdateIdentityProtection { it.copy(protectFaces = v) } }
                        )
                        ProtectionSwitchRow(
                            title = "حماية تفاصيل العبوة والملصق",
                            checked = p.protectPackagingDetails,
                            onCheckedChange = { v -> onUpdateIdentityProtection { it.copy(protectPackagingDetails = v) } }
                        )
                        ProtectionSwitchRow(
                            title = "منع إضافة أي نص أو أرقام جديدة",
                            checked = p.forbidNewText,
                            onCheckedChange = { v -> onUpdateIdentityProtection { it.copy(forbidNewText = v) } }
                        )
                        ProtectionSwitchRow(
                            title = "منع إضافة شعارات أو علامات مائية",
                            checked = p.forbidWatermarks,
                            onCheckedChange = { v -> onUpdateIdentityProtection { it.copy(forbidWatermarks = v) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LabeledSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun ProtectionSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
