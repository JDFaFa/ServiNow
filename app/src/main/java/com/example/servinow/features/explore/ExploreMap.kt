package com.example.servinow.features.explore

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun ExploreMap(jobs: List<Job>, filters: SearchFilters, modifier: Modifier,
    onSelect: (Int) -> Unit, onLocation: ((Double, Double) -> Unit)? = null) {
    val select by rememberUpdatedState(onSelect)
    val location by rememberUpdatedState(onLocation)
    AndroidView(modifier = modifier, factory = { context -> WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.userAgentString += " ServiNow/1.0 (com.example.servinow)"
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (request.url.host != "appassets.androidplatform.net") return null
                val file = request.url.lastPathSegment
                return if (file == "leaflet.js" || file == "leaflet.css") WebResourceResponse(
                    if (file.endsWith("js")) "text/javascript" else "text/css", "UTF-8", context.assets.open("map/$file"))
                else WebResourceResponse("text/plain", "UTF-8", "".byteInputStream())
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                if (request.isForMainFrame && u.scheme == "servinow") {
                    if (u.host == "job") u.getQueryParameter("id")?.toIntOrNull()?.let { id -> if (jobs.any { it.id == id }) select(id) }
                    if (u.host == "point") {
                        val lat = u.getQueryParameter("lat")?.toDoubleOrNull()
                        val lng = u.getQueryParameter("lng")?.toDoubleOrNull()
                        if (lat != null && lng != null && lat.isFinite() && lng.isFinite() && lat in -90.0..90.0 && lng in -180.0..180.0) location?.invoke(lat, lng)
                    }
                } else if (request.isForMainFrame && u.scheme == "https" && u.host == "www.openstreetmap.org") {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, u)) }
                }
                return true
            }
        }
        val markers = jobs.joinToString("\n") { j -> "L.marker([${j.latitude},${j.longitude}],{icon:L.divIcon({className:'price',html:'${j.price/1000} mil',iconSize:[70,32]})}).addTo(map).on('click',()=>window.location.href='servinow://job?id=${j.id}');" }
        val picker = if (onLocation != null) """
            let pin=L.circleMarker([${filters.latitude},${filters.longitude}],{color:'#1664c0'}).addTo(map);
            map.on('click',e=>{pin.setLatLng(e.latlng);window.location.href='servinow://point?lat='+e.latlng.lat+'&lng='+e.latlng.lng;});
        """ else ""
        loadDataWithBaseURL("https://appassets.androidplatform.net/map/", """
          <!doctype html><html lang="es"><head><meta name="viewport" content="width=device-width,initial-scale=1">
          <link rel="stylesheet" href="leaflet.css"><style>html,body,#map{height:100%;margin:0}.price{background:#1664c0;color:white;border-radius:12px;text-align:center;line-height:32px;font:bold 14px/32px sans-serif}#error{display:none;position:absolute;top:10px;left:50px;right:10px;background:white;padding:10px;z-index:1000;font:14px sans-serif}</style></head>
          <body><div id="map"></div><div id="error">No se pudo cargar el mapa. Revisa tu conexión y vuelve a abrir esta vista.</div><script src="leaflet.js"></script><script>
          const map=L.map('map').setView([${filters.latitude},${filters.longitude}],13);
          const tiles=L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'}).addTo(map);
          tiles.on('tileerror',()=>document.getElementById('error').style.display='block');
          tiles.on('tileload',()=>document.getElementById('error').style.display='none');
          $markers $picker
          </script></body></html>
        """.trimIndent(), "text/html", "UTF-8", null)
    } }, onRelease = { it.stopLoading(); it.destroy() })
}
