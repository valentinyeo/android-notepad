package com.simplenotepad.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopMenuBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onShowMenu: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onQuickSave: () -> Unit,
    onFormatBold: () -> Unit,
    onFormatItalic: () -> Unit,
    onFormatStrikethrough: () -> Unit,
    onFormatInlineCode: () -> Unit,
    onFormatHeader: (Int) -> Unit,
    onFormatBulletList: () -> Unit,
    onFormatNumberedList: () -> Unit,
    onFormatLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHeaderMenu by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onShowMenu) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        title = {
            // Formatting toolbar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header dropdown
                Box {
                    IconButton(
                        onClick = { showHeaderMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Aa",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "▾",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showHeaderMenu,
                        onDismissRequest = { showHeaderMenu = false }
                    ) {
                        val headerLabels = listOf(
                            1 to "Title",
                            2 to "Subtitle",
                            3 to "Heading",
                            4 to "Subheading",
                            5 to "Section",
                            6 to "Subsection"
                        )
                        headerLabels.forEach { (level, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { showHeaderMenu = false; onFormatHeader(level) }
                            )
                        }
                    }
                }

                // List dropdown
                Box {
                    IconButton(
                        onClick = { showListMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = "List",
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "▾",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showListMenu,
                        onDismissRequest = { showListMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Bulleted list") },
                            onClick = { showListMenu = false; onFormatBulletList() }
                        )
                        DropdownMenuItem(
                            text = { Text("Numbered list") },
                            onClick = { showListMenu = false; onFormatNumberedList() }
                        )
                    }
                }

                // Bold
                IconButton(
                    onClick = onFormatBold,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.FormatBold,
                        contentDescription = "Bold",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Italic
                IconButton(
                    onClick = onFormatItalic,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.FormatItalic,
                        contentDescription = "Italic",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Link
                IconButton(
                    onClick = onFormatLink,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = "Link",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Strikethrough
                IconButton(
                    onClick = onFormatStrikethrough,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.FormatStrikethrough,
                        contentDescription = "Strikethrough",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Code
                IconButton(
                    onClick = onFormatInlineCode,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = "Code",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        actions = {
            // Quick Save
            IconButton(onClick = onQuickSave) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }

            // Undo/Redo
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
            }
        },
        modifier = modifier
    )
}
