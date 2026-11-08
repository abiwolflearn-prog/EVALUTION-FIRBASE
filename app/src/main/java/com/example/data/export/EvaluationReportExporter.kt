package com.example.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.EvaluationExportDataset
import com.example.data.model.ExportFormat
import com.example.data.model.GeneratedReportFile
import com.example.data.model.UserProfile
import com.example.data.model.WcuEvaluationRubricCategories
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Production-grade PDF (`.pdf`) and Excel (`.xlsx`) Evaluation Report Generator for
 * Wachemo University Administrator Dashboards (Phase 5).
 *
 * SECURITY INVARIANT:
 * - Enforces a secondary defense-in-depth role check before writing any file:
 *   if `dataset.generatedByRole == "student"` or is not an authorized admin role, throws `SecurityException`.
 */
object EvaluationReportExporter {

  fun getReportsDirectory(context: Context): File {
    val dir = File(context.cacheDir, "wcu_reports")
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  private fun verifyExportRoleAuthorization(dataset: EvaluationExportDataset) {
    if (dataset.generatedByUid.isBlank()) {
      throw SecurityException("Export denied: Missing authenticated administrator UID.")
    }
    if (dataset.generatedByRole == UserProfile.ROLE_STUDENT) {
      throw SecurityException(
        "Export denied: Students are strictly prohibited from exporting evaluation reports."
      )
    }
    val allowedRoles =
      setOf(
        UserProfile.ROLE_SUPER_ADMIN,
        UserProfile.ROLE_DEPARTMENT_ADMIN,
        UserProfile.ROLE_ADMIN,
      )
    if (dataset.generatedByRole !in allowedRoles) {
      throw SecurityException(
        "Export denied: Role '${dataset.generatedByRole}' is not authorized to export evaluation reports."
      )
    }
  }

  private fun buildSafeFileName(
    dataset: EvaluationExportDataset,
    format: ExportFormat,
  ): String {
    val sanitizedScope =
      dataset.reportScope
        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        .trim('_')
        .ifBlank { "UNIVERSITY" }
    val yearSuffix =
      dataset.academicYearLabel
        .replace(Regex("[^0-9]"), "_")
        .trim('_')
        .ifBlank { "2026" }
    return "WCU_Evaluation_Report_${sanitizedScope}_${yearSuffix}.${format.extension}"
  }

  /**
   * Generates a professional, multi-page, printable Wachemo University PDF 1.4 evaluation report.
   *
   * Contains:
   * - Wachemo University branding & vector header banner
   * - Report title, scope, and generation timestamp
   * - Faculty, Department, Semester, and Academic Year metadata
   * - Evaluation statistics and KPI summary box
   * - Category-level performance averages (`cat_1`..`cat_6`) with vector progress bar charts
   * - Teacher & course evaluation results table
   * - Automatic pagination and `Page X of Y` footers
   */
  fun generatePdfReport(
    outputDir: File,
    dataset: EvaluationExportDataset,
    customFileName: String? = null,
  ): Result<GeneratedReportFile> = runCatching {
    verifyExportRoleAuthorization(dataset)

    if (!outputDir.exists() && !outputDir.mkdirs()) {
      throw IllegalStateException("Unable to create export directory: ${outputDir.absolutePath}")
    }

    val fileName =
      customFileName?.trim()?.takeIf { it.isNotBlank() }
        ?: buildSafeFileName(dataset, ExportFormat.PDF)
    val targetFile = File(outputDir, fileName)

    val pdfBytes = buildPdf14DocumentBytes(dataset)
    FileOutputStream(targetFile).use { fos ->
      fos.write(pdfBytes)
      fos.flush()
    }

    if (!targetFile.exists() || targetFile.length() <= 0L) {
      throw IllegalStateException("Generated PDF report file is empty.")
    }

    GeneratedReportFile(
      filePath = targetFile.absolutePath,
      fileName = targetFile.name,
      format = ExportFormat.PDF,
      byteSize = targetFile.length(),
      exportedRecordCount = dataset.totalEvaluationsCount,
      reportScope = dataset.reportScope,
      generatedAtIso = dataset.generatedAtIso,
    )
  }

  /**
   * Generates a structured OpenXML `.xlsx` Excel spreadsheet containing:
   * - Sheet 1: `Summary & Metadata` (University metadata, filters, overall statistics, category rubric summary)
   * - Sheet 2: `Teacher Evaluations` (Teacher info, Faculty, Department, Course, Semester, Evaluation count, Average rating, `cat_1`..`cat_6` scores, Overall result)
   * - Sheet 3: `Category Rubric Scores` (6-category breakdown + Department comparison metrics)
   */
  fun generateExcelReport(
    outputDir: File,
    dataset: EvaluationExportDataset,
    customFileName: String? = null,
  ): Result<GeneratedReportFile> = runCatching {
    verifyExportRoleAuthorization(dataset)

    if (!outputDir.exists() && !outputDir.mkdirs()) {
      throw IllegalStateException("Unable to create export directory: ${outputDir.absolutePath}")
    }

    val fileName =
      customFileName?.trim()?.takeIf { it.isNotBlank() }
        ?: buildSafeFileName(dataset, ExportFormat.XLSX)
    val targetFile = File(outputDir, fileName)

    FileOutputStream(targetFile).use { fos ->
      ZipOutputStream(fos).use { zos ->
        writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml())
        writeZipEntry(zos, "_rels/.rels", buildRootRelsXml())
        writeZipEntry(zos, "xl/workbook.xml", buildWorkbookXml())
        writeZipEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())
        writeZipEntry(zos, "xl/styles.xml", buildStylesXml())
        writeZipEntry(zos, "xl/worksheets/sheet1.xml", buildSummaryWorksheetXml(dataset))
        writeZipEntry(zos, "xl/worksheets/sheet2.xml", buildTeacherResultsWorksheetXml(dataset))
        writeZipEntry(zos, "xl/worksheets/sheet3.xml", buildCategoryBreakdownWorksheetXml(dataset))
      }
    }

