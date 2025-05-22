package com.example.drawarcp.presentation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.drawarcp.presentation.viewmodels.PermissionsViewModel
import kotlin.system.exitProcess

@Composable
fun RequirePermission(
    permission: String,
    permissionViewModel: PermissionsViewModel,
    activity: Activity,
    onGranted: @Composable () -> Unit
) {
    val permissionInfo by permissionViewModel.currentPermissionData.collectAsState()
    var isGranted by remember { mutableStateOf(permissionViewModel.isPermissionGranted(permission))}

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = true
    }

    LaunchedEffect(Unit) {
        permissionViewModel.loadPermissionData(permission)

        if (!isGranted && !permissionViewModel.shouldShowRationale(activity, permission)) {
            launcher.launch(permission)
        }
    }

    if (isGranted) {
        onGranted()
    } else {
        if (permissionViewModel.shouldShowRationale(activity, permission)) {
            AlertDialog(
                onDismissRequest = { exitProcess(-1) },
                confirmButton = {
                    TextButton(onClick = { launcher.launch(permission) }) {
                        Text("OK")
                    }
                },
                text = { Text(permissionInfo?.rationaleText ?: "This permission is required.") },
                dismissButton = {
                    TextButton(onClick = { exitProcess(-1) }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

