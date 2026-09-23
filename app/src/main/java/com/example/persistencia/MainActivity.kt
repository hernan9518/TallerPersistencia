package com.example.persistencia

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.example.persistencia.data.AppDatabase
import com.example.persistencia.data.Tarea
import com.example.persistencia.ui.TareaScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    // Variables de estado temporal (Punto 1)
    private var tituloEstado = mutableStateOf("")
    private var descripcionEstado = mutableStateOf("")
    private var segundosTranscurridos = mutableStateOf(0)
    private var temporizadorActivo = mutableStateOf(false)

    // Lista observable para Room (Punto 2)
    private var listaTareas = mutableStateListOf<Tarea>()

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            if (temporizadorActivo.value) {
                segundosTranscurridos.value++
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recuperación en onCreate (Punto 1)
        savedInstanceState?.let { bundle ->
            tituloEstado.value = bundle.getString("KEY_TITULO", "")
            descripcionEstado.value = bundle.getString("KEY_DESCRIPCION", "")
            segundosTranscurridos.value = bundle.getInt("KEY_SEGUNDOS", 0)
            temporizadorActivo.value = bundle.getBoolean("KEY_ACTIVO", false)
            if (temporizadorActivo.value) {
                handler.post(runnable)
            }
        }

        cargarTareasDesdeRoom()

        setContent {
            TareaScreen(
                titulo = tituloEstado.value,
                onTituloChange = { tituloEstado.value = it },
                descripcion = descripcionEstado.value,
                onDescripcionChange = { descripcionEstado.value = it },
                segundosTranscurridos = segundosTranscurridos.value,
                enEjecucion = temporizadorActivo.value,
                onToggleTemporizador = { toggleTemporizador() },
                onGuardarTarea = { guardarTareaEnRoom() },
                listaTareas = listaTareas,
                onEliminarTarea = { eliminarTareaDeRoom(it) }
            )
        }
    }

    private fun toggleTemporizador() {
        temporizadorActivo.value = !temporizadorActivo.value
        if (temporizadorActivo.value) {
            handler.post(runnable)
        } else {
            handler.removeCallbacks(runnable)
        }
    }

    private fun guardarTareaEnRoom() {
        if (tituloEstado.value.isBlank()) {
            Toast.makeText(this, "Ingresa un título", Toast.LENGTH_SHORT).show()
            return
        }

        val fechaActual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val nuevaTarea = Tarea(
            titulo = tituloEstado.value,
            descripcion = "${descripcionEstado.value} (Tiempo enfocado: ${segundosTranscurridos.value}s)",
            estadoCompletado = false,
            fechaCreacion = fechaActual
        )

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.tareaDao().insertarTarea(nuevaTarea)
            cargarTareasDesdeRoom()
        }

        // Limpiar campos temporales
        tituloEstado.value = ""
        descripcionEstado.value = ""
        segundosTranscurridos.value = 0
        temporizadorActivo.value = false
        handler.removeCallbacks(runnable)
    }

    private fun cargarTareasDesdeRoom() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val tareas = db.tareaDao().obtenerTodasLasTareas()
            CoroutineScope(Dispatchers.Main).launch {
                listaTareas.clear()
                listaTareas.addAll(tareas)
            }
        }
    }

    private fun eliminarTareaDeRoom(tarea: Tarea) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.tareaDao().eliminarTarea(tarea)
            cargarTareasDesdeRoom()
        }
    }

    // --- MANEJO EXPLÍCITO DEL CICLO DE VIDA (Punto 1) ---

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Guardar estado temporal en el Bundle
        outState.putString("KEY_TITULO", tituloEstado.value)
        outState.putString("KEY_DESCRIPCION", descripcionEstado.value)
        outState.putInt("KEY_SEGUNDOS", segundosTranscurridos.value)
        outState.putBoolean("KEY_ACTIVO", temporizadorActivo.value)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Recuperación explícita
        tituloEstado.value = savedInstanceState.getString("KEY_TITULO", "")
        descripcionEstado.value = savedInstanceState.getString("KEY_DESCRIPCION", "")
        segundosTranscurridos.value = savedInstanceState.getInt("KEY_SEGUNDOS", 0)
        temporizadorActivo.value = savedInstanceState.getBoolean("KEY_ACTIVO", false)
    }

    override fun onPause() {
        super.onPause()
        // Borrador rápido / Pausa de temporizador al pasar a segundo plano
        if (temporizadorActivo.value) {
            handler.removeCallbacks(runnable)
        }
    }

    override fun onStop() {
        super.onStop()
        // Liberar recursos adicionales si es necesario
    }
}