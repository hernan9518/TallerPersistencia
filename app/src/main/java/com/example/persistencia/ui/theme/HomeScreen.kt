package com.example.persistencia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.persistencia.data.Tarea
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    listaTareas: List<Tarea>,
    hayConexion: Boolean,
    sincronizando: Boolean,
    onSincronizar: () -> Unit,
    onAgregarTarea: (String, String) -> Unit,
    onEliminarTarea: (Tarea) -> Unit,
    onToggleCompletada: (Tarea) -> Unit,
    onIniciarSesion: (Tarea) -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var nuevoTitulo by remember { mutableStateOf("") }
    var nuevaDescripcion by remember { mutableStateOf("") }
    var mostrarAcercaDe by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Sesiones de Enfoque") },
                actions = {
                    IconButton(onClick = { mostrarAcercaDe = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Acerca de")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogo = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Tarea")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // --- Estado Offline-First ---
            val pendientes = listaTareas.count { it.pendienteSincronizacion }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (hayConexion) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            sincronizando -> "🔄 Sincronizando..."
                            !hayConexion -> "📴 Sin conexión · $pendientes cambio(s) pendiente(s)"
                            pendientes > 0 -> "🌐 En línea · $pendientes cambio(s) pendiente(s)"
                            else -> "✅ Todo sincronizado"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (hayConexion && pendientes > 0 && !sincronizando) {
                        TextButton(onClick = onSincronizar) { Text("Sincronizar") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Selecciona una tarea para iniciar el cronómetro de enfoque o crea una nueva.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (listaTareas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay tareas creadas. ¡Toca + para agregar una!")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(listaTareas) { tarea ->
                        ItemTarea(
                            tarea = tarea,
                            onEliminar = { onEliminarTarea(tarea) },
                            onToggleCompletada = { onToggleCompletada(tarea) },
                            onIniciarSesion = { onIniciarSesion(tarea) }
                        )
                    }
                }
            }
        }

        if (mostrarAcercaDe) {
            AlertDialog(
                onDismissRequest = { mostrarAcercaDe = false },
                title = { Text("Acerca de:") },
                text = {
                    Column {
                        Text("Sesiones de Enfoque", style = MaterialTheme.typography.titleMedium)
                        Text("Versión 1.0", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Integrantes:", style = MaterialTheme.typography.titleSmall)
                        Text("• David Barahona")
                        Text("• Jesús Villota")
                        Text("• Manuel Rosero")
                        Text("• Brayan Perenguez")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { mostrarAcercaDe = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        // Diálogo para crear nueva tarea
        if (mostrarDialogo) {
            AlertDialog(
                onDismissRequest = { mostrarDialogo = false },
                title = { Text("Nueva Tarea / Asignatura") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = nuevoTitulo,
                            onValueChange = { nuevoTitulo = it },
                            label = { Text("Título") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = nuevaDescripcion,
                            onValueChange = { nuevaDescripcion = it },
                            label = { Text("Descripción / Objetivo") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (nuevoTitulo.isNotBlank()) {
                                onAgregarTarea(nuevoTitulo, nuevaDescripcion)
                                nuevoTitulo = ""
                                nuevaDescripcion = ""
                                mostrarDialogo = false
                            }
                        }
                    ) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogo = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun ItemTarea(
    tarea: Tarea,
    onEliminar: () -> Unit,
    onToggleCompletada: () -> Unit,
    onIniciarSesion: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (tarea.estadoCompletado)
                MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = tarea.estadoCompletado,
                onCheckedChange = { onToggleCompletada() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = tarea.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (tarea.estadoCompletado) TextDecoration.LineThrough else null
                )
                if (tarea.descripcion.isNotBlank()) {
                    Text(
                        text = tarea.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Formateo de tiempo acumulado
                val minutos = tarea.tiempoAcumuladoSegundos / 60
                val segundos = tarea.tiempoAcumuladoSegundos % 60
                Text(
                    text = "⏱️ Tiempo enfocado: ${minutos}m ${segundos}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (tarea.pendienteSincronizacion) "⏳ Pendiente de sincronizar" else "✔ Sincronizada",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón para iniciar sesión de temporizador
            IconButton(onClick = onIniciarSesion) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Iniciar Enfoque",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Botón para eliminar
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar Tarea",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}