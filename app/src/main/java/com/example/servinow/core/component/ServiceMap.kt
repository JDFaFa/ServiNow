package com.example.servinow.core.component

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.servinow.R
import com.example.servinow.domain.model.Job
import com.example.servinow.domain.model.SearchFilters
import com.example.servinow.domain.model.WorkPost
import com.example.servinow.features.explore.ExploreMap
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationSourceOptions
import com.mapbox.maps.plugin.annotation.ClusterOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import kotlinx.coroutines.awaitCancellation

/** Reusable map. Coordinates and selected post belong to its caller, never to the map. */
@Composable
fun ServiceMap(
    latitude: Double, longitude: Double, modifier: Modifier = Modifier,
    posts: List<WorkPost> = emptyList(), selectedPoint: Point? = null,
    interactive: Boolean = true, onPost: (WorkPost) -> Unit = {},
    onPoint: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val token = stringResource(R.string.mapbox_access_token)
    val pointCallback by rememberUpdatedState(onPoint)
    val postCallback by rememberUpdatedState(onPost)
    if (!token.startsWith("pk.")) {
        // Preserve the working public map until the team configures Mapbox locally.
        Box(modifier) {
            key(posts.map { it.id }) {
                ExploreMap(posts.mapIndexed { i, p -> Job(i, p.title, p.category, p.city, p.budget.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), 0, p.interested.size, p.latitude, p.longitude) },
                    SearchFilters(latitude = latitude, longitude = longitude), Modifier.matchParentSize(),
                    { i -> posts.getOrNull(i)?.let(postCallback) },
                    if (interactive) onPoint else null)
            }
            if (!interactive) Box(Modifier.matchParentSize().pointerInput(Unit) {})
        }
        return
    }
    val permissions = remember { arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) }
    fun granted() = permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    var hasPermission by remember { mutableStateOf(granted()) }
    var request by remember { mutableIntStateOf(0) }
    var denied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermission = result.values.any { it }
        if (hasPermission) request++ else denied = true
    }
    LifecycleResumeEffect(Unit) {
        hasPermission = granted()
        onPauseOrDispose { }
    }
    val viewport = rememberMapViewportState { setCameraOptions { center(Point.fromLngLat(longitude, latitude)); zoom(13.5) } }
    LaunchedEffect(latitude, longitude) {
        viewport.setCameraOptions { center(Point.fromLngLat(longitude, latitude)) }
    }
    val marker = rememberIconImage(key = R.drawable.map_pin_blue, painter = painterResource(R.drawable.map_pin_blue))
    val groups = remember(posts) { posts.groupBy { it.category } }
    val byId = remember(posts) { posts.associateBy { it.id } }
    val clusters = remember { AnnotationConfig(annotationSourceOptions = AnnotationSourceOptions(clusterOptions = ClusterOptions(clusterRadius = 60L))) }
    Box(modifier) {
        MapboxMap(modifier = Modifier.matchParentSize(), mapViewportState = viewport,
            onMapClickListener = { p -> if (interactive) pointCallback?.invoke(p.latitude(), p.longitude()); true }) {
            groups.forEach { (category, group) ->
                val icon = rememberIconImage(key = category, painter = painterResource(when (category) {
                    "Electricidad", "Mecánica" -> R.drawable.map_pin_orange
                    "Jardinería", "Limpieza" -> R.drawable.map_pin_green
                    else -> R.drawable.map_pin_blue
                }))
                val annotations = remember(group) { group.map { p -> PointAnnotationOptions().withPoint(Point.fromLngLat(p.longitude, p.latitude)).withData(JsonPrimitive(p.id)) } }
                PointAnnotationGroup(annotations = annotations, annotationConfig = clusters) {
                    iconImage = icon
                    interactionsState.onClicked { annotation ->
                        val post = annotation.getData()?.asString?.let(byId::get)
                        if (interactive) post?.let(postCallback)
                        post != null
                    }
                }
            }
            selectedPoint?.let { p -> PointAnnotation(point = p) { iconImage = marker } }
            MapEffect(hasPermission, request) { view ->
                view.location.updateSettings { enabled = hasPermission && request > 0; locationPuck = createDefault2DPuck(withBearing = true) }
                if (hasPermission && request > 0) {
                    var first = true
                    val listener = OnIndicatorPositionChangedListener { point ->
                        if (first) {
                            first = false
                            viewport.setCameraOptions { center(point); zoom(15.5) }
                            pointCallback?.invoke(point.latitude(), point.longitude())
                        }
                    }
                    view.location.addOnIndicatorPositionChangedListener(listener)
                    try { awaitCancellation() } finally { view.location.removeOnIndicatorPositionChangedListener(listener) }
                }
            }
        }
        if (!interactive) Box(Modifier.matchParentSize().pointerInput(Unit) {})
        else SmallFloatingActionButton(onClick = { if (hasPermission) request++ else permissionLauncher.launch(permissions) }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            Text(stringResource(R.string.map_my_location), modifier = Modifier.padding(8.dp))
        }
    }
    if (denied) AlertDialog(onDismissRequest = { denied = false }, text = { Text(stringResource(R.string.map_permission_denied)) }, confirmButton = {
        TextButton(onClick = { denied = false }) { Text(stringResource(R.string.photo_ok)) }
    })
}
