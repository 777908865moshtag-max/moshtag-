package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.LayerTransformState
import com.example.model.MockupCategory
import com.example.model.StudioAspectRatio
import com.example.model.StudioImageSize
import com.example.util.MockupCompositorEngine
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `verify app name and studio mockup compositor and export pipeline`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("مشتاق جرافكس", appName)

        // 1. Verify Sample Arabic Brand Design creation
        val sampleAsset = MockupCompositorEngine.createSampleArabicBrandDesign()
        assertNotNull(sampleAsset.bitmap)
        assertEquals(800, sampleAsset.widthPx)
        assertEquals(800, sampleAsset.heightPx)

        // 2. Verify Strict Preservation Composite Rendering across categories & aspect ratios
        val compositeBmp = MockupCompositorEngine.renderCompositeMockup(
            originalDesign = sampleAsset.bitmap,
            aiSceneBackdrop = null,
            category = MockupCategory.OIL_PERFUME_LABELS,
            transform = LayerTransformState(scale = 0.5f, perspectiveX = 0.05f, shadowIntensity = 0.4f),
            aspectRatio = StudioAspectRatio.RATIO_16_9,
            imageSize = StudioImageSize.SIZE_1K,
            drawOriginalLayer = true
        )
        assertNotNull(compositeBmp)
        assertEquals(1024, compositeBmp.width)
        assertTrue(compositeBmp.height in 570..580)

        // 3. Verify PNG Export produces a real, non-empty file on disk
        val exportResult = MockupCompositorEngine.exportFinalImage(
            context = context,
            bitmap = compositeBmp,
            format = "PNG",
            projectTitle = "اختبار_موك_أب"
        )
        assertTrue(exportResult.success)
        val exportedFile = File(exportResult.savedFilePath)
        assertTrue(exportedFile.exists())
        assertTrue(exportedFile.length() > 0L)
    }

    @Test
    fun `verify MainActivity launches without crash`() {
        org.robolectric.Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            controller.setup()
            val activity = controller.get()
            assertNotNull(activity)
        }
    }
}
