package com.example.myapplication
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.Plan
import com.example.myapplication.model.Shp
import com.example.myapplication.model.Task
import diewald_shapeFile.shapeFile.ShapeFile

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

class ShpFileViewModel : ViewModel() {
    private var shapeFile: ShapeFile? = null

    fun setShapeFile(file: ShapeFile) {
        shapeFile = file
    }

    fun getShapeFile(): ShapeFile? {
        return shapeFile
    }
}

object GlobalData {
    var shapeFile: ShapeFile? = null
}
