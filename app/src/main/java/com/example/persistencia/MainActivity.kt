package com.example.persistencia

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.persistencia.data.AppDatabase
import com.example.persistencia.data.Tarea
import com.example.persistencia.ui.HomeScreen
import com.example.persistencia.ui.TareaScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    // Variables de estado temporal para la sesión activa (Punto 1)
    private var tareaSeleccionadaId = mutableStateOf<Int?>(null)
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

        // Recuperación explícita del ciclo de vida (Punto 1)
        savedInstanceState?.let { bundle ->
            if (bundle.containsKey("KEY_TAREA_ID")) {
                tareaSeleccionadaId.value = bundle.getInt("KEY_TAREA_ID")
            }
            descripcionEstado.value = bundle.getString("KEY_DESCRIPCION", "")
            segundosTranscurridos.value = bundle.getInt("KEY_SEGUNDOS", 0)
            temporizadorActivo.value = bundle.getBoolean("KEY_ACTIVO", false)

            if (temporizadorActivo.value) {
                handler.post(runnable)
            }
        }

        cargarTareasDesdeRoom()

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "home") {
                // Pantalla 1: Lista Principal
                composable("home") {
                    HomeScreen(
                        listaTareas = listaTareas,
                        onAgregarTarea = { titulo, desc -> crearTareaEnRoom(titulo, desc) },
                        onEliminarTarea = { tarea -> eliminarTareaDeRoom(tarea) },
                        onToggleCompletada = { tarea -> toggleCompletadaEnRoom(tarea) },
                        onIniciarSesion = { tarea ->
                            tareaSeleccionadaId.value = tarea.id
                            descripcionEstado.value = tarea.descripcion
                            segundosTranscurridos.value = 0
                            navController.navigate("sesion")
                        }
                    )
                }

                // Pantalla 2: Sesión de Enfoque
                composable("sesion") {
                    val tareaActual = listaTareas.find { it.id == tareaSeleccionadaId.value }

                    TareaScreen(
                        tarea = tareaActual,
                        descripcion = descripcionEstado.value,
                        onDescripcionChange = { descripcionEstado.value = it },
                        segundosTranscurridos = segundosTranscurridos.value,
                        enEjecucion = temporizadorActivo.value,
                        onToggleTemporizador = { toggleTemporizador() },
                        onGuardarSesion = {
                            tareaActual?.let { tarea ->
                                guardarSesionEnRoom(tarea)
                                navController.popBackStack()
                            }
                        },
                        onVolver = {
                            detenerTemporizador()
                            navController.popBackStack()
                        }
                    )
                }
            }
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

    private fun detenerTemporizador() {
        temporizadorActivo.value = false
        handler.removeCallbacks(runnable)
    }

    // --- OPERACIONES DE ROOM (Punto 2) ---

    private fun crearTareaEnRoom(titulo: String, descripcion: String) {
        val fechaActual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val nuevaTarea = Tarea(
            titulo = titulo,
            descripcion = descripcion,
            estadoCompletado = false,
            fechaCreacion = fechaActual,
            tiempoAcumuladoSegundos = 0
        )

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.tareaDao().insertarTarea(nuevaTarea)
            cargarTareasDesdeRoom()
        }
    }

    private fun guardarSesionEnRoom(tarea: Tarea) {
        val tareaActualizada = tarea.copy(
            descripcion = descripcionEstado.value,
            tiempoAcumuladoSegundos = tarea.tiempoAcumuladoSegundos + segundosTranscurridos.value
        )

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.tareaDao().actualizarTarea(tareaActualizada)
            cargarTareasDesdeRoom()
        }

        detenerTemporizador()
        segundosTranscurridos.value = 0
        Toast.makeText(this, "Sesión guardada en Room", Toast.LENGTH_SHORT).show()
    }

    private fun toggleCompletadaEnRoom(tarea: Tarea) {
        val tareaActualizada = tarea.copy(estadoCompletado = !tarea.estadoCompletado)
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.tareaDao().actualizarTarea(tareaActualizada)
            cargarTareasDesdeRoom()
        }
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

    // --- MANEJO DEL CICLO DE VIDA (Punto 1) ---

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tareaSeleccionadaId.value?.let { outState.putInt("KEY_TAREA_ID", it) }
        outState.putString("KEY_DESCRIPCION", descripcionEstado.value)
        outState.putInt("KEY_SEGUNDOS", segundosTranscurridos.value)
        outState.putBoolean("KEY_ACTIVO", temporizadorActivo.value)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.containsKey("KEY_TAREA_ID")) {
            tareaSeleccionadaId.value = savedInstanceState.getInt("KEY_TAREA_ID")
        }
        descripcionEstado.value = savedInstanceState.getString("KEY_DESCRIPCION", "")
        segundosTranscurridos.value = savedInstanceState.getInt("KEY_SEGUNDOS", 0)
        temporizadorActivo.value = savedInstanceState.getBoolean("KEY_ACTIVO", false)
    }

    override fun onPause() {
        super.onPause()
        if (temporizadorActivo.value) {
            handler.removeCallbacks(runnable)
        }
    }
}