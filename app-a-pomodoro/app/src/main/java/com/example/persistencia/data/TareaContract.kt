package com.example.persistencia.data

import android.net.Uri

/**
 * Contrato público del ContentProvider de App A.
 * App B (o cualquier otra app) importa solo estos valores, nunca las clases de Room.
 */
object TareaContract {
    const val AUTHORITY = "com.example.persistencia.provider"
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/tareas")

    // Permiso personalizado que App B debe declarar para poder leer
    const val PERMISO_LECTURA = "com.example.persistencia.permission.READ_TAREAS"

    // Nombres de columnas (deben coincidir con la tabla de Room)
    const val COL_ID = "id"
    const val COL_TITULO = "titulo"
    const val COL_DESCRIPCION = "descripcion"
    const val COL_ESTADO_COMPLETADO = "estadoCompletado"
    const val COL_FECHA_CREACION = "fechaCreacion"
    const val COL_TIEMPO_ACUMULADO = "tiempoAcumuladoSegundos"
}