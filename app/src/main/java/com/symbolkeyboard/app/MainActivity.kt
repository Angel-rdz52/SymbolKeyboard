package com.symbolkeyboard.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = CharMapRepository(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        repository = repository,
                        onOpenKeyboardSettings = { openKeyboardSettings() }
                    )
                }
            }
        }
    }

    private fun openKeyboardSettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }
}

/** Fila editable de mapeo: original -> reemplazo. */
data class MapRow(var original: String, var replacement: String)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: CharMapRepository,
    onOpenKeyboardSettings: () -> Unit
) {
    var enabled by remember { mutableStateOf(repository.isReplacementEnabled()) }
    val rows = remember {
        mutableStateListOf<MapRow>().apply {
            repository.loadMap().forEach { (k, v) -> add(MapRow(k, v)) }
        }
    }
    var showPresetsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Symbol Keyboard") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Reemplazá letras por símbolos Unicode mientras escribís",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenKeyboardSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Activar teclado en Ajustes del sistema")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Reemplazo activado", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        repository.setReplacementEnabled(it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Mapeo de caracteres", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { showPresetsDialog = true }) {
                    Text("Presets")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Original",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Reemplazo",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(1.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rows.size) { index ->
                    val row = rows[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = row.original,
                            onValueChange = {
                                rows[index] = row.copy(original = it)
                                persistSilently(repository, rows, enabled)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = row.replacement,
                            onValueChange = {
                                rows[index] = row.copy(replacement = it)
                                persistSilently(repository, rows, enabled)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            rows.removeAt(index)
                            persistSilently(repository, rows, enabled)
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Borrar fila")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    rows.add(MapRow("", ""))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agregar fila")
            }
        }
    }

    if (showPresetsDialog) {
        PresetsDialog(
            onDismiss = { showPresetsDialog = false },
            onPresetSelected = { preset ->
                rows.clear()
                preset.forEach { (k, v) -> rows.add(MapRow(k, v)) }
                persistSilently(repository, rows, enabled)
                showPresetsDialog = false
            }
        )
    }
}

private fun persistSilently(
    repository: CharMapRepository,
    rows: List<MapRow>,
    enabled: Boolean
) {
    val map = LinkedHashMap<String, String>()
    for (row in rows) {
        val key = row.original.trim()
        if (key.isNotEmpty()) {
            map[key] = row.replacement
        }
    }
    repository.saveMap(map)
    repository.setReplacementEnabled(enabled)
}

@Composable
fun PresetsDialog(
    onDismiss: () -> Unit,
    onPresetSelected: (Map<String, String>) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegí un preset") },
        text = {
            Column {
                PresetOption("Al revés (ɐqɔp…)") {
                    onPresetSelected(CharMapRepository.PRESET_UPSIDE_DOWN)
                }
                PresetOption("Leet speak (4, 3, 1, 0…)") {
                    onPresetSelected(CharMapRepository.PRESET_LEET)
                }
                PresetOption("Runas (ᚨᛒᚲᛞ…)") {
                    onPresetSelected(CharMapRepository.PRESET_RUNES)
                }
                PresetOption("Círculos (ⓐⓑⓒⓓ…)") {
                    onPresetSelected(CharMapRepository.PRESET_CIRCLES)
                }
                PresetOption("Sin cambios (resetear)") {
                    onPresetSelected(CharMapRepository.PRESET_NONE)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun PresetOption(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            TextButton(onClick = onClick) {
                Text(label)
            }
        }
    }
}

