package com.danielebonaldo.dashboard.clockcompassdial

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.NonCancellable.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val StopwatchPillHeightRatio = 0.6f
private const val StopwatchPillWidthRatio = 1.7f
private const val TransitionDurationMs = 600

@Composable
fun MorphingDial(uiState: UiState) {
    val transition = updateTransition(targetState = uiState, label = "DialTransition")

    val outerBackgroundColor by transition.animateColor(
        transitionSpec = { tween(TransitionDurationMs) },
        label = "FaceColor"
    ) { state ->
        when (state) {
            is UiState.Clock -> Color(0xFFCCCCCC)
            is UiState.Stopwatch -> Color(0xFFFFDD00)
            is UiState.Compass -> Color(0xFFCCCCCC)
        }
    }

    val centerShapeWidthRatio by transition.animateFloat(
        transitionSpec = { tween(TransitionDurationMs) },
        label = "CenterShapeWidth"
    ) { state ->
        when (state) {
            is UiState.Clock -> 1.8f
            is UiState.Compass -> 0.6f
            is UiState.Stopwatch -> StopwatchPillWidthRatio
        }
    }

    val centerShapeHeightRatio by transition.animateFloat(
        transitionSpec = { tween(TransitionDurationMs) },
        label = "CenterShapeHeight"
    ) { state ->
        when (state) {
            is UiState.Clock -> 1.8f
            is UiState.Compass -> 0.6f
            is UiState.Stopwatch -> StopwatchPillHeightRatio
        }
    }

    val stopwatchSubdialsVisibilityRatio by transition.animateFloat(
        transitionSpec = { tween(TransitionDurationMs) },
        label = "StopwatchSubdialsSizeRatio"
    ) { state ->
        when (state) {
            is UiState.Clock -> 0f
            is UiState.Compass -> 0f
            is UiState.Stopwatch -> 1f
        }
    }

    val clockTextAlpha by transition.animateFloat(
        transitionSpec = { tween(TransitionDurationMs) },
        label = "ClockTextAlpha"
    ) { state ->
        if (state is UiState.Clock) 1f else 0f
    }
    val stopwatchTextAlpha by transition.animateFloat(
        transitionSpec = { tween(TransitionDurationMs) },
        label = "StopwatchTextAlpha"
    ) { state ->
        if (state is UiState.Stopwatch) 1f else 0f
    }
    val compassTextAlpha by transition.animateFloat(
        transitionSpec = { tween(TransitionDurationMs) },
        label = "CompassTextAlpha"
    ) { state ->
        if (state is UiState.Compass) 1f else 0f
    }

    val primaryTickLength by transition.animateDp(
        transitionSpec = { tween(TransitionDurationMs) },
        label = "TickLength"
    ) { state ->
        when (state) {
            is UiState.Clock -> 36.dp
            is UiState.Stopwatch -> 48.dp
            is UiState.Compass -> 24.dp
        }
    }

    // Separate time sources so each freezes when its mode is inactive.
    // This preserves the correct "from" position when a mode transition starts.
    var clockElapsedMs by remember {
        mutableLongStateOf(if (uiState is UiState.Clock) uiState.timeMillis else 0L)
    }
    var stopwatchElapsedMs by remember {
        mutableLongStateOf(
            when (uiState) {
                is UiState.Stopwatch.Running -> uiState.elapsedMillis
                is UiState.Stopwatch.Paused -> uiState.elapsedMillis
                else -> 0L
            }
        )
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Clock -> clockElapsedMs = uiState.timeMillis
            is UiState.Stopwatch.Running -> stopwatchElapsedMs = uiState.elapsedMillis
            is UiState.Stopwatch.Paused -> stopwatchElapsedMs = uiState.elapsedMillis
            is UiState.Stopwatch.Zero -> stopwatchElapsedMs = 0L
            is UiState.Compass -> {}
        }
    }

    val currentUiState by rememberUpdatedState(uiState)
    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { currentFrameTime ->
                val delta = currentFrameTime - lastFrameTime
                when (currentUiState) {
                    is UiState.Clock, is UiState.Compass -> clockElapsedMs += delta
                    is UiState.Stopwatch.Running -> stopwatchElapsedMs += delta
                    else -> {}
                }
                lastFrameTime = currentFrameTime
            }
        }
    }

    // Animated lerp factors for mode transitions (fixed targets, no time-based values)
    val nonCompassLerp by transition.animateFloat(
        transitionSpec = { tween(TransitionDurationMs) },
        label = "NonCompassLerp"
    ) { state -> if (state is UiState.Compass) 0f else 1f }

    val stopwatchLerp by transition.animateFloat(
        transitionSpec = { tween(TransitionDurationMs) },
        label = "StopwatchLerp"
    ) { state -> if (state is UiState.Stopwatch) 1f else 0f }

    // Clock angles (freeze when not in Clock/Compass mode, preserving position for transitions)
    val clockSecondsAngle = (clockElapsedMs % 60_000L / 60_000f) * 360f
    val clockMinutesAngle = (clockElapsedMs % 3_600_000L / 3_600_000f) * 360f
    val clockHoursAngle = (clockElapsedMs % 43_200_000L / 43_200_000f) * 360f

    // Stopwatch angles (freeze when not in Stopwatch mode, preserving position for transitions)
    val swSecondsAngle = (stopwatchElapsedMs % 60_000L / 60_000f) * 360f
    val swMinutesAngle = (stopwatchElapsedMs % 3_600_000L / 3_600_000f) * 360f
    val swHoursAngle = (stopwatchElapsedMs % 43_200_000L / 43_200_000f) * 360f

    // When Compass↔Stopwatch, both lerps animate simultaneously at the same rate, so their
    // ratio is always 1 — meaning we only use swSecondsAngle, never clockSecondsAngle.
    // When Compass↔Clock, stopwatchLerp stays at 0 so ratio is 0 — only clockSecondsAngle.
    // This prevents clock angles from bleeding into Compass↔Stopwatch transitions.
    val swRatio = if (nonCompassLerp > 0f) (stopwatchLerp / nonCompassLerp).coerceIn(0f, 1f) else 0f
    val secondsHandAngle = (clockSecondsAngle + (swSecondsAngle - clockSecondsAngle) * swRatio) * nonCompassLerp
    val minutesHandAngle = (clockMinutesAngle + (swSecondsAngle - clockMinutesAngle) * swRatio) * nonCompassLerp
    val hoursHandAngle = (clockHoursAngle + (swSecondsAngle - clockHoursAngle) * swRatio) * nonCompassLerp
    val stopwatchMinutesHandAngle = swMinutesAngle * stopwatchLerp
    val stopwatchHoursHandAngle = swHoursAngle * stopwatchLerp

    val compassDegrees by transition.animateFloat(label = "CompassDegrees") { state ->
        when (state) {
            is UiState.Compass -> state.compassDegrees.toFloat()
            else -> 0f
        }
    }

    MorphingDialCanvas(
        faceColor = outerBackgroundColor,
        primaryTickLength = primaryTickLength,
        clockTextAlpha = clockTextAlpha,
        stopwatchTextAlpha = stopwatchTextAlpha,
        compassTextAlpha = compassTextAlpha,
        centerShapeWidthRatio = centerShapeWidthRatio,
        centerShapeHeightRatio = centerShapeHeightRatio,
        secondsHandAngle = secondsHandAngle,
        minutesHandAngle = minutesHandAngle,
        hoursHandAngle = hoursHandAngle,
        stopwatchMinutesHandAngle = stopwatchMinutesHandAngle,
        stopwatchHoursHandAngle = stopwatchHoursHandAngle,
        compassDegrees = compassDegrees,
        stopwatchSubdialsVisibilityRatio = stopwatchSubdialsVisibilityRatio,
        modifier = Modifier.aspectRatio(1f)
    )
}

