package com.gthncz.audiovisuailzer.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtil {

    @JvmStatic
    fun checkPermissionGranted(context: Context, vararg permissions: String): Boolean {
        if (permissions.isEmpty()) {
            return true
        }
        return permissions.all {permission ->
            val result = ContextCompat.checkSelfPermission(context, permission)
            result == PackageManager.PERMISSION_GRANTED
        }
    }

    @JvmStatic
    fun registerRequestPermissionLauncher(activity: ComponentActivity, vararg permissions: String, callback: ActivityResultCallback<Map<String, Boolean>>) =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            callback
        )
}