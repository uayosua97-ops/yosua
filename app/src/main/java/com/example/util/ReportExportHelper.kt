package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportColumn(val header: String, val weight: Float)

object ReportExportHelper {

    fun formatIdr(amount: Double): String {
        val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        fmt.maximumFractionDigits = 0
        return fmt.format(amount)
    }

    fun formatTons(tons: Double): String =
        String.format(Locale.US, "%.2f Ton", tons)

    fun exportCsvAndShare(
        context: Context,
        fileNamePrefix: String,
        headers: List<String>,
        rows: List<List<String>>
    ): Result<File> {
        return runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(exportDir, "${fileNamePrefix}_$stamp.csv")
            val sb = StringBuilder()
            // UTF-8 BOM for clean Excel compatibility
            sb.append('\uFEFF')
            sb.append(headers.joinToString(",") { escapeCsv(it) }).append('\n')
            rows.forEach { row ->
                sb.append(row.joinToString(",") { escapeCsv(it) }).append('\n')
            }
            file.writeText(sb.toString(), Charsets.UTF_8)
            shareFile(context, file, "text/csv", "Export Excel/CSV - $fileNamePrefix")
            file
        }
    }

    fun exportPdfAndShare(
        context: Context,
        title: String,
        companyName: String,
        subtitle: String,
        headers: List<String>,
        rows: List<List<String>>
    ): Result<File> {
        return runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safePrefix = title.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_")
            val file = File(exportDir, "${safePrefix}_$stamp.pdf")

            val pdf = PdfDocument()
            val pageWidth = 842 // A4 Landscape
            val pageHeight = 595
            val margin = 32f

            val titlePaint = Paint().apply {
                color = Color.rgb(13, 64, 49)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subPaint = Paint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 11f
            }
            val headerBgPaint = Paint().apply {
                color = Color.rgb(13, 64, 49)
            }
            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9f
            }
            val rowAltPaint = Paint().apply {
                color = Color.rgb(241, 245, 249)
            }

            val colCount = headers.size.coerceAtLeast(1)
            val usableWidth = pageWidth - (margin * 2)
            val colWidth = usableWidth / colCount

            var pageNum = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            var canvas = page.canvas
            var y = margin + 16f

            fun drawHeaderBlock() {
                canvas.drawText(companyName.uppercase(Locale.ROOT), margin, y, titlePaint)
                y += 18f
                canvas.drawText("$title — $subtitle", margin, y, subPaint)
                y += 18f
                canvas.drawRect(margin, y, pageWidth - margin, y + 22f, headerBgPaint)
                headers.forEachIndexed { idx, h ->
                    val x = margin + idx * colWidth + 4f
                    canvas.drawText(h.take(18), x, y + 15f, headerTextPaint)
                }
                y += 28f
            }

            drawHeaderBlock()

            rows.forEachIndexed { rowIdx, row ->
                if (y > pageHeight - margin - 20f) {
                    pdf.finishPage(page)
                    pageNum++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    y = margin + 16f
                    drawHeaderBlock()
                }
                if (rowIdx % 2 == 1) {
                    canvas.drawRect(margin, y - 12f, pageWidth - margin, y + 6f, rowAltPaint)
                }
                row.forEachIndexed { colIdx, value ->
                    if (colIdx < colCount) {
                        val x = margin + colIdx * colWidth + 4f
                        canvas.drawText(value.take(20), x, y, cellPaint)
                    }
                }
                y += 18f
            }

            pdf.finishPage(page)
            FileOutputStream(file).use { pdf.writeTo(it) }
            pdf.close()

            shareFile(context, file, "application/pdf", "Export PDF - $title")
            file
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        val cleaned = value.replace("\"", "\"\"")
        return "\"$cleaned\""
    }
}
