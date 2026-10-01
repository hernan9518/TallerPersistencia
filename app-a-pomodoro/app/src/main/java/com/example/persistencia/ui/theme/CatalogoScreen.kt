package com.example.persistencia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.persistencia.data.TareaSugerida

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    sugeridas: List<TareaSugerida>,
    actualizando: Boolean,
    error: String?,
    onImportar: (TareaSugerida) -> Unit,
    onVolver: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tareas sugeridas (caché híbrida)") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Text("⬅️") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            // Primero se ve el caché local; luego se actualiza en segundo plano.
            Text(
                text = when {
                    actualizando -> "🔄 Actualizando desde la nube..."
                    error != null -> "📴 Mostrando caché local (sin conexión)"
                    else -> "✅ Sugerencias actualizadas"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Importa una sugerencia para convertirla en una tarea real: " +
                        "podrás usar el cronómetro de enfoque sobre ella.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (sugeridas.isEmpty() && !actualizando) {
                Text("Aún no hay sugerencias en caché. Conéctate a internet para descargarlas.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sugeridas) { sugerida ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sugerida.titulo, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = if (sugerida.completadaEnOrigen) "Completada en el origen" else "Pendiente en el origen",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (sugerida.importada) {
                                    Text("✔ Importada", style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Button(onClick = { onImportar(sugerida) }) {
                                        Text("Importar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}