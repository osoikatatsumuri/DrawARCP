package com.example.drawarcp.data

import android.content.res.Resources.NotFoundException
import com.example.drawarcp.data.models.PermissionData
import com.example.drawarcp.domain.interfaces.IPermissionsRepository

class PermissionRepositoryImpl(): IPermissionsRepository {

    private val permissionsMap = mapOf(
        android.Manifest.permission.CAMERA to PermissionData(
            name = android.Manifest.permission.CAMERA,
            rationaleText = "Camera is needed to take photos or detect the environment."
        ),
    )

    override fun getPermission(permissionName: String): Result<PermissionData> {
        val searchedPermissionData = permissionsMap[permissionName]

        if (searchedPermissionData == null) {
            return Result.failure(NotFoundException("There's no info about this permission. Add info to the repository"))
        }

        return Result.success(searchedPermissionData)
    }

}