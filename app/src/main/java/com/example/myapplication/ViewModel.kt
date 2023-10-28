package com.example.myapplication
import androidx.lifecycle.ViewModel
import com.arcgismaps.data.ShapefileFeatureTable
import com.example.myapplication.model.Plan
import com.example.myapplication.model.Shp
import com.example.myapplication.model.Task

class ViewModel : ViewModel() {
    var shps = mutableListOf(
        Shp(
            id = 1,
            plans = mutableListOf(
                Plan(
                    id = 1,
                    tasks = mutableListOf(
                        Task(id = 1),
                        Task(id = 2)
                    )
                ),
                Plan(
                    id = 2,
                    tasks = mutableListOf(
                        Task(id = 1),
                        Task(id = 2)
                    )
                )
            )
        ),
        Shp(
            id = 2,
            plans = mutableListOf(
                Plan(
                    id = 1,
                    tasks = mutableListOf(
                        Task(id = 1),
                        Task(id = 2)
                    )
                ),
                Plan(
                    id = 2,
                    tasks = mutableListOf(
                        Task(id = 1),
                        Task(id = 2)
                    )
                )
            )
        )
    )
}

class PermissionsViewModel : ViewModel() {
    var permissionsGranted: Boolean = false
}

object GlobalData {
    var shapeFile: ShapefileFeatureTable? = null
}
