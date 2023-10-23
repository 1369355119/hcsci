package com.example.myapplication.ui.screen.shpList

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ViewModel
import com.example.myapplication.config.AppRoute
import com.example.myapplication.model.Plan
import com.example.myapplication.model.Shp
import com.example.myapplication.model.Task
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun ShpListView(appNavController: NavHostController, shps: List<Shp>) {
    Column(
        modifier = Modifier.fillMaxSize(),
        // 居中
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn {
            itemsIndexed(shps) { index, shp ->
                ShpListItem(appNavController, shp)
            }
        }
    }
}

@Composable
private fun ShpListItem(appNavController: NavHostController, shp: Shp) {
    val viewModel: ViewModel = viewModel()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp) // 外边距
            .clickable { appNavController.navigate(AppRoute.PLAN_SCREEN) },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 10.dp
        ) // 设置阴影
    ) {
        Column(
            modifier = Modifier.padding(15.dp) // 内边距
        ) {
            Text(
                buildAnnotatedString {
                    append("SHP文件 ")
                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.W900, color = Color(0xFF4552B8))
                    ) {
                        append(shp.id.toString())
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShpListViewPreview() {
    val navController = rememberNavController() // 创建NavHostController实例
    val shps = remember {
        listOf(
            Shp(
                id = 1,
                plans = mutableStateListOf(
                    Plan(id = 1, tasks = mutableStateListOf(Task(id = 1), Task(id = 2))),
                    Plan(id = 2, tasks = mutableStateListOf(Task(id = 1), Task(id = 2)))
                )
            ),
            Shp(
                id = 2,
                plans = mutableStateListOf(
                    Plan(id = 1, tasks = mutableStateListOf(Task(id = 1), Task(id = 2))),
                    Plan(id = 2, tasks = mutableStateListOf(Task(id = 1), Task(id = 2)))
                )
            )
        )
    }

    MyApplicationTheme {
        ShpListView(appNavController = navController, shps = shps)
    }
}
