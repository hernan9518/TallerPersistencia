package com.example.persistencia

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.persistencia.data.AppDatabase
import com.example.persistencia.data.Tarea
import com.example.persistencia.data.TareaDao
import com.example.persistencia.ui.HomeScreen
import com.example.persistencia.ui.TareaScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        // Claves del Bundle (estado de instancia)
        private const val KEY_TAREA_ID = "KEY_TAREA_ID"
        private const val KEY_DESCRIPCION = "KEY_DESCRIPCION"
        private const val KEY_CURSOR_INI = "KEY_CURSOR_INI"
        private const val KEY_CURSOR_FIN = "KEY_CURSOR_FIN"
        private const val KEY_FOCO = "KEY_FOCO"
        private const val KEY_SEGUNDOS = "KEY_SEGUNDOS"
        private const val KEY_META = "KEY_META"
        private const val KEY_DESCANSO = "KEY_DESCANSO"
        private const val KEY_ACTIVO = "KEY_ACTIVO"

        // Borrador rápido (SharedPreferences)
        private const val PREFS_BORRADOR = "borrador_sesion"
    }

    // ---------------- Estado temporal de la sesión (1) ----------------
    private val tareaSeleccionadaId = mutableStateOf<Int?>(null)
    private val descripcionEstado = mutableStateOf(TextFieldValue(""))   // texto + posición del cursor
    private val campoEnfocado = mutableStateOf(false)                    // ¿el campo tenía el foco?
    private val segundosTranscurridos = mutableStateOf(0)
    private val minutosMeta = mutableStateOf(5)
    private val esModoDescanso = mutableStateOf(false)

    // "Intención" del usuario: true = el cronómetro está en marcha (aunque esté en segundo plano)
    private val temporizadorActivo = mutableStateOf(false)

    private var ringtoneActual: Ringtone? = null

    // ---------------- Room + Offline-First (2) ----------------
    private val listaTareas = mutableStateListOf<Tarea>()
    private val hayConexion = mutableStateOf(true)
    private val sincronizando = mutableStateOf(false)

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            if (temporizadorActivo.value) {
                segundosTranscurridos.value++

                val segundosMeta = minutosMeta.value * 60
                if (segundosTranscurridos.value == segundosMeta && !esModoDescanso.value) {
                    reproducirAlarma()
                    esModoDescanso.value = true
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread {
                hayConexion.value = true
                sincronizarPendientes()
            }
        }

        override fun onLost(network: Network) {
            runOnUiThread { hayConexion.value = false }
        }
    }

    //  CICLO DE VIDA (1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recuperación explícita del estado guardado en onSaveInstanceState
        savedInstanceState?.let { restaurarEstado(it) }

        cargarTareasDesdeRoom()

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "home") {

                composable("home") {
                    HomeScreen(
                        listaTareas = listaTareas,
                        hayConexion = hayConexion.value,
                        sincronizando = sincronizando.value,
                        onSincronizar = { sincronizarPendientes() },
                        onAgregarTarea = { titulo, desc -> crearTareaEnRoom(titulo, desc) },
                        onEliminarTarea = { tarea -> eliminarTareaDeRoom(tarea) },
                        onToggleCompletada = { tarea -> toggleCompletadaEnRoom(tarea) },
                        onIniciarSesion = { tarea ->
                            detenerTemporizador()
                            tareaSeleccionadaId.value = tarea.id
                            segundosTranscurridos.value = 0
                            esModoDescanso.value = false
                            campoEnfocado.value = false

                            // Si quedó un borrador de esta tarea, se recupera
                            val borrador = leerBorrador(tarea.id)
                            if (borrador != null && borrador.text != tarea.descripcion) {
                                descripcionEstado.value = borrador
                                Toast.makeText(this@MainActivity, "Borrador recuperado", Toast.LENGTH_SHORT).show()
                            } else {
                                descripcionEstado.value = TextFieldValue(tarea.descripcion)
                            }
                            navController.navigate("sesion")
                        }
                    )
                }

                composable("sesion") {
                    val tareaActual = listaTareas.find { it.id == tareaSeleccionadaId.value }

                    // El gesto/botón "atrás" del sistema también debe cerrar la sesión
                    BackHandler {
                        salirDeSesion()
                        navController.popBackStack()
                    }

                    TareaScreen(
                        tarea = tareaActual,
                        descripcion = descripcionEstado.value,
                        onDescripcionChange = { descripcionEstado.value = it },
                        campoEnfocado = campoEnfocado.value,
                        onFocoChange = { enfocado ->
                            // Perder el foco porque la Activity se pausa/destruye NO cuenta
                            if (enfocado || this@MainActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                campoEnfocado.value = enfocado
                            }
                        },
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
                            salirDeSesion()
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registrarMonitorDeRed()
    }

    override fun onResume() {
        super.onResume()
        // Reanuda el conteo solo si el usuario lo tenía en marcha
        if (temporizadorActivo.value) {
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 1000)
        }
    }

    override fun onPause() {
        super.onPause()
        // Segundo plano / rotación: el conteo se detiene, pero NO se cambia
        // temporizadorActivo (así onSaveInstanceState guarda que estaba corriendo).
        handler.removeCallbacks(runnable)
        silenciarAlarma()
        guardarBorrador() // borrador rápido del campo en edición
    }

    override fun onStop() {
        super.onStop()
        guardarBorrador() // la actividad deja de ser visible
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .unregisterNetworkCallback(networkCallback)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        empaquetarEstado(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restaurarEstado(savedInstanceState) // idempotente: ya se hizo en onCreate
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        silenciarAlarma()
    }

    private fun empaquetarEstado(b: Bundle) {
        tareaSeleccionadaId.value?.let { b.putInt(KEY_TAREA_ID, it) }
        val d = descripcionEstado.value
        b.putString(KEY_DESCRIPCION, d.text)
        b.putInt(KEY_CURSOR_INI, d.selection.start)
        b.putInt(KEY_CURSOR_FIN, d.selection.end)
        b.putBoolean(KEY_FOCO, campoEnfocado.value)
        b.putInt(KEY_SEGUNDOS, segundosTranscurridos.value)
        b.putInt(KEY_META, minutosMeta.value)
        b.putBoolean(KEY_DESCANSO, esModoDescanso.value)
        b.putBoolean(KEY_ACTIVO, temporizadorActivo.value)
    }

    private fun restaurarEstado(b: Bundle) {
        tareaSeleccionadaId.value = if (b.containsKey(KEY_TAREA_ID)) b.getInt(KEY_TAREA_ID) else null

        val texto = b.getString(KEY_DESCRIPCION) ?: ""
        val ini = b.getInt(KEY_CURSOR_INI, texto.length).coerceIn(0, texto.length)
        val fin = b.getInt(KEY_CURSOR_FIN, texto.length).coerceIn(0, texto.length)
        descripcionEstado.value = TextFieldValue(texto, TextRange(ini, fin))
        campoEnfocado.value = b.getBoolean(KEY_FOCO, false)

        segundosTranscurridos.value = b.getInt(KEY_SEGUNDOS, 0)
        minutosMeta.value = b.getInt(KEY_META, 5)
        esModoDescanso.value = b.getBoolean(KEY_DESCANSO, false)
        temporizadorActivo.value = b.getBoolean(KEY_ACTIVO, false)
        // El Handler se re-arma en onResume()
    }

    // ---------------- Borrador rápido (SharedPreferences) ----------------
    // onStop() no recibe un Bundle, por eso el borrador durable va a SharedPreferences.

    private fun guardarBorrador() {
        val id = tareaSeleccionadaId.value ?: return
        val d = descripcionEstado.value
        getSharedPreferences(PREFS_BORRADOR, MODE_PRIVATE).edit()
            .putInt("id", id)
            .putString("texto", d.text)
            .putInt("ini", d.selection.start)
            .putInt("fin", d.selection.end)
            .apply()
    }

    private fun leerBorrador(tareaId: Int): TextFieldValue? {
        val prefs = getSharedPreferences(PREFS_BORRADOR, MODE_PRIVATE)
        if (prefs.getInt("id", -1) != tareaId) return null
        val texto = prefs.getString("texto", null) ?: return null
        val ini = prefs.getInt("ini", texto.length).coerceIn(0, texto.length)
        val fin = prefs.getInt("fin", texto.length).coerceIn(0, texto.length)
        return TextFieldValue(texto, TextRange(ini, fin))
    }

    private fun borrarBorrador() {
        getSharedPreferences(PREFS_BORRADOR, MODE_PRIVATE).edit().clear().apply()
    }

    // ---------------- Temporizador ----------------

    private fun toggleTemporizador() {
        if (temporizadorActivo.value) {
            detenerTemporizador()
        } else {
            temporizadorActivo.value = true
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 1000)
        }
    }

    private fun detenerTemporizador() {
        temporizadorActivo.value = false
        handler.removeCallbacks(runnable)
        silenciarAlarma()
    }

    private fun salirDeSesion() {
        detenerTemporizador()
        borrarBorrador()
        tareaSeleccionadaId.value = null
        campoEnfocado.value = false
        segundosTranscurridos.value = 0
        esModoDescanso.value = false
    }

    private fun reproducirAlarma() {
        try {
            ringtoneActual?.stop()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtoneActual = RingtoneManager.getRingtone(applicationContext, uri)
            ringtoneActual?.play()
            Toast.makeText(this, "🔔 ¡Meta alcanzada! Tómate un descanso.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun silenciarAlarma() {
        if (ringtoneActual?.isPlaying == true) ringtoneActual?.stop()
    }

    //  ROOM: CRUD (2)

    /** Ejecuta una escritura en Room fuera del hilo principal. NonCancellable: si el usuario gira
     *  la pantalla justo al guardar, la escritura no se pierde. */
    private fun ejecutarEnRoom(bloque: suspend TareaDao.() -> Unit) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO + NonCancellable) {
                AppDatabase.getDatabase(applicationContext).tareaDao().bloque()
            }
            cargarTareasDesdeRoom()
            sincronizarPendientes() // si hay red, sincroniza de inmediato; si no, queda pendiente
        }
    }

    private fun cargarTareasDesdeRoom() {
        lifecycleScope.launch {
            val tareas = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext).tareaDao().obtenerTodasLasTareas()
            }
            listaTareas.clear()
            listaTareas.addAll(tareas)
        }
    }

    // CREATE
    private fun crearTareaEnRoom(titulo: String, descripcion: String) {
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val nueva = Tarea(titulo = titulo, descripcion = descripcion, fechaCreacion = fecha)
        ejecutarEnRoom { insertarTarea(nueva) }
    }

    // UPDATE (marcar completada)
    private fun toggleCompletadaEnRoom(tarea: Tarea) {
        val actualizada = tarea.copy(
            estadoCompletado = !tarea.estadoCompletado,
            pendienteSincronizacion = true
        )
        ejecutarEnRoom { actualizarTarea(actualizada) }
    }

    // UPDATE (fin de la sesión: notas + tiempo)
    private fun guardarSesionEnRoom(tarea: Tarea) {
        val actualizada = tarea.copy(
            descripcion = descripcionEstado.value.text,
            tiempoAcumuladoSegundos = tarea.tiempoAcumuladoSegundos + segundosTranscurridos.value,
            pendienteSincronizacion = true
        )
        ejecutarEnRoom { actualizarTarea(actualizada) }
        salirDeSesion()
        Toast.makeText(this, "Sesión guardada en Room", Toast.LENGTH_SHORT).show()
    }

    // DELETE (borrado lógico hasta que se sincronice)
    private fun eliminarTareaDeRoom(tarea: Tarea) {
        val marcada = tarea.copy(eliminada = true, pendienteSincronizacion = true)
        ejecutarEnRoom { actualizarTarea(marcada) }
    }


    //  OFFLINE-FIRST: detección de red + sincronización

    private fun registrarMonitorDeRed() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        hayConexion.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        cm.registerDefaultNetworkCallback(networkCallback)
    }

    /** Envía al "servidor" los cambios hechos sin conexión. Es idempotente: si se interrumpe,
     *  los registros siguen marcados como pendientes y se reintenta después.
      */
    private fun sincronizarPendientes() {
        if (!hayConexion.value || sincronizando.value) return
        lifecycleScope.launch {
            sincronizando.value = true
            try {
                withContext(Dispatchers.IO) {
                    val dao = AppDatabase.getDatabase(applicationContext).tareaDao()
                    val pendientes = dao.obtenerPendientes()
                    if (pendientes.isEmpty()) return@withContext
                    delay(1500) // simula la latencia de red
                    pendientes.forEach { t ->
                        if (t.eliminada) dao.eliminarPorId(t.id) else dao.marcarSincronizada(t.id)
                    }
                }
                cargarTareasDesdeRoom()
            } finally {
                sincronizando.value = false
            }
        }
    }
}
