package com.simplenotepad.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplenotepad.BuildConfig

@Composable
fun GoToLineDialog(
    currentLine: Int,
    maxLine: Int,
    onGoTo: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var lineText by remember { mutableStateOf(currentLine.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go To Line") },
        text = {
            Column {
                Text("Line number (1 - $maxLine):")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lineText,
                    onValueChange = { lineText = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    lineText.toIntOrNull()?.let { onGoTo(it) }
                }
            ) {
                Text("Go To")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FontDialog(
    currentSize: Float,
    onFontSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var fontSize by remember { mutableFloatStateOf(currentSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Font Settings") },
        text = {
            Column {
                Text("Font Size: ${fontSize.toInt()} sp")
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 8f..32f,
                    steps = 23,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(10f, 12f, 14f, 16f, 18f, 20f).forEach { size ->
                        TextButton(onClick = { fontSize = size }) {
                            Text("${size.toInt()}", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onFontSizeChange(fontSize); onDismiss() }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    onShowChangelog: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notepad") },
        text = {
            Column {
                Text(
                    "Version ${BuildConfig.VERSION_NAME}",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "A simple, lightweight text editor for Android.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Inspired by Windows Notepad",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onShowChangelog) {
                Text("Changelog")
            }
        }
    )
}

@Composable
fun ChangelogDialog(
    onDismiss: () -> Unit
) {
    val changelog = listOf(
        "1.0.x" to listOf(
            "Add Select All and Copy icons to toolbar",
            "Auto-scroll to keep cursor visible while typing",
            "Telegram notifications with direct APK download",
            "Consistent APK signing for updates without uninstall",
            "Fix font size dialog to show actual displayed size",
            "Version number in About dialog",
            "This changelog view"
        ),
        "1.0.0" to listOf(
            "Initial release",
            "Tabbed interface for multiple files",
            "Markdown formatting toolbar",
            "Find and Replace",
            "Auto-save",
            "Theme support (Light/Dark/System)",
            "Bottom sheet menus (Android style)",
            "Formatted/Markdown view toggle",
            "Notes explorer (All Notes view)"
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Changelog") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                changelog.forEachIndexed { index, (version, changes) ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(
                        "Version $version",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    changes.forEach { change ->
                        Text(
                            "• $change",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Unsaved Changes") },
        text = {
            Text("Do you want to save changes to your file?")
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) {
                    Text("Don't Save")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun ThemeDialog(
    currentTheme: String,
    onThemeSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column {
                listOf("AUTO" to "System default", "LIGHT" to "Light", "DARK" to "Dark").forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = currentTheme == value,
                            onClick = { onThemeSelect(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun CloseTabDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Close Tab") },
        text = {
            Text("This tab has unsaved changes. Do you want to save before closing?")
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) {
                    Text("Don't Save")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun RecentFilesDialog(
    recentFiles: List<String>,
    onFileSelect: (String) -> Unit,
    onRemoveFile: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recent Files") },
        text = {
            Column {
                if (recentFiles.isEmpty()) {
                    Text(
                        "No recent files",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    recentFiles.forEach { uri ->
                        val displayName = uri.substringAfterLast("/").substringAfterLast("%2F")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            TextButton(
                                onClick = { onFileSelect(uri) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = displayName,
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            TextButton(onClick = { onRemoveFile(uri) }) {
                                Text("×", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            if (recentFiles.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }
        }
    )
}
