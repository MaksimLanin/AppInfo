package com.example.appinfo.ui.theme.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.appinfo.ui.theme.screens.AppDetailScreen
import com.example.appinfo.ui.theme.screens.AppListScreen

@Composable

fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "app_list") {
        composable("app_list") {
            AppListScreen(navController = navController)
        }
        composable(
            "app_detail/{packageName}",
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName")
            packageName?.let { AppDetailScreen(packageName = it, navController = navController) }
        }
    }
}