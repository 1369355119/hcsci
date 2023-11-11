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
import com.arcgismaps.Color
import com.arcgismaps.LoadStatus
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.location.NmeaLocationDataSource
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

@Composable
fun ExecuteView(appNavController: NavHostController) {
    // 添加debug变量，用于控制数据来源. true的时候从文件中拿NMEA数据，false时从串口服务中拿
    val debug = remember { mutableStateOf(true) }
    // 使用 remember mutable state 来记录地图是否加载完毕
    var isMapLoaded by remember { mutableStateOf(false) }
    // 记忆并处理MapView的生命周期
    val mapView = rememberMapViewWithLifecycle()
    // 初始化地图并设置API密钥
    val map = rememberMapWithApiKey()
    // 从全局数据中获取FeatureLayer
    val featureLayer = GlobalData.featureLayer
    // 创建用于绘制线段的GraphicsOverlay
    val graphicsOverlay = remember { GraphicsOverlay() }
    // 显示地图和FeatureLayer
    DisplayMap(mapView, map, featureLayer, graphicsOverlay)

    // 判断地图是否加载完毕
    LaunchedEffect(mapView.map) {
        launch {
            mapView.map?.loadStatus?.collect { loadStatus ->
                if (loadStatus == LoadStatus.Loaded) {
                    isMapLoaded = true
                }
            }
        }
    }

    // 获取地图的位置显示
    val locationDisplay = mapView.locationDisplay
    // 创建NMEA位置数据源
    val nmeaLocationDataSource = remember { NmeaLocationDataSource() }
    // 设置位置显示的符号、显示的数据源和模式:使用NMEA
    SetupLocationDisplay(locationDisplay, nmeaLocationDataSource)
    // 处理设备传感器更新，让定位符号变成指南针，绘制线段
    HandleSensorUpdates(locationDisplay, graphicsOverlay, mapView) { isMapLoaded }

    // 获取应用的Context
    val context = LocalContext.current
    // 创建SerialPortClient实例
    val serialPortClient = remember { SerialPortClient(context) }
    // 启动串口服务客户端并接收NMEA数据
    LaunchedEffect(Unit) {
        if (debug.value) {
            // 从文件中读取NMEA数据(文件中是模拟数据，nmea_data.txt是不动的一个点,nmea_data_move.txt是移动的数据)
            readFileAndPushData(context, nmeaLocationDataSource) { isMapLoaded }
        } else {
            // 从串口服务中获取NMEA数据
            serialPortClient.start()
            // 在协程中收集NMEA数据
            launch {
                serialPortClient.nmeaDataFlow.collect { nmeaData ->
                    // 处理接收到的NMEA数据
                    if (isMapLoaded) {
                        nmeaLocationDataSource.pushData(nmeaData.toByteArray())
                    }
                }
            }
        }
    }

    // 当Compose退出时停止串口服务客户端
    DisposableEffect(Unit) {
        onDispose {
            if (!debug.value) {
                serialPortClient.stop()
            }
            isMapLoaded = false
        }
    }
}

// 从文件中读取NMEA数据
suspend fun readFileAndPushData(context: Context, nmeaLocationDataSource: NmeaLocationDataSource, isMapLoaded: () -> Boolean) {
    withContext(Dispatchers.IO) {
        try {
            while (true) { // 无限循环，确保文件数据被不断重复读取
                val groupedData = mutableMapOf<String, MutableList<String>>()

                // 读取文件并根据时间戳分组
                val file =
                    File("/sdcard/Android/data/com.example.myapplication/files/Documents/nmea_data_move.txt")
                file.forEachLine { line ->
                    val timestamp = line.substringBefore(":").trim()
                    val data = line.substringAfter(":").trim()

                    if (data.isNotEmpty()) {
                        groupedData.getOrPut(timestamp) { mutableListOf() }.add(data)
                    }
                }

                // 按时间戳顺序推送数据
                for ((_, dataLines) in groupedData) {
                    dataLines.forEach { nmeaData ->
                        if (isMapLoaded()) {
                            val completeData = "$nmeaData\r\n"
                            nmeaLocationDataSource.pushData(completeData.toByteArray())
                        }
                    }
                    delay(1000) // 每组数据后等待1秒
                }
            }
        } catch (e: Exception) {
            Log.e("ReadFile", "Error reading NMEA data file", e)
        }
    }
}

@Composable
fun rememberMapWithApiKey(): ArcGISMap {
    // 使用remember保证只初始化一次
    return remember {
        ArcGISMap(BasemapStyle.ArcGISNavigation).also {
            // 设置ArcGIS API密钥
            ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        }
    }
}

