package cn.yhsb.base

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.nio.file.Files
import java.nio.file.Paths


object Excels {
    enum class Type {
        XLS, XLSX, AUTO
    }

    fun load(fileName: String, type: Type = Type.AUTO): Workbook {
        var t = type
        if (t == Type.AUTO) {
            val fn = fileName.toLowerCase()
            t = when {
                fn.endsWith(".xls") -> Type.XLS
                fn.endsWith(".xlsx") -> Type.XLSX
                else -> Type.AUTO
            }
        }
        return when (t) {
            Type.XLS -> HSSFWorkbook(Files.newInputStream(Paths.get(fileName)))
            Type.XLSX -> XSSFWorkbook(Files.newInputStream(Paths.get(fileName)))
            Type.AUTO -> throw UnsupportedOperationException("Unknown excel type")
        }
    }
}

fun Workbook.save(fileName: String) {
    Files.newOutputStream(Paths.get(fileName)).use { out -> write(out) }
}


class ExcelException : RuntimeException {
    constructor(msg: String) : super(msg)
    constructor(e: Exception) : super(e)
}

fun Sheet.createRow(targetRowIndex: Int,
                    sourceRowIndex: Int, clearValue: Boolean): Row {
    if (targetRowIndex == sourceRowIndex) throw ExcelException(
            "sourceIndex and targetIndex cannot be same")
    var newRow = getRow(targetRowIndex)
    val srcRow = getRow(sourceRowIndex)
    if (newRow != null) {
        shiftRows(targetRowIndex, lastRowNum, 1, true, false)
    }
    newRow = createRow(targetRowIndex)
    newRow.height = srcRow.height
    for (idx in srcRow.firstCellNum until srcRow
            .physicalNumberOfCells) {
        val srcCell = srcRow.getCell(idx) ?: continue
        val newCell = newRow.createCell(idx)
        newCell.cellStyle = srcCell.cellStyle
        newCell.cellComment = srcCell.cellComment
        newCell.hyperlink = srcCell.hyperlink
        when (srcCell.cellType) {
            CellType.NUMERIC -> newCell.setCellValue(
                    if (clearValue) 0.0 else srcCell.numericCellValue)
            CellType.STRING -> newCell.setCellValue(
                    if (clearValue) "" else srcCell.stringCellValue)
            CellType.FORMULA -> newCell.cellFormula = srcCell.cellFormula
            CellType.BLANK -> newCell.setBlank()
            CellType.BOOLEAN -> newCell.setCellValue(
                    if (clearValue) false else srcCell.booleanCellValue)
            CellType.ERROR -> newCell.setCellErrorValue(srcCell.errorCellValue)
            else -> {
            }
        }
    }
    val merged = CellRangeAddressList()
    for (i in 0 until numMergedRegions) {
        val address = getMergedRegion(i)
        if (sourceRowIndex == address.firstRow
                && sourceRowIndex == address.lastRow) {
            merged.addCellRangeAddress(targetRowIndex,
                    address.firstColumn, targetRowIndex,
                    address.lastColumn)
        }
    }
    for (region in merged.cellRangeAddresses) {
        addMergedRegion(region)
    }
    return newRow
}

fun Sheet.getOrCopyRowFrom(targetRowIndex: Int,
                           sourceRowIndex: Int, clearValue: Boolean = false): Row {
    return if (targetRowIndex == sourceRowIndex) {
        getRow(sourceRowIndex)
    } else {
        if (lastRowNum >= targetRowIndex)
            shiftRows(targetRowIndex, lastRowNum, 1, true, false)
        createRow(targetRowIndex, sourceRowIndex, clearValue)
    }
}

fun Sheet.copyRowsFrom(start: Int, count: Int,
                       srcRowIdx: Int, clearValue: Boolean = false) {
    shiftRows(start, lastRowNum, count, true, false)
    for (i in 0 until count) {
        createRow(start + i, srcRowIdx, clearValue)
    }
}

fun Sheet.getRowIterator(start: Int, end: Int): Iterator<Row> {
    return object : Iterator<Row> {
        private var index = 0.coerceAtLeast(start)
        private val last = (end - 1).coerceAtMost(lastRowNum)

        override fun hasNext(): Boolean {
            return index <= last
        }

        override fun next(): Row {
            return getRow(index++)
        }
    }
}

