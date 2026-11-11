package com.example.servinow

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.AndroidEntryPoint
import com.example.servinow.core.theme.ServiNowTheme
import com.example.servinow.features.community.CommunityRoute
import com.example.servinow.domain.model.MemberNotice

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val pendingNotice=mutableStateOf<MemberNotice?>(null)
    private fun readNotice(intent:Intent?) {
        val recipient=intent?.getStringExtra("notice_recipient") ?: return
        val post=intent.getStringExtra("notice_post") ?: return
        val id=intent.getStringExtra("notice_id") ?: return
        if(listOf(recipient,post,id).any { !it.matches(Regex("[A-Za-z0-9_-]{1,300}")) })return
        pendingNotice.value=MemberNotice(id,recipient,"ServiNow",intent.getStringExtra("notice_body").orEmpty(),post)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(savedInstanceState==null)readNotice(intent)
        enableEdgeToEdge()
        setContent { ServiNowTheme { CommunityRoute(notification=pendingNotice.value,onNotificationHandled={pendingNotice.value=null}) } }
    }
    override fun onNewIntent(intent:Intent) { super.onNewIntent(intent);readNotice(intent) }
}