@Composable
fun MorphingDialCanvas(
    faceColor: Color,
    primaryTickLength: Dp,
    clockTextAlpha: Float,
    stopwatchTextAlpha: Float,
    compassTextAlpha: Float,
    centerShapeWidthRatio: Float,
    centerShapeHeightRatio: Float,
    modifier: Modifier = Modifier,
    secondsHandAngle: Float,
    minutesHandAngle: Float,
    hoursHandAngle: Float,
    stopwatchMinutesHandAngle: Float,
    stopwatchHoursHandAngle: Float,
    compassDegrees: Float,
    stopwatchSubdialsVisibilityRatio: Float
) {
    val textMeasurer = rememberTextMeasurer()
    var sizeRatio by remember { mutableFloatStateOf(1f) }

    Canvas(modifier.onGloballyPositioned {
        sizeRatio = it.size.width / 1144f // to scale the text and the dial when scaling the window
    }) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = canvasWidth / 2f
        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

        drawCircle(
            color = faceColor,
            radius = radius,
            center = center
        )

        val centerShapeWidth = centerShapeWidthRatio * radius
        val centerShapeHeight = centerShapeHeightRatio * radius
        val cornerRadiusPx = minOf(centerShapeWidth, centerShapeHeight) / 2f

        val shapeTopLeft = Offset(
            x = center.x - (centerShapeWidth / 2f),
            y = center.y - (centerShapeHeight / 2f)
        )

        drawRoundRect(
            color = Color.Black,
            topLeft = shapeTopLeft,
            size = Size(width = centerShapeWidth, height = centerShapeHeight),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )

        drawOuterTicks(compassDegrees, primaryTickLength.value * sizeRatio, center, sizeRatio)

        if (clockTextAlpha > 0f) {
            drawClockNumbers(
                textMeasurer = textMeasurer,
                center = center,
                radius = radius,
                sizeRatio = sizeRatio,
                alpha = clockTextAlpha
            )
        }

        if (stopwatchTextAlpha > 0f) {
            drawStopwatchSecondsNumbers(
                textMeasurer = textMeasurer,
                center = center,
                radius = radius,
                sizeRatio = sizeRatio,
                alpha = stopwatchTextAlpha
            )
        }

        if (compassTextAlpha > 0f) {
            drawCompassLabels(
                textMeasurer = textMeasurer,
                center = center,
                radius = radius,
                alpha = compassTextAlpha,
                sizeRatio = sizeRatio,
                compassDegrees = compassDegrees
            )
        }

        if (stopwatchSubdialsVisibilityRatio > 0f) {
            val maxSubdialRadius = radius * StopwatchPillHeightRatio / 2
            val subdialPrimaryTickLengthPx = primaryTickLength.value * 0.6f * sizeRatio
            val handColor = Color(0xFFBB1E2B).copy(alpha = stopwatchSubdialsVisibilityRatio)
            translate(left = -(radius * StopwatchPillWidthRatio / 2 - maxSubdialRadius) * stopwatchSubdialsVisibilityRatio) {
                drawStopwatchHoursTicks(
                    primaryTickLength = subdialPrimaryTickLengthPx,
                    center = center,
                    maxRadius = maxSubdialRadius,
                    sizeRatio = stopwatchSubdialsVisibilityRatio * sizeRatio
                )
                drawStopwatchHoursNumbers(
                    textMeasurer = textMeasurer,
                    center = center,
                    radius = maxSubdialRadius,
                    sizeRatio = sizeRatio,
                    alpha = stopwatchSubdialsVisibilityRatio
                )
                translate(left = center.x, top = center.y) {
                    drawStopwatchHand(
                        angleDegrees = stopwatchHoursHandAngle,
                        handLength = maxSubdialRadius * 0.95f,
                        baseWidth = 8f * sizeRatio,
                        handColor = handColor,
                    )
                }
            }
            translate(left = +(radius * StopwatchPillWidthRatio / 2 - maxSubdialRadius) * stopwatchSubdialsVisibilityRatio) {
                drawStopwatchTicks(
                    primaryTickLength = subdialPrimaryTickLengthPx,
                    center = center,
                    maxRadius = maxSubdialRadius,
                    sizeRatio = stopwatchSubdialsVisibilityRatio * sizeRatio
                )
                drawStopwatchMinutesNumbers(
                    textMeasurer = textMeasurer,
                    center = center,
                    radius = maxSubdialRadius,
                    sizeRatio = sizeRatio,
                    alpha = stopwatchSubdialsVisibilityRatio
                )
                translate(left = center.x, top = center.y) {
                    drawStopwatchHand(
                        angleDegrees = stopwatchMinutesHandAngle,
                        handLength = maxSubdialRadius * 0.95f,
                        baseWidth = 8f * sizeRatio,
                        handColor = handColor,
                    )
                }
            }
        }

        translate(left = center.x, top = center.y) {
            drawMainHand(
                angleDegrees = hoursHandAngle,
                handLength = radius * 0.5f,
                baseWidth = 36f * sizeRatio,
                handColor = Color(0xFFDBDBDB)
            )

            drawMainHand(
                angleDegrees = minutesHandAngle,
                handLength = radius * 0.8f,
                baseWidth = 32f * sizeRatio,
                handColor = Color(0xFFDBDBDB)
            )

            drawMainHand(
                angleDegrees = secondsHandAngle,
                handLength = radius * 0.85f,
                tailLength = radius * 0.2f,
                baseWidth = 24f * sizeRatio,
                handColor = Color(0xFFE32636),
                drawColorTip = false
            )

            drawCircle(
                color = Color.Black,
                radius = 16f * sizeRatio,
                center = Offset(0f, 0f)
            )
            drawCircle(
                color = Color.LightGray,
                radius = 16f * sizeRatio,
                center = Offset(0f, 0f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f * sizeRatio)
            )
        }
    }
}

