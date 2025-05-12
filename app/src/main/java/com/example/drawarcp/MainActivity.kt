package com.example.drawarcp

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.drawarcp.presentation.viewmodels.ARSceneViewModel
import com.example.drawarcp.presentation.screens.ARSceneScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: ARSceneViewModel by viewModels()

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        )
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)

        setContent {
            ARSceneScreen(viewModel)
        }
    }
}