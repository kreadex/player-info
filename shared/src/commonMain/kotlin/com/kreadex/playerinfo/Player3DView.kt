package com.kreadex.playerinfo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class PlayerModelType {
    STEVE, ALEX, OLD
}

@Composable
expect fun Player3DView(
    modifier: Modifier,
    modelType: PlayerModelType,
    skinBytes: ByteArray?,
    onHit: () -> Unit,
    onLongPress: () -> Unit
)

fun getDirectionalAnimation(dx: Double, dz: Double): String {
    val angle = kotlin.math.atan2(dx, dz) * (180.0 / kotlin.math.PI)
    return when {
        angle in -45.0..45.0 -> "hit_front"
        angle > 45.0 && angle <= 135.0 -> "hit_right"
        angle < -45.0 && angle >= -135.0 -> "hit_left"
        else -> "hit_back"
    }
}