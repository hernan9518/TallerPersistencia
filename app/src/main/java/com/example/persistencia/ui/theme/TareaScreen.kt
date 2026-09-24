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
    minutosMeta: Int,
    onMetaSelected: (Int) -> Unit,
    esModoDescanso: Boolean,
    enEjecucion: Boolean,
    onToggleTemporizador: () -> Unit,
    onGuardarSesion: () -> Unit,
    onVolver: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tarea?.titulo ?: "Sesión Pomodoro") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Text("⬅️")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (esModoDescanso)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.primaryContainer
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
                text = if (esModoDescanso) "☕ ¡MODO DESCANSO ACTIVO!" else "🎯 MODO ENFOQUE ACTIVO",
                style = MaterialTheme.typography.titleMedium,
                color = if (esModoDescanso) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- SELECTOR DE TIEMPO META ---
            Text(text = "Seleccionar Meta de Enfoque:", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                listOf(1, 5, 10, 15, 25).forEach { min ->
                    FilterChip(
                        selected = minutosMeta == min,
                        onClick = { onMetaSelected(min) },
                        label = { Text("$min min") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- TARJETA DE CRONÓMETRO CON CONTEO ASCENDENTE ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (esModoDescanso)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tiempo Acumulado (Meta: $minutosMeta min)",
                        style = MaterialTheme.typography.titleMedium
                    )
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
                label = { Text("Escribe tus notas extensas aquí...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onGuardarSesion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Finalizar y Guardar Tiempo en Room (Punto 2)")
            }
        }
    }
}