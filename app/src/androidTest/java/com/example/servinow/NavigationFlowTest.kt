package com.example.servinow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.example.servinow.features.community.CommunityViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NavigationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val vm get() = ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    private fun at(route: String) {
        compose.waitUntil(10_000) { vm.ui.value.page == route }
        compose.waitForIdle()
    }
    private fun click(text: String) = compose.onAllNodesWithText(text).onLast().performScrollTo().performClick()

    @Test fun recoveryBackTabsRotationAndLogout() {
        compose.onNodeWithText("Ya tengo cuenta · Iniciar sesión").performScrollTo().performClick()
        at("login")
        click("Olvidé mi contraseña")
        at("recovery")
        compose.onNodeWithContentDescription("Volver").performClick()
        at("login")
        compose.runOnIdle { vm.login("andres@gmail.com", "ServiNow1!") }
        at("home")
        compose.onNodeWithText("Cuenta").performClick()
        at("account")
        click("Mis estadísticas")
        at("dashboard")
        compose.onNodeWithContentDescription("Volver").performClick()
        at("account")
        compose.activityRule.scenario.recreate()
        at("account")
        compose.onNodeWithText("Inicio").performClick()
        at("home")
        click("Publicar un trabajo")
        at("create")
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        at("home")
        compose.onNodeWithText("Cuenta").performClick()
        at("account")
        click("Cambiar usuario / salir")
        at("login")
        compose.runOnIdle { vm.back() }
        compose.waitForIdle()
        assertEquals("login", vm.ui.value.page)
        compose.onNodeWithText("Bienvenido de nuevo.").assertIsDisplayed()
    }
}
