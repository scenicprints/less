package com.scenicprints.less.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One gesture for an icon: a tap, a hold, or a hold that becomes a carry.
 *
 * A finger that lifts before the long-press timeout is a tap. One that moves before it is a
 * scroll and belongs to whatever is underneath. One that stays is a hold, and if it then moves
 * past the slop the icon lifts and follows it until it lets go. Everything is in one detector so
 * the parts cannot cancel each other.
 */
fun Modifier.holdable(
    key: Any?,
    onTap: () -> Unit,
    onHold: () -> Unit,
    onLift: () -> Unit,
    onMove: (Offset) -> Unit,
    onDrop: () -> Unit
): Modifier = pointerInput(key) {
    val slop = viewConfiguration.touchSlop
    val timeout = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown()
        val t0 = System.currentTimeMillis()
        val up = withTimeoutOrNull(timeout) { waitForUpOrCancellation() }
        if (up != null) {
            up.consume()
            onTap()
            return@awaitEachGesture
        }
        if (System.currentTimeMillis() - t0 < timeout - 20) return@awaitEachGesture
        onHold()
        var moved = false
        var total = Offset.Zero
        drag(down.id) { change ->
            total += change.positionChange()
            if (!moved && total.getDistance() > slop) {
                moved = true
                onLift()
            }
            if (moved) {
                onMove(total)
                change.consume()
            }
        }
        if (moved) onDrop()
    }
}

/**
 * A hold on empty ground. It waits a little longer than an icon's hold, and asks [claimed]
 * first, so holding an icon never also opens settings.
 */
fun Modifier.holdGround(claimed: () -> Boolean, onDown: () -> Unit, onHold: () -> Unit): Modifier = pointerInput(Unit) {
    val timeout = viewConfiguration.longPressTimeoutMillis + 60
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        onDown()
        val t0 = System.currentTimeMillis()
        val up = withTimeoutOrNull(timeout) { waitForUpOrCancellation() }
        if (up == null && System.currentTimeMillis() - t0 >= timeout - 20 && !claimed()) onHold()
    }
}
