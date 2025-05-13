package com.example.drawarcp.domain.interfaces

import com.example.drawarcp.data.models.PermissionData

interface IPermissionsRepository {
    fun getPermission(permission: String): Result<PermissionData>
}