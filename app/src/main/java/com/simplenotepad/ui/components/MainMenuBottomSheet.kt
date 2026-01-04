package com.simplenotepad.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuBottomSheet(
    onDismiss: () -> Unit,
    // File operations
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onSaveAsMarkdown: () -> Unit,
    onShowRecentFiles: () -> Unit,
    onShowNotesExplorer: () -> Unit,
    onShare: () -> Unit,
    // Edit operations
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onSelectAll: () -> Unit,
    onFind: () -> Unit,
    onFindReplace: () -> Unit,
    onGoToLine: () -> Unit,
    onInsertDateTime: () -> Unit,
    // View options
    wordWrap: Boolean,
    showStatusBar: Boolean,
    autoSave: Boolean,
    formattedView: Boolean,
    tabsAtBottom: Boolean,
    onToggleWordWrap: () -> Unit,
    onToggleStatusBar: () -> Unit,
    onToggleAutoSave: () -> Unit,
    onToggleFormattedView: () -> Unit,
    onToggleTabsPosition: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onShowFontDialog: () -> Unit,
    onShowThemeDialog: () -> Unit,
    onShowAbout: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // File Section
            SectionHeader("File")
            MenuItem(Icons.Default.Add, "New") { onDismiss(); onNewFile() }
            MenuItem(Icons.Default.FolderOpen, "Open...") { onDismiss(); onOpenFile() }
            MenuItem(Icons.Default.Save, "Save") { onDismiss(); onSave() }
            MenuItem(Icons.Default.SaveAs, "Save As...") { onDismiss(); onSaveAs() }
            MenuItem(Icons.Default.Description, "Save As Markdown...") { onDismiss(); onSaveAsMarkdown() }
            MenuItem(Icons.Default.History, "Recent Files") { onDismiss(); onShowRecentFiles() }
            MenuItem(Icons.Default.Folder, "All Notes") { onDismiss(); onShowNotesExplorer() }
            MenuItem(Icons.Default.Share, "Share") { onDismiss(); onShare() }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Edit Section
            SectionHeader("Edit")
            MenuItem(Icons.AutoMirrored.Filled.Undo, "Undo", enabled = canUndo) { onDismiss(); onUndo() }
            MenuItem(Icons.AutoMirrored.Filled.Redo, "Redo", enabled = canRedo) { onDismiss(); onRedo() }
            MenuItem(Icons.Default.ContentCut, "Cut") { onDismiss(); onCut() }
            MenuItem(Icons.Default.ContentCopy, "Copy") { onDismiss(); onCopy() }
            MenuItem(Icons.Default.ContentPaste, "Paste") { onDismiss(); onPaste() }
            MenuItem(Icons.Default.SelectAll, "Select All") { onDismiss(); onSelectAll() }
            MenuItem(Icons.Default.FindInPage, "Find...") { onDismiss(); onFind() }
            MenuItem(Icons.Default.FindReplace, "Replace...") { onDismiss(); onFindReplace() }
            MenuItem(Icons.Default.TextFields, "Go To Line...") { onDismiss(); onGoToLine() }
            MenuItem(Icons.Default.Schedule, "Insert Date/Time") { onDismiss(); onInsertDateTime() }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // View Section
            SectionHeader("View")
            ToggleMenuItem("Word Wrap", wordWrap) { onDismiss(); onToggleWordWrap() }
            ToggleMenuItem("Status Bar", showStatusBar) { onDismiss(); onToggleStatusBar() }
            ToggleMenuItem("Auto Save", autoSave) { onDismiss(); onToggleAutoSave() }
            ToggleMenuItem("Tabs at Bottom", tabsAtBottom) { onDismiss(); onToggleTabsPosition() }
            MenuItem(
                Icons.Default.Visibility,
                if (formattedView) "Switch to Markdown View" else "Switch to Formatted View"
            ) { onDismiss(); onToggleFormattedView() }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Zoom Section
            SectionHeader("Zoom")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ZoomButton(Icons.Default.ZoomOut, "Zoom Out") { onZoomOut() }
                ZoomButton(Icons.Default.FormatSize, "Reset") { onResetZoom() }
                ZoomButton(Icons.Default.ZoomIn, "Zoom In") { onZoomIn() }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Settings Section
            SectionHeader("Settings")
            MenuItem(Icons.Default.FormatSize, "Font...") { onDismiss(); onShowFontDialog() }
            MenuItem(Icons.Default.Palette, "Theme...") { onDismiss(); onShowThemeDialog() }
            MenuItem(Icons.Default.Info, "About") { onDismiss(); onShowAbout() }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun ToggleMenuItem(
    text: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ZoomButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = contentDescription,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
