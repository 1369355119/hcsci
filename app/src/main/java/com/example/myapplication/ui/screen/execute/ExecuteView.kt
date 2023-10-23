package com.example.myapplication.ui.screen.execute

import android.graphics.Color
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps2d.model.Polygon
import com.amap.api.maps2d.model.PolygonOptions
import org.geotools.api.data.DataStoreFinder
import org.geotools.api.data.FeatureSource
import org.geotools.api.feature.simple.SimpleFeature
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.feature.FeatureCollection
import org.geotools.geometry.jts.JTSFactoryFinder
import java.io.File

data class FolderData(
    val shapefileCollection: FeatureCollection<*, *>,
    val dbfRecords: FeatureCollection<*, *>,
)

// 读取shp文件夹
// 这个文件夹里面有 .shp    .dbf   .shx   .cpg   .prj
fun readFolder(folderPath: String): FolderData {
    val folder = File(folderPath)
    val files = folder.listFiles()
    var shapefileCollection: FeatureCollection<*, *>? = null
    var dbfRecords: FeatureCollection<*, *>? = null

    // 遍历文件夹，根据文件后缀使用不同方法读取文件内容
    for (file in files) {
        when (file.extension.lowercase()) {
            "shp" -> shapefileCollection = readShpOrDbfFile(file.absolutePath)
            "dbf" -> dbfRecords = readShpOrDbfFile(file.absolutePath)
        }
    }

    return FolderData(
        shapefileCollection = shapefileCollection ?: throw IllegalStateException("Missing shapefile"),
        dbfRecords = dbfRecords ?: throw IllegalStateException("Missing shapefile")
    )
}

// 读取shp或者dbf文件（这两个文件可以使用相同的方法来读取）
// 因为 .shp 和 .dbf 文件是 Shapefile 数据的核心文件，而 .shx、.cpg 和 .prj 文件是 Shapefile 的附属文件。
fun readShpOrDbfFile(shapefilePath: String): FeatureCollection<*, *> {
    // 使用 Geotools 提供的 DataStoreFinder 类来自动检测和获取适当的数据存储对象，以便读取指定的 Shapefile 文件。
    val file = File(shapefilePath)
    val map = HashMap<String, Any>()
    map["url"] = file.toURI().toURL()
    // DataStore 对象可以用于进一步操作和分析 Shapefile 数据。
    val dataStore = DataStoreFinder.getDataStore(map)

    val typeName = dataStore.typeNames.first()
    val featureSource: FeatureSource<*, *> = dataStore.getFeatureSource(typeName)

    // 返回一个包含Shapefile中所有要素的FeatureCollection对象。
    return featureSource.features
}


// 在这里使用GeoTools库处理shapefileCollection和dbfRecords数据
// 提取需要的地理数据，例如坐标信息
// 将提取的数据转换为Polygon对象，并添加到shapefileData列表中
fun loadShapefileData(
    shapefileCollection: FeatureCollection<*, *>,
    dbfRecords: FeatureCollection<*, *>
): List<Polygon> {
    val shapefileData = mutableListOf<Polygon>()

    // 将shapefileCollection和dbfRecords转换为SimpleFeatureCollection
    val shapefileFeatures = shapefileCollection as SimpleFeatureCollection
    val dbfFeatures = dbfRecords as SimpleFeatureCollection

    // 创建GeometryFactory实例
    val geometryFactory = JTSFactoryFinder.getGeometryFactory(null)

    // 遍历shapefileFeatures获取需要的地理数据
    val shapefileIterator = shapefileFeatures.features()
    while (shapefileIterator.hasNext()) {
        val shapefileFeature = shapefileIterator.next() as SimpleFeature
        val geometry = shapefileFeature.defaultGeometry as Polygon
        shapefileData.add(geometry)
    }
    shapefileIterator.close()

    return shapefileData
}


// 绘制地图的函数
// 使用shapefileData和高德API绘制地图
@Composable
fun drawMap(shapefileData: List<Polygon>) {
    AndroidView(
        factory = { context ->
            val mapView = MapView(context)
            mapView.onCreate(null)
            mapView.onResume()
            mapView
        },
        update = { mapView ->
            mapView.map.apply {
                // 根据需要进行地图初始化配置
                // 设置地图中心点、缩放级别等

                // 绘制 shapefileData 中的多边形
                val map: AMap = mapView.map
                shapefileData.forEach { polygon ->
                    val latLngs = mutableListOf<LatLng>()
                    polygon.points.forEach { point ->
                        latLngs.add(LatLng(point.latitude, point.longitude))
                    }
                    val options = PolygonOptions()
                        .addAll(latLngs)
                        .strokeColor(Color.RED) // 设置边框颜色
                        .fillColor(Color.BLUE) // 设置填充颜色
                    map.addPolygon(options)
                }
            }
        }
    )
}

@Composable
fun ExecuteView(appNavController: NavHostController) {
    // TODO 这里获取不到文件数据的问题在于，没有弹窗获取用户权限，待修改.
    // 读取shp文件夹，获取所有数据
    val folderPath = Environment.getExternalStorageDirectory().absolutePath + "/Download/SHP"
    val folderData  = readFolder(folderPath)
    val shapefileCollection = folderData.shapefileCollection
    val dbfRecords = folderData.dbfRecords

    // 使用从shp文件得到的数据来构造用于绘制地图的List<Polygon>
    val shapefileData = loadShapefileData(shapefileCollection, dbfRecords)

    // 使用shapefileData和高德API绘制地图
    drawMap(shapefileData)
}

