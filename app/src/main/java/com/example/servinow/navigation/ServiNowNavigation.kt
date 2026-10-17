package com.example.servinow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.servinow.features.community.CommunityViewModel

sealed interface NavigationEvent {
    data class Open(val route: String) : NavigationEvent
    data object Back : NavigationEvent
}

object Routes {
    const val LOGIN = "login"
    const val WELCOME = "welcome"
    val tabs = setOf("home", "mine", "account", "notifications", "moderation")
    val all = setOf(
        "welcome", "onboarding", "login", "mod-login", "register", "recovery", "sent", "new-password",
        "home", "empty", "filters", "map", "location", "detail", "comments",
        "create", "create-details", "post-location", "preview", "submitted", "mine",
        "delete-post", "offers", "confirm-offer", "tracking", "finish", "review",
        "offer", "my-offers", "worker", "profile", "cv", "professional",
        "notifications", "dashboard", "achievements", "account", "edit-account",
        "delete-account", "moderation", "mod-detail", "reject", "mod-finish", "error",
    )
}

/** Navigation owns the back stack; the ViewModel only requests navigation after actions. */
@Composable
fun NavigationEffects(controller: NavHostController, vm: CommunityViewModel) {
    LaunchedEffect(controller, vm) {
        vm.navigation.collect { event ->
            when (event) {
                NavigationEvent.Back -> {
                    if (controller.previousBackStackEntry != null) controller.popBackStack()
                }
                is NavigationEvent.Open -> {
                    require(event.route in Routes.all) { "Unknown destination: ${event.route}" }
                    if (controller.currentDestination?.route != event.route) {
                        val current = controller.currentDestination?.route
                        controller.navigate(event.route) {
                            launchSingleTop = true
                            if (event.route == Routes.LOGIN ||
                                (current == "onboarding" && event.route == "register") ||
                                (current in setOf("login", "mod-login") && event.route in Routes.tabs)) {
                                // Never return to an authenticated screen after logout, or to login after entry.
                                popUpTo(controller.graph.id) { inclusive = false }
                            } else if (event.route in Routes.tabs) {
                                val root = if (vm.state.value.me?.role == com.example.servinow.domain.model.MemberRole.MODERATOR) "moderation" else "home"
                                popUpTo(root) { inclusive = false }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiNowNavHost(controller: NavHostController, modifier: Modifier = Modifier, content: @Composable (String) -> Unit) {
    NavHost(controller, startDestination = Routes.WELCOME, modifier = modifier) {
        Routes.all.forEach { route -> composable(route) { content(route) } }
    }
}
