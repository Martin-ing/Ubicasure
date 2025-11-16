package com.example.ubicasure.navigation

import androidx.navigation.compose.*
import com.example.ubicasure.screens.Login.LoginScreen
import com.example.ubicasure.screens.Login.LoginViewModel
import com.example.ubicasure.screens.Login.RegisterScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.ubicasure.screens.Login.ForgotPasswordScreen

@Composable
fun NavGraph(viewModel: LoginViewModel, navController: NavHostController, onLoginSuccess: () -> Unit) {

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                viewModel,
                onLoginSuccess = {
                    onLoginSuccess()
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
            RegisterScreen(navController = navController
            )
        }
        composable ("forgotPassword") {
            ForgotPasswordScreen(navController = navController)
        }
    }
}