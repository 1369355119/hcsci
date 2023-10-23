package com.example.myapplication.ui.screen.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.config.AppRoute
import com.example.myapplication.ui.screen.shp.ShpView
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

@Composable
fun StartView(appNavController: NavHostController) {
    LaunchedEffect(Unit){
        delay(1500)
        appNavController.navigate(AppRoute.SHP_SCREEN)
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        // 居中
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "瀚辰光翼出品")
    }
}

@Preview(showBackground = true)
@Composable
fun StartPreview() {
    val navController = rememberNavController() // 创建NavHostController实例
    MyApplicationTheme {
        StartView(navController)
    }
}