package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.CloudStudioRepository
import com.example.data.StudioCloudProject
import com.example.data.StudioProjectEntity
import com.example.data.StudioRepository
import com.example.model.AdBackgroundConfig
import com.example.model.AiImageModelChoice
import com.example.model.IdentityProtectionConfig
import com.example.model.ImageClassification
import com.example.model.LayerTransformState
import com.example.model.LightingPreset
import com.example.model.MaskShapeType
import com.example.model.MockupCategory
import com.example.model.OriginalDesignAsset
import com.example.model.PerspectivePreset
import com.example.model.ProcessingMode
import com.example.model.ProductEnhancementConfig
import com.example.model.StudioAspectRatio
import com.example.model.StudioImageSize
import com.example.model.StudioTab
import com.example.util.MockupCompositorEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "StudioViewModel"

sealed interface CloudProjectsUiState {
    data object Loading : CloudProjectsUiState
    data class Success(val projects: List<StudioCloudProject>) : CloudProjectsUiState
    data class Error(val message: String) : CloudProjectsUiState
}

data class StudioUiState(
    val isDarkTheme: Boolean = true,
    val currentTab: StudioTab = StudioTab.MOCKUP_STUDIO,
    val originalAsset: OriginalDesignAsset? = null,
    val workingDesignBitmap: Bitmap? = null,
    val aiGeneratedSceneBitmap: Bitmap? = null,
    val compositeResultBitmap: Bitmap? = null,
    val processingMode: ProcessingMode = ProcessingMode.STRICT_PRESERVATION,
    val selectedCategory: MockupCategory = MockupCategory.PLASTIC_BAGS,
    val customCategoryPromptAr: String = "",
    val selectedAspectRatio: StudioAspectRatio = StudioAspectRatio.RATIO_1_1,
    val selectedImageSize: StudioImageSize = StudioImageSize.SIZE_1K,
    val selectedImageModel: AiImageModelChoice = AiImageModelChoice.NANO_BANANA_2_1,
    val selectedLighting: LightingPreset = LightingPreset.LUXURY_GOLD_RIM,
    val selectedPerspective: PerspectivePreset = PerspectivePreset.FRONT_DIRECT,
    val identityProtection: IdentityProtectionConfig = IdentityProtectionConfig(),
    val layerTransform: LayerTransformState = LayerTransformState(),
    val productEnhanceConfig: ProductEnhancementConfig = ProductEnhancementConfig(),
    val adBackgroundConfig: AdBackgroundConfig = AdBackgroundConfig(),
    val isComparingBeforeAfter: Boolean = false,
    val comparisonSliderPosition: Float = 0.5f,
    val isProcessingAi: Boolean = false,
    val processingStepMessageAr: String = "",
    val processingProgress: Float = 0f,
    val statusBannerMessageAr: String? = null,
    val errorBannerMessageAr: String? = null,
    val aiConsultantQueryAr: String = "",
    val aiConsultantResponseAr: String? = null,
    val isThinkingModeEnabled: Boolean = true,
    val isSearchGroundingEnabled: Boolean = false
)

