package com.live.life.intoxication.dreams

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {

    const val STORAGE_PERMISSION_CODE = 1001

    private val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10+ 使用 MediaStore 保存图片到公共目录不需要权限
        emptyArray<String>()
    } else {
        // Android 9 及以下需要存储权限
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    fun hasStoragePermission(context: Context): Boolean {
        // Android 10+ 不需要存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }

        return STORAGE_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestStoragePermission(activity: Activity) {
        // Android 10+ 不需要申请存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return
        }

        ActivityCompat.requestPermissions(
            activity,
            STORAGE_PERMISSIONS,
            STORAGE_PERMISSION_CODE
        )
    }

    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        // Android 10+ 不需要存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return false
        }

        return STORAGE_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    fun showPermissionDeniedDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Permission denied")
            .setMessage("Need to store permissions to save pictures, please manually enable permissions in settings")
            .setPositiveButton("Go to Settings") { _, _ ->
                openAppSettings(context)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}