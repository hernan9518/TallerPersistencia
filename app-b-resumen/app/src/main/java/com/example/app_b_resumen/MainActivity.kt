package com.example.app_b_resumen

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// URI y nombres de columna copiados del TareaContract de App A.
// App B NO importa código de App A: solo conoce este contrato por convención.
private val URI_TAREAS: Uri = Uri.parse("content://com.example.persistencia.provider/tareas")
private const val COL_TITULO = "titulo"
private const val COL_ESTADO_COMPLETADO = "estadoCompletado"
private const val COL_TIEMPO_ACUMULADO = "tiempoAcumuladoSegundos"

data class ResumenTarea(val titulo: String, val completada: Boolean, val segundos: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaResumen(onConsultar = { consultarTareasDeAppA() })
            }
        }
    }

    /**
     * Lee la tabla de tareas de App A a través de ContentResolver.
     * Si App A no tiene instalado el provider, o si App B no declaró el
     * permiso en su manifiesto, esto lanza SecurityException.
     */
    private fun consultarTareasDeAppA(): Result<List<ResumenTarea>> {
        return try {
            val cursor = contentResolver.query(
                URI_TAREAS, null, null, null, null
            ) ?: return Result.failure(Exception("El proveedor de App A no respondió (¿está instalada?)"))

            val lista = mutableListOf<ResumenTarea>()
            cursor.use {
                val idxTitulo = it.getColumnIndexOrThrow(COL_TITULO)
                val idxCompletada = it.getColumnIndexOrThrow(COL_ESTADO_COMPLETADO)
                val idxSegundos = it.getColumnIndexOrThrow(COL_TIEMPO_ACUMULADO)
                while (it.moveToNext()) {
                    lista.add(
                        ResumenTarea(
                            titulo = it.getString(idxTitulo),
                            completada = it.getInt(idxCompletada) != 0,
                            segundos = it.getInt(idxSegundos)
                        )
                    )
                }
            }
            Result.success(lista)
        } catch (e: SecurityException) {
            // Esto es justo lo que el taller pide demostrar: sin el permiso, no hay acceso.
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaResumen(onConsultar: () -> Result<List<ResumenTarea>>) {
    var tareas by remember { mutableStateOf<List<ResumenTarea>>(emptyList()) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Resumen de Enfoque (App B)") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Button(onClick = {
                val resultado = onConsultar()
                resultado
                    .onSuccess { tareas = it; mensajeError = null }
                    .onFailure { mensajeError = it.message ?: "Error desconocido" }
            }) {
                Text("Consultar tareas de App A")
            }

            Spacer(modifier = Modifier.height(16.dp))

            mensajeError?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text("⚠️ $it", modifier = Modifier.padding(12.dp))
                }
            }

            if (tareas.isNotEmpty()) {
                val completadas = tareas.count { it.completada }
                val segundosTotales = tareas.sumOf { it.segundos }

                Text("Total de tareas: ${tareas.size}")
                Text("Completadas: $completadas")
                Text("Tiempo total enfocado: ${segundosTotales / 60} min ${segundosTotales % 60} s")

                Spacer(modifier = Modifier.height(16.dp))

                tareas.forEach { t ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(t.titulo)
                        Text(if (t.completada) "✔" else "—")
                    }
                }
            }
        }
    }
}