class StudioViewModel(
    private val repository: StudioRepository,
    private val cloudRepository: CloudStudioRepository? = null,
    private val currentUserId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StudioUiState(isDarkTheme = repository.isDarkThemePreferred())
    )
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val recentProjects: StateFlow<List<StudioProjectEntity>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Two-Tier Kotlin Flow Error Handling Architecture for Firestore Realtime Observation
    val cloudProjectsState: StateFlow<CloudProjectsUiState> =
        if (cloudRepository != null && !currentUserId.isNullOrBlank()) {
            cloudRepository.observeUserProjects(currentUserId)
                .map<List<StudioCloudProject>, CloudProjectsUiState> { CloudProjectsUiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing cloud projects", error)
                    emit(CloudProjectsUiState.Error(error.localizedMessage ?: "تعذر مزامنة المشاريع السحابية"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = CloudProjectsUiState.Loading
                )
        } else {
            flowOf(CloudProjectsUiState.Success(emptyList()))
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = CloudProjectsUiState.Success(emptyList())
                )
        }


    // Lazy Firebase AI Logic Models
    private val imageAnalysisSchema = Schema.obj(
        properties = mapOf(
            "classificationIndex" to Schema.integer(description = "1 to 7 matching the 7 classification types"),
            "containsArabicOrEnglishText" to Schema.boolean(description = "Whether the image has visible text"),
            "containsBrandLogo" to Schema.boolean(description = "Whether the image has a logo or emblem"),
            "containsHumanFaces" to Schema.boolean(description = "Whether the image has human faces"),
            "recommendedMode" to Schema.enumeration(
                values = listOf("STRICT_PRESERVATION", "AI_ENHANCEMENT"),
                description = "Recommended processing mode"
            ),
            "detailedSummaryAr" to Schema.string(description = "Detailed professional analysis in Arabic")
        )
    )

    private val imageAnalyzerModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = imageAnalysisSchema
            }
        )
    }

    private val highThinkingModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    setThinkingLevel(ThinkingLevel.HIGH)
                }
            }
        )
    }

    private val searchGroundedModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch())
        )
    }

    private val fastLiteModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite"
        )
    }

    private var previewRenderJob: kotlinx.coroutines.Job? = null

    init {
        // Initialize sample design and composite preview asynchronously on background thread
        // so the main UI thread never skips frames during startup.
        viewModelScope.launch(Dispatchers.Default) {
            val savedCatId = repository.getLastSelectedCategoryId()
            val initialCategory = MockupCategory.entries.find { it.id == savedCatId } ?: MockupCategory.PLASTIC_BAGS
            val sampleAsset = MockupCompositorEngine.createSampleArabicBrandDesign()

            _uiState.update { state ->
                state.copy(
                    selectedCategory = initialCategory,
                    originalAsset = sampleAsset,
                    workingDesignBitmap = sampleAsset.bitmap,
                    layerTransform = state.layerTransform.copy(
                        scale = initialCategory.defaultScale,
                        perspectiveX = initialCategory.defaultPerspectiveX,
                        perspectiveY = initialCategory.defaultPerspectiveY,
                        maskShape = initialCategory.maskShape
                    )
                )
            }
            refreshCompositePreview()
        }
    }

    fun toggleTheme() {
        val nextDark = !_uiState.value.isDarkTheme
        repository.setDarkThemePreferred(nextDark)
        _uiState.update { it.copy(isDarkTheme = nextDark) }
        refreshCompositePreview()
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(currentTab = tab, errorBannerMessageAr = null) }
    }

    fun clearBanners() {
        _uiState.update { it.copy(statusBannerMessageAr = null, errorBannerMessageAr = null) }
    }

    /**
     * Loads and validates an image from a device URI without auto-modifying the original asset.
     */
    fun onUploadDesignUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    processingStepMessageAr = "جارٍ فحص نوع الملف وحجمه وأبعاده...",
                    processingProgress = 0.3f,
                    errorBannerMessageAr = null
                )
            }

            val result = withContext(Dispatchers.IO) {
                MockupCompositorEngine.inspectAndLoadUri(context, uri)
            }

            result.fold(
                onSuccess = { asset ->
                    _uiState.update { state ->
                        state.copy(
                            isProcessingAi = false,
                            processingStepMessageAr = "",
                            processingProgress = 1f,
                            originalAsset = asset,
                            workingDesignBitmap = asset.bitmap,
                            statusBannerMessageAr = "تم رفع الصورة الأصلية بنجاح (${asset.widthPx}×${asset.heightPx} بكسل - ${asset.fileSizeKb} كيلوبايت). النسخة الأصلية محفوظة ومحمية."
                        )
                    }
                    refreshCompositePreview()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            processingStepMessageAr = "",
                            processingProgress = 0f,
                            errorBannerMessageAr = error.localizedMessage ?: "فشل تحميل ملف الصورة."
                        )
                    }
                }
            )
        }
    }

    /**
     * Handles a Bitmap captured directly from the device camera.
     */
    fun onCaptureCameraBitmap(bitmap: Bitmap) {
        val immutableCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val asset = OriginalDesignAsset(
            bitmap = immutableCopy,
            fileName = "camera_capture_${System.currentTimeMillis()}.png",
            mimeType = "image/png",
            fileSizeKb = maxOf(1L, (immutableCopy.byteCount / 1024).toLong()),
            widthPx = immutableCopy.width,
            heightPx = immutableCopy.height,
            classification = ImageClassification.REAL_PRODUCT_PHOTO
        )
        _uiState.update {
            it.copy(
                originalAsset = asset,
                workingDesignBitmap = immutableCopy,
                statusBannerMessageAr = "تم التقاط الصورة بالكاميرا وحفظ النسخة الأصلية بنجاح (${asset.widthPx}×${asset.heightPx} بكسل).",
                errorBannerMessageAr = null
            )
        }
        refreshCompositePreview()
    }

    fun loadSampleArabicDesign() {
        viewModelScope.launch(Dispatchers.Default) {
            val sample = MockupCompositorEngine.createSampleArabicBrandDesign()
            _uiState.update {
                it.copy(
                    originalAsset = sample,
                    workingDesignBitmap = sample.bitmap,
                    statusBannerMessageAr = "تم تحميل نموذج تصميم «مشتاق جرافكس» التجريبي لاختبار دقة النصوص والموك أب.",
                    errorBannerMessageAr = null
                )
            }
            refreshCompositePreview()
        }
    }

    fun restoreUntouchedOriginalDesign() {
        val orig = _uiState.value.originalAsset?.bitmap ?: return
        _uiState.update {
            it.copy(
                workingDesignBitmap = orig,
                layerTransform = LayerTransformState(
                    scale = it.selectedCategory.defaultScale,
                    perspectiveX = it.selectedCategory.defaultPerspectiveX,
                    perspectiveY = it.selectedCategory.defaultPerspectiveY,
                    maskShape = it.selectedCategory.maskShape
                ),
                statusBannerMessageAr = "تمت استعادة النسخة الأصلية غير المعدلة وإعادة ضبط أبعاد الطبقة."
            )
        }
        refreshCompositePreview()
    }

    fun removeBackgroundAroundProduct() {
        val currentBitmap = _uiState.value.workingDesignBitmap ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    processingStepMessageAr = "جارٍ تفريغ الخلفية المحيطة مع المحافظة الكاملة على المنتج والنصوص...",
                    processingProgress = 0.5f
                )
            }
            val cutBitmap = withContext(Dispatchers.Default) {
                MockupCompositorEngine.removeBackgroundEdges(currentBitmap)
            }
            _uiState.update {
                it.copy(
                    isProcessingAi = false,
                    processingStepMessageAr = "",
                    processingProgress = 1f,
                    workingDesignBitmap = cutBitmap,
                    layerTransform = it.layerTransform.copy(maskShape = MaskShapeType.NONE),
                    statusBannerMessageAr = "تم تفريغ الحواف الخلفية بنجاح مع الحفاظ على تفاصيل المنتج والملصق الأصلي."
                )
            }
            refreshCompositePreview()
        }
    }

    fun setProcessingMode(mode: ProcessingMode) {
        _uiState.update { it.copy(processingMode = mode) }
        refreshCompositePreview()
    }

    fun selectMockupCategory(category: MockupCategory) {
        repository.setLastSelectedCategoryId(category.id)
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                layerTransform = state.layerTransform.copy(
                    scale = category.defaultScale,
                    perspectiveX = category.defaultPerspectiveX,
                    perspectiveY = category.defaultPerspectiveY,
                    maskShape = category.maskShape,
                    show3DEmbossEffect = (category == MockupCategory.LOGOS_3D_IDENTITY)
                )
            )
        }
        refreshCompositePreview()
    }

    fun updateCustomCategoryPrompt(text: String) {
        _uiState.update { it.copy(customCategoryPromptAr = text) }
    }

    fun selectAspectRatio(ratio: StudioAspectRatio) {
        _uiState.update { it.copy(selectedAspectRatio = ratio) }
        refreshCompositePreview()
    }

    fun selectImageSize(size: StudioImageSize) {
        _uiState.update { it.copy(selectedImageSize = size) }
        refreshCompositePreview()
    }

    fun selectAiImageModel(modelChoice: AiImageModelChoice) {
        _uiState.update { it.copy(selectedImageModel = modelChoice) }
    }

    fun selectLightingPreset(preset: LightingPreset) {
        _uiState.update { state ->
            state.copy(
                selectedLighting = preset,
                layerTransform = state.layerTransform.copy(surfaceLightingBlend = preset.highlightAlpha)
            )
        }
        refreshCompositePreview()
    }

    fun selectPerspectivePreset(preset: PerspectivePreset) {
        _uiState.update { state ->
            state.copy(
                selectedPerspective = preset,
                layerTransform = state.layerTransform.copy(
                    perspectiveX = preset.skewX,
                    perspectiveY = preset.skewY
                )
            )
        }
        refreshCompositePreview()
    }

    fun updateIdentityProtection(transform: (IdentityProtectionConfig) -> IdentityProtectionConfig) {
        _uiState.update { state ->
            val updated = transform(state.identityProtection)
            state.copy(identityProtection = updated)
        }
    }

    fun updateLayerTransform(transform: (LayerTransformState) -> LayerTransformState) {
        _uiState.update { state ->
            state.copy(layerTransform = transform(state.layerTransform))
        }
        refreshCompositePreview()
    }

    fun updateProductEnhanceConfig(transform: (ProductEnhancementConfig) -> ProductEnhancementConfig) {
        _uiState.update { state ->
            state.copy(productEnhanceConfig = transform(state.productEnhanceConfig))
        }
        refreshCompositePreview()
    }

    fun updateAdBackgroundConfig(transform: (AdBackgroundConfig) -> AdBackgroundConfig) {
        _uiState.update { state ->
            state.copy(adBackgroundConfig = transform(state.adBackgroundConfig))
        }
    }

    fun toggleCompareBeforeAfter(enabled: Boolean) {
        _uiState.update { it.copy(isComparingBeforeAfter = enabled) }
    }

    fun updateComparisonSlider(position: Float) {
        _uiState.update { it.copy(comparisonSliderPosition = position.coerceIn(0.05f, 0.95f)) }
    }

    fun refreshCompositePreview() {
        previewRenderJob?.cancel()
        previewRenderJob = viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val designBmp = state.workingDesignBitmap
            val drawLayer = when (state.currentTab) {
                StudioTab.AD_BACKGROUNDS -> false
                StudioTab.PRODUCT_ENHANCE -> {
                    state.processingMode == ProcessingMode.STRICT_PRESERVATION ||
                            state.productEnhanceConfig.preserveOriginalLabelLayer ||
                            state.aiGeneratedSceneBitmap == null
                }
                else -> {
                    state.processingMode == ProcessingMode.STRICT_PRESERVATION ||
                            state.aiGeneratedSceneBitmap == null
                }
            }

            val composite = MockupCompositorEngine.renderCompositeMockup(
                originalDesign = designBmp,
                aiSceneBackdrop = state.aiGeneratedSceneBitmap,
                category = state.selectedCategory,
                transform = state.layerTransform,
                aspectRatio = state.selectedAspectRatio,
                imageSize = state.selectedImageSize,
                drawOriginalLayer = drawLayer,
                reflectionIntensity = state.productEnhanceConfig.surfaceReflectionIntensity,
                ambientLightIntensity = state.productEnhanceConfig.ambientLightingLevel,
                isDarkStudioBackdropFallback = state.isDarkTheme,
                isInteractivePreview = true
            )

            _uiState.update { it.copy(compositeResultBitmap = composite) }
        }
    }

    private fun mapAspectRatioToSdk(ratio: StudioAspectRatio): AspectRatio = when (ratio) {
        StudioAspectRatio.RATIO_1_1 -> AspectRatio.SQUARE_1x1
        StudioAspectRatio.RATIO_2_3 -> AspectRatio.PORTRAIT_2x3
        StudioAspectRatio.RATIO_3_2 -> AspectRatio.LANDSCAPE_3x2
        StudioAspectRatio.RATIO_3_4 -> AspectRatio.PORTRAIT_3x4
        StudioAspectRatio.RATIO_4_3 -> AspectRatio.LANDSCAPE_4x3
        StudioAspectRatio.RATIO_9_16 -> AspectRatio.PORTRAIT_9x16
        StudioAspectRatio.RATIO_16_9 -> AspectRatio.LANDSCAPE_16x9
        StudioAspectRatio.RATIO_21_9 -> AspectRatio.LANDSCAPE_21x9
    }

    private fun mapImageSizeToSdk(size: StudioImageSize): ImageSize = when (size) {
        StudioImageSize.SIZE_1K -> ImageSize.SIZE_1K
        StudioImageSize.SIZE_2K -> ImageSize.SIZE_2K
        StudioImageSize.SIZE_4K -> ImageSize.SIZE_4K
    }

    private fun buildConfiguredImageModel(
        modelChoice: AiImageModelChoice,
        ratio: StudioAspectRatio,
        size: StudioImageSize
    ): GenerativeModel {
        val imgConfig = ImageConfig.builder()
            .setAspectRatio(mapAspectRatioToSdk(ratio))
            .setImageSize(mapImageSizeToSdk(size))
            .build()

        val genConfig = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = imgConfig
        }

        return Firebase.ai.generativeModel(
            modelName = modelChoice.modelId,
            generationConfig = genConfig
        )
    }

    /**
     * Generates the Mockup Scene or AI-Enhanced Product Scene using Firebase AI Logic
     * (gemini-nano-banana-2.1 or gemini-3-pro-image with aspect ratio & 1K/2K/4K resolution).
     */
    fun generateStudioMockupWithAi() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    processingStepMessageAr = "جارٍ توليد مشهد الموك أب عبر ${state.selectedImageModel.titleAr} (${state.selectedAspectRatio.label} - ${state.selectedImageSize.label})...",
                    processingProgress = 0.25f,
                    errorBannerMessageAr = null,
                    statusBannerMessageAr = null
                )
            }

            try {
                val model = buildConfiguredImageModel(
                    modelChoice = state.selectedImageModel,
                    ratio = state.selectedAspectRatio,
                    size = state.selectedImageSize
                )

                val protectionGuardrails = state.identityProtection.buildStrictPromptGuardrailsEn()
                val customDetail = if (state.customCategoryPromptAr.isNotBlank()) {
                    "Additional context requested by designer: ${state.customCategoryPromptAr}."
                } else ""

                val generatedBitmap: Bitmap? = withTimeout(60_000L) {
                    if (state.processingMode == ProcessingMode.STRICT_PRESERVATION) {
                        // In Strict Preservation mode, generate the unbranded mockup carrier/environment
                        // and composite the untouched original design layer on top with perspective & shadows.
                        val prompt = buildString {
                            append(state.selectedCategory.basePromptEn)
                            append(". Lighting style: ${state.selectedLighting.promptEn}.")
                            append(" Camera angle: ${state.selectedPerspective.promptEn}.")
                            append(" $customDetail ")
                            append(protectionGuardrails)
                        }
                        val response = model.generateContent(prompt)
                        response.candidates.firstOrNull()
                            ?.content?.parts?.filterIsInstance<ImagePart>()
                            ?.firstOrNull()?.image
                    } else {
                        // In AI Enhancement mode, pass the original image + edit instructions
                        val inputBmp = state.workingDesignBitmap ?: state.originalAsset?.bitmap
                        val editPrompt = buildString {
                            append("Place and enhance this product/design inside a realistic commercial mockup scene: ")
                            append(state.selectedCategory.basePromptEn)
                            append(". Lighting: ${state.selectedLighting.promptEn}.")
                            append(" $customDetail ")
                            append("Preserve the original text, logo, colors, and proportions as closely as possible. ")
                            append(protectionGuardrails)
                        }
                        val response = if (inputBmp != null) {
                            model.generateContent(
                                content {
                                    image(inputBmp)
                                    text(editPrompt)
                                }
                            )
                        } else {
                            model.generateContent(editPrompt)
                        }
                        response.candidates.firstOrNull()
                            ?.content?.parts?.filterIsInstance<ImagePart>()
                            ?.firstOrNull()?.image
                    }
                }

                if (generatedBitmap != null) {
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            processingStepMessageAr = "",
                            processingProgress = 1f,
                            aiGeneratedSceneBitmap = generatedBitmap,
                            statusBannerMessageAr = if (state.processingMode == ProcessingMode.STRICT_PRESERVATION) {
                                "تم توليد مشهد الموك أب بنجاح ودمج التصميم الأصلي كطبقة مستقلة محمية بنسبة 100%."
                            } else {
                                "تم توليد المشهد بالذكاء الاصطناعي بنجاح. يمكنك المقارنة (قبل/بعد) أو تفعيل طبقة التصميم الأصلي."
                            }
                        )
                    }
                    refreshCompositePreview()
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            processingStepMessageAr = "",
                            processingProgress = 0f,
                            errorBannerMessageAr = "لم يُرجع نموذج الذكاء الاصطناعي صورة صالحة. يرجى المحاولة مرة أخرى."
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Mockup generation timed out", e)
                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 0f,
                        errorBannerMessageAr = "انتهت مهلة معالجة الصورة (60 ثانية). يرجى المحاولة مرة أخرى أو اختيار دقة 1K السريعة."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Mockup generation failed", e)
                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 0f,
                        errorBannerMessageAr = "تعذر توليد المشهد بالذكاء الاصطناعي: ${e.localizedMessage ?: "خطأ في الخدمة"}"
                    )
                }
            }
        }
    }

    /**
     * Enhances product presentation (Section 5) using AI scene generation + optional strict label overlay.
     */
    fun enhanceProductPresentationWithAi() {
        val state = _uiState.value
        val cfg = state.productEnhanceConfig
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    processingStepMessageAr = "جارٍ تجهيز المشهد التجاري الفاخر وتحسين الإضاءة والانعكاسات...",
                    processingProgress = 0.3f,
                    errorBannerMessageAr = null
                )
            }

            try {
                val model = buildConfiguredImageModel(
                    modelChoice = state.selectedImageModel,
                    ratio = state.selectedAspectRatio,
                    size = state.selectedImageSize
                )

                val prompt = buildString {
                    append("Commercial product photography studio backdrop: ${cfg.backgroundStylePromptEn}. ")
                    append("Viewing angle: ${cfg.viewingAngleAr}. ")
                    if (cfg.luxurySceneEnabled) {
                        append("Ultra-luxury commercial advertising studio atmosphere with balanced rim lighting, natural depth of field, and realistic surface reflections. ")
                    }
                    if (cfg.customChangesPromptAr.isNotBlank()) {
                        append("Specific designer instructions: ${cfg.customChangesPromptAr}. ")
                    }
                    append("Leave the center podium clear for the product. ")
                    append(state.identityProtection.buildStrictPromptGuardrailsEn())
                }

                val generatedBitmap = withTimeout(60_000L) {
                    val inputBmp = state.workingDesignBitmap
                    val response = if (!cfg.preserveOriginalLabelLayer && inputBmp != null) {
                        model.generateContent(
                            content {
                                image(inputBmp)
                                text("Enhance the surrounding environment, lighting, shadows, and reflections around this product without changing the product shape or label: $prompt")
                            }
                        )
                    } else {
                        model.generateContent(prompt)
                    }
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }

                if (generatedBitmap != null) {
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            processingStepMessageAr = "",
                            processingProgress = 1f,
                            aiGeneratedSceneBitmap = generatedBitmap,
                            statusBannerMessageAr = if (cfg.preserveOriginalLabelLayer) {
                                "تم إنشاء المشهد التجاري الفاخر مع تثبيت المنتج/الملصق الأصلي كطبقة محمية دون المساس بتفاصيله."
                            } else {
                                "تم تحسين عرض المنتج بالذكاء الاصطناعي بنجاح."
                            }
                        )
                    }
                    refreshCompositePreview()
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            processingStepMessageAr = "",
                            processingProgress = 0f,
                            errorBannerMessageAr = "لم يتم إرجاع صورة من النموذج. حاول مجددًا."
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Product enhancement timed out", e)
                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 0f,
                        errorBannerMessageAr = "استغرقت المعالجة وقتًا أطول من المعتاد (60 ثانية). يرجى المحاولة مجددًا."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Product enhancement failed", e)
                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 0f,
                        errorBannerMessageAr = "فشل تحسين عرض المنتج: ${e.localizedMessage ?: "خطأ غير متوقع"}"
                    )
                }
            }
        }
    }

    /**
     * Generates a pure advertising background completely free of text, logos, watermarks, or numbers (Section 6).
     */
    fun generatePureAdBackground() {
        val state = _uiState.value
        val bg = state.adBackgroundConfig
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    processingStepMessageAr = "جارٍ توليد خلفية إعلانية صافية خالية من النصوص (${state.selectedAspectRatio.label} - ${state.selectedImageSize.label})...",
                    processingProgress = 0.35f,
                    errorBannerMessageAr = null
                )
            }

            try {
                val model = buildConfiguredImageModel(
                    modelChoice = state.selectedImageModel,
                    ratio = state.selectedAspectRatio,
                    size = state.selectedImageSize
                )

                val postureText = if (bg.isDarkPosture) {
                    "Dark luxury dramatic studio theme with deep rich shadows and warm highlights"
                } else {
                    "Bright clean airy commercial studio theme with soft natural daylight"
                }

                val prompt = buildString {
                    append("Commercial advertising background only for business sector: ${bg.businessTypeAr}, ")
                    append("suitable for showcasing product type: ${bg.productTypeAr}. ")
                    append("Color palette and visual style: ${bg.colorVisualStyleAr}. ")
                    append("Lighting mood: $postureText. ")
                    append("Presentation style: ${bg.displayStyleAr}. ")
                    append("Composition layout: Leave clean uncluttered negative space (${bg.textSafeZoneAr}) so a graphic designer can add Arabic typography later in design software. ")
                    append("STRICT REQUIREMENT: Background only. NO text, NO letters, NO numbers, NO logos, NO watermarks, NO labels, NO signs anywhere in the image.")
                }

                val generatedBitmap = withTimeout(60_000L) {
                    val response = model.generateContent(prompt)
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }

                if (generatedBitmap != null) {
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            processingStepMessageAr = "",
                            processingProgress = 1f,
                            aiGeneratedSceneBitmap = generatedBitmap,
                            compositeResultBitmap = generatedBitmap,
                            statusBannerMessageAr = "تم توليد الخلفية الإعلانية الصافية بدون أي نصوص أو علامات مائية، مع مساحة مهيأة للكتابة."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            processingStepMessageAr = "",
                            processingProgress = 0f,
                            errorBannerMessageAr = "تعذر توليد الخلفية الإعلانية. يرجى المحاولة مرة أخرى."
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Ad background generation timed out", e)
                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 0f,
                        errorBannerMessageAr = "انتهت مهلة توليد الخلفية (60 ثانية). يرجى المحاولة مرة أخرى."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ad background generation failed", e)
                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 0f,
                        errorBannerMessageAr = "فشل توليد الخلفية الإعلانية: ${e.localizedMessage ?: "خطأ في الاتصال"}"
                    )
                }
            }
        }
    }

    /**
     * Analyzes the uploaded image using `gemini-4-argon` (Image Understanding)
     * and classifies it into one of the 7 protection categories (Section 5).
     */
    fun analyzeUploadedImageWithGemini() {
        val asset = _uiState.value.originalAsset ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    processingStepMessageAr = "جارٍ تحليل الصورة وتصنيف عناصر الهوية عبر Gemini 4 Argon...",
                    processingProgress = 0.4f,
                    errorBannerMessageAr = null
                )
            }

            try {
                val prompt = """
                    أنت خبير فحص جودة الطباعة والتصميم الجرافيكي في استوديو مشتاق جرافكس.
                    حلل هذه الصورة بدقة وصنفها إلى أحد الأنواع السبعة التالية (classificationIndex من 1 إلى 7):
                    1 = تصميم مسطح يحتوي على نصوص وشعارات
                    2 = صورة منتج حقيقي
                    3 = عبوة أو ملصق تجاري
                    4 = لوحة محل أو إعلان مطبوع
                    5 = هوية بصرية أو شعار
                    6 = صورة تحتوي على أشخاص
                    7 = تصميم مركب يحتوي على عدة عناصر
                    حدد هل تحتوي على نصوص عربية أو إنجليزية، وهل تحتوي على شعار، وهل تحتوي على وجوه أشخاص، وما هو وضع المعالجة الأنسب لحماية الهوية الأصلية، واكتب تقريرًا فنيًا موجزًا باللغة العربية في detailedSummaryAr.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    imageAnalyzerModel.generateContent(
                        content {
                            image(asset.bitmap)
                            text(prompt)
                        }
                    )
                }

                val jsonText = response.text ?: throw IllegalStateException("استجابة التحليل فارغة.")
                val json = JSONObject(jsonText)
                val classIdx = json.optInt("classificationIndex", 1).coerceIn(1, 7)
                val hasText = json.optBoolean("containsArabicOrEnglishText", true)
                val hasLogo = json.optBoolean("containsBrandLogo", true)
                val hasFaces = json.optBoolean("containsHumanFaces", false)
                val recModeStr = json.optString("recommendedMode", "STRICT_PRESERVATION")
                val summaryAr = json.optString("detailedSummaryAr", "تم فحص التصميم وتحديد متطلبات حماية الهوية.")

                val classification = ImageClassification.entries[classIdx - 1]
                val recMode = if (recModeStr == "AI_ENHANCEMENT" && !hasText) {
                    ProcessingMode.AI_ENHANCEMENT
                } else {
                    ProcessingMode.STRICT_PRESERVATION
                }

                _uiState.update { state ->
                    state.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 1f,
                        processingMode = recMode,
                        originalAsset = asset.copy(
                            classification = classification,
                            analysisReportAr = summaryAr
                        ),
                        identityProtection = state.identityProtection.copy(
                            protectTexts = hasText || state.identityProtection.protectTexts,
                            protectLogo = hasLogo || state.identityProtection.protectLogo,
                            protectFaces = hasFaces || state.identityProtection.protectFaces
                        ),
                        statusBannerMessageAr = "اكتمل التحليل الذكي: تم تصنيف الصورة ضمن «${classification.titleAr}» وضبط حماية الهوية تلقائيًا."
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Image analysis timed out", e)
                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 0f,
                        errorBannerMessageAr = "انتهت مهلة تحليل الصورة (60 ثانية). يرجى المحاولة مرة أخرى."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image analysis failed", e)
                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 0f,
                        errorBannerMessageAr = "فشل تحليل الصورة: ${e.localizedMessage ?: "خطأ في الاتصال"}"
                    )
                }
            }
        }
    }

    fun updateConsultantQuery(query: String) {
        _uiState.update { it.copy(aiConsultantQueryAr = query) }
    }

    fun toggleThinkingMode(enabled: Boolean) {
        _uiState.update { it.copy(isThinkingModeEnabled = enabled) }
    }

    fun toggleSearchGrounding(enabled: Boolean) {
        _uiState.update { it.copy(isSearchGroundingEnabled = enabled) }
    }

    /**
     * Runs the Creative & Print Production AI Consultant using either:
     * - `gemini-4-argon` with `ThinkingLevel.HIGH` for complex creative/technical queries
     * - `gemini-3.8-flash` with `Tool.googleSearch()` when Search Grounding is active
     * - `gemini-3.5-flash-lite` for rapid prompt/caption generation
     */
    fun askStudioConsultant(useFastLite: Boolean = false) {
        val state = _uiState.value
        val query = state.aiConsultantQueryAr.trim()
        if (query.isEmpty()) {
            _uiState.update { it.copy(errorBannerMessageAr = "يرجى كتابة سؤالك أو وصف المشروع التجاري أولًا.") }
            return
        }

        viewModelScope.launch {
            val modeLabel = when {
                useFastLite -> "الوضع السريع (Gemini 3.5 Flash-Lite)"
                state.isSearchGroundingEnabled -> "البحث المباشر عبر Google (Gemini 3.8 Flash)"
                state.isThinkingModeEnabled -> "التفكير العميق (Gemini 4 Argon - High Thinking)"
                else -> "المستشار الذكي"
            }

            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    processingStepMessageAr = "جارٍ إعداد الاستشارة الفنية عبر $modeLabel...",
                    processingProgress = 0.4f,
                    errorBannerMessageAr = null
                )
            }

            try {
                val systemPrompt = """
                    أنت المستشار الفني والمدير الإبداعي في «استوديو مشتاق جرافكس Moshtaq Grafx» المتخصص في التصميم الجرافيكي والطباعة التجارية وتصميم العبوات والملصقات والهويات البصرية.
                    قدم إجابة احترافية ومنظمة باللغة العربية تساعد المصمم عمليًا في اختيار الخامات، الألوان، الإضاءة، مقاسات الطباعة، وأفكار الموك أب، مع الالتزام التام بعدم تحريف هوية العميل.
                    سؤال المصمم: $query
                """.trimIndent()

                val activeModel = when {
                    useFastLite -> fastLiteModel
                    state.isSearchGroundingEnabled -> searchGroundedModel
                    else -> highThinkingModel
                }

                val response = withTimeout(60_000L) {
                    val bmp = state.originalAsset?.bitmap
                    if (bmp != null && !state.isSearchGroundingEnabled && !useFastLite) {
                        activeModel.generateContent(
                            content {
                                image(bmp)
                                text(systemPrompt)
                            }
                        )
                    } else {
                        activeModel.generateContent(systemPrompt)
                    }
                }

                val replyText = response.text
                if (!replyText.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            processingStepMessageAr = "",
                            processingProgress = 1f,
                            aiConsultantResponseAr = replyText,
                            statusBannerMessageAr = "تم توليد التوصيات الفنية بنجاح عبر $modeLabel."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            processingStepMessageAr = "",
                            processingProgress = 0f,
                            errorBannerMessageAr = "أرجع النموذج استجابة فارغة."
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Consultant query timed out", e)
                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 0f,
                        errorBannerMessageAr = "انتهت مهلة الاستشارة الذكية (60 ثانية). يرجى المحاولة مرة أخرى."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Consultant query failed", e)
                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 0f,
                        errorBannerMessageAr = "تعذر إتمام الاستشارة الذكية: ${e.localizedMessage ?: "خطأ في الاتصال"}"
                    )
                }
            }
        }
    }

    /**
     * Exports the final composite image in PNG or JPEG, verifies file integrity,
     * and records the entry in the local Room project history.
     */
    fun exportCurrentResult(context: Context, format: String) {
        val state = _uiState.value
        if (state.compositeResultBitmap == null && state.workingDesignBitmap == null) {
            _uiState.update { it.copy(errorBannerMessageAr = "لا توجد صورة جاهزة للتصدير. يرجى رفع تصميم أولًا.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    processingStepMessageAr = "جارٍ تجهيز وتصدير الملف النهائي عالي الدقة بصيغة $format...",
                    processingProgress = 0.6f,
                    errorBannerMessageAr = null
                )
            }

            val result = withContext(Dispatchers.IO) {
                val fullResBitmap = if (state.currentTab == StudioTab.AD_BACKGROUNDS && state.aiGeneratedSceneBitmap != null) {
                    state.aiGeneratedSceneBitmap
                } else {
                    val drawLayer = when (state.currentTab) {
                        StudioTab.AD_BACKGROUNDS -> false
                        StudioTab.PRODUCT_ENHANCE -> {
                            state.processingMode == ProcessingMode.STRICT_PRESERVATION ||
                                    state.productEnhanceConfig.preserveOriginalLabelLayer ||
                                    state.aiGeneratedSceneBitmap == null
                        }
                        else -> {
                            state.processingMode == ProcessingMode.STRICT_PRESERVATION ||
                                    state.aiGeneratedSceneBitmap == null
                        }
                    }
                    MockupCompositorEngine.renderCompositeMockup(
                        originalDesign = state.workingDesignBitmap,
                        aiSceneBackdrop = state.aiGeneratedSceneBitmap,
                        category = state.selectedCategory,
                        transform = state.layerTransform,
                        aspectRatio = state.selectedAspectRatio,
                        imageSize = state.selectedImageSize,
                        drawOriginalLayer = drawLayer,
                        reflectionIntensity = state.productEnhanceConfig.surfaceReflectionIntensity,
                        ambientLightIntensity = state.productEnhanceConfig.ambientLightingLevel,
                        isDarkStudioBackdropFallback = state.isDarkTheme,
                        isInteractivePreview = false
                    )
                }

                MockupCompositorEngine.exportFinalImage(
                    context = context,
                    bitmap = fullResBitmap,
                    format = format,
                    projectTitle = state.selectedCategory.titleAr
                )
            }

            if (result.success) {
                val entity = StudioProjectEntity(
                    title = state.selectedCategory.titleAr,
                    categoryTitleAr = state.selectedCategory.titleAr,
                    processingModeAr = state.processingMode.titleAr,
                    aspectRatioLabel = state.selectedAspectRatio.label,
                    resolutionLabel = state.selectedImageSize.label,
                    outputWidth = result.widthPx,
                    outputHeight = result.heightPx,
                    exportFormat = format.uppercase(),
                    savedFilePath = result.savedFilePath,
                    originalFileName = state.originalAsset?.fileName ?: "design.png"
                )
                repository.insertProject(entity)

                // Sync to Firestore Cloud if authenticated
                if (cloudRepository != null && !currentUserId.isNullOrBlank()) {
                    val cloudProject = StudioCloudProject(
                        userId = currentUserId,
                        title = state.selectedCategory.titleAr,
                        categoryTitleAr = state.selectedCategory.titleAr,
                        processingModeAr = state.processingMode.titleAr,
                        aspectRatioLabel = state.selectedAspectRatio.label,
                        resolutionLabel = state.selectedImageSize.label,
                        outputWidth = result.widthPx.toLong(),
                        outputHeight = result.heightPx.toLong(),
                        exportFormat = format.uppercase(),
                        savedFilePath = result.savedFilePath,
                        originalFileName = state.originalAsset?.fileName ?: "design.png",
                        notesAr = state.originalAsset?.analysisReportAr.orEmpty()
                    )
                    cloudRepository.createProject(cloudProject)
                }

                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 1f,
                        statusBannerMessageAr = result.messageAr
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessingAi = false,
                        processingStepMessageAr = "",
                        processingProgress = 0f,
                        errorBannerMessageAr = result.messageAr
                    )
                }
            }
        }
    }

    fun saveCurrentProjectToCloud() {
        val state = _uiState.value
        val cloudRepo = cloudRepository
        val uid = currentUserId
        if (cloudRepo == null || uid.isNullOrBlank()) {
            _uiState.update { it.copy(errorBannerMessageAr = "يرجى تسجيل الدخول بحساب Google لمزامنة المشاريع سحابيًا.") }
            return
        }
        viewModelScope.launch {
            val bmp = state.compositeResultBitmap ?: state.workingDesignBitmap
            val cloudProject = StudioCloudProject(
                userId = uid,
                title = state.selectedCategory.titleAr,
                categoryTitleAr = state.selectedCategory.titleAr,
                processingModeAr = state.processingMode.titleAr,
                aspectRatioLabel = state.selectedAspectRatio.label,
                resolutionLabel = state.selectedImageSize.label,
                outputWidth = (bmp?.width ?: 1024).toLong(),
                outputHeight = (bmp?.height ?: 1024).toLong(),
                exportFormat = "PNG",
                savedFilePath = "/cloud_sync/${System.currentTimeMillis()}.png",
                originalFileName = state.originalAsset?.fileName ?: "design.png",
                notesAr = state.originalAsset?.analysisReportAr.orEmpty()
            )
            val res = cloudRepo.createProject(cloudProject)
            res.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(statusBannerMessageAr = "تمت مزامنة وحفظ المشروع في سحابة Firebase Firestore بنجاح.")
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(errorBannerMessageAr = "تعذر الحفظ في السحابة: ${err.localizedMessage ?: "خطأ"}")
                    }
                }
            )
        }
    }

    fun deleteCloudProjectItem(projectId: String) {
        val cloudRepo = cloudRepository ?: return
        viewModelScope.launch {
            cloudRepo.deleteProject(projectId)
        }
    }

    fun reloadProjectFromHistory(project: StudioProjectEntity) {
        viewModelScope.launch {
            val loadedBmp = withContext(Dispatchers.IO) {
                val f = File(project.savedFilePath)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
            }
            if (loadedBmp != null) {
                _uiState.update {
                    it.copy(
                        currentTab = StudioTab.MOCKUP_STUDIO,
                        compositeResultBitmap = loadedBmp,
                        statusBannerMessageAr = "تم فتح المشروع المحفوظ «${project.title}» (${project.outputWidth}×${project.outputHeight}) من سجل الأعمال."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorBannerMessageAr = "ملف الصورة المحفوظ لم يعد موجودًا على الجهاز.")
                }
            }
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteProjectById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllProjects()
            _uiState.update { it.copy(statusBannerMessageAr = "تم مسح سجل الأعمال الأخيرة.") }
        }
    }

    companion object {
        fun provideFactory(
            repository: StudioRepository,
            cloudRepository: CloudStudioRepository? = null,
            currentUserId: String? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { StudioViewModel(repository, cloudRepository, currentUserId) }
        }
    }
}

