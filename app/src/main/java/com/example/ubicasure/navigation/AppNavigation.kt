package com.example.ubicasure.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ubicasure.screens.ChatScreen
import com.example.ubicasure.screens.EditProfileScreen
import com.example.ubicasure.screens.EmergencyContactsScreen
import com.example.ubicasure.screens.Login.ForgotPasswordScreen
import com.example.ubicasure.screens.Login.LoginScreen
import com.example.ubicasure.screens.Login.LoginViewModel
import com.example.ubicasure.screens.Login.RegisterScreen
import com.example.ubicasure.screens.MapScreen
import com.example.ubicasure.screens.MessagesScreen
import com.example.ubicasure.screens.SettingsScreen
import com.example.ubicasure.viewModels.SettingsViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    usuario: String,
    viewModel: LoginViewModel,
    signOut: () -> Unit
) {
    val startDestination = if (usuario == "sin usuario") "login" else "map"
    val settingsViewModel: SettingsViewModel = composeViewModel()

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
            MapScreen(
                navController = navController,
                usuario = usuario,
                settingsViewModel = settingsViewModel
            )
        }

        composable("settings") {
            SettingsScreen(
                navController = navController,
                settingsViewModel = settingsViewModel,
                signOut = signOut
            )
        }

        composable("emergencyContacts") {
            EmergencyContactsScreen(navController = navController)
        }

        composable("editProfile") {
            EditProfileScreen(
                navController = navController,
                loginViewModel = viewModel
            )
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
            val userProfile = viewModel.userProfile.collectAsState().value
            val senderName = userProfile
                ?.fullName
                ?.takeIf { it.isNotBlank() }
                ?: usuarioArg

            MessagesScreen(
                chatId = chatId,
                usuario = usuarioArg,
                senderName = senderName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