    if (!targetFile.exists() || targetFile.length() <= 0L) {
      throw IllegalStateException("Generated Excel (.xlsx) workbook is empty.")
    }

    GeneratedReportFile(
      filePath = targetFile.absolutePath,
      fileName = targetFile.name,
      format = ExportFormat.XLSX,
      byteSize = targetFile.length(),
      exportedRecordCount = dataset.totalEvaluationsCount,
      reportScope = dataset.reportScope,
      generatedAtIso = dataset.generatedAtIso,
    )
  }

  /**
   * Launches the Android system share sheet (`Intent.ACTION_SEND`) for a generated PDF or Excel report
   * using AndroidX `FileProvider`.
   */
  fun shareGeneratedReport(
    context: Context,
    reportFile: GeneratedReportFile,
  ): Result<Unit> = runCatching {
    val file = File(reportFile.filePath)
    require(file.exists() && file.length() > 0L) {
      "Report file not found at ${reportFile.filePath}"
    }
    val authority = "${context.packageName}.fileprovider"
    val contentUri = FileProvider.getUriForFile(context, authority, file)
    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = reportFile.format.mimeType
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(
          Intent.EXTRA_SUBJECT,
          "Wachemo University Evaluation Report (${reportFile.reportScope})",
        )
        putExtra(
          Intent.EXTRA_TEXT,
          "Attached ${reportFile.format.displayName}: ${reportFile.fileName} " +
            "(Generated ${reportFile.generatedAtIso} • ${reportFile.exportedRecordCount} evaluation records).",
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    val chooser =
      Intent.createChooser(shareIntent, "Export / Share WCU Evaluation Report").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }

  /**
   * Utility for tests and verification: reads the ZIP entries and worksheet XML contents of a `.xlsx` file.
   */
  fun inspectExcelWorkbook(xlsxFile: File): Map<String, String> {
    val contents = linkedMapOf<String, String>()
    ZipFile(xlsxFile).use { zip ->
      val entries = zip.entries()
      while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        if (!entry.isDirectory) {
          val text =
            zip.getInputStream(entry).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
          contents[entry.name] = text
        }
      }
    }
    return contents
  }

  // =========================================================================
  // PDF 1.4 Multi-Page Vector & Typography Generator
  // =========================================================================

  private fun escapePdfText(input: String): String {
    return input
      .replace("\\", "\\\\")
      .replace("(", "\\(")
      .replace(")", "\\)")
      .replace("\r", " ")
      .replace("\n", " ")
  }

  private fun buildPdf14DocumentBytes(dataset: EvaluationExportDataset): ByteArray {
    val rowsPerFirstPage = 8
    val rowsPerContinuationPage = 18
    val allRows = dataset.teacherCourseRows

    val pageRowChunks: List<List<com.example.data.model.TeacherEvaluationReportRow>> =
      if (allRows.size <= rowsPerFirstPage) {
        listOf(allRows)
      } else {
        val firstChunk = allRows.take(rowsPerFirstPage)
        val remaining = allRows.drop(rowsPerFirstPage).chunked(rowsPerContinuationPage)
        listOf(firstChunk) + remaining
      }

    val totalPages = pageRowChunks.size.coerceAtLeast(1)
    val pageStreams =
      pageRowChunks.mapIndexed { pageIdx, rowsChunk ->
        buildPdfPageContentStream(
          dataset = dataset,
          rowsChunk = rowsChunk,
          pageNumber = pageIdx + 1,
          totalPages = totalPages,
          isFirstPage = pageIdx == 0,
        )
      }

    // Object layout:
    // 1: Catalog
    // 2: Pages
    // 3: Font F1 (Helvetica)
    // 4: Font F2 (Helvetica-Bold)
    // For each page i (0..totalPages-1):
    //   pageObjId = 5 + i * 2
    //   contentObjId = 6 + i * 2
    val out = ByteArrayOutputStream()
    val offsets = mutableMapOf<Int, Int>()

    fun writeAscii(text: String) {
      val bytes = text.toByteArray(StandardCharsets.ISO_8859_1)
      out.write(bytes)
    }

    writeAscii("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")

    // Obj 1: Catalog
    offsets[1] = out.size()
    writeAscii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

    // Obj 2: Pages
    val kidsRefs = (0 until totalPages).joinToString(" ") { i -> "${5 + i * 2} 0 R" }
    offsets[2] = out.size()
    writeAscii("2 0 obj\n<< /Type /Pages /Kids [ $kidsRefs ] /Count $totalPages >>\nendobj\n")

    // Obj 3: Helvetica
    offsets[3] = out.size()
    writeAscii(
      "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n"
    )

    // Obj 4: Helvetica-Bold
    offsets[4] = out.size()
    writeAscii(
      "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n"
    )

    for (i in 0 until totalPages) {
      val pageObjId = 5 + i * 2
      val contentObjId = 6 + i * 2
      val streamText = pageStreams[i]
      val streamBytes = streamText.toByteArray(StandardCharsets.ISO_8859_1)

      offsets[pageObjId] = out.size()
      writeAscii(
        "$pageObjId 0 obj\n" +
          "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
          "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> " +
          "/Contents $contentObjId 0 R >>\n" +
          "endobj\n"
      )

      offsets[contentObjId] = out.size()
      writeAscii("$contentObjId 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n")
      out.write(streamBytes)
      writeAscii("\nendstream\nendobj\n")
    }

    val totalObjects = 4 + totalPages * 2
    val xrefOffset = out.size()
    writeAscii("xref\n0 ${totalObjects + 1}\n")
    writeAscii("0000000000 65535 f \n")
    for (objId in 1..totalObjects) {
      val pos = offsets[objId] ?: 0
      writeAscii(String.format(Locale.US, "%010d 00000 n \n", pos))
    }

    writeAscii(
      "trailer\n" +
        "<< /Size ${totalObjects + 1} /Root 1 0 R >>\n" +
        "startxref\n" +
        "$xrefOffset\n" +
        "%%EOF\n"
    )

    return out.toByteArray()
  }

  private fun buildPdfPageContentStream(
    dataset: EvaluationExportDataset,
    rowsChunk: List<com.example.data.model.TeacherEvaluationReportRow>,
    pageNumber: Int,
    totalPages: Int,
    isFirstPage: Boolean,
  ): String {
    val sb = StringBuilder()

    fun drawRect(x: Int, y: Int, w: Int, h: Int, r: Double, g: Double, b: Double) {
      sb.append(
        String.format(
          Locale.US,
          "%.3f %.3f %.3f rg %d %d %d %d re f\n",
          r,
          g,
          b,
          x,
          y,
          w,
          h,
        )
      )
    }

    fun drawText(
      x: Int,
      y: Int,
      text: String,
      bold: Boolean = false,
      fontSize: Int = 10,
      r: Double = 0.08,
      g: Double = 0.12,
      b: Double = 0.20,
    ) {
      val font = if (bold) "/F2" else "/F1"
      sb.append(
        String.format(
          Locale.US,
          "BT %.3f %.3f %.3f rg %s %d Tf %d %d Td (%s) Tj ET\n",
          r,
          g,
          b,
          font,
          fontSize,
          x,
          y,
          escapePdfText(text),
        )
      )
    }

    // 1. Wachemo University Header Banner (Navy #0B1F3A + Gold #D4AF37 accent bar)
    drawRect(36, 760, 523, 54, 0.043, 0.122, 0.227)
    drawRect(36, 756, 523, 4, 0.831, 0.686, 0.216)

    drawText(
      x = 48,
      y = 793,
      text = "WACHEMO UNIVERSITY (WCU) - ACADEMIC QUALITY ASSURANCE DIRECTORATE",
      bold = true,
      fontSize = 11,
      r = 1.0,
      g = 1.0,
      b = 1.0,
    )
    drawText(
      x = 48,
      y = 777,
      text = dataset.reportTitle.take(85),
      bold = true,
      fontSize = 10,
      r = 0.95,
      g = 0.82,
      b = 0.35,
    )
    drawText(
      x = 48,
      y = 764,
      text = "Generated: ${dataset.generatedAtIso} | Scope: ${dataset.reportScope} | By: ${dataset.generatedByName} (${dataset.generatedByRole})",
      bold = false,
      fontSize = 8,
      r = 0.85,
      g = 0.90,
      b = 0.98,
    )

    var cursorY = 736

    if (isFirstPage) {
      // 2. Institutional Metadata Box
      drawRect(36, cursorY - 62, 523, 66, 0.95, 0.97, 0.99)
      drawText(46, cursorY - 12, "Faculty / College: ${dataset.facultyLabel.take(70)}", bold = true, fontSize = 9)
      drawText(46, cursorY - 26, "Academic Department: ${dataset.departmentLabel.take(70)}", bold = true, fontSize = 9)
      drawText(
        46,
        cursorY - 40,
        "Semester & Academic Year: ${dataset.semesterLabel} (${dataset.academicYearLabel})",
        bold = false,
        fontSize = 9,
      )
      drawText(
        46,
        cursorY - 54,
        "Applied Filters: ${dataset.appliedFilterSummary.take(85)}",
        bold = false,
        fontSize = 8,
        r = 0.25,
        g = 0.32,
        b = 0.42,
      )
      cursorY -= 78

      // 3. Executive Evaluation Statistics & Summary Box
      drawRect(36, cursorY - 44, 523, 46, 0.91, 0.94, 0.98)
      drawText(
        46,
        cursorY - 14,
        "EXECUTIVE EVALUATION SUMMARY & STATISTICS",
        bold = true,
        fontSize = 9,
        r = 0.043,
        g = 0.122,
        b = 0.227,
      )
      val avgFormatted = String.format(Locale.US, "%.2f / 5.00", dataset.overallAverageRating)
      drawText(
        46,
        cursorY - 30,
        "Submitted Evaluations: ${dataset.totalEvaluationsCount}   |   " +
          "Teachers Evaluated: ${dataset.totalTeachersEvaluated}   |   " +
          "Courses Evaluated: ${dataset.totalCoursesEvaluated}   |   " +
          "Overall Average Rating: $avgFormatted",
        bold = true,
        fontSize = 9,
      )
      cursorY -= 58

      // 4. Category-Level Performance Averages & Visual Chart Bars
      drawText(
        36,
        cursorY,
        "6-CATEGORY RUBRIC PERFORMANCE AVERAGES & VISUAL CHART SUMMARY",
        bold = true,
        fontSize = 10,
        r = 0.043,
        g = 0.122,
        b = 0.227,
      )
      cursorY -= 16

      WcuEvaluationRubricCategories.ALL_CATEGORIES.forEachIndexed { idx, (catId, catLabel) ->
        val score = dataset.categoryAverages[catId] ?: 0.0
        val scoreStr = String.format(Locale.US, "%.2f / 5.00", score)
        val barMaxW = 180
        val filledW = ((score.coerceIn(0.0, 5.0) / 5.0) * barMaxW).toInt()
        val asciiBar = "#".repeat((score.coerceIn(0.0, 5.0) * 4).toInt()).padEnd(20, '-')

        drawText(
          42,
          cursorY,
          "0${idx + 1}. $catLabel ($catId)",
          bold = false,
          fontSize = 9,
        )
        // Background track bar
        drawRect(250, cursorY - 2, barMaxW, 8, 0.86, 0.89, 0.93)
        // Filled score bar
        if (filledW > 0) {
          drawRect(250, cursorY - 2, filledW, 8, 0.11, 0.36, 0.66)
        }
        drawText(
          440,
          cursorY,
          "$scoreStr [$asciiBar]",
          bold = true,
          fontSize = 8,
        )
        cursorY -= 15
      }

      cursorY -= 8
    }

    // 5. Teacher & Course Evaluation Results Table
    drawText(
      36,
      cursorY,
      "TEACHER & COURSE EVALUATION RESULTS (${dataset.teacherCourseRows.size} Records)",
      bold = true,
      fontSize = 10,
      r = 0.043,
      g = 0.122,
      b = 0.227,
    )
    cursorY -= 18

    // Table Header
    drawRect(36, cursorY - 4, 523, 18, 0.043, 0.122, 0.227)
    drawText(
      42,
      cursorY,
      "Instructor Name         | Dept     | Course Code & Title            | Sem      | Evals | Avg  | Status",
      bold = true,
      fontSize = 8,
      r = 1.0,
      g = 1.0,
      b = 1.0,
    )
    cursorY -= 18

    if (rowsChunk.isEmpty()) {
      drawRect(36, cursorY - 24, 523, 30, 0.96, 0.97, 0.98)
      drawText(
        48,
        cursorY - 12,
        "No evaluation records matched the selected filter criteria for this report scope.",
        bold = false,
        fontSize = 9,
        r = 0.35,
        g = 0.40,
        b = 0.48,
      )
      cursorY -= 36
    } else {
      rowsChunk.forEachIndexed { idx, row ->
        if (idx % 2 == 0) {
          drawRect(36, cursorY - 14, 523, 26, 0.96, 0.97, 0.99)
        }
        val tName = row.teacherName.take(22).padEnd(22, ' ')
        val dId = row.departmentId.take(8).padEnd(8, ' ')
        val cTitle = "${row.courseCode} ${row.courseName}".take(30).padEnd(30, ' ')
        val sem = row.semesterLabel.take(8).padEnd(8, ' ')
        val cnt = row.evaluationCount.toString().padStart(3, ' ')
        val avg = String.format(Locale.US, "%.2f", row.averageRating)
        val status = row.performanceClassification.take(16)

        drawText(
          42,
          cursorY,
          "$tName | $dId | $cTitle | $sem | $cnt   | $avg | $status",
          bold = true,
          fontSize = 8,
        )

        val catSummaryLine =
          WcuEvaluationRubricCategories.ALL_CATEGORIES.joinToString("  ") { (catId, label) ->
            val v = row.categoryScores[catId] ?: 0.0
            "${label.take(10)}: ${String.format(Locale.US, "%.2f", v)}"
          }
        drawText(
          48,
          cursorY - 10,
          "Rubric Breakdown -> $catSummaryLine",
          bold = false,
          fontSize = 7,
          r = 0.28,
          g = 0.35,
          b = 0.45,
        )
        cursorY -= 28
      }
    }

    // 6. Footer & Page Numbering
    drawRect(36, 38, 523, 1, 0.78, 0.82, 0.88)
    drawText(
      36,
      24,
      "Wachemo University Official Teacher Evaluation Report • Confidential Administrative Record",
      bold = false,
      fontSize = 8,
      r = 0.35,
      g = 0.40,
      b = 0.48,
    )
    drawText(
      465,
      24,
      "Page $pageNumber of $totalPages",
      bold = true,
      fontSize = 8,
      r = 0.043,
      g = 0.122,
      b = 0.227,
    )

    return sb.toString()
  }

  // =========================================================================
  // OpenXML (.xlsx) Multi-Sheet Workbook Generator
  // =========================================================================

  private fun writeZipEntry(zos: ZipOutputStream, entryName: String, xmlContent: String) {
    val entry = ZipEntry(entryName)
    zos.putNextEntry(entry)
    val bytes = xmlContent.toByteArray(StandardCharsets.UTF_8)
    zos.write(bytes)
    zos.closeEntry()
  }

  private fun escapeXml(input: String): String {
    return input
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")
  }

  private fun excelColumnName(colIndex1Based: Int): String {
    var dividend = colIndex1Based
    val columnName = StringBuilder()
    while (dividend > 0) {
      val modulo = (dividend - 1) % 26
      columnName.insert(0, ('A'.code + modulo).toChar())
      dividend = (dividend - modulo) / 26
    }
    return columnName.toString()
  }

  private fun buildContentTypesXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

  private fun buildRootRelsXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

  private fun buildWorkbookXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Summary &amp; Metadata" sheetId="1" r:id="rId1"/>
    <sheet name="Teacher Evaluations" sheetId="2" r:id="rId2"/>
    <sheet name="Category Rubric Scores" sheetId="3" r:id="rId3"/>
  </sheets>
