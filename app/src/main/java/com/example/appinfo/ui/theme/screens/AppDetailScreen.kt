package com.example.appinfo.ui.theme.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.appinfo.model.InstalledApp
import com.example.appinfo.viewmodel.AppListViewModel
import kotlinx.coroutines.flow.collectLatest


private const val TAG = "AppDetailScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    navController: NavController,
    viewModel: AppListViewModel = viewModel()
) {
    val context = LocalContext.current
    var app by remember { mutableStateOf<InstalledApp?>(null) }
    var isLoadingChecksum by remember { mutableStateOf(false) }
    var hasAttemptedCalculation by remember { mutableStateOf(false) }

    LaunchedEffect(packageName) {
        Log.d(TAG, "LaunchedEffect started for packageName: $packageName")

        viewModel.appList.collectLatest { list ->
            Log.d(TAG, "Received updated app list, searching for: $packageName")
            val updatedApp = list.find { it.packageName == packageName }
            if (updatedApp != null) {
                Log.d(TAG, "Found updated app: $updatedApp")
                app = updatedApp

                if (updatedApp.apkCheckSum == null && !hasAttemptedCalculation) {
                    Log.d(
                        TAG,
                        "Checksum is null and not yet calculated, initiating calculation for: $packageName"
                    )
                    isLoadingChecksum = true
                    hasAttemptedCalculation = true
                    viewModel.updateApkChecksum(packageName)
                } else if (updatedApp.apkCheckSum != null) {
                    Log.d(TAG, "Checksum is now available: ${updatedApp.apkCheckSum}")
                    isLoadingChecksum = false
                } else {
                    Log.d(
                        TAG,
                        "Checksum is still null, but calculation attempt was made or is pending."
                    )
                }
            } else {
                Log.d(TAG, "Updated app with packageName $packageName not found in list.")
                if (app != null) {
                    app = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app?.appName ?: "App Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            app?.let { app ->
                AsyncImage(
                    model = app.icon,
                    contentDescription = "${app.appName} icon",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Версия: ${app.versionName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Пакет: ${app.packageName}",
                    style = MaterialTheme.typography.bodyMedium
                )

                val checksumText = when {
                    isLoadingChecksum -> "SHA-256: Calculating..."
                    app.apkCheckSum != null -> "SHA-256: ${app.apkCheckSum}"
                    else -> "SHA-256: Not available"
                }
                Text(text = checksumText, style = MaterialTheme.typography.bodySmall)

                //  индикатор загрузки SHA
                if (isLoadingChecksum) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { launchApp(context, app.packageName) },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Открыть приложение")
                }
            } ?: run {
                Text("Приложение не найдено.")
            }
        }
    }
}

private fun launchApp(context: Context, packageName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        try {
            context.startActivity(launchIntent)
        } catch (e: ActivityNotFoundException) {

        }
    }
}