@Composable
fun SetupLocationDisplay(
    locationDisplay: LocationDisplay,
    nmeaLocationDataSource: NmeaLocationDataSource
) {
    // 获取当前上下文
    val context = LocalContext.current
    // 获取导航图标的drawable资源
    val navigationDrawable = ContextCompat.getDrawable(
        context,
        R.drawable.locationdisplayon
    ) as? NinePatchDrawable
    // 将drawable转换为PictureMarkerSymbol
    val navigationSymbol =
        navigationDrawable?.toBitmap()?.let { BitmapDrawable(context.resources, it) }
            ?.let { PictureMarkerSymbol.createWithImage(it) }

    // 应用导航图标到位置显示
    navigationSymbol?.let {
        locationDisplay.defaultSymbol = it
        locationDisplay.courseSymbol = it //courseSymbol 通常用于表示设备的移动方向
//        locationDisplay.headingSymbol = it //headingSymbol 用于表示设备面对的方向
    }

    // 启动位置显示的数据源
    LaunchedEffect(locationDisplay) {
        // 设置自动居中模式
        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
        // 设置NMEA位置数据源
        locationDisplay.dataSource = nmeaLocationDataSource
        // 启动
        locationDisplay.dataSource.start()
    }
}

@Composable
fun HandleSensorUpdates(
    locationDisplay: LocationDisplay,
    graphicsOverlay: GraphicsOverlay,
    mapView: MapView,
    isMapLoaded: () -> Boolean
) {
    // 使用 remember mutable state 来记录位置数据是否有效
    var isLocated by remember { mutableStateOf(false) }
    // 使用 remember mutable state 来记录当前位置
    var currentLocation by remember { mutableStateOf<Point?>(null) }
    // 获取当前上下文
    val context = LocalContext.current
    // 获取系统的传感器服务
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    // 获取磁力计和加速度传感器
    val magneticFieldSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    // 创建并记住传感器事件监听器
    val sensorEventListener =
        rememberSensorEventListener(locationDisplay, graphicsOverlay, mapView,
            { isLocated }) { currentLocation }

    // 使用LaunchedEffect启动协程来监听位置变化
    LaunchedEffect(key1 = locationDisplay) {
        locationDisplay.location.collect { location ->
            // 检查位置数据是否有效
            if (location != null && isMapLoaded()) {
                isLocated = true
                currentLocation = location.position
            }
        }
    }

    // 注册传感器监听器，并在Composable移除时注销
    DisposableEffect(sensorManager, sensorEventListener) {
        sensorManager?.registerListener(
            sensorEventListener,
            magneticFieldSensor,
            SensorManager.SENSOR_DELAY_UI
        )
        sensorManager?.registerListener(
            sensorEventListener,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_UI
        )
        onDispose {
            sensorManager?.unregisterListener(sensorEventListener)
            isLocated = false
        }
    }
}

