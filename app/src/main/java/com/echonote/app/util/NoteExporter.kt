package com.echonote.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.echonote.app.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NoteExporter {

    private val FRONTMATTER_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun exportAsText(context: Context, note: Note): Uri {
        val title = note.title.ifBlank { "Notiz" }
        val file = exportFile(context, title, "txt")
        file.writeText(buildTextBody(note, title))
        return uriFor(context, file)
    }

    fun exportAsMarkdown(context: Context, note: Note): Uri {
        val title = note.title.ifBlank { "Notiz" }
        val file = exportFile(context, title, "md")
        file.writeText(buildMarkdownBody(note, title))
        return uriFor(context, file)
    }

    // Writes into a user-picked SAF folder (persisted tree Uri), overwriting `fileName` if it
    // already exists there - used for the background auto-export, not the share-sheet path above.
    suspend fun exportToFolder(
        context: Context,
        note: Note,
        treeUri: Uri,
        format: ExportFormat,
        fileName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val tree = DocumentFile.fromTreeUri(context, treeUri)
                ?: error("Zielordner nicht verfügbar")
            if (!tree.isDirectory || !tree.canWrite()) {
                error("Kein Schreibzugriff auf den Exportordner")
            }

            val title = note.title.ifBlank { "Notiz" }
            val body = when (format) {
                ExportFormat.MARKDOWN -> buildMarkdownBody(note, title)
                ExportFormat.TEXT -> buildTextBody(note, title)
            }

            val target = tree.findFile(fileName) ?: tree.createFile(format.mimeType, fileName)
            ?: error("Datei konnte nicht angelegt werden")
            val stream = context.contentResolver.openOutputStream(target.uri, "wt")
                ?: error("Datei konnte nicht geöffnet werden")
            stream.use { it.write(body.toByteArray()) }
        }
    }

    private fun buildMarkdownBody(note: Note, title: String): String = buildString {
        appendLine("---")
        appendLine("tags: [${note.tagList.joinToString(", ")}]")
        appendLine("created: ${Instant.ofEpochMilli(note.createdAt).atZone(ZoneId.systemDefault()).format(FRONTMATTER_DATE_FORMAT)}")
        appendLine("---")
        appendLine()
        appendLine("# $title")
        appendLine()
        append(note.content)
    }

    private fun buildTextBody(note: Note, title: String): String = buildString {
        appendLine(title)
        if (note.tagList.isNotEmpty()) {
            appendLine("Tags: ${note.tagList.joinToString(", ")}")
        }
        appendLine()
        append(note.content)
    }

    fun exportAsPdf(context: Context, note: Note): Uri {
        val title = note.title.ifBlank { "Notiz" }
        val file = exportFile(context, title, "pdf")

        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = (pageWidth - margin * 2).toInt()
        val contentHeight = pageHeight - margin * 2

        val titlePaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 20f
            color = Color.BLACK
            typeface = Typeface.DEFAULT_BOLD
        }
        val bodyPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 12f
            color = Color.BLACK
        }

        val titleLayout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, contentWidth).build()
        val bodyText = note.content
        val bodyLayout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length, bodyPaint, contentWidth)
            .setLineSpacing(4f, 1f)
            .build()

        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        canvas.translate(margin, margin)
        titleLayout.draw(canvas)

        var cursorY = titleLayout.height + 20f
        val totalLines = bodyLayout.lineCount
        var lineIndex = 0
        while (lineIndex < totalLines) {
            if (cursorY >= contentHeight) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                canvas.translate(margin, margin)
                cursorY = 0f
            }
            val remainingHeight = contentHeight - cursorY
            val lineTop = bodyLayout.getLineTop(lineIndex)
            var linesFit = 0
            var idx = lineIndex
            while (idx < totalLines && bodyLayout.getLineBottom(idx) - lineTop <= remainingHeight) {
                linesFit++
                idx++
            }
            if (linesFit == 0) linesFit = 1
            val sliceBottom = bodyLayout.getLineBottom(lineIndex + linesFit - 1)
            val sliceHeight = sliceBottom - lineTop
            canvas.save()
            canvas.clipRect(0f, cursorY, contentWidth.toFloat(), cursorY + sliceHeight)
            canvas.translate(0f, cursorY - lineTop)
            bodyLayout.draw(canvas)
            canvas.restore()
            cursorY += sliceHeight
            lineIndex += linesFit
        }
        document.finishPage(page)

        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()

        return uriFor(context, file)
    }

    private fun exportFile(context: Context, title: String, extension: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        return File(dir, "${sanitizeFileName(title)}.$extension")
    }

    // Exposed (not private) so ExportPreferences can derive stable export file names from it.
    internal fun sanitizeFileName(title: String): String =
        title.replace(Regex("[^A-Za-z0-9äöüÄÖÜß _-]"), "_").trim().take(60).ifBlank { "Notiz" }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
