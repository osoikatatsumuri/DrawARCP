package com.example.drawarcp.presentation.screens.arscene

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.BottomAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ARBottomBar(
    onPickImage: () -> Unit,
    onAddPlane: () -> Unit,
    onAddImage: () -> Unit
) {
    BottomAppBar(
        containerColor = Color.White.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val buttonModifier = Modifier
                .height(44.dp)
                .weight(1f)

            TransparentIconButton(
                icon = Icons.Default.ArrowDropDown,
                text = "Выбрать",
                onClick = onPickImage,
                modifier = buttonModifier
            )

            Spacer(modifier = Modifier.width(8.dp))

            TransparentIconButton(
                icon = Icons.Default.AddCircle,
                text = "Плоскость",
                onClick = onAddPlane,
                modifier = buttonModifier
            )

            Spacer(modifier = Modifier.width(8.dp))

            TransparentIconButton(
                icon = Icons.Default.Add,
                text = "Изобр.",
                onClick = onAddImage,
                modifier = buttonModifier
            )
        }
    }
}