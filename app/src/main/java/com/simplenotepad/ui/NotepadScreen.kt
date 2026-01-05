package com.simplenotepad.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.simplenotepad.ui.components.AboutDialog
import com.simplenotepad.ui.components.CloseTabDialog
import com.simplenotepad.ui.components.FindReplaceBar
import com.simplenotepad.ui.components.FontDialog
import com.simplenotepad.ui.components.FormattingToolbar
import com.simplenotepad.ui.components.GoToLineDialog
import com.simplenotepad.ui.components.MainMenuBottomSheet
import com.simplenotepad.ui.components.MarkdownEditor
import com.simplenotepad.ui.components.NotesExplorer
import com.simplenotepad.ui.components.RecentFilesDialog
import com.simplenotepad.ui.components.StatusBar
import com.simplenotepad.ui.components.TabBar
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
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val folderFiles by viewModel.folderFiles.collectAsState()
    val preferences by viewModel.preferences.collectAsState(
        initial = com.simplenotepad.data.PreferencesRepository.AppPreferences()
    )

    var showThemeDialog by remember { mutableStateOf(false) }
    var showMainMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Share function
    fun shareNote() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, editorState.fileName)
            putExtra(Intent.EXTRA_TEXT, editorState.textFieldValue.text)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
    }

    // Clipboard functions
    fun cutText() {
        val selection = editorState.textFieldValue.selection
        if (!selection.collapsed) {
            clipboardManager.setText(AnnotatedString(editorState.textFieldValue.text.substring(selection.min, selection.max)))
            val newText = editorState.textFieldValue.text.removeRange(selection.min, selection.max)
            viewModel.onTextChange(TextFieldValue(newText, TextRange(selection.min)))
        }
    }

    fun copyText() {
        val selection = editorState.textFieldValue.selection
        if (!selection.collapsed) {
            clipboardManager.setText(AnnotatedString(editorState.textFieldValue.text.substring(selection.min, selection.max)))
        }
    }

    fun pasteText() {
        clipboardManager.getText()?.let { paste ->
            val selection = editorState.textFieldValue.selection
            val newText = editorState.textFieldValue.text.replaceRange(selection.min, selection.max, paste.text)
            viewModel.onTextChange(TextFieldValue(newText, TextRange(selection.min + paste.text.length)))
        }
    }

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

    val saveMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let { viewModel.saveFile(it) }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.setNotesFolder(it) }
    }

    // Show Notes Explorer as full screen when active
    if (uiState.showNotesExplorer) {
        NotesExplorer(
            tabs = tabs,
            activeTabId = activeTabId,
            folderFiles = folderFiles,
            notesFolderUri = preferences.notesFolderUri,
            onNoteClick = { viewModel.openNoteFromExplorer(it) },
            onNoteDelete = { viewModel.deleteNote(it) },
            onFolderFileClick = { viewModel.openFileFromFolder(it) },
            onSelectFolder = { folderPickerLauncher.launch(null) },
            onClearFolder = { viewModel.clearNotesFolder() },
            onRefreshFolder = { viewModel.refreshFolderFiles() },
            onBack = { viewModel.hideNotesExplorer() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopMenuBar(
                title = editorState.fileName,
                canUndo = viewModel.canUndo(),
                canRedo = viewModel.canRedo(),
                onShowMenu = { showMainMenu = true },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onQuickSave = {
                    if (editorState.fileUri != null) {
                        viewModel.saveFile()
                    } else {
                        saveFileLauncher.launch(editorState.fileName + ".txt")
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = preferences.showStatusBar) {
                StatusBar(
                    editorState = editorState,
                    zoomLevel = preferences.zoomLevel,
                    formattedView = preferences.formattedView,
                    onToggleFormattedView = { viewModel.toggleFormattedView() },
                    modifier = Modifier.imePadding()
                )
            }
        },
        modifier = Modifier.imePadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab bar at top (if not at bottom)
            if (!preferences.tabsAtBottom) {
                TabBar(
                    tabs = tabs,
                    activeTabId = activeTabId,
                    onTabSelect = { viewModel.switchTab(it) },
                    onTabClose = { viewModel.closeTab(it) },
                    onNewTab = { viewModel.addTab() }
                )
            }

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

            MarkdownEditor(
                value = editorState.textFieldValue,
                onValueChange = { newValue ->
                    viewModel.onTextChange(newValue)
                },
                fontSize = effectiveFontSize,
                formattedView = preferences.formattedView,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Tab) {
                            // Insert tab character at cursor position
                            val currentValue = editorState.textFieldValue
                            val selection = currentValue.selection
                            val tabChar = "\t"
                            val newText = currentValue.text.substring(0, selection.min) +
                                    tabChar +
                                    currentValue.text.substring(selection.max)
                            viewModel.onTextChange(
                                TextFieldValue(newText, TextRange(selection.min + 1))
                            )
                            true // Consume the event
                        } else {
                            false // Let other keys pass through
                        }
                    }
            )

            // Tab bar at bottom (if enabled)
            if (preferences.tabsAtBottom) {
                TabBar(
                    tabs = tabs,
                    activeTabId = activeTabId,
                    onTabSelect = { viewModel.switchTab(it) },
                    onTabClose = { viewModel.closeTab(it) },
                    onNewTab = { viewModel.addTab() }
                )
            }

            // Formatting toolbar at bottom
            FormattingToolbar(
                onFormatBold = { viewModel.formatBold() },
                onFormatItalic = { viewModel.formatItalic() },
                onFormatStrikethrough = { viewModel.formatStrikethrough() },
                onFormatInlineCode = { viewModel.formatInlineCode() },
                onFormatHeader = { level -> viewModel.formatHeader(level) },
                onFormatBulletList = { viewModel.formatBulletList() },
                onFormatNumberedList = { viewModel.formatNumberedList() },
                onFormatLink = { viewModel.formatLink() }
            )
        }
    }

    // Main Menu Bottom Sheet
    if (showMainMenu) {
        MainMenuBottomSheet(
            onDismiss = { showMainMenu = false },
            // File operations
            onNewFile = { viewModel.newFile() },
            onOpenFile = { openFileLauncher.launch(arrayOf("text/plain", "text/markdown", "*/*")) },
            onSave = {
                if (editorState.fileUri != null) {
                    viewModel.saveFile()
                } else {
                    saveFileLauncher.launch(editorState.fileName + ".txt")
                }
            },
            onSaveAs = { saveFileLauncher.launch(editorState.fileName + ".txt") },
            onSaveAsMarkdown = {
                val baseName = editorState.fileName.removeSuffix(".txt").removeSuffix(".md")
                saveMarkdownLauncher.launch("$baseName.md")
            },
            onShowRecentFiles = { viewModel.showRecentFiles() },
            onShowNotesExplorer = { viewModel.showNotesExplorer() },
            onShare = { shareNote() },
            // Edit operations
            canUndo = viewModel.canUndo(),
            canRedo = viewModel.canRedo(),
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onCut = { cutText() },
            onCopy = { copyText() },
            onPaste = { pasteText() },
            onSelectAll = { viewModel.selectAll() },
            onFind = { viewModel.showFind() },
            onFindReplace = { viewModel.showFindReplace() },
            onGoToLine = { viewModel.showGoToDialog() },
            onInsertDateTime = { viewModel.insertDateTime() },
            // View options
            wordWrap = preferences.wordWrap,
            showStatusBar = preferences.showStatusBar,
            autoSave = preferences.autoSave,
            formattedView = preferences.formattedView,
            tabsAtBottom = preferences.tabsAtBottom,
            onToggleWordWrap = { viewModel.setWordWrap(!preferences.wordWrap) },
            onToggleStatusBar = { viewModel.setShowStatusBar(!preferences.showStatusBar) },
            onToggleAutoSave = { viewModel.setAutoSave(!preferences.autoSave) },
            onToggleFormattedView = { viewModel.toggleFormattedView() },
            onToggleTabsPosition = { viewModel.toggleTabsPosition() },
            onZoomIn = { viewModel.zoomIn() },
            onZoomOut = { viewModel.zoomOut() },
            onResetZoom = { viewModel.resetZoom() },
            onShowFontDialog = { viewModel.showFontDialog() },
            onShowThemeDialog = { showThemeDialog = true },
            onShowAbout = { viewModel.showAboutDialog() }
        )
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

    if (uiState.showCloseTabDialog) {
        CloseTabDialog(
            onSave = {
                viewModel.dismissCloseTabDialog()
                // Save then close
                if (editorState.fileUri != null) {
                    viewModel.saveFile()
                    viewModel.confirmCloseTab()
                } else {
                    // Need to save as first - for now just close without saving
                    viewModel.confirmCloseTab()
                }
            },
            onDiscard = {
                viewModel.confirmCloseTab()
            },
            onCancel = {
                viewModel.dismissCloseTabDialog()
            }
        )
    }
}
