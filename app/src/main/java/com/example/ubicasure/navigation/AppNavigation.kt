package com.example.ubicasure.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ubicasure.screens.Login.LoginScreen
import com.example.ubicasure.screens.Login.RegisterScreen
import com.example.ubicasure.screens.Login.ForgotPasswordScreen
import com.example.ubicasure.screens.Login.LoginViewModel
import com.example.ubicasure.screens.MapScreen
import com.example.ubicasure.screens.ChatScreen
import com.example.ubicasure.screens.MessagesScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    usuario: String,
    viewModel: LoginViewModel,
    signOut: () -> Unit
) {
    val startDestination = if (usuario == "sin usuario") "login" else "map"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable("login") {
            LoginScreen(
                viewModel,
                onLoginSuccess = {

                    navController.navigate("map") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onNavigateToForgotPassword = {
                    navController.navigate("forgotPassword")
                }
            )
        }

        composable("register") {
            RegisterScreen(navController = navController)
        }

        composable("forgotPassword") {
            ForgotPasswordScreen(navController = navController)
        }

        composable("map") {
            MapScreen(navController, usuario, signOut)
        }

        composable("chat") {
            ChatScreen(navController, usuario)
        }

        composable(
            "messages/{chatId}/{usuario}",
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
