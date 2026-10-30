package com.example.model

import android.graphics.Bitmap

enum class StudioTab(val titleAr: String) {
    MOCKUP_STUDIO("استوديو الموك أب"),
    PRODUCT_ENHANCE("تحسين المنتجات"),
    AD_BACKGROUNDS("الخلفيات الإعلانية"),
    AI_CONSULTANT("المحلل والمستشار"),
    PROJECT_HISTORY("سجل الأعمال")
}

enum class ProcessingMode(val titleAr: String, val subtitleAr: String) {
    STRICT_PRESERVATION(
        "الوضع الأول: المحافظة القصوى على الأصل",
        "يدمج تصميمك الأصلي كطبقة مستقلة فوق مشهد الموك أب مع المنظور والظلال دون تغيير أي حرف أو لون أو شعار بنسبة 100%."
    ),
    AI_ENHANCEMENT(
        "الوضع الثاني: التحسين بالذكاء الاصطناعي",
        "يولّد أو يحسّن المشهد بالذكاء الاصطناعي مع الحفاظ على التفاصيل قدر الإمكان وإمكانية تثبيت الملصق الأصلي فوق النتيجة."
    )
}

enum class ImageClassification(val titleAr: String, val adviceAr: String) {
    FLAT_DESIGN_TEXT_LOGO(
        "1. تصميم مسطح يحتوي على نصوص وشعارات",
        "يُنصح باستخدام وضع المحافظة القصوى على الأصل لتركيب التصميم كطبقة مستقلة ومنع أي تحريف للنصوص."
    ),
    REAL_PRODUCT_PHOTO(
        "2. صورة منتج حقيقي",
        "مناسب لتحسين الإضاءة والظلال والخلفية التجارية مع الحفاظ على أبعاد المنتج وملصقه الأصلي."
    ),
    PACKAGING_COMMERCIAL_LABEL(
        "3. عبوة أو ملصق تجاري",
        "احرص على تفعيل حماية النصوص وتفاصيل العبوة، واستخدم دمج الطبقات للحفاظ على البيانات المطبوعة."
    ),
    SHOP_SIGN_PRINTED_AD(
        "4. لوحة محل أو إعلان مطبوع",
        "يُركّب التصميم على واجهة المحل أو اللوحة الإعلانية باستخدام التحويل الهندسي والمنظور دون المساس بأرقام الهواتف والشعار."
    ),
    BRAND_IDENTITY_LOGO(
        "5. هوية بصرية أو شعار",
        "مثالي للعرض ثلاثي الأبعاد والدمج على بطاقات الأعمال والمطبوعات مع حماية الألوان الأصلية للشعار."
    ),
    PORTRAIT_PEOPLE(
        "6. صورة تحتوي على أشخاص",
        "تم تفعيل حماية الوجوه وملامح الأشخاص تلقائيًا لمنع أي تغيير في الملامح."
    ),
    COMPOSITE_MULTI_ELEMENT(
        "7. تصميم مركب يحتوي على عدة عناصر",
        "يُفضّل معالجة الإضاءة والخلفية المحيطة فقط مع الحفاظ على ترتيب العناصر وتكوين التصميم الأصلي."
    )
}

