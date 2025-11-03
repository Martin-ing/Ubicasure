package com.example.ubicasure.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ubicasure.screens.MapScreen
import com.example.ubicasure.screens.ChatScreen
import com.example.ubicasure.screens.MessagesScreen

sealed class Screen(val route: String) {
    object Map : Screen("map")
    object Chat : Screen("chat")
    object Messages : Screen("messages/{chatId}/{usuario}")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    usuario: String,
    signOut: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route
    ) {
        composable(Screen.Map.route) {
            MapScreen(navController, usuario)
        }


        composable(Screen.Chat.route) {
            ChatScreen(navController, usuario)
        }

        composable(
            route = Screen.Messages.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("usuario") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val usuarioArg = backStackEntry.arguments?.getString("usuario") ?: ""
            MessagesScreen(
                chatId = chatId,
                usuario = usuarioArg,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
