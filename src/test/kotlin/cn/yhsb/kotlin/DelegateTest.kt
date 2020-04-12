package cn.yhsb.kotlin

import org.junit.Test
import org.junit.Assert
import kotlin.reflect.KProperty

class DelegateTest {
    var content: String=""

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        println("Delegate setValue: $value")
        content = value
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        println("Delegate getValue: $content")
        return content
    }

    @Test
    fun testDelegate() {
        var s by DelegateTest()
        Assert.assertEquals(s, "")
        s = "abc"
        Assert.assertEquals(s, "abc")
    }
}