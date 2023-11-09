package com.example.myapplication.ui.screen.shp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.data.ShapefileFeatureTable
import com.arcgismaps.mapping.layers.FeatureLayer
import com.example.myapplication.GlobalData
import com.example.myapplication.config.AppRoute
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


// 选择shp文件
@Composable
fun ChooseShpButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "选择shp文件")
    }
}

@Composable
fun ImportShpButton(
    onLoadingChange: (Boolean) -> Unit,
    onDirectoryChosen: (Uri?) -> Unit,
    onFilesProcessed: () -> Unit
) {
    val context = LocalContext.current // 获取当前 Composable 的 Context
    val scope = rememberCoroutineScope()

    val directoryChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onLoadingChange(true) // 启用加载状态
            scope.launch {
                result.data?.data?.let { uri ->
                    // 假设readFilesFromFolder是一个挂起函数，用于处理文件夹中的文件
                    readFilesFromFolder(uri, context)
                    onDirectoryChosen(uri)
                }
                onLoadingChange(false) // 禁用加载状态
                onFilesProcessed()
            }
        } else {
            onDirectoryChosen(null)
        }
    }

    // 创建一个用于选择目录的Intent
    val selectDirectoryIntent = remember {
        {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            directoryChooserLauncher.launch(intent)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(onClick = selectDirectoryIntent) {
            Text(text = "导入shp文件")
        }
    }
}

suspend fun readFilesFromFolder(folderUri: Uri, context: Context) {
    withContext(Dispatchers.IO) {  // 确保在IO线程执行文件操作
        // 创建临时文件夹
        val tempDir = File(context.cacheDir, "shpFilesTemp")
        if (!tempDir.exists()) {
            tempDir.mkdir()
        }

        // 获取原始文件夹中的所有文件
        val folder = DocumentFile.fromTreeUri(context, folderUri)
        folder?.listFiles()?.forEach { file ->
            file.uri.copyUriContentToTempFile(context, File(tempDir, file.name ?: "tempFile"))
        }

        // 现在tempDir包含了所有复制的文件，可以用它来初始化ShapeFile
        // 搜索 .shp 文件
        val shpFile = tempDir.listFiles().firstOrNull { it.extension == "shp" }
        if (shpFile != null) {
            try {
                // 使用 ArcGIS SDK 加载 Shapefile
                val shapefileTable = ShapefileFeatureTable(shpFile.canonicalPath)
                shapefileTable.load().onSuccess {
                    GlobalData.shapeFile = shapefileTable
                    // 根据 ArcGIS API 的使用方式和对象的所有权模型，一旦一个 FeatureTable 被用于创建一个 FeatureLayer，它可能就不能被用于创建另一个 FeatureLayer 实例。
                    GlobalData.featureLayer = FeatureLayer.createWithFeatureTable(shapefileTable)
                }.onFailure {
                    Log.e("Shapefile", "Error loading shapefile: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e("Shapefile", "Error loading shapefile: ${e.message}")
            }
        } else {
            Log.e("Shapefile", "No .shp file found in the folder")
        }

        // 删除临时文件夹及其内容
        deleteRecursively(tempDir)
    }
}

fun Uri.copyUriContentToTempFile(context: Context, tempFile: File) {
    context.contentResolver.openInputStream(this)?.use { inputStream ->
        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}

fun deleteRecursively(file: File) {
    if (file.isDirectory) {
        file.listFiles()?.forEach { child ->
            deleteRecursively(child)
        }
    }
    file.delete()
}

@Composable
fun ShpView(appNavController: NavHostController) {
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ChooseShpButton {
                // 处理按钮点击事件
                appNavController.navigate(AppRoute.EXECUTE_SCREEN)
            }
            ImportShpButton(
                onLoadingChange = { isLoading = it },
                onDirectoryChosen = { uri ->
                    // 当选择文件夹时
                    if (uri != null) {
                        // 开始加载
                        isLoading = true
                    }
                },
                onFilesProcessed = {
                    // 文件处理完成
                    isLoading = false
                    appNavController.navigate(AppRoute.SHP_LIST_SCREEN)
                }
            )
        }

        // 加载遮罩层和指示器
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .clickable(enabled = false, onClick = {})
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ShpPreview() {
    val navController = rememberNavController() // 创建NavHostController实例
    MyApplicationTheme {
        ShpView(navController)
    }
}