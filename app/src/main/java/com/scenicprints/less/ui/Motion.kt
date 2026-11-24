package com.scenicprints.less.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

/**
 * Everything moves like a mechanism. One easing, one duration, no bounce, no overshoot, no
 * spring. A quick start that settles, which is how a well damped knob comes to rest.
 */
val Settle = CubicBezierEasing(0.2f, 0f, 0f, 1f)
const val DURATION = 200

fun <T> mechanism(ms: Int = DURATION): TweenSpec<T> = tween(ms, easing = Settle)