enum class MockupCategory(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val basePromptEn: String,
    val defaultScale: Float,
    val defaultPerspectiveX: Float,
    val defaultPerspectiveY: Float,
    val maskShape: MaskShapeType
) {
    PLASTIC_BAGS(
        id = "plastic_bags",
        titleAr = "موك أب أكياس بلاستيكية",
        descriptionAr = "أكياس تسوق بلاستيكية لامعة ومطفية معلقة أو موضوعة على منصة عرض",
        basePromptEn = "A blank unbranded glossy white and dark luxury die-cut plastic shopping bag standing upright on a sleek studio pedestal, realistic plastic creases and soft studio reflections, completely blank center surface ready for design placement, no text, no logos",
        defaultScale = 0.46f,
        defaultPerspectiveX = 0f,
        defaultPerspectiveY = 0.04f,
        maskShape = MaskShapeType.ROUNDED_RECT
    ),
    FABRIC_TOTE_BAGS(
        id = "fabric_bags",
        titleAr = "موك أب أكياس قماشية",
        descriptionAr = "أكياس قماشية (Tote Bags) بملامس قطنية وكتانية طبيعية واقعية",
        basePromptEn = "A blank unbranded natural canvas fabric tote bag resting on a warm wooden and marble studio table, realistic woven cotton texture, soft natural daylight shadows, completely blank front area, no text, no logos",
        defaultScale = 0.44f,
        defaultPerspectiveX = 0.02f,
        defaultPerspectiveY = 0.03f,
        maskShape = MaskShapeType.ROUNDED_RECT
    ),
    PRODUCT_BOXES_CONTAINERS(
        id = "product_boxes",
        titleAr = "موك أب علب وعبوات المنتجات",
        descriptionAr = "علب كرتونية فاخرة وعبوات تغليف تجارية بزوايا احترافية",
        basePromptEn = "A blank unbranded premium matte cardboard product packaging box standing on a luxury dark slate studio podium with warm golden rim light, completely blank front face, photorealistic commercial packaging photography, no text, no logos",
        defaultScale = 0.48f,
        defaultPerspectiveX = -0.06f,
        defaultPerspectiveY = 0.04f,
        maskShape = MaskShapeType.RECTANGLE
    ),
    OIL_PERFUME_LABELS(
        id = "oil_perfume",
        titleAr = "موك أب ملصقات الزيوت والعطور",
        descriptionAr = "زجاجات عطور فاخرة وعبوات زيوت طبيعية مع انعكاسات زجاجية",
        basePromptEn = "A luxury amber glass perfume and essential oil bottle with golden cap standing on a black reflective marble surface with warm dramatic backlighting, smooth blank front label area on the bottle, photorealistic commercial shot, no text, no logos",
        defaultScale = 0.36f,
        defaultPerspectiveX = 0f,
        defaultPerspectiveY = 0.02f,
        maskShape = MaskShapeType.CYLINDER_LABEL
    ),
    SPICES_HERBS_POUCHES(
        id = "spices_pouches",
        titleAr = "موك أب أكياس وعبوات العطارة والتوابل",
        descriptionAr = "أكياس كرافت وعبوات عطارة وبن وتوابل واقعية",
        basePromptEn = "A blank unbranded stand-up kraft paper and matte foil pouch bag for spices and coffee on a rustic dark stone surface surrounded by subtle warm studio light, completely blank front face, no text, no logos",
        defaultScale = 0.44f,
        defaultPerspectiveX = 0f,
        defaultPerspectiveY = 0.05f,
        maskShape = MaskShapeType.ROUNDED_RECT
    ),
    BUSINESS_CARDS(
        id = "business_cards",
        titleAr = "موك أب بطاقات الأعمال",
        descriptionAr = "كروت شخصية فاخرة على أسطح مكتبية رخامية أو خشبية",
        basePromptEn = "A flat blank unbranded thick luxury textured business card resting on a dark architectural slate and brushed brass desk surface with soft dramatic shadows, top-down angled studio view, completely blank card surface, no text, no logos",
        defaultScale = 0.58f,
        defaultPerspectiveX = 0.08f,
        defaultPerspectiveY = -0.06f,
        maskShape = MaskShapeType.ROUNDED_RECT
    ),
    INVOICES_PRINTABLES(
        id = "invoices_print",
        titleAr = "موك أب الفواتير والمطبوعات",
        descriptionAr = "ورق رسمي، فواتير تجارية، وسندات مكتبية احترافية",
        basePromptEn = "A blank unbranded A4 corporate letterhead invoice paper sheet on a luxury executive dark leather and walnut desk next to a metallic pen, clean flat blank paper surface, studio lighting, no text, no logos",
        defaultScale = 0.56f,
        defaultPerspectiveX = 0.04f,
        defaultPerspectiveY = -0.04f,
        maskShape = MaskShapeType.RECTANGLE
    ),
    BROCHURES_FLYERS(
        id = "brochures_flyers",
        titleAr = "موك أب البروشورات والمنشورات",
        descriptionAr = "فلايرات إعلانية، مطويات، وكتيبات دعائية",
        basePromptEn = "A blank unbranded glossy promotional flyer and brochure resting on a modern minimal dark concrete podium with soft artistic shadow, completely blank front cover, commercial print studio photography, no text, no logos",
        defaultScale = 0.54f,
        defaultPerspectiveX = -0.05f,
        defaultPerspectiveY = 0.05f,
        maskShape = MaskShapeType.RECTANGLE
    ),
    SHOP_SIGNS_STOREFRONTS(
        id = "shop_signs",
        titleAr = "موك أب لوحات المحلات والواجهات",
        descriptionAr = "لوحات واجهات تجارية مضيئة ولافتات طرق ومول تجاري",
        basePromptEn = "A modern luxury commercial storefront facade at dusk with a large clean blank rectangular illuminated signboard frame mounted above the glass entrance, warm architectural lighting, completely blank sign board ready for graphic overlay, no text, no logos",
        defaultScale = 0.68f,
        defaultPerspectiveX = 0.05f,
        defaultPerspectiveY = 0.02f,
        maskShape = MaskShapeType.RECTANGLE
    ),
    LOGOS_3D_IDENTITY(
        id = "logos_3d",
        titleAr = "موك أب الشعارات والهويات البصرية",
        descriptionAr = "جداريات استوديو فاخرة وأسطح رخامية ومعدنية لإبراز الشعارات",
        basePromptEn = "A luxury dark brushed charcoal and gold-lit corporate reception wall backdrop with subtle 3D depth and spotlight illumination in the center, completely blank center wall, photorealistic architectural render, no text, no logos",
        defaultScale = 0.52f,
        defaultPerspectiveX = 0f,
        defaultPerspectiveY = 0f,
        maskShape = MaskShapeType.NONE
    ),
    CUSTOM_GENERAL_MOCKUP(
        id = "custom_general",
        titleAr = "موك أب عام حسب النشاط",
        descriptionAr = "مشهد مخصص يحدده المستخدم حسب نشاط العميل التجاري",
        basePromptEn = "A versatile luxury commercial presentation podium with clean blank display surface in a professional photography studio, dramatic rim lighting, no text, no logos",
        defaultScale = 0.50f,
        defaultPerspectiveX = 0f,
        defaultPerspectiveY = 0f,
        maskShape = MaskShapeType.ROUNDED_RECT
    )
}

