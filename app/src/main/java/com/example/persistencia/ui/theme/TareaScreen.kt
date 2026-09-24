package com.example.persistencia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.persistencia.data.Tarea

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareaScreen(
    tarea: Tarea?,
    descripcion: String,
    onDescripcionChange: (String) -> Unit,
    segundosTranscurridos: Int,
    enEjecucion: Boolean,
    onToggleTemporizador: () -> Unit,
    onGuardarSesion: () -> Unit,
    onVolver: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tarea?.titulo ?: "Sesión de Enfoque") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Text("⬅️")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Punto 1: Ciclo de Vida Temporal (onSaveInstanceState / Bundle)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- SECCIÓN TEMPORIZADOR (Punto 1) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Tiempo de esta sesión", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${segundosTranscurridos / 60}:${String.format("%02d", segundosTranscurridos % 60)}",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onToggleTemporizador) {
                        Text(if (enEjecucion) "Pausar Cronómetro" else "Iniciar Cronómetro")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SECCIÓN NOTAS / BORRADOR (Punto 1) ---
            Text(
                text = "Bitácora / Apuntes de la sesión:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = descripcion,
                onValueChange = onDescripcionChange,
                label = { Text("Escribe tus avances o notas extensas aquí...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onGuardarSesion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Finalizar y Guardar en Room (Punto 2)")
            }
        }
    }
}