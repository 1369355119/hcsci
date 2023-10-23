package com.example.myapplication.ui.screen.plan

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

// 自由规划
@Composable
fun FreeButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "自由规划")
    }
}

// 导入规划
@Composable
fun ImportPlanButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "导入规划")
    }
}

// 打开规划
@Composable
fun OpenButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "打开规划")
    }
}

@Composable
fun PlanView(appNavController: NavHostController) {
    // 垂直布局
    Column(
        modifier = Modifier.fillMaxSize(),
        // 居中
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FreeButton {

        }
        ImportPlanButton{

        }
        OpenButton {
            appNavController.navigate(AppRoute.PLAN_LIST_SCREEN)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlanPreview() {
    val navController = rememberNavController() // 创建NavHostController实例
    MyApplicationTheme {
        PlanView(navController)
    }
}