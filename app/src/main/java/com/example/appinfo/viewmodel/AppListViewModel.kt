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

    private val _appList = MutableStateFlow<List<InstalledApp>>(emptyList())
    val appList: StateFlow<List<InstalledApp>> = _appList

    private val context = application.applicationContext

    init {
        loadAppMetas()
    }

    private fun loadAppMetas() {
        viewModelScope.launch(Dispatchers.Default) {
            Log.d(TAG, "Starting to load app metadata...")
            val apps = AppRepository.getInstalledAppMetas(context)
            Log.d(TAG, "Loaded ${apps.size} apps metadata.")
            _appList.value = apps
        }
    }

    fun updateApkChecksum(packageName: String) {
        Log.d(TAG, "updateApkChecksum called for: $packageName")
        viewModelScope.launch(Dispatchers.Default) {
            Log.d(TAG, "Starting checksum calculation for: $packageName")
            val checksum = AppRepository.calculateApkChecksum(context, packageName)
            Log.d(TAG, "Checksum calculation finished for: $packageName, result: $checksum")
            if (checksum != null) {
                val updatedList = _appList.value.map { app ->
                    if (app.packageName == packageName) {
                        Log.d(TAG, "Updating checksum for app: $packageName to: $checksum")
                        app.copy(apkCheckSum = checksum)
                    } else {
                        app
                    }
                }
                _appList.value = updatedList
                Log.d(TAG, "Updated app list in ViewModel state.")
            } else {
                Log.e(TAG, "Checksum calculation returned null for: $packageName")
            }
        }
    }

}