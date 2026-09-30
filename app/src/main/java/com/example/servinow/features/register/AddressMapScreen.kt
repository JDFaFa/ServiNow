package com.example.servinow.features.register

import com.example.servinow.core.component.screenSpacing
import com.example.servinow.core.component.mapHeight

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AddressMapScreen(state: RegisterUiState, onSelect: (Double, Double) -> Unit,
    onConfirm: () -> Unit, onBack: () -> Unit) {
    val select by rememberUpdatedState(onSelect)
    var retry by remember { mutableIntStateOf(0) }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding().verticalScroll(rememberScrollState()).padding(screenSpacing()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack) { Text("← Volver al registro") }
            Text("¿Dónde te encuentras?", style = MaterialTheme.typography.headlineMedium)
            Text("Mueve y amplía el mapa. Toca el lugar de tu dirección para colocar el marcador.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            key(retry) {
                AndroidView(modifier = Modifier.fillMaxWidth().height(mapHeight()).clip(RoundedCornerShape(20.dp)),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.userAgentString += " ServiNow/1.0 (com.example.servinow)"
                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                    if (request.url.host == "appassets.androidplatform.net") {
                                        val file = request.url.lastPathSegment
                                        if (file == "leaflet.js" || file == "leaflet.css") {
                                            return WebResourceResponse(if (file.endsWith("js")) "text/javascript" else "text/css",
                                                "UTF-8", context.assets.open("map/$file"))
                                        }
                                        return WebResourceResponse("text/plain", "UTF-8", "".byteInputStream())
                                    }
                                    return null
                                }
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val uri = request.url
                                    if (request.isForMainFrame && uri.scheme == "servinow" && uri.host == "location") {
                                        val lat = uri.getQueryParameter("lat")?.toDoubleOrNull()
                                        val lng = uri.getQueryParameter("lng")?.toDoubleOrNull()
                                        if (lat != null && lng != null) select(lat, lng)
                                    } else if (request.isForMainFrame && uri.scheme == "https" && uri.host == "www.openstreetmap.org") {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                    }
                                    return true
                                }
                            }
                            loadDataWithBaseURL("https://appassets.androidplatform.net/map/",
                                mapHtml(state.latitude, state.longitude), "text/html", "UTF-8", null)
                        }
                    }, onRelease = { it.stopLoading(); it.destroy() })
            }
            Text(if (state.latitude == null) "Aún no has seleccionado una dirección."
                else String.format(Locale.US, "Ubicación seleccionada: %.6f, %.6f", state.latitude, state.longitude),
                style = MaterialTheme.typography.bodyMedium)
            if (state.locationConfirmed) Text("Ubicación confirmada", color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = { retry++ }) { Text("Recargar mapa") }
            Button(onClick = onConfirm, enabled = state.latitude != null,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Confirmar dirección")
            }
        }
    }
}

private fun mapHtml(latitude: Double?, longitude: Double?): String {
    val lat = latitude ?: 4.7110
    val lng = longitude ?: -74.0721
    return """
        <!doctype html><html lang="es"><head>
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <link rel="stylesheet" href="leaflet.css">
        <style>html,body,#map{height:100%;margin:0}#error{display:none;position:absolute;top:12px;left:52px;right:12px;z-index:1000;background:white;padding:12px;border-radius:12px;font:14px sans-serif;color:#172d4d}</style>
        </head><body><div id="map" aria-label="Selecciona tu dirección en el mapa"></div>
        <div id="error">No se pudo cargar el mapa. Revisa tu conexión y pulsa Recargar mapa.</div>
        <script src="leaflet.js"></script><script>
        const map=L.map('map').setView([$lat,$lng],${if (latitude == null) 12 else 17});
        const tiles=L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{
          maxZoom:19, attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'}).addTo(map);
        tiles.on('tileerror',()=>document.getElementById('error').style.display='block');
        tiles.on('tileload',()=>document.getElementById('error').style.display='none');
        let marker;
        function mark(p){if(marker)map.removeLayer(marker);marker=L.circleMarker(p,{radius:12,color:'#fff',weight:3,fillColor:'#1664c0',fillOpacity:1}).addTo(map);}
        ${if (latitude != null) "mark([$lat,$lng]);" else ""}
        map.on('click',e=>{mark(e.latlng);window.location.href='servinow://location?lat='+e.latlng.lat+'&lng='+e.latlng.lng;});
        </script></body></html>
    """.trimIndent()
}
