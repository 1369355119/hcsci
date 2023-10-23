package com.example.myapplication.ui.screen.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.config.AppRoute
import com.example.myapplication.model.Task
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun TaskView(appNavController: NavHostController, tasks: List<Task>) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp) // 添加底部间距
        ) {
            itemsIndexed(tasks) { index, task ->
                TaskListItem(appNavController, task)
            }
        }

        Button(
            onClick = { /* TODO */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter) // 底部居中对齐
        ) {
            Text(text = "创建任务")
        }
    }
}

@Composable
private fun TaskListItem(appNavController: NavHostController, task: Task) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp) // 外边距
            .clickable { appNavController.navigate(AppRoute.EXECUTE_SCREEN) },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 10.dp
        ) // 设置阴影
    ) {
        Column(
            modifier = Modifier.padding(15.dp) // 内边距
        ) {
            Text(
                buildAnnotatedString {
                    append("任务 ")
                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.W900, color = Color(0xFF4552B8))
                    ) {
                        append(task.id.toString())
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskViewPreview() {
    val navController = rememberNavController() // 创建NavHostController实例
    var tasks = remember {
        listOf(
            Task(id = 3),
            Task(id = 4)
        )
    }

    MyApplicationTheme {
        TaskView(appNavController = navController, tasks = tasks)
    }
}

