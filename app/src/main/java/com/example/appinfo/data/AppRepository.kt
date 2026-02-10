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

    fun getInstalledAppMetas(context: Context): List<InstalledApp> {
        Log.d(TAG, "getInstalledAppMetas: Начинаем загрузку списка приложений.")
        val packageManager = context.packageManager
        val apps = packageManager.getInstalledApplications(0)
        val result = apps.mapNotNull { appInfo ->
            try {
                val pkgName = appInfo.packageName
                val pkgInfo = packageManager.getPackageInfo(pkgName, 0)
                val versionName = pkgInfo.versionName ?: "N/A"


                val icon: Drawable? = try {
                    appInfo.loadIcon(packageManager)
                } catch (e: PackageManager.NameNotFoundException) {

                    Log.w(TAG, "getInstalledAppMetas: Иконка для $pkgName требует удалённый пакет.", e)
                    null
                } catch (e: Exception) {
                    Log.w(TAG, "getInstalledAppMetas: Не удалось загрузить иконку для $pkgName", e)
                    null
                }
                // ---

                InstalledApp(
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    packageName = pkgName,
                    versionName = versionName,
                    apkCheckSum = null,
                    icon = icon
                )
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "getInstalledAppMetas: Информация о приложении ${appInfo.packageName} не найдена, пропускаем.", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "getInstalledAppMetas: Неожиданная ошибка при получении данных для ${appInfo.packageName}", e)
                null
            }
        }
        Log.d(TAG, "getInstalledAppMetas: Загружено ${result.size} приложений.")
        return result
    }


    fun calculateApkChecksum(context: Context, packageName: String): String? {
        Log.d(TAG, "calculateApkChecksum: Начинаем вычисление чексуммы для $packageName")
        return try {
            val packageManager = context.packageManager
            val pkgInfo = packageManager.getPackageInfo(packageName, 0)
            val apkPath = pkgInfo.applicationInfo?.sourceDir ?: run {
                Log.w(TAG, "calculateApkChecksum: Путь к APK для $packageName не найден.")
                return null
            }
            Log.d(TAG, "calculateApkChecksum: Путь к APK: $apkPath")
            val checksum = calculateSHA256(File(apkPath))
            Log.d(TAG, "calculateApkChecksum: Вычисленная чексумма: $checksum")
            checksum
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "calculateApkChecksum: Информация о приложении $packageName не найдена.", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "calculateApkChecksum: Ошибка при вычислении чексуммы для $packageName", e)
            null
        }
    }

    private fun calculateSHA256(file: File): String? {
        Log.d(TAG, "calculateSHA256: Начинаем вычисление SHA-256 для файла: ${file.absolutePath}")
        return try {
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
            Log.d(TAG, "calculateSHA256: Чексумма для ${file.name}: $checksum")
            checksum
        } catch (e: java.io.IOException) {
            Log.e(TAG, "calculateSHA256: Ошибка ввода-вывода при чтении файла: ${file.absolutePath}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "calculateSHA256: Неожиданная ошибка при вычислении чексуммы для файла: ${file.absolutePath}", e)
            null
        }
    }
}