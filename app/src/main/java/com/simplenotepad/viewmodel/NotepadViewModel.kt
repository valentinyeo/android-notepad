package com.simplenotepad.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simplenotepad.data.PreferencesRepository
import com.simplenotepad.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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

data class TabState(
    val id: String = UUID.randomUUID().toString(),
    val editorState: EditorState = EditorState(),
    val undoStack: MutableList<TextFieldValue> = mutableListOf(),
    val redoStack: MutableList<TextFieldValue> = mutableListOf(),
    val lastSavedContent: String = ""
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
    val showCloseTabDialog: Boolean = false,
    val showRecentFiles: Boolean = false,
    val showNotesExplorer: Boolean = false,
    val pendingAction: PendingAction? = null,
    val pendingCloseTabId: String? = null
)

enum class PendingAction {
    NEW_FILE, OPEN_FILE, EXIT
}

data class FolderFile(
    val uri: Uri,
    val name: String,
    val lastModified: Long = 0,
    val size: Long = 0
)

class NotepadViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepository = PreferencesRepository(application)

    // Tab state management
    private val initialTab = TabState()
    private val _tabs = MutableStateFlow<List<TabState>>(listOf(initialTab))
    val tabs: StateFlow<List<TabState>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    // Derived active tab state for UI
    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _findReplaceState = MutableStateFlow(FindReplaceState())
    val findReplaceState: StateFlow<FindReplaceState> = _findReplaceState.asStateFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val preferences = prefsRepository.preferences

    // Folder files
    private val _folderFiles = MutableStateFlow<List<FolderFile>>(emptyList())
    val folderFiles: StateFlow<List<FolderFile>> = _folderFiles.asStateFlow()

    // Auto-save
    private var autoSaveJob: Job? = null
    private val autoSaveIntervalMs = 30_000L // 30 seconds

    // Helper to get current active tab
    private fun getActiveTab(): TabState? = _tabs.value.find { it.id == _activeTabId.value }

    // Helper to update active tab
    private fun updateActiveTab(update: (TabState) -> TabState) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == _activeTabId.value) update(tab) else tab
        }
        // Sync editorState for UI
        getActiveTab()?.let { _editorState.value = it.editorState }
    }

    init {
        // Restore session or use initial tab
        viewModelScope.launch {
            restoreSession()
        }

        // Start auto-save monitoring
        viewModelScope.launch {
            preferences.collect { prefs ->
                if (prefs.autoSave) {
                    startAutoSave()
                } else {
                    stopAutoSave()
                }
            }
        }
    }

    private suspend fun restoreSession() {
        try {
            val (sessionJson, activeTabId) = prefsRepository.getSession()
            if (sessionJson != null && sessionJson.isNotEmpty()) {
                val tabs = deserializeTabs(sessionJson)
                if (tabs.isNotEmpty()) {
                    _tabs.value = tabs
                    _activeTabId.value = activeTabId ?: tabs.first().id
                    getActiveTab()?.let { _editorState.value = it.editorState }
                    return
                }
            }
        } catch (e: Exception) {
            // If restore fails, start fresh
        }
        // Default: sync initial editor state
        _editorState.value = initialTab.editorState
    }

    fun saveSession() {
        viewModelScope.launch {
            try {
                val sessionJson = serializeTabs(_tabs.value)
                prefsRepository.saveSession(sessionJson, _activeTabId.value)
            } catch (e: Exception) {
                // Ignore save errors
            }
        }
    }

    private fun serializeTabs(tabs: List<TabState>): String {
        val jsonArray = JSONArray()
        tabs.forEach { tab ->
            val jsonObj = JSONObject().apply {
                put("id", tab.id)
                put("fileName", tab.editorState.fileName)
                put("fileUri", tab.editorState.fileUri?.toString() ?: "")
                put("content", tab.editorState.textFieldValue.text)
                put("isModified", tab.editorState.isModified)
                put("lastSavedContent", tab.lastSavedContent)
                put("cursorPosition", tab.editorState.textFieldValue.selection.start)
            }
            jsonArray.put(jsonObj)
        }
        return jsonArray.toString()
    }

    private fun deserializeTabs(json: String): List<TabState> {
        val tabs = mutableListOf<TabState>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val jsonObj = jsonArray.getJSONObject(i)
            val content = jsonObj.getString("content")
            val cursorPos = jsonObj.optInt("cursorPosition", 0).coerceIn(0, content.length)
            val fileUriStr = jsonObj.getString("fileUri")
            val lastSavedContent = jsonObj.optString("lastSavedContent", "")

            val tab = TabState(
                id = jsonObj.getString("id"),
                editorState = EditorState(
                    textFieldValue = TextFieldValue(content, TextRange(cursorPos)),
                    fileName = jsonObj.getString("fileName"),
                    fileUri = if (fileUriStr.isNotEmpty()) Uri.parse(fileUriStr) else null,
                    isModified = jsonObj.getBoolean("isModified"),
                    characterCount = content.length,
                    lineCount = content.count { it == '\n' } + 1
                ),
                lastSavedContent = lastSavedContent
            )
            tabs.add(tab)
        }
        return tabs
    }

    private fun startAutoSave() {
        if (autoSaveJob?.isActive == true) return
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(autoSaveIntervalMs)
                // Save all modified tabs that have a file URI
                _tabs.value.forEach { tab ->
                    if (tab.editorState.isModified && tab.editorState.fileUri != null) {
                        saveTabFile(tab.id)
                    }
                }
            }
        }
    }

    private fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }

    // Tab management
    fun addTab() {
        val newTab = TabState()
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        _editorState.value = newTab.editorState
        _findReplaceState.value = FindReplaceState() // Reset find state for new tab
    }

    fun switchTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
            getActiveTab()?.let { _editorState.value = it.editorState }
            _findReplaceState.value = FindReplaceState() // Reset find state when switching
        }
    }

    fun closeTab(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return

        if (tab.editorState.isModified) {
            // Show unsaved dialog for this tab
            _uiState.value = _uiState.value.copy(
                showCloseTabDialog = true,
                pendingCloseTabId = tabId
            )
            return
        }

        performCloseTab(tabId)
    }

    fun confirmCloseTab() {
        val tabId = _uiState.value.pendingCloseTabId
        _uiState.value = _uiState.value.copy(showCloseTabDialog = false, pendingCloseTabId = null)
        tabId?.let { performCloseTab(it) }
    }

    fun dismissCloseTabDialog() {
        _uiState.value = _uiState.value.copy(showCloseTabDialog = false, pendingCloseTabId = null)
    }

    private fun performCloseTab(tabId: String) {
        val currentTabs = _tabs.value
        if (currentTabs.size == 1) {
            // Last tab - create a new empty one
            val newTab = TabState()
            _tabs.value = listOf(newTab)
            _activeTabId.value = newTab.id
            _editorState.value = newTab.editorState
        } else {
            // Remove tab and switch to adjacent one if needed
            val tabIndex = currentTabs.indexOfFirst { it.id == tabId }
            _tabs.value = currentTabs.filter { it.id != tabId }

            if (_activeTabId.value == tabId) {
                // Switch to previous or next tab
                val newIndex = (tabIndex - 1).coerceAtLeast(0)
                val newActiveTab = _tabs.value[newIndex]
                _activeTabId.value = newActiveTab.id
                _editorState.value = newActiveTab.editorState
            }
        }
        _findReplaceState.value = FindReplaceState()
    }

    fun onTextChange(newValue: TextFieldValue) {
        val activeTab = getActiveTab() ?: return
        val oldValue = activeTab.editorState.textFieldValue

        // Add to undo stack if content changed
        if (oldValue.text != newValue.text) {
            activeTab.undoStack.add(oldValue)
            if (activeTab.undoStack.size > 100) activeTab.undoStack.removeAt(0)
            activeTab.redoStack.clear()
        }

        // Check for list auto-continuation (when Enter is pressed after a list item)
        val processedValue = processListContinuation(oldValue, newValue)

        val (line, col) = calculateLineColumn(processedValue.text, processedValue.selection.start)

        updateActiveTab { tab ->
            tab.copy(
                editorState = tab.editorState.copy(
                    textFieldValue = processedValue,
                    isModified = processedValue.text != tab.lastSavedContent,
                    lineNumber = line,
                    columnNumber = col,
                    characterCount = processedValue.text.length,
                    lineCount = processedValue.text.count { it == '\n' } + 1
                )
            )
        }

        // Update find matches if search is active
        if (_findReplaceState.value.searchQuery.isNotEmpty()) {
            updateFindMatches()
        }
    }

    private fun processListContinuation(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
        // Only process if a single newline was just inserted
        if (newValue.text.length != oldValue.text.length + 1) return newValue

        val cursorPos = newValue.selection.start
        if (cursorPos == 0) return newValue

        // Check if a newline was just typed
        if (newValue.text.getOrNull(cursorPos - 1) != '\n') return newValue

        // Find the previous line
        val textBeforeNewline = newValue.text.substring(0, cursorPos - 1)
        val prevLineStart = textBeforeNewline.lastIndexOf('\n') + 1
        val prevLine = textBeforeNewline.substring(prevLineStart)

        // Check for bullet list patterns: "- ", "* ", "+ "
        val bulletMatch = Regex("^(\\s*)([-*+])\\s").find(prevLine)
        if (bulletMatch != null) {
            val indent = bulletMatch.groupValues[1]
            val bullet = bulletMatch.groupValues[2]
            // If previous line was just the bullet with no content, remove it
            if (prevLine.trim() == bullet) {
                val newText = newValue.text.substring(0, prevLineStart) + newValue.text.substring(cursorPos)
                return TextFieldValue(newText, TextRange(prevLineStart))
            }
            val continuation = "$indent$bullet "
            val newText = newValue.text.substring(0, cursorPos) + continuation + newValue.text.substring(cursorPos)
            return TextFieldValue(newText, TextRange(cursorPos + continuation.length))
        }

        // Check for numbered list pattern: "1. ", "2. ", etc.
        val numberMatch = Regex("^(\\s*)(\\d+)\\.\\s").find(prevLine)
        if (numberMatch != null) {
            val indent = numberMatch.groupValues[1]
            val number = numberMatch.groupValues[2].toIntOrNull() ?: return newValue
            // If previous line was just the number with no content, remove it
            if (prevLine.trim() == "$number.") {
                val newText = newValue.text.substring(0, prevLineStart) + newValue.text.substring(cursorPos)
                return TextFieldValue(newText, TextRange(prevLineStart))
            }
            val continuation = "$indent${number + 1}. "
            val newText = newValue.text.substring(0, cursorPos) + continuation + newValue.text.substring(cursorPos)
            return TextFieldValue(newText, TextRange(cursorPos + continuation.length))
        }

        // Check for blockquote pattern: "> "
        val quoteMatch = Regex("^(\\s*>\\s*)").find(prevLine)
        if (quoteMatch != null) {
            val quotePrefix = quoteMatch.groupValues[1]
            // If previous line was just ">", remove it
            if (prevLine.trim() == ">") {
                val newText = newValue.text.substring(0, prevLineStart) + newValue.text.substring(cursorPos)
                return TextFieldValue(newText, TextRange(prevLineStart))
            }
            val newText = newValue.text.substring(0, cursorPos) + quotePrefix + newValue.text.substring(cursorPos)
            return TextFieldValue(newText, TextRange(cursorPos + quotePrefix.length))
        }

        return newValue
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
        val activeTab = getActiveTab() ?: return
        if (activeTab.undoStack.isNotEmpty()) {
            activeTab.redoStack.add(activeTab.editorState.textFieldValue)
            val previous = activeTab.undoStack.removeLast()
            updateActiveTab { tab ->
                tab.copy(
                    editorState = tab.editorState.copy(
                        textFieldValue = previous,
                        isModified = previous.text != tab.lastSavedContent,
                        characterCount = previous.text.length,
                        lineCount = previous.text.count { it == '\n' } + 1
                    )
                )
            }
        }
    }

    fun redo() {
        val activeTab = getActiveTab() ?: return
        if (activeTab.redoStack.isNotEmpty()) {
            activeTab.undoStack.add(activeTab.editorState.textFieldValue)
            val next = activeTab.redoStack.removeLast()
            updateActiveTab { tab ->
                tab.copy(
                    editorState = tab.editorState.copy(
                        textFieldValue = next,
                        isModified = next.text != tab.lastSavedContent,
                        characterCount = next.text.length,
                        lineCount = next.text.count { it == '\n' } + 1
                    )
                )
            }
        }
    }

    fun canUndo(): Boolean = getActiveTab()?.undoStack?.isNotEmpty() == true
    fun canRedo(): Boolean = getActiveTab()?.redoStack?.isNotEmpty() == true

    fun newFile(force: Boolean = false) {
        // Always create a new tab
        addTab()
    }

    fun openFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                    val content = reader.readText()
                    val fileName = getFileName(uri)

                    // Create new tab with file content
                    val newTab = TabState(
                        editorState = EditorState(
                            textFieldValue = TextFieldValue(content),
                            fileUri = uri,
                            fileName = fileName,
                            characterCount = content.length,
                            lineCount = content.count { it == '\n' } + 1
                        ),
                        lastSavedContent = content
                    )

                    _tabs.value = _tabs.value + newTab
                    _activeTabId.value = newTab.id
                    _editorState.value = newTab.editorState
                    _findReplaceState.value = FindReplaceState()

                    prefsRepository.addRecentFile(uri.toString())
                }
            } catch (e: Exception) {
                // Handle error - could show a toast/snackbar
            }
        }
    }

    fun saveFile(uri: Uri? = null) {
        val activeTab = getActiveTab() ?: return
        val targetUri = uri ?: activeTab.editorState.fileUri ?: return
        saveTabFile(activeTab.id, targetUri)
    }

    private fun saveTabFile(tabId: String, uri: Uri? = null) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        val targetUri = uri ?: tab.editorState.fileUri ?: return

        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openOutputStream(targetUri, "wt")?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)
                    writer.write(tab.editorState.textFieldValue.text)
                    writer.flush()

                    val fileName = getFileName(targetUri)
                    val savedContent = tab.editorState.textFieldValue.text

                    _tabs.value = _tabs.value.map { t ->
                        if (t.id == tabId) {
                            t.copy(
                                editorState = t.editorState.copy(
                                    fileUri = targetUri,
                                    fileName = fileName,
                                    isModified = false
                                ),
                                lastSavedContent = savedContent
                            )
                        } else t
                    }

                    // Sync editorState if this is active tab
                    if (tabId == _activeTabId.value) {
                        getActiveTab()?.let { _editorState.value = it.editorState }
                    }

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
        updateActiveTab { tab ->
            tab.copy(
                editorState = tab.editorState.copy(
                    textFieldValue = tab.editorState.textFieldValue.copy(
                        selection = TextRange(match.first, match.last + 1)
                    )
                )
            )
        }
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

        updateActiveTab { tab ->
            tab.copy(
                editorState = tab.editorState.copy(
                    textFieldValue = tab.editorState.textFieldValue.copy(
                        selection = TextRange(position)
                    ),
                    lineNumber = targetLine,
                    columnNumber = 1
                )
            )
        }
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

    // Markdown formatting helpers
    fun formatBold() {
        wrapSelection("**", "**")
    }

    fun formatItalic() {
        wrapSelection("*", "*")
    }

    fun formatStrikethrough() {
        wrapSelection("~~", "~~")
    }

    fun formatInlineCode() {
        wrapSelection("`", "`")
    }

    fun formatCodeBlock() {
        val currentValue = _editorState.value.textFieldValue
        val selection = currentValue.selection
        val selectedText = if (selection.collapsed) "" else currentValue.text.substring(selection.min, selection.max)

        val newText = currentValue.text.substring(0, selection.min) +
                "```\n$selectedText\n```" +
                currentValue.text.substring(selection.max)

        val newCursorPos = selection.min + 4 // After ```\n
        onTextChange(TextFieldValue(newText, TextRange(newCursorPos)))
    }

    fun formatHeader(level: Int) {
        val prefix = "#".repeat(level.coerceIn(1, 6)) + " "
        prefixLine(prefix)
    }

    fun formatBulletList() {
        prefixSelectedLines("- ")
    }

    fun formatNumberedList() {
        val currentValue = _editorState.value.textFieldValue
        val selection = currentValue.selection
        val text = currentValue.text

        // Find start and end of selected lines
        val lineStart = text.lastIndexOf('\n', selection.min - 1).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', selection.max).let { if (it < 0) text.length else it }

        val selectedLines = text.substring(lineStart, lineEnd).split('\n')
        val numberedLines = selectedLines.mapIndexed { index, line ->
            "${index + 1}. $line"
        }.joinToString("\n")

        val newText = text.substring(0, lineStart) + numberedLines + text.substring(lineEnd)
        // Place cursor after "1. " on the first line (ready to type)
        val cursorPos = lineStart + "1. ".length + selectedLines.first().length
        onTextChange(TextFieldValue(newText, TextRange(cursorPos)))
    }

    fun formatBlockquote() {
        prefixSelectedLines("> ")
    }

    fun formatLink() {
        val currentValue = _editorState.value.textFieldValue
        val selection = currentValue.selection
        val selectedText = if (selection.collapsed) "link text" else currentValue.text.substring(selection.min, selection.max)

        val linkMarkdown = "[$selectedText](url)"
        val newText = currentValue.text.substring(0, selection.min) + linkMarkdown + currentValue.text.substring(selection.max)

        // Position cursor at "url" for easy replacement
        val urlStart = selection.min + selectedText.length + 3
        val urlEnd = urlStart + 3
        onTextChange(TextFieldValue(newText, TextRange(urlStart, urlEnd)))
    }

    fun formatImage() {
        val currentValue = _editorState.value.textFieldValue
        val selection = currentValue.selection
        val altText = if (selection.collapsed) "alt text" else currentValue.text.substring(selection.min, selection.max)

        val imageMarkdown = "![$altText](image-url)"
        val newText = currentValue.text.substring(0, selection.min) + imageMarkdown + currentValue.text.substring(selection.max)

        // Position cursor at "image-url"
        val urlStart = selection.min + altText.length + 4
        val urlEnd = urlStart + 9
        onTextChange(TextFieldValue(newText, TextRange(urlStart, urlEnd)))
    }

    fun formatHorizontalRule() {
        val currentValue = _editorState.value.textFieldValue
        val selection = currentValue.selection
        val text = currentValue.text

        // Insert on new line
        val insertText = if (selection.start > 0 && text[selection.start - 1] != '\n') "\n---\n" else "---\n"
        val newText = text.substring(0, selection.start) + insertText + text.substring(selection.end)

        onTextChange(TextFieldValue(newText, TextRange(selection.start + insertText.length)))
    }

    private fun wrapSelection(prefix: String, suffix: String) {
        val currentValue = _editorState.value.textFieldValue
        val selection = currentValue.selection
        val text = currentValue.text

        if (selection.collapsed) {
            // No selection - just insert markers and place cursor between them
            val newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.end)
            onTextChange(TextFieldValue(newText, TextRange(selection.start + prefix.length)))
        } else {
            // Wrap selected text
            val selectedText = text.substring(selection.min, selection.max)
            val newText = text.substring(0, selection.min) + prefix + selectedText + suffix + text.substring(selection.max)
            onTextChange(TextFieldValue(newText, TextRange(selection.min + prefix.length, selection.max + prefix.length)))
        }
    }

    private fun prefixLine(prefix: String) {
        val currentValue = _editorState.value.textFieldValue
        val selection = currentValue.selection
        val text = currentValue.text

        // Find start of current line
        val lineStart = text.lastIndexOf('\n', selection.min - 1).let { if (it < 0) 0 else it + 1 }

        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        onTextChange(TextFieldValue(newText, TextRange(selection.start + prefix.length)))
    }

    private fun prefixSelectedLines(prefix: String) {
        val currentValue = _editorState.value.textFieldValue
        val selection = currentValue.selection
        val text = currentValue.text

        // Find start and end of selected lines
        val lineStart = text.lastIndexOf('\n', selection.min - 1).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', selection.max).let { if (it < 0) text.length else it }

        val selectedLines = text.substring(lineStart, lineEnd).split('\n')
        val prefixedLines = selectedLines.joinToString("\n") { "$prefix$it" }

        val newText = text.substring(0, lineStart) + prefixedLines + text.substring(lineEnd)
        // Place cursor after the prefix on the first line (ready to type)
        val cursorPos = lineStart + prefix.length + selectedLines.first().length
        onTextChange(TextFieldValue(newText, TextRange(cursorPos)))
    }

    // Select all
    fun selectAll() {
        val text = _editorState.value.textFieldValue.text
        updateActiveTab { tab ->
            tab.copy(
                editorState = tab.editorState.copy(
                    textFieldValue = tab.editorState.textFieldValue.copy(
                        selection = TextRange(0, text.length)
                    )
                )
            )
        }
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

    // Notes Explorer
    fun showNotesExplorer() {
        _uiState.value = _uiState.value.copy(showNotesExplorer = true)
    }

    fun hideNotesExplorer() {
        _uiState.value = _uiState.value.copy(showNotesExplorer = false)
    }

    fun openNoteFromExplorer(tabId: String) {
        // If it's an existing tab, just switch to it
        if (_tabs.value.any { it.id == tabId }) {
            switchTab(tabId)
            hideNotesExplorer()
        }
    }

    fun deleteNote(tabId: String) {
        // Close the tab without prompting
        performCloseTab(tabId)
    }

    fun setNotesFolder(uri: Uri) {
        viewModelScope.launch {
            // Take persistent permission
            try {
                getApplication<Application>().contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission might already be taken
            }
            prefsRepository.setNotesFolderUri(uri.toString())
            refreshFolderFiles()
        }
    }

    fun clearNotesFolder() {
        viewModelScope.launch {
            prefsRepository.setNotesFolderUri(null)
            _folderFiles.value = emptyList()
        }
    }

    fun refreshFolderFiles() {
        viewModelScope.launch {
            val folderUriString = preferences.first().notesFolderUri ?: return@launch
            val folderUri = Uri.parse(folderUriString)

            try {
                val files = mutableListOf<FolderFile>()
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    folderUri,
                    DocumentsContract.getTreeDocumentId(folderUri)
                )

                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE
                )

                getApplication<Application>().contentResolver.query(
                    childrenUri,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    val sizeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idColumn)
                        val name = cursor.getString(nameColumn)
                        val mimeType = cursor.getString(mimeColumn) ?: ""
                        val lastModified = cursor.getLong(modifiedColumn)
                        val size = cursor.getLong(sizeColumn)

                        // Filter for text files (.txt, .md, text/*)
                        if (name.endsWith(".txt", ignoreCase = true) ||
                            name.endsWith(".md", ignoreCase = true) ||
                            mimeType.startsWith("text/")) {

                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)
                            files.add(FolderFile(fileUri, name, lastModified, size))
                        }
                    }
                }

                _folderFiles.value = files.sortedByDescending { it.lastModified }
            } catch (e: Exception) {
                // Handle error - folder might no longer be accessible
                _folderFiles.value = emptyList()
            }
        }
    }

    fun openFileFromFolder(file: FolderFile) {
        openFile(file.uri)
        hideNotesExplorer()
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

    fun setAutoSave(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setAutoSave(enabled) }
    }

    fun setFormattedView(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setFormattedView(enabled) }
    }

    fun toggleFormattedView() {
        viewModelScope.launch {
            val current = prefsRepository.preferences.first().formattedView
            prefsRepository.setFormattedView(!current)
        }
    }

    fun clearRecentFiles() {
        viewModelScope.launch { prefsRepository.clearRecentFiles() }
    }

    fun removeRecentFile(uri: String) {
        viewModelScope.launch { prefsRepository.removeRecentFile(uri) }
    }
}
