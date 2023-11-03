package com.example.myapplication.ui.screen.execute

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.NinePatchDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.example.myapplication.BuildConfig
import com.example.myapplication.R

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
    // 获取 NinePatchDrawable 的实例
    val navigationDrawable = ContextCompat.getDrawable(LocalContext.current, R.drawable.locationdisplaynavigation) as? NinePatchDrawable
    // 将 NinePatchDrawable 转换为 Bitmap
    val navigationBitmap = navigationDrawable?.toBitmap()
    // 将 Bitmap 转换为 BitmapDrawable
    val navigationBitmapDrawable = navigationBitmap?.let { BitmapDrawable(LocalContext.current.resources, it) }
    // 创建 PictureMarkerSymbol
    val navigationSymbol = navigationBitmapDrawable?.let { PictureMarkerSymbol.createWithImage(it) }
    // 应用到 LocationDisplay
    navigationSymbol?.let {
        locationDisplay.defaultSymbol = it // 把默认图标给更换成导航图标
    }
    // 重新居中模式
    locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)

    // 获取系统服务
    val context = LocalContext.current
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // 创建一个 SensorEventListener
    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        // 计算旋转矩阵
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                        // 计算设备的方向
                        val orientationAngles = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        // 更新位置显示的图标方向
                        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                        // 这里可以根据方位角更新图标的旋转
                        // 注意：您可能需要将 azimuth 转换为适合您图标的旋转角度
                        // 更新图标的旋转
                        navigationSymbol?.angle = azimuth
                    }
                }
            }
        }
    }

    // 注册传感器监听器
    DisposableEffect(sensorManager, sensorEventListener) {
        sensorManager?.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager?.unregisterListener(sensorEventListener)
        }
    }

    LaunchedEffect(locationDisplay) {
        //启动地图视图的位置显示
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