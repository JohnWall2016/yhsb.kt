package cn.yhsb.base

import org.junit.Assert.assertEquals
import org.junit.Test

class ExcelTest {
    @Test
    fun testExcel() {
        var ref = CellRef.fromAddress("B3")
        assertEquals(ref.column, 2)
        assertEquals(ref.row, 3)

        ref = CellRef.fromAddress("\$B\$3")
        assertEquals(ref.columnAnchored, true)
        assertEquals(ref.column, 2)
        assertEquals(ref.rowAnchored, true)
        assertEquals(ref.row, 3)
    }
}