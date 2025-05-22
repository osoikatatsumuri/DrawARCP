package com.example.drawarcp.presentation.screens.arscene

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.drawarcp.presentation.uistate.nodes.AnchorNodeUIState
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.collision.Vector3
import kotlinx.coroutines.FlowPreview
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun TransformNodeSheet(
    onDismiss: () -> Unit,
    nodeSelected: AnchorNodeUIState,
    onScaleChange: (Float) -> Unit,
    onOpacityChange: (Int) -> Unit,
    onRotationChange: (Float3) -> Unit,
    onNodeDelete: (String) -> Unit,
) {

    var scale by remember(nodeSelected.id) { mutableFloatStateOf(nodeSelected.scale.x) }
    var opacity by remember(nodeSelected.id) { mutableIntStateOf(nodeSelected.opacity) }

    var rotationAngles by remember(nodeSelected.id) { mutableStateOf(nodeSelected.rotationAngles) }

    var selectedAxis by remember {mutableStateOf(Pair("X", Vector3(1f, 0f, 0f)))}

    val rotationAxes = listOf(
        Pair<String, Vector3>("X", Vector3(1f, 0f, 0f)),
        Pair<String, Vector3>("Y", Vector3(0f, 1f, 0f)),
        Pair<String, Vector3>("Z", Vector3(0f, 0f, 1f)),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Трансформация объекта", modifier = Modifier.padding(bottom = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                rotationAxes.forEach { axis ->
                    LocalOrientationAxisChip(axis, onSelected = {
                        selectedAxis = axis
                    })
                }
            }

            Text("Масштаб: ${"%.2f".format(scale)}")
            Slider(
                value = scale,
                onValueChange = {
                    scale = it
                    onScaleChange(it)
                },
                valueRange = 0.1f..3f
            )

            Text(
                "Rotation: ${
                    "%.0f".format(
                        rotationAngles.toFloatArray()[ when (selectedAxis.first) {
                            "X" -> 0
                            "Y" -> 1
                            else -> 2
                        } ]
                    )
                }° on ${selectedAxis.first}"
            )

            Slider(
                value = rotationAngles.toFloatArray()[ when (selectedAxis.first) {
                    "X" -> 0
                    "Y" -> 1
                    else -> 2
                } ],
                onValueChange = { newValue ->
                    val (x, y, z) = rotationAngles.toFloatArray()
                    val updated = when (selectedAxis.first) {
                        "X" -> Float3(newValue, y, z)
                        "Y" -> Float3(x, newValue, z)
                        else -> Float3(x, y, newValue)
                    }
                    rotationAngles = updated
                    onRotationChange(updated)
                },
                valueRange = 0f..360f
            )

            Text("Прозрачность: $opacity")
            Slider(
                value = opacity.toFloat(),
                onValueChange = {
                    opacity = it.toInt()
                    onOpacityChange(opacity)
                },
                valueRange = 0f..255f
            )

            TextButton(
                onClick = {
                    onDismiss()

                    onNodeDelete(nodeSelected.id)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .height(6.dp)
                    .weight(1f)
            ) {
                Text(text = "Удалить узел", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}