package com.app.changescout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.changescout.data.local.entity.ProductoImportadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoImportadoDao {
    @Query("SELECT * FROM productos_importados ORDER BY nombre ASC")
    fun observarTodos(): Flow<List<ProductoImportadoEntity>>

    @Query("SELECT * FROM productos_importados WHERE id = :productoId LIMIT 1")
    fun observarPorId(productoId: Long): Flow<ProductoImportadoEntity?>

    @Query("SELECT * FROM productos_importados WHERE id = :productoId LIMIT 1")
    suspend fun obtenerPorId(productoId: Long): ProductoImportadoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(producto: ProductoImportadoEntity): Long
}
