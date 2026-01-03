package com.simplenotepad.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopMenuBar(
    fileName: String,
    isModified: Boolean,
    textFieldValue: TextFieldValue,
    canUndo: Boolean,
    canRedo: Boolean,
    wordWrap: Boolean,
    showStatusBar: Boolean,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onShowRecentFiles: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTextChange: (TextFieldValue) -> Unit,
    onSelectAll: () -> Unit,
    onFind: () -> Unit,
    onFindReplace: () -> Unit,
    onGoTo: () -> Unit,
    onInsertDateTime: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleWordWrap: () -> Unit,
    onToggleStatusBar: () -> Unit,
    onShowFontDialog: () -> Unit,
    onShowThemeDialog: () -> Unit,
    onShowAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var expandedMenu by remember { mutableStateOf<String?>(null) }

    TopAppBar(
        title = {
            Row {
                // Menu buttons
                MenuButton("File", expandedMenu == "file") { expandedMenu = if (expandedMenu == "file") null else "file" }
                MenuButton("Edit", expandedMenu == "edit") { expandedMenu = if (expandedMenu == "edit") null else "edit" }
                MenuButton("View", expandedMenu == "view") { expandedMenu = if (expandedMenu == "view") null else "view" }
                MenuButton("Help", expandedMenu == "help") { expandedMenu = if (expandedMenu == "help") null else "help" }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = (if (isModified) "• " else "") + fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        actions = {
            // Quick action icons
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
            }
            IconButton(onClick = onShowThemeDialog) {
                Icon(Icons.Default.DarkMode, contentDescription = "Theme")
            }
        },
        modifier = modifier
    )

    // File Menu
    Box {
        DropdownMenu(
            expanded = expandedMenu == "file",
            onDismissRequest = { expandedMenu = null }
        ) {
            DropdownMenuItem(text = { Text("New") }, onClick = { expandedMenu = null; onNewFile() })
            DropdownMenuItem(text = { Text("Open...") }, onClick = { expandedMenu = null; onOpenFile() })
            DropdownMenuItem(text = { Text("Save") }, onClick = { expandedMenu = null; onSave() })
            DropdownMenuItem(text = { Text("Save As...") }, onClick = { expandedMenu = null; onSaveAs() })
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Recent Files") }, onClick = { expandedMenu = null; onShowRecentFiles() })
        }
    }

    // Edit Menu
    Box {
        DropdownMenu(
            expanded = expandedMenu == "edit",
            onDismissRequest = { expandedMenu = null }
        ) {
            DropdownMenuItem(
                text = { Text("Undo") },
                onClick = { expandedMenu = null; onUndo() },
                enabled = canUndo,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, null) }
            )
            DropdownMenuItem(
                text = { Text("Redo") },
                onClick = { expandedMenu = null; onRedo() },
                enabled = canRedo,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Redo, null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Cut") },
                onClick = {
                    expandedMenu = null
                    val selection = textFieldValue.selection
                    if (!selection.collapsed) {
                        clipboardManager.setText(AnnotatedString(textFieldValue.text.substring(selection.min, selection.max)))
                        val newText = textFieldValue.text.removeRange(selection.min, selection.max)
                        onTextChange(TextFieldValue(newText, androidx.compose.ui.text.TextRange(selection.min)))
                    }
                },
                leadingIcon = { Icon(Icons.Default.ContentCut, null) }
            )
            DropdownMenuItem(
                text = { Text("Copy") },
                onClick = {
                    expandedMenu = null
                    val selection = textFieldValue.selection
                    if (!selection.collapsed) {
                        clipboardManager.setText(AnnotatedString(textFieldValue.text.substring(selection.min, selection.max)))
                    }
                },
                leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
            )
            DropdownMenuItem(
                text = { Text("Paste") },
                onClick = {
                    expandedMenu = null
                    clipboardManager.getText()?.let { paste ->
                        val selection = textFieldValue.selection
                        val newText = textFieldValue.text.replaceRange(selection.min, selection.max, paste.text)
                        onTextChange(TextFieldValue(newText, androidx.compose.ui.text.TextRange(selection.min + paste.text.length)))
                    }
                },
                leadingIcon = { Icon(Icons.Default.ContentPaste, null) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    expandedMenu = null
                    val selection = textFieldValue.selection
                    if (!selection.collapsed) {
                        val newText = textFieldValue.text.removeRange(selection.min, selection.max)
                        onTextChange(TextFieldValue(newText, androidx.compose.ui.text.TextRange(selection.min)))
                    }
                }
            )
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Find...") }, onClick = { expandedMenu = null; onFind() })
            DropdownMenuItem(text = { Text("Replace...") }, onClick = { expandedMenu = null; onFindReplace() })
            DropdownMenuItem(text = { Text("Go To Line...") }, onClick = { expandedMenu = null; onGoTo() })
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Select All") }, onClick = { expandedMenu = null; onSelectAll() })
            DropdownMenuItem(text = { Text("Time/Date") }, onClick = { expandedMenu = null; onInsertDateTime() })
        }
    }

    // View Menu
    Box {
        DropdownMenu(
            expanded = expandedMenu == "view",
            onDismissRequest = { expandedMenu = null }
        ) {
            DropdownMenuItem(text = { Text("Zoom In") }, onClick = { expandedMenu = null; onZoomIn() })
            DropdownMenuItem(text = { Text("Zoom Out") }, onClick = { expandedMenu = null; onZoomOut() })
            DropdownMenuItem(text = { Text("Restore Default Zoom") }, onClick = { expandedMenu = null; onResetZoom() })
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (wordWrap) "✓ Word Wrap" else "  Word Wrap") },
                onClick = { expandedMenu = null; onToggleWordWrap() }
            )
            DropdownMenuItem(
                text = { Text(if (showStatusBar) "✓ Status Bar" else "  Status Bar") },
                onClick = { expandedMenu = null; onToggleStatusBar() }
            )
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Font...") }, onClick = { expandedMenu = null; onShowFontDialog() })
        }
    }

    // Help Menu
    Box {
        DropdownMenu(
            expanded = expandedMenu == "help",
            onDismissRequest = { expandedMenu = null }
        ) {
            DropdownMenuItem(text = { Text("About Notepad") }, onClick = { expandedMenu = null; onShowAbout() })
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(modifier = Modifier.width(4.dp))
}
