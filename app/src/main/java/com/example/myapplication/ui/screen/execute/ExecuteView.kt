package com.example.myapplication.ui.screen.execute

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.NinePatchDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.MapView
import com.example.myapplication.BuildConfig
import com.example.myapplication.R

@Composable
fun ExecuteView(appNavController: NavHostController) {
    // 记忆并处理MapView的生命周期
    val mapView = rememberMapViewWithLifecycle()
    // 初始化地图并设置API密钥
    val map = rememberMapWithApiKey()
    // 获取地图的位置显示
    val locationDisplay = mapView.locationDisplay

    // 设置位置显示的图标和模式
    SetupLocationDisplay(locationDisplay)
    // 处理设备方向传感器更新
    HandleSensorUpdates(locationDisplay)

    // 从全局数据中获取FeatureLayer
    val featureLayer = GlobalData.featureLayer
    // 显示地图和FeatureLayer
    DisplayMap(mapView, map, featureLayer)
}

@Composable
fun rememberMapWithApiKey(): ArcGISMap {
    // 使用remember保证只初始化一次
    return remember {
        ArcGISMap(BasemapStyle.ArcGISImageryStandard).also {
            // 设置ArcGIS API密钥
            ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        }
    }
}

@Composable
fun SetupLocationDisplay(locationDisplay: LocationDisplay) {
    // 获取当前上下文
    val context = LocalContext.current
    // 获取导航图标的drawable资源
    val navigationDrawable = ContextCompat.getDrawable(context, R.drawable.locationdisplaynavigation) as? NinePatchDrawable
    // 将drawable转换为PictureMarkerSymbol
    val navigationSymbol = navigationDrawable?.toBitmap()?.let { BitmapDrawable(context.resources, it) }
        ?.let { PictureMarkerSymbol.createWithImage(it) }

    // 应用导航图标到位置显示
    navigationSymbol?.let {
        locationDisplay.defaultSymbol = it
        // 设置自动居中模式
        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
    }

    // 启动位置显示的数据源
    LaunchedEffect(locationDisplay) {
        locationDisplay.dataSource.start()
    }
}

@Composable
fun HandleSensorUpdates(locationDisplay: LocationDisplay) {
    // 获取当前上下文
    val context = LocalContext.current
    // 获取系统的传感器服务
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    // 获取旋转向量传感器
    val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    // 创建并记住传感器事件监听器
    val sensorEventListener = rememberSensorEventListener(locationDisplay)

    // 注册传感器监听器，并在Composable移除时注销
    DisposableEffect(sensorManager, sensorEventListener) {
        sensorManager?.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager?.unregisterListener(sensorEventListener)
        }
    }
}

@Composable
fun rememberSensorEventListener(locationDisplay: LocationDisplay): SensorEventListener {
    // 获取位置显示的默认符号作为PictureMarkerSymbol
    val navigationSymbol = locationDisplay.defaultSymbol as? PictureMarkerSymbol
    // 创建并记住传感器事件监听器
    return remember {
        object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // 当传感器类型为旋转向量时
                    if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        // 获取方位角并更新图标旋转角度
                        val azimuth = getAzimuthFromSensorEvent(it)
                        navigationSymbol?.angle = azimuth
                    }
                }
            }
        }
    }
}

// 从传感器事件中提取方位角
fun getAzimuthFromSensorEvent(event: SensorEvent): Float {
    // 创建旋转矩阵
    val rotationMatrix = FloatArray(9)
    // 从旋转向量获取旋转矩阵
    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
    // 创建方向数组
    val orientationAngles = FloatArray(3)
    // 获取方向
    SensorManager.getOrientation(rotationMatrix, orientationAngles)
    // 返回以度为单位的方位角
    return Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
}

@Composable
fun DisplayMap(mapView: MapView, map: ArcGISMap, featureLayer: FeatureLayer?) {
    // 使用AndroidView来显示MapView
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView }
    ) { mapView ->
        // 设置地图实例
        mapView.map = map
        // 如果FeatureLayer不为空，则添加到地图的操作图层中
        featureLayer?.let { layer ->
            if (!map.operationalLayers.contains(layer)) {
                // 移除之前的FeatureLayer
                map.operationalLayers.removeAll { it is FeatureLayer }
                // 添加新的FeatureLayer
                map.operationalLayers.add(layer)
            }
        }
    }

    // 当Composable被移除时执行清理
    DisposableEffect(mapView) {
        onDispose {
            // 移除FeatureLayer
            featureLayer?.let { layer ->
                map.operationalLayers.remove(layer)
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