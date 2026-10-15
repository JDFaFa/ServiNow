package com.example.servinow.data.repository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.example.servinow.domain.model.Publication
import com.example.servinow.domain.repository.PublicationRepository
import org.json.JSONArray
import org.json.JSONObject
class LocalPublicationRepository @Inject constructor(@ApplicationContext context: Context): PublicationRepository {
 private val prefs = context.getSharedPreferences("publications_local", 0)
    override fun read(): List<Publication> = runCatching {
        val a = JSONArray(prefs.getString("posts", "[]"))
        (0 until a.length()).map { i -> a.getJSONObject(i).let { o ->
            val photos = o.getJSONArray("photos")
            Publication(o.getString("id"), o.getString("title"), o.getString("category"), o.getString("description"),
                o.getString("budget"), (0 until photos.length()).map { photos.getString(it) },
                o.getDouble("lat"), o.getDouble("lng"), o.getString("status"), o.optString("reason"))
        } }
    }.getOrDefault(emptyList())
    override fun save(posts: List<Publication>): Boolean = runCatching {
        val a = JSONArray()
        posts.forEach { p -> a.put(JSONObject().put("id",p.id).put("title",p.title).put("category",p.category)
            .put("description",p.description).put("budget",p.budget).put("photos",JSONArray(p.photos))
            .put("lat",p.latitude).put("lng",p.longitude).put("status",p.status).put("reason",p.reason)) }
        prefs.edit().putString("posts",a.toString()).commit()
    }.getOrDefault(false)
}
