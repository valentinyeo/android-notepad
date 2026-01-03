package com.simplenotepad.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplenotepad.ui.components.AboutDialog
import com.simplenotepad.ui.components.FindReplaceBar
import com.simplenotepad.ui.components.FontDialog
import com.simplenotepad.ui.components.GoToLineDialog
import com.simplenotepad.ui.components.RecentFilesDialog
import com.simplenotepad.ui.components.StatusBar
import com.simplenotepad.ui.components.ThemeDialog
import com.simplenotepad.ui.components.TopMenuBar
import com.simplenotepad.ui.components.UnsavedChangesDialog
import com.simplenotepad.ui.theme.ThemeMode
import com.simplenotepad.viewmodel.NotepadViewModel

@Composable
fun NotepadScreen(
    viewModel: NotepadViewModel
) {
    val editorState by viewModel.editorState.collectAsState()
    val findReplaceState by viewModel.findReplaceState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState(
        initial = com.simplenotepad.data.PreferencesRepository.AppPreferences()
    )

    var showThemeDialog by remember { mutableStateOf(false) }
    var pendingSaveAsUri by remember { mutableStateOf<Uri?>(null) }

    // File picker launchers
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.openFile(it) }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { viewModel.saveFile(it) }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopMenuBar(
                fileName = editorState.fileName,
                isModified = editorState.isModified,
                textFieldValue = editorState.textFieldValue,
                canUndo = viewModel.canUndo(),
                canRedo = viewModel.canRedo(),
                wordWrap = preferences.wordWrap,
                showStatusBar = preferences.showStatusBar,
                onNewFile = { viewModel.newFile() },
                onOpenFile = { openFileLauncher.launch(arrayOf("text/plain", "*/*")) },
                onSave = {
                    if (editorState.fileUri != null) {
                        viewModel.saveFile()
                    } else {
                        saveFileLauncher.launch(editorState.fileName + ".txt")
                    }
                },
                onSaveAs = { saveFileLauncher.launch(editorState.fileName + ".txt") },
                onShowRecentFiles = { viewModel.showRecentFiles() },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onTextChange = { viewModel.onTextChange(it) },
                onSelectAll = { viewModel.selectAll() },
                onFind = { viewModel.showFind() },
                onFindReplace = { viewModel.showFindReplace() },
                onGoTo = { viewModel.showGoToDialog() },
                onInsertDateTime = { viewModel.insertDateTime() },
                onZoomIn = { viewModel.zoomIn() },
                onZoomOut = { viewModel.zoomOut() },
                onResetZoom = { viewModel.resetZoom() },
                onToggleWordWrap = { viewModel.setWordWrap(!preferences.wordWrap) },
                onToggleStatusBar = { viewModel.setShowStatusBar(!preferences.showStatusBar) },
                onShowFontDialog = { viewModel.showFontDialog() },
                onShowThemeDialog = { showThemeDialog = true },
                onShowAbout = { viewModel.showAboutDialog() }
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = preferences.showStatusBar) {
                StatusBar(
                    editorState = editorState,
                    zoomLevel = preferences.zoomLevel
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Find/Replace bar
            AnimatedVisibility(visible = findReplaceState.isVisible) {
                FindReplaceBar(
                    state = findReplaceState,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onReplaceQueryChange = { viewModel.updateReplaceQuery(it) },
                    onFindNext = { viewModel.findNext() },
                    onFindPrevious = { viewModel.findPrevious() },
                    onReplaceCurrent = { viewModel.replaceCurrent() },
                    onReplaceAll = { viewModel.replaceAll() },
                    onToggleMatchCase = { viewModel.toggleMatchCase() },
                    onToggleWrapAround = { viewModel.toggleWrapAround() },
                    onClose = { viewModel.hideFind() }
                )
            }

            // Editor
            val effectiveFontSize = (preferences.fontSize * preferences.zoomLevel).sp

            BasicTextField(
                value = editorState.textFieldValue,
                onValueChange = { viewModel.onTextChange(it) },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = effectiveFontSize,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
                    .then(
                        if (preferences.wordWrap) {
                            Modifier.verticalScroll(scrollState)
                        } else {
                            Modifier.verticalScroll(scrollState)
                        }
                    )
                    .padding(12.dp)
            )
        }
    }

    // Dialogs
    if (uiState.showGoToDialog) {
        GoToLineDialog(
            currentLine = editorState.lineNumber,
            maxLine = editorState.lineCount,
            onGoTo = { viewModel.goToLine(it) },
            onDismiss = { viewModel.hideGoToDialog() }
        )
    }

    if (uiState.showFontDialog) {
        FontDialog(
            currentSize = preferences.fontSize,
            onFontSizeChange = { viewModel.setFontSize(it) },
            onDismiss = { viewModel.hideFontDialog() }
        )
    }

    if (uiState.showAboutDialog) {
        AboutDialog(onDismiss = { viewModel.hideAboutDialog() })
    }

    if (uiState.showUnsavedDialog) {
        UnsavedChangesDialog(
            onSave = {
                viewModel.dismissUnsavedDialog()
                if (editorState.fileUri != null) {
                    viewModel.saveFile()
                    viewModel.confirmUnsavedAction()
                } else {
                    saveFileLauncher.launch(editorState.fileName + ".txt")
                }
            },
            onDiscard = {
                viewModel.confirmUnsavedAction()
            },
            onCancel = {
                viewModel.dismissUnsavedDialog()
            }
        )
    }

    if (uiState.showRecentFiles) {
        RecentFilesDialog(
            recentFiles = preferences.recentFiles,
            onFileSelect = { uri ->
                viewModel.hideRecentFiles()
                viewModel.openFile(Uri.parse(uri))
            },
            onRemoveFile = { viewModel.removeRecentFile(it) },
            onClearAll = { viewModel.clearRecentFiles() },
            onDismiss = { viewModel.hideRecentFiles() }
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = preferences.themeMode.name,
            onThemeSelect = { viewModel.setThemeMode(ThemeMode.valueOf(it)) },
            onDismiss = { showThemeDialog = false }
        )
    }
}
