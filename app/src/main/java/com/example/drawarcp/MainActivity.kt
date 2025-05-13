package com.example.drawarcp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.drawarcp.presentation.viewmodels.ARSceneViewModel
import com.example.drawarcp.presentation.screens.ARSceneScreen
import com.example.drawarcp.presentation.RequirePermission
import com.example.drawarcp.presentation.viewmodels.PermissionsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: ARSceneViewModel by viewModels()
        val permissionViewModel: PermissionsViewModel by viewModels<PermissionsViewModel>()

        setContent {
            RequirePermission(Manifest.permission.CAMERA, permissionViewModel, this) {
                ARSceneScreen(viewModel, permissionViewModel)
            }
        }
    }
}