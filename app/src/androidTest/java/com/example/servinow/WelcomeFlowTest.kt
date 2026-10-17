package com.example.servinow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class WelcomeFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()

    @Test fun walkthroughPreservesStepAndOpensRegistration() {
        compose.onNodeWithContentDescription("Logo de ServiNow: hogar y rayo").assertIsDisplayed()
        click("Empezar")
        compose.onNodeWithText("Paso 1 de 3").assertIsDisplayed()
        click("Siguiente")
        compose.onNodeWithText("Comparte tu talento").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Paso 2 de 3").assertIsDisplayed()
        click("Anterior")
        compose.onNodeWithText("Paso 1 de 3").assertIsDisplayed()
        click("Siguiente")
        click("Siguiente")
        compose.onNodeWithText("Elige con información").assertIsDisplayed()
        click("Crear mi cuenta")
        compose.onNodeWithText("Nombre completo").assertIsDisplayed()
    }

    @Test fun skipOpensLogin() {
        click("Empezar")
        click("Omitir")
        compose.onNodeWithText("Bienvenido de nuevo.").assertIsDisplayed()
    }
}