private fun DrawScope.drawOuterTicks(
    compassDegrees: Float,
    primaryTickLength: Float,
    center: Offset,
    sizeRatio: Float
) {
    for (i in 0 until 300) {
        val angleDegrees = i * 1.2f - compassDegrees
        val isPrimary = i % 25 == 0
        val isSecondary = i % 5 == 0
        val currentTickLength = if (isPrimary) primaryTickLength
        else if (isSecondary) primaryTickLength * 0.7f
        else primaryTickLength * 0.4f

        rotate(degrees = angleDegrees) {
            drawLine(
                color = Color.Black,
                start = Offset(center.x, 0f),
                end = Offset(center.x, currentTickLength),
                strokeWidth = if (isPrimary) 8f * sizeRatio else 4f * sizeRatio
            )
        }
    }
}

private fun DrawScope.drawStopwatchTicks(
    primaryTickLength: Float,
    center: Offset,
    maxRadius: Float,
    sizeRatio: Float
) {
    for (i in 0 until 60) {
        val angleDegrees = i * 6f
        val isPrimary = i % 5 == 0
        val currentTickLength = if (isPrimary) primaryTickLength else primaryTickLength * 0.5f
        val currentTickAlpha = (if (isPrimary) 1f else 0.5f) * sizeRatio

        rotate(degrees = angleDegrees) {
            drawLine(
                color = Color.White.copy(alpha = currentTickAlpha),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y - maxRadius + currentTickLength),
                strokeWidth = 4f * sizeRatio
            )
        }
    }
}

