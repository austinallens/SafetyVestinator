package com.example.safetyvestinator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.safetyvestinator.data.GpsLocation
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun VestMapView(
    location: GpsLocation?,
    modifier: Modifier = Modifier
) {
    val mapView = remember { mutableStateOfMapView() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(19.0)

                // Default to Center of IESB until we have a real location
                controller.setCenter(GeoPoint(32.526401899687684, -92.6437512553508))

                mapView.value = this
            }
        },
        update = { view ->
            location?.let {
                val point = GeoPoint(it.latitude, it.longitude)
                view.controller.animateTo(point)

                view.overlays.removeAll { overlay -> overlay is Marker }
                val marker = Marker(view).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Vest"
                }
                view.overlays.add(marker)
                view.invalidate()
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            mapView.value?.onDetach()
        }
    }
}

private fun mutableStateOfMapView() =
    androidx.compose.runtime.mutableStateOf<MapView?>(null)