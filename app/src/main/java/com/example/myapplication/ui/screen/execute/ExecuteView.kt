package com.example.myapplication.ui.screen.execute

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.FeatureLayer
import com.example.myapplication.GlobalData
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ExecuteView(appNavController: NavHostController) {
    // FeatureLayer 状态
    var featureLayer by remember { mutableStateOf<FeatureLayer?>(null) }

    LaunchedEffect(Unit) {
        // 加载 Shapefile 数据并设置 FeatureLayer
        loadShapefile { layer ->
            featureLayer = layer
        }
    }

    // 当 featureLayer 加载完成，显示地图
    featureLayer?.let { layer ->
        MapViewContainer(featureLayer = layer)
    }
}

// 显示 ArcGIS 地图的 Composable
@Composable
fun MapViewContainer(featureLayer: FeatureLayer) {
    AndroidView(factory = { context ->
        MapView(context).apply {
            // 设置基础地图样式
            map = ArcGISMap().also {
                it.operationalLayers.add(featureLayer)
                // 可以调整视点以适应你的 shapefile 数据
                setViewpoint(Viewpoint(30.0, -100.0, 1000.0))
            }
        }
    })
}

// 加载shapefile数据的函数，现在支持一个回调来处理 FeatureLayer
private suspend fun loadShapefile(onLayerLoaded: (FeatureLayer) -> Unit) {
    // shapefile数据已经在另一个页面读取出来，这里直接拿来用
    val shapeFileTable = GlobalData.shapeFile
    shapeFileTable?.let {
        shapeFileTable.load().onSuccess {
            // 使用 shapefile 加载要素图层
            val featureLayer = FeatureLayer.createWithFeatureTable(shapeFileTable)
            onLayerLoaded(featureLayer)
        }.onFailure {
            Log.d("Shapefile", "Error loading shapefile: ${it.message}")
        }
    }
}