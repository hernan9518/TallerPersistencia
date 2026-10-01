package com.example.persistencia.data

class TareaSugeridaRepository(private val dao: TareaSugeridaDao) {

    /** Lo que ya está guardado localmente. Se muestra de inmediato, sin esperar red. */
    suspend fun obtenerCache(): List<TareaSugerida> = dao.obtenerTodas()

    /**
     * Descarga el catálogo remoto y lo guarda. No reemplaza lo que ya existe
     * (OnConflictStrategy.IGNORE en el DAO), así no se pierde la marca de
     * "importada" de sugerencias que el usuario ya trajo a su lista real.
     */
    suspend fun actualizarDesdeRed(): List<TareaSugerida> {
        val remotas = RetrofitClient.api.obtenerTareas()
        val sugeridas = remotas.map { it.aTareaSugerida() }
        dao.insertarTodas(sugeridas)
        return dao.obtenerTodas()
    }

    suspend fun marcarImportada(id: Int) = dao.marcarImportada(id)
}