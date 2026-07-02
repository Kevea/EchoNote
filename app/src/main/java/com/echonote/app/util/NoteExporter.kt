package com.echonote.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.echonote.app.data.Note
import java.io.File
import java.io.FileOutputStream

object NoteExporter {

    fun exportAsText(context: Context, note: Note): Uri {
        val title = note.title.ifBlank { "Notiz" }
        val file = exportFile(context, title, "txt")
        file.writeText(buildString {
            appendLine(title)
            appendLine()
            append(note.content)
        })
        return uriFor(context, file)
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
        val safeName = title.replace(Regex("[^A-Za-z0-9äöüÄÖÜß _-]"), "_").trim().take(60).ifBlank { "Notiz" }
        return File(dir, "$safeName.$extension")
    }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
