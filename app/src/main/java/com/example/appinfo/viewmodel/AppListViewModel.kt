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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "AppListViewModel"

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val _appMap = MutableStateFlow<Map<String, InstalledApp>>(emptyMap())
    val appMap: StateFlow<Map<String, InstalledApp>> = _appMap.asStateFlow()

    // AtomicBoolean для потокобезопасной проверки, была ли загрузка начата
    private val isLoadStarted = AtomicBoolean(false)

    // StateFlow для списка приложений
    val appList: StateFlow<List<InstalledApp>> = _appMap
        .map { it.values.toList() }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val context = application.applicationContext

    init {
        startLoadingAppMetas()
    }

    private fun startLoadingAppMetas() {
        if (isLoadStarted.compareAndSet(false, true)) {
            Log.d(TAG, "startLoadingAppMetas: Загрузка начата.")
            viewModelScope.launch(Dispatchers.Default) {
                Log.d(TAG, "startLoadingAppMetas: Начинаем загрузку метаданных приложений...")
                val apps = AppRepository.getInstalledAppMetas(context)
                val appMap = apps.associateBy { it.packageName }
                Log.d(TAG, "startLoadingAppMetas: Загружено ${apps.size} метаданных приложений.")
                _appMap.value = appMap
                Log.d(TAG, "startLoadingAppMetas: Загрузка завершена, данные обновлены в состоянии.")
            }
        } else {
            Log.d(TAG, "startLoadingAppMetas: Загрузка уже была начата, пропускаем.")
        }
    }


    fun updateApkChecksum(packageName: String) {
        // Проверка происходит в AppDetailScreen, чтобы избежать дубликатов
        Log.d(TAG, "updateApkChecksum: Вызван для $packageName")
        viewModelScope.launch(Dispatchers.Default) {
            // Проверяем состояние *сразу* в корутине, на случай, если данные изменились между проверкой в UI и запуском
            val currentApp = _appMap.value[packageName]
            if (currentApp?.apkCheckSum != null) {
                Log.d(TAG, "updateApkChecksum: Чексумма для $packageName уже существует, пропускаем вычисление.")
                return@launch
            }

            Log.d(TAG, "updateApkChecksum: Начинаем вычисление чексуммы для: $packageName")
            val checksum = AppRepository.calculateApkChecksum(context, packageName)
            Log.d(TAG, "updateApkChecksum: Вычисление чексуммы завершено для: $packageName, результат: $checksum")
            if (checksum != null) {
                val currentMap = _appMap.value
                val existingApp = currentMap[packageName]
                if (existingApp != null) {
                    val updatedApp = existingApp.copy(apkCheckSum = checksum)
                    val newMap = currentMap.toMutableMap().apply {
                        this[packageName] = updatedApp
                    }
                    _appMap.value = newMap
                    Log.d(TAG, "updateApkChecksum: Обновлена чексумма для приложения: $packageName в карте.")
                } else {
                    Log.w(TAG, "updateApkChecksum: Приложение с packageName $packageName исчезло во время обновления чексуммы.")
                }
            } else {
                Log.e(TAG, "updateApkChecksum: Вычисление чексуммы вернуло null для: $packageName")
            }
        }
    }
}