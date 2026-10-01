package com.example.persistencia.data

class ProductoRepository(private val dao: ProductoDao) {

    /** Lo que ya está guardado localmente. Se muestra de inmediato, sin esperar red. */
    suspend fun obtenerCache(): List<Producto> = dao.obtenerTodos()

    /**
     * Descarga el catálogo remoto y reemplaza la copia local.
     * Si falla (sin red, timeout, error del servidor), lanza la excepción
     * y el caché local queda intacto: la app sigue mostrando lo que ya tenía.
     */
    suspend fun actualizarDesdeRed(): List<Producto> {
        val remotos = RetrofitClient.api.obtenerProductos()
        val productos = remotos.map { it.aProducto() }
        dao.insertarTodos(productos)
        return productos
    }
}