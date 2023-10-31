package com.example.myapplication

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.config.AppRoute
import com.example.myapplication.ui.screen.execute.ExecuteView
import com.example.myapplication.ui.screen.photograph.PhotoGraphView
import com.example.myapplication.ui.screen.plan.PlanView
import com.example.myapplication.ui.screen.planList.PlanListView
import com.example.myapplication.ui.screen.shp.ShpView
import com.example.myapplication.ui.screen.shpList.ShpListView
import com.example.myapplication.ui.screen.start.StartView
import com.example.myapplication.ui.screen.task.TaskView
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // 获取对 FragmentActivity 的引用
                val fragmentActivity = remember { this }
                // viewModel
                val viewModel: ViewModel = viewModel()
                // 整体app导航控制器
                val appNavController = rememberNavController()
                NavHost(navController = appNavController, startDestination = AppRoute.START_SCREEN){
                    composable(AppRoute.START_SCREEN){
                        StartView(appNavController, fragmentActivity)
                    }
                    composable(AppRoute.SHP_SCREEN){
                        ShpView(appNavController)
                    }
                    composable(AppRoute.SHP_LIST_SCREEN){
                        ShpListView(appNavController, viewModel.shps)
                    }
                    composable(AppRoute.PLAN_SCREEN){
                        PlanView(appNavController)
                    }
                    composable(AppRoute.PLAN_LIST_SCREEN){
                        PlanListView(appNavController, viewModel.shps[0].plans)
                    }
                    composable(AppRoute.TASK_SCREEN){
                        TaskView(appNavController, viewModel.shps[0].plans[0].tasks)
                    }
                    composable(AppRoute.EXECUTE_SCREEN){
                        ExecuteView(appNavController)
                    }
                    composable(AppRoute.PHOTO_SCREEN){
                        PhotoGraphView(appNavController)
                    }
                }
            }
        }
    }
}