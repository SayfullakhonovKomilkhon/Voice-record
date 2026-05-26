package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.os.Build
import androidx.core.content.FileProvider
import com.example.data.model.Meeting
import com.example.data.model.MeetingTask
import com.example.data.model.Utterance
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.*

object MeetingExportHelper {

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    /**
     * Exports the meeting details into a Word companion (.doc formatted styled HTML) file.
     * Accessible by Word processors with fully designed styles, tables, list formatting, and beautiful alignment.
     */
    fun exportToWord(context: Context, meeting: Meeting): Uri? {
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("ru"))
        val formattedDate = dateFormat.format(Date(meeting.date))

        val wordHtml = StringBuilder()
        wordHtml.append("""
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8">
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333333; margin: 30px; }
                h1 { color: #1a73e8; border-bottom: 2px solid #1a73e8; padding-bottom: 8px; margin-bottom: 5px; font-size: 24px; }
                h2 { color: #1b5e20; border-bottom: 1px solid #dddddd; padding-bottom: 5px; margin-top: 30px; font-size: 18px; }
                .meta { color: #666666; font-size: 14px; margin-bottom: 25px; line-height: 1.5; }
                .meta strong { color: #111111; }
                .card { background-color: #f8f9fa; border-left: 4px solid #1a73e8; padding: 14px 18px; margin: 15px 0; border-radius: 4px; }
                table { width: 100%; border-collapse: collapse; margin-top: 15px; margin-bottom: 25px; }
                th { background-color: #f1f3f4; color: #202124; font-weight: bold; border: 1px solid #dadce0; padding: 12px; text-align: left; font-size: 13px; }
                td { border: 1px solid #dadce0; padding: 12px; vertical-align: top; font-size: 13px; }
                .status-badge { display: inline-block; padding: 4px 8px; font-size: 11px; font-weight: bold; border-radius: 4px; text-transform: uppercase; }
                .status-completed { background-color: #e6f4ea; color: #137333; }
                .status-pending { background-color: #fef7e0; color: #b06000; }
                .bullet-list { margin: 10px 0; padding-left: 20px; }
                .bullet-item { margin-bottom: 8px; font-size: 14px; }
                .utterance { margin-bottom: 14px; padding: 10px 14px; border-radius: 4px; background-color: #fafafa; border: 1px solid #f1f3f4; }
                .speaker-name { font-weight: bold; color: #1a73e8; font-size: 14px; }
                .timestamp { font-size: 12px; color: #888888; margin-left: 10px; font-family: 'Courier New', monospace; }
                .transcription-text { margin-top: 6px; font-size: 14px; color: #222222; }
            </style>
            </head>
            <body>
                <h1>${meeting.title}</h1>
                <div class="meta">
                    <strong>Дата и время:</strong> ${formattedDate}<br>
                    <strong>Участники беседы:</strong> ${meeting.participantA} • ${meeting.participantB}
                </div>

                <h2>Резюме встречи</h2>
                <div class="card">
                    ${(meeting.summaryOverview ?: "Резюме не заполнено.").replace("\n", "<br>")}
                </div>

                <h2>Обсуждаемые темы</h2>
                ${if (!meeting.summaryTopics.isNullOrEmpty()) {
                    "<ul class=\"bullet-list\">" + meeting.summaryTopics.joinToString("") { "<li class=\"bullet-item\">$it</li>" } + "</ul>"
                } else {
                    "<p style=\"color: #666;\">Обсуждаемые темы отсутствуют.</p>"
                }}

                <h2>Согласованные решения</h2>
                ${if (!meeting.summaryDecisions.isNullOrEmpty()) {
                    "<ul class=\"bullet-list\">" + meeting.summaryDecisions.joinToString("") { "<li class=\"bullet-item\">$it</li>" } + "</ul>"
                } else {
                    "<p style=\"color: #666;\">Принятые решения не зафиксированы.</p>"
                }}

                <h2>Задачи</h2>
                ${if (!meeting.summaryTasks.isNullOrEmpty()) {
                    """
                    <table>
                        <thead>
                            <tr>
                                <th>Исполнитель</th>
                                <th>Задача</th>
                                <th>Статус</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${meeting.summaryTasks.joinToString("") { task ->
                                val badgeClass = if (task.isCompleted) "status-completed" else "status-pending"
                                val statusText = if (task.isCompleted) "Выполнено" else "В процессе"
                                """
                                <tr>
                                    <td style="font-weight: bold;">${task.assignedTo}</td>
                                    <td>${task.taskText}</td>
                                    <td><span class="status-badge $badgeClass">$statusText</span></td>
                                </tr>
                                """
                            }}
                        </tbody>
                    </table>
                    """
                } else {
                    "<p style=\"color: #666;\">Задачи на встрече не зафиксированы.</p>"
                }}

                <h2>Полная расшифровка диалога</h2>
                <div>
                ${if (!meeting.transcript.isNullOrEmpty()) {
                    meeting.transcript.joinToString("") { utterance ->
                        val durationStr = formatDuration(utterance.timestampMs)
                        """
                        <div class="utterance">
                            <span class="speaker-name">${utterance.speaker}</span>
                            <span class="timestamp">[$durationStr]</span>
                            <div class="transcription-text">${utterance.text}</div>
                        </div>
                        """
                    }
                } else {
                    "<p style=\"color: #666;\">Текстовая расшифровка диалога отсутствует.</p>"
                }}
                </div>
            </body>
            </html>
        """.trimIndent())

