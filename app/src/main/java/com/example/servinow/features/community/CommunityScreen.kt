package com.example.servinow.features.community

import androidx.compose.ui.platform.testTag
import com.example.servinow.features.welcome.WelcomeScreen
import com.example.servinow.features.welcome.OnboardingScreen
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.servinow.navigation.NavigationEffects
import com.example.servinow.navigation.ServiNowNavHost
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.servinow.core.component.PhotoPicker
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.servinow.core.util.localizedLabel
import com.example.servinow.R
import com.example.servinow.core.component.PasswordVisibilityButton
import com.example.servinow.core.component.screenSpacing
import com.example.servinow.domain.model.*
import com.example.servinow.domain.validation.reputationLevel
import com.example.servinow.core.component.ServiceMap
import com.mapbox.geojson.Point
import com.example.servinow.features.recovery.RecoveryHeader
import java.text.NumberFormat
import java.util.Locale

private fun money(n: Long) =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(n) + " COP"

@Composable
private fun Title(s: String) {
    Text(s, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun Action(s: String, click: () -> Unit) = Action(s, true, click)

@Composable
private fun Action(s: String, enabled: Boolean, click: () -> Unit) {
    Button(
        click,
        Modifier.fillMaxWidth().heightIn(min = 50.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
    ) {
        Text(s)
    }
}

@Composable
private fun Link(s: String, click: () -> Unit) {
    OutlinedButton(click, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text(s) }
}

@Composable
private fun Field(
    s: String,
    value: String,
    change: (String) -> Unit,
    multi: Boolean = false,
    password: Boolean = false,
) {
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    var visible by rememberSaveable(s) { mutableStateOf(false) }
    OutlinedTextField(
        value,
        change,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(s) },
        shape = RoundedCornerShape(16.dp),
        singleLine = !multi,
        minLines = if (multi) 3 else 1,
        visualTransformation =
            if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon =
            if (password) {
                { PasswordVisibilityButton(visible) { visible = !visible } }
            } else null,
    )
}

@Composable
private fun Choice(label: String, value: String, values: List<String>, choose: (String) -> Unit) {
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    var open by remember { mutableStateOf(false) }
    Box {
        Link("$label: ${localizedLabel(resources, value)} ▾") { open = true }
        DropdownMenu(open, { open = false }) {
            values.forEach { v ->
                DropdownMenuItem(
                    text = { Text(localizedLabel(resources, v)) },
                    onClick = {
                        choose(v)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ExampleImage() {
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    Image(
        painterResource(R.drawable.service_example),
        resources.getString(R.string.ui_sample_service_image_6c0f),
        modifier = Modifier.fillMaxWidth().height(145.dp),
    )
    Text(
        resources.getString(R.string.ui_sample_image_26b2),
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun title(page: String): String {
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    return when (page) {
        "login" -> "ServiNow"
        "register" -> resources.getString(R.string.ui_create_account_45a1)
        "recovery" -> resources.getString(R.string.ui_recover_password_bac5)
        "sent" -> resources.getString(R.string.ui_recovery_link_9b8b)
        "new-password" -> resources.getString(R.string.ui_new_password_7e2a)
        "home" -> "ServiNow"
        "filters" -> resources.getString(R.string.ui_search_filters_0e07)
        "map",
        "location",
        "post-location" -> resources.getString(R.string.ui_choose_location_6f56)
        "detail" -> resources.getString(R.string.ui_job_details_fff3)
        "create" -> resources.getString(R.string.ui_new_request_a95a)
        "create-details" -> resources.getString(R.string.ui_job_details_3a51)
        "preview" -> resources.getString(R.string.ui_review_post_edfa)
        "submitted" -> resources.getString(R.string.ui_request_saved_0d0f)
        "mine" -> resources.getString(R.string.ui_my_posts_83fe)
        "delete-post" -> resources.getString(R.string.ui_delete_post_fd4a)
        "offers" -> resources.getString(R.string.ui_received_offers_2032)
        "confirm-offer" -> resources.getString(R.string.ui_confirm_worker_ba80)
        "chat" -> resources.getString(R.string.private_chat)
        "tracking" -> resources.getString(R.string.ui_job_progress_aebc)
        "finish",
        "mod-finish" -> resources.getString(R.string.ui_finish_job_49b9)
        "review" -> resources.getString(R.string.ui_rate_experience_45bd)
        "offer" -> resources.getString(R.string.ui_send_offer_caf4)
        "my-offers" -> resources.getString(R.string.ui_my_offers_1f1a)
        "worker" -> resources.getString(R.string.ui_worker_dashboard_701e)
        "profile" -> resources.getString(R.string.ui_professional_profile_7402)
        "cv" -> resources.getString(R.string.ui_resume_d7c6)
        "professional" -> resources.getString(R.string.ui_edit_professional_profile_bbe0)
        "notifications" -> resources.getString(R.string.ui_notifications_fa1e)
        "dashboard" -> resources.getString(R.string.ui_my_statistics_cae0)
        "achievements" -> resources.getString(R.string.ui_my_achievements_ee1b)
        "account" -> resources.getString(R.string.ui_my_account_106f)
        "edit-account" -> resources.getString(R.string.ui_personal_details_648c)
        "delete-account" -> resources.getString(R.string.ui_delete_account_df29)
        "mod-login" -> resources.getString(R.string.ui_moderator_access_8742)
        "moderation" -> resources.getString(R.string.ui_moderation_inbox_a717)
        "mod-detail" -> resources.getString(R.string.ui_review_post_edfa)
        "reject" -> resources.getString(R.string.ui_rejection_reason_371a)
        "empty" -> resources.getString(R.string.ui_no_results_a38a)
        "error" -> resources.getString(R.string.ui_connection_error_46f7)
        else -> resources.getString(R.string.ui_comments_043e)
    }

}

@Composable
fun CommunityRoute(vm: CommunityViewModel = hiltViewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = context.resources
    val ready by vm.ready.collectAsStateWithLifecycle()
    val preferences by vm.preferences.collectAsStateWithLifecycle()
    if (!ready) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentPage = entry?.destination?.route ?: "welcome"
    NavigationEffects(navController, vm)
    LaunchedEffect(currentPage) { vm.destinationChanged(currentPage) }
    val s by vm.state.collectAsStateWithLifecycle()
    val u by vm.ui.collectAsStateWithLifecycle()
    val remoteError by vm.remoteError.collectAsStateWithLifecycle()
    val me = s.me
    val post = s.posts.find { it.id == u.postId }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Todas") }
    var city by rememberSaveable { mutableStateOf("Todas") }
    var order by rememberSaveable { mutableStateOf("Recientes") }
    var onlyOpen by rememberSaveable { mutableStateOf(true) }
    var lat by rememberSaveable { mutableStateOf(4.6486) }
    var lng by rememberSaveable { mutableStateOf(-74.0628) }
    var nearby by rememberSaveable { mutableStateOf(false) }
    fun name(id: String?) = s.users.find { it.id == id }?.name ?: resources.getString(R.string.ui_deleted_user_f4f7)
    fun publicPosts() =
        s.posts.filter {
            it.status in listOf(PostStatus.VERIFIED, PostStatus.IN_PROGRESS, PostStatus.FINISHED)
        }
    val results =
        publicPosts()
            .filter {
                (category == "Todas" || it.category == category) &&
                    (city == "Todas" || it.city == city) &&
                    (!onlyOpen || it.status == PostStatus.VERIFIED) &&
                    it.title.contains(query, true) &&
                    (!nearby ||
                        com.example.servinow.domain.validation.distanceKm(
                            Job(
                                0,
                                it.title,
                                it.category,
                                it.city,
                                0,
                                0,
                                0,
                                it.latitude,
                                it.longitude,
                            ),
                            SearchFilters(latitude = lat, longitude = lng),
                        ) <= 5)
            }
            .let {
                if (order == "Más relevantes") it.sortedByDescending { it.interested.size }
                else it.sortedByDescending { it.createdAt }
            }
    u.message?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.message(null) },
            title = { Text("ServiNow") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { vm.message(null) }) { Text(resources.getString(R.string.ui_ok_fed2)) } },
        )
    }
    Scaffold(
        bottomBar = {
            if (me != null && currentPage !in setOf("welcome", "onboarding")) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.navigationBarsPadding().padding(12.dp).fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        val tabs =
                            if (me.role == MemberRole.MODERATOR)
                                listOf(
                                    resources.getString(R.string.ui_review_28c2) to "moderation",
                                    resources.getString(R.string.ui_notifications_fa1e) to "notifications",
                                    resources.getString(R.string.ui_account_00a5) to "account",
                                )
                            else
                                listOf(
                                    resources.getString(R.string.ui_home_c37e) to "home",
                                    resources.getString(R.string.ui_jobs_256f) to "mine",
                                    resources.getString(R.string.ui_notifications_fa1e) to "notifications",
                                    resources.getString(R.string.ui_account_00a5) to "account",
                                )
                        tabs.forEachIndexed { i, (label, page) ->
                            Surface(
                                onClick = { vm.go(page) },
                                color =
                                    if (currentPage == page) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.primary,
                                contentColor =
                                    if (currentPage == page) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f),
                            ) {
                                Column(
                                    Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        painterResource(
                                            when (page) {
                                                "home",
                                                "moderation" -> R.drawable.ic_home
                                                "mine" -> R.drawable.ic_work
                                                "notifications" -> R.drawable.ic_bell
                                                else -> R.drawable.ic_account
                                            }
                                        ),
                                        null,
                                        Modifier.size(22.dp),
                                    )
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            if (currentPage !in setOf("welcome", "onboarding")) RecoveryHeader(
                title(currentPage),
                vm::back,
                { vm.go("notifications") },
                showNotifications = currentPage in setOf("profile", "account"),
            )
            ServiNowNavHost(navController, Modifier.weight(1f), startDestination = vm.initialDestination) { destination ->
                val u = u.copy(page = destination)
                val scroll = rememberScrollState()
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(screenSpacing()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if(remoteError!=null) Text(resources.getString(R.string.cloud_load_failed),color=MaterialTheme.colorScheme.error)
                if (u.authBusy || u.operationBusy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(resources.getString(R.string.auth_working))
                }
                key(u.page) {
                    when (u.page) {
                        "welcome" -> WelcomeScreen(onStart = { vm.go("onboarding") }, onLogin = { vm.go("login") })
                        "onboarding" -> OnboardingScreen(onFinish = { vm.finishWelcome("register") }, onLogin = { vm.finishWelcome("login") })
                        "login",
                        "mod-login" -> {
                            var email by rememberSaveable { mutableStateOf("") }
                            var password by remember { mutableStateOf("") }
                            Text(
                                resources.getString(R.string.ui_good_to_see_you_a305),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Title(
                                if (u.page == "mod-login") resources.getString(R.string.ui_manage_your_community_2c0c)
                                else resources.getString(R.string.ui_welcome_back_0a55)
                            )
                            Text(resources.getString(R.string.ui_your_jobs_connections_and_new_opportunities_are_waiting_for_you_1619))
                            Field(resources.getString(R.string.ui_email_00ff), email, { email = it })
                            Field(resources.getString(R.string.ui_password_6129), password, { password = it }, password = true)
                            TextButton(onClick = { vm.go("recovery") }) {
                                Text(resources.getString(R.string.ui_forgot_my_password_5cf8))
                            }
                            Action(resources.getString(R.string.ui_sign_in_ef16), enabled = !u.authBusy) {
                                vm.login(email, password, u.page == "mod-login")
                            }
                            if (u.page == "login") {
                                com.example.servinow.features.cloud.GoogleSignInButton(!u.authBusy,vm::googleToken) { vm.message("AUTH_FAILED") }
                                Link(resources.getString(R.string.ui_new_here_create_an_account_fa98)) { vm.go("register") }
                                TextButton(onClick = { vm.go("mod-login") }) {
                                    Text(resources.getString(R.string.ui_i_am_a_moderator_0675))
                                }
                            }
                            if (!vm.usesRemoteAuthentication) Card {
                                Text(resources.getString(R.string.ui_demo_access_eddb), fontWeight = FontWeight.Bold)
                                Text(
                                    if (u.page == "mod-login") "moderador@gmail.com\nModerador1!"
                                    else "valentina@gmail.com o andres@gmail.com\nServiNow1!"
                                )
                                Text(
                                    resources.getString(R.string.ui_data_is_stored_in_memory_and_resets_when_the_process_closes_b3da),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        "register" -> {
                            var n by rememberSaveable { mutableStateOf("") }
                            var e by rememberSaveable { mutableStateOf("") }
                            var p by remember { mutableStateOf("") }
                            var c by remember { mutableStateOf("") }
                            Title(resources.getString(R.string.ui_create_your_account_a9de))
                            Text(resources.getString(R.string.ui_request_help_and_offer_your_services_from_the_same_account_6bca))
                            Field(resources.getString(R.string.ui_full_name_8229), n, { n = it })
                            Field(resources.getString(R.string.ui_email_00ff), e, { e = it })
                            Field(resources.getString(R.string.ui_password_6129), p, { p = it }, password = true)
                            Field(resources.getString(R.string.ui_confirm_password_02bf), c, { c = it }, password = true)
                            Action(resources.getString(R.string.ui_create_account_45a1), enabled = !u.authBusy) { vm.register(n, e, p, c) }
                            Text(
                                resources.getString(R.string.ui_you_can_complete_your_address_from_my_account_e341),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        "recovery" -> {
                            var e by rememberSaveable { mutableStateOf("") }
                            Icon(
                                painterResource(R.drawable.ic_mail),
                                null,
                                Modifier.align(Alignment.CenterHorizontally).size(64.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Title(resources.getString(R.string.ui_recover_your_access_47db))
                            Text(resources.getString(R.string.ui_enter_your_account_email_2b4e))
                            Field(resources.getString(R.string.ui_email_00ff), e, { e = it })
                            Action(resources.getString(R.string.ui_request_link_933e), enabled = !u.authBusy) { vm.recovery(e) }
                            Text(resources.getString(if (vm.usesRemoteAuthentication) R.string.auth_recovery_notice else R.string.ui_demo_recovery_no_real_email_will_be_sent_3b40))
                        }
                        "sent" -> {
                            Icon(
                                painterResource(R.drawable.ic_circle_check),
                                null,
                                Modifier.align(Alignment.CenterHorizontally).size(64.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Title(resources.getString(R.string.ui_request_ready_4fe2))
                            Text(
                                resources.getString(R.string.recovery_email_notice, u.recoveryEmail)
                            )
                            if (!vm.usesRemoteAuthentication) Card {
                                Text(resources.getString(R.string.ui_demo_link_7942), fontWeight = FontWeight.Bold)
                                Text(
                                    resources.getString(R.string.ui_open_the_demo_link_in_the_app_to_try_recovery_without_an_email_se_22b3)
                                )
                                Action(resources.getString(R.string.ui_open_demo_link_8189)) { vm.go("new-password") }
                            }
                            Link(resources.getString(R.string.ui_resend_request_5a05)) { vm.recovery(u.recoveryEmail) }
                            Link(resources.getString(R.string.ui_back_to_sign_in_b64a)) { vm.go("login") }
                        }
                        "new-password" -> {
                            var p by remember { mutableStateOf("") }
                            var c by remember { mutableStateOf("") }
                            Text(
                                resources.getString(R.string.ui_account_recovery_07f0),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Title(resources.getString(R.string.ui_a_new_password_28e2))
                            Text(
                                resources.getString(R.string.ui_choose_a_password_you_can_remember_and_others_cannot_guess_c958)
                            )
                            Field(resources.getString(R.string.ui_new_password_7e2a), p, { p = it }, password = true)
                            Field(resources.getString(R.string.ui_confirm_password_02bf), c, { c = it }, password = true)
                            Action(resources.getString(R.string.ui_save_password_5b76)) { vm.reset(p, c) }
                        }
                        "home",
                        "empty" -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(resources.getString(R.string.ui_your_community_close_by_db9c), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Title(resources.getString(R.string.home_greeting, me?.name?.substringBefore(' ').orEmpty()))
                                    Text(resources.getString(R.string.ui_what_can_we_help_with_today_9b1b), style = MaterialTheme.typography.titleMedium)
                                    Text(resources.getString(R.string.ui_find_help_for_your_projects_or_share_your_skills_593e), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Surface(
                                onClick = vm::startPost,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(20.dp),
                            ) {
                                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("+", style = MaterialTheme.typography.headlineLarge)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(resources.getString(R.string.ui_post_a_job_43d8), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(resources.getString(R.string.ui_tell_us_what_you_need_and_receive_offers_42db), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("→", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            Surface(
                                onClick = { vm.go("worker") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(resources.getString(R.string.ui_find_work_ede4), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(resources.getString(R.string.ui_find_opportunities_for_your_skills_51a8), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("→", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            Text(resources.getString(R.string.ui_explore_jobs_7c3d), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Field(resources.getString(R.string.ui_search_for_a_job_23b2), query, { query = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { vm.go("filters") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text(resources.getString(R.string.ui_filters_737c)) }
                                OutlinedButton(onClick = { vm.go("map") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text(resources.getString(R.string.ui_view_map_1283)) }
                            }
                            Text(resources.getString(R.string.jobs_available, results.size), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (results.isEmpty())
                                Card {
                                    Text(resources.getString(R.string.ui_no_jobs_match_these_filters_444b))
                                    Action(resources.getString(R.string.ui_reset_filters_dd65)) {
                                        query = ""
                                        category = "Todas"
                                        city = "Todas"
                                        nearby = false
                                        onlyOpen = true
                                    }
                                }
                            results.forEach { p ->
                                PostCard(p, name(p.author)) { vm.openPost(p.id) }
                            }
                        }
                        "filters" -> {
                            Title(resources.getString(R.string.ui_find_what_you_need_72e9))
                            Choice(resources.getString(R.string.ui_category_028c), category, categories) { category = it }
                            Choice(resources.getString(R.string.ui_city_dcbc), city, listOf("Todas", "Bogotá", "Medellín", "Cali")) {
                                city = it
                            }
                            Choice(resources.getString(R.string.ui_order_3538), order, listOf("Recientes", "Más relevantes")) {
                                order = it
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(onlyOpen, { onlyOpen = it })
                                Text(resources.getString(R.string.ui_open_jobs_only_4b29))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(nearby, { nearby = it })
                                Text(resources.getString(R.string.ui_nearby_within_5_km_d8e7))
                            }
                            Link(resources.getString(R.string.ui_choose_search_center_on_map_ef5a)) { vm.go("location") }
                            Action(resources.getString(R.string.ui_apply_filters_671a)) { vm.go("home") }
                            Link(resources.getString(R.string.ui_reset_filters_dd65)) {
                                query = ""
                                category = "Todas"
                                city = "Todas"
                                nearby = false
                                onlyOpen = true
                            }
                        }
                        "location",
                        "map" -> {
                            Title(resources.getString(R.string.ui_choose_your_area_f5bf))
                            Text(resources.getString(R.string.ui_tap_the_map_to_choose_a_location_7f21))
                            var selectedPostId by rememberSaveable { mutableStateOf<String?>(null) }
                            ServiceMap(lat, lng, Modifier.fillMaxWidth().height(340.dp),
                                posts = if (u.page == "map") results else emptyList(),
                                selectedPoint = if (u.page == "location") Point.fromLngLat(lng, lat) else null,
                                onPost = { selectedPostId = it.id },
                                onPoint = { x, y ->
                                    selectedPostId = null
                                    lat = x; lng = y
                                })
                            results.find { it.id == selectedPostId }?.let { selected ->
                                PostCard(selected, name(selected.author)) { vm.openPost(selected.id) }
                            }
                            Text("%.5f, %.5f".format(Locale.US, lat, lng))
                            Action(if (u.page == "map") resources.getString(R.string.ui_view_list_d713) else resources.getString(R.string.ui_use_this_location_6ce8)) {
                                if (u.page == "location") {
                                    nearby = true
                                    vm.go("filters")
                                } else vm.go("home")
                            }
                        }
                        "detail",
                        "comments" -> {
                            if (post == null) Text(resources.getString(R.string.ui_this_post_was_deleted_a292))
                            else {
                                LaunchedEffect(post.latitude, post.longitude) { vm.lookupAddress(post.latitude, post.longitude) }
                                Title(post.title)
                                Text(
                                    "${localizedLabel(resources, post.category)} · ${localizedLabel(resources, post.status.label)}",
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (post.imageUri != null) AsyncImage(model = post.imageUri, contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop) else if (post.hasImage) ExampleImage()
                                Text(post.description)
                                Text("${post.city} · ${money(post.budget)}")
                                ServiceMap(post.latitude, post.longitude, Modifier.fillMaxWidth().height(160.dp), posts = listOf(post), interactive = false)
                                u.address?.let { Text(it) }
                                Text(
                                    "${post.latitude}, ${post.longitude}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(resources.getString(R.string.posted_by, name(post.author)))
                                if (post.reason.isNotEmpty())
                                    Card {
                                        Text(resources.getString(R.string.ui_rejection_reason_371a), fontWeight = FontWeight.Bold)
                                        Text(post.reason)
                                    }
                                if (
                                    post.status in
                                        listOf(
                                            PostStatus.VERIFIED,
                                            PostStatus.IN_PROGRESS,
                                            PostStatus.FINISHED,
                                        )
                                ) {
                                    Action(
                                        resources.getString(if (me?.id in post.interested) R.string.interested_count else R.string.interest_count, post.interested.size),
                                        vm::vote,
                                    )
                                    if (post.author != me?.id && post.status == PostStatus.VERIFIED)
                                        Action(resources.getString(R.string.ui_send_an_offer_cf68)) { vm.go("offer") }
                                }
                                if (post.status in listOf(PostStatus.VERIFIED, PostStatus.IN_PROGRESS, PostStatus.FINISHED)) {
                                    Link(resources.getString(R.string.share_job)) {
                                        val text = resources.getString(R.string.share_job_body, post.title,
                                            localizedLabel(resources, post.category), post.city, money(post.budget), post.description)
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, post.title)
                                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                                        }
                                        try { context.startActivity(android.content.Intent.createChooser(intent, resources.getString(R.string.share_job))) }
                                        catch (_: android.content.ActivityNotFoundException) { vm.message(resources.getString(R.string.share_unavailable)) }
                                    }
                                }
                                if (post.author == me?.id) {
                                    if (post.status != PostStatus.FINISHED) {
                                        Action(resources.getString(R.string.ui_mark_as_finished_788c)) { vm.go("finish") }
                                    }
                                    Link(resources.getString(R.string.offer_count, s.offers.count { it.post == post.id })) {
                                        vm.go("offers")
                                    }
                                    Link(resources.getString(R.string.ui_job_progress_aebc)) { vm.go("tracking") }
                                    if (
                                        post.status in
                                            listOf(
                                                PostStatus.PENDING,
                                                PostStatus.VERIFIED,
                                                PostStatus.REJECTED,
                                            )
                                    )
                                        Link(resources.getString(R.string.ui_edit_post_53bd)) { vm.editPost(post) }
                                    Link(resources.getString(R.string.ui_delete_post_fd4a)) { vm.go("delete-post") }
                                }
                                Title(resources.getString(R.string.ui_comments_043e))
                                s.comments
                                    .filter { it.post == post.id }
                                    .forEach { c ->
                                        Card {
                                            Text(name(c.author), fontWeight = FontWeight.Bold)
                                            Text(c.text)
                                        }
                                    }
                                var text by rememberSaveable { mutableStateOf("") }
                                Field(resources.getString(R.string.ui_add_comment_0e7c), text, { text = it }, true)
                                Action(resources.getString(R.string.ui_post_comment_5824)) { vm.comment(text) { text = "" } }
                            }
                        }
                        "create",
                        "create-details",
                        "post-location",
                        "preview" -> {
                            val d = u.draft
                            LaunchedEffect(d?.latitude, d?.longitude, currentPage) {
                                if (d != null && currentPage in setOf("post-location", "preview")) vm.lookupAddress(d.latitude, d.longitude)
                            }
                            if (d == null) Text(resources.getString(R.string.ui_start_a_new_request_4208))
                            else
                                when (u.page) {
                        "welcome" -> WelcomeScreen(onStart = { vm.go("onboarding") }, onLogin = { vm.go("login") })
                        "onboarding" -> OnboardingScreen(onFinish = { vm.finishWelcome("register") }, onLogin = { vm.finishWelcome("login") })
                                    "create" -> {
                                        Text(
                                            resources.getString(R.string.ui_step_1_of_4_98a2),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Title(resources.getString(R.string.ui_what_service_do_you_need_24bf))
                                        Choice(resources.getString(R.string.ui_category_028c), d.category, categories.drop(1)) {
                                            vm.draft(d.copy(category = it))
                                        }
                                        Field(
                                            resources.getString(R.string.ui_job_title_d0ff),
                                            d.title,
                                            { vm.draft(d.copy(title = it)) },
                                        )
                                        Action(resources.getString(R.string.ui_continue_8bec)) {
                                            if (d.title.trim().length < 4)
                                                vm.message(
                                                    resources.getString(R.string.ui_enter_a_title_with_at_least_4_characters_7744)
                                                )
                                            else vm.go("create-details")
                                        }
                                    }
                                    "create-details" -> {
                                        Text(
                                            resources.getString(R.string.ui_step_2_of_4_2312),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Title(resources.getString(R.string.ui_tell_us_the_details_b702))
                                        Field(
                                            resources.getString(R.string.ui_description_7059),
                                            d.description,
                                            { vm.draft(d.copy(description = it)) },
                                            true,
                                        )
                                        var budget by rememberSaveable {
                                            mutableStateOf(
                                                d.budget.takeIf { it > 0 }?.toString().orEmpty()
                                            )
                                        }
                                        Field(
                                            resources.getString(R.string.ui_budget_in_cop_b11f),
                                            budget,
                                            {
                                                budget = it
                                                vm.draft(d.copy(budget = it.toLongOrNull() ?: 0))
                                            },
                                        )
                                        PhotoPicker(d.imageUri) { uri -> vm.draft(d.copy(imageUri = uri, hasImage = uri != null)) }
                                        com.example.servinow.features.cloud.CloudTools(d.description, d.imageUri,
                                            { vm.draft(d.copy(description = it)) },
                                            { vm.draft(d.copy(imageUri = it, hasImage = true)) })
                                        Action(resources.getString(R.string.ui_continue_8bec)) {
                                            if (
                                                d.description.trim().length < 10 ||
                                                    d.budget <= 0 ||
                                                    !d.hasImage
                                            )
                                                vm.message(
                                                    resources.getString(R.string.ui_add_a_description_a_positive_budget_and_a_photo_86ee)
                                                )
                                            else vm.go("post-location")
                                        }
                                    }
                                    "post-location" -> {
                                        Text(
                                            resources.getString(R.string.ui_step_3_of_4_acb4),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Title(resources.getString(R.string.ui_where_is_the_job_0853))
                                        Choice(
                                            resources.getString(R.string.ui_city_dcbc),
                                            d.city,
                                            listOf("Armenia", "Bogotá", "Medellín", "Cali"),
                                        ) { c ->
                                            val point =
                                                when (c) {
                                                    "Armenia" -> 4.535 to -75.675
                                                    "Medellín" -> 6.244 to -75.581
                                                    "Cali" -> 3.451 to -76.532
                                                    else -> 4.6486 to -74.0628
                                                }
                                            vm.draft(
                                                d.copy(
                                                    city = c,
                                                    latitude = point.first,
                                                    longitude = point.second,
                                                )
                                            )
                                        }
                                        ServiceMap(d.latitude, d.longitude, Modifier.fillMaxWidth().height(300.dp).testTag("job-location-map"), selectedPoint = Point.fromLngLat(d.longitude, d.latitude), onPoint = { x, y -> vm.draft(d.copy(latitude = x, longitude = y)) })
                                        Text(if (u.addressLoading) resources.getString(R.string.map_address_loading) else u.address ?: resources.getString(R.string.location_reference, d.latitude.toString(), d.longitude.toString()))
                                        Text(
                                            resources.getString(R.string.ui_choose_the_city_where_the_job_will_take_place_a01d)
                                        )
                                        Action(resources.getString(R.string.ui_review_post_edfa)) { vm.go("preview") }
                                    }
                                    else -> {
                                        Text(
                                            resources.getString(R.string.ui_step_4_of_4_fb25),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Title(d.title)
                                        if (d.imageUri != null) AsyncImage(model = d.imageUri, contentDescription = resources.getString(R.string.photo_preview), modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop) else if (d.hasImage) ExampleImage()
                                        Text("${localizedLabel(resources, d.category)} · ${d.city}")
                                        Text(d.description)
                                        Text(money(d.budget))
                                        Text(
                                            resources.getString(R.string.ui_the_post_will_be_pending_and_will_appear_in_the_feed_after_approv_10e8)
                                        )
                                        Text(resources.getString(R.string.post_upload_notice))
                                        Action(resources.getString(R.string.ui_submit_for_review_b370), enabled = !u.operationBusy) { vm.savePost() }
                                    }
                                }
                        }
                        "submitted" -> {
                            Icon(
                                painterResource(R.drawable.ic_circle_check),
                                null,
                                Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Title(resources.getString(R.string.ui_you_have_taken_the_first_step_e22e))
                            Text(resources.getString(R.string.ui_pending_verification_9a02))
                            Text(post?.title.orEmpty())
                            Action(resources.getString(R.string.ui_view_my_posts_3cdc)) { vm.go("mine") }
                        }
                        "mine" -> {
                            Title(resources.getString(R.string.ui_my_requests_e46d))
                            Action(resources.getString(R.string.ui_new_request_a95a), vm::startPost)
                            var filter by rememberSaveable { mutableStateOf("Todas") }
                            Choice(
                                resources.getString(R.string.ui_status_843e),
                                filter,
                                listOf("Todas") + PostStatus.entries.map { it.label },
                            ) {
                                filter = it
                            }
                            val list =
                                s.posts.filter {
                                    it.author == me?.id &&
                                        (filter == "Todas" || it.status.label == filter)
                                }
                            if (list.isEmpty()) Text(resources.getString(R.string.ui_you_have_no_posts_with_this_status_c66f))
                            list.forEach { p -> PostCard(p, name(p.author)) { vm.openPost(p.id) } }
                        }
                        "delete-post" -> {
                            Title(resources.getString(R.string.ui_delete_this_post_e996))
                            Text(post?.title.orEmpty())
                            Text(resources.getString(R.string.ui_its_offers_and_comments_will_also_be_deleted_dcbb))
                            Action(resources.getString(R.string.ui_yes_delete_post_d315), vm::deletePost)
                            Link(resources.getString(R.string.ui_keep_post_6757)) { vm.go("detail") }
                        }
                        "offers" -> {
                            Title(resources.getString(R.string.ui_choose_with_confidence_0497))
                            Text(post?.title.orEmpty())
                            val offers = s.offers.filter { it.post == u.postId }
                            if (offers.isEmpty()) Text(resources.getString(R.string.ui_no_offers_yet_2df2))
                            offers.forEach { o ->
                                Card {
                                    Text(name(o.worker), fontWeight = FontWeight.Bold)
                                    Text(money(o.price))
                                    Text(o.description)
                                    Text(o.availability)
                                    Link(resources.getString(R.string.ui_view_profile_and_resume_d211)) { vm.profile(o.worker) }
                                    if (post?.status == PostStatus.VERIFIED)
                                        Action(resources.getString(R.string.ui_choose_worker_8452)) { vm.selectOffer(o.id) }
                                    else
                                        Text(
                                            if (post?.assigned == o.worker) resources.getString(R.string.ui_worker_assigned_2fa4)
                                            else resources.getString(R.string.ui_offer_not_selected_a953)
                                        )
                                }
                            }
                        }
                        "confirm-offer" -> {
                            val o = s.offers.find { it.id == u.offerId }
                            Title(resources.getString(R.string.ui_one_more_step_d93d))
                            if (o != null)
                                Card {
                                    Text(name(o.worker), fontWeight = FontWeight.Bold)
                                    Text(money(o.price))
                                    Text(o.description)
                                }
                            Text(
                                resources.getString(R.string.ui_confirming_assigns_the_job_and_closes_it_to_new_offers_ebf0)
                            )
                            Action(resources.getString(R.string.ui_confirm_worker_ba80), vm::accept)
                            Link(resources.getString(R.string.ui_keep_comparing_4ae8)) { vm.go("offers") }
                        }
                        "chat" -> {
                            val allowed = post?.assigned != null && me?.id in setOf(post?.author, post?.assigned)
                            if (!allowed) Text(resources.getString(R.string.chat_unavailable))
                            else {
                                Title(resources.getString(R.string.private_chat))
                                Text(post?.title.orEmpty())
                                Text(resources.getString(R.string.chat_private_hint))
                                val messages = s.messages.filter { it.post == post?.id && me?.id in setOf(it.sender, it.recipient) }
                                if (messages.isEmpty()) Text(resources.getString(R.string.chat_empty))
                                messages.forEach { item ->
                                    Card {
                                        Text(if (item.sender == me?.id) resources.getString(R.string.chat_you) else name(item.sender), fontWeight = FontWeight.Bold)
                                        Text(item.text)
                                        Text(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(item.createdAt)), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                var text by rememberSaveable(u.postId, me?.id) { mutableStateOf("") }
                                Field(resources.getString(R.string.chat_message), text, { if (it.length <= 2000) text = it }, true)
                                Button(onClick = { vm.sendMessage(text) { text = "" } }, enabled = text.isNotBlank() && !u.operationBusy, modifier = Modifier.fillMaxWidth()) { Text(resources.getString(R.string.chat_send)) }
                            }
                        }
                        "tracking" -> {
                            Title(resources.getString(R.string.ui_your_job_is_in_good_hands_e694))
                            Text(post?.title.orEmpty())
                            Text(
                                post?.status?.label.orEmpty(),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            post?.assigned?.let { id ->
                                Card {
                                    Text(name(id), fontWeight = FontWeight.Bold)
                                    Text(s.users.find { it.id == id }?.profession.orEmpty())
                                    Link(resources.getString(R.string.ui_view_profile_and_resume_d211)) { vm.profile(id) }
                                }
                            }
                            listOf(
                                    resources.getString(R.string.ui_post_approved_781b),
                                    resources.getString(R.string.ui_offer_accepted_9a9a),
                                    resources.getString(R.string.ui_work_in_progress_53ca),
                                    resources.getString(R.string.ui_completion_and_review_5ace),
                                )
                                .forEachIndexed { i, label ->
                                    Card {
                                        Text(label, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (
                                                post?.status == PostStatus.FINISHED ||
                                                    i < 3 && post?.assigned != null
                                            )
                                                resources.getString(R.string.ui_completed_e70c)
                                            else resources.getString(R.string.ui_pending_37f6)
                                        )
                                    }
                                }
                            if (post != null && post.author == me?.id) {
                                if (post.status != PostStatus.FINISHED)
                                    Action(resources.getString(R.string.ui_mark_as_finished_788c)) { vm.go("finish") }
                                else if (
                                    post.assigned != null && s.reviews.none { it.post == post.id }
                                )
                                    Action(resources.getString(R.string.ui_rate_experience_45bd)) { vm.go("review") }
                            }
                            if (post?.assigned != null && me?.id in setOf(post?.author, post?.assigned)) {
                                Action(resources.getString(R.string.private_chat)) { vm.go("chat") }
                            }
                            Link(resources.getString(R.string.ui_view_conversation_d351)) { vm.go("comments") }
                        }
                        "finish",
                        "mod-finish" -> {
                            Title(resources.getString(R.string.ui_finish_job_bd77))
                            Text(post?.title.orEmpty())
                            Text(
                                resources.getString(R.string.ui_confirm_that_the_job_is_complete_the_author_and_worker_will_be_no_a944)
                            )
                            Action(resources.getString(R.string.ui_confirm_completion_4467), vm::finish)
                            Link(resources.getString(R.string.ui_back_a785), vm::back)
                        }
                        "review" -> {
                            Title(resources.getString(R.string.ui_how_was_your_experience_fcdd))
                            Text(name(post?.assigned))
                            var stars by rememberSaveable { mutableIntStateOf(0) }
                            var text by rememberSaveable { mutableStateOf("") }
                            Row(Modifier.fillMaxWidth()) {
                                (1..5).forEach { n ->
                                    TextButton(
                                        onClick = { stars = n },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            if (n <= stars) "★" else "☆",
                                            style = MaterialTheme.typography.headlineMedium,
                                        )
                                    }
                                }
                            }
                            Text(resources.getString(R.string.stars_rating, stars))
                            Field(resources.getString(R.string.ui_your_review_d90e), text, { text = it }, true)
                            Action(resources.getString(R.string.ui_publish_review_7375)) { vm.review(stars, text) }
                        }
                        "worker" -> {
                            Title(resources.getString(R.string.ui_share_your_skills_69c2))
                            Card {
                                Text(
                                    resources.getString(R.string.finished_jobs_count, s.posts.count { it.assigned == me?.id && it.status == PostStatus.FINISHED })
                                )
                                Text(
                                    resources.getString(R.string.active_jobs_count, s.posts.count { it.assigned == me?.id && it.status == PostStatus.IN_PROGRESS })
                                )
                                Text(resources.getString(R.string.sent_offers_count, s.offers.count { it.worker == me?.id }))
                            }
                            Action(resources.getString(R.string.ui_find_jobs_c24c)) { vm.go("home") }
                            Link(resources.getString(R.string.ui_my_offers_1f1a)) { vm.go("my-offers") }
                            Link(resources.getString(R.string.ui_my_professional_profile_7358)) { me?.let { vm.profile(it.id) } }
                            s.posts
                                .filter { it.assigned == me?.id }
                                .forEach { p ->
                                    PostCard(p, name(p.author)) { vm.openPost(p.id, "tracking") }
                                }
                        }
                        "offer" -> {
                            Title(resources.getString(R.string.ui_make_your_proposal_1d20))
                            Text(post?.title.orEmpty())
                            var price by rememberSaveable { mutableStateOf("") }
                            var text by rememberSaveable { mutableStateOf("") }
                            var whenText by rememberSaveable { mutableStateOf("") }
                            Field(resources.getString(R.string.ui_price_in_cop_ff89), price, { price = it })
                            Field(resources.getString(R.string.ui_what_is_included_8c99), text, { text = it }, true)
                            Field(resources.getString(R.string.ui_availability_cd96), whenText, { whenText = it })
                            Action(resources.getString(R.string.ui_send_offer_caf4)) { vm.offer(price, text, whenText) }
                        }
                        "my-offers" -> {
                            Title(resources.getString(R.string.ui_your_opportunities_17c1))
                            val offers = s.offers.filter { it.worker == me?.id }
                            if (offers.isEmpty()) Text(resources.getString(R.string.ui_you_have_not_sent_any_offers_yet_bd3c))
                            offers.forEach { o ->
                                val p = s.posts.find { it.id == o.post }
                                Card {
                                    Text(p?.title.orEmpty(), fontWeight = FontWeight.Bold)
                                    Text(money(o.price))
                                    Text(
                                        if (p != null && p.assigned == me?.id)
                                            resources.getString(R.string.accepted_offer_status, localizedLabel(resources, p.status.label))
                                        else if (p?.assigned != null) resources.getString(R.string.ui_not_selected_cfa1)
                                        else resources.getString(R.string.ui_pending_37f6)
                                    )
                                    Action(resources.getString(R.string.ui_view_job_2479)) { vm.openPost(o.post) }
                                }
                            }
                        }
                        "profile",
                        "cv" -> {
                            val person = s.users.find { it.id == u.profileId }
                            if (person != null) {
                                if (person.photoUri != null) AsyncImage(model = person.photoUri, contentDescription = null, modifier = Modifier.size(100.dp).clip(androidx.compose.foundation.shape.CircleShape), contentScale = ContentScale.Crop)
                                Title(person.name)
                                Text(person.profession.ifBlank { resources.getString(R.string.ui_community_professional_1ab1) })
                                val reviews = s.reviews.filter { it.worker == person.id }
                                Card {
                                    Text(
                                        if (reviews.isEmpty()) resources.getString(R.string.ui_no_ratings_yet_1bc6)
                                        else
                                            resources.getString(R.string.ui_1f_d_reviews_03ac)
                                                .format(
                                                    reviews.map { it.stars }.average(),
                                                    reviews.size,
                                                )
                                    )
                                    Text(resources.getString(R.string.experience_years, person.years))
                                    Text(person.bio.ifBlank { resources.getString(R.string.ui_no_introduction_yet_7f6c) })
                                }
                                if (u.page == "profile") {
                                    Action(resources.getString(R.string.ui_view_resume_9c3b)) { vm.go("cv") }
                                    Title(resources.getString(R.string.ui_community_reviews_f1ef))
                                    reviews.forEach { r ->
                                        Card {
                                            Text(name(r.author), fontWeight = FontWeight.Bold)
                                            Text("★".repeat(r.stars))
                                            Text(r.text)
                                        }
                                    }
                                } else {
                                    listOf(
                                            resources.getString(R.string.ui_experience_1aaf) to person.experience,
                                            resources.getString(R.string.ui_education_2640) to person.training,
                                            resources.getString(R.string.ui_skills_c9b9) to person.skills,
                                        )
                                        .forEach { (label, value) ->
                                            Card {
                                                Text(label, fontWeight = FontWeight.Bold)
                                                Text(value.ifBlank { resources.getString(R.string.ui_no_information_5554) })
                                            }
                                        }
                                }
                                if (person.id == me?.id)
                                    Link(resources.getString(R.string.ui_edit_professional_profile_bbe0)) { vm.go("professional") }
                            }
                        }
                        "account" -> {
                            com.example.servinow.features.cloud.NotificationPermission()
                            val accountPoints = s.points[me?.id] ?: 0
                            val initials = me?.name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1).uppercase() }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        modifier = Modifier.size(76.dp),
                                        shape = RoundedCornerShape(38.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (me?.photoUri != null) AsyncImage(model = me.photoUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            else Text(initials, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(me?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Text(me?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
                                        Text(resources.getString(R.string.account_points, localizedLabel(resources, reputationLevel(accountPoints)), accountPoints), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                    TextButton(onClick = { vm.go("edit-account") }) { Text(resources.getString(R.string.ui_edit_my_details_de16)) }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
 Checkbox(preferences.rememberSession, vm::rememberSession)
 Text(resources.getString(R.string.ui_remember_my_session_275a))
 }
 Text(resources.getString(R.string.ui_manage_your_account_decd), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            listOf(
                                    resources.getString(R.string.ui_personal_details_648c) to "edit-account",
                                    resources.getString(R.string.ui_professional_profile_7402) to "professional",
                                    resources.getString(R.string.ui_my_statistics_cae0) to "dashboard",
                                    resources.getString(R.string.achievements) to "achievements",
                                    resources.getString(R.string.ui_notifications_fa1e) to "notifications",
                                )
                                .forEach { (label, page) -> Link(label) { vm.go(page) } }
                            Link(resources.getString(R.string.ui_view_my_profile_2426)) { me?.let { vm.profile(it.id) } }
                            Link(resources.getString(R.string.ui_recover_password_bac5)) { vm.go("recovery") }
                            if (me?.role == MemberRole.USER)
                                OutlinedButton(
                                    onClick = { vm.go("delete-account") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Text(resources.getString(R.string.ui_delete_my_account_e425), color = MaterialTheme.colorScheme.error)
                                }
                            Text(resources.getString(R.string.settings_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            com.example.servinow.core.component.LanguageSetting()
                            Action(resources.getString(R.string.ui_switch_user_sign_out_e760), vm::logout)
                        }
                        "edit-account",
                        "professional" -> {
                            if (me != null) {
                                var m by remember { mutableStateOf(me) }
                                Title(
                                    if (u.page == "professional") resources.getString(R.string.ui_your_experience_opens_doors_1bda)
                                    else resources.getString(R.string.ui_your_personal_details_9a34)
                                )
                                if (u.page == "professional") {
                                    Field(
                                        resources.getString(R.string.ui_professional_title_5e22),
                                        m.profession,
                                        { m = m.copy(profession = it) },
                                    )
                                    Field(resources.getString(R.string.ui_about_you_096a), m.bio, { m = m.copy(bio = it) }, true)
                                    var years by rememberSaveable {
                                        mutableStateOf(m.years.toString())
                                    }
                                    Field(
                                        resources.getString(R.string.ui_years_of_experience_4bbd),
                                        years,
                                        {
                                            years = it
                                            m = m.copy(years = it.toIntOrNull() ?: -1)
                                        },
                                    )
                                    Field(
                                        resources.getString(R.string.ui_work_experience_3b6d),
                                        m.experience,
                                        { m = m.copy(experience = it) },
                                        true,
                                    )
                                    Field(resources.getString(R.string.ui_education_2640), m.training, { m = m.copy(training = it) })
                                    Field(resources.getString(R.string.ui_skills_c9b9), m.skills, { m = m.copy(skills = it) })
                                    Text(
                                        resources.getString(R.string.ui_your_resume_is_generated_from_these_details_51e6)
                                    )
                                } else {
                                    PhotoPicker(m.photoUri, circular = true) { m = m.copy(photoUri = it) }
                                    com.example.servinow.features.cloud.CloudTools("", m.photoUri, {}, { m = m.copy(photoUri = it) }, showAi = false)
                                    Field(resources.getString(R.string.ui_full_name_8229), m.name, { m = m.copy(name = it) })
                                    Field(resources.getString(R.string.ui_email_00ff), m.email, { m = m.copy(email = it) })
                                    Field(
                                        resources.getString(R.string.ui_phone_optional_604d),
                                        m.phone,
                                        { m = m.copy(phone = it) },
                                    )
                                    Choice(resources.getString(R.string.ui_city_dcbc), m.city, listOf("Armenia", "Bogotá", "Medellín", "Cali")) {
                                        m = m.copy(city = it)
                                    }
                                    ServiceMap(m.latitude, m.longitude, Modifier.fillMaxWidth().height(280.dp), selectedPoint = Point.fromLngLat(m.longitude, m.latitude), onPoint = { x, y -> m = m.copy(latitude = x, longitude = y) })
                                }
                                Action(resources.getString(R.string.ui_save_changes_b9be)) { vm.saveMember(m) }
                            }
                        }
                        "delete-account" -> {
                            Title(resources.getString(R.string.ui_delete_your_account_164d))
                            Text(
                                resources.getString(R.string.ui_your_posts_and_associated_demo_data_will_be_deleted_this_cannot_b_eddf)
                            )
                            var text by rememberSaveable { mutableStateOf("") }
                            Field(resources.getString(R.string.ui_type_eliminar_8862), text, { text = it })
                            Action(resources.getString(R.string.ui_delete_account_df29)) { vm.deleteAccount(text) }
                            Link(resources.getString(R.string.ui_keep_my_account_2cfc)) { vm.go("account") }
                        }
                        "notifications" -> {
                            val notices = s.notices.filter { it.recipient == me?.id }
                            Title(resources.getString(R.string.ui_your_notifications_b48b))
                            Text(resources.getString(R.string.unread_count, notices.count { !it.read }))
                            Link(resources.getString(R.string.ui_mark_all_as_read_0483), vm::markAll)
                            if (notices.isEmpty()) Text(resources.getString(R.string.ui_no_notifications_yet_4dd9))
                            notices.forEach { n ->
                                Card {
                                    Text(localizedLabel(resources, n.title), fontWeight = FontWeight.Bold)
                                    Text(localizedLabel(resources, n.body))
                                    Text(
                                        if (n.read) resources.getString(R.string.ui_read_f091) else resources.getString(R.string.ui_new_9c50),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    TextButton(onClick = { vm.mark(n.id, !n.read) }) {
                                        Text(if (n.read) resources.getString(R.string.ui_mark_unread_be17) else resources.getString(R.string.ui_mark_read_2e82))
                                    }
                                    Action(resources.getString(R.string.ui_view_details_3aa8)) {
                                        vm.mark(n.id, true)
                                        if (n.post == null) vm.go("achievements")
                                        else if (s.posts.none { it.id == n.post })
                                            vm.message(resources.getString(R.string.ui_the_post_was_deleted_2057))
                                        else vm.openPost(n.post)
                                    }
                                }
                            }
                        }
                        "dashboard" -> {
                            val own = s.posts.filter { it.author == me?.id }
                            val completed = own.count { it.status == PostStatus.FINISHED }
                            val points = s.points[me?.id] ?: 0
                            Title(resources.getString(R.string.ui_your_activity_at_a_glance_26ca))
                            Text(resources.getString(R.string.ui_see_how_your_posts_and_community_contributions_are_doing_31c3), style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(22.dp),
                            ) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(resources.getString(R.string.ui_your_posts_51d9), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(own.size.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                    Text(if (own.isEmpty()) resources.getString(R.string.ui_your_activity_starts_with_your_first_post_022f) else resources.getString(R.string.completed_count, completed, own.size), style = MaterialTheme.typography.titleMedium)
                                    if (own.isNotEmpty()) {
                                        LinearProgressIndicator(
                                            progress = { completed.toFloat() / own.size },
                                            modifier = Modifier.fillMaxWidth().height(8.dp),
                                            trackColor = MaterialTheme.colorScheme.surface,
                                        )
                                    }
                                    Text(resources.getString(R.string.ui_summary_of_posts_in_your_account_f2ec), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text(resources.getString(R.string.ui_how_are_your_jobs_going_4c7c), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            val statuses = listOf(
                                Triple(resources.getString(R.string.ui_looking_for_a_worker_934e), own.count { it.status == PostStatus.VERIFIED }, resources.getString(R.string.ui_open_to_offers_2426)),
                                Triple(resources.getString(R.string.ui_in_progress_44fa), own.count { it.status == PostStatus.IN_PROGRESS }, resources.getString(R.string.ui_worker_assigned_2f2f)),
                                Triple(resources.getString(R.string.ui_finished_7e06), completed, resources.getString(R.string.ui_jobs_you_have_completed_f394)),
                                Triple(resources.getString(R.string.ui_under_review_54a3), own.count { it.status == PostStatus.PENDING }, resources.getString(R.string.ui_awaiting_approval_61ce)),
                            )
                            statuses.chunked(2).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { (label, count, description) ->
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        ) {
                                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(count.toString(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                                                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(description, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                            val rejected = own.count { it.status == PostStatus.REJECTED }
                            if (rejected > 0) Card {
                                Text(resources.getString(R.string.rejected_count, rejected), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(resources.getString(R.string.ui_these_posts_were_rejected_check_the_reason_in_my_posts_and_update_f029), style = MaterialTheme.typography.bodyMedium)
                            }
                            Action(if (own.isEmpty()) resources.getString(R.string.ui_post_my_first_job_b8e9) else resources.getString(R.string.ui_view_my_posts_3cdc)) { vm.go(if (own.isEmpty()) "create" else "mine") }
                            Text(resources.getString(R.string.ui_your_community_contributions_30dc), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Card {
                                listOf(
                                    resources.getString(R.string.ui_comments_you_wrote_170f) to s.comments.count { it.author == me?.id },
                                    resources.getString(R.string.ui_interest_in_your_posts_a65d) to own.sumOf { it.interested.size },
                                ).forEach { (label, value) ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        Text(value.toString(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(resources.getString(R.string.ui_interest_is_counted_per_post_a_person_can_be_interested_in_severa_b9fc), style = MaterialTheme.typography.bodySmall)
                            }
                            Card {
                                Text(resources.getString(R.string.ui_your_reputation_9777), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(resources.getString(R.string.level_points, localizedLabel(resources, reputationLevel(points)), points), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(resources.getString(R.string.ui_see_how_close_you_are_to_the_next_level_and_new_badges_5c97), style = MaterialTheme.typography.bodyMedium)
                                Action(resources.getString(R.string.ui_view_my_achievements_b7fc)) { vm.go("achievements") }
                            }
                        }
                        "achievements" -> {
                            val points = s.points[me?.id] ?: 0
                            val levels = listOf("Vecino" to 0, "Colaborador" to 100, "Experto" to 500, "Referente" to 1500)
                            val levelIndex = levels.indexOfLast { points >= it.second }.coerceAtLeast(0)
                            val current = levels[levelIndex]
                            val next = levels.getOrNull(levelIndex + 1)
                            Title(resources.getString(R.string.ui_your_participation_matters_3305))
                            Text(resources.getString(R.string.ui_help_the_community_earn_points_and_unlock_achievements_aa84), style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(resources.getString(R.string.ui_your_current_level_4ead), style = MaterialTheme.typography.labelMedium)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(localizedLabel(resources, current.first), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                        Text("$points pts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                    LinearProgressIndicator(
                                        progress = { if (next == null) 1f else ((points - current.second).toFloat() / (next.second - current.second)).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                    )
                                    Text(if (next == null) resources.getString(R.string.ui_you_reached_the_highest_level_thank_you_for_contributing_2f98) else resources.getString(R.string.next_level_points, next.second - points, localizedLabel(resources, next.first)), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Text(resources.getString(R.string.ui_your_badges_2746), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            val firstCount = s.posts.count { it.author == me?.id }
                            val commentCount = if(vm.usesRemoteAuthentication) s.rewards.count { it.startsWith("${me?.id}:comment:") } else s.comments.count { it.author == me?.id }
                            val verifiedCount = s.rewards.count { it.startsWith("${me?.id}:verify:") }
                            listOf(
                                Triple("first", resources.getString(R.string.ui_your_first_post_6935), Triple(firstCount, 1, resources.getString(R.string.ui_post_a_job_to_get_started_cdf6))),
                                Triple("comments", resources.getString(R.string.ui_helpful_conversations_7a0d), Triple(commentCount, 10, resources.getString(R.string.ui_write_10_comments_on_jobs_e3f0))),
                                Triple("verified", resources.getString(R.string.ui_trusted_posts_15f5), Triple(verifiedCount, 10, resources.getString(R.string.ui_have_10_posts_approved_by_a_moderator_f90b))),
                            ).forEach { (key, label, details) ->
                                val unlocked = "${me?.id}:badge:$key" in s.rewards
                                val count = if (unlocked) details.second else details.first.coerceAtMost(details.second)
                                Card {
                                    Text(if (unlocked) resources.getString(R.string.ui_achievement_unlocked_a707) else resources.getString(R.string.ui_in_progress_c5b6), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(if (unlocked) resources.getString(R.string.ui_you_did_it_this_badge_is_yours_529d) else details.third, style = MaterialTheme.typography.bodyMedium)
                                    if (!unlocked) {
                                        LinearProgressIndicator(progress = { count.toFloat() / details.second }, modifier = Modifier.fillMaxWidth())
                                        Text(resources.getString(R.string.badge_progress, count, details.second), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            Text(resources.getString(R.string.ui_how_do_i_earn_points_ca60), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Card {
                                listOf(resources.getString(R.string.ui_post_a_job_43d8) to "+10", resources.getString(R.string.ui_your_post_is_approved_8c1c) to "+20", resources.getString(R.string.ui_write_a_comment_157c) to "+2", resources.getString(R.string.ui_someone_marks_interested_7c53) to "+1").forEach { (label, value) ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        Text("$value pts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(resources.getString(R.string.ui_each_action_counts_once_removing_and_adding_interest_again_does_n_60ea), style = MaterialTheme.typography.bodySmall)
                            }
                            var showLevels by rememberSaveable { mutableStateOf(false) }
                            TextButton(onClick = { showLevels = !showLevels }) { Text(if (showLevels) resources.getString(R.string.ui_hide_levels_8c98) else resources.getString(R.string.ui_view_all_levels_fe71)) }
                            if (showLevels) Card {
                                levels.forEachIndexed { index, (name, minimum) ->
                                    val maximum = levels.getOrNull(index + 1)?.second?.minus(1)
                                    Text(resources.getString(R.string.level_range, localizedLabel(resources, name), if (maximum == null) resources.getString(R.string.or_more, minimum) else "$minimum–$maximum"), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        "moderation" -> {
                            Title(resources.getString(R.string.ui_let_us_care_for_our_community_a408))
                            Text(
                                resources.getString(R.string.moderation_counts, s.posts.count { it.status == PostStatus.PENDING }, s.posts.count { it.status == PostStatus.VERIFIED })
                            )
                            var filter by rememberSaveable { mutableStateOf(resources.getString(R.string.ui_pending_37f6)) }
                            Choice(
                                resources.getString(R.string.ui_status_843e),
                                filter,
                                listOf("Todas") + PostStatus.entries.map { it.label },
                            ) {
                                filter = it
                            }
                            val list =
                                s.posts.filter { filter == "Todas" || it.status.label == filter }
                            if (list.isEmpty()) Text(resources.getString(R.string.ui_no_posts_with_this_status_d7e3))
                            list.forEach { p ->
                                PostCard(p, name(p.author)) { vm.openPost(p.id, "mod-detail") }
                            }
                        }
                        "mod-detail" -> {
                            if (post != null) {
                                Title(post.title)
                                Text("${localizedLabel(resources, post.status.label)} · ${name(post.author)}")
                                if (post.imageUri != null) AsyncImage(model = post.imageUri, contentDescription = resources.getString(R.string.photo_preview), modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop) else if (post.hasImage) ExampleImage()
                                Text(post.description)
                                Text("${localizedLabel(resources, post.category)} · ${post.city} · ${money(post.budget)}")
                                if (post.status == PostStatus.PENDING) {
                                    Action(resources.getString(R.string.ui_approve_post_c4cc), vm::verify)
                                    Link(resources.getString(R.string.ui_reject_with_reason_cd8f)) { vm.go("reject") }
                                }
                                if (post.status != PostStatus.FINISHED)
                                    Link(resources.getString(R.string.ui_mark_as_finished_ed23)) { vm.go("mod-finish") }
                            }
                        }
                        "reject" -> {
                            Title(resources.getString(R.string.ui_explain_the_reason_930f))
                            Text(post?.title.orEmpty())
                            var reason by rememberSaveable { mutableStateOf("") }
                            Field(resources.getString(R.string.ui_rejection_reason_371a), reason, { reason = it }, true)
                            Text(
                                resources.getString(R.string.ui_the_author_will_receive_the_reason_the_post_will_not_appear_in_th_76f9)
                            )
                            Action(resources.getString(R.string.ui_confirm_rejection_567e)) { vm.reject(reason) }
                        }
                        "error" -> {
                            Title(resources.getString(R.string.ui_could_not_load_the_information_0fd8))
                            Text(
                                resources.getString(R.string.ui_demo_error_screen_in_memory_data_does_not_require_a_connection_265e)
                            )
                            Action(resources.getString(R.string.retry)) { vm.go("home") }
                        }
                        else -> Text(resources.getString(R.string.ui_select_a_menu_option_27f7))
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun PostCard(p: WorkPost, author: String, open: () -> Unit) {
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    Card {
        Text(
            "${localizedLabel(resources, p.category)} · ${localizedLabel(resources, p.status.label)}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(p.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(p.description, maxLines = 2)
        Text("${p.city} · ${money(p.budget)}")
        Text(
            resources.getString(R.string.post_interest_count, author, p.interested.size),
            style = MaterialTheme.typography.bodySmall,
        )
        Action(resources.getString(R.string.ui_view_details_3aa8), open)
    }
}
