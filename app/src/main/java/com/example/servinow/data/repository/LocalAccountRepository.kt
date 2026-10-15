package com.example.servinow.data.repository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.example.servinow.domain.model.*
import com.example.servinow.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.json.JSONArray
class LocalAccountRepository @Inject constructor(@ApplicationContext app:Context):AccountRepository {
 private val prefs=app.getSharedPreferences("account_local",0)
 private val _data=MutableStateFlow(readAccount());override val data=_data.asStateFlow()
 private val _notices=MutableStateFlow(readNotices());override val notices=_notices.asStateFlow()
 private val _points=MutableStateFlow(prefs.getInt("points",0));override val points=_points.asStateFlow()
 private val earned=prefs.getStringSet("earned",emptySet()).orEmpty().toMutableSet()
 private fun readAccount():AccountData=runCatching { val o=JSONObject(prefs.getString("account","{}")!!);AccountData(o.optString("name"),o.optString("email"),o.optString("phone"),o.optString("photo"),if(o.has("lat"))o.getDouble("lat") else null,if(o.has("lng"))o.getDouble("lng") else null,o.optString("title"),o.optString("about"),o.optString("years"),o.optString("specialty","Plomería"),o.optString("cv"),o.optBoolean("worker")) }.getOrDefault(AccountData())
 private fun readNotices():List<Notice> = runCatching { val a=JSONArray(prefs.getString("notices","[]"));(0 until a.length()).map { a.getJSONObject(it).let { Notice(it.getString("id"),it.getString("text"),it.getString("target"),it.optString("post"),it.optBoolean("read")) } } }.getOrDefault(emptyList())
 override fun save(a:AccountData):Boolean { val o=JSONObject().put("name",a.name.trim()).put("email",a.email.trim()).put("phone",a.phone).put("photo",a.photo).put("lat",a.lat).put("lng",a.lng).put("title",a.title).put("about",a.about).put("years",a.years).put("specialty",a.specialty).put("cv",a.cv).put("worker",a.worker);if(!prefs.edit().putString("account",o.toString()).commit())return false;_data.value=a;return true }
 private fun storeNotices(n:List<Notice>) { val a=JSONArray();n.forEach { a.put(JSONObject().put("id",it.id).put("text",it.text).put("target",it.target).put("post",it.post).put("read",it.read)) };if(prefs.edit().putString("notices",a.toString()).commit())_notices.value=n }
 override fun mark(id:String,read:Boolean)=storeNotices(_notices.value.map { if(it.id==id)it.copy(read=read) else it })
 override fun markAll()=storeNotices(_notices.value.map { it.copy(read=true) })
 override fun sync(posts:List<Publication>,comments:Int) {
  var total=_points.value;val notices=_notices.value.toMutableList()
  fun reward(key:String,value:Int,text:String,target:String,post:String="") { if(earned.add(key)){total+=value;notices.add(0,Notice(key,text,target,post))} }
  posts.forEach { reward("created-${it.id}",10,"Publicación guardada: ${it.title}. Pendiente de verificación.","mine",it.id);if(it.status=="Verificada")reward("verified-${it.id}",20,"Publicación verificada: ${it.title}","mine",it.id) }
  for(i in 1..comments)if(earned.add("comment-$i"))total+=2
  if(earned.any { it.startsWith("created-") })reward("badge-first",0,"Logro desbloqueado: primera publicación.","achievements")
  if(comments>=10)reward("badge-comments",0,"Logro desbloqueado: diez comentarios.","achievements")
  if(earned.count { it.startsWith("verified-") }>=10)reward("badge-verified",0,"Logro desbloqueado: diez publicaciones verificadas.","achievements")
  if(prefs.edit().putStringSet("earned",earned.toSet()).putInt("points",total).commit())_points.value=total
  if(notices!=_notices.value)storeNotices(notices)
 }
 override fun achieved(key:String)=key in earned
 override fun reload() { _data.value=readAccount();_notices.value=readNotices();_points.value=prefs.getInt("points",0);earned.clear();earned.addAll(prefs.getStringSet("earned",emptySet()).orEmpty()) }
}
