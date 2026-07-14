package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceEntity
import com.example.data.model.UserEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AttendanceExporter {

    private val monthMap = mapOf(
        "January" to "01", "February" to "02", "March" to "03", "April" to "04",
        "May" to "05", "June" to "06", "July" to "07", "August" to "08",
        "September" to "09", "October" to "10", "November" to "11", "December" to "12"
    )

    fun exportMonthlyAttendance(
        context: Context,
        monthYearString: String,
        workers: List<UserEntity>,
        allAttendance: List<AttendanceEntity>,
        format: String // "CSV" or "PDF"
    ) {
        val parts = monthYearString.split(" ")
        if (parts.size != 2) return
        val monthWord = parts[0]
        val yearStr = parts[1]
        val monthCode = monthMap[monthWord] ?: "01"
        val prefix = "$yearStr-$monthCode"

        // Filter and sort attendance records for this month
        val monthlyRecords = allAttendance
            .filter { it.dateString.startsWith(prefix) }
            .sortedWith(compareBy({ it.dateString }, { record ->
                workers.find { it.id == record.userId }?.name ?: ""
            }))

        val extension = if (format == "CSV") "csv" else "pdf"
        val fileName = "MTS_Attendance_Report_${monthYearString.replace(" ", "_")}.$extension"
        val file = File(context.cacheDir, fileName)

        try {
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()

            if (format == "CSV") {
                writeCsv(file, monthlyRecords, workers)
            } else {
                writePdf(file, monthYearString, monthlyRecords, workers)
            }

            shareFile(context, file, monthYearString, format)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeCsv(file: File, records: List<AttendanceEntity>, workers: List<UserEntity>) {
        FileOutputStream(file).use { fos ->
            val header = "Date,Employee Name,Username,Status\n"
            fos.write(header.toByteArray())

            for (record in records) {
                val worker = workers.find { it.id == record.userId }
                val name = worker?.name ?: "Unknown"
                val username = worker?.username ?: "N/A"
                val line = "${record.dateString},\"$name\",$username,${record.status}\n"
                fos.write(line.toByteArray())
            }
        }
    }

    private fun writePdf(
        file: File,
        monthYearString: String,
        records: List<AttendanceEntity>,
        workers: List<UserEntity>
    ) {
        val pdfDocument = PdfDocument()

        // Page dimensions (A4 size: 595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        val paintText = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
        }

        val paintHeaderBlock = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(19, 141, 151) // LogoTeal
        }

        val paintRowAlt = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(245, 247, 250) // Light grayish/blue for alternating rows
        }

        val paintLine = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(220, 225, 230)
            strokeWidth = 1f
        }

        val paintRedStatus = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(229, 29, 37) // LogoRed
        }

        val paintTealStatus = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(19, 141, 151) // LogoTeal
        }

        // Paginate records
        val rowsPerPage = 22
        var recordIndex = 0

        while (recordIndex < records.size || records.isEmpty()) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // 1. Draw header background block
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 120f, paintHeaderBlock)

            // 2. Draw Header Text
            paintText.color = android.graphics.Color.WHITE
            paintText.textSize = 18f
            paintText.isFakeBoldText = true
            canvas.drawText("MULTITECH TECHNICAL SERVICES L.L.C", 30f, 45f, paintText)

            paintText.textSize = 12f
            paintText.isFakeBoldText = false
            canvas.drawText("Monthly Attendance Log - $monthYearString", 30f, 75f, paintText)

            // Dynamic Info on right of header
            paintText.textSize = 9f
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val genDateStr = sdf.format(Date())
            paintText.textAlign = Paint.Align.RIGHT
            canvas.drawText("Generated: $genDateStr", (pageWidth - 30).toFloat(), 40f, paintText)
            canvas.drawText("Page $pageNumber", (pageWidth - 30).toFloat(), 60f, paintText)
            canvas.drawText("Total Records: ${records.size}", (pageWidth - 30).toFloat(), 80f, paintText)

            // Reset text paint
            paintText.textAlign = Paint.Align.LEFT
            paintText.color = android.graphics.Color.BLACK

            // 3. Draw Table Headers
            var y = 160f
            paintText.textSize = 10f
            paintText.isFakeBoldText = true
            paintText.color = android.graphics.Color.rgb(100, 110, 120)

            canvas.drawText("DATE", 40f, y, paintText)
            canvas.drawText("EMPLOYEE NAME", 150f, y, paintText)
            canvas.drawText("USERNAME", 350f, y, paintText)
            canvas.drawText("STATUS", 480f, y, paintText)

            y += 8f
            canvas.drawLine(30f, y, (pageWidth - 30).toFloat(), y, paintLine)
            y += 18f

            // 4. Draw Rows
            paintText.isFakeBoldText = false
            paintText.color = android.graphics.Color.BLACK

            var count = 0
            if (records.isEmpty()) {
                paintText.textSize = 12f
                paintText.color = android.graphics.Color.GRAY
                canvas.drawText("No attendance records found for this month.", 50f, 220f, paintText)
            } else {
                while (count < rowsPerPage && recordIndex < records.size) {
                    val record = records[recordIndex]
                    val worker = workers.find { it.id == record.userId }
                    val name = worker?.name ?: "Unknown"
                    val username = worker?.username ?: "N/A"

                    // Draw alternating row bg
                    if (count % 2 == 1) {
                        canvas.drawRect(30f, y - 14f, (pageWidth - 30).toFloat(), y + 6f, paintRowAlt)
                    }

                    // Content
                    paintText.textSize = 9f
                    canvas.drawText(record.dateString, 40f, y, paintText)

                    // Truncate name if too long
                    val dispName = if (name.length > 28) name.substring(0, 26) + "..." else name
                    canvas.drawText(dispName, 150f, y, paintText)

                    canvas.drawText(username, 350f, y, paintText)

                    // Draw status with color code
                    val statusPaint = if (record.status == "Present") paintTealStatus else paintRedStatus
                    statusPaint.textSize = 9f
                    statusPaint.isFakeBoldText = true
                    canvas.drawText(record.status, 480f, y, statusPaint)

                    y += 24f
                    count++
                    recordIndex++
                }
            }

            // Draw footer line
            canvas.drawLine(30f, (pageHeight - 50).toFloat(), (pageWidth - 30).toFloat(), (pageHeight - 50).toFloat(), paintLine)

            paintText.textSize = 8f
            paintText.color = android.graphics.Color.rgb(120, 130, 140)
            paintText.isFakeBoldText = false
            canvas.drawText("MTS Attendance System © 2026. Confidential Report.", 40f, (pageHeight - 35).toFloat(), paintText)

            pdfDocument.finishPage(page)
            pageNumber++

            // Break if records are empty to avoid infinite loop
            if (records.isEmpty()) break
        }

        FileOutputStream(file).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
    }

    private fun shareFile(context: Context, file: File, monthYearString: String, format: String) {
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val mimeType = if (format == "CSV") "text/csv" else "application/pdf"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "MTS Attendance Report - $monthYearString")
            putExtra(Intent.EXTRA_TEXT, "Please find attached the monthly attendance report for $monthYearString.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(intent, "Share Report via")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}
