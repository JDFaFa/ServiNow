package com.example.servinow

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.servinow.domain.model.*
import com.example.servinow.features.community.CommunityViewModel
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Screenshots come from the real emulator. Navigation is driven through the same ViewModel as UI
 * events.
 */
class DeliveryEvidenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val vm
        get() = ViewModelProvider(compose.activity)[CommunityViewModel::class.java]

    private fun act(action: CommunityViewModel.() -> Unit) {
        compose.runOnIdle { vm.action() }
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        Thread.sleep(400)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        assertNotNull(bitmap)
        val dir =
            File(compose.activity.getExternalFilesDir(null), "delivery-evidence").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        // Preserve evidence outside app storage: Gradle removes the test app after completion.
        fun shell(command: String): ByteArray = instrumentation.uiAutomation.executeShellCommand(command).use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
        }
        shell("mkdir -p /data/local/tmp/servinow-evidence")
        shell("cp ${File(dir, "$name.png").absolutePath} /data/local/tmp/servinow-evidence/$name.png")
        val exported = shell("cat /data/local/tmp/servinow-evidence/$name.png")
        assertTrue("Screenshot export must contain PNG data", exported.size > 1000)

    }

    private fun page(page: String, name: String = page) {
        act { go(page) }
        shot(name)
    }

    @Test
    fun captureFunctionalDelivery() {
        shot("00-welcome")
        compose.onNodeWithText("Empezar").performScrollTo().performClick()
        shot("00-onboarding-1")
        compose.onNodeWithText("Siguiente").performScrollTo().performClick()
        shot("00-onboarding-2")
        compose.onNodeWithText("Siguiente").performScrollTo().performClick()
        shot("00-onboarding-3")
        compose.onNodeWithText("Ya tengo cuenta").performScrollTo().performClick()
        shot("01-login")
        page("register", "02-register")
        compose.onAllNodesWithText("Crear cuenta").onLast().performScrollTo().performClick()
        compose.onNodeWithText("Escribe un nombre de 2 a 100 caracteres.").assertIsDisplayed()
        shot("50-empty-form")
        act { message(null) }
        compose.onNodeWithText("Nombre completo").performScrollTo().performTextInput("Usuario Evaluacion")
        compose.onNodeWithText("Correo electrónico").performScrollTo().performTextInput("correo-invalido")
        compose.onAllNodesWithText("Crear cuenta").onLast().performScrollTo().performClick()
        compose.onNodeWithText("Escribe un correo válido, como nombre@gmail.com").assertIsDisplayed()
        shot("51-invalid-email")
        act { message(null) }
        compose.onNodeWithText("Correo electrónico").performScrollTo().performTextReplacement("evaluacion@gmail.com")
        compose.onNodeWithText("Contraseña", substring = false).performScrollTo().performTextInput("123")
        compose.onAllNodesWithText("Crear cuenta").onLast().performScrollTo().performClick()
        shot("52-password-rules")
        act { message(null) }
        compose.onNodeWithText("Contraseña", substring = false).performScrollTo().performTextReplacement("Prueba123!")
        compose.onNodeWithText("Confirmar contraseña").performScrollTo().performTextInput("Otra123!")
        compose.onAllNodesWithText("Crear cuenta").onLast().performScrollTo().performClick()
        compose.onNodeWithText("Las contraseñas no coinciden.").assertIsDisplayed()
        shot("53-password-mismatch")
        act { message(null) }
        compose.onNodeWithText("Confirmar contraseña").performScrollTo().performTextReplacement("Prueba123!")
        compose.onNodeWithText("Crear cuenta", substring = false).let {
            /* Header and action share text; choose button node. */
        }
        compose.onAllNodesWithText("Crear cuenta").onLast().performScrollTo().performClick()
        compose.waitForIdle()
        shot("03-registration-success")
        act { message(null) }
        compose.onNodeWithText("Correo electrónico").performTextInput("evaluacion@gmail.com")
        compose.onNodeWithText("Contraseña", substring = false).performTextInput("Incorrecta123!")
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        shot("54-login-error")
        act { message(null) }
        compose.onNodeWithText("Contraseña", substring = false).performScrollTo().performTextReplacement("Prueba123!")
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("evaluacion@gmail.com", vm.state.value.me?.email)
        act {
            startPost()
            draft(ui.value.draft!!.copy(title = "Ajustar una ventana", description = "La ventana de la sala no cierra y necesito ajustar las bisagras.", budget = 50000, hasImage = true))
            savePost()
            message(null)
            go("notifications")
        }
        shot("55-achievement-notice")
        act { logout() }
        page("recovery", "05-recovery")
        act { recovery("valentina@gmail.com") }
        shot("06-sent")
        page("new-password", "07-password")
        act { reset("Nueva123!", "Nueva123!") }
        shot("07-password-saved")
        act {
            message(null)
            login("valentina@gmail.com", "Nueva123!")
        }
        shot("08-home")
        page("filters", "09-filters")
        page("location", "10-location")
        Thread.sleep(1500)
        shot("10-location")
        act { openPost("p1") }
        shot("11-detail")
        act { go("comments") }
        compose.onNodeWithText("Agregar comentario").performScrollTo()
        shot("12-comments")
        act {
            startPost()
            draft(
                ui.value.draft!!.copy(
                    title = "Revisión de ducha",
                    description = "Necesito revisar la llave y cambiar el empaque de la ducha.",
                    budget = 85000,
                    hasImage = true,
                )
            )
        }
        shot("13-create")
        page("create-details", "14-create-details")
        page("post-location", "15-post-location")
        page("preview", "16-preview")
        act { savePost() }
        assertEquals(PostStatus.PENDING, vm.state.value.posts.last().status)
        shot("17-submitted")
        val created = vm.state.value.posts.last().id
        page("mine", "18-mine")
        act { editPost(state.value.posts.last()) }
        shot("19-edit")
        act { openPost("p4") }
        shot("20-rejected")
        act { openPost("p1", "offers") }
        shot("21-offers")
        act { selectOffer("o1") }
        shot("22-confirm")
        act {
            accept()
            message(null)
        }
        assertEquals("andres", vm.state.value.posts.first { it.id == "p1" }.assigned)
        shot("23-tracking")
        page("finish", "24-finish")
        act {
            finish()
            message(null)
        }
        page("review", "25-review")
        compose.onAllNodesWithText("☆").onLast().performClick()
        compose.onNodeWithText("Tu reseña").performTextInput("Muy puntual y buen trabajo.")
        compose.onNodeWithText("Publicar reseña").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(vm.state.value.reviews.any { it.post == "p1" && it.stars == 5 })
        act { message(null) }
        act { openPost(created, "delete-post") }
        shot("26-delete-post")
        act {
            logout()
            login("andres@gmail.com", "ServiNow1!")
            go("worker")
        }
        shot("27-worker")
        act { openPost("p3", "offer") }
        shot("28-offer")
        act {
            offer("130000", "Ajuste de puertas y bisagras incluido.", "Mañana por la tarde")
            message(null)
        }
        shot("29-my-offers")
        act { profile("andres") }
        compose.onNodeWithText("Lo que dice la comunidad").performScrollTo()
        shot("30-profile")
        page("cv", "31-cv")
        page("professional", "32-professional")
        act {
            openPost("p3")
            comment("Puedo revisar las bisagras mañana.")
            vote()
        }
        act {
            logout()
            login("valentina@gmail.com", "Nueva123!")
        }
        page("notifications", "33-notifications")
        compose.onAllNodesWithText("Marcar leído").onFirst().performScrollTo().performClick()
        shot("56-read-notice")
        compose.onAllNodesWithText("Ver detalle").onFirst().performScrollTo().performClick()
        shot("57-notice-detail")
        page("dashboard", "34-dashboard")
        compose.onNodeWithText("Tu aporte a la comunidad").performScrollTo()
        shot("34-dashboard-bottom")
        page("achievements", "35-achievements")
        compose.onNodeWithText("Conocer todos los niveles").performScrollTo().performClick()
        compose.onNodeWithText("Referente · 1500 o más puntos").performScrollTo()
        shot("35-levels")
        page("account", "36-account")
        page("edit-account", "37-edit-account")
        page("delete-account", "38-delete-account")
        act { logout() }
        page("mod-login", "39-mod-login")
        act { login("moderador@gmail.com", "Moderador1!", true) }
        shot("40-moderation")
        act { openPost("p2", "mod-detail") }
        shot("41-mod-detail")
        page("reject", "42-reject")
        act {
            reject("Describe el tipo de lámpara y la altura de instalación.")
            message(null)
        }
        assertEquals(PostStatus.REJECTED, vm.state.value.posts.first { it.id == "p2" }.status)
        act {
            openPost(created, "mod-detail")
            verify()
            message(null)
        }
        assertEquals(PostStatus.VERIFIED, vm.state.value.posts.first { it.id == created }.status)
        act { openPost("p3", "mod-finish") }
        shot("43-mod-finish")
        act {
            logout()
            login("valentina@gmail.com", "Nueva123!")
            go("home")
        }
        compose
            .onNodeWithText("Buscar un trabajo")
            .performScrollTo()
            .performTextInput("NoExiste123")
        compose.onNodeWithText("No encontramos trabajos con esos filtros.").performScrollTo()
        shot("44-empty")
        page("error", "45-error")
        act { go("notifications") }
        shot("46-notification-moderation")
    }
}
