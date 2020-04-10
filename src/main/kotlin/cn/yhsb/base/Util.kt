package cn.yhsb.base

import picocli.CommandLine
import java.lang.StringBuilder


@CommandLine.Command(mixinStandardHelpOptions = true)
abstract class CommandWithHelp : Runnable

object DateTime {
    fun toDashedDate(date: String,
                     format: String = """^(\d\d\d\d)(\d\d)(\d\d)$"""): String {
        val m = Regex(format).find(date)
        if (m != null) {
            return m.groupValues.drop(1).joinToString("-")
        }
        throw IllegalArgumentException("invalid date format")
    }
}

data class SpecialChars(val range: CharRange, val width: Int)

val chineseChars = SpecialChars(CharRange('\u4e00', '\u9fa5'), 2)

fun padCount(s: String, width: Int, specialChars: Array<out SpecialChars>): Int {
    var n = 0
    out@for (c in s) {
        for ((r, w) in specialChars) {
            if (c in r) {
                n += w
                continue@out
            }
        }
        n += 1
    }
    return width - n
}

private fun Char.times(n: Int, b: StringBuilder) {
    for (i in 1..n) {
        b.append(this)
    }
}

fun String.padLeft(width: Int, padChar: Char = ' ',
                   vararg specialChars: SpecialChars = arrayOf(chineseChars)): String {
    val count = padCount(this, width, specialChars)
    return if (count > 0) {
        val b = StringBuilder()
        padChar.times(count, b)
        b.append(this)
        b.toString()
    } else {
        this
    }
}

fun String.padRight(width: Int, padChar: Char = ' ',
                  vararg specialChars: SpecialChars = arrayOf(chineseChars)): String {
    val count = padCount(this, width, specialChars)
    return if (count > 0) {
        val b = StringBuilder()
        b.append(this)
        padChar.times(count, b)
        b.toString()
    } else {
        this
    }
}