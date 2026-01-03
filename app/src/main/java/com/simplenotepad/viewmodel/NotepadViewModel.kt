package com.simplenotepad.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simplenotepad.data.PreferencesRepository
import com.simplenotepad.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EditorState(
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val fileUri: Uri? = null,
    val fileName: String = "Untitled",
    val isModified: Boolean = false,
    val lineNumber: Int = 1,
    val columnNumber: Int = 1,
    val characterCount: Int = 0,
    val lineCount: Int = 1,
    val encoding: String = "UTF-8"
)

data class FindReplaceState(
    val isVisible: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val matchCase: Boolean = false,
    val wrapAround: Boolean = true,
    val showReplace: Boolean = false,
    val matchCount: Int = 0,
    val currentMatch: Int = 0,
    val matchPositions: List<IntRange> = emptyList()
)

data class UiState(
    val showGoToDialog: Boolean = false,
    val showFontDialog: Boolean = false,
    val showAboutDialog: Boolean = false,
    val showUnsavedDialog: Boolean = false,
    val showRecentFiles: Boolean = false,
    val pendingAction: PendingAction? = null
)

enum class PendingAction {
    NEW_FILE, OPEN_FILE, EXIT
}

class NotepadViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepository = PreferencesRepository(application)

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _findReplaceState = MutableStateFlow(FindReplaceState())
    val findReplaceState: StateFlow<FindReplaceState> = _findReplaceState.asStateFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val preferences = prefsRepository.preferences

    // Undo/Redo stacks
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var lastSavedContent: String = ""

    init {
        viewModelScope.launch {
            val prefs = preferences.first()
            // Initialize with preferences
        }
    }

    fun onTextChange(newValue: TextFieldValue) {
        val oldValue = _editorState.value.textFieldValue

        // Add to undo stack if content changed
        if (oldValue.text != newValue.text) {
            undoStack.add(oldValue)
            if (undoStack.size > 100) undoStack.removeAt(0)
            redoStack.clear()
        }

        val (line, col) = calculateLineColumn(newValue.text, newValue.selection.start)

        _editorState.value = _editorState.value.copy(
            textFieldValue = newValue,
            isModified = newValue.text != lastSavedContent,
            lineNumber = line,
            columnNumber = col,
            characterCount = newValue.text.length,
            lineCount = newValue.text.count { it == '\n' } + 1
        )

        // Update find matches if search is active
        if (_findReplaceState.value.searchQuery.isNotEmpty()) {
            updateFindMatches()
        }
    }

    private fun calculateLineColumn(text: String, position: Int): Pair<Int, Int> {
        if (position <= 0) return Pair(1, 1)

        val textBefore = text.substring(0, minOf(position, text.length))
        val lines = textBefore.split("\n")
        val line = lines.size
        val col = lines.lastOrNull()?.length?.plus(1) ?: 1

        return Pair(line, col)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(_editorState.value.textFieldValue)
            val previous = undoStack.removeLast()
            _editorState.value = _editorState.value.copy(
                textFieldValue = previous,
                isModified = previous.text != lastSavedContent,
                characterCount = previous.text.length,
                lineCount = previous.text.count { it == '\n' } + 1
            )
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(_editorState.value.textFieldValue)
            val next = redoStack.removeLast()
            _editorState.value = _editorState.value.copy(
                textFieldValue = next,
                isModified = next.text != lastSavedContent,
                characterCount = next.text.length,
                lineCount = next.text.count { it == '\n' } + 1
            )
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun newFile(force: Boolean = false) {
        if (_editorState.value.isModified && !force) {
            _uiState.value = _uiState.value.copy(
                showUnsavedDialog = true,
                pendingAction = PendingAction.NEW_FILE
            )
            return
        }

        lastSavedContent = ""
        undoStack.clear()
        redoStack.clear()
        _editorState.value = EditorState()
    }

    fun openFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                    val content = reader.readText()

                    lastSavedContent = content
                    undoStack.clear()
                    redoStack.clear()

                    val fileName = getFileName(uri)
                    _editorState.value = EditorState(
                        textFieldValue = TextFieldValue(content),
                        fileUri = uri,
                        fileName = fileName,
                        characterCount = content.length,
                        lineCount = content.count { it == '\n' } + 1
                    )

                    prefsRepository.addRecentFile(uri.toString())
                }
            } catch (e: Exception) {
                // Handle error - could show a toast/snackbar
            }
        }
    }

    fun saveFile(uri: Uri? = _editorState.value.fileUri) {
        val targetUri = uri ?: return

        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openOutputStream(targetUri, "wt")?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)
                    writer.write(_editorState.value.textFieldValue.text)
                    writer.flush()

                    lastSavedContent = _editorState.value.textFieldValue.text
                    _editorState.value = _editorState.value.copy(
                        fileUri = targetUri,
                        fileName = getFileName(targetUri),
                        isModified = false
                    )

                    prefsRepository.addRecentFile(targetUri.toString())
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        val contentResolver = getApplication<Application>().contentResolver
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (nameIndex >= 0) cursor.getString(nameIndex) else "Untitled"
        } ?: uri.lastPathSegment ?: "Untitled"
    }

    // Find & Replace
    fun showFind() {
        _findReplaceState.value = _findReplaceState.value.copy(isVisible = true, showReplace = false)
    }

    fun showFindReplace() {
        _findReplaceState.value = _findReplaceState.value.copy(isVisible = true, showReplace = true)
    }

    fun hideFind() {
        _findReplaceState.value = FindReplaceState()
    }

    fun updateSearchQuery(query: String) {
        _findReplaceState.value = _findReplaceState.value.copy(searchQuery = query)
        updateFindMatches()
    }

    fun updateReplaceQuery(query: String) {
        _findReplaceState.value = _findReplaceState.value.copy(replaceQuery = query)
    }

    fun toggleMatchCase() {
        _findReplaceState.value = _findReplaceState.value.copy(
            matchCase = !_findReplaceState.value.matchCase
        )
        updateFindMatches()
    }

    fun toggleWrapAround() {
        _findReplaceState.value = _findReplaceState.value.copy(
            wrapAround = !_findReplaceState.value.wrapAround
        )
    }

    private fun updateFindMatches() {
        val query = _findReplaceState.value.searchQuery
        if (query.isEmpty()) {
            _findReplaceState.value = _findReplaceState.value.copy(
                matchCount = 0,
                currentMatch = 0,
                matchPositions = emptyList()
            )
            return
        }

        val text = _editorState.value.textFieldValue.text
        val matches = mutableListOf<IntRange>()
        val searchText = if (_findReplaceState.value.matchCase) text else text.lowercase()
        val searchQuery = if (_findReplaceState.value.matchCase) query else query.lowercase()

        var index = searchText.indexOf(searchQuery)
        while (index >= 0) {
            matches.add(index until index + query.length)
            index = searchText.indexOf(searchQuery, index + 1)
        }

        _findReplaceState.value = _findReplaceState.value.copy(
            matchCount = matches.size,
            currentMatch = if (matches.isNotEmpty()) 1 else 0,
            matchPositions = matches
        )
    }

    fun findNext() {
        val state = _findReplaceState.value
        if (state.matchPositions.isEmpty()) return

        val newCurrent = if (state.currentMatch >= state.matchCount) {
            if (state.wrapAround) 1 else state.matchCount
        } else {
            state.currentMatch + 1
        }

        _findReplaceState.value = state.copy(currentMatch = newCurrent)
        selectCurrentMatch()
    }

    fun findPrevious() {
        val state = _findReplaceState.value
        if (state.matchPositions.isEmpty()) return

        val newCurrent = if (state.currentMatch <= 1) {
            if (state.wrapAround) state.matchCount else 1
        } else {
            state.currentMatch - 1
        }

        _findReplaceState.value = state.copy(currentMatch = newCurrent)
        selectCurrentMatch()
    }

    private fun selectCurrentMatch() {
        val state = _findReplaceState.value
        if (state.currentMatch == 0 || state.matchPositions.isEmpty()) return

        val match = state.matchPositions[state.currentMatch - 1]
        _editorState.value = _editorState.value.copy(
            textFieldValue = _editorState.value.textFieldValue.copy(
                selection = TextRange(match.first, match.last + 1)
            )
        )
    }

    fun replaceCurrent() {
        val state = _findReplaceState.value
        if (state.currentMatch == 0 || state.matchPositions.isEmpty()) return

        val match = state.matchPositions[state.currentMatch - 1]
        val text = _editorState.value.textFieldValue.text
        val newText = text.substring(0, match.first) + state.replaceQuery + text.substring(match.last + 1)

        onTextChange(TextFieldValue(newText, TextRange(match.first + state.replaceQuery.length)))
        updateFindMatches()
    }

    fun replaceAll() {
        val state = _findReplaceState.value
        if (state.searchQuery.isEmpty()) return

        val text = _editorState.value.textFieldValue.text
        val newText = if (state.matchCase) {
            text.replace(state.searchQuery, state.replaceQuery)
        } else {
            text.replace(state.searchQuery, state.replaceQuery, ignoreCase = true)
        }

        onTextChange(TextFieldValue(newText))
        updateFindMatches()
    }

    // Go to line
    fun showGoToDialog() {
        _uiState.value = _uiState.value.copy(showGoToDialog = true)
    }

    fun hideGoToDialog() {
        _uiState.value = _uiState.value.copy(showGoToDialog = false)
    }

    fun goToLine(lineNumber: Int) {
        val text = _editorState.value.textFieldValue.text
        val lines = text.split("\n")
        val targetLine = lineNumber.coerceIn(1, lines.size)

        var position = 0
        for (i in 0 until targetLine - 1) {
            position += lines[i].length + 1
        }

        _editorState.value = _editorState.value.copy(
            textFieldValue = _editorState.value.textFieldValue.copy(
                selection = TextRange(position)
            ),
            lineNumber = targetLine,
            columnNumber = 1
        )
        hideGoToDialog()
    }

    // Date/Time insertion
    fun insertDateTime() {
        val dateTime = SimpleDateFormat("h:mm a M/d/yyyy", Locale.getDefault()).format(Date())
        val currentValue = _editorState.value.textFieldValue
        val selection = currentValue.selection
        val newText = currentValue.text.substring(0, selection.start) +
                dateTime +
                currentValue.text.substring(selection.end)

        onTextChange(TextFieldValue(newText, TextRange(selection.start + dateTime.length)))
    }

    // Select all
    fun selectAll() {
        val text = _editorState.value.textFieldValue.text
        _editorState.value = _editorState.value.copy(
            textFieldValue = _editorState.value.textFieldValue.copy(
                selection = TextRange(0, text.length)
            )
        )
    }

    // Dialogs
    fun showFontDialog() {
        _uiState.value = _uiState.value.copy(showFontDialog = true)
    }

    fun hideFontDialog() {
        _uiState.value = _uiState.value.copy(showFontDialog = false)
    }

    fun showAboutDialog() {
        _uiState.value = _uiState.value.copy(showAboutDialog = true)
    }

    fun hideAboutDialog() {
        _uiState.value = _uiState.value.copy(showAboutDialog = false)
    }

    fun dismissUnsavedDialog() {
        _uiState.value = _uiState.value.copy(showUnsavedDialog = false, pendingAction = null)
    }

    fun confirmUnsavedAction() {
        val action = _uiState.value.pendingAction
        _uiState.value = _uiState.value.copy(showUnsavedDialog = false, pendingAction = null)
        when (action) {
            PendingAction.NEW_FILE -> newFile(force = true)
            PendingAction.OPEN_FILE -> { /* Handled by caller */ }
            PendingAction.EXIT -> { /* Handled by caller */ }
            null -> {}
        }
    }

    fun showRecentFiles() {
        _uiState.value = _uiState.value.copy(showRecentFiles = true)
    }

    fun hideRecentFiles() {
        _uiState.value = _uiState.value.copy(showRecentFiles = false)
    }

    // Preferences
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepository.setThemeMode(mode) }
    }

    fun setWordWrap(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setWordWrap(enabled) }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { prefsRepository.setFontSize(size) }
    }

    fun setShowStatusBar(show: Boolean) {
        viewModelScope.launch { prefsRepository.setShowStatusBar(show) }
    }

    fun zoomIn() {
        viewModelScope.launch {
            val current = preferences.first().zoomLevel
            prefsRepository.setZoomLevel(current + 0.1f)
        }
    }

    fun zoomOut() {
        viewModelScope.launch {
            val current = preferences.first().zoomLevel
            prefsRepository.setZoomLevel(current - 0.1f)
        }
    }

    fun resetZoom() {
        viewModelScope.launch { prefsRepository.setZoomLevel(1f) }
    }

    fun clearRecentFiles() {
        viewModelScope.launch { prefsRepository.clearRecentFiles() }
    }

    fun removeRecentFile(uri: String) {
        viewModelScope.launch { prefsRepository.removeRecentFile(uri) }
    }
}
