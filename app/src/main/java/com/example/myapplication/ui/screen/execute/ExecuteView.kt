package com.example.myapplication.ui.screen.execute

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.FeatureLayer
import com.example.myapplication.GlobalData
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import com.example.myapplication.BuildConfig

@Composable
fun ExecuteView(appNavController: NavHostController) {
    // FeatureLayer 状态
    var featureLayer by remember { mutableStateOf<FeatureLayer?>(null) }

    // 记忆 MapView 并处理它的生命周期
    val mapView = rememberMapViewWithLifecycle()

    // 初始化并只加载一次底图
    val map = remember {
        ArcGISMap(BasemapStyle.ArcGISImageryStandard).also {
            ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 当退出 ExecuteView 时移除 FeatureLayer
            map.operationalLayers.removeAll { it is FeatureLayer }
        }
    }

    // 定位
    val locationDisplay = mapView.locationDisplay
    locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
    LaunchedEffect(locationDisplay) {
        locationDisplay.dataSource.start()
    }

    featureLayer = GlobalData.featureLayer

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView }
    ) { mapView ->
        mapView.map = map

        // 在地图上添加 FeatureLayer
        featureLayer?.let { layer ->
            // 检查当前 Map 实例是否已经包含这个 FeatureLayer
            if (!map.operationalLayers.contains(layer)) {
                // 移除先前的相同类型的 FeatureLayer
                map.operationalLayers.removeAll { it is FeatureLayer }
                // 添加新的 FeatureLayer
                map.operationalLayers.add(layer)
            }
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply {
            // 初始化地图视图
        }
    }

    // 确保 MapView 能够感知到 Compose 的生命周期
    DisposableEffect(lifecycleOwner, mapView) {
        // 添加观察者
        lifecycleOwner.lifecycle.addObserver(mapView)
        // 当组件销毁时，这个块会被调用
        onDispose {
            // 当组件销毁时，这个块会被调用
            lifecycleOwner.lifecycle.removeObserver(mapView)
        }
    }

    return mapView
}