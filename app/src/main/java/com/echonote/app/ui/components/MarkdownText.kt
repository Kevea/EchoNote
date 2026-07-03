package com.echonote.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

// Minimal Obsidian-style renderer: headers, bullets, **bold**/_italic_ — not full CommonMark.
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, textColor: Color = Color.Unspecified) {
    Column(modifier = modifier) {
        text.lines().forEach { line ->
            when {
                line.startsWith("### ") -> Text(
                    parseInline(line.removePrefix("### ")),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                )
                line.startsWith("## ") -> Text(
                    parseInline(line.removePrefix("## ")),
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                )
                line.startsWith("# ") -> Text(
                    parseInline(line.removePrefix("# ")),
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor,
                )
                line.startsWith("- ") || line.startsWith("* ") -> Row(modifier = Modifier.padding(start = 4.dp)) {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge, color = textColor)
                    Text(parseInline(line.drop(2)), style = MaterialTheme.typography.bodyLarge, color = textColor)
                }
                line.isBlank() -> Spacer(modifier = Modifier.height(8.dp))
                else -> Text(parseInline(line), style = MaterialTheme.typography.bodyLarge, color = textColor)
            }
        }
    }
}

private val boldItalicRegex = Regex("(\\*\\*.+?\\*\\*|_.+?_)")

private fun parseInline(line: String): AnnotatedString = buildAnnotatedString {
    var lastIndex = 0
    for (match in boldItalicRegex.findAll(line)) {
        append(line.substring(lastIndex, match.range.first))
        val token = match.value
        when {
            token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(token.removeSurrounding("**"))
            }
            token.startsWith("_") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(token.removeSurrounding("_"))
            }
        }
        lastIndex = match.range.last + 1
    }
    append(line.substring(lastIndex))
}