private fun DrawScope.drawStopwatchHoursTicks(
    primaryTickLength: Float,
    center: Offset,
    maxRadius: Float,
    sizeRatio: Float
) {
    for (i in 0 until 72) {
        val angleDegrees = i * 5f
        val isPrimary = i % 6 == 0
        val currentTickLength = if (isPrimary) primaryTickLength else primaryTickLength * 0.5f
        val currentTickAlpha = (if (isPrimary) 1f else 0.5f) * sizeRatio

        rotate(degrees = angleDegrees) {
            drawLine(
                color = Color.White.copy(alpha = currentTickAlpha),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y - maxRadius + currentTickLength),
                strokeWidth = 4f * sizeRatio
            )
        }
    }
}


private fun DrawScope.drawCompassLabels(
    textMeasurer: TextMeasurer,
    center: Offset,
    radius: Float,
    alpha: Float,
    compassDegrees: Float,
    sizeRatio: Float
) {
    // Inset the text slightly more than the ticks
    val textRadius = radius - 100f * sizeRatio

    // Notice in the image that N, E, S, W are larger than the numbers
    val cardinalStyle = TextStyle(
        color = Color.Black.copy(alpha = alpha),
        fontSize = (96f * sizeRatio / density / fontScale).sp,
        fontWeight = FontWeight.Bold
    )
    val degreeStyle = TextStyle(
        color = Color.Black.copy(alpha = alpha),
        fontSize = (48f * sizeRatio / density / fontScale).sp,
        fontWeight = FontWeight.Bold
    )

    // The compass labels appear every 30 degrees
    for (angle in 0 until 360 step 30) {

        // Determine what text to show based on the angle
        val label = when (angle) {
            0 -> "N"
            90 -> "E"
            180 -> "S"
            270 -> "W"
            else -> angle.toString()
        }

        // Pick the style depending on if it's a letter or a number
        val currentStyle = if (angle % 90 == 0) cardinalStyle else degreeStyle

        val textLayoutResult = textMeasurer.measure(
            text = label,
            style = currentStyle
        )

        // The Magic Trick: Rotate the Canvas around the center point
        rotate(degrees = angle.toFloat() - compassDegrees, pivot = center) {

            // Because the canvas is rotated, we ALWAYS draw the text
            // pointing straight up at the 12 o'clock position!
            val x = center.x - (textLayoutResult.size.width / 2f)

            // Subtracting textRadius moves it up towards the top edge
            val y = center.y - textRadius - (textLayoutResult.size.height / 2f)

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawClockNumbers(
    textMeasurer: TextMeasurer,
    center: Offset,
    radius: Float,
    alpha: Float,
    sizeRatio: Float
) {
    val textRadius = radius - 130f * sizeRatio
    val textStyle = TextStyle(
        color = Color.White.copy(alpha = alpha),
        fontSize = (100f * sizeRatio / density / fontScale).sp,
        fontWeight = FontWeight.SemiBold
    )

    for (i in 1..12) {
        val angleDegrees = (i * 30) - 90
        val angleRad = angleDegrees * (PI / 180.0)

        val x = center.x + (textRadius * cos(angleRad)).toFloat()
        val y = center.y + (textRadius * sin(angleRad)).toFloat()

        val textLayoutResult = textMeasurer.measure(
            text = i.toString(),
            style = textStyle
        )

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = x - (textLayoutResult.size.width / 2f),
                y = y - (textLayoutResult.size.height / 2f)
            )
        )
    }
}

private fun DrawScope.drawStopwatchSecondsNumbers(
    textMeasurer: TextMeasurer,
    center: Offset,
    radius: Float,
    alpha: Float,
    sizeRatio: Float
) {
    val textRadius = radius - 130f * sizeRatio
    val textStyle = TextStyle(
        color = Color.Black.copy(alpha = alpha),
        fontSize = (100f * sizeRatio / density / fontScale).sp,
        fontWeight = FontWeight.SemiBold
    )

    for (i in 1..12) {
        if (i == 3 || i == 9) continue // skip to not clash with the pill

        val angleDegrees = (i * 30) - 90
        val angleRad = angleDegrees * (PI / 180.0)

        val x = center.x + (textRadius * cos(angleRad)).toFloat()
        val y = center.y + (textRadius * sin(angleRad)).toFloat()

        val textLayoutResult = textMeasurer.measure(
            text = (i * 5).toString().padStart(2, '0'),
            style = textStyle
        )

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = x - (textLayoutResult.size.width / 2f),
                y = y - (textLayoutResult.size.height / 2f)
            )
        )
    }
}

