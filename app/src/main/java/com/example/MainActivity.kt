package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.CloudStudioRepository
import com.example.data.StudioDatabase
import com.example.data.StudioRepository
import com.example.model.StudioTab
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutFromGoogle
import com.example.ui.components.AiConsultantAndAnalyzerTabContent
import com.example.ui.components.MockupStudioWorkspaceContent
import com.example.ui.components.ProductEnhancementTabContent
import com.example.ui.components.ProjectHistoryTabContent
import com.example.ui.components.PureAdBackgroundsTabContent
import com.example.ui.components.StudioPreviewAndExportPanel
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.example.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        // Unauthenticated session: Render Google Sign-In UI only.
        // Feature screens and their Firestore ViewModels are NOT composed here.
        AuthScreen()
    } else {
        // Authenticated session: Key ViewModel by user.uid and resolve R.string.firestore_database_id at factory boundary
        val currentUserId = user.uid
        val studioViewModel: StudioViewModel = viewModel(
            key = currentUserId,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    val roomDb = StudioDatabase.getInstance(app)
                    StudioViewModel(
                        repository = StudioRepository(roomDb.studioProjectDao(), app),
                        cloudRepository = CloudStudioRepository(db),
                        currentUserId = currentUserId
                    )
                }
            }
        )
        val uiState by studioViewModel.uiState.collectAsStateWithLifecycle()

        MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
            MoshtaqGrafxStudioApp(
                viewModel = studioViewModel,
                userEmailOrName = user.displayName ?: user.email
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoshtaqGrafxStudioApp(
    viewModel: StudioViewModel,
    userEmailOrName: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentProjects by viewModel.recentProjects.collectAsStateWithLifecycle()
    val cloudProjectsState by viewModel.cloudProjectsState.collectAsStateWithLifecycle()

    // BackHandler for secondary tabs to return to the primary Mockup Studio tab
    BackHandler(enabled = uiState.currentTab != StudioTab.MOCKUP_STUDIO) {
        viewModel.selectTab(StudioTab.MOCKUP_STUDIO)
    }

    // Zero-permission Photo Picker for uploading designs (JPEG, PNG, WebP)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onUploadDesignUri(context, uri)
        }
    }

    // Camera capture preview launcher
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.onCaptureCameraBitmap(bitmap)
        }
    }

    // Runtime Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraCaptureLauncher.launch(null)
        }
    }

    val triggerCameraCapture = remember(context) {
        {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                cameraCaptureLauncher.launch(null)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "استوديو مشتاق جرافكس الذكي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userEmailOrName ?: "Moshtaq Grafx AI Studio • التصميم والموك أب الاحترافي",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("toggle_theme_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (uiState.isDarkTheme) "الوضع الفاتح" else "الوضع الداكن",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (userEmailOrName != null) {
                        IconButton(
                            onClick = {
                                signOutFromGoogle(
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {},
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "تسجيل الخروج",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("studio_bottom_nav")
            ) {
                StudioTab.entries.forEach { tab ->
                    val selected = uiState.currentTab == tab
                    val icon = when (tab) {
                        StudioTab.MOCKUP_STUDIO -> Icons.Default.Layers
                        StudioTab.PRODUCT_ENHANCE -> Icons.Default.Image
                        StudioTab.AD_BACKGROUNDS -> Icons.Default.Wallpaper
                        StudioTab.AI_CONSULTANT -> Icons.Default.Psychology
                        StudioTab.PROJECT_HISTORY -> Icons.Default.History
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(imageVector = icon, contentDescription = tab.titleAr) },
                        label = {
                            Text(
                                text = tab.titleAr,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name}")
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("studio_main_lazy_column"),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.statusBannerMessageAr != null || uiState.errorBannerMessageAr != null) {
                item(key = "banners") {
                    StatusAndErrorBanners(
                        statusMessage = uiState.statusBannerMessageAr,
                        errorMessage = uiState.errorBannerMessageAr,
                        onDismiss = { viewModel.clearBanners() }
                    )
                }
            }

            item(key = "preview_panel") {
                StudioPreviewAndExportPanel(
                    uiState = uiState,
                    onToggleCompare = { viewModel.toggleCompareBeforeAfter(it) },
                    onComparisonSliderChange = { viewModel.updateComparisonSlider(it) },
                    onDragDesignLayer = { dx, dy ->
                        viewModel.updateLayerTransform { tf ->
                            tf.copy(
                                offsetXFraction = (tf.offsetXFraction + dx).coerceIn(-0.8f, 0.8f),
                                offsetYFraction = (tf.offsetYFraction + dy).coerceIn(-0.8f, 0.8f)
                            )
                        }
                    },
                    onExportFormat = { fmt -> viewModel.exportCurrentResult(context, fmt) },
                    onRestoreOriginal = { viewModel.restoreUntouchedOriginalDesign() },
                    onRemoveBackgroundEdges = { viewModel.removeBackgroundAroundProduct() }
                )
            }

            item(key = "active_tab_${uiState.currentTab.name}") {
                ActiveTabToolsSection(
                    viewModel = viewModel,
                    onPickFromDevice = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onCaptureCamera = triggerCameraCapture,
                    recentProjects = recentProjects,
                    cloudProjectsState = cloudProjectsState
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ActiveTabToolsSection(
    viewModel: StudioViewModel,
    onPickFromDevice: () -> Unit,
    onCaptureCamera: () -> Unit,
    recentProjects: List<com.example.data.StudioProjectEntity>,
    cloudProjectsState: com.example.viewmodel.CloudProjectsUiState
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.currentTab) {
        StudioTab.MOCKUP_STUDIO -> {
            MockupStudioWorkspaceContent(
                uiState = uiState,
                onPickImageFromDevice = onPickFromDevice,
                onCaptureFromCamera = onCaptureCamera,
                onLoadSampleDesign = { viewModel.loadSampleArabicDesign() },
                onAnalyzeOriginalWithGemini = { viewModel.analyzeUploadedImageWithGemini() },
                onSelectProcessingMode = { viewModel.setProcessingMode(it) },
                onSelectCategory = { viewModel.selectMockupCategory(it) },
                onUpdateCustomCategoryPrompt = { viewModel.updateCustomCategoryPrompt(it) },
                onSelectAspectRatio = { viewModel.selectAspectRatio(it) },
                onSelectImageSize = { viewModel.selectImageSize(it) },
                onSelectImageModel = { viewModel.selectAiImageModel(it) },
                onSelectLighting = { viewModel.selectLightingPreset(it) },
                onSelectPerspective = { viewModel.selectPerspectivePreset(it) },
                onUpdateLayerTransform = { viewModel.updateLayerTransform(it) },
                onUpdateIdentityProtection = { viewModel.updateIdentityProtection(it) },
                onGenerateMockupWithAi = { viewModel.generateStudioMockupWithAi() }
            )
        }
        StudioTab.PRODUCT_ENHANCE -> {
            ProductEnhancementTabContent(
                uiState = uiState,
                onUpdateConfig = { viewModel.updateProductEnhanceConfig(it) },
                onUpdateLayerTransform = { viewModel.updateLayerTransform(it) },
                onSelectAspectRatio = { viewModel.selectAspectRatio(it) },
                onSelectImageSize = { viewModel.selectImageSize(it) },
                onEnhanceProductWithAi = { viewModel.enhanceProductPresentationWithAi() }
            )
        }
        StudioTab.AD_BACKGROUNDS -> {
            PureAdBackgroundsTabContent(
                uiState = uiState,
                onUpdateBgConfig = { viewModel.updateAdBackgroundConfig(it) },
                onSelectAspectRatio = { viewModel.selectAspectRatio(it) },
                onSelectImageSize = { viewModel.selectImageSize(it) },
                onGeneratePureBackground = { viewModel.generatePureAdBackground() }
            )
        }
        StudioTab.AI_CONSULTANT -> {
            AiConsultantAndAnalyzerTabContent(
                uiState = uiState,
                onAnalyzeImage = { viewModel.analyzeUploadedImageWithGemini() },
                onUpdateQuery = { viewModel.updateConsultantQuery(it) },
                onToggleThinking = { viewModel.toggleThinkingMode(it) },
                onToggleSearchGrounding = { viewModel.toggleSearchGrounding(it) },
                onAskConsultant = { fast -> viewModel.askStudioConsultant(fast) }
            )
        }
        StudioTab.PROJECT_HISTORY -> {
            ProjectHistoryTabContent(
                projects = recentProjects,
                cloudProjectsState = cloudProjectsState,
                onSaveCurrentToCloud = { viewModel.saveCurrentProjectToCloud() },
                onDeleteCloudProject = { viewModel.deleteCloudProjectItem(it) },
                onReloadProject = { viewModel.reloadProjectFromHistory(it) },
                onDeleteProject = { viewModel.deleteHistoryItem(it) },
                onClearAll = { viewModel.clearAllHistory() }
            )
        }
    }
}

@Composable
private fun StatusAndErrorBanners(
    statusMessage: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = errorMessage != null) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("error_banner")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "خطأ",
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    AnimatedVisibility(visible = statusMessage != null && errorMessage == null) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_banner")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "نجاح",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = statusMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
