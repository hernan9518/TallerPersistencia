package com.example.persistencia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.persistencia.data.Tarea

@Composable
fun TareaScreen(
    titulo: String,
    onTituloChange: (String) -> Unit,
    descripcion: String,
    onDescripcionChange: (String) -> Unit,
    segundosTranscurridos: Int,
    enEjecucion: Boolean,
    onToggleTemporizador: () -> Unit,
    onGuardarTarea: () -> Unit,
    listaTareas: List<Tarea>,
    onEliminarTarea: (Tarea) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Punto 1: Ciclo de Vida + Punto 2: Room",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECCIÓN TEMPORIZADOR (Punto 1) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Temporizador Activo", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${segundosTranscurridos / 60}:${String.format("%02d", segundosTranscurridos % 60)}",
                    style = MaterialTheme.typography.displayMedium
                )
                Button(onClick = onToggleTemporizador) {
                    Text(if (enEjecucion) "Pausar Cronómetro" else "Iniciar Cronómetro")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECCIÓN FORMULARIO (Punto 1 & Punto 2) ---
        OutlinedTextField(
            value = titulo,
            onValueChange = onTituloChange,
            label = { Text("Título de la tarea") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = descripcion,
            onValueChange = onDescripcionChange,
            label = { Text("Descripción / Notas extensas") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onGuardarTarea,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Tarea Permanentemente (Room)")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // --- SECCIÓN LISTA DE TAREAS (Punto 2: CRUD) ---
        Text(text = "Tareas Almacenadas Localmente:", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(listaTareas) { tarea ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = tarea.titulo, style = MaterialTheme.typography.titleSmall)
                            Text(text = tarea.descripcion, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Fecha: ${tarea.fechaCreacion}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onEliminarTarea(tarea) }) {
                            Text("❌")
                        }
                    }
                }
            }
        }
    }
}