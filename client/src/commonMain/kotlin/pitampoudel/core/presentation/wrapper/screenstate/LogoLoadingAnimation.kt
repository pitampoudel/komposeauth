package pitampoudel.core.presentation.wrapper.screenstate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun LogoLoadingAnimation(
    logo: Painter,
    animationDelay: Int = 1500
) {
    val animatable = remember {
        Animatable(initialValue = 0f)
    }

    LaunchedEffect(Unit) {
        delay(timeMillis = (animationDelay / 3L))
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

    Image(
        modifier = Modifier
            .graphicsLayer(
                scaleX = animatable.value,
                scaleY = animatable.value
            )
            .size(size = 200.dp)
            .clip(shape = CircleShape)
            .alpha(1 - animatable.value),
        contentDescription = null,
        painter = logo
    )


}

