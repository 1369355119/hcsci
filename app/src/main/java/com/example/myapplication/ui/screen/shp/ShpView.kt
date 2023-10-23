package com.example.myapplication.ui.screen.shp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.config.AppRoute
import com.example.myapplication.ui.theme.MyApplicationTheme

// 选择shp文件
@Composable
fun ChooseShpButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "选择shp文件")
    }
}

// 导入shp文件
@Composable
fun ImportShpButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "导入shp文件")
    }
}

@Composable
fun ShpView(appNavController: NavHostController) {
    // 垂直布局
    Column(
        modifier = Modifier.fillMaxSize(),
        // 居中
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChooseShpButton {
            // 处理按钮点击事件
            // 在这里添加您要执行的操作
            appNavController.navigate(AppRoute.SHP_LIST_SCREEN)
        }
        ImportShpButton{

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