package com.example.servinow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.servinow.domain.model.MemberRole
import com.example.servinow.domain.model.PostStatus
import com.example.servinow.features.community.CommunityViewModel
import org.json.JSONObject
import org.junit.*
import java.io.File

/** Opt-in live-service checks. Supply live-role-config.json in app private files; never commit credentials. */
class LiveRoleFlowsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val vm get() = ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    private lateinit var config: JSONObject
    private fun text(id: Int) = compose.activity.getString(id)
    private fun act(block: CommunityViewModel.() -> Unit) = compose.runOnIdle { vm.block() }
    private fun at(page: String) { compose.waitUntil(45_000) { vm.ui.value.page == page }; compose.waitForIdle() }
    private fun click(id: Int) = compose.onAllNodesWithText(text(id)).onLast().performScrollTo().performClick()
    private fun input(id: Int, value: String) = compose.onNode(hasText(text(id)) and hasSetTextAction()).performScrollTo().performTextReplacement(value)
    @Before fun setup() {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "live-role-config.json")
        Assume.assumeTrue("Opt-in credentials required", file.exists())
        config = JSONObject(file.readText())
        compose.waitUntil(30_000) { vm.ready.value }
        act { message(null); logout() }; at("login")
    }
    private fun login(key: String) {
        val user = config.getJSONObject(key)
        input(R.string.ui_email_00ff, user.getString("email"))
        input(R.string.ui_password_6129, user.getString("password"))
        click(R.string.ui_sign_in_ef16)
        at(if(key == "admin") "moderation" else "home")
        Assert.assertEquals(if(key == "admin") MemberRole.MODERATOR else MemberRole.USER, vm.state.value.me?.role)
    }
    private fun open(id: String, page: String) {
        compose.waitUntil(30_000) { vm.state.value.posts.any { it.id == id } }
        act { openPost(id, page) }; at(page)
    }
    private fun shot(name: String) {
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir = File(compose.activity.filesDir,"live-role-evidence").apply { mkdirs() }
        File(dir,"$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
    @Test fun admin01LoginAndNavigation() {
        login("admin")
        shot("admin-01-bandeja")
        act { go("account") }; at("account")
        compose.onNodeWithText(config.getJSONObject("admin").getString("email")).assertExists()
        act { go("notifications") }; at("notifications")
        act { go("account") }; at("account")
        compose.onNodeWithText("Idioma:",substring=true).performScrollTo().performClick()
        compose.onNodeWithText("English",useUnmergedTree=true).performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Language: English").fetchSemanticsNodes().isNotEmpty() }
        act { go("moderation") }; at("moderation")
        compose.waitUntil(20_000) { vm.state.value.posts.any { it.status == PostStatus.PENDING } }
        compose.onNodeWithText(text(R.string.ui_no_posts_with_this_status_d7e3)).assertDoesNotExist()
        shot("admin-01-english-pending")
        act { go("account") }; at("account")
        compose.onNodeWithText("Language: English").performScrollTo().performClick()
        compose.onNodeWithText("Español",useUnmergedTree=true).performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Idioma: Español").fetchSemanticsNodes().isNotEmpty() }
        act { go("moderation") }; at("moderation")
        Assert.assertNull(vm.remoteError.value)
    }
    @Test fun admin02Approve() {
        login("admin"); val id = config.getString("approve")
        open(id,"mod-detail")
        compose.onNodeWithText(text(R.string.ui_mark_as_finished_ed23)).assertDoesNotExist()
        shot("admin-02-antes")
        click(R.string.ui_approve_post_c4cc)
        at("moderation")
        compose.waitUntil(30_000) { vm.state.value.posts.any { it.id==id && it.status==PostStatus.VERIFIED } }
        shot("admin-02-aprobado")
    }
    @Test fun admin03Reject() {
        login("admin"); val id = config.getString("reject")
        open(id,"mod-detail")
        click(R.string.ui_reject_with_reason_cd8f); at("reject")
        input(R.string.ui_rejection_reason_371a,"[PRUEBA UI] Agrega detalles sobre el servicio solicitado.")
        shot("admin-03-motivo")
        click(R.string.ui_confirm_rejection_567e); at("moderation")
        Assert.assertNull(vm.ui.value.message)
    }
    @Test fun normal01CreateAndSubmit() {
        input(R.string.ui_email_00ff,"servinow.noexiste." + System.currentTimeMillis() + "@gmail.com")
        input(R.string.ui_password_6129,"PruebaIncorrecta123!")
        click(R.string.ui_sign_in_ef16)
        compose.waitUntil(30_000) { vm.ui.value.message != null }
        Assert.assertEquals(text(R.string.auth_invalid_credentials),vm.ui.value.message)
        shot("normal-01-cuenta-inexistente")
        act { message(null) }
        act { go("register") }; at("register")
        input(R.string.ui_full_name_8229,"Prueba de cuenta duplicada")
        val existing = config.getJSONObject("client")
        input(R.string.ui_email_00ff,existing.getString("email"))
        input(R.string.ui_password_6129,existing.getString("password"))
        input(R.string.ui_confirm_password_02bf,existing.getString("password"))
        click(R.string.ui_create_account_45a1)
        compose.waitUntil(30_000) { vm.ui.value.message != null }
        Assert.assertEquals(text(R.string.ui_an_account_with_this_email_already_exists_1cf5),vm.ui.value.message)
        shot("normal-01-cuenta-existente")
        act { message(null); go("login") }; at("login")
        login("client")
        act { startPost() }; at("create")
        input(R.string.ui_job_title_d0ff,"Casa")
        click(R.string.ui_continue_8bec)
        compose.waitUntil(5_000) { vm.ui.value.message != null }
        shot("normal-01-validacion"); act { message(null) }
        input(R.string.ui_job_title_d0ff,"[PRUEBA UI] Reparación del grifo")
        click(R.string.ui_continue_8bec); at("create-details")
        input(R.string.ui_description_7059,"El grifo de la cocina gotea y requiere cambiar el empaque.")
        input(R.string.ui_budget_in_cop_b11f,"85000")
        act { draft(ui.value.draft!!.copy(imageUri=config.getString("image"),hasImage=true)) }
        click(R.string.ui_continue_8bec); at("post-location")
        Assert.assertEquals("Armenia",vm.ui.value.draft!!.city)
        val original = vm.ui.value.draft!!
        compose.onNodeWithTag("job-location-map").performScrollTo().performTouchInput { click(androidx.compose.ui.geometry.Offset(width * 0.7f, height * 0.35f)) }
        compose.waitUntil(15_000) { vm.ui.value.draft!!.latitude != original.latitude }
        shot("normal-01-mapa")
        click(R.string.ui_review_post_edfa); at("preview")
        click(R.string.ui_submit_for_review_b370); at("submitted")
        shot("normal-01-enviado")
        val id=vm.ui.value.draft!!.id
        compose.waitUntil(20_000) { vm.state.value.posts.any { it.id==id && it.status==PostStatus.PENDING } }
    }
    @Test fun normal02TrackingChatAndNotices() {
        login("worker"); val id = config.getString("crossPost")
        open(id,"detail")
        click(R.string.ui_send_an_offer_cf68); at("offer")
        input(R.string.ui_price_in_cop_ff89,"80000")
        input(R.string.ui_what_is_included_8c99,"[PRUEBA UI] Revisión y cambio del empaque del grifo.")
        input(R.string.ui_availability_cd96,"Mañana por la mañana")
        click(R.string.ui_send_offer_caf4); at("my-offers")
        compose.waitUntil(30_000) { vm.state.value.offers.any { it.post==id } }
        shot("normal-02-propuesta-enviada")
        act { logout() }; at("login"); login("client")
        open(id,"offers")
        compose.waitUntil(30_000) { vm.state.value.offers.any { it.post==id } }
        compose.onNodeWithText("[PRUEBA UI] Revisión y cambio del empaque del grifo.").assertExists()
        shot("normal-02-propuesta-recibida")
        click(R.string.ui_choose_worker_8452); at("confirm-offer")
        click(R.string.ui_confirm_worker_ba80); at("tracking")
        act { message(null) }; shot("normal-02-seguimiento")
        click(R.string.private_chat); at("chat")
        input(R.string.chat_message,"[PRUEBA UI] Confirmamos el servicio para mañana.")
        click(R.string.chat_send)
        compose.waitUntil(30_000) { vm.state.value.messages.any { it.post==id } }
        shot("normal-02-chat")
        act { go("tracking") }; at("tracking")
        click(R.string.ui_mark_as_finished_788c); at("finish")
        click(R.string.ui_confirm_completion_4467); at("tracking")
        compose.waitUntil(30_000) { vm.state.value.posts.any { it.id==id && it.status==PostStatus.FINISHED } }
        act { message(null); go("notifications") }; at("notifications")
        compose.waitUntil(20_000) { vm.state.value.notices.isNotEmpty() }
        act { markAll() }
        compose.waitUntil(20_000) { vm.state.value.notices.all { it.read } }
        shot("normal-02-avisos")
    }
    @Test fun normal03AccountAndLanguage() {
        login("client")
        act { go("account") }; at("account")
        compose.onNodeWithText("Idioma:",substring=true).performScrollTo().performClick()
        compose.onNodeWithText("English",useUnmergedTree=true).performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Language: English").fetchSemanticsNodes().isNotEmpty() }
        shot("normal-03-english")
        compose.activityRule.scenario.recreate(); at("account")
        compose.onNodeWithText("Language: English").performScrollTo().performClick()
        compose.onNodeWithText("Español",useUnmergedTree=true).performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Idioma: Español").fetchSemanticsNodes().isNotEmpty() }
        for (route in listOf("edit-account","professional","cv","worker","my-offers","mine","filters","location")) {
            act { go(route) }; at(route)
            Assert.assertNull(vm.ui.value.message)
        }
        act { go("dashboard") }; at("dashboard"); shot("normal-03-estadisticas")
        act { go("achievements") }; at("achievements"); shot("normal-03-logros")
        act { logout() }; at("login")
        Assert.assertNull(vm.state.value.me)
    }
}
