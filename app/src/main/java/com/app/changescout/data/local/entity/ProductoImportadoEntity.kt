package com.app.changescout.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.changescout.domain.model.ComponentesCostoImportacion
import com.app.changescout.domain.model.ProductoImportado

@Entity(tableName = "productos_importados")
data class ProductoImportadoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val nombre: String,
    val queryCompetencia: String,
    val precioFobUsd: Double,
    val fleteUsd: Double,
    val seguroUsd: Double,
    val arancelesUsd: Double,
    val otrosCargosUsd: Double,
    val cantidadDisponible: Int,
    val notas: String?
) {
    fun toDomain(): ProductoImportado {
        return ProductoImportado(
            id = id,
            nombre = nombre,
            queryCompetencia = queryCompetencia,
            componentesCosto = ComponentesCostoImportacion(
                precioFobUsd = precioFobUsd,
                fleteUsd = fleteUsd,
                seguroUsd = seguroUsd,
                arancelesUsd = arancelesUsd,
                otrosCargosUsd = otrosCargosUsd
            ),
            cantidadDisponible = cantidadDisponible,
            notas = notas
        )
    }

    companion object {
        fun fromDomain(producto: ProductoImportado): ProductoImportadoEntity {
            return ProductoImportadoEntity(
                id = producto.id,
                nombre = producto.nombre,
                queryCompetencia = producto.queryCompetencia,
                precioFobUsd = producto.componentesCosto.precioFobUsd,
                fleteUsd = producto.componentesCosto.fleteUsd,
                seguroUsd = producto.componentesCosto.seguroUsd,
                arancelesUsd = producto.componentesCosto.arancelesUsd,
                otrosCargosUsd = producto.componentesCosto.otrosCargosUsd,
                cantidadDisponible = producto.cantidadDisponible,
                notas = producto.notas
            )
        }
    }
}
