package cn.yhsb.base

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilTest {
    @Test
    fun testPadCount() {
        assertEquals(padCount("中国", 4, arrayOf(chineseChars)), 0)
        assertEquals(padCount("周春秀", 10, arrayOf(chineseChars)), 4)
        assertEquals(padCount("朱亮", 10, arrayOf(chineseChars)), 6)
    }
}