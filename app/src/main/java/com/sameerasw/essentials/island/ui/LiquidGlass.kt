package com.sameerasw.essentials.island.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline

/**
 * Visual de "liquid glass" para a superficie da ilha.
 *
 * [progress] vai de 0 (pilula compacta, preta e opaca para se fundir com a camera) a 1 (ilha expandida).
 * Conforme expande, o fundo fica translucido e ganha brilho especular, tom de profundidade e borda de luz.
 * Nao e um desfoque real do que esta atras: janelas de overlay nao conseguem desfocar so dentro de uma forma arredondada.
 */
fun Modifier.liquidGlass(progress: () -> Float, shape: () -> Shape): Modifier = drawBehind {
    val g = progress().coerceIn(0f, 1f)
    // Base: preto opaco no compacto, translucido no expandido.
    drawRect(Color.Black.copy(alpha = 1f - GLASS_CLEARING * g))
    if (g <= 0f) return@drawBehind

    // Brilho suave vindo do canto superior esquerdo.
    drawRect(
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f * g),
                Color.White.copy(alpha = 0.05f * g),
                Color.Transparent,
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
    )
    // Profundidade: azul frio leve na parte de baixo.
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xFF8FB4FF).copy(alpha = 0.10f * g)),
        ),
    )
    // Borda de luz: mais forte nas quinas opostas, como a luz batendo no vidro.
    val outline = shape().createOutline(size, layoutDirection, this)
    drawOutline(
        outline = outline,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f * g),
                Color.White.copy(alpha = 0.08f * g),
                Color.White.copy(alpha = 0.30f * g),
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        style = Stroke(width = 2.2f * density),
    )
}

private const val GLASS_CLEARING = 0.34f
