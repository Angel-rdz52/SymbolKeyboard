package com.example.symbolkeyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MapRow(var from: String, var to: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = CharMapRepository(this)
        setContent {
            MaterialTheme {
                Surface {
                    SettingsScreen(
                        repo = repo,
                        onOpenSystemKeyboardSettings = {
                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repo: CharMapRepository, onOpenSystemKeyboardSettings: () -> Unit) {
    var rows by remember {
        mutableStateOf(repo.loadMap().map { MapRow(it.key, it.value) }.toMutableStateList())
    }
    var enabled by remember { mutableStateOf(repo.isEnabled()) }
    var showPresets by remember { mutableStateOf(false) }

    fun persist() {
        repo.saveMap(rows.filter { it.from.isNotBlank() }.associate { it.from to it.to })
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Symbol Keyboard") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                rows.add(MapRow("", ""))
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar fila")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Paso 1: activar el teclado en el sistema
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("1. Activá el teclado", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Primero hay que habilitarlo como método de entrada del sistema.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenSystemKeyboardSettings) {
                        Text("Abrir ajustes de teclado")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Switch general on/off
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reemplazo de símbolos activo", modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    repo.setEnabled(it)
                })
            }

            Spacer(Modifier.height(8.dp))

            // Presets
            OutlinedButton(onClick = { showPresets = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Cargar preset (Al revés, Leet, Runas, Círculos...)")
            }

            Spacer(Modifier.height(12.dp))

            Text("2. Personalizá tu mapeo", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            // Encabezado de las 2 columnas
            Row(Modifier.fillMaxWidth()) {
                Text("Original", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("Reemplazo", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(40.dp))
            }
            Divider()

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rows.size) { index ->
                    val row = rows[index]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = row.from,
                            onValueChange = { rows[index] = row.copy(from = it); persist() },
                            singleLine = true,
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            placeholder = { Text("a") }
                        )
                        OutlinedTextField(
                            value = row.to,
                            onValueChange = { rows[index] = row.copy(to = it); persist() },
                            singleLine = true,
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            placeholder = { Text("ɐ") }
                        )
                        IconButton(onClick = {
                            rows.removeAt(index)
                            persist()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Borrar")
                        }
                    }
                }
            }
        }
    }

    if (showPresets) {
        AlertDialog(
            onDismissRequest = { showPresets = false },
            title = { Text("Elegí un preset") },
            text = {
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    CharMapRepository.allPresets().forEach { (name, map) ->
                        TextButton(onClick = {
                            rows.clear()
                            map.forEach { (k, v) -> rows.add(MapRow(k, v)) }
                            persist()
                            showPresets = false
                        }) {
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresets = false }) { Text("Cerrar") }
            }
        )
    }
}

