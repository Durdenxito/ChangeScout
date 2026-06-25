package com.app.changescout.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MigracionesBaseDatosChangeScout {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS evaluaciones_comerciales (
                    evaluacionId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    productoId INTEGER NOT NULL,
                    costoTotalUsd REAL,
                    costoTotalPen REAL,
                    tipoCambioVentaUsdPen REAL,
                    precioPromedioRealPen REAL,
                    competidoresValidos INTEGER NOT NULL,
                    margenNetoPct REAL,
                    erosionPrecioLocalPct REAL,
                    variacionCompetidoresPct REAL,
                    presionCambiariaPct REAL,
                    ventanaHistoricaDias INTEGER,
                    veredicto TEXT,
                    estadoEvaluacion TEXT NOT NULL,
                    evaluadoEnEpochMillis INTEGER NOT NULL,
                    versionAlgoritmo TEXT NOT NULL,
                    trazaProveedor TEXT,
                    FOREIGN KEY(productoId) REFERENCES productos_importados(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO evaluaciones_comerciales (
                    evaluacionId,
                    productoId,
                    costoTotalUsd,
                    costoTotalPen,
                    tipoCambioVentaUsdPen,
                    precioPromedioRealPen,
                    competidoresValidos,
                    margenNetoPct,
                    erosionPrecioLocalPct,
                    variacionCompetidoresPct,
                    presionCambiariaPct,
                    ventanaHistoricaDias,
                    veredicto,
                    estadoEvaluacion,
                    evaluadoEnEpochMillis,
                    versionAlgoritmo,
                    trazaProveedor
                )
                SELECT
                    snapshotId,
                    productoId,
                    costoTotalUsd,
                    costoTotalPen,
                    tipoCambioVentaUsdPen,
                    precioPromedioRealPen,
                    competidoresValidos,
                    margenNetoPct,
                    erosionPrecioLocalPct,
                    variacionCompetidoresPct,
                    presionCambiariaPct,
                    ventanaHistoricaDias,
                    veredicto,
                    estadoSnapshot,
                    evaluadoEnEpochMillis,
                    versionAlgoritmo,
                    trazaProveedor
                FROM snapshots_evaluacion_comercial
                """.trimIndent()
            )
            db.execSQL("DROP TABLE snapshots_evaluacion_comercial")
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_evaluaciones_comerciales_productoId
                ON evaluaciones_comerciales(productoId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_evaluaciones_comerciales_productoId_evaluadoEnEpochMillis
                ON evaluaciones_comerciales(productoId, evaluadoEnEpochMillis)
                """.trimIndent()
            )
        }
    }
}
