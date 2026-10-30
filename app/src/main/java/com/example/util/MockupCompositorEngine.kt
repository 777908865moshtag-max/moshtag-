package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.model.ImageClassification
import com.example.model.LayerTransformState
import com.example.model.MaskShapeType
import com.example.model.MockupCategory
import com.example.model.OriginalDesignAsset
import com.example.model.StudioAspectRatio
import com.example.model.StudioImageSize
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ExportResult(
    val success: Boolean,
    val messageAr: String,
    val savedFilePath: String = "",
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val fileSizeKb: Long = 0L
)

object MockupCompositorEngine {

    private val SUPPORTED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    /**
     * Validates and loads an uploaded image from a Uri, ensuring supported format,
     * safe dimensions, and non-corrupted bitmap data.
     */
    fun inspectAndLoadUri(context: Context, uri: Uri): Result<OriginalDesignAsset> {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = (contentResolver.getType(uri) ?: "image/png").lowercase()

            if (mimeType !in SUPPORTED_MIME_TYPES && !mimeType.startsWith("image/")) {
                return Result.failure(
                    IllegalArgumentException("صيغة الملف غير مدعومة ($mimeType). يرجى رفع ملف بصيغة PNG أو JPEG أو WebP.")
                )
            }

            var fileName = "design_${System.currentTimeMillis()}.png"
            var fileSizeBytes = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        fileSizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }

            if (fileSizeBytes > 20 * 1024 * 1024) {
                return Result.failure(
                    IllegalArgumentException("حجم الصورة يتجاوز الحد الأقصى المسموح به (20 ميجابايت). يرجى اختيار صورة بحجم أقل.")
                )
            }

