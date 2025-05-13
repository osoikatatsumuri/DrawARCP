package com.example.drawarcp.presentation.viewmodels

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drawarcp.data.models.PermissionData
import com.example.drawarcp.domain.usecases.GetPermissionDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getPermissionDataUseCase: GetPermissionDataUseCase
) : ViewModel() {

    private val _currentPermissionData = MutableStateFlow<PermissionData?>(null)
    val currentPermissionData: StateFlow<PermissionData?> = _currentPermissionData

    fun loadPermissionData(permission: String){
        viewModelScope.launch {
            getPermissionDataUseCase.invoke(permission)
                .onSuccess { permissionData ->
                    _currentPermissionData.value = permissionData
                }
                .onFailure { exception ->
                    Log.d("AR", exception.stackTraceToString())
                }
        }
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowRationale(activity: android.app.Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}