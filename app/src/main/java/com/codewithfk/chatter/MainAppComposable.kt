package com.codewithfk.chatter

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codewithfk.chatter.feature.auth.signin.SignInScreen
import com.codewithfk.chatter.feature.auth.signup.SignUpScreen
import com.codewithfk.chatter.feature.chat.ChatScreen
import com.codewithfk.chatter.feature.home.HomeScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainApp() {
    Surface(modifier = Modifier.fillMaxSize()) {
        val navController = rememberNavController()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val start = if (currentUser != null) "home" else "login"
        NavHost(navController = navController, startDestination = start) {

            composable("login") {
                SignInScreen(navController)
            }
            composable("signup") {
                SignUpScreen(navController)
            }
            composable("home") {
                HomeScreen(navController)
            }
            composable("chat/{channelId}&{channelName}", arguments = listOf(
                navArgument("channelId") {
                    type = NavType.StringType
                },
                navArgument("channelName") {
                    type = NavType.StringType
                }
            )) {
                val channelId = it.arguments?.getString("channelId") ?: ""
                val channelName = it.arguments?.getString("channelName") ?: ""
                ChatScreen(navController, channelId,channelName)
            }
        }
    }
}