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
import com.arcgismaps.location.NmeaLocationDataSource
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.MapView
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import kotlinx.coroutines.launch

@Composable
fun ExecuteView(appNavController: NavHostController) {
    // 记忆并处理MapView的生命周期
    val mapView = rememberMapViewWithLifecycle()
    // 初始化地图并设置API密钥
    val map = rememberMapWithApiKey()
    // 获取地图的位置显示
    val locationDisplay = mapView.locationDisplay

    // 创建NMEA位置数据源
    val nmeaLocationDataSource = remember { NmeaLocationDataSource() }

    // 设置位置显示的符号、显示的数据源和模式:使用NMEA
    SetupLocationDisplay(locationDisplay, nmeaLocationDataSource)
    // 处理设备传感器更新，让定位符号变成指南针
    HandleSensorUpdates(locationDisplay)

    // 从全局数据中获取FeatureLayer
    val featureLayer = GlobalData.featureLayer
    // 显示地图和FeatureLayer
    DisplayMap(mapView, map, featureLayer)

    // 获取应用的Context
    val context = LocalContext.current
    // 创建SerialPortClient实例
    val serialPortClient = remember { SerialPortClient(context) }

    // 启动串口服务客户端并接收NMEA数据
    LaunchedEffect(Unit) {
        serialPortClient.start()

        // 在协程中收集NMEA数据
        launch {
            serialPortClient.nmeaDataFlow.collect { nmeaData ->
                // 处理接收到的NMEA数据
                nmeaLocationDataSource.pushData(nmeaData.toByteArray())
            }

//            serialPortClient.nmeaDataFlow.collect { nmeaData ->
//                // 处理接收到的NMEA数据:
//                // 只需要GNRMC和GNGGA，把GNRMC中的航向信息清空
//                if (nmeaData.startsWith("\$GNRMC")) {
//                    val nmeaFields = nmeaData.split(",").toMutableList()
//                    nmeaFields[8] = "" // 清空航向数据
//                    nmeaLocationDataSource.pushData(nmeaFields.joinToString(",").toByteArray())
//                }
//                if (nmeaData.startsWith("\$GNGGA")) {
//                    nmeaLocationDataSource.pushData(nmeaData.toByteArray())
//                }
//            }
        }
    }

    // 当Compose退出时停止串口服务客户端
    DisposableEffect(Unit) {
        onDispose {
            serialPortClient.stop()
        }
    }
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
fun SetupLocationDisplay(locationDisplay: LocationDisplay, nmeaLocationDataSource: NmeaLocationDataSource) {
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
        locationDisplay.courseSymbol = it //courseSymbol 通常用于表示设备的移动方向
        locationDisplay.headingSymbol = it //headingSymbol 用于表示设备面对的方向
    }

    // 设置自动居中模式
    locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)

    // 启动位置显示的数据源
    LaunchedEffect(locationDisplay) {
        // 设置NMEA位置数据源
        locationDisplay.dataSource = nmeaLocationDataSource
        locationDisplay.dataSource.start()
    }
}

@Composable
fun HandleSensorUpdates(locationDisplay: LocationDisplay) {
    // 获取当前上下文
    val context = LocalContext.current
    // 获取系统的传感器服务
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    // 获取磁力计和加速度传感器
    val magneticFieldSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    // 创建并记住传感器事件监听器
    val sensorEventListener = rememberSensorEventListener(locationDisplay)

    // 注册传感器监听器，并在Composable移除时注销
    DisposableEffect(sensorManager, sensorEventListener) {
        sensorManager?.registerListener(sensorEventListener, magneticFieldSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager?.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager?.unregisterListener(sensorEventListener)
        }
    }
}

@Composable
fun rememberSensorEventListener(locationDisplay: LocationDisplay): SensorEventListener {
    // 创建存储磁力计和加速度传感器数据的数组
    val gravity = FloatArray(3)
    val geomagnetic = FloatArray(3)
    val rotationMatrix = FloatArray(9)
    val inclinationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)

    // 获取位置显示的默认符号作为PictureMarkerSymbol
    val navigationSymbol = locationDisplay.defaultSymbol as? PictureMarkerSymbol

    // 创建并记住传感器事件监听器
    return remember {
        object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                        Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                    }

                    // 请求更新方位角
                    val success = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geomagnetic)
                    if (success) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        locationDisplay.headingSymbol
                        // 将弧度转换为度
                        val azimuthInDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                        // 更新位置显示的符号旋转角度
                        navigationSymbol?.angle = azimuthInDegrees
                    }
                }
            }
        }
    }
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