</workbook>"""

  private fun buildWorkbookRelsXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

  private fun buildStylesXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="3">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FF0B1F3A"/><name val="Calibri"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0B1F3A"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF2F6FA"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFD0D7DE"/></left>
      <right style="thin"><color rgb="FFD0D7DE"/></right>
      <top style="thin"><color rgb="FFD0D7DE"/></top>
      <bottom style="thin"><color rgb="FFD0D7DE"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="3">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
  </cellXfs>
</styleSheet>"""

  private sealed interface CellSpec {
    val styleIndex: Int

    data class Text(val value: String, override val styleIndex: Int = 0) : CellSpec

    data class NumberValue(val value: Number, override val styleIndex: Int = 0) : CellSpec
  }

  private fun buildWorksheetXmlFromRows(rows: List<List<CellSpec>>): String {
    val sb = StringBuilder()
    sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""").append('\n')
    sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
      .append('\n')
    sb.append("  <sheetData>\n")

    rows.forEachIndexed { rIdx, rowCells ->
      val rowNum = rIdx + 1
      sb.append("    <row r=\"$rowNum\">\n")
      rowCells.forEachIndexed { cIdx, cell ->
        val cellRef = "${excelColumnName(cIdx + 1)}$rowNum"
        when (cell) {
          is CellSpec.Text -> {
            sb.append(
              "      <c r=\"$cellRef\" s=\"${cell.styleIndex}\" t=\"inlineStr\"><is><t>${escapeXml(cell.value)}</t></is></c>\n"
            )
          }
          is CellSpec.NumberValue -> {
            val formatted =
              if (cell.value is Double || cell.value is Float) {
                String.format(Locale.US, "%.2f", cell.value.toDouble())
              } else {
                cell.value.toString()
              }
            sb.append(
              "      <c r=\"$cellRef\" s=\"${cell.styleIndex}\"><v>$formatted</v></c>\n"
            )
          }
        }
      }
      sb.append("    </row>\n")
    }

    sb.append("  </sheetData>\n")
    sb.append("</worksheet>")
    return sb.toString()
  }

  private fun buildSummaryWorksheetXml(dataset: EvaluationExportDataset): String {
    val rows = mutableListOf<List<CellSpec>>()
    rows += listOf(
      CellSpec.Text("WACHEMO UNIVERSITY (WCU) - OFFICIAL EVALUATION REPORT", styleIndex = 1),
      CellSpec.Text(dataset.reportScope, styleIndex = 1),
    )
    rows += listOf(
      CellSpec.Text("Report Title", styleIndex = 2),
      CellSpec.Text(dataset.reportTitle),
    )
    rows += listOf(
      CellSpec.Text("Generated Timestamp", styleIndex = 2),
      CellSpec.Text(dataset.generatedAtIso),
    )
    rows += listOf(
      CellSpec.Text("Generated By Administrator", styleIndex = 2),
      CellSpec.Text("${dataset.generatedByName} (${dataset.generatedByUid})"),
    )
    rows += listOf(
      CellSpec.Text("Administrator Role", styleIndex = 2),
      CellSpec.Text(dataset.generatedByRole),
    )
    rows += listOf(
      CellSpec.Text("Faculty / College", styleIndex = 2),
      CellSpec.Text(dataset.facultyLabel),
    )
    rows += listOf(
      CellSpec.Text("Academic Department", styleIndex = 2),
      CellSpec.Text(dataset.departmentLabel),
    )
    rows += listOf(
      CellSpec.Text("Semester", styleIndex = 2),
      CellSpec.Text(dataset.semesterLabel),
    )
    rows += listOf(
      CellSpec.Text("Academic Year", styleIndex = 2),
      CellSpec.Text(dataset.academicYearLabel),
    )
    rows += listOf(
      CellSpec.Text("Applied Filter Criteria", styleIndex = 2),
      CellSpec.Text(dataset.appliedFilterSummary),
    )
    rows += listOf(
      CellSpec.Text("Total Submitted Evaluations", styleIndex = 2),
      CellSpec.NumberValue(dataset.totalEvaluationsCount),
    )
    rows += listOf(
      CellSpec.Text("Total Teachers Evaluated", styleIndex = 2),
      CellSpec.NumberValue(dataset.totalTeachersEvaluated),
    )
    rows += listOf(
      CellSpec.Text("Total Courses Evaluated", styleIndex = 2),
      CellSpec.NumberValue(dataset.totalCoursesEvaluated),
    )
    rows += listOf(
      CellSpec.Text("Overall Average Rating (Out of 5.00)", styleIndex = 2),
      CellSpec.NumberValue(dataset.overallAverageRating),
    )

    // Category summary section in Sheet 1
    rows += listOf(
      CellSpec.Text("Rubric Category ID", styleIndex = 1),
      CellSpec.Text("Rubric Category Name", styleIndex = 1),
      CellSpec.Text("Average Score (Out of 5.00)", styleIndex = 1),
    )
    WcuEvaluationRubricCategories.ALL_CATEGORIES.forEach { (catId, label) ->
      rows += listOf(
        CellSpec.Text(catId),
        CellSpec.Text(label),
        CellSpec.NumberValue(dataset.categoryAverages[catId] ?: 0.0),
      )
    }

    return buildWorksheetXmlFromRows(rows)
  }

  private fun buildTeacherResultsWorksheetXml(dataset: EvaluationExportDataset): String {
    val rows = mutableListOf<List<CellSpec>>()

    // Header Row (19 columns)
    rows += listOf(
      CellSpec.Text("Teacher ID", styleIndex = 1),
      CellSpec.Text("Teacher Name", styleIndex = 1),
      CellSpec.Text("Teacher Email", styleIndex = 1),
      CellSpec.Text("Specialization", styleIndex = 1),
      CellSpec.Text("Faculty", styleIndex = 1),
      CellSpec.Text("Department", styleIndex = 1),
      CellSpec.Text("Course Code", styleIndex = 1),
      CellSpec.Text("Course Name", styleIndex = 1),
      CellSpec.Text("Semester", styleIndex = 1),
      CellSpec.Text("Academic Year", styleIndex = 1),
      CellSpec.Text("Evaluation Count", styleIndex = 1),
      CellSpec.Text("Average Rating", styleIndex = 1),
      CellSpec.Text("Teaching Quality (cat_1)", styleIndex = 1),
      CellSpec.Text("Communication Skills (cat_2)", styleIndex = 1),
      CellSpec.Text("Punctuality & Preparation (cat_3)", styleIndex = 1),
      CellSpec.Text("Course Management (cat_4)", styleIndex = 1),
      CellSpec.Text("Student Support (cat_5)", styleIndex = 1),
      CellSpec.Text("Professionalism & Ethics (cat_6)", styleIndex = 1),
      CellSpec.Text("Overall Evaluation Result", styleIndex = 1),
    )

    if (dataset.teacherCourseRows.isEmpty()) {
      rows += listOf(
        CellSpec.Text("N/A"),
        CellSpec.Text("No matching evaluation records for the selected filters"),
        CellSpec.Text(""),
        CellSpec.Text(""),
        CellSpec.Text(dataset.facultyLabel),
        CellSpec.Text(dataset.departmentLabel),
        CellSpec.Text("N/A"),
        CellSpec.Text("No courses matched"),
        CellSpec.Text(dataset.semesterLabel),
        CellSpec.Text(dataset.academicYearLabel),
        CellSpec.NumberValue(0),
        CellSpec.NumberValue(0.0),
        CellSpec.NumberValue(0.0),
        CellSpec.NumberValue(0.0),
        CellSpec.NumberValue(0.0),
        CellSpec.NumberValue(0.0),
        CellSpec.NumberValue(0.0),
        CellSpec.NumberValue(0.0),
        CellSpec.Text("No Submissions Yet"),
      )
    } else {
      dataset.teacherCourseRows.forEach { row ->
        rows += listOf(
          CellSpec.Text(row.teacherId),
          CellSpec.Text(row.teacherName),
          CellSpec.Text(row.teacherEmail),
          CellSpec.Text(row.specialization),
          CellSpec.Text(row.facultyName),
          CellSpec.Text(row.departmentName),
          CellSpec.Text(row.courseCode),
          CellSpec.Text(row.courseName),
          CellSpec.Text(row.semesterLabel),
          CellSpec.Text(row.academicYear),
          CellSpec.NumberValue(row.evaluationCount),
          CellSpec.NumberValue(row.averageRating),
          CellSpec.NumberValue(row.categoryScores["cat_1"] ?: 0.0),
          CellSpec.NumberValue(row.categoryScores["cat_2"] ?: 0.0),
          CellSpec.NumberValue(row.categoryScores["cat_3"] ?: 0.0),
          CellSpec.NumberValue(row.categoryScores["cat_4"] ?: 0.0),
          CellSpec.NumberValue(row.categoryScores["cat_5"] ?: 0.0),
          CellSpec.NumberValue(row.categoryScores["cat_6"] ?: 0.0),
          CellSpec.Text(row.performanceClassification),
        )
      }
    }

    return buildWorksheetXmlFromRows(rows)
  }

  private fun buildCategoryBreakdownWorksheetXml(dataset: EvaluationExportDataset): String {
    val rows = mutableListOf<List<CellSpec>>()
    rows += listOf(
      CellSpec.Text("Category Code", styleIndex = 1),
      CellSpec.Text("Evaluation Rubric Pillar", styleIndex = 1),
      CellSpec.Text("Average Score", styleIndex = 1),
      CellSpec.Text("Percentage of 5.00 Scale", styleIndex = 1),
    )

    WcuEvaluationRubricCategories.ALL_CATEGORIES.forEach { (catId, label) ->
      val avg = dataset.categoryAverages[catId] ?: 0.0
      val pct = (avg / 5.0) * 100.0
      rows += listOf(
        CellSpec.Text(catId),
        CellSpec.Text(label),
        CellSpec.NumberValue(avg),
        CellSpec.NumberValue(pct),
      )
    }

    if (dataset.departmentComparisonRows.isNotEmpty()) {
      rows += listOf(
        CellSpec.Text("Department ID", styleIndex = 1),
        CellSpec.Text("Department Name", styleIndex = 1),
        CellSpec.Text("Faculty Name", styleIndex = 1),
        CellSpec.Text("Active Students", styleIndex = 1),
        CellSpec.Text("Active Teachers", styleIndex = 1),
        CellSpec.Text("Active Courses", styleIndex = 1),
        CellSpec.Text("Submitted Evaluations", styleIndex = 1),
        CellSpec.Text("Average Score", styleIndex = 1),
        CellSpec.Text("Completion %", styleIndex = 1),
      )
      dataset.departmentComparisonRows.forEach { deptRow ->
        rows += listOf(
          CellSpec.Text(deptRow.departmentId),
          CellSpec.Text(deptRow.departmentName),
          CellSpec.Text(deptRow.facultyName),
          CellSpec.NumberValue(deptRow.studentCount),
          CellSpec.NumberValue(deptRow.activeTeacherCount),
          CellSpec.NumberValue(deptRow.activeCourseCount),
          CellSpec.NumberValue(deptRow.submittedEvaluationCount),
          CellSpec.NumberValue(deptRow.averageScore),
          CellSpec.NumberValue(deptRow.completionPercentage),
        )
      }
    }

    return buildWorksheetXmlFromRows(rows)
  }
}
