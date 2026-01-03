package com.simplenotepad.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplenotepad.viewmodel.FindReplaceState

@Composable
fun FindReplaceBar(
    state: FindReplaceState,
    onSearchQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onToggleMatchCase: () -> Unit,
    onToggleWrapAround: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        // Find row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            SearchField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Find",
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (state.matchCount > 0) "${state.currentMatch}/${state.matchCount}" else "No results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(onClick = onFindPrevious, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Find previous")
            }
            IconButton(onClick = onFindNext, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Find next")
            }
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        // Replace row (conditional)
        AnimatedVisibility(visible = state.showReplace) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SearchField(
                        value = state.replaceQuery,
                        onValueChange = onReplaceQueryChange,
                        placeholder = "Replace",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(onClick = onReplaceCurrent) {
                        Text("Replace", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    TextButton(onClick = onReplaceAll) {
                        Text("All", fontSize = 12.sp)
                    }
                }
            }
        }

        // Options row
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = state.matchCase,
                onClick = onToggleMatchCase,
                label = { Text("Match case", fontSize = 12.sp) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = state.wrapAround,
                onClick = onToggleWrapAround,
                label = { Text("Wrap around", fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )
            }
            innerTextField()
        }
    )
}
