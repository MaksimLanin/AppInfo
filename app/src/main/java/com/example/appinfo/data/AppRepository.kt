package com.example.appinfo.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import com.example.appinfo.model.InstalledApp
import java.io.File
import java.security.MessageDigest

private const val TAG = "AppRepository"

object AppRepository {

    sealed class Result<T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error<T>(val exception: Exception) : Result<T>()
        data class Loading<T>(val partialData: T? = null) : Result<T>()
    }

    fun getInstalledAppMetas(context: Context): Result<List<InstalledApp>> {
        Log.d(TAG, "getInstalledAppMetas called.")
        return try {
            val packageManager = context.packageManager
            val apps = packageManager.getInstalledApplications(0)
            val result = apps.mapNotNull { appInfo ->
                try {
                    val pkgName = appInfo.packageName
                    val pkgInfo = packageManager.getPackageInfo(pkgName, 0)
                    val versionName = pkgInfo.versionName ?: "N/A"
                    val icon = appInfo.loadIcon(packageManager)
                    InstalledApp(
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        packageName = pkgName,
                        versionName = versionName,
                        apkCheckSum = null,
                        icon = icon
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting metadata for app: ${appInfo.packageName}", e)
                    null
                }
            }
            Log.d(TAG, "getInstalledAppMetas returning ${result.size} apps.")
            Result.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Critical error loading app list", e)
            Result.Error(e)
        }
    }

    fun calculateApkChecksum(context: Context, packageName: String): Result<String?> {
        Log.d(TAG, "calculateApkChecksum called for: $packageName")
        return try {
            val packageManager = context.packageManager
            val pkgInfo = packageManager.getPackageInfo(packageName, 0)
            val apkPath = pkgInfo.applicationInfo?.sourceDir ?: run {
                Log.w(TAG, "sourceDir is null for package: $packageName")
                return Result.Success(null)
            }
            Log.d(TAG, "APK path for $packageName: $apkPath")
            val checksum = calculateSHA256(File(apkPath))
            Log.d(TAG, "Calculated checksum for $packageName: $checksum")
            Result.Success(checksum)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating checksum for $packageName", e)
            Result.Error(e)
        }
    }

    private fun calculateSHA256(file: File): String {
        Log.d(TAG, "Starting SHA-256 calculation for file: ${file.absolutePath}")
        val md = MessageDigest.getInstance("SHA-256")
        var bytesProcessed = 0L
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
                bytesProcessed += bytesRead.toLong()
            }
        }
        val digest = md.digest()
        val checksum = digest.joinToString("") { "%02x".format(it) }
        Log.d(TAG, "SHA-256 calculation completed for ${file.name}, checksum: $checksum")
        return checksum
    }
}
