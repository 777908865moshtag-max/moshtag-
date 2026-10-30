package com.example.ui.components

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.StudioProjectEntity
import com.example.model.AdBackgroundConfig
import com.example.model.LayerTransformState
import com.example.model.ProductEnhancementConfig
import com.example.model.StudioAspectRatio
import com.example.model.StudioImageSize
import com.example.viewmodel.StudioUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val PRODUCT_BACKDROP_PRESETS = listOf(
    "رخام أسود فاخر مع إضاءة ذهبية" to "luxury black marble podium with warm golden bokeh lighting in background",
    "منصة خشب جوز طبيعي دافئ" to "warm walnut wood pedestal with soft natural morning studio light",
    "خلفية استوديو بيضاء نقية للمتاجر" to "clean pure white e-commerce studio sweep with soft natural contact shadow",
    "حجر بازلت داكن مع نباتات عطرية" to "dark volcanic basalt stone podium with subtle botanical leaves in soft focus",
    "منصة مخملية كحلية وذهبية ملكية" to "royal deep navy velvet and brushed gold studio stage with dramatic rim lighting"
)

private val VIEWING_ANGLE_PRESETS = listOf(
    "زاوية أمامية احترافية (Eye-Level)",
    "زاوية علوية مائلة 45° (Elevated Angle)",
    "زاوية منخفضة فخمة (Hero Low-Angle)"
)

private val BUSINESS_SECTORS = listOf(
    "العطور ومستحضرات التجميل الفاخرة",
    "العطارة والتوابل والقهوة المختصة",
    "المطاعم والكافيهات والحلويات",
    "الأزياء والمجوهرات والإكسسوارات",
    "الإلكترونيات والتقنية الحديثة",
    "الخدمات التجارية والمطابع والدعاية"
)

private val COLOR_STYLES = listOf(
    "ذهبي ملكي وأسود فحمي",
    "أبيض ناصع ورمادي فضي عصري",
    "بيج رملي دافئ وبني طبيعي",
    "زمردي داكن مع نحاسي فاخر",
    "أزرق ياقوتي مع إضاءة سينمائية"
)

private val DISPLAY_STYLES = listOf(
    "منصة عرض استوديو ثلاثية الأبعاد (3D Podium)",
    "سطح رخامي مسطح (Flat Lay Minimal)",
    "إضاءة خلفية درامية مع عمق ميدان (Bokeh Studio)",
    "تكوين هندسي معماري فاخر (Architectural Stage)"
)