@Composable
fun rememberSensorEventListener(
    locationDisplay: LocationDisplay,
    graphicsOverlay: GraphicsOverlay,
    mapView: MapView,
    isLocated: () -> Boolean,
    currentLocation: () -> Point?
): SensorEventListener {
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
                        Sensor.TYPE_ACCELEROMETER -> System.arraycopy(
                            event.values,
                            0,
                            gravity,
                            0,
                            event.values.size
                        )

                        Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(
                            event.values,
                            0,
                            geomagnetic,
                            0,
                            event.values.size
                        )
                    }

                    // 请求更新方位角
                    val success = SensorManager.getRotationMatrix(
                        rotationMatrix,
                        inclinationMatrix,
                        gravity,
                        geomagnetic
                    )
                    if (success) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        locationDisplay.headingSymbol
                        // 将弧度转换为度
                        val azimuthInDegrees =
                            Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                        if (isLocated()) {
                            // 更新位置显示的符号旋转角度
                            navigationSymbol?.angle = azimuthInDegrees
                            // 绘制线段
                            currentLocation()?.let { location ->
                                drawLineFromLocation(
                                    mapView,
                                    graphicsOverlay,
                                    location,
                                    azimuthInDegrees
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun drawLineFromLocation(
    mapView: MapView,
    graphicsOverlay: GraphicsOverlay,
    currentLocation: Point,
    azimuthInDegrees: Float
) {
    // 计算屏幕边缘的点(这里得到的是webMercator坐标系的点)
    val screenEdgePoint = calculateScreenEdgePoint(mapView, currentLocation, azimuthInDegrees)
        ?: return // 如果 screenEdgePoint 为空，则直接返回

    // 把当前坐标转为webMercator坐标系
    val projectedPoint = GeometryEngine.projectOrNull(currentLocation, SpatialReference.webMercator())

    // 创建线段
    val polylineSymbol =
        SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.fromRgba(255, 0, 255, 255), 4.0f)
    val polylineBuilder = PolylineBuilder(SpatialReference.webMercator()) {
        if (projectedPoint != null) {
            addPoint(projectedPoint)
        }
        addPoint(screenEdgePoint)
    }
    val polyline = polylineBuilder.toGeometry()

    // 创建线段的Graphic
    val polylineGraphic = Graphic(polyline, polylineSymbol)

    // 清除之前的Graphic并添加新的Graphic到Overlay
    graphicsOverlay.graphics.clear()
    graphicsOverlay.graphics.add(polylineGraphic)
}

// 使用向量和射线投射的概念来找到正确的交点
// 1、计算射线方向向量：基于方位角，计算一个单位方向向量。这个向量表示从屏幕上的定位点出发的射线方向。
// 2、计算屏幕边界的四个角点：计算屏幕左上角、右上角、左下角和右下角的坐标。
// 3、检查射线与每个边界的交点：对于屏幕的每条边（上、下、左、右），使用方向向量和边界线段来计算交点。这里我们可以使用向量的点积和叉积来判断射线是否与边界相交，并计算交点。
// 4、选择正确的交点：从计算出的交点中选择一个在屏幕范围内且最接近定位点的交点作为最终结果。
fun calculateScreenEdgePoint(
    mapView: MapView,
    locationPoint: Point,
    azimuthInDegrees: Float
): Point? {
    // 获取屏幕尺寸
    val screenWidth = mapView.width
    val screenHeight = mapView.height

    // 将方位角转换为弧度
    val azimuthInRadians = Math.toRadians(azimuthInDegrees.toDouble())

    // 计算定位点在屏幕上的位置
    val screenLocation = mapView.locationToScreen(locationPoint)

    // 计算射线方向向量
    val directionVector = ScreenCoordinate(
        cos(azimuthInRadians),
        sin(azimuthInRadians)
    )

    // 屏幕边界的四个角点
    val corners = listOf(
        ScreenCoordinate(0.0, 0.0),
        ScreenCoordinate(screenWidth.toDouble(), 0.0),
        ScreenCoordinate(screenWidth.toDouble(), screenHeight.toDouble()),
        ScreenCoordinate(0.0, screenHeight.toDouble())
    )

    // 检查射线与每个边界的交点
    var closestIntersection: ScreenCoordinate? = null
    var minDistance = Double.MAX_VALUE
    for (i in corners.indices) {
        val corner1 = corners[i]
        val corner2 = corners[(i + 1) % corners.size]

        val intersection = findIntersection(screenLocation, directionVector, corner1, corner2)
        intersection?.let {
            val distance = distanceBetween(screenLocation, it)
            if (distance < minDistance) {
                minDistance = distance
                closestIntersection = it
            }
        }
    }

    // 将屏幕坐标转换回地图坐标
    return closestIntersection?.let { mapView.screenToLocation(it) }
}

// 计算两点之间的距离
fun distanceBetween(p1: ScreenCoordinate, p2: ScreenCoordinate): Double {
    return sqrt((p2.x - p1.x).pow(2.0) + (p2.y - p1.y).pow(2.0))
}

// 寻找射线与线段的交点
fun findIntersection(
    rayOrigin: ScreenCoordinate,
    rayDirection: ScreenCoordinate,
    segmentStart: ScreenCoordinate,
    segmentEnd: ScreenCoordinate
): ScreenCoordinate? {
    val v1 = ScreenCoordinate(rayOrigin.x - segmentStart.x, rayOrigin.y - segmentStart.y)
    val v2 = ScreenCoordinate(segmentEnd.x - segmentStart.x, segmentEnd.y - segmentStart.y)
    val v3 = ScreenCoordinate(-rayDirection.y, rayDirection.x)

    val dot = v2.x * v3.x + v2.y * v3.y
    if (abs(dot) < 0.000001)
        return null // 平行或共线

    val t1 = (v2.x * v1.y - v2.y * v1.x) / dot
    val t2 = (v1.x * v3.x + v1.y * v3.y) / dot

    if (t1 >= 0.0 && t2 >= 0.0 && t2 <= 1.0)
        return ScreenCoordinate(rayOrigin.x + t1 * rayDirection.x, rayOrigin.y + t1 * rayDirection.y)

    return null
}


@Composable
fun DisplayMap(
    mapView: MapView,
    map: ArcGISMap,
    featureLayer: FeatureLayer?,
    graphicsOverlay: GraphicsOverlay?
) {
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
        // 同样的逻辑应用于GraphicsOverlay
        graphicsOverlay?.let { overlay ->
            if (!mapView.graphicsOverlays.contains(overlay)) {
                mapView.graphicsOverlays.add(overlay)
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
            // 移除GraphicsOverlay
            graphicsOverlay?.let { graphicsOverlay ->
                mapView.graphicsOverlays.remove(graphicsOverlay)
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