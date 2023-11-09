package com.example.myapplication.ui.screen.start

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.PermissionsViewModel
import com.example.myapplication.R
import com.example.myapplication.config.AppRoute
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.delay

// 启动页面
@Composable
fun StartView(appNavController: NavHostController, fragmentActivity: FragmentActivity) {
    val permissionsViewModel = viewModel<PermissionsViewModel>()

    // 进入启动页面
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 在页面上显示"瀚辰光翼出品"
        Text(text = stringResource(R.string.logo))

        // 显示1秒后弹出权限申请
        LaunchedEffect(Unit) {
            delay(1000)

            // 请求权限
            PermissionX.init(fragmentActivity)
                .permissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .onExplainRequestReason { scope, deniedList ->
                    val message = "需要您同意以下权限才能正常使用"
                    scope.showRequestReasonDialog(deniedList, message, "Allow", "Deny")
                }
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        permissionsViewModel.permissionsGranted = allGranted
                        Toast.makeText(fragmentActivity, "所有申请的权限都已通过", Toast.LENGTH_SHORT).show()
                        appNavController.navigate(AppRoute.SHP_SCREEN)
                    } else {
                        Toast.makeText(fragmentActivity, "您拒绝了如下权限：$deniedList", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}