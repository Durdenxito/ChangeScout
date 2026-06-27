package com.app.changescout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.app.changescout.data.local.entity.ProductoImportadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoImportadoDao {
    @Query("SELECT * FROM productos_importados WHERE usuarioId = :usuarioId ORDER BY nombre ASC")
    fun observarTodos(usuarioId: String): Flow<List<ProductoImportadoEntity>>

    @Query("SELECT * FROM productos_importados WHERE usuarioId = :usuarioId AND id = :productoId LIMIT 1")
    fun observarPorId(usuarioId: String, productoId: Long): Flow<ProductoImportadoEntity?>

    @Query("SELECT * FROM productos_importados WHERE usuarioId = :usuarioId AND id = :productoId LIMIT 1")
    suspend fun obtenerPorId(usuarioId: String, productoId: Long): ProductoImportadoEntity?

    @Transaction
    suspend fun upsert(producto: ProductoImportadoEntity): Long {
        return if (producto.id == 0L) {
            insertar(producto)
        } else {
            actualizar(producto)
            producto.id
        }
    }

    @Insert
    suspend fun insertar(producto: ProductoImportadoEntity): Long

    @Update
    suspend fun actualizar(producto: ProductoImportadoEntity)

    @Query("DELETE FROM productos_importados WHERE usuarioId = :usuarioId AND id = :productoId")
    suspend fun eliminar(usuarioId: String, productoId: Long)
}
