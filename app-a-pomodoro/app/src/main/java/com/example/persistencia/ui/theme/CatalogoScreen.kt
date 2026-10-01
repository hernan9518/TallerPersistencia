package com.example.persistencia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.persistencia.data.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    productos: List<Producto>,
    actualizando: Boolean,
    error: String?,
    onVolver: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo (caché híbrida)") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Text("⬅️") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            // Estado de la sincronización: se ve primero el caché, luego se actualiza.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        actualizando -> "🔄 Actualizando desde la nube..."
                        error != null -> "📴 Mostrando caché local (sin conexión)"
                        else -> "✅ Catálogo actualizado"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (productos.isEmpty() && !actualizando) {
                Text("Aún no hay productos en caché. Conéctate a internet para descargarlos.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(productos) { producto ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(producto.titulo, style = MaterialTheme.typography.titleSmall)
                                Text(producto.categoria, style = MaterialTheme.typography.labelSmall)
                                Text("$${producto.precio}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}