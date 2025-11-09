package pitampoudel.core.presentation.wrapper.screenstate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoadingAnimation(
    circleCount: Int = 3,
    circleColor: Color = MaterialTheme.colorScheme.primary,
    animationDelay: Int = 1500
) {
    val circles = remember {
        (1..circleCount).map {
            Animatable(initialValue = 0f)
        }
    }

    LaunchedEffect(Unit) {
        circles.forEachIndexed { index, animatable ->
            launch {
                // Use coroutine delay to sync animations
                // divide the animation delay by number of circles
                delay(timeMillis = (animationDelay / 3L) * (index + 1))
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = animationDelay,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        }
    }

    // outer circle
    Box(
        modifier = Modifier
            .size(size = 200.dp)
            .background(color = Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // animating circles
        circles.forEach { animatable ->
            Box(
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = animatable.value,
                        scaleY = animatable.value
                    )
                    .size(size = 200.dp)
                    .clip(shape = CircleShape)
                    .background(
                        color = circleColor
                            .copy(alpha = (1 - animatable.value))
                    )
            ) {
            }
        }
    }
}
