package com.example.myapplication.ui.screen.execute

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.myapplication.GlobalData
import diewald_shapeFile.files.shp.shapeTypes.ShpPoint
import diewald_shapeFile.files.shp.shapeTypes.ShpPolyLine
import diewald_shapeFile.files.shp.shapeTypes.ShpPolygon
import diewald_shapeFile.shapeFile.ShapeFile
import kotlin.math.max
import kotlin.math.min


@Composable
fun ExecuteView(appNavController: NavHostController) {
    val shapeFile = GlobalData.shapeFile

    // 使用 Canvas 绘制 ShapeFile 数据
    AndroidView(factory = { context ->
        ShapeFileCanvasView(context).apply {
            this.shapeFile = shapeFile
        }
    }, modifier = Modifier.fillMaxSize())
}

class ShapeFileCanvasView(context: Context) : View(context) {
    var shapeFile: ShapeFile? = null
        set(value) {
            field = value
            calculateBounds(value) // 计算边界值
            invalidate()  // 当 ShapeFile 数据更新时，请求重绘视图
        }

    // 地图数据的边界值
    private var mapMinX = Double.MAX_VALUE  // 最小 X 坐标
    private var mapMaxX = -Double.MAX_VALUE // 最大 X 坐标
    private var mapMinY = Double.MAX_VALUE  // 最小 Y 坐标
    private var mapMaxY = -Double.MAX_VALUE // 最大 Y 坐标

    // 计算边界值，遍历所有的形状以计算最小和最大的 X、Y 坐标
    private fun calculateBounds(shapeFile: ShapeFile?) {
        shapeFile?.let { sf ->
            for (i in 0 until sf.shP_shapeCount) {
                when (val shape: ShpPolygon = shapeFile.getSHP_shape(i)) {
                    is ShpPoint -> updateBounds(shape.point)
                    is ShpPolyLine, is ShpPolygon -> {
                        shape.points.forEach { point ->
                            updateBounds(point)
                        }
                    }
                }
            }
        }
    }

    private fun updateBounds(point: DoubleArray) {
        if (point.size >= 2) {
            mapMinX = min(mapMinX, point[0])
            mapMaxX = max(mapMaxX, point[0])
            mapMinY = min(mapMinY, point[1])
            mapMaxY = max(mapMaxY, point[1])
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        shapeFile?.let { sf ->
            drawShapeFileOnCanvas(canvas, sf)
        } ?: Log.d("ShapeFileCanvasView", "ShapeFile is null")
    }


    // 绘制 ShapeFile 数据的逻辑
    private fun drawShapeFileOnCanvas(canvas: Canvas, shapeFile: ShapeFile) {
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        val numberOfShapes: Int = shapeFile.shP_shapeCount
        for (i in 0 until numberOfShapes) {
            when (val shape: ShpPolygon = shapeFile.getSHP_shape(i)) {
                is ShpPoint -> {
                    // getPoint() 返回一个包含 x 和 y 坐标的数组
                    val point = shape.point
                    val (x, y) = convertWebMercatorToScreenCoordinates(point[0], point[1], canvas.width.toFloat(), canvas.height.toFloat())
                    canvas.drawCircle(x, y, 5f, paint) // 绘制点
                }
                is ShpPolyLine -> {
                    // getPoints() 返回一个包含所有点的二维数组
                    val path = Path()
                    shape.getPoints().forEachIndexed { index, point ->
                        val (x, y) = convertWebMercatorToScreenCoordinates(point[0], point[1], canvas.width.toFloat(), canvas.height.toFloat())
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    canvas.drawPath(path, paint) // 绘制线
                }
                is ShpPolygon -> {
                    // getPointsAs3DArray() 方法
                    val parts = shape.pointsAs3DArray
                    parts.forEach { part ->
                        val path = Path()
                        part.forEachIndexed { index, point ->
                            val (x, y) = convertWebMercatorToScreenCoordinates(point[0], point[1], canvas.width.toFloat(), canvas.height.toFloat())
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        canvas.drawPath(path, paint) // 绘制多边形
                    }
                }
            }
        }
    }

    // 转换投影坐标系中的值为屏幕的坐标
    private fun convertWebMercatorToScreenCoordinates(
        mercatorX: Double, mercatorY: Double,
        screenWidth: Float, screenHeight: Float
    ): Pair<Float, Float> {
        // 地图的实际宽度和高度（单位：米）
        val mapWidth = mapMaxX - mapMinX
        val mapHeight = mapMaxY - mapMinY

        // 计算 x 和 y 坐标相对于地图宽度和高度的比例
        val xRatio = (mercatorX - mapMinX) / mapWidth
        val yRatio = (mercatorY - mapMinY) / mapHeight

        // 将比例转换为屏幕坐标
        val screenX = (xRatio * screenWidth).toFloat()
        // Y坐标需要反转，因为屏幕坐标系的原点在左上角
        val screenY = ((1 - yRatio) * screenHeight).toFloat()

        return Pair(screenX, screenY)
    }

}