@Composable
fun ProductEnhancementTabContent(
    uiState: StudioUiState,
    onUpdateConfig: ((ProductEnhancementConfig) -> ProductEnhancementConfig) -> Unit,
    onUpdateLayerTransform: ((LayerTransformState) -> LayerTransformState) -> Unit,
    onSelectAspectRatio: (StudioAspectRatio) -> Unit,
    onSelectImageSize: (StudioImageSize) -> Unit,
    onEnhanceProductWithAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = uiState.productEnhanceConfig
    val tf = uiState.layerTransform

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_enhancement_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "5. تحسين عرض المنتجات والتصوير التجاري",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = "اختر خلفية المشهد التجاري، واضبط الإضاءة المحيطة والظلال والانعكاسات وموضع المنتج دون تشويه أبعاده أو تغيير الملصق الموجود عليه.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "اختيار خلفية المشهد التجاري",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PRODUCT_BACKDROP_PRESETS.forEach { (titleAr, promptEn) ->
                    FilterChip(
                        selected = cfg.backgroundStyleAr == titleAr,
                        onClick = {
                            onUpdateConfig {
                                it.copy(
                                    backgroundStyleAr = titleAr,
                                    backgroundStylePromptEn = promptEn
                                )
                            }
                        },
                        label = { Text(titleAr) }
                    )
                }
            }

            Text(
                text = "زاوية العرض والتكوين",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VIEWING_ANGLE_PRESETS.forEach { angleAr ->
                    FilterChip(
                        selected = cfg.viewingAngleAr == angleAr,
                        onClick = { onUpdateConfig { it.copy(viewingAngleAr = angleAr) } },
                        label = { Text(angleAr) }
                    )
                }
            }

            LabeledSliderRow(
                label = "تحسين الإضاءة المحيطة (${(cfg.ambientLightingLevel * 100).roundToInt()}%)",
                value = cfg.ambientLightingLevel,
                valueRange = 0.1f..1f,
                onValueChange = { v -> onUpdateConfig { it.copy(ambientLightingLevel = v) } }
            )

            LabeledSliderRow(
                label = "إضافة ظلال واقعية (${(tf.shadowIntensity * 100).roundToInt()}%)",
                value = tf.shadowIntensity,
                valueRange = 0f..1f,
                onValueChange = { v ->
                    onUpdateLayerTransform { it.copy(shadowIntensity = v) }
                    onUpdateConfig { it.copy(realisticShadowDepth = v) }
                }
            )

            LabeledSliderRow(
                label = "تحسين انعكاسات الأسطح (${(cfg.surfaceReflectionIntensity * 100).roundToInt()}%)",
                value = cfg.surfaceReflectionIntensity,
                valueRange = 0f..0.85f,
                onValueChange = { v -> onUpdateConfig { it.copy(surfaceReflectionIntensity = v) } }
            )

            LabeledSliderRow(
                label = "حجم المنتج داخل الكادر دون تشويه (${(tf.scale * 100).roundToInt()}%)",
                value = tf.scale,
                valueRange = 0.2f..0.9f,
                onValueChange = { v -> onUpdateLayerTransform { it.copy(scale = v) } }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "وضع الملصق/المنتج الأصلي فوق المشهد المركب",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "يضمن المحافظة الدقيقة 100% على ملصق المنتج ونصوصه بدلًا من إعادة رسمه بالذكاء الاصطناعي.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = cfg.preserveOriginalLabelLayer,
                    onCheckedChange = { checked ->
                        onUpdateConfig { it.copy(preserveOriginalLabelLayer = checked) }
                    },
                    modifier = Modifier.testTag("preserve_label_layer_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إنشاء مشهد تجاري فاخر (Luxury Studio Scene)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = cfg.luxurySceneEnabled,
                    onCheckedChange = { checked ->
                        onUpdateConfig { it.copy(luxurySceneEnabled = checked) }
                    }
                )
            }

            Text(
                text = "قص وتغيير أبعاد مساحة العرض دون تشويه المنتج",
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
                        label = { Text(ratio.label) }
                    )
                }
            }

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
                        label = { Text(size.label) }
                    )
                }
            }

            OutlinedTextField(
                value = cfg.customChangesPromptAr,
                onValueChange = { text -> onUpdateConfig { it.copy(customChangesPromptAr = text) } },
                label = { Text("تعديلات إضافية مطلوبة في المشهد المحيط (اختياري)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_custom_changes_input"),
                singleLine = true
            )

            Button(
                onClick = onEnhanceProductWithAi,
                enabled = !uiState.isProcessingAi,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("enhance_product_ai_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "توليد المشهد التجاري وتحسين عرض المنتج",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PureAdBackgroundsTabContent(
    uiState: StudioUiState,
    onUpdateBgConfig: ((AdBackgroundConfig) -> AdBackgroundConfig) -> Unit,
    onSelectAspectRatio: (StudioAspectRatio) -> Unit,
    onSelectImageSize: (StudioImageSize) -> Unit,
    onGeneratePureBackground: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = uiState.adBackgroundConfig

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ad_backgrounds_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wallpaper,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "6. توليد الخلفيات الإعلانية الصافية (بدون نصوص)",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = "يولّد خلفيات إعلانية احترافية خالية تمامًا من النصوص والشعارات والعلامات المائية والأرقام، مع ترك مساحة مريحة لإضافة النصوص يدويًا في برامج التصميم.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "نوع النشاط التجاري",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BUSINESS_SECTORS.forEach { sector ->
                    FilterChip(
                        selected = bg.businessTypeAr == sector,
                        onClick = { onUpdateBgConfig { it.copy(businessTypeAr = sector) } },
                        label = { Text(sector) }
                    )
                }
            }

            OutlinedTextField(
                value = bg.productTypeAr,
                onValueChange = { v -> onUpdateBgConfig { it.copy(productTypeAr = v) } },
                label = { Text("نوع المنتج المراد عرضه") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bg_product_type_input"),
                singleLine = true
            )

            Text(
                text = "اللون أو النمط البصري",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COLOR_STYLES.forEach { style ->
                    FilterChip(
                        selected = bg.colorVisualStyleAr == style,
                        onClick = { onUpdateBgConfig { it.copy(colorVisualStyleAr = style) } },
                        label = { Text(style) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (bg.isDarkPosture) "النمط الضوئي: خلفية داكنة فخمة" else "النمط الضوئي: خلفية فاتحة مشرقة",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = bg.isDarkPosture,
                    onCheckedChange = { dark -> onUpdateBgConfig { it.copy(isDarkPosture = dark) } },
                    modifier = Modifier.testTag("bg_dark_light_switch")
                )
            }

            Text(
                text = "أسلوب العرض والتكوين",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DISPLAY_STYLES.forEach { dStyle ->
                    FilterChip(
                        selected = bg.displayStyleAr == dStyle,
                        onClick = { onUpdateBgConfig { it.copy(displayStyleAr = dStyle) } },
                        label = { Text(dStyle) }
                    )
                }
            }

            Text(
                text = "نسبة أبعاد الخلفية ودقة الإخراج",
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
                        label = { Text(ratio.label) }
                    )
                }
            }

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
                        label = { Text(size.label) }
                    )
                }
            }

            Button(
                onClick = onGeneratePureBackground,
                enabled = !uiState.isProcessingAi,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_pure_bg_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "توليد خلفية إعلانية صافية (خالية من النصوص)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AiConsultantAndAnalyzerTabContent(
    uiState: StudioUiState,
    onAnalyzeImage: () -> Unit,
    onUpdateQuery: (String) -> Unit,
    onToggleThinking: (Boolean) -> Unit,
    onToggleSearchGrounding: (Boolean) -> Unit,
    onAskConsultant: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_consultant_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "المحلل الذكي والمستشار الإبداعي (Gemini 4 Argon & Search)",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            OutlinedButton(
                onClick = onAnalyzeImage,
                enabled = !uiState.isProcessingAi,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("consultant_analyze_image_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("فحص وتحليل الصورة المرفوعة عبر Gemini 4 Argon")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "وضع التفكير العميق (ThinkingLevel.HIGH)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يستخدم Gemini 4 Argon للتخطيط الإعلاني وهندسة التغليف المعقدة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.isThinkingModeEnabled,
                    onCheckedChange = onToggleThinking,
                    modifier = Modifier.testTag("high_thinking_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "ربط بيانات بحث Google المباشر (Search Grounding)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يستخدم Gemini 3.8 Flash مع Google Search لأحدث اتجاهات الطباعة والتغليف",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.isSearchGroundingEnabled,
                    onCheckedChange = onToggleSearchGrounding,
                    modifier = Modifier.testTag("search_grounding_switch")
                )
            }

            OutlinedTextField(
                value = uiState.aiConsultantQueryAr,
                onValueChange = onUpdateQuery,
                label = { Text("اطلب استشارة تصميمية أو مواصفات طباعة أو أفكار موك أب") },
                placeholder = { Text("مثال: ما هي أفضل خامات ومقاسات طباعة ملصقات زجاجات العطور الفاخرة 50 مل؟") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("consultant_query_input"),
                minLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAskConsultant(false) },
                    enabled = !uiState.isProcessingAi,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("ask_consultant_main_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استشارة شاملة")
                }

                OutlinedButton(
                    onClick = { onAskConsultant(true) },
                    enabled = !uiState.isProcessingAi,
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp)
                        .testTag("ask_consultant_fast_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("رد سريع (Lite)")
                }
            }

            uiState.aiConsultantResponseAr?.let { reply ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("consultant_response_box")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "توصيات مستشار مشتاق جرافكس الذكي:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = reply,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectHistoryTabContent(
    projects: List<StudioProjectEntity>,
    cloudProjectsState: com.example.viewmodel.CloudProjectsUiState = com.example.viewmodel.CloudProjectsUiState.Success(emptyList()),
    onSaveCurrentToCloud: () -> Unit = {},
    onDeleteCloudProject: (String) -> Unit = {},
    onReloadProject: (StudioProjectEntity) -> Unit,
    onDeleteProject: (Int) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("project_history_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cloud Firestore Sync Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "المزامنة السحابية (Firebase Firestore)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = onSaveCurrentToCloud,
                    modifier = Modifier.testTag("save_to_cloud_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("مزامنة للسحابة الآن")
                }
            }

            when (cloudProjectsState) {
                is com.example.viewmodel.CloudProjectsUiState.Loading -> {
                    Text(
                        text = "جارٍ مزامنة المشاريع السحابية...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is com.example.viewmodel.CloudProjectsUiState.Error -> {
                    Text(
                        text = cloudProjectsState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is com.example.viewmodel.CloudProjectsUiState.Success -> {
                    val cloudItems = cloudProjectsState.projects
                    if (cloudItems.isEmpty()) {
                        Text(
                            text = "لا توجد مشاريع محفوظة في السحابة بعد. سيتم حفظ مشاريعك تلقائيًا في Firestore عند التصدير أو الضغط على «مزامنة للسحابة الآن».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        cloudItems.forEach { cloudItem ->
                            val displayTime = (cloudItem.updatedAt ?: cloudItem.createdAt)?.toDate() ?: Date()
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cloud_history_item_${cloudItem.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "☁️ ${cloudItem.title}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${cloudItem.processingModeAr} • ${cloudItem.exportFormat} (${cloudItem.outputWidth}×${cloudItem.outputHeight}px)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "الأصل: ${cloudItem.originalFileName} • ${dateFormat.format(displayTime)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteCloudProject(cloudItem.id) },
                                        modifier = Modifier.testTag("delete_cloud_${cloudItem.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف من السحابة",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

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
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "سجل الأعمال الأخيرة المحفوظة محليًا (${projects.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (projects.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearAll,
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Text("مسح الكل")
                    }
                }
            }

            if (projects.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "لا توجد مشاريع مصدرة في السجل حتى الآن",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "عند تنزيل أي نتيجة بصيغة PNG أو JPEG، سيتم حفظ نسخة في سجل أعمالك المحلي والسحابي للرجوع إليها في أي وقت.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                projects.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_item_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${item.processingModeAr} • ${item.exportFormat} (${item.outputWidth}×${item.outputHeight}px)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "الأصل: ${item.originalFileName} • ${dateFormat.format(Date(item.timestamp))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { onReloadProject(item) },
                                    modifier = Modifier.testTag("reload_history_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "فتح المشروع",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteProject(item.id) },
                                    modifier = Modifier.testTag("delete_history_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف من السجل",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