enum class MaskShapeType(val titleAr: String) {
    NONE("بدون قناع (كامل مع الشفافية)"),
    RECTANGLE("مستطيل حاد (مطبوعات ولوحات)"),
    ROUNDED_RECT("مستطيل بحواف ناعمة (أكياس وبطاقات)"),
    CYLINDER_LABEL("قناع أسطواني منحني (زجاجات وعبوات)"),
    CIRCLE("قناع دائري (استيكرات وأختام)")
}

enum class StudioAspectRatio(
    val label: String,
    val titleAr: String,
    val widthRatio: Float,
    val heightRatio: Float
) {
    RATIO_1_1("1:1", "مربع (1:1) - منشورات ومتاجر", 1f, 1f),
    RATIO_2_3("2:3", "عمودي كلاسيكي (2:3) - ملصقات", 2f, 3f),
    RATIO_3_2("3:2", "أفقي كلاسيكي (3:2) - مطبوعات", 3f, 2f),
    RATIO_3_4("3:4", "عمودي متوسط (3:4) - إعلانات", 3f, 4f),
    RATIO_4_3("4:3", "أفقي متوسط (4:3) - عروض", 4f, 3f),
    RATIO_9_16("9:16", "قصص وريلز (9:16)", 9f, 16f),
    RATIO_16_9("16:9", "شاشة عريضة ولوحات (16:9)", 16f, 9f),
    RATIO_21_9("21:9", "بانورامي سينمائي ولوحات واجهات (21:9)", 21f, 9f);

    val floatRatio: Float get() = widthRatio / heightRatio
}

enum class StudioImageSize(val label: String, val titleAr: String, val maxDimensionPx: Int) {
    SIZE_1K("1K", "دقة قياسية سريعة (1K - 1024px)", 1024),
    SIZE_2K("2K", "دقة احترافية عالية (2K - 2048px)", 2048),
    SIZE_4K("4K", "دقة طباعة واستوديو فائقة (4K - 4096px)", 4096)
}

enum class AiImageModelChoice(val modelId: String, val titleAr: String, val subtitleAr: String) {
    NANO_BANANA_2_1(
        "gemini-nano-banana-2.1",
        "Gemini Nano Banana 2.1 (سريع)",
        "مثالي للتوليد السريع للمشاهد والخلفيات والتعديلات الفورية"
    ),
    PRO_IMAGE_3(
        "gemini-3-pro-image",
        "Gemini 3 Pro Image (جودة استوديو 1K/2K/4K)",
        "أعلى دقة تفاصيل وإضاءة استوديو احترافية للإنتاج التجاري"
    )
}

enum class LightingPreset(val titleAr: String, val promptEn: String, val highlightAlpha: Float) {
    STUDIO_SOFT("إضاءة استوديو ناعمة متوازنة", "soft balanced commercial studio lighting", 0.12f),
    LUXURY_GOLD_RIM("إضاءة حواف ذهبية دافئة فاخرة", "dramatic warm golden rim lighting and deep luxury shadows", 0.22f),
    NATURAL_DAYLIGHT("ضوء نهار طبيعي مشرق", "bright natural window daylight with soft organic shadows", 0.16f),
    HIGH_CONTRAST_DRAMATIC("إضاءة درامية عالية التباين", "high contrast cinematic spotlight with crisp reflections", 0.26f)
}

