package com.example.servinow

import android.app.LocaleManager
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import com.example.servinow.data.repository.local.SessionRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

class FreshDemoRule : ExternalResource() {
    override fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("es")
        runBlocking {
            val sessions = SessionRepositoryImpl(context)
            sessions.saveSession(null)
            sessions.setWelcomeSeen(false)
            sessions.setRememberSession(true)
        }
    }
}
