package ru.madbrains.smartyard.ui.main.address.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer


enum class CardFace(val angle: Float) {
    Front(0f) {
        override val next: CardFace
            get() = Back
    },
    Back(180f) {
        override val next: CardFace
            get() = Front
    };
    abstract val next: CardFace
}

enum class RotationAxis {
    AxisX,
    AxisY,
}

@ExperimentalMaterialApi
@Composable
fun FlipCard(
    cardFace: CardFace,
    onClick: (CardFace) -> Unit = {},
    modifier: Modifier = Modifier,
    axis: RotationAxis = RotationAxis.AxisY,
    back: @Composable (Modifier) -> Unit = {},
    front: @Composable (Modifier) -> Unit = {},
) {
    val rotation = animateFloatAsState(
        targetValue = cardFace.angle,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing,
        ),
        label = ""
    )

    Column(
        modifier = modifier
//            .clickable {
//                onClick(cardFace)
//            }
            .graphicsLayer {
                if (axis == RotationAxis.AxisX) {
                    rotationX = rotation.value
                } else {
                    rotationY = rotation.value
                }
                cameraDistance = 12f * density
            },
    ) {
        if (rotation.value <= 90f) {
            front(Modifier)
        } else {
            back(
                Modifier
                    .graphicsLayer {
                        if (axis == RotationAxis.AxisX) {
                            rotationX = 180f
                        } else {
                            rotationY = 180f
                        }
                    }
            )
        }
    }
}