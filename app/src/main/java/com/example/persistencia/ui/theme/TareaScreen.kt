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
import kotlin.math.roundToInt

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
        },
        // imePadding() asegura que el teclado no tape los componentes al escribir
        modifier = Modifier.imePadding()
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

            Spacer(modifier = Modifier.height(12.dp))

            // --- SELECTOR DE TIEMPO TIPO SLIDER ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Meta de tiempo: $minutosMeta minutos",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Slider(
                        value = minutosMeta.toFloat(),
                        onValueChange = { onMetaSelected(it.roundToInt()) },
                        valueRange = 1f..60f,
                        steps = 59, // Permite seleccionar de 1 en 1 min
                        enabled = !enEjecucion // Se deshabilita mientras corre el cronómetro
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TARJETA DE CRONÓMETRO ---
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
                        text = "Tiempo Acumulado",
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

            // --- SECCIÓN NOTAS / BORRADOR ---
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
                Text("Finalizar y Guardar Tiempo en Room")
            }
        }
    }
}