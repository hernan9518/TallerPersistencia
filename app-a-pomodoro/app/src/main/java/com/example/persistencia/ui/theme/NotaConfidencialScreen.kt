package com.example.persistencia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotaConfidencialScreen(
    rutaArchivoExterno: String,
    onGuardarCifrada: (String) -> Unit,
    onGuardarSinCifrar: (String) -> Unit,
    onLeerCifrada: () -> String,
    onLeerSinCifrar: () -> String,
    onVolver: () -> Unit
) {
    var texto by rememberSaveable { mutableStateOf("") }
    var leidoCifrado by rememberSaveable { mutableStateOf("") }
    var leidoSinCifrar by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notas Confidenciales") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Text("⬅️") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Compara qué tan seguro queda un mismo texto guardado cifrado " +
                        "frente a guardado sin cifrar.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Escribe la nota sensible") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onGuardarCifrada(texto) }) {
                    Text("Guardar cifrada")
                }
                OutlinedButton(onClick = { onGuardarSinCifrar(texto) }) {
                    Text("Guardar sin cifrar")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                leidoCifrado = onLeerCifrada()
                leidoSinCifrar = onLeerSinCifrar()
            }) {
                Text("Leer ambos archivos")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Contenido leído (cifrado, interno):", style = MaterialTheme.typography.titleSmall)
                    Text(if (leidoCifrado.isBlank()) "—" else leidoCifrado)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Contenido leído (sin cifrar, externo):", style = MaterialTheme.typography.titleSmall)
                    Text(if (leidoSinCifrar.isBlank()) "—" else leidoSinCifrar)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Ruta del archivo externo (para inspeccionar con ADB o Device File Explorer):",
                style = MaterialTheme.typography.labelSmall
            )
            Text(rutaArchivoExterno, style = MaterialTheme.typography.labelSmall)
        }
    }
}