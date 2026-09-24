package com.example.persistencia

import android.media.Ringtone
import android.media.RingtoneManager
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
    private var minutosMeta = mutableStateOf(5) // Por defecto 5 min
    private var esModoDescanso = mutableStateOf(false)
    private var temporizadorActivo = mutableStateOf(false)

    // Referencia global para detener la tonada de alarma
    private var ringtoneActual: Ringtone? = null

    // Lista observable para Room (Punto 2)
    private var listaTareas = mutableStateListOf<Tarea>()

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            if (temporizadorActivo.value) {
                segundosTranscurridos.value++

                // Verificar si se alcanzó la meta de tiempo seleccionada
                val segundosMeta = minutosMeta.value * 60
                if (segundosTranscurridos.value == segundosMeta && !esModoDescanso.value) {
                    reproducirAlarma()
                    esModoDescanso.value = true
                }

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
            minutosMeta.value = bundle.getInt("KEY_META", 5)
            esModoDescanso.value = bundle.getBoolean("KEY_DESCANSO", false)
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
                            esModoDescanso.value = false
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
                        minutosMeta = minutosMeta.value,
                        onMetaSelected = { minutosMeta.value = it },
                        esModoDescanso = esModoDescanso.value,
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

    private fun reproducirAlarma() {
        try {
            // Si ya está sonando, lo detiene antes de iniciar uno nuevo
            ringtoneActual?.stop()

            val notificacionUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            ringtoneActual = RingtoneManager.getRingtone(applicationContext, notificacionUri)
            ringtoneActual?.play()

            Toast.makeText(this, "🔔 ¡Meta alcanzada! Tómate un descanso.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleTemporizador() {
        temporizadorActivo.value = !temporizadorActivo.value
        if (temporizadorActivo.value) {
            handler.post(runnable)
        } else {
            detenerTemporizador()
        }
    }

    private fun detenerTemporizador() {
        temporizadorActivo.value = false
        handler.removeCallbacks(runnable)

        // Silenciar la alarma inmediatamente
        if (ringtoneActual?.isPlaying == true) {
            ringtoneActual?.stop()
        }
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
        outState.putInt("KEY_META", minutosMeta.value)
        outState.putBoolean("KEY_DESCANSO", esModoDescanso.value)
        outState.putBoolean("KEY_ACTIVO", temporizadorActivo.value)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.containsKey("KEY_TAREA_ID")) {
            tareaSeleccionadaId.value = savedInstanceState.getInt("KEY_TAREA_ID")
        }
        descripcionEstado.value = savedInstanceState.getString("KEY_DESCRIPCION", "")
        segundosTranscurridos.value = savedInstanceState.getInt("KEY_SEGUNDOS", 0)
        minutosMeta.value = savedInstanceState.getInt("KEY_META", 5)
        esModoDescanso.value = savedInstanceState.getBoolean("KEY_DESCANSO", false)
        temporizadorActivo.value = savedInstanceState.getBoolean("KEY_ACTIVO", false)
    }

    override fun onPause() {
        super.onPause()
        if (temporizadorActivo.value) {
            detenerTemporizador()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberación de recursos del audio al destruir la actividad
        if (ringtoneActual?.isPlaying == true) {
            ringtoneActual?.stop()
        }
    }
}