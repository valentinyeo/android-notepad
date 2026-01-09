package com.simplenotepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontSize: TextUnit,
    formattedView: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Calculate approximate line height in pixels
    val lineHeightPx = with(density) { (fontSize.value * 1.5f).dp.toPx() }

    // Auto-scroll to keep cursor visible when text changes
    LaunchedEffect(value.selection.end, value.text.length) {
        val cursorPosition = value.selection.end
        val textBeforeCursor = value.text.take(cursorPosition)
        val lineNumber = textBeforeCursor.count { it == '\n' }

        // Estimate cursor Y position (line number * line height + padding)
        val cursorY = (lineNumber * lineHeightPx + 12 * density.density).toInt()

        // Get visible area
        val viewportHeight = scrollState.viewportSize
        val currentScroll = scrollState.value

        // Scroll if cursor is below visible area (with some margin)
        val bottomMargin = (lineHeightPx * 2).toInt()
        if (cursorY > currentScroll + viewportHeight - bottomMargin) {
            scrollState.animateScrollTo(cursorY - viewportHeight + bottomMargin)
        }
        // Scroll if cursor is above visible area
        else if (cursorY < currentScroll + lineHeightPx) {
            scrollState.animateScrollTo(maxOf(0, cursorY - lineHeightPx.toInt()))
        }
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val headerColor = textColor
    val codeColor = MaterialTheme.colorScheme.tertiary
    val linkColor = MaterialTheme.colorScheme.secondary

    // Parse markdown and create styled text (only in formatted view)
    val styledText = remember(value.text, textColor, headerColor, codeColor, linkColor, fontSize, formattedView) {
        if (formattedView) {
            parseMarkdown(value.text, textColor, headerColor, codeColor, linkColor, fontSize)
        } else {
            // Raw markdown view - just plain text with syntax visible
            AnnotatedString(value.text)
        }
    }

    Box(modifier = modifier) {
        BasicTextField(
            value = value.copy(annotatedString = styledText),
            onValueChange = { newValue ->
                // Keep only the text changes, our styling will be recomputed
                onValueChange(TextFieldValue(
                    text = newValue.text,
                    selection = newValue.selection,
                    composition = newValue.composition
                ))
            },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize,
                color = textColor
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(12.dp)
        )
    }
}

private fun parseMarkdown(
    text: String,
    textColor: androidx.compose.ui.graphics.Color,
    headerColor: androidx.compose.ui.graphics.Color,
    codeColor: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color,
    baseFontSize: TextUnit
): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")

        lines.forEachIndexed { lineIndex, line ->
            if (lineIndex > 0) append("\n")

            // Hidden style for markdown syntax (completely invisible)
            val hiddenStyle = SpanStyle(
                color = textColor.copy(alpha = 0f),
                fontSize = 0.1.sp,
                letterSpacing = (-0.5).sp
            )

            when {
                // Headers: # to ###### - completely hide the hashtags
                line.startsWith("######") && line.length > 6 -> {
                    withStyle(hiddenStyle) { append("###### ") }
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = headerColor,
                        fontSize = baseFontSize * 1.1f
                    )) { append(line.drop(7)) }
                }
                line.startsWith("#####") && line.length > 5 -> {
                    withStyle(hiddenStyle) { append("##### ") }
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = headerColor,
                        fontSize = baseFontSize * 1.15f
                    )) { append(line.drop(6)) }
                }
                line.startsWith("####") && line.length > 4 -> {
                    withStyle(hiddenStyle) { append("#### ") }
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = headerColor,
                        fontSize = baseFontSize * 1.2f
                    )) { append(line.drop(5)) }
                }
                line.startsWith("###") && line.length > 3 -> {
                    withStyle(hiddenStyle) { append("### ") }
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = headerColor,
                        fontSize = baseFontSize * 1.3f
                    )) { append(line.drop(4)) }
                }
                line.startsWith("##") && line.length > 2 -> {
                    withStyle(hiddenStyle) { append("## ") }
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = headerColor,
                        fontSize = baseFontSize * 1.5f
                    )) { append(line.drop(3)) }
                }
                line.startsWith("# ") && line.length > 2 -> {
                    withStyle(hiddenStyle) { append("# ") }
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = headerColor,
                        fontSize = baseFontSize * 1.8f
                    )) { append(line.drop(2)) }
                }
                // Bullet points - show bullet character with faint original marker
                line.trimStart().startsWith("- ") ||
                line.trimStart().startsWith("* ") ||
                line.trimStart().startsWith("+ ") -> {
                    val indent = line.takeWhile { it.isWhitespace() }
                    val content = line.trimStart().drop(2)
                    append(indent)
                    // Replace marker visually with bullet (same char count: "- " -> "• ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = headerColor)) {
                        append("• ")
                    }
                    appendInlineMarkdown(content, textColor, codeColor, linkColor)
                }
                // Numbered list
                line.trimStart().matches(Regex("^\\d+\\.\\s.*")) -> {
                    val indent = line.takeWhile { it.isWhitespace() }
                    val match = Regex("^(\\d+)\\.\\s(.*)").find(line.trimStart())
                    if (match != null) {
                        val number = match.groupValues[1]
                        val content = match.groupValues[2]
                        append(indent)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("$number. ")
                        }
                        appendInlineMarkdown(content, textColor, codeColor, linkColor)
                    } else {
                        append(line)
                    }
                }
                // Blockquote
                line.trimStart().startsWith(">") -> {
                    withStyle(SpanStyle(
                        color = textColor.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic
                    )) { append(line) }
                }
                // Code block markers
                line.trim() == "```" || line.trim().startsWith("```") -> {
                    withStyle(SpanStyle(
                        color = codeColor,
                        background = codeColor.copy(alpha = 0.1f)
                    )) { append(line) }
                }
                // Horizontal rule
                line.trim().matches(Regex("^-{3,}$|^\\*{3,}$|^_{3,}$")) -> {
                    withStyle(SpanStyle(color = textColor.copy(alpha = 0.5f))) {
                        append("─".repeat(20))
                    }
                }
                else -> {
                    appendInlineMarkdown(line, textColor, codeColor, linkColor)
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    textColor: androidx.compose.ui.graphics.Color,
    codeColor: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color
) {
    var i = 0
    while (i < text.length) {
        when {
            // Bold: **text** or __text__
            text.substring(i).startsWith("**") -> {
                val end = text.indexOf("**", i + 2)
                if (end > i) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Strikethrough: ~~text~~
            text.substring(i).startsWith("~~") -> {
                val end = text.indexOf("~~", i + 2)
                if (end > i) {
                    withStyle(SpanStyle(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Inline code: `code`
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i) {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = codeColor,
                        background = codeColor.copy(alpha = 0.1f)
                    )) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            // Italic: *text* (but not **)
            text[i] == '*' && (i + 1 >= text.length || text[i + 1] != '*') -> {
                val end = text.indexOf('*', i + 1)
                if (end > i && (end + 1 >= text.length || text[end + 1] != '*')) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            // Link: [text](url)
            text[i] == '[' -> {
                val closeBracket = text.indexOf(']', i)
                if (closeBracket > i && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen > closeBracket) {
                        val linkText = text.substring(i + 1, closeBracket)
                        withStyle(SpanStyle(
                            color = linkColor,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )) {
                            append(linkText)
                        }
                        i = closeParen + 1
                    } else {
                        append(text[i])
                        i++
                    }
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
