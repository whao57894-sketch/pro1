package com.example.myapplication.utils

import android.content.Context
import android.os.Environment
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtil {

    /**
     * 导出记账数据为 Excel 文件
     * @param context 上下文
     * @param records 记账记录 JSON 数组
     * @param month 月份（如 "2026-06"）
     * @return 导出的文件路径，失败返回 null
     */
    fun exportToExcel(context: Context, records: JSONArray, month: String): String? {
        return try {
            // 创建工作簿
            val workbook: Workbook = XSSFWorkbook()
            val sheet: Sheet = workbook.createSheet("记账记录-$month")

            // 创建标题行样式
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
            }

            val headerFont = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 12
            }
            headerStyle.setFont(headerFont)

            // 创建标题行
            val headerRow: Row = sheet.createRow(0)
            val headers = arrayOf("日期", "类型", "类目", "金额", "备注")
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            // 创建数据样式
            val dataStyle = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.LEFT
                verticalAlignment = VerticalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
            }

            val amountStyle = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
            }

            // 填充数据
            var totalIncome = 0.0
            var totalExpense = 0.0

            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                val row = sheet.createRow(i + 1)

                // 日期
                row.createCell(0).apply {
                    setCellValue(record.optString("recordDate"))
                    cellStyle = dataStyle
                }

                // 类型
                val type = record.optString("type")
                row.createCell(1).apply {
                    setCellValue(type)
                    cellStyle = dataStyle
                }

                // 类目
                row.createCell(2).apply {
                    setCellValue(record.optString("category"))
                    cellStyle = dataStyle
                }

                // 金额
                val amount = record.optDouble("amount", 0.0)
                row.createCell(3).apply {
                    setCellValue(amount)
                    cellStyle = amountStyle
                }

                // 备注
                row.createCell(4).apply {
                    setCellValue(record.optString("remark", ""))
                    cellStyle = dataStyle
                }

                // 累计统计
                if (type == "收入") {
                    totalIncome += amount
                } else {
                    totalExpense += amount
                }
            }

            // 添加汇总行
            val summaryRow = sheet.createRow(records.length() + 2)
            val summaryStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
            }
            val summaryFont = workbook.createFont().apply {
                bold = true
            }
            summaryStyle.setFont(summaryFont)

            summaryRow.createCell(0).apply {
                setCellValue("汇总")
                cellStyle = summaryStyle
            }
            summaryRow.createCell(1).apply {
                setCellValue("收入总计")
                cellStyle = summaryStyle
            }
            summaryRow.createCell(2).apply {
                setCellValue(totalIncome)
                cellStyle = summaryStyle
            }
            summaryRow.createCell(3).apply {
                setCellValue("支出总计")
                cellStyle = summaryStyle
            }
            summaryRow.createCell(4).apply {
                setCellValue(totalExpense)
                cellStyle = summaryStyle
            }

            // 自动调整列宽
            for (i in 0 until headers.size) {
                sheet.autoSizeColumn(i)
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000)
            }

            // 保存文件
            val fileName = "记账记录_${month}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.xlsx"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            val fileOutputStream = FileOutputStream(file)
            workbook.write(fileOutputStream)
            fileOutputStream.close()
            workbook.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 导出为 CSV 格式
     */
    fun exportToCSV(context: Context, records: JSONArray, month: String): String? {
        return try {
            val fileName = "记账记录_${month}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            file.bufferedWriter().use { writer ->
                // 写入 UTF-8 BOM，使 Excel 正确识别中文
                writer.write("﻿")

                // 写入标题行
                writer.write("日期,类型,类目,金额,备注\n")

                // 写入数据行
                var totalIncome = 0.0
                var totalExpense = 0.0

                for (i in 0 until records.length()) {
                    val record = records.getJSONObject(i)
                    val date = record.optString("recordDate")
                    val type = record.optString("type")
                    val category = record.optString("category")
                    val amount = record.optDouble("amount", 0.0)
                    val remark = record.optString("remark", "").replace(",", "，") // 替换逗号避免 CSV 格式问题

                    writer.write("$date,$type,$category,$amount,$remark\n")

                    if (type == "收入") {
                        totalIncome += amount
                    } else {
                        totalExpense += amount
                    }
                }

                // 写入汇总行
                writer.write("\n汇总,收入总计,$totalIncome,支出总计,$totalExpense\n")
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 生成简单的文本格式报告
     */
    fun exportToText(context: Context, records: JSONArray, month: String): String? {
        return try {
            val fileName = "记账记录_${month}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            file.bufferedWriter().use { writer ->
                writer.write("=".repeat(50) + "\n")
                writer.write("记账记录 - $month\n")
                writer.write("=".repeat(50) + "\n\n")

                var totalIncome = 0.0
                var totalExpense = 0.0

                for (i in 0 until records.length()) {
                    val record = records.getJSONObject(i)
                    val date = record.optString("recordDate")
                    val type = record.optString("type")
                    val category = record.optString("category")
                    val amount = record.optDouble("amount", 0.0)
                    val remark = record.optString("remark", "")

                    writer.write("【$date】\n")
                    writer.write("  类型：$type\n")
                    writer.write("  类目：$category\n")
                    writer.write("  金额：¥$amount\n")
                    if (remark.isNotBlank()) {
                        writer.write("  备注：$remark\n")
                    }
                    writer.write("-".repeat(50) + "\n")

                    if (type == "收入") {
                        totalIncome += amount
                    } else {
                        totalExpense += amount
                    }
                }

                writer.write("\n" + "=".repeat(50) + "\n")
                writer.write("汇总统计\n")
                writer.write("=".repeat(50) + "\n")
                writer.write("收入总计：¥$totalIncome\n")
                writer.write("支出总计：¥$totalExpense\n")
                writer.write("结余：¥${totalIncome - totalExpense}\n")
                writer.write("=".repeat(50) + "\n")
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}