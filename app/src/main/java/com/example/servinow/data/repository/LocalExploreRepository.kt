package com.example.servinow.data.repository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.json.JSONObject
import org.json.JSONArray
import com.example.servinow.domain.model.ExploreActivity
import com.example.servinow.domain.repository.ExploreRepository
class LocalExploreRepository @Inject constructor(@ApplicationContext context: Context): ExploreRepository {
 private val prefs=context.getSharedPreferences("explore_local",0)
 override fun jobs()=com.example.servinow.data.demo.exampleJobs
 override fun read():ExploreActivity {
  val votes=prefs.getStringSet("interested",emptySet()).orEmpty().mapNotNull(String::toIntOrNull).toSet()
  val comments=runCatching { val json=JSONObject(prefs.getString("comments","{}")!!)
   json.keys().asSequence().mapNotNull { key -> key.toIntOrNull()?.let { id -> val a=json.getJSONArray(key);id to (0 until a.length()).map { a.getString(it) } } }.toMap()
  }.getOrDefault(emptyMap())
  return ExploreActivity(votes,comments)
 }
 override fun save(activity:ExploreActivity):Boolean=runCatching {
  val json=JSONObject();activity.comments.forEach { (id,values)->json.put(id.toString(),JSONArray(values)) }
  prefs.edit().putStringSet("interested",activity.interested.map(Int::toString).toSet()).putString("comments",json.toString()).commit()
 }.getOrDefault(false)
}
