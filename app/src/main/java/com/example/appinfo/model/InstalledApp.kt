package com.example.appinfo.model

import android.graphics.drawable.Drawable

data class InstalledApp(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val apkCheckSum: String? = null,
    val icon: Drawable? = null
)