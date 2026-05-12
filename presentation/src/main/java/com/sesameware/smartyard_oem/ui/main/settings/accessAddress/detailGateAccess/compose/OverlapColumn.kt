package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

@Composable
fun OverlapColumn(
    overlapDp: Dp,
    modifier: Modifier = Modifier,
    content: @Composable OverlapScope.() -> Unit
) {
    val overlapPx = with(LocalDensity.current) { overlapDp.roundToPx() }

    Layout(
        modifier = modifier,
        content = { OverlapScopeInstance.content() }
    ) { measurables, constraints ->
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        val weights = measurables.map { it.parentData as? LayoutWeightData }
        val totalWeight = weights.sumOf { it?.weight?.toDouble() ?: 0.0 }

        var usedHeight = 0
        measurables.forEachIndexed { i, measurable ->
            if (weights[i] == null) {
                val placeable = measurable.measure(constraints.copy(minHeight = 0))
                placeables[i] = placeable
                usedHeight += placeable.height
            }
        }

        val totalOverlap = overlapPx * (measurables.size - 1)
        val remainingHeight = (constraints.maxHeight - usedHeight + totalOverlap).coerceAtLeast(0)

        measurables.forEachIndexed { i, measurable ->
            val weightData = weights[i]
            if (weightData != null) {
                val h = (remainingHeight * (weightData.weight / totalWeight)).toInt()
                placeables[i] = measurable.measure(
                    constraints.copy(minHeight = h, maxHeight = h)
                )
            }
        }

        val finalHeight = placeables.filterNotNull().sumOf { it.height } - totalOverlap

        layout(constraints.maxWidth, finalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var yPosition = 0
            placeables.forEach { placeable ->
                placeable?.placeRelative(0, yPosition)
                yPosition += (placeable?.height ?: 0) - overlapPx
            }
        }
    }
}

interface OverlapScope {
    fun Modifier.weight(weight: Float): Modifier
}

private object OverlapScopeInstance : OverlapScope {
    override fun Modifier.weight(weight: Float) = this.then(
        LayoutWeightData(weight)
    )
}

private class LayoutWeightData(val weight: Float) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@LayoutWeightData
}
