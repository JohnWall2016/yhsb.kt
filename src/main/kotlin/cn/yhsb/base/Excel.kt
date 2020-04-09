package cn.yhsb.base

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.ICell
import java.math.BigDecimal
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

/**
 * get the cell by address
 * @param address format "(\$?)([A-Z]+)(\$?)(\d+)"
 */
fun Sheet.cell(address: String): Cell {
    val cell = CellRef.fromAddress(address)
    return getRow(cell.row - 1).getCell(cell.column - 1)
}

/**
 * get the cell by row and column
 * @param row from 1
 * @param column from 1
 */
fun Sheet.cell(row: Int, column: Int): Cell = getRow(row - 1).getCell(column - 1)

fun Cell.setValue(v: String?) = setCellValue(v ?: "")
fun Cell.setValue(v: Double?) = setCellValue(v ?: 0.0)
fun Cell.setValue(v: BigDecimal?) = setCellValue(v?.toString() ?: "")
fun Cell.setValue(v: Int?) = setCellValue(v?.toString() ?: "")


/**
 * get cell from column name
 * @param column format "[A-Z]+"
 */
fun Row.cell(column: String): Cell = getCell(CellRef.columnNameToNumber(column) - 1)

/**
 * get cell from column position
 * @param column from 1
 */
fun Row.cell(column: Int): Cell = getCell(column - 1)

class CellRef(val row: Int, val column: Int,
              anchored: Boolean = false,
              rowAnchored: Boolean = false,
              columnAnchored: Boolean = false,
              columnName: String? = null) {
    val columnName: String = columnName ?: columnNumberToName(column)
    val rowAnchored: Boolean = anchored || rowAnchored
    val columnAnchored: Boolean = anchored || columnAnchored

    companion object {
        val cellRegex = Regex("""(\$?)([A-Z]+)(\$?)(\d+)""")

        fun columnNumberToName(number: Int): String {
            var dividend = number
            var name = ""
            while (dividend > 0) {
                val modulo = (dividend - 1) % 26
                name = (65 + modulo).toChar().toString() + name
                dividend = (dividend - modulo) / 26
            }
            return name
        }

        fun columnNameToNumber(name: String): Int {
            var sum = 0
            for (c in name.toUpperCase()) {
                sum *= 26
                sum += c.toInt() - 64
            }
            return sum
        }

        fun fromAddress(address: String): CellRef {
            val match = cellRegex.find(address)
            if (match != null) {
                return CellRef(
                        columnAnchored = match.groupValues[1].isNotEmpty(),
                        columnName = match.groupValues[2],
                        column = columnNameToNumber(match.groupValues[2]),
                        rowAnchored = match.groupValues[3].isNotEmpty(),
                        row = match.groupValues[4].toInt()
                )
            }
            throw ExcelException("invalid cell address")
        }
    }

    fun toAddress(): String {
        var address = ""
        if (columnAnchored) address += "$"
        address += columnName
        if (rowAnchored) address += "$"
        address += row.toString()
        return address
    }
}
