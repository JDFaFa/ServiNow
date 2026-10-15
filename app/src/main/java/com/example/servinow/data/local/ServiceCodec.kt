package com.example.servinow.data.local
import com.example.servinow.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
object ServiceCodec {
    fun encode(d: ServiceData): String {
        val offers = JSONArray(); d.offers.forEach { p -> offers.put(JSONObject().put("id",p.id).put("job",p.job).put("title",p.title).put("worker",p.worker).put("price",p.price).put("message",p.message).put("availability",p.availability).put("duration",p.duration).put("mine",p.mine)) }
        val accepted = JSONObject(); d.accepted.forEach { (k,v) -> accepted.put(k,v) }
        val reviews = JSONArray(); d.reviews.forEach { reviews.put(JSONObject().put("job",it.job).put("worker",it.worker).put("stars",it.stars).put("text",it.text)) }
        return JSONObject().put("offers",offers).put("accepted",accepted).put("completed",JSONArray(d.completed.toList())).put("done",JSONArray(d.doneRequests.toList())).put("reviews",reviews).toString()
    }
    fun decode(raw: String): ServiceData {
        val o = JSONObject(raw); val a=o.getJSONArray("offers"); val r=o.getJSONArray("reviews");val accepted=o.getJSONObject("accepted")
        fun strings(key: String): Set<String> { val list=o.getJSONArray(key);return (0 until list.length()).map { list.getString(it) }.toSet() }
        return ServiceData((0 until a.length()).map { i -> a.getJSONObject(i).let { Proposal(it.getString("id"),it.getString("job"),it.getString("title"),it.getString("worker"),it.getLong("price"),it.getString("message"),it.getString("availability"),it.getString("duration"),it.getBoolean("mine")) } },
            accepted.keys().asSequence().associateWith { accepted.getString(it) },strings("completed"),strings("done"),
            (0 until r.length()).map { i -> r.getJSONObject(i).let { ServiceReview(it.getString("job"),it.getString("worker"),it.getInt("stars"),it.getString("text")) } })
    }
}