            // First decode bounds
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) {
                return Result.failure(
                    IllegalArgumentException("تعذر قراءة أبعاد الصورة. تأكد من أن الملف غير تالف.")
                )
            }

            // Calculate safe inSampleSize (preserve high sharpness up to 2048px)
            var sampleSize = 1
            val maxDim = max(rawWidth, rawHeight)
            while (maxDim / sampleSize > 2048) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return Result.failure(
                IllegalArgumentException("تعذر فتح ملف الصورة من الجهاز.")
            )

            // Make an immutable ARGB_8888 copy to guarantee original is never mutated
            val immutableOriginal = decoded.copy(Bitmap.Config.ARGB_8888, false)
            val heuristicClass = classifyImageHeuristically(immutableOriginal, fileName)

            val sizeKb = if (fileSizeBytes > 0) {
                max(1L, fileSizeBytes / 1024L)
            } else {
                max(1L, (immutableOriginal.byteCount / 1024).toLong())
            }

            Result.success(
                OriginalDesignAsset(
                    bitmap = immutableOriginal,
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSizeKb = sizeKb,
                    widthPx = rawWidth,
                    heightPx = rawHeight,
                    classification = heuristicClass
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("حدث خطأ أثناء فحص الصورة: ${e.localizedMessage ?: "ملف غير صالح"}")
            )
        }
    }

    /**
     * Heuristic initial classification before optional deep AI analysis.
     */
    private fun classifyImageHeuristically(bitmap: Bitmap, fileName: String): ImageClassification {
        val lower = fileName.lowercase()
        val ratio = bitmap.width.toFloat() / max(1, bitmap.height).toFloat()
        return when {
            lower.contains("logo") || lower.contains("شعار") -> ImageClassification.BRAND_IDENTITY_LOGO
            lower.contains("sign") || lower.contains("لوحة") || ratio > 2.0f -> ImageClassification.SHOP_SIGN_PRINTED_AD
            lower.contains("box") || lower.contains("label") || lower.contains("ملصق") -> ImageClassification.PACKAGING_COMMERCIAL_LABEL
            lower.contains("product") || lower.contains("منتج") -> ImageClassification.REAL_PRODUCT_PHOTO
            else -> ImageClassification.FLAT_DESIGN_TEXT_LOGO
        }
    }

    /**
     * Creates a crisp, high-resolution Arabic brand design artwork ("مشتاق جرافكس - عود ملكي")
     * so the user can immediately test strict preservation mockup compositing, perspective, and export.
     */
    fun createSampleArabicBrandDesign(): OriginalDesignAsset {
        val width = 800
        val height = 800
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Luxury dark emerald & charcoal royal card background
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                Color.parseColor("#101722"),
                Color.parseColor("#192334"),
                Color.parseColor("#0D121B")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val outerRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(outerRect, 38f, 38f, paint)
        paint.shader = null

        // Double Golden Ornamental Frame
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = Color.parseColor("#F5A623")
        canvas.drawRoundRect(RectF(34f, 34f, width - 34f, height - 34f), 26f, 26f, paint)

        paint.strokeWidth = 2.5f
        paint.color = Color.parseColor("#80F5A623")
        canvas.drawRoundRect(RectF(50f, 50f, width - 50f, height - 50f), 20f, 20f, paint)

        // Geometric Golden Star / Crown Emblem at top center
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F5A623")
        val cx = width / 2f
        val cy = 188f
        val emblemPath = Path().apply {
            moveTo(cx, cy - 68f)
            lineTo(cx + 52f, cy - 16f)
            lineTo(cx + 32f, cy + 52f)
            lineTo(cx - 32f, cy + 52f)
            lineTo(cx - 52f, cy - 16f)
            close()
        }
        canvas.drawPath(emblemPath, paint)

        paint.color = Color.parseColor("#101722")
        canvas.drawCircle(cx, cy, 26f, paint)
        paint.color = Color.parseColor("#F5A623")
        canvas.drawCircle(cx, cy, 11f, paint)

        // Brand Typography (Arabic + English + Phone Number to prove 100% text preservation)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        paint.color = Color.parseColor("#F5A623")
        paint.textSize = 54f
        canvas.drawText("مشتاق جرافكس", cx, 340f, paint)

        paint.color = Color.parseColor("#FFFFFF")
        paint.textSize = 35f
        canvas.drawText("MOSHTAQ GRAFX STUDIO", cx, 396f, paint)

        // Divider bar
        paint.color = Color.parseColor("#E07A5F")
        canvas.drawRoundRect(RectF(cx - 144f, 428f, cx + 144f, 436f), 4f, 4f, paint)

        paint.color = Color.parseColor("#FFE4B5")
        paint.textSize = 43f
        canvas.drawText("عود وكهرمان ملكي فاخر", cx, 508f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#D1D5DB")
        paint.textSize = 27f
        canvas.drawText("تصميم الهويات البصرية • الطباعة التجارية • التغليف", cx, 572f, paint)

        // Contact Badge at bottom
        paint.color = Color.parseColor("#243044")
        canvas.drawRoundRect(RectF(cx - 232f, 620f, cx + 232f, 692f), 36f, 36f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.parseColor("#F5A623")
        canvas.drawRoundRect(RectF(cx - 232f, 620f, cx + 232f, 692f), 36f, 36f, paint)

        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 27f
        paint.color = Color.parseColor("#F5A623")
        canvas.drawText("777908865 • الدرجة الأولى الممتازة", cx, 666f, paint)

        return OriginalDesignAsset(
            bitmap = bmp,
            fileName = "moshtaq_royal_sample.png",
            mimeType = "image/png",
            fileSizeKb = 320L,
            widthPx = width,
            heightPx = height,
            classification = ImageClassification.FLAT_DESIGN_TEXT_LOGO,
            analysisReportAr = "تصميم هوية وملصق تجاري يحتوي على نصوص عربية وإنجليزية وشعار هندسي وأرقام تواصل. تم تفعيل وضع المحافظة القصوى على الأصل لحماية النصوص بنسبة 100%."
        )
    }

    /**
     * Removes uniform background color around the edges while preserving the interior product/logo.
     */
    fun removeBackgroundEdges(source: Bitmap, tolerance: Int = 38): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample corner pixels to detect background color
        val corners = listOf(
            pixels[0],
            pixels[width - 1],
            pixels[(height - 1) * width],
            pixels[(height - 1) * width + (width - 1)]
        )
        val bgR = corners.map { Color.red(it) }.average().roundToInt()
        val bgG = corners.map { Color.green(it) }.average().roundToInt()
        val bgB = corners.map { Color.blue(it) }.average().roundToInt()

        val visited = BooleanArray(width * height)
        val queue = IntArray(width * height)
        var head = 0
        var tail = 0

        fun enqueueIfMatch(x: Int, y: Int) {
            if (x < 0 || x >= width || y < 0 || y >= height) return
            val idx = y * width + x
            if (visited[idx]) return
            val c = pixels[idx]
            val dist = sqrt(
                ((Color.red(c) - bgR) * (Color.red(c) - bgR) +
                        (Color.green(c) - bgG) * (Color.green(c) - bgG) +
                        (Color.blue(c) - bgB) * (Color.blue(c) - bgB)).toDouble()
            )
            if (dist <= tolerance) {
                visited[idx] = true
                queue[tail++] = idx
            }
        }

        for (x in 0 until width) {
            enqueueIfMatch(x, 0)
            enqueueIfMatch(x, height - 1)
        }
        for (y in 0 until height) {
            enqueueIfMatch(0, y)
            enqueueIfMatch(width - 1, y)
        }

        while (head < tail) {
            val idx = queue[head++]
            pixels[idx] = Color.TRANSPARENT
            val x = idx % width
            val y = idx / width
            enqueueIfMatch(x + 1, y)
            enqueueIfMatch(x - 1, y)
            enqueueIfMatch(x, y + 1)
            enqueueIfMatch(x, y - 1)
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Renders the complete composite mockup or product scene.
     * When isInteractivePreview is true, uses a GPU-friendly 768px buffer to prevent HWUI frame drops;
     * when exporting (isInteractivePreview = false), renders at full requested resolution.
     */
    fun renderCompositeMockup(
        originalDesign: Bitmap?,
        aiSceneBackdrop: Bitmap?,
        category: MockupCategory,
        transform: LayerTransformState,
        aspectRatio: StudioAspectRatio,
        imageSize: StudioImageSize,
        drawOriginalLayer: Boolean = true,
        reflectionIntensity: Float = 0.25f,
        ambientLightIntensity: Float = 0.5f,
        isDarkStudioBackdropFallback: Boolean = true,
        isInteractivePreview: Boolean = false
    ): Bitmap {
        val targetCap = if (isInteractivePreview) 600 else min(2048, imageSize.maxDimensionPx)
        val maxDim = min(targetCap, imageSize.maxDimensionPx)
        val ratio = aspectRatio.floatRatio
        val outWidth: Int
        val outHeight: Int
        if (ratio >= 1f) {
            outWidth = maxDim
            outHeight = max(256, (maxDim / ratio).roundToInt())
        } else {
            outHeight = maxDim
            outWidth = max(256, (maxDim * ratio).roundToInt())
        }

        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Draw AI-generated scene backdrop OR procedural luxury studio mockup stage
        if (aiSceneBackdrop != null) {
            drawCenterCropBitmap(canvas, aiSceneBackdrop, outWidth, outHeight, paint)
        } else {
            drawProceduralStudioStage(
                canvas = canvas,
                width = outWidth,
                height = outHeight,
                category = category,
                isDark = isDarkStudioBackdropFallback,
                ambientLight = ambientLightIntensity
            )
        }

        // 2. If original design layer is enabled, composite it with perspective, mask, shadow & reflection
        if (drawOriginalLayer && originalDesign != null) {
            val maskedDesign = applyLayerMask(
                source = originalDesign,
                maskShape = transform.maskShape,
                cornerRadiusRatio = transform.cornerRadiusRatio
            )

            val designW = maskedDesign.width.toFloat()
            val designH = maskedDesign.height.toFloat()
            val fitScale = min(
                (outWidth * transform.scale) / max(1f, designW),
                (outHeight * transform.scale) / max(1f, designH)
            )

            val targetW = designW * fitScale
            val targetH = designH * fitScale

            val centerX = (outWidth / 2f) + (transform.offsetXFraction * outWidth * 0.45f)
            val centerY = (outHeight / 2f) + (transform.offsetYFraction * outHeight * 0.45f)

            val left = centerX - targetW / 2f
            val top = centerY - targetH / 2f
            val right = centerX + targetW / 2f
            val bottom = centerY + targetH / 2f

            // Perspective 4-corner distortion (PolyToPoly)
            val px = transform.perspectiveX * targetW * 0.35f
            val py = transform.perspectiveY * targetH * 0.35f

            val srcPts = floatArrayOf(
                0f, 0f,
                designW, 0f,
                designW, designH,
                0f, designH
            )
            val dstPts = floatArrayOf(
                left + px, top + py,
                right - px, top - py,
                right + px, bottom + py,
                left - px, bottom - py
            )

            val polyMatrix = Matrix()
            polyMatrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)
            if (abs(transform.rotationDegrees) > 0.1f) {
                polyMatrix.postRotate(transform.rotationDegrees, centerX, centerY)
            }

            // 2a. Fast, realistic multi-layer soft ground/contact shadow (no slow CPU BlurMaskFilter)
            if (transform.shadowIntensity > 0.02f) {
                val baseShadowOffset = min(outWidth, outHeight) * 0.022f * transform.shadowIntensity
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                }
                canvas.save()
                if (abs(transform.rotationDegrees) > 0.1f) {
                    canvas.rotate(transform.rotationDegrees, centerX, centerY)
                }
                for (layer in 3 downTo 1) {
                    val spread = layer * 4f * transform.shadowIntensity
                    val layerAlpha = ((transform.shadowIntensity * 52f) / layer).roundToInt().coerceIn(8, 90)
                    shadowPaint.color = Color.argb(layerAlpha, 0, 0, 0)
                    val shadowPath = Path().apply {
                        moveTo(dstPts[0] - spread + baseShadowOffset * 0.5f, dstPts[1] - spread + baseShadowOffset)
                        lineTo(dstPts[2] + spread + baseShadowOffset * 0.5f, dstPts[3] - spread + baseShadowOffset)
                        lineTo(dstPts[4] + spread + baseShadowOffset * 0.5f, dstPts[5] + spread + baseShadowOffset * 1.25f)
                        lineTo(dstPts[6] - spread + baseShadowOffset * 0.5f, dstPts[7] + spread + baseShadowOffset * 1.25f)
                        close()
                    }
                    canvas.drawPath(shadowPath, shadowPaint)
                }
                canvas.restore()
            }

            // 2b. Optional 3D Emboss / Extrusion depth for 3D Logos & Storefront Signs
            if (transform.show3DEmbossEffect || category == MockupCategory.LOGOS_3D_IDENTITY) {
                val depthSteps = 7
                val depthPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    colorFilter = android.graphics.PorterDuffColorFilter(
                        Color.parseColor("#3D2B10"),
                        PorterDuff.Mode.SRC_ATOP
                    )
                }
                for (i in depthSteps downTo 1) {
                    val stepMatrix = Matrix(polyMatrix)
                    stepMatrix.postTranslate(i * 2.2f, i * 2.6f)
                    canvas.drawBitmap(maskedDesign, stepMatrix, depthPaint)
                }
            }

            // 2c. Draw the 100% Untouched Original Design Layer
            val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (transform.opacity * 255).roundToInt().coerceIn(25, 255)
            }
            canvas.drawBitmap(maskedDesign, polyMatrix, layerPaint)

            // 2d. Subtle Surface Specular Lighting Sheen (without obscuring text)
            if (transform.surfaceLightingBlend > 0.02f) {
                val sheenPath = Path().apply {
                    moveTo(dstPts[0], dstPts[1])
                    lineTo(dstPts[2], dstPts[3])
                    lineTo(dstPts[4], dstPts[5])
                    lineTo(dstPts[6], dstPts[7])
                    close()
                }
                val sheenAlpha = (transform.surfaceLightingBlend * 75).roundToInt().coerceIn(0, 95)
                val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        left, top, right, bottom,
                        intArrayOf(
                            Color.argb(sheenAlpha, 255, 250, 235),
                            Color.argb(0, 255, 255, 255),
                            Color.argb((sheenAlpha * 0.6f).roundToInt(), 0, 0, 0)
                        ),
                        floatArrayOf(0f, 0.48f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.save()
                if (abs(transform.rotationDegrees) > 0.1f) {
                    canvas.rotate(transform.rotationDegrees, centerX, centerY)
                }
                canvas.drawPath(sheenPath, sheenPaint)
                canvas.restore()
            }

            // 2e. Floor reflection beneath product/mockup
            if (reflectionIntensity > 0.03f) {
                val reflHeight = targetH * 0.28f
                val reflTop = bottom + 6f
                if (reflTop < outHeight) {
                    val reflPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            centerX, reflTop, centerX, reflTop + reflHeight,
                            Color.argb((reflectionIntensity * 70).roundToInt(), 245, 166, 35),
                            Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawOval(
                        RectF(left + targetW * 0.08f, reflTop, right - targetW * 0.08f, reflTop + reflHeight),
                        reflPaint
                    )
                }
            }
        }

        return output
    }

    private fun applyLayerMask(
        source: Bitmap,
        maskShape: MaskShapeType,
        cornerRadiusRatio: Float
    ): Bitmap {
        if (maskShape == MaskShapeType.NONE) return source

        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        when (maskShape) {
            MaskShapeType.NONE -> {}
            MaskShapeType.RECTANGLE -> {
                canvas.drawRect(rect, paint)
            }
            MaskShapeType.ROUNDED_RECT -> {
                val r = min(w, h) * cornerRadiusRatio.coerceIn(0.01f, 0.35f)
                canvas.drawRoundRect(rect, r, r, paint)
            }
            MaskShapeType.CYLINDER_LABEL -> {
                val r = min(w, h) * 0.12f
                canvas.drawRoundRect(rect, r, r, paint)
            }
            MaskShapeType.CIRCLE -> {
                val radius = min(w, h) / 2f
                canvas.drawCircle(w / 2f, h / 2f, radius, paint)
            }
        }

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)
        paint.xfermode = null

        // For cylinder labels (perfume/oil bottles), add subtle left/right cylindrical shading
        if (maskShape == MaskShapeType.CYLINDER_LABEL) {
            val cylPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), 0f,
                    intArrayOf(
                        Color.argb(65, 0, 0, 0),
                        Color.argb(0, 255, 255, 255),
                        Color.argb(45, 255, 255, 255),
                        Color.argb(0, 255, 255, 255),
                        Color.argb(75, 0, 0, 0)
                    ),
                    floatArrayOf(0f, 0.22f, 0.45f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(rect, cylPaint)
        }

        return out
    }

    private fun drawCenterCropBitmap(canvas: Canvas, src: Bitmap, outW: Int, outH: Int, paint: Paint) {
        val scale = max(outW.toFloat() / src.width, outH.toFloat() / src.height)
        val scaledW = src.width * scale
        val scaledH = src.height * scale
        val dx = (outW - scaledW) / 2f
        val dy = (outH - scaledH) / 2f
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        canvas.drawBitmap(src, matrix, paint)
    }

    /**
     * Draws a realistic studio photography stage with category-specific carrier silhouette
     * (bag handles, bottle neck/cap, storefront frame, podium, etc.) when previewing before/without AI scene.
     */
    private fun drawProceduralStudioStage(
        canvas: Canvas,
        width: Int,
        height: Int,
        category: MockupCategory,
        isDark: Boolean,
        ambientLight: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val w = width.toFloat()
        val h = height.toFloat()

        // Studio background gradient
        val topColor = if (isDark) Color.parseColor("#121620") else Color.parseColor("#F1F4F9")
        val midColor = if (isDark) Color.parseColor("#1D2331") else Color.parseColor("#E2E8F0")
        val floorColor = if (isDark) Color.parseColor("#0D0F14") else Color.parseColor("#CBD5E1")

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(topColor, midColor, floorColor),
            floatArrayOf(0f, 0.68f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, paint)

        // Ambient studio spotlight glow
        val glowAlpha = (ambientLight * 95).roundToInt().coerceIn(20, 140)
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.42f, max(w, h) * 0.55f,
            Color.argb(glowAlpha, 245, 166, 35),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Studio Podium / Horizon Table
        val horizonY = h * 0.74f
        paint.color = if (isDark) Color.parseColor("#161A24") else Color.parseColor("#DCE3EC")
        canvas.drawRect(0f, horizonY, w, h, paint)

        paint.color = Color.argb(70, 245, 166, 35)
        paint.strokeWidth = 2f
        canvas.drawLine(0f, horizonY, w, horizonY, paint)

        // Category-specific physical mockup carrier cues (handles, bottle cap, sign frame)
        val cx = w * 0.5f
        val cy = h * 0.5f
        when (category) {
            MockupCategory.PLASTIC_BAGS, MockupCategory.FABRIC_TOTE_BAGS -> {
                // Draw realistic shopping/tote bag body & handles behind the design placement area
                val bagW = w * 0.56f
                val bagH = h * 0.58f
                val bagRect = RectF(cx - bagW / 2f, cy - bagH * 0.42f, cx + bagW / 2f, cy + bagH * 0.58f)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = max(8f, w * 0.016f)
                paint.color = if (category == MockupCategory.FABRIC_TOTE_BAGS) {
                    Color.parseColor("#D6C7B2")
                } else {
                    Color.parseColor("#E5E7EB")
                }
                val handleW = bagW * 0.32f
                val handleRect = RectF(
                    cx - handleW / 2f,
                    bagRect.top - bagH * 0.22f,
                    cx + handleW / 2f,
                    bagRect.top + bagH * 0.12f
                )
                canvas.drawArc(handleRect, 180f, 180f, false, paint)

                paint.style = Paint.Style.FILL
                paint.color = if (category == MockupCategory.FABRIC_TOTE_BAGS) {
                    Color.parseColor("#EAE0D0")
                } else {
                    Color.parseColor("#262C3A")
                }
                canvas.drawRoundRect(bagRect, 22f, 22f, paint)
            }
            MockupCategory.OIL_PERFUME_LABELS -> {
                // Draw luxury glass perfume/oil bottle with golden spray cap
                val bottleW = w * 0.44f
                val bottleH = h * 0.56f
                val bottleRect = RectF(cx - bottleW / 2f, cy - bottleH * 0.38f, cx + bottleW / 2f, cy + bottleH * 0.62f)
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#2A1B0E")
                canvas.drawRoundRect(bottleRect, 42f, 42f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = Color.parseColor("#F5A623")
                canvas.drawRoundRect(bottleRect, 42f, 42f, paint)

                // Golden Neck & Cap
                paint.style = Paint.Style.FILL
                val neckW = bottleW * 0.26f
                canvas.drawRoundRect(
                    RectF(cx - neckW / 2f, bottleRect.top - bottleH * 0.18f, cx + neckW / 2f, bottleRect.top),
                    10f, 10f, paint
                )
            }
            MockupCategory.SHOP_SIGNS_STOREFRONTS -> {
                // Architectural storefront mounting frame
                val signW = w * 0.76f
                val signH = h * 0.44f
                val signRect = RectF(cx - signW / 2f, cy - signH / 2f, cx + signW / 2f, cy + signH / 2f)
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#1A1F2C")
                canvas.drawRoundRect(signRect, 18f, 18f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 8f
                paint.color = Color.parseColor("#F5A623")
                canvas.drawRoundRect(signRect, 18f, 18f, paint)
            }
            else -> {
                // Sleek studio podium platform beneath the item
                val podW = w * 0.62f
                val podH = h * 0.08f
                val podRect = RectF(cx - podW / 2f, horizonY - podH * 0.3f, cx + podW / 2f, horizonY + podH * 0.7f)
                paint.style = Paint.Style.FILL
                paint.color = if (isDark) Color.parseColor("#232938") else Color.parseColor("#CBD5E1")
                canvas.drawOval(podRect, paint)
            }
        }
    }

    /**
     * Exports the final rendered bitmap to the device's MediaStore gallery AND internal app storage
     * for the project history log, verifying file integrity before returning success.
     */
    fun exportFinalImage(
        context: Context,
        bitmap: Bitmap,
        format: String, // "PNG" or "JPEG"
        projectTitle: String
    ): ExportResult {
        return try {
            val isPng = format.uppercase() == "PNG"
            val ext = if (isPng) "png" else "jpg"
            val mime = if (isPng) "image/png" else "image/jpeg"
            val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val quality = if (isPng) 100 else 95

            val cleanName = projectTitle
                .replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
                .take(30)
                .ifBlank { "moshtaq_grafx" }
            val timestamp = System.currentTimeMillis()
            val fileName = "${cleanName}_${timestamp}.$ext"

            // 1. Save a verified copy in internal app storage for fast Project History reload
            val historyDir = File(context.filesDir, "exported_projects").apply { mkdirs() }
            val internalFile = File(historyDir, fileName)
            FileOutputStream(internalFile).use { out ->
                val ok = bitmap.compress(compressFormat, quality, out)
                out.flush()
                if (!ok) {
                    return ExportResult(
                        success = false,
                        messageAr = "تعذر ضغط الصورة أثناء التصدير."
                    )
                }
            }

            if (!internalFile.exists() || internalFile.length() == 0L) {
                return ExportResult(
                    success = false,
                    messageAr = "الملف الناتج فارغ أو غير صالح. يرجى المحاولة مرة أخرى."
                )
            }

            // 2. Also export to MediaStore so the user can access it directly in their Gallery/Downloads
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MoshtaqGrafx")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val mediaUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (mediaUri != null) {
                resolver.openOutputStream(mediaUri)?.use { outStream ->
                    internalFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(mediaUri, contentValues, null, null)
                }
            }

            val sizeKb = max(1L, internalFile.length() / 1024L)
            ExportResult(
                success = true,
                messageAr = "تم تصدير وحفظ الصورة بنجاح بصيغة $format بأبعاد ${bitmap.width}×${bitmap.height} بكسل (${sizeKb} كيلوبايت) في مجلد Pictures/MoshtaqGrafx.",
                savedFilePath = internalFile.absolutePath,
                widthPx = bitmap.width,
                heightPx = bitmap.height,
                fileSizeKb = sizeKb
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                messageAr = "فشل تنزيل الصورة: ${e.localizedMessage ?: "خطأ في نظام الملفات"}"
            )
        }
    }
}