private fun DrawScope.drawStopwatchHoursNumbers(
    textMeasurer: TextMeasurer,
    center: Offset,
    radius: Float,
    alpha: Float,
    sizeRatio: Float
) {
    val textRadius = radius - 64f * sizeRatio
    val textStyle = TextStyle(
        color = Color.White.copy(alpha = alpha),
        fontSize = (48f * sizeRatio / density / fontScale).sp,
        fontWeight = FontWeight.Normal
    )

    for (i in 1..4) {
        val angleDegrees = (i * 90) - 90
        val angleRad = angleDegrees * (PI / 180.0)

        val x = center.x + (textRadius * cos(angleRad)).toFloat()
        val y = center.y + (textRadius * sin(angleRad)).toFloat()

        val textLayoutResult = textMeasurer.measure(
            text = (i * 3).toString(),
            style = textStyle
        )

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = x - (textLayoutResult.size.width / 2f),
                y = y - (textLayoutResult.size.height / 2f)
            )
        )
    }
}


private fun DrawScope.drawStopwatchMinutesNumbers(
    textMeasurer: TextMeasurer,
    center: Offset,
    radius: Float,
    alpha: Float,
    sizeRatio: Float
) {
    val textRadius = radius - 64f * sizeRatio
    val textStyle = TextStyle(
        color = Color.White.copy(alpha = alpha),
        fontSize = (48f * sizeRatio / density / fontScale).sp,
        fontWeight = FontWeight.Normal
    )

    for (i in 1..6) {
        val angleDegrees = (i * 60) - 90
        val angleRad = angleDegrees * (PI / 180.0)

        val x = center.x + (textRadius * cos(angleRad)).toFloat()
        val y = center.y + (textRadius * sin(angleRad)).toFloat()

        val textLayoutResult = textMeasurer.measure(
            text = (i * 10).toString(),
            style = textStyle
        )

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = x - (textLayoutResult.size.width / 2f),
                y = y - (textLayoutResult.size.height / 2f)
            )
        )
    }
}