enum class PerspectivePreset(val titleAr: String, val promptEn: String, val skewX: Float, val skewY: Float) {
    FRONT_DIRECT("أمامي مباشر (مسطح)", "straight-on front studio view", 0f, 0f),
    ANGLED_3D_RIGHT("زاوية ثلاثية الأبعاد (يمين)", "three-quarter angled 3D perspective view from right", -0.08f, 0.04f),
    ANGLED_3D_LEFT("زاوية ثلاثية الأبعاد (يسار)", "three-quarter angled 3D perspective view from left", 0.08f, 0.04f),
    TOP_DOWN_FLATLAY("تصوير علوي مائل (Flat Lay)", "elevated top-down 45-degree flat lay studio angle", 0f, -0.08f)
}

data class OriginalDesignAsset(
    val bitmap: Bitmap,
    val fileName: String,
    val mimeType: String,
    val fileSizeKb: Long,
    val widthPx: Int,
    val heightPx: Int,
    val classification: ImageClassification = ImageClassification.FLAT_DESIGN_TEXT_LOGO,
    val analysisReportAr: String? = null
)

data class IdentityProtectionConfig(
    val protectTexts: Boolean = true,
    val protectLogo: Boolean = true,
    val protectColors: Boolean = true,
    val protectDimensions: Boolean = true,
    val protectFaces: Boolean = true,
    val protectPackagingDetails: Boolean = true,
    val forbidNewText: Boolean = true,
    val forbidWatermarks: Boolean = true
) {
    fun buildStrictPromptGuardrailsEn(): String {
        val rules = mutableListOf<String>()
        if (forbidNewText) {
            rules.add("CRITICAL: Do NOT render, write, or invent any Arabic or English text, letters, numbers, typography, or labels anywhere in the generated image.")
        }
        if (forbidWatermarks) {
            rules.add("Do NOT add any logos, brand marks, watermarks, stamps, or signatures.")
        }
        if (protectColors) {
            rules.add("Maintain neutral, color-accurate studio lighting that does not distort or tint the original brand color palette.")
        }
        if (protectDimensions) {
            rules.add("Keep realistic geometric proportions without warping or stretching the subject.")
        }
        if (protectFaces) {
            rules.add("Never alter, morph, or generate artificial human facial features.")
        }
        if (protectPackagingDetails) {
            rules.add("Keep the surface clean and unobstructed so original packaging details remain 100% intact.")
        }
        return rules.joinToString(" ")
    }
}

data class LayerTransformState(
    val offsetXFraction: Float = 0f,
    val offsetYFraction: Float = 0f,
    val scale: Float = 0.50f,
    val rotationDegrees: Float = 0f,
    val perspectiveX: Float = 0f,
    val perspectiveY: Float = 0f,
    val opacity: Float = 1.0f,
    val shadowIntensity: Float = 0.45f,
    val surfaceLightingBlend: Float = 0.18f,
    val maskShape: MaskShapeType = MaskShapeType.ROUNDED_RECT,
    val cornerRadiusRatio: Float = 0.06f,
    val show3DEmbossEffect: Boolean = false
)

data class ProductEnhancementConfig(
    val backgroundStyleAr: String = "رخام أسود فاخر مع إضاءة ذهبية",
    val backgroundStylePromptEn: String = "luxury black marble podium with warm golden bokeh lighting in background",
    val ambientLightingLevel: Float = 0.65f,
    val realisticShadowDepth: Float = 0.55f,
    val surfaceReflectionIntensity: Float = 0.35f,
    val viewingAngleAr: String = "زاوية أمامية احترافية (Eye-Level)",
    val luxurySceneEnabled: Boolean = true,
    val preserveOriginalLabelLayer: Boolean = true,
    val customChangesPromptAr: String = ""
)

data class AdBackgroundConfig(
    val businessTypeAr: String = "العطور ومستحضرات التجميل الفاخرة",
    val productTypeAr: String = "زجاجة عطر / عبوة تجميل",
    val colorVisualStyleAr: String = "ذهبي ملكي وأسود فحمي",
    val isDarkPosture: Boolean = true,
    val displayStyleAr: String = "منصة عرض استوديو ثلاثية الأبعاد (3D Podium)",
    val textSafeZoneAr: String = "مساحة فارغة في الأعلى والجانب لكتابة النصوص في برنامج التصميم"
)
