package com.example.ubicasure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ubicasure.ui.theme.UbicasureTheme
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.ubicasure.navigation.AppNavigation
import com.example.ubicasure.screens.Login.LoginScreen
import com.example.ubicasure.screens.Login.LoginViewModel
import com.example.ubicasure.navigation.NavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UbicasureTheme {
                val usuarioViewModel: LoginViewModel = viewModel()
                val usuario by usuarioViewModel.usuario.collectAsState()
                val navController = rememberNavController()

                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        navController = navController,
                        usuario = usuario,
                        viewModel = usuarioViewModel,
                        signOut = usuarioViewModel::signOut
                    )
                }
            }
        }
    }
}
