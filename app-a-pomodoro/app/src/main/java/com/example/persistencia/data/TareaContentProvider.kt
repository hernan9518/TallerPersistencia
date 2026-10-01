package com.example.persistencia.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase

private const val TABLA = "tabla_tareas"
private const val CODIGO_TAREAS = 1
private const val CODIGO_TAREA_ID = 2
private const val CODIGO_NOTAS_CONFIDENCIALES = 3

class TareaContentProvider : ContentProvider() {

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(TareaContract.AUTHORITY, "tareas", CODIGO_TAREAS)
        addURI(TareaContract.AUTHORITY, "tareas/#", CODIGO_TAREA_ID)
        addURI(TareaContract.AUTHORITY, "notasConfidenciales", CODIGO_NOTAS_CONFIDENCIALES)
    }

    // Reutiliza la MISMA base de datos que ya usa Room en App A (Punto 2)
    private fun db(): SupportSQLiteDatabase =
        AppDatabase.getDatabase(context!!.applicationContext).openHelper.writableDatabase

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/tareas"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        return when (uriMatcher.match(uri)) {
            CODIGO_NOTAS_CONFIDENCIALES -> consultarConteoNotas()
            else -> consultarTareas(selection, selectionArgs, sortOrder)
        }
    }

    private fun consultarTareas(
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        // Nunca expone las tareas con borrado lógico (ver Punto 2, Offline-First)
        var sql = "SELECT * FROM $TABLA WHERE eliminada = 0"
        if (!selection.isNullOrBlank()) sql += " AND ($selection)"
        sql += " ORDER BY " + (sortOrder ?: "id DESC")

        return db().query(SimpleSQLiteQuery(sql, selectionArgs ?: emptyArray()))
    }

    /**
     * No puede devolver el contenido de la nota: la clave de cifrado vive en el
     * Keystore de esta misma app. Solo informa si existe (1) o no (0) una nota guardada.
     */
    private fun consultarConteoNotas(): Cursor {
        val manager = NotaSeguraManager(context!!.applicationContext)
        val cursor = MatrixCursor(arrayOf(TareaContract.COL_CANTIDAD_NOTAS))
        cursor.addRow(arrayOf(manager.cantidadNotas()))
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        values ?: return null
        val id = db().insert(TABLA, SQLiteDatabase.CONFLICT_REPLACE, values)
        context?.contentResolver?.notifyChange(uri, null)
        return Uri.withAppendedPath(TareaContract.CONTENT_URI, id.toString())
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        values ?: return 0
        val filas = db().update(TABLA, SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs)
        context?.contentResolver?.notifyChange(uri, null)
        return filas
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val filas = db().delete(TABLA, selection, selectionArgs)
        context?.contentResolver?.notifyChange(uri, null)
        return filas
    }
}