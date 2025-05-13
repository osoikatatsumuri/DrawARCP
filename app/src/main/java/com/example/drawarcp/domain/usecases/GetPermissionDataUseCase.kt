package com.example.drawarcp.domain.usecases

import com.example.drawarcp.data.models.PermissionData
import com.example.drawarcp.domain.interfaces.IPermissionsRepository

class GetPermissionDataUseCase(private val repository: IPermissionsRepository) {
    operator fun invoke(permission: String): Result<PermissionData> {
        return repository.getPermission(permission)
    }
}