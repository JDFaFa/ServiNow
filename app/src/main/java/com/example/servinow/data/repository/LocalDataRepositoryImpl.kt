package com.example.servinow.data.repository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.example.servinow.domain.repository.*
class LocalDataRepositoryImpl @Inject constructor(@param:ApplicationContext private val context:Context, private val account:AccountRepository):LocalDataRepository {
 override fun clear():Boolean {
  val results=listOf("account_local","publications_local","services_local","explore_local").map { context.getSharedPreferences(it,0).edit().clear().commit() }
  account.reload()
  return results.all { it }
 }
}
