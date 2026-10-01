package com.example.persistencia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.persistencia.data.NotaConfidencial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaNotasScreen(
    notas: List<NotaConfidencial>,
    rutaArchivoExterno: String,
    onLeerCopiaSinCifrar: () -> String,
    onNuevaNota: () -> Unit,
    onAbrirNota: (String) -> Unit,
    onEliminarNota: (String) -> Unit,
    onVolver: () -> Unit
) {
    var mostrarComparacion by remember { mutableStateOf(false) }
    var textoComparacion by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notas Confidenciales") },
                navigationIcon = { IconButton(onClick = onVolver) { Text("⬅️") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaNota) {
                Icon(Icons.Default.Add, contentDescription = "Nueva nota")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            Text(
                "Cada nota se guarda cifrada. Hay también una copia sin cifrar en " +
                        "almacenamiento externo, solo para comparar qué tan legible queda.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (notas.isEmpty()) {
                Text("No hay notas confidenciales. Toca + para crear una.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(notas) { nota ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onAbrirNota(nota.id) }
                                ) {
                                    Text(nota.titulo, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "🔒 Contenido cifrado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onEliminarNota(nota.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar nota")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                textoComparacion = onLeerCopiaSinCifrar()
                mostrarComparacion = true
            }) {
                Text("Ver copia externa sin cifrar")
            }

            if (mostrarComparacion) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Esto es lo que cualquiera podría leer en la SD, sin cifrar:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(textoComparacion)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(rutaArchivoExterno, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotaEditorScreen(
    tituloInicial: String,
    contenidoInicial: String,
    onGuardar: (String, String) -> Unit,
    onVolver: () -> Unit
) {
    var titulo by rememberSaveable { mutableStateOf(tituloInicial) }
    var contenido by rememberSaveable { mutableStateOf(contenidoInicial) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tituloInicial.isBlank()) "Nueva nota" else "Editar nota") },
                navigationIcon = { IconButton(onClick = onVolver) { Text("⬅️") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = contenido,
                onValueChange = { contenido = it },
                label = { Text("Contenido confidencial") },
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { if (titulo.isNotBlank()) onGuardar(titulo, contenido) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }
        }
    }
}