private fun DrawScope.drawMainHand(
    angleDegrees: Float,
    handLength: Float,
    baseWidth: Float,
    handColor: Color,
    tailLength: Float = 0f,
    drawColorTip: Boolean = true
) {
    val path = Path().apply {
        moveTo(-baseWidth / 2f, 0f)
        lineTo(-baseWidth / 2f, -handLength + baseWidth / 2f)
        arcTo(
            rect = Rect(
                left = -baseWidth / 2f,
                top = -handLength,
                right = baseWidth / 2f,
                bottom = -handLength + baseWidth
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        lineTo(baseWidth / 2f, tailLength)
        lineTo(-baseWidth / 2f, tailLength)
        close()
    }

    rotate(degrees = angleDegrees, pivot = Offset.Zero) {
        drawPath(
            path = path,
            color = handColor
        )

        if (drawColorTip) {
            drawRoundRect(
                color = Color.Red,
                topLeft = Offset(-baseWidth / 4f, -handLength + baseWidth / 4),
                size = Size(baseWidth / 2, handLength / 4),
                cornerRadius = CornerRadius(baseWidth / 4)
            )
        }

        drawCircle(
            color = handColor,
            radius = baseWidth * 1.2f,
            center = Offset.Zero
        )
    }
}

private fun DrawScope.drawStopwatchHand(
    angleDegrees: Float,
    handLength: Float,
    baseWidth: Float,
    handColor: Color
) {
    val path = Path().apply {
        moveTo(-baseWidth / 2f, 0f)
        lineTo(-baseWidth / 2f, -handLength + baseWidth / 2f)
        arcTo(
            rect = Rect(
                left = -baseWidth / 2f,
                top = -handLength,
                right = baseWidth / 2f,
                bottom = -handLength + baseWidth
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        lineTo(baseWidth / 2f, 0f)
        close()
    }

    rotate(degrees = angleDegrees, pivot = Offset.Zero) {
        drawPath(
            path = path,
            color = handColor
        )

        drawCircle(
            color = handColor,
            radius = baseWidth * 1.5f,
            center = Offset.Zero
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = baseWidth * 0.8f,
            center = Offset.Zero
        )
    }
}

@Preview
@Composable
private fun MorphingDialClockPreview() {
    // 10:10:30 AM in millis
    MorphingDial(UiState.Clock(timeMillis = 36_630_000L))
}

@Preview
@Composable
private fun MorphingDialStopwatchPreview() {
    MorphingDial(UiState.Stopwatch.Paused(elapsedMillis = 75_500L))
}

@Preview
@Composable
private fun MorphingDialCompassPreview() {
    MorphingDial(UiState.Compass(compassDegrees = 50))
}
