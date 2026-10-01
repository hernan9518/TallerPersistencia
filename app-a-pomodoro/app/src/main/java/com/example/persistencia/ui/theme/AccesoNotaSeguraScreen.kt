package com.example.persistencia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearPasswordScreen(
    onCrear: (String) -> Unit,
    onVolver: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear contraseña") },
                navigationIcon = { IconButton(onClick = onVolver) { Text("⬅️") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Es la primera vez que entras a Notas Confidenciales. Crea una contraseña para protegerlas.")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Nueva contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmacion,
                onValueChange = { confirmacion = it; error = null },
                label = { Text("Confirmar contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    when {
                        password.length < 4 -> error = "La contraseña debe tener al menos 4 caracteres"
                        password != confirmacion -> error = "Las contraseñas no coinciden"
                        else -> onCrear(password)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear y continuar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngresarPasswordScreen(
    onIngresar: (String) -> Boolean, // true si la contraseña es correcta
    onVolver: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notas Confidenciales") },
                navigationIcon = { IconButton(onClick = onVolver) { Text("⬅️") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Ingresa tu contraseña para ver las notas confidenciales.")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (!onIngresar(password)) {
                        error = "Contraseña incorrecta"
                        password = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ingresar")
            }
        }
    }
}