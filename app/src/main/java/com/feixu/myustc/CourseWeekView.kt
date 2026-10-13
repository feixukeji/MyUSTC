package com.feixu.myustc

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 自定义课程表视图
 */
class CourseWeekView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var renderBlocks: List<CourseRenderBlock> = emptyList()
    private var showAllCourses: Boolean = false
    private var weekStart: LocalDate = LocalDate.now().with(DayOfWeek.SUNDAY)
    private var zoomScale: Float = 1f
    private val minZoomScale: Float = 0.6f
    private val maxZoomScale: Float = 2.5f
    private val maxTextScale: Float = 1.28f
    private val courseContentTopPadding = 1f
    private val courseContentBottomPadding = 1f
    private val courseContentCenterBias = 0.42f
    private val courseMetaTextScale = 0.8f
    private val courseTitleBackgroundSurfaceBlend = 0.75f
    private val courseTextLineGap = 0f
    private val courseTextItemGap = 1f
    private val courseTextCourseGap = 12f
    private val locationBackgroundHorizontalPadding = 6f
    private val locationBackgroundVerticalPadding = 0f
    private val locationBackgroundCornerRadius = 20f

    private data class CourseRenderBlock(
        val displayCourse: CourseData,
        val sourceCourses: List<CourseData>
    )

    private enum class CourseTextType {
        TITLE,
        BODY,
        META,
        LOCATION;

        fun resolveMetric(title: Float, body: Float, meta: Float): Float = when (this) {
            TITLE -> title
            META -> meta
            BODY,
            LOCATION -> body
        }
    }

    private enum class CourseTextGap {
        ITEM,
        COURSE
    }

    private data class CourseTextItem(
        val type: CourseTextType,
        val lines: List<String>,
        val gapBefore: CourseTextGap = CourseTextGap.ITEM
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titleBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val surfaceColor = MaterialColors.getColor(
        context,
        com.google.android.material.R.attr.colorSurface,
        ContextCompat.getColor(context, R.color.white)
    )
    private val primaryTextColor = ContextCompat.getColor(context, R.color.text_primary)
    private val secondaryTextColor = ContextCompat.getColor(context, R.color.text_secondary)
    private val dayLabels = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    private val headerDateFormatter = DateTimeFormatter.ofPattern("MM/dd")
    private val courseTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val dayHeaderHeight = 64f
    private val dayHeaderBottomGap = 10f
    private val startUnit = 1
    private val endUnit = 13
    private val rowCount = endUnit - startUnit + 1
    private val compactRowHeight = 120f
    private var cellWidth = 0f
    private var cellHeight = 0f
    private var viewportHeight = 0

    private fun currentHeaderHeight(): Float = dayHeaderHeight * zoomScale
    private fun baseHeaderBottomGap(): Float = if (showAllCourses) 0f else dayHeaderBottomGap
    private fun currentHeaderBottomGap(): Float = baseHeaderBottomGap() * currentTextScale()
    private fun currentGridTop(): Float = currentHeaderHeight() + currentHeaderBottomGap()
    private fun currentTextScale(): Float = zoomScale.coerceAtMost(maxTextScale)

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            // 必须在DOWN阶段返回true，后续手势（含双指缩放）事件才能持续分发到当前View
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTouch(e.x, e.y)
            return true
        }
    })

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val oldScale = zoomScale
                zoomScale = (zoomScale * detector.scaleFactor).coerceIn(minZoomScale, maxZoomScale)
                if (zoomScale != oldScale) {
                    requestLayout()
                    invalidate()
                }
                return true
            }
        }
    )

    var onCourseClickListener: ((List<CourseData>) -> Unit)? = null

    init {
        gridPaint.strokeWidth = 1f
        gridPaint.style = Paint.Style.STROKE
        gridPaint.color = ContextCompat.getColor(context, R.color.divider_color)

        maskPaint.color = surfaceColor
    }

    fun setCourses(courses: List<CourseData>, showAll: Boolean) {
        if (showAllCourses != showAll) {
            showAllCourses = showAll
            requestLayout()
        }
        renderBlocks = buildRenderBlocks(courses)
        invalidate()
    }

    private fun buildRenderBlocks(dayCourses: List<CourseData>): List<CourseRenderBlock> {
        val blocks = mutableListOf<CourseRenderBlock>()
        val groupedByDay = dayCourses.groupBy { it.dayOfWeek }

        groupedByDay.forEach { (dayOfWeek, coursesOfDay) ->
            val sorted = coursesOfDay.sortedWith(compareBy<CourseData> { it.startUnit }.thenBy { it.endUnit })

            var groupStart = sorted.first().startUnit
            var groupEnd = sorted.first().endUnit
            val groupCourses = mutableListOf(sorted.first())

            fun flushGroup() {
                val primary = groupCourses.first()
                val startTime = groupCourses.minOf { it.startTime }
                val endTime = groupCourses.maxOf { it.endTime }
                val display = primary.copy(
                    dayOfWeek = dayOfWeek,
                    startUnit = groupStart,
                    endUnit = groupEnd,
                    startTime = startTime,
                    endTime = endTime
                )
                blocks.add(CourseRenderBlock(display, groupCourses.toList()))
            }

            for (i in 1 until sorted.size) {
                val course = sorted[i]
                if (course.startUnit <= groupEnd) {
                    groupCourses.add(course)
                    groupEnd = max(groupEnd, course.endUnit)
                } else {
                    flushGroup()
                    groupCourses.clear()
                    groupCourses.add(course)
                    groupStart = course.startUnit
                    groupEnd = course.endUnit
                }
            }

            flushGroup()
        }

        return blocks.sortedWith(
            compareBy<CourseRenderBlock> { it.displayCourse.dayOfWeek }
                .thenBy { it.displayCourse.startUnit }
                .thenBy { it.displayCourse.endUnit }
        )
    }

    fun setWeekStart(date: LocalDate) {
        this.weekStart = date.with(DayOfWeek.SUNDAY)
        invalidate()
    }

    fun setViewportHeight(height: Int) {
        val newHeight = height.coerceAtLeast(0)
        if (viewportHeight == newHeight) return
        viewportHeight = newHeight
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        cellWidth = (width - paddingLeft - paddingRight) / dayLabels.size.toFloat()

        val resolvedViewportHeight = when {
            viewportHeight > 0 -> viewportHeight
            MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED -> MeasureSpec.getSize(heightMeasureSpec)
            else -> 0
        }
        val availableGridHeight = max(
            0f,
            resolvedViewportHeight.toFloat() - paddingTop - paddingBottom -
                dayHeaderHeight - baseHeaderBottomGap()
        )
        cellHeight = max(compactRowHeight, availableGridHeight / rowCount) * zoomScale

        val totalHeight = currentHeaderHeight() + currentHeaderBottomGap() + cellHeight * rowCount
        setMeasuredDimension(width, totalHeight.toInt() + paddingTop + paddingBottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawDayHeaders(canvas)
        drawGrid(canvas)
        drawCourses(canvas)
    }

    private fun drawDayHeaders(canvas: Canvas) {
        val headerHeight = currentHeaderHeight()
        val textScale = currentTextScale()
        val showDate = !showAllCourses
        val today = LocalDate.now()

        val dayTextSize = 32f * textScale
        val dateTextSize = 24f * textScale
        textPaint.textSize = dayTextSize
        textPaint.textAlign = Paint.Align.CENTER

        for (i in dayLabels.indices) {
            val x = paddingLeft + cellWidth * i + cellWidth / 2
            val date = weekStart.plusDays(i.toLong())
            val isToday = date == today
            textPaint.isFakeBoldText = isToday
            textPaint.color = if (isToday) primaryTextColor else secondaryTextColor

            canvas.drawText(dayLabels[i], x, paddingTop + headerHeight * 0.38f, textPaint)

            if (showDate) {
                textPaint.textSize = dateTextSize
                canvas.drawText(date.format(headerDateFormatter), x, paddingTop + headerHeight * 0.8f, textPaint)
            }
            textPaint.textSize = dayTextSize
            textPaint.isFakeBoldText = false
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val gridTop = currentGridTop()
        val thickPaint = Paint(gridPaint).apply {
            strokeWidth = 2.5f
        }
        for (i in 0..rowCount) {
            val y = gridTop + cellHeight * i
            val boundaryAfterUnit = startUnit + i - 1
            val paintToUse = if (boundaryAfterUnit == 5 || boundaryAfterUnit == 10) thickPaint else gridPaint
            canvas.drawLine(paddingLeft.toFloat(), y, width.toFloat() - paddingRight, y, paintToUse)
        }

        for (i in 0..dayLabels.size) {
            val x = paddingLeft + cellWidth * i
            canvas.drawLine(x, gridTop, x, gridTop + cellHeight * rowCount, gridPaint)
        }
    }

    private fun drawCourses(canvas: Canvas) {
        for (block in renderBlocks) {
            drawCourse(canvas, block)
        }
    }

    private fun getColumnIndex(dayOfWeek: Int): Int = dayOfWeek % dayLabels.size

    private fun isCourseOngoing(course: CourseData): Boolean {
        val today = LocalDate.now()
        val courseDate = weekStart.plusDays(getColumnIndex(course.dayOfWeek).toLong())
        if (today != courseDate) return false
        val now = LocalTime.now()
        return !now.isBefore(course.startTime) && !now.isAfter(course.endTime)
    }

    private fun drawCourse(canvas: Canvas, block: CourseRenderBlock) {
        val gridTop = currentGridTop()
        val textScale = currentTextScale()
        val course = block.displayCourse
        val columnIndex = getColumnIndex(course.dayOfWeek)
        val x = paddingLeft + cellWidth * columnIndex
        val y = gridTop + cellHeight * (course.startUnit - startUnit)
        val height = cellHeight * (course.endUnit - course.startUnit + 1)

        val rect = RectF(x + 4f, y + 4f, x + cellWidth - 4f, y + height - 4f)
        val cornerRadius = 12f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, maskPaint)

        paint.style = Paint.Style.FILL
        paint.color = course.color
        paint.alpha = 100
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // 边框最后绘制，避免课程名称的全宽底色盖住边框。
        val borderStrokeWidth: Float
        val borderAlpha: Int
        if (!showAllCourses && isCourseOngoing(course)) {
            borderStrokeWidth = 5f
            borderAlpha = 255
        } else {
            borderStrokeWidth = 2f
            borderAlpha = 180
        }

        val contentLeft = rect.left + 10f
        val contentRight = rect.right - 10f
        val maxWidth = contentRight - contentLeft
        val locationMaxWidth = max(0f, maxWidth - locationBackgroundHorizontalPadding * 2f)

        val baseNameSize = 34f * textScale
        val baseOtherSize = 28f * textScale
        val baseTimeSize = 34f * textScale
        val minNameSize = 12f
        val minOtherSize = 10f
        val minTimeSize = 10f

        var scale = 1f
        var lineGap = courseTextLineGap
        var itemGap = courseTextItemGap
        var courseGap = courseTextCourseGap
        var nameSize = baseNameSize
        var otherSize = baseOtherSize
        var metaSize = baseOtherSize * courseMetaTextScale
        var timeSize = baseTimeSize
        var nameLineHeight = 0f
        var otherLineHeight: Float
        var metaLineHeight: Float
        var textItems = emptyList<CourseTextItem>()
        var totalHeight = 0f

        val startTimeStr = course.startTime.format(courseTimeFormatter)
        val endTimeStr = course.endTime.format(courseTimeFormatter)

        var attempts = 0
        while (attempts < 40) {
            nameSize = max(minNameSize, baseNameSize * scale)
            otherSize = max(minOtherSize, baseOtherSize * scale)
            metaSize = max(minOtherSize, otherSize * courseMetaTextScale)
            timeSize = max(minTimeSize, baseTimeSize * scale)

            textPaint.textSize = timeSize
            val timeHeight = textPaint.fontMetrics.run { bottom - top }

            val contentTop = rect.top + timeHeight + courseContentTopPadding
            val contentBottom = rect.bottom - timeHeight - courseContentBottomPadding
            val contentHeight = max(0f, contentBottom - contentTop)

            textPaint.textSize = nameSize
            textPaint.isFakeBoldText = true
            nameLineHeight = textPaint.visualLineHeight()

            textPaint.textSize = otherSize
            textPaint.isFakeBoldText = false
            otherLineHeight = textPaint.visualLineHeight()
            textPaint.textSize = metaSize
            metaLineHeight = textPaint.visualLineHeight()
            lineGap = courseTextLineGap * scale
            itemGap = courseTextItemGap * scale
            courseGap = courseTextCourseGap * scale

            textItems = buildCourseTextItems(
                sourceCourses = block.sourceCourses,
                maxWidth = maxWidth,
                locationMaxWidth = locationMaxWidth,
                titleSize = nameSize,
                bodySize = otherSize,
                metaSize = metaSize
            )
            totalHeight = calculateTextItemsHeight(
                items = textItems,
                titleLineHeight = nameLineHeight,
                bodyLineHeight = otherLineHeight,
                metaLineHeight = metaLineHeight,
                lineGap = lineGap,
                itemGap = itemGap,
                courseGap = courseGap
            )

            if (totalHeight <= contentHeight || (nameSize <= minNameSize && otherSize <= minOtherSize && timeSize <= minTimeSize)) {
                textPaint.textSize = timeSize
                while (timeSize > minTimeSize && (textPaint.measureText(startTimeStr) > rect.width() * 0.5f || textPaint.measureText(endTimeStr) > rect.width() * 0.5f)) {
                    timeSize = max(minTimeSize, timeSize - 1f)
                    textPaint.textSize = timeSize
                }
                break
            }

            scale *= 0.92f
            attempts++
        }

        textPaint.textSize = timeSize
        textPaint.color = secondaryTextColor
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(startTimeStr, rect.left + 6f, rect.top + timeSize, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(endTimeStr, rect.right - 6f, rect.bottom - 8f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        val timeHeight = textPaint.fontMetrics.run { bottom - top }
        val contentTop = rect.top + timeHeight + courseContentTopPadding
        val contentBottom = rect.bottom - timeHeight - courseContentBottomPadding
        val contentHeight = max(0f, contentBottom - contentTop)

        val textTop = if (totalHeight < contentHeight) {
            contentTop + (contentHeight - totalHeight) * courseContentCenterBias
        } else {
            contentTop
        }

        val titleBackgroundColor = blendColor(course.color, surfaceColor, courseTitleBackgroundSurfaceBlend)
        drawCourseTextItems(
            canvas = canvas,
            items = textItems,
            contentLeft = contentLeft,
            startTop = textTop,
            titleSize = nameSize,
            bodySize = otherSize,
            metaSize = metaSize,
            lineGap = lineGap,
            itemGap = itemGap,
            courseGap = courseGap,
            titleBackgroundLeft = rect.left,
            titleBackgroundRight = rect.right,
            titleBackgroundColor = titleBackgroundColor
        )

        drawCourseBorder(canvas, rect, cornerRadius, course.color, borderStrokeWidth, borderAlpha)
    }

    private fun drawCourseBorder(
        canvas: Canvas,
        rect: RectF,
        cornerRadius: Float,
        color: Int,
        strokeWidth: Float,
        alpha: Int
    ) {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = strokeWidth
        paint.alpha = alpha
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
    }

    private fun drawTitleBackground(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        color: Int
    ) {
        if (right <= left || bottom <= top) return
        titleBackgroundPaint.color = color
        canvas.drawRect(left, top, right, bottom, titleBackgroundPaint)
    }

    private fun drawLocationBackground(
        canvas: Canvas,
        lines: List<String>,
        left: Float,
        firstBaseline: Float,
        baselineStep: Float,
        color: Int
    ) {
        if (lines.isEmpty()) return
        val metrics = textPaint.fontMetrics
        val maxLineWidth = lines.maxOf { textPaint.measureText(it) }
        val right = left + maxLineWidth + locationBackgroundHorizontalPadding
        val rect = RectF(
            left - locationBackgroundHorizontalPadding,
            firstBaseline + metrics.ascent - locationBackgroundVerticalPadding,
            right,
            firstBaseline + ((lines.size - 1) * baselineStep) + metrics.descent + locationBackgroundVerticalPadding
        )
        titleBackgroundPaint.color = color
        canvas.drawRoundRect(rect, locationBackgroundCornerRadius, locationBackgroundCornerRadius, titleBackgroundPaint)
    }

    private fun blendColor(source: Int, target: Int, targetRatio: Float): Int {
        val ratio = targetRatio.coerceIn(0f, 1f)
        val inverseRatio = 1f - ratio
        return Color.argb(
            255,
            (Color.red(source) * inverseRatio + Color.red(target) * ratio).roundToInt(),
            (Color.green(source) * inverseRatio + Color.green(target) * ratio).roundToInt(),
            (Color.blue(source) * inverseRatio + Color.blue(target) * ratio).roundToInt()
        )
    }

    private fun buildCourseTextItems(
        sourceCourses: List<CourseData>,
        maxWidth: Float,
        locationMaxWidth: Float,
        titleSize: Float,
        bodySize: Float,
        metaSize: Float
    ): List<CourseTextItem> {
        return sourceCourses
            .sortedWith(compareBy<CourseData> { it.startUnit }.thenBy { it.endUnit }.thenBy { it.name })
            .flatMapIndexed { index, course ->
                buildCourseTextItemsForCourse(
                    course = course,
                    maxWidth = maxWidth,
                    locationMaxWidth = locationMaxWidth,
                    titleSize = titleSize,
                    bodySize = bodySize,
                    metaSize = metaSize,
                    titleGap = if (index == 0) CourseTextGap.ITEM else CourseTextGap.COURSE
                )
            }
    }

    private fun buildCourseTextItemsForCourse(
        course: CourseData,
        maxWidth: Float,
        locationMaxWidth: Float,
        titleSize: Float,
        bodySize: Float,
        metaSize: Float,
        titleGap: CourseTextGap
    ): List<CourseTextItem> {
        return listOfNotNull(
            courseTextItem(
                type = CourseTextType.TITLE,
                rawText = course.name,
                maxWidth = maxWidth,
                textSize = titleSize,
                fakeBold = true,
                gapBefore = titleGap
            ),
            courseTextItem(
                type = CourseTextType.BODY,
                rawText = course.teacher,
                maxWidth = maxWidth,
                textSize = bodySize,
                fakeBold = false
            ),
            if (showAllCourses) {
                courseTextItem(
                    type = CourseTextType.META,
                    rawText = course.simplifiedWeeksWithSuffixText(),
                    maxWidth = maxWidth,
                    textSize = metaSize,
                    fakeBold = false
                )
            } else {
                null
            },
            courseTextItem(
                type = CourseTextType.META,
                rawText = "${course.unitRangeText()}节",
                maxWidth = maxWidth,
                textSize = metaSize,
                fakeBold = false
            ),
            courseTextItem(
                type = CourseTextType.LOCATION,
                rawText = course.location,
                maxWidth = locationMaxWidth,
                textSize = bodySize,
                fakeBold = false
            )
        )
    }

    private fun courseTextItem(
        type: CourseTextType,
        rawText: String,
        maxWidth: Float,
        textSize: Float,
        fakeBold: Boolean,
        gapBefore: CourseTextGap = CourseTextGap.ITEM
    ): CourseTextItem? {
        val text = rawText.trim()
        if (text.isEmpty()) return null

        textPaint.textSize = textSize
        textPaint.isFakeBoldText = fakeBold
        val lines = text.split('\n').flatMap { wrapTextAll(it, maxWidth, textPaint) }
        return lines.takeIf { it.isNotEmpty() }?.let { CourseTextItem(type, it, gapBefore) }
    }

    private fun calculateTextItemsHeight(
        items: List<CourseTextItem>,
        titleLineHeight: Float,
        bodyLineHeight: Float,
        metaLineHeight: Float,
        lineGap: Float,
        itemGap: Float,
        courseGap: Float
    ): Float {
        if (items.isEmpty()) return 0f
        var total = 0f
        items.forEachIndexed { index, item ->
            val lineHeight = item.type.resolveMetric(titleLineHeight, bodyLineHeight, metaLineHeight)
            total += gapBeforeItem(items, index, itemGap, courseGap)
            total += item.lines.size * lineHeight
            total += max(0, item.lines.size - 1) * lineGap
        }
        return total
    }

    private fun gapBeforeItem(
        items: List<CourseTextItem>,
        index: Int,
        itemGap: Float,
        courseGap: Float
    ): Float {
        if (index == 0) return 0f
        return when (items[index].gapBefore) {
            CourseTextGap.ITEM -> itemGap
            CourseTextGap.COURSE -> courseGap
        }
    }

    private fun drawCourseTextItems(
        canvas: Canvas,
        items: List<CourseTextItem>,
        contentLeft: Float,
        startTop: Float,
        titleSize: Float,
        bodySize: Float,
        metaSize: Float,
        lineGap: Float,
        itemGap: Float,
        courseGap: Float,
        titleBackgroundLeft: Float,
        titleBackgroundRight: Float,
        titleBackgroundColor: Int
    ) {
        var top = startTop
        items.forEachIndexed { itemIndex, item ->
            top += gapBeforeItem(items, itemIndex, itemGap, courseGap)
            setCourseTextPaint(item.type, titleSize, bodySize, metaSize)
            val metrics = textPaint.fontMetrics
            val lineHeight = textPaint.visualLineHeight()
            val itemHeight = item.lines.size * lineHeight + max(0, item.lines.size - 1) * lineGap

            when (item.type) {
                CourseTextType.TITLE -> {
                    drawTitleBackground(canvas, titleBackgroundLeft, titleBackgroundRight, top, top + itemHeight, titleBackgroundColor)
                }
                CourseTextType.LOCATION -> {
                    drawLocationBackground(canvas, item.lines, contentLeft, top - metrics.ascent, lineHeight + lineGap, titleBackgroundColor)
                }
                else -> Unit
            }

            var baseline = top - metrics.ascent
            item.lines.forEachIndexed { lineIndex, line ->
                canvas.drawText(line, contentLeft, baseline, textPaint)
                baseline += lineHeight
                if (lineIndex != item.lines.lastIndex) baseline += lineGap
            }
            top += itemHeight
        }

        textPaint.isFakeBoldText = false
    }

    private fun Paint.visualLineHeight(): Float {
        return fontMetrics.run { descent - ascent }
    }

    private fun setCourseTextPaint(
        type: CourseTextType,
        titleSize: Float,
        bodySize: Float,
        metaSize: Float
    ) {
        textPaint.textSize = type.resolveMetric(titleSize, bodySize, metaSize)
        textPaint.isFakeBoldText = type == CourseTextType.TITLE
        textPaint.color = if (type == CourseTextType.TITLE) primaryTextColor else secondaryTextColor
    }

    private fun wrapTextAll(
        text: String,
        maxWidth: Float,
        paint: Paint
    ): List<String> {
        if (text.isEmpty() || maxWidth <= 0f) return emptyList()
        val lines = mutableListOf<String>()
        var remaining = text
        while (remaining.isNotEmpty()) {
            val (lineText, rest) = breakLine(remaining, maxWidth, paint)
            lines.add(lineText)
            remaining = rest
        }
        return lines
    }

    private fun breakLine(text: String, maxWidth: Float, paint: Paint): Pair<String, String> {
        if (paint.measureText(text) <= maxWidth) return text to ""
        var start = 0
        var low = 1
        var high = text.length
        while (low <= high) {
            val mid = (low + high) / 2
            val sub = text.substring(0, mid)
            if (paint.measureText(sub) <= maxWidth) {
                start = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        val end = max(1, start)
        val lineText = text.substring(0, end)
        val rest = text.substring(end)
        return lineText to rest
    }

    private fun handleTouch(x: Float, y: Float) {
        val gridTop = currentGridTop()
        for (block in renderBlocks) {
            val course = block.displayCourse
            val columnIndex = getColumnIndex(course.dayOfWeek)
            val courseX = paddingLeft + cellWidth * columnIndex
            val courseY = gridTop + cellHeight * (course.startUnit - startUnit)
            val height = cellHeight * (course.endUnit - course.startUnit + 1)

            if (x >= courseX && x <= courseX + cellWidth &&
                y >= courseY && y <= courseY + height
            ) {
                onCourseClickListener?.invoke(block.sourceCourses)
                return
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return super.dispatchTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (event.pointerCount > 1 || scaleGestureDetector.isInProgress) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        val handledByGesture = gestureDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            return true
        }
        return handledByGesture || super.onTouchEvent(event)
    }
}
