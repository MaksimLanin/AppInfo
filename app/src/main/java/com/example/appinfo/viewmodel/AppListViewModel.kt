package com.example.appinfo.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appinfo.data.AppRepository
import com.example.appinfo.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


private const val TAG = "AppListViewModel"

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val _appListResult = MutableStateFlow<AppRepository.Result<List<InstalledApp>>>(AppRepository.Result.Loading())
    val appListResult: StateFlow<AppRepository.Result<List<InstalledApp>>> = _appListResult

    private val context = application.applicationContext

    init {
        loadAppMetas()
    }

    private fun loadAppMetas() {
        viewModelScope.launch(Dispatchers.Default) {
            Log.d(TAG, "Starting to load app metadata...")
            val result = AppRepository.getInstalledAppMetas(context)
            _appListResult.value = result
            Log.d(TAG, "Loaded app metadata result: $result")
        }
    }

    fun updateApkChecksum(packageName: String) {
        Log.d(TAG, "updateApkChecksum called for: $packageName")
        viewModelScope.launch(Dispatchers.Default) {
            Log.d(TAG, "Starting checksum calculation for: $packageName")
            val result = AppRepository.calculateApkChecksum(context, packageName)
            Log.d(TAG, "Checksum calculation finished for: $packageName, result: $result")
            if (result is AppRepository.Result.Success) {
                val checksum = result.data
                val currentListResult = _appListResult.value
                if (currentListResult is AppRepository.Result.Success) {
                    val updatedList = currentListResult.data.map { app ->
                        if (app.packageName == packageName) {
                            Log.d(TAG, "Updating checksum for app: $packageName to: $checksum")
                            app.copy(apkCheckSum = checksum)
                        } else {
                            app
                        }
                    }
                    _appListResult.value = AppRepository.Result.Success(updatedList)
                    Log.d(TAG, "Updated app list in ViewModel state after checksum calc.")
                }
            } else if (result is AppRepository.Result.Error) {

                Log.e(TAG, "Failed to calculate checksum for $packageName", result.exception)
                // для простоты, не обновляем список, если вычисление не удалось
            }
        }
    }

    fun retryLoadApps() {
        // сбрасываем состояние на Loading
        _appListResult.value = AppRepository.Result.Loading()
        // снова запускаем загрузку
        loadAppMetas()
    }

}