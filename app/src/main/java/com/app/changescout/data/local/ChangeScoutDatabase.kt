package com.app.changescout.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.changescout.data.local.dao.ProductoImportadoDao
import com.app.changescout.data.local.dao.SnapshotEvaluacionComercialDao
import com.app.changescout.data.local.entity.ProductoImportadoEntity
import com.app.changescout.data.local.entity.SnapshotEvaluacionComercialEntity

@Database(
    entities = [
        ProductoImportadoEntity::class,
        SnapshotEvaluacionComercialEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChangeScoutDatabase : RoomDatabase() {
    abstract fun productoImportadoDao(): ProductoImportadoDao
    abstract fun snapshotEvaluacionComercialDao(): SnapshotEvaluacionComercialDao
}
