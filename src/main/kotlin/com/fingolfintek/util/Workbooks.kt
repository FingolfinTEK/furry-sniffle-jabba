package com.fingolfintek.util

import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFSheet

fun String.safe() = WorkbookUtil.createSafeSheetName(this)

fun XSSFRow.createCell(index: Int, text: String): XSSFCell {
  val compatible = createCell(index)
  compatible.cellStyle = sheet.wrappableTextStyle()
  compatible.setCellValue(text)
  return compatible
}

fun XSSFSheet.wrappableTextStyle(): XSSFCellStyle {
  val style = this.workbook.createCellStyle()
  style.wrapText = true
  style.setVerticalAlignment(VerticalAlignment.CENTER)
  return style
}
