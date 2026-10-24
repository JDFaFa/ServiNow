package com.example.servinow.features.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.servinow.R

@Composable
fun WelcomeScreen(onStart: () -> Unit, onLogin: () -> Unit) {
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Spacer(Modifier.height(28.dp))
        Image(painterResource(R.drawable.servinow_logo), resources.getString(R.string.ui_servinow_logo_house_and_lightning_b64f), Modifier.size(124.dp))
        Text("ServiNow", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(resources.getString(R.string.ui_people_who_help_njobs_that_connect_2f06), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(resources.getString(R.string.ui_find_the_help_you_need_or_new_opportunities_for_your_skills_2497), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(resources.getString(R.string.ui_a_place_to_collaborate_8205), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(resources.getString(R.string.ui_post_a_request_talk_and_choose_who_to_work_with_37e6), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) { Text(resources.getString(R.string.ui_get_started_fc75)) }
        TextButton(onClick = onLogin) { Text(resources.getString(R.string.ui_already_have_an_account_sign_in_a4b4)) }
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit, onLogin: () -> Unit) {
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    val titles = listOf(resources.getString(R.string.ui_tell_us_what_you_need_1710), resources.getString(R.string.ui_share_your_skills_ab4c), resources.getString(R.string.ui_make_an_informed_choice_b59e))
    val descriptions = listOf(
        resources.getString(R.string.ui_post_a_job_choose_its_location_and_receive_offers_from_the_commun_79ae),
        resources.getString(R.string.ui_explore_jobs_send_your_offer_and_agree_on_the_details_with_the_pe_2baa),
        resources.getString(R.string.ui_read_profiles_and_reviews_track_the_job_and_share_your_experience_001d),
    )
    val icons = listOf(R.drawable.ic_home, R.drawable.ic_work, R.drawable.ic_account)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(resources.getString(R.string.ui_discover_servinow_d923), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onLogin) { Text(resources.getString(R.string.ui_skip_bef5)) }
        }
        Text(resources.getString(R.string.onboarding_step, step + 1), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                Surface(modifier = Modifier.width(42.dp).height(6.dp), color = if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(3.dp)) {}
            }
        }
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(32.dp), modifier = Modifier.padding(vertical = 18.dp).size(152.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(painterResource(icons[step]), null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Text(titles[step], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(descriptions[step], textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = { if (step < 2) step++ else onFinish() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) { Text(if (step < 2) resources.getString(R.string.ui_next_3b6d) else resources.getString(R.string.ui_create_my_account_ccc8)) }
        if (step > 0) TextButton(onClick = { step-- }) { Text(resources.getString(R.string.ui_back_97b8)) }
        if (step == 2) TextButton(onClick = onLogin) { Text(resources.getString(R.string.ui_i_already_have_an_account_af47)) }
    }
}
