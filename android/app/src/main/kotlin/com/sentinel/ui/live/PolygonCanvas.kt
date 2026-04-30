package com.sentinel.ui.live

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sentinel.domain.SafeZone
import com.sentinel.ui.theme.*

data class CanvasVertex(val x: Float, val y: Float)   // pixel coords within canvas

private const val HANDLE_RADIUS = 18f   // px hit area
private const val HANDLE_DRAW   = 10f   // px drawn radius
private const val CLOSE_RADIUS  = 30f

fun CanvasVertex.toNorm(w: Float, h: Float) = listOf((x / w).toDouble(), (y / h).toDouble())
fun List<Double>.toCanvas(w: Float, h: Float) = CanvasVertex((this[0] * w).toFloat(), (this[1] * h).toFloat())

@Composable
fun PolygonCanvasOverlay(
    modifier: Modifier = Modifier,
    editorState: EditorState,
    vertices: List<CanvasVertex>,
    onVerticesChange: (List<CanvasVertex>) -> Unit,
    onStateChange: (EditorState) -> Unit,
) {
    var cursorPos by remember { mutableStateOf<Offset?>(null) }
    var dragTarget by remember { mutableStateOf<Int?>(null) }

    val interactive = editorState != EditorState.Idle

    Canvas(
        modifier = modifier
            .then(if (interactive) Modifier.pointerInput(editorState) {
                detectTapGestures(
                    onTap = { offset ->
                        when (editorState) {
                            EditorState.Drawing -> {
                                if (vertices.size >= 3) {
                                    val first = vertices.first()
                                    val d = Offset(first.x, first.y).minus(offset).getDistance()
                                    if (d <= CLOSE_RADIUS) {
                                        onStateChange(EditorState.Editing)
                                        return@detectTapGestures
                                    }
                                }
                                onVerticesChange(vertices + CanvasVertex(offset.x, offset.y))
                            }
                            else -> Unit
                        }
                    }
                )
            } else Modifier)
            .then(if (editorState == EditorState.Editing) Modifier.pointerInput(vertices) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragTarget = vertices.indexOfFirst { v ->
                            Offset(v.x, v.y).minus(offset).getDistance() <= HANDLE_RADIUS
                        }.takeIf { it >= 0 }
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        val idx = dragTarget
                        if (idx != null) {
                            onVerticesChange(vertices.toMutableList().also {
                                it[idx] = CanvasVertex(
                                    (it[idx].x + delta.x).coerceIn(0f, size.width.toFloat()),
                                    (it[idx].y + delta.y).coerceIn(0f, size.height.toFloat()),
                                )
                            })
                        } else {
                            onVerticesChange(vertices.map {
                                CanvasVertex(
                                    (it.x + delta.x).coerceIn(0f, size.width.toFloat()),
                                    (it.y + delta.y).coerceIn(0f, size.height.toFloat()),
                                )
                            })
                        }
                    },
                    onDragEnd = { dragTarget = null }
                )
            } else Modifier)
    ) {
        if (vertices.isEmpty()) return@Canvas
        val px = vertices.map { Offset(it.x, it.y) }

        // Filled polygon (editing mode)
        if (editorState == EditorState.Editing && px.size >= 3) {
            val path = Path().apply {
                moveTo(px[0].x, px[0].y)
                px.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path, color = Accent.copy(alpha = 0.2f), style = Fill)
            drawPath(path, color = Accent, style = Stroke(width = 2.dp.toPx()))
        }

        // Dashed lines while drawing
        if (editorState == EditorState.Drawing && px.size >= 2) {
            for (i in 0 until px.size - 1) {
                drawLine(Accent, px[i], px[i + 1], strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
            cursorPos?.let { cursor ->
                drawLine(Accent.copy(alpha = 0.5f), px.last(), cursor, strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            }
        }

        // Vertex handles
        px.forEachIndexed { i, p ->
            val isFirst = i == 0
            val closeable = editorState == EditorState.Drawing && vertices.size >= 3 && isFirst
            drawCircle(Color.White, radius = HANDLE_DRAW, center = p)
            drawCircle(
                color = if (closeable) Primary else Accent,
                radius = HANDLE_DRAW,
                center = p,
                style = Stroke(width = if (closeable) 3.dp.toPx() else 2.dp.toPx())
            )
        }
    }
}
