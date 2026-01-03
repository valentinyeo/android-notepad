package com.simplenotepad.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow

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
    var showMainMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = {
            Box {
                IconButton(onClick = { showMainMenu = true }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }

                // Main Menu (File + Edit operations)
                DropdownMenu(
                    expanded = showMainMenu,
                    onDismissRequest = { showMainMenu = false }
                ) {
                    // File operations
                    DropdownMenuItem(text = { Text("New") }, onClick = { showMainMenu = false; onNewFile() })
                    DropdownMenuItem(text = { Text("Open...") }, onClick = { showMainMenu = false; onOpenFile() })
                    DropdownMenuItem(text = { Text("Save") }, onClick = { showMainMenu = false; onSave() })
                    DropdownMenuItem(text = { Text("Save As...") }, onClick = { showMainMenu = false; onSaveAs() })
                    DropdownMenuItem(text = { Text("Recent Files") }, onClick = { showMainMenu = false; onShowRecentFiles() })
                    HorizontalDivider()
                    // Edit operations
                    DropdownMenuItem(
                        text = { Text("Cut") },
                        onClick = {
                            showMainMenu = false
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
                            showMainMenu = false
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
                            showMainMenu = false
                            clipboardManager.getText()?.let { paste ->
                                val selection = textFieldValue.selection
                                val newText = textFieldValue.text.replaceRange(selection.min, selection.max, paste.text)
                                onTextChange(TextFieldValue(newText, androidx.compose.ui.text.TextRange(selection.min + paste.text.length)))
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.ContentPaste, null) }
                    )
                    DropdownMenuItem(text = { Text("Select All") }, onClick = { showMainMenu = false; onSelectAll() })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Find...") }, onClick = { showMainMenu = false; onFind() })
                    DropdownMenuItem(text = { Text("Replace...") }, onClick = { showMainMenu = false; onFindReplace() })
                    DropdownMenuItem(text = { Text("Go To Line...") }, onClick = { showMainMenu = false; onGoTo() })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Time/Date") }, onClick = { showMainMenu = false; onInsertDateTime() })
                }
            }
        },
        title = {
            Text(
                text = (if (isModified) "• " else "") + fileName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

            // More menu (View + Help)
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    // View options
                    DropdownMenuItem(text = { Text("Zoom In") }, onClick = { showMoreMenu = false; onZoomIn() })
                    DropdownMenuItem(text = { Text("Zoom Out") }, onClick = { showMoreMenu = false; onZoomOut() })
                    DropdownMenuItem(text = { Text("Reset Zoom") }, onClick = { showMoreMenu = false; onResetZoom() })
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (wordWrap) "✓ Word Wrap" else "   Word Wrap") },
                        onClick = { showMoreMenu = false; onToggleWordWrap() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (showStatusBar) "✓ Status Bar" else "   Status Bar") },
                        onClick = { showMoreMenu = false; onToggleStatusBar() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Font...") }, onClick = { showMoreMenu = false; onShowFontDialog() })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("About") }, onClick = { showMoreMenu = false; onShowAbout() })
                }
            }
        },
        modifier = modifier
    )
}