        return try {
            val fileName = "Протокол_${meeting.title.replace(" ", "_")}.doc"
            val shareCacheDir = File(context.cacheDir, "shared_documents")
            if (!shareCacheDir.exists()) {
                shareCacheDir.mkdirs()
            }
            val wordFile = File(shareCacheDir, fileName)
            FileOutputStream(wordFile).use { fos ->
                fos.write(wordHtml.toString().toByteArray(Charsets.UTF_8))
            }
            
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, wordFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports the meeting details into a high-fidelity fully-wrapped PDF file.
     * Uses native android.graphics.pdf.PdfDocument with line-by-line flowing algorithm.
     */
    fun exportToPdf(context: Context, meeting: Meeting): Uri? {
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("ru"))
        val formattedDate = dateFormat.format(Date(meeting.date))

        val pdfDocument = PdfDocument()
        val generator = PdfDocGenerator(pdfDocument)

        // Draw headers on page 1
        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#1A73E8")
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaPaint = TextPaint().apply {
            color = Color.parseColor("#555555")
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val h1Paint = TextPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 13f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = TextPaint().apply {
            color = Color.parseColor("#333333")
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val italicPaint = TextPaint().apply {
            color = Color.parseColor("#666666")
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        // Draw Document title
        generator.drawText(meeting.title, titlePaint, spacing = 8f)
        generator.drawText("Дата: $formattedDate | Собеседники: ${meeting.participantA} и ${meeting.participantB}", metaPaint, spacing = 16f)
        generator.drawHorizontalLine("#DADCE0", height = 1.5f, spacing = 14f)

        // 1. Резюме
        generator.drawText("Резюме встречи:", h1Paint, spacing = 8f)
        val overview = meeting.summaryOverview ?: "Резюме не заполнено."
        generator.drawText(overview, bodyPaint, spacing = 15f)

        // 2. Обсуждаемые темы
        generator.drawHorizontalLine("#E0E0E0", height = 1f, spacing = 12f)
        generator.drawText("Обсуждаемые темы:", h1Paint, spacing = 8f)
        if (!meeting.summaryTopics.isNullOrEmpty()) {
            meeting.summaryTopics.forEach { topic ->
                generator.drawText("• $topic", bodyPaint, spacing = 5f)
            }
            generator.currentY += 10f
        } else {
            generator.drawText("Темы встреч отсутствуют.", italicPaint, spacing = 10f)
        }

        // 3. Согласованные решения
        generator.drawHorizontalLine("#E0E0E0", height = 1f, spacing = 12f)
        generator.drawText("Принятые решения:", h1Paint, spacing = 8f)
        if (!meeting.summaryDecisions.isNullOrEmpty()) {
            meeting.summaryDecisions.forEach { decision ->
                generator.drawText("✓ $decision", bodyPaint, spacing = 5f)
            }
            generator.currentY += 10f
        } else {
            generator.drawText("Решения на встрече не зафиксированы.", italicPaint, spacing = 10f)
        }

        // 4. Задачи
        generator.drawHorizontalLine("#E0E0E0", height = 1f, spacing = 12f)
        generator.drawText("Задачи (план действий):", h1Paint, spacing = 10f)
        if (!meeting.summaryTasks.isNullOrEmpty()) {
            generator.drawTasksTable(meeting.summaryTasks)
            generator.currentY += 10f
        } else {
            generator.drawText("Согласованные задачи отсутствуют.", italicPaint, spacing = 10f)
        }

        // 5. Транскрипция
        generator.drawHorizontalLine("#DADCE0", height = 1.5f, spacing = 14f)
        generator.drawText("Полная расшифровка диалога:", h1Paint, spacing = 12f)
        if (!meeting.transcript.isNullOrEmpty()) {
            val speakerPaint = TextPaint().apply {
                color = Color.parseColor("#1A73E8")
                textSize = 9f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            meeting.transcript.forEach { utterance ->
                val timeText = formatDuration(utterance.timestampMs)
                generator.drawText("${utterance.speaker} [$timeText]", speakerPaint, spacing = 2f)
                generator.drawText(utterance.text, bodyPaint, spacing = 8f)
            }
        } else {
            generator.drawText("Текстовая расшифровка отсутствует.", italicPaint, spacing = 10f)
        }

        generator.finish()

        return try {
            val fileName = "Протокол_${meeting.title.replace(" ", "_")}.pdf"
            val shareCacheDir = File(context.cacheDir, "shared_documents")
            if (!shareCacheDir.exists()) {
                shareCacheDir.mkdirs()
            }
            val pdfFile = File(shareCacheDir, fileName)
            FileOutputStream(pdfFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Helper to sharing file with multiple apps.
     */
    fun shareFile(context: Context, fileUri: Uri, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (fileUri.toString().endsWith(".pdf")) "application/pdf" else "application/msword"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private class PdfDocGenerator(val pdfDocument: PdfDocument) {
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 50f
        
        val width = 515f // 595 - 40 - 40
        val leftMargin = 40f
        val pageHeight = 842f
        val bottomMargin = 50f

        init {
            drawFooter()
        }
        
        fun checkNewPage(neededHeight: Float) {
            if (currentY + neededHeight > pageHeight - bottomMargin) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawFooter()
                currentY = 50f
            }
        }
        
        fun drawFooter() {
            val footerPaint = TextPaint().apply {
                color = Color.LTGRAY
                textSize = 8f
                isAntiAlias = true
            }
            canvas.drawText("Страница $pageNumber", 595f - 100f, 842f - 30f, footerPaint)
        }
        
        fun drawText(text: String, paint: TextPaint, spacing: Float = 6f) {
            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(
                    text,
                    paint,
                    width.toInt(),
                    Layout.Alignment.ALIGN_NORMAL,
                    1.15f,
                    0f,
                    false
                )
            }
            
            val lineCount = staticLayout.lineCount
            var startLine = 0
            while (startLine < lineCount) {
                val currentRemainingHeight = pageHeight - bottomMargin - currentY
                var linesThatFit = 0
                while (startLine + linesThatFit < lineCount) {
                    val nextHeight = staticLayout.getLineBottom(startLine + linesThatFit) - staticLayout.getLineTop(startLine)
                    if (nextHeight > currentRemainingHeight) {
                        break
                    }
                    linesThatFit++
                }
                
                if (linesThatFit == 0) {
                    checkNewPage(20f)
                    continue
                }
                
                canvas.save()
                canvas.translate(leftMargin, currentY)
                
                val topClip = staticLayout.getLineTop(startLine)
                val bottomClip = staticLayout.getLineBottom(startLine + linesThatFit - 1)
                canvas.clipRect(0f, 0f, width, (bottomClip - topClip).toFloat())
                canvas.translate(0f, -topClip.toFloat())
                staticLayout.draw(canvas)
                canvas.restore()
                
                currentY += (bottomClip - topClip).toFloat() + spacing
                startLine += linesThatFit
                
                if (startLine < lineCount) {
                    checkNewPage(20f)
                }
            }
        }

        fun drawHorizontalLine(colorHex: String, height: Float, spacing: Float) {
            checkNewPage(height + spacing)
            val linePaint = Paint().apply {
                color = Color.parseColor(colorHex)
                strokeWidth = height
                style = Paint.Style.STROKE
            }
            canvas.drawLine(leftMargin, currentY, 595f - leftMargin, currentY, linePaint)
            currentY += height + spacing
        }
        
        fun drawTasksTable(tasks: List<MeetingTask>) {
            val headerHeight = 22f
            checkNewPage(headerHeight + 25f)
            
            val colWidths = floatArrayOf(150f, 265f, 100f) // Total = 515
            val colHeaders = listOf("Исполнитель", "Описание задачи", "Статус")
            
            val bgPaint = Paint().apply {
                color = Color.parseColor("#F1F3F4")
                style = Paint.Style.FILL
            }
            canvas.drawRect(leftMargin, currentY, leftMargin + width, currentY + headerHeight, bgPaint)
            
            val strokePaint = Paint().apply {
                color = Color.parseColor("#DADCE0")
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            canvas.drawRect(leftMargin, currentY, leftMargin + width, currentY + headerHeight, strokePaint)
            
            val headerTextPaint = TextPaint().apply {
                color = Color.parseColor("#202124")
                textSize = 8.5f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            
            var xOffset = leftMargin
            for (i in colHeaders.indices) {
                canvas.drawText(colHeaders[i], xOffset + 6f, currentY + 14f, headerTextPaint)
                if (i > 0) {
                    canvas.drawLine(xOffset, currentY, xOffset, currentY + headerHeight, strokePaint)
                }
                xOffset += colWidths[i]
            }
            
            currentY += headerHeight
            
            val cellTextPaint = TextPaint().apply {
                color = Color.parseColor("#333333")
                textSize = 8.5f
                isAntiAlias = true
            }
            
            val cellTextBoldPaint = TextPaint().apply {
                color = Color.parseColor("#333333")
                textSize = 8.5f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            
            tasks.forEach { task ->
                val assigneeStr = task.assignedTo
                val assigneeLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(assigneeStr, 0, assigneeStr.length, cellTextBoldPaint, (colWidths[0] - 12f).toInt()).build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(assigneeStr, cellTextBoldPaint, (colWidths[0] - 12f).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.1f, 0f, false)
                }

                val taskTextStr = task.taskText
                val taskLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(taskTextStr, 0, taskTextStr.length, cellTextPaint, (colWidths[1] - 12f).toInt()).build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(taskTextStr, cellTextPaint, (colWidths[1] - 12f).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.1f, 0f, false)
                }
                
                val statusStr = if (task.isCompleted) "Выполнено" else "Активно"
                val statusLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(statusStr, 0, statusStr.length, cellTextBoldPaint, (colWidths[2] - 12f).toInt()).build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(statusStr, cellTextBoldPaint, (colWidths[2] - 12f).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.1f, 0f, false)
                }
                
                val maxHeight = maxOf(assigneeLayout.height, taskLayout.height, statusLayout.height)
                val rowHeight = maxHeight + 14f
                
                checkNewPage(rowHeight)
                
                // Draw row background container boundary
                canvas.drawRect(leftMargin, currentY, leftMargin + width, currentY + rowHeight, strokePaint)
                
                var xLoc = leftMargin
                
                // Col 1
                canvas.save()
                canvas.translate(xLoc + 6f, currentY + 7f)
                assigneeLayout.draw(canvas)
                canvas.restore()
                
                xLoc += colWidths[0]
                canvas.drawLine(xLoc, currentY, xLoc, currentY + rowHeight, strokePaint)
                
                // Col 2
                canvas.save()
                canvas.translate(xLoc + 6f, currentY + 7f)
                taskLayout.draw(canvas)
                canvas.restore()
                
                xLoc += colWidths[1]
                canvas.drawLine(xLoc, currentY, xLoc, currentY + rowHeight, strokePaint)
                
                // Col 3 status badging
                val badgeColor = Color.parseColor(if (task.isCompleted) "#E6F4EA" else "#FEF7E0")
                val badgePaint = Paint().apply {
                    color = badgeColor
                    style = Paint.Style.FILL
                }
                canvas.drawRect(xLoc + 4f, currentY + 4f, xLoc + colWidths[2] - 4f, currentY + rowHeight - 4f, badgePaint)
                
                canvas.save()
                canvas.translate(xLoc + 8f, currentY + 7f)
                statusLayout.paint.color = Color.parseColor(if (task.isCompleted) "#137333" else "#B06000")
                statusLayout.draw(canvas)
                canvas.restore()
                
                currentY += rowHeight
            }
        }
        
        fun finish() {
            pdfDocument.finishPage(page)
        }
    